import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
// 1. 상품 API 메트릭
const productListTrend = new Trend('product_list_trend');
const productListNoOffsetTrend = new Trend('product_list_no_offset_trend');
const productPopularTrend = new Trend('product_popular_trend');
const productErrors = new Rate('product_errors');

// 2. 주문 API 메트릭
const orderCreationTrend = new Trend('order_creation_trend');
const orderCreationErrors = new Counter('order_creation_errors');

// 3. 유저 포인트 API 메트릭
const pointRetrieveTrend = new Trend('point_retrieve_trend');
const pointChargeTrend = new Trend('point_charge_trend');
const pointChargeErrors = new Counter('point_charge_errors');

// 4. 쿠폰 API 메트릭
const couponListTrend = new Trend('coupon_list_trend');
const couponIssueTrend = new Trend('coupon_issue_trend');
const couponIssueErrors = new Counter('coupon_issue_errors');

// 전체 API 오류율
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
const keywords = ["의류", "전자", "가구", "식품", "도서", ""];

// 테스트 구성
export let options = {
  scenarios: {
    // 시나리오 1: 상품 검색 및 조회
    products_scenario: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'productsTest'
    },
    
    // 시나리오 2: 주문 관련 작업
    orders_scenario: {
      executor: 'ramping-vus',
      startVUs: 3,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'ordersTest'
    },
    
    // 시나리오 3: 포인트 충전 및 조회
    points_scenario: {
      executor: 'ramping-vus',
      startVUs: 3,
      stages: [
        { duration: '30s', target: 15 },
        { duration: '1m', target: 15 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'pointsTest'
    },
    
    // 시나리오 4: 쿠폰 발급 및 조회
    coupons_scenario: {
      executor: 'ramping-vus',
      startVUs: 3,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'couponsTest'
    }
  },
  thresholds: {
    // 공통 SLO/SLA
    http_req_duration: ['p(95)<2000'], // 95%의 모든 요청이 2초 이내에 완료되어야 함
    'api_call_errors': ['rate<0.05'],  // 전체 API 호출 오류 5% 미만
    
    // 상품 API SLO/SLA
    'product_list_trend': ['p(95)<1000'],  // 상품 목록 95%가 1초 이내
    'product_list_no_offset_trend': ['p(95)<500'],  // No-Offset 페이지네이션 95%가 500ms 이내
    'product_popular_trend': ['p(95)<700'],  // 인기 상품 95%가 700ms 이내
    
    // 주문 API SLO/SLA
    'order_creation_trend': ['p(95)<2500'],  // 주문 생성 95%가 2.5초 이내
    
    // 포인트 API SLO/SLA
    'point_retrieve_trend': ['p(95)<700'],   // 포인트 조회 95%가 700ms 이내
    'point_charge_trend': ['p(95)<1200'],    // 포인트 충전 95%가 1.2초 이내
    
    // 쿠폰 API SLO/SLA
    'coupon_list_trend': ['p(95)<800'],      // 쿠폰 목록 95%가 800ms 이내
    'coupon_issue_trend': ['p(95)<1200']     // 쿠폰 발급 95%가 1.2초 이내
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 1. 상품 API 테스트 함수
export function productsTest() {
  group('Products API Tests', () => {
    // 1.1 상품 목록 조회 (페이징)
    const page = Math.floor(Math.random() * 3);
    const size = 20;
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];
    
    let listResponse = http.get(
      `${BASE_URL}/products?page=${page}&size=${size}&keyword=${keyword}`,
      { headers }
    );
    
    check(listResponse, {
      'Product List - Status 200': (r) => r.status === 200,
      'Product List - Has Data': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && Array.isArray(body.data.content);
        } catch (e) {
          return false;
        }
      }
    }) || productErrors.add(1);
    
    productListTrend.add(listResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 1.2 상품 목록 조회 (No-Offset 페이징)
    const lastId = Math.floor(Math.random() * 100);
    
    let noOffsetResponse = http.get(
      `${BASE_URL}/products?size=${size}&lastId=${lastId}&keyword=${keyword}`,
      { headers }
    );
    
    check(noOffsetResponse, {
      'Product List No-Offset - Status 200': (r) => r.status === 200,
      'Product List No-Offset - Has Data': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && Array.isArray(body.data.content);
        } catch (e) {
          return false;
        }
      }
    }) || productErrors.add(1);
    
    productListNoOffsetTrend.add(noOffsetResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 1.3 인기 상품 조회
    let popularResponse = http.get(
      `${BASE_URL}/products/popular`,
      { headers }
    );
    
    check(popularResponse, {
      'Popular Products - Status 200': (r) => r.status === 200,
      'Popular Products - Has Data': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && Array.isArray(body.data);
        } catch (e) {
          return false;
        }
      }
    }) || productErrors.add(1);
    
    productPopularTrend.add(popularResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 2. 주문 API 테스트 함수
export function ordersTest() {
  group('Orders API Tests', () => {
    // 사용자 선택
    const userId = users[Math.floor(Math.random() * users.length)];
    
    // 주문할 상품 선택 (1-3개)
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
    
    // 쿠폰 선택 (0-2개)
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
    
    // 주문 생성 요청
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
    }) || orderCreationErrors.add(1);
    
    orderCreationTrend.add(orderResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 3. 유저 포인트 API 테스트 함수
export function pointsTest() {
  group('User Points API Tests', () => {
    // 사용자 선택
    const userId = users[Math.floor(Math.random() * users.length)];
    
    // 포인트 조회
    let retrieveResponse = http.get(
      `${BASE_URL}/users/${userId}/balance`, 
      { headers }
    );
    
    check(retrieveResponse, {
      'User Point Retrieve - Status 200': (r) => r.status === 200,
      'User Point Retrieve - Has Point': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && typeof body.data.point !== 'undefined';
        } catch (e) {
          return false;
        }
      }
    }) || apiCallErrors.add(1);
    
    pointRetrieveTrend.add(retrieveResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 포인트 충전
    const chargeAmount = randomIntBetween(1000, 100000);
    
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
    }) || pointChargeErrors.add(1);
    
    pointChargeTrend.add(chargeResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 4. 쿠폰 API 테스트 함수
export function couponsTest() {
  group('User Coupons API Tests', () => {
    // 사용자 선택
    const userId = users[Math.floor(Math.random() * users.length)];
    
    // 쿠폰 목록 조회
    const page = Math.floor(Math.random() * 3);
    const size = 10;
    
    let listResponse = http.get(
      `${BASE_URL}/users/${userId}/coupons?page=${page}&size=${size}`, 
      { headers }
    );
    
    check(listResponse, {
      'User Coupon List - Status 200': (r) => r.status === 200,
      'User Coupon List - Has Data': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && body.data.content;
        } catch (e) {
          return false;
        }
      }
    }) || apiCallErrors.add(1);
    
    couponListTrend.add(listResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 쿠폰 발급
    const couponId = coupons[Math.floor(Math.random() * coupons.length)];
    
    const issuePayload = {
      couponId: couponId
    };
    
    let issueResponse = http.post(
      `${BASE_URL}/users/${userId}/coupons`, 
      JSON.stringify(issuePayload), 
      { headers }
    );
    
    check(issueResponse, {
      'User Coupon Issue - Status 200 or 400': (r) => r.status === 200 || r.status === 400, // 중복 발급 가능성 고려
      'User Coupon Issue - Response OK': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data || body.error; // 정상 응답 또는 오류 메시지 확인
        } catch (e) {
          return false;
        }
      }
    }) || couponIssueErrors.add(1);
    
    couponIssueTrend.add(issueResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 종합 시나리오 테스트 (기본 exported 함수)
export default function() {
  // 실행할 함수 결정 (환경 변수로 지정 가능)
  const functionToRun = __ENV.FUNCTION || 'default';
  
  switch(functionToRun) {
    case 'products':
      productsTest();
      break;
    case 'orders':
      ordersTest();
      break;
    case 'points':
      pointsTest();
      break;
    case 'coupons':
      couponsTest();
      break;
    case 'comprehensive':
      comprehensiveTest();
      break;
    default:
      // 랜덤하게 테스트 선택
      const rand = Math.random();
      if (rand < 0.25) {
        productsTest();
      } else if (rand < 0.5) {
        ordersTest();
      } else if (rand < 0.75) {
        pointsTest();
      } else {
        couponsTest();
      }
  }
}

// 종합 시나리오 테스트 (모든 API 순차적 호출)
export function comprehensiveTest() {
  // 1. 유저 포인트 확인 및 충전
  pointsTest();
  
  // 2. 상품 목록 및 인기 상품 조회
  productsTest();
  
  // 3. 쿠폰 조회 및 발급
  couponsTest();
  
  // 4. 주문 생성
  ordersTest();
}
