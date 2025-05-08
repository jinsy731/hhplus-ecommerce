import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const orderCreationTrend = new Trend('order_creation_trend');
const orderCreationP99 = new Trend('order_creation_p99');
const orderCreationP95 = new Trend('order_creation_p95');
const orderCreationP50 = new Trend('order_creation_p50');
const orderCreationErrors = new Counter('order_creation_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 2;

// 상품 목록 준비
const products = [];
for (let i = 1; i <= 300000; i++) {
  products.push({
    id: Math.floor((i - 1) / 3) + 1,
    variantId: i
  });
}

// 사용자: 1~100000
let users = Array.from({ length: 100000 }, (_, i) => i + 1);

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 테스트 구성
export let options = {
  scenarios: {
    sparse_order_load: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    'http_req_duration': ['p(99)<5000', 'p(95)<3000', 'p(50)<1500'],
    'order_creation_trend': ['p(99)<5000', 'p(95)<3000', 'p(50)<1500'],
    'api_call_errors': ['rate<0.05']
  }
};

// 주문 생성 테스트
export default function() {
  if (users.length === 0) {
    console.log('No more users available!');
    return;
  }

  // 사용자 선택 (랜덤으로 뽑고 제거)
  const randomIndex = Math.floor(Math.random() * users.length);
  const userId = users[randomIndex];
  users.splice(randomIndex, 1);

  // 사용자 쿠폰 조회 (성능 측정 제외)
  let userCouponIds = [];
  try {
    const couponResponse = http.get(`${BASE_URL}/users/${userId}/coupons`, { headers });
    if (couponResponse.status === 200) {
      const body = JSON.parse(couponResponse.body);
      const coupons = body.data?.coupons || [];

      // UNUSED 쿠폰만 필터
      const availableCoupons = coupons.filter(coupon => coupon.status === 'UNUSED');

      // 70% 확률로 쿠폰 사용
      const useCoupon = Math.random() > 0.3;

      if (useCoupon && availableCoupons.length > 0) {
        const numCoupons = Math.min(randomIntBetween(1, 2), availableCoupons.length);
        for (let i = 0; i < numCoupons; i++) {
          const randomCoupon = availableCoupons.splice(Math.floor(Math.random() * availableCoupons.length), 1)[0];
          userCouponIds.push(randomCoupon.id); // 유저 쿠폰 ID 사용
        }
      }
    } else {
      console.log(`Failed to retrieve coupons for user ${userId}`);
    }
  } catch (error) {
    console.log(`Error retrieving coupons for user ${userId}: ${error}`);
  }

  // 주문할 상품 구성 (1~3개 랜덤)
  const numItems = randomIntBetween(1, 3);
  const selectedProducts = new Set();
  const orderItems = [];

  while (orderItems.length < numItems) {
    const product = products[Math.floor(Math.random() * products.length)];
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

  // 주문 요청
  const orderPayload = {
    userId: userId,
    items: orderItems,
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
    console.log(`Error creating order: ${orderResponse.status} - ${orderResponse.body}`);
  }

  // 메트릭 기록
  orderCreationTrend.add(duration);
  orderCreationP99.add(duration);
  orderCreationP95.add(duration);
  orderCreationP50.add(duration);

  // 사용자 행동 시뮬레이션을 위한 대기
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
