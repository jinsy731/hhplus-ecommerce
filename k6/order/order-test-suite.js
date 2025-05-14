import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의 - 공통
const orderCreationErrors = new Counter('order_creation_errors');
const apiCallErrors = new Rate('api_call_errors');

// 메트릭 정의 - 시나리오별
const sparseOrderTrend = new Trend('sparse_order_p99_p95_p50');
const concentratedOrderTrend = new Trend('concentrated_order_p99_p95_p50');
const mixedOrderTrend = new Trend('mixed_order_p99_p95_p50');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 0.5;
const THINK_TIME_MAX = 2;

// 테스트 데이터 - 상품
// 분산된 상품 목록 (다양한 상품에 분산된 주문을 위한)
const sparseProducts = [];
for (let i = 1; i <= 100; i++) {
  sparseProducts.push({ 
    id: i, 
    variantId: Math.floor((i - 1) / 5) + 1
  });
}

// 인기 상품 목록 (소수의 인기 상품에 집중된 주문을 위한)
const hotProducts = [
  { id: 1, variantId: 1 },
  { id: 2, variantId: 1 },
  { id: 3, variantId: 2 },
  { id: 4, variantId: 2 },
  { id: 5, variantId: 3 }
];

// 테스트용 사용자와 쿠폰 데이터
const users = Array.from({ length: 50 }, (_, i) => i + 1);
const coupons = Array.from({ length: 10 }, (_, i) => i + 1);

// 테스트 구성
export let options = {
  // 전체 테스트 시나리오 구성
  scenarios: {
    // 시나리오 1: 분산된 상품에 대한 주문 요청
    sparse_order_scenario: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'sparseOrderTest'
    },
    
    // 시나리오 2: 집중된 상품에 대한 주문 요청
    concentrated_order_scenario: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 30 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'concentratedOrderTest',
      startTime: '3m' // 이전 시나리오 종료 후 시작
    },
    
    // 시나리오 3: 혼합 패턴 (현실적인 시나리오)
    mixed_order_scenario: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 40 },
        { duration: '1m', target: 40 },
        { duration: '30s', target: 60 },
        { duration: '1m', target: 60 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'mixedOrderTest',
      startTime: '6m' // 이전 시나리오 종료 후 시작
    }
  },
  
  // 임계값 설정
  thresholds: {
    'http_req_duration': ['p(99)<7000', 'p(95)<5000', 'p(50)<3000'],
    'sparse_order_p99_p95_p50': ['p(99)<5000', 'p(95)<3000', 'p(50)<1500'],
    'concentrated_order_p99_p95_p50': ['p(99)<7000', 'p(95)<5000', 'p(50)<2500'],
    'mixed_order_p99_p95_p50': ['p(99)<6000', 'p(95)<4000', 'p(50)<2000'],
    'api_call_errors': ['rate<0.1']
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 공통 함수: 주문 실행
function executeOrder(userId, items, userCouponIds, metricTrend) {
  const orderPayload = {
    userId: userId,
    items: items,
    userCouponIds: userCouponIds
  };
  
  const startTime = new Date().getTime();
  const orderResponse = http.post(
    `${BASE_URL}/orders`, 
    JSON.stringify(orderPayload), 
    { headers }
  );
  const endTime = new Date().getTime();
  const duration = endTime - startTime;
  
  // 응답 확인
  const isSuccessful = check(orderResponse, {
    'Order Creation - Status 200': (r) => r.status === 200,
    'Order Creation - Has Order ID': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.orderId;
      } catch (e) {
        return false;
      }
    }
  });
  
  if (!isSuccessful) {
    orderCreationErrors.add(1);
    apiCallErrors.add(1);
  }
  
  // 응답 시간 지표 기록
  metricTrend.add(duration);
  
  return {
    isSuccessful,
    duration,
    response: orderResponse
  };
}

// 랜덤 쿠폰 ID 생성
function getRandomCoupons() {
  let userCouponIds = [];
  const useCoupon = Math.random() > 0.3; // 70% 확률로 쿠폰
  
  if (useCoupon) {
    const numCoupons = randomIntBetween(1, 2);
    for (let i = 0; i < numCoupons; i++) {
      const couponId = coupons[Math.floor(Math.random() * coupons.length)];
      if (!userCouponIds.includes(couponId)) {
        userCouponIds.push(couponId);
      }
    }
  }
  
  return userCouponIds;
}

