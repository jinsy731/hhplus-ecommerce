import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const apiTrend = new Trend('api_trend');
const apiErrors = new Counter('api_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';

// 테스트 데이터
const users = Array.from({ length: 20 }, (_, i) => i + 1);
const products = [
  { id: 1, variantId: 1 },
  { id: 2, variantId: 1 },
  { id: 3, variantId: 2 },
  { id: 4, variantId: 2 },
  { id: 5, variantId: 3 }
];
const coupons = Array.from({ length: 10 }, (_, i) => i + 1);

// 테스트 구성 - 극한 부하 테스트
export let options = {
  scenarios: {
    spike_test: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 100,
      stages: [
        { duration: '1m', target: 20 },  // 1분 동안 초당 요청을 5에서 20으로 증가
        { duration: '2m', target: 20 },  // 2분 동안 초당 20 요청을 유지
        { duration: '30s', target: 50 }, // 30초 동안 초당 요청을 20에서 50으로 급격히 증가 (스파이크)
        { duration: '1m', target: 50 },  // 1분 동안 초당 50 요청을 유지 (극한 부하)
        { duration: '3m', target: 10 },  // 3분 동안 초당 요청을 50에서 10으로 감소 (점진적 회복)
        { duration: '1m', target: 0 }    // 1분 동안 요청을 0으로 감소
      ]
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<5000', 'p(99)<7000'], // 95%의 요청이 5초 이내, 99%가 7초 이내
    'api_trend': ['p(95)<5000'],                      // API 응답 95%가 5초 이내
    'api_call_errors': ['rate<0.10'],                 // API 호출 오류 10% 미만 (고부하 상황 고려)
    http_req_failed: ['rate<0.10']                    // 실패 요청 10% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// API 테스트 함수
const apis = [
  // 1. 상품 목록 조회 API
  function testProductList() {
    const page = Math.floor(Math.random() * 5);
    const size = 20;
    
    let response = http.get(
      `${BASE_URL}/products?page=${page}&size=${size}`,
      { headers, tags: { name: 'ProductList' } }
    );
    
    check(response, {
      'Status 200': (r) => r.status === 200
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  },
  
  // 2. 인기 상품 조회 API
  function testPopularProducts() {
    let response = http.get(
      `${BASE_URL}/products/popular`,
      { headers, tags: { name: 'PopularProducts' } }
    );
    
    check(response, {
      'Status 200': (r) => r.status === 200
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  },
  
  // 3. 포인트 조회 API
  function testPointRetrieve() {
    const userId = users[Math.floor(Math.random() * users.length)];
    
    let response = http.get(
      `${BASE_URL}/users/${userId}/balance`,
      { headers, tags: { name: 'PointRetrieve' } }
    );
    
    check(response, {
      'Status 200': (r) => r.status === 200
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  },
  
  // 4. 포인트 충전 API
  function testPointCharge() {
    const userId = users[Math.floor(Math.random() * users.length)];
    const amount = randomIntBetween(1000, 50000);
    
    let response = http.post(
      `${BASE_URL}/users/${userId}/balance`,
      JSON.stringify({ amount: amount }),
      { headers, tags: { name: 'PointCharge' } }
    );
    
    check(response, {
      'Status 200': (r) => r.status === 200
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  },
  
  // 5. 쿠폰 목록 조회 API
  function testCouponList() {
    const userId = users[Math.floor(Math.random() * users.length)];
    
    let response = http.get(
      `${BASE_URL}/users/${userId}/coupons?page=0&size=10`,
      { headers, tags: { name: 'CouponList' } }
    );
    
    check(response, {
      'Status 200': (r) => r.status === 200
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  },
  
  // 6. 쿠폰 발급 API
  function testCouponIssue() {
    const userId = users[Math.floor(Math.random() * users.length)];
    const couponId = coupons[Math.floor(Math.random() * coupons.length)];
    
    let response = http.post(
      `${BASE_URL}/users/${userId}/coupons`,
      JSON.stringify({ couponId: couponId }),
      { headers, tags: { name: 'CouponIssue' } }
    );
    
    check(response, {
      'Status Valid': (r) => r.status === 200 || r.status === 400 // 중복 발급 가능성 고려
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  },
  
  // 7. 주문 생성 API
  function testOrderCreation() {
    const userId = users[Math.floor(Math.random() * users.length)];
    
    // 주문 항목 생성 (1-3개)
    const numItems = randomIntBetween(1, 3);
    let orderItems = [];
    
    for (let i = 0; i < numItems; i++) {
      const product = products[Math.floor(Math.random() * products.length)];
      orderItems.push({
        productId: product.id,
        variantId: product.variantId,
        quantity: randomIntBetween(1, 3)
      });
    }
    
    // 쿠폰 사용 여부 (30% 확률)
    const userCouponIds = Math.random() < 0.3 
      ? [coupons[Math.floor(Math.random() * coupons.length)]] 
      : [];
    
    const orderPayload = {
      userId: userId,
      items: orderItems,
      userCouponIds: userCouponIds
    };
    
    let response = http.post(
      `${BASE_URL}/orders`,
      JSON.stringify(orderPayload),
      { headers, tags: { name: 'OrderCreation' } }
    );
    
    check(response, {
      'Status 200': (r) => r.status === 200
    }) || apiErrors.add(1);
    
    apiTrend.add(response.timings.duration);
    return response;
  }
];

// 고부하 시나리오 테스트
export default function() {
  // 랜덤하게 API 선택 (가중치 적용)
  const rand = Math.random();
  
  // 읽기 작업에 더 높은 가중치 부여
  if (rand < 0.3) {
    // 상품 목록 조회 (30%)
    apis[0]();
  } else if (rand < 0.5) {
    // 인기 상품 조회 (20%)
    apis[1]();
  } else if (rand < 0.65) {
    // 포인트 조회 (15%)
    apis[2]();
  } else if (rand < 0.75) {
    // 쿠폰 목록 조회 (10%)
    apis[4]();
  } else if (rand < 0.85) {
    // 포인트 충전 (10%)
    apis[3]();
  } else if (rand < 0.95) {
    // 쿠폰 발급 (10%)
    apis[5]();
  } else {
    // 주문 생성 (5%) - 가장 부하가 큰 작업이므로 낮은 빈도
    apis[6]();
  }
  
  // VU간 간격을 조금씩 두어 동시에 요청하는 것을 방지
  sleep(Math.random() * 0.5);
}