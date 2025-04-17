import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const orderCreationTrend = new Trend('order_creation_trend');
const orderCreationErrors = new Counter('order_creation_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 데이터 (실제 환경에 맞게 수정 필요)
const users = [1, 2, 3, 4, 5];
const products = [
  { id: 1, variantId: 1 },
  { id: 2, variantId: 1 },
  { id: 3, variantId: 2 },
  { id: 4, variantId: 2 },
  { id: 5, variantId: 3 }
];
const coupons = [1, 2, 3, 4, 5];

// 테스트 구성
export let options = {
  scenarios: {
    order_creation: {
      executor: 'ramping-vus',
      startVUs: 2,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95%의 요청이 3초 이내에 완료되어야 함
    'order_creation_trend': ['p(95)<2500'],  // 주문 생성 95%가 2.5초 이내
    'api_call_errors': ['rate<0.05']      // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 주문 생성 테스트
export default function() {
  // 1. 사용자 선택
  const userId = users[Math.floor(Math.random() * users.length)];
  
  // 2. 주문할 상품 선택 (1-3개)
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
  
  // 3. 쿠폰 선택 (0-2개)
  let userCouponIds = [];
  const useCoupon = Math.random() > 0.3; // 70% 확률로 쿠폰 사용
  
  if (useCoupon) {
    const numCoupons = randomIntBetween(1, 2);
    for (let i = 0; i < numCoupons; i++) {
      const couponId = coupons[Math.floor(Math.random() * coupons.length)];
      if (!userCouponIds.includes(couponId)) {
        userCouponIds.push(couponId);
      }
    }
  }
  
  // 4. 결제 방법 설정
  const totalAmount = orderItems.reduce((sum, item) => sum + item.quantity * 10000, 0); // 임의의 금액 계산
  
  const payMethods = [{
    method: "POINT",
    amount: totalAmount
  }];
  
  // 5. 주문 생성 요청
  const orderPayload = {
    userId: userId,
    items: orderItems,
    userCouponIds: userCouponIds,
    payMethods: payMethods
  };
  
  let orderResponse = http.post(
    `${BASE_URL}/orders`, 
    JSON.stringify(orderPayload), 
    { headers }
  );
  
  check(orderResponse, {
    'Order Creation - Status 200': (r) => r.status === 200,
    'Order Creation - Has Order ID': (r) => {
      try {
        let body = JSON.parse(r.body);
        return body.data && body.data.orderId;
      } catch (e) {
        return false;
      }
    }
  }) || orderCreationErrors.add(1);
  
  orderCreationTrend.add(orderResponse.timings.duration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