// 시나리오 1: 분산된 상품에 대한 주문 요청
export function sparseOrderTest() {
  group('Sparse Order Pattern', () => {
    const userId = users[Math.floor(Math.random() * users.length)];
    const numItems = randomIntBetween(1, 3);
    let selectedProducts = new Set();
    let orderItems = [];
    
    while (orderItems.length < numItems) {
      const product = sparseProducts[Math.floor(Math.random() * sparseProducts.length)];
      const productKey = `${product.id}-${product.variantId}`;
      
      if (!selectedProducts.has(productKey)) {
        selectedProducts.add(productKey);
        orderItems.push({
          productId: product.id,
          variantId: product.variantId,
          quantity: randomIntBetween(1, 3)
        });
      }
    }
    
    const userCouponIds = getRandomCoupons();
    
    const result = executeOrder(userId, orderItems, userCouponIds, sparseOrderTrend);
    
    if (result.isSuccessful) {
      console.log(`[Sparse] Order successful: ${result.duration}ms`);
    } else {
      console.log(`[Sparse] Order failed: ${result.response.status}`);
    }
  });
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}

// 시나리오 2: 집중된 상품에 대한 주문 요청
export function concentratedOrderTest() {
  group('Concentrated Order Pattern', () => {
    const userId = users[Math.floor(Math.random() * users.length)];
    const numItems = randomIntBetween(1, 3);
    let selectedProducts = new Set();
    let orderItems = [];
    
    while (orderItems.length < numItems) {
      const product = hotProducts[Math.floor(Math.random() * hotProducts.length)];
      const productKey = `${product.id}-${product.variantId}`;
      
      if (!selectedProducts.has(productKey)) {
        selectedProducts.add(productKey);
        orderItems.push({
          productId: product.id,
          variantId: product.variantId,
          quantity: randomIntBetween(1, 5)
        });
      }
    }
    
    const userCouponIds = getRandomCoupons();
    
    const result = executeOrder(userId, orderItems, userCouponIds, concentratedOrderTrend);
    
    if (result.isSuccessful) {
      console.log(`[Concentrated] Order successful: ${result.duration}ms`);
    } else {
      console.log(`[Concentrated] Order failed: ${result.response.status}`);
    }
  });
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}

// 시나리오 3: 혼합 패턴 (인기 상품과 일반 상품 혼합)
export function mixedOrderTest() {
  group('Mixed Order Pattern', () => {
    const userId = users[Math.floor(Math.random() * users.length)];
    const numItems = randomIntBetween(1, 4);
    let selectedProducts = new Set();
    let orderItems = [];
    
    // 80% 확률로 인기 상품을 1개 이상 포함
    const includeHotProduct = Math.random() <= 0.8;
    
    if (includeHotProduct) {
      const hotProduct = hotProducts[Math.floor(Math.random() * hotProducts.length)];
      const productKey = `${hotProduct.id}-${hotProduct.variantId}`;
      
      selectedProducts.add(productKey);
      orderItems.push({
        productId: hotProduct.id,
        variantId: hotProduct.variantId,
        quantity: randomIntBetween(1, 3)
      });
    }
    
    // 나머지 상품은 분산된 상품에서 선택
    while (orderItems.length < numItems) {
      const product = sparseProducts[Math.floor(Math.random() * sparseProducts.length)];
      const productKey = `${product.id}-${product.variantId}`;
      
      if (!selectedProducts.has(productKey)) {
        selectedProducts.add(productKey);
        orderItems.push({
          productId: product.id,
          variantId: product.variantId,
          quantity: randomIntBetween(1, 3)
        });
      }
    }
    
    const userCouponIds = getRandomCoupons();
    
    const result = executeOrder(userId, orderItems, userCouponIds, mixedOrderTrend);
    
    if (result.isSuccessful) {
      console.log(`[Mixed] Order successful: ${result.duration}ms`);
    } else {
      console.log(`[Mixed] Order failed: ${result.response.status}`);
    }
  });
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}

// 기본 실행 함수 (시나리오 없이 실행할 경우)
export default function() {
  mixedOrderTest();
}
