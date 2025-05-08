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
const lockContentionRate = new Rate('lock_contention_rate');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 0.5;
const THINK_TIME_MAX = 1.5;

// 테스트 시나리오: 소수의 인기 상품에 집중된 주문 요청
// 소수의 상품(10개)에 대해 집중적으로 주문 생성
const hotProducts = [
  { id: 1, variantId: 3 },
  { id: 2, variantId: 6 },
  { id: 3, variantId: 9 },
  { id: 4, variantId: 12 },
  { id: 5, variantId: 15 },
  { id: 6, variantId: 18 },
  { id: 7, variantId: 21 },
  { id: 8, variantId: 24 },
  { id: 9, variantId: 27 },
  { id: 10, variantId: 30 }
];

// 매우 인기있는 상품(DB lock 경합 가능성이 높은 상품)
const superHotProducts = [
  { id: 1, variantId: 3 },
  { id: 2, variantId: 6 },
  { id: 3, variantId: 9 }
];

// 테스트용 사용자와 쿠폰 데이터
const users = Array.from({ length: 100000 }, (_, i) => i + 1);  // 50명의 사용자
const coupons = Array.from({ length: 10 }, (_, i) => i + 1); // 10개의 쿠폰

// 테스트 구성
export let options = {
  scenarios: {
    concentrated_order_load: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 30 },  // 서서히 부하 증가
        { duration: '1m', target: 30 },   // 1분간 부하 유지
        { duration: '30s', target: 70 },  // 부하 증가
        { duration: '1m', target: 70 },   // 1분간 높은 부하 유지
        { duration: '30s', target: 0 }    // 서서히 부하 감소
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    'http_req_duration': ['p(99)<6000', 'p(95)<4000', 'p(50)<2000'], // 응답 시간 임계값
    'order_creation_trend': ['p(99)<6000', 'p(95)<4000', 'p(50)<2000'], // 주문 생성 응답 시간
    'api_call_errors': ['rate<0.08']  // API 호출 오류 8% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 주문 생성 테스트
export default function() {
  // 1. 사용자 선택 (랜덤으로 뽑고 삭제)
    if (users.length === 0) {
        console.log('No more users available!');
        return;
    }
    const randomIndex = Math.floor(Math.random() * users.length);
    const userId = users[randomIndex];
    users.splice(randomIndex, 1);

    // 2. 사용자 쿠폰 조회 (성능 측정 제외)
      let userCouponIds = [];
      try {
        const couponResponse = http.get(`${BASE_URL}/users/${userId}/coupons`, { headers });
        if (couponResponse.status === 200) {
          const body = JSON.parse(couponResponse.body);
          const coupons = body.data?.coupons || [];

          // UNUSED 쿠폰만 필터링
          const availableCoupons = coupons.filter(coupon => coupon.status === 'UNUSED');

          if (availableCoupons.length > 0) {
            const numCoupons = Math.min(randomIntBetween(1, 2), availableCoupons.length);
            for (let i = 0; i < numCoupons; i++) {
              const randomCoupon = availableCoupons.splice(Math.floor(Math.random() * availableCoupons.length), 1)[0];
              userCouponIds.push(randomCoupon.id);  // 여기서는 유저 쿠폰 ID (userCouponId)를 써야 함
            }
          }
        } else {
          console.log(`Failed to retrieve coupons for user ${userId}`);
        }
      } catch (error) {
        console.log(`Error retrieving coupons for user ${userId}: ${error}`);
      }
    // 3. 주문할 상품 구성
    const numItems = randomIntBetween(1, 3);
    const selectedProducts = new Set();
    const orderItems = [];
    const useSuperHotProduct = Math.random() <= 0.8;
    const productPool = useSuperHotProduct ? superHotProducts : hotProducts;

    while (orderItems.length < numItems) {
      const product = productPool[Math.floor(Math.random() * productPool.length)];
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
  }
  // 4. 주문 생성 (여기서만 성능 측정)
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

      if (orderResponse.status === 409) {
        lockContentionRate.add(1);
        console.log(`Lock contention detected: ${orderResponse.body}`);
      } else {
        console.log(`Error creating order: ${orderResponse.status} - ${orderResponse.body}`);
      }
    } else {
      lockContentionRate.add(0);
    }

    orderCreationTrend.add(duration);
    orderCreationP99.add(duration);
    orderCreationP95.add(duration);
    orderCreationP50.add(duration);

    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
