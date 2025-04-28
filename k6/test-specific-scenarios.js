import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const scenarioTrend = new Trend('scenario_trend');
const scenarioErrors = new Counter('scenario_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 데이터
const users = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
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
    complete_purchase_flow: {
      executor: 'ramping-vus',
      startVUs: 3,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '2m', target: 10 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95%의 요청이 3초 이내에 완료되어야 함
    'scenario_trend': ['p(95)<5000'],  // 전체 시나리오 95%가 5초 이내
    'api_call_errors': ['rate<0.05']   // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 완전한 구매 시나리오 (포인트 충전 -> 상품 조회 -> 쿠폰 발급 -> 주문 생성)
export default function() {
  const scenarioStartTime = new Date().getTime();
  
  // 1. 사용자 선택
  const userId = users[Math.floor(Math.random() * users.length)];
  
  // 2. 포인트 조회
  let retrieveResponse = http.get(
    `${BASE_URL}/users/${userId}/balance`, 
    { headers }
  );
  
  const pointCheck = check(retrieveResponse, {
    'User Point Retrieve - Status 200': (r) => r.status === 200,
    'User Point Retrieve - Has Point': (r) => {
      try {
        let body = JSON.parse(r.body);
        return body.data && typeof body.data.point !== 'undefined';
      } catch (e) {
        return false;
      }
    }
  });
  
  if (!pointCheck) {
    apiCallErrors.add(1);
    return;
  }
  
  let currentPoint = 0;
  try {
    const body = JSON.parse(retrieveResponse.body);
    currentPoint = body.data.point;
  } catch (e) {
    // 포인트 값을 얻지 못한 경우 기본값 사용
  }
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  
  // 3. 포인트 충전 (잔액이 50,000 미만인 경우)
  if (currentPoint < 50000) {
    const chargeAmount = randomIntBetween(50000, 100000);
    
    const chargePayload = {
      amount: chargeAmount
    };
    
    let chargeResponse = http.post(
      `${BASE_URL}/users/${userId}/balance`, 
      JSON.stringify(chargePayload), 
      { headers }
    );
    
    check(chargeResponse, {
      'User Point Charge - Status 200': (r) => r.status === 200,
      'User Point Charge - Updated Point': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && typeof body.data.point !== 'undefined';
        } catch (e) {
          return false;
        }
      }
    }) || apiCallErrors.add(1);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  }
  
  // 4. 인기 상품 조회
  let popularResponse = http.get(
    `${BASE_URL}/products/popular`,
    { headers }
  );
  
  const productCheck = check(popularResponse, {
    'Popular Products - Status 200': (r) => r.status === 200,
    'Popular Products - Has Data': (r) => {
      try {
        let body = JSON.parse(r.body);
        return body.data && Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    }
  });
  
  if (!productCheck) {
    apiCallErrors.add(1);
    return;
  }
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  
  // 5. 쿠폰 목록 조회
  let couponListResponse = http.get(
    `${BASE_URL}/users/${userId}/coupons?page=0&size=10`, 
    { headers }
  );
  
  check(couponListResponse, {
    'User Coupon List - Status 200': (r) => r.status === 200
  }) || apiCallErrors.add(1);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  
  // 6. 쿠폰 발급 (50% 확률)
  let userCouponIds = [];
  
  if (Math.random() > 0.5) {
    const couponId = coupons[Math.floor(Math.random() * coupons.length)];
    
    const issuePayload = {
      couponId: couponId
    };
    
    let issueResponse = http.post(
      `${BASE_URL}/users/${userId}/coupons`, 
      JSON.stringify(issuePayload), 
      { headers }
    );
    
    const couponCheck = check(issueResponse, {
      'User Coupon Issue - Status': (r) => r.status === 200 || r.status === 400 // 중복 발급 가능성 고려
    });
    
    if (couponCheck && issueResponse.status === 200) {
      try {
        const body = JSON.parse(issueResponse.body);
        if (body.data && body.data.userCouponId) {
          userCouponIds.push(body.data.userCouponId);
        }
      } catch (e) {
        // 쿠폰 ID를 얻지 못한 경우 무시
      }
    }
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  }
  
  // 7. 주문할 상품 선택 (1-3개)
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
  
  // 8. 주문 생성 요청
  const orderPayload = {
    userId: userId,
    items: orderItems,
    userCouponIds: userCouponIds
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
  }) || scenarioErrors.add(1);
  
  // 시나리오 종료 시간 및 총 소요 시간 측정
  const scenarioEndTime = new Date().getTime();
  const scenarioDuration = scenarioEndTime - scenarioStartTime;
  
  // 총 시나리오 소요 시간 기록
  scenarioTrend.add(scenarioDuration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}