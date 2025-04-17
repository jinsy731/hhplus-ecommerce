import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { SharedArray } from 'k6/data';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 모든 API에 대한 메트릭 정의
const apiTrends = {
  // 상품 API
  productList: new Trend('product_list_api'),
  productSearch: new Trend('product_search_api'),
  productPopular: new Trend('product_popular_api'),
  
  // 주문 API
  orderCreate: new Trend('order_create_api'),
  
  // 사용자 포인트 API
  pointRetrieve: new Trend('point_retrieve_api'),
  pointCharge: new Trend('point_charge_api'),
  
  // 쿠폰 API
  couponList: new Trend('coupon_list_api'),
  couponIssue: new Trend('coupon_issue_api')
};

// 에러 메트릭
const apiErrors = {
  productErrors: new Counter('product_api_errors'),
  orderErrors: new Counter('order_api_errors'),
  pointErrors: new Counter('point_api_errors'),
  couponErrors: new Counter('coupon_api_errors')
};

// 전체 에러율
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 데이터 (실제 환경에 맞게 수정 필요)
const users = new SharedArray('users', function() {
  return [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
});

const products = new SharedArray('products', function() {
  return [
    { id: 1, variantId: 1, price: 10000 },
    { id: 2, variantId: 1, price: 20000 },
    { id: 3, variantId: 2, price: 15000 },
    { id: 4, variantId: 2, price: 30000 },
    { id: 5, variantId: 3, price: 25000 }
  ];
});

const coupons = new SharedArray('coupons', function() {
  return [1, 2, 3, 4, 5];
});

const searchKeywords = ['phone', 'laptop', 'book', 'shirt', ''];

// 테스트 구성
export let options = {
  scenarios: {
    comprehensive_test: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },  // 점진적 증가
        { duration: '1m', target: 20 },   // 일정 부하
        { duration: '30s', target: 0 }    // 점진적 감소
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],    // 전체 요청의 95%가 2초 이내
    'product_list_api': ['p(95)<1000'],   // 상품 목록 API 95%가 1초 이내
    'product_popular_api': ['p(95)<1000'], // 인기 상품 API 95%가 1초 이내
    'order_create_api': ['p(95)<3000'],    // 주문 생성 API 95%가 3초 이내
    'point_retrieve_api': ['p(95)<800'],   // 포인트 조회 API 95%가 800ms 이내
    'point_charge_api': ['p(95)<1200'],    // 포인트 충전 API 95%가 1.2초 이내
    'coupon_list_api': ['p(95)<800'],      // 쿠폰 목록 API 95%가 800ms 이내
    'coupon_issue_api': ['p(95)<1200'],    // 쿠폰 발급 API 95%가 1.2초 이내
    'api_call_errors': ['rate<0.05']       // 전체 API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 공통 HTTP 응답 확인 함수
function checkResponse(res, expectedStatus, checkFn, errorMetric) {
  const success = check(res, {
    [`Status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
    'Valid response': checkFn
  });
  
  if (!success) {
    errorMetric.add(1);
    apiCallErrors.add(1);
  }
  
  return success;
}

// 상품 API 테스트
function testProductApis() {
  group('Product APIs', function() {
    // 1. 상품 목록 조회
    const productListRes = http.get(
      `${BASE_URL}/products?page=0&size=20&sort=id,desc`, 
      { headers }
    );
    
    checkResponse(
      productListRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && body.data.content && Array.isArray(body.data.content);
        } catch (e) {
          return false;
        }
      },
      apiErrors.productErrors
    );
    
    apiTrends.productList.add(productListRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 2. 상품 검색
    const keyword = randomItem(searchKeywords);
    const productSearchRes = http.get(
      `${BASE_URL}/products?page=0&size=20&sort=id,desc&keyword=${keyword}`, 
      { headers }
    );
    
    checkResponse(
      productSearchRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && typeof body.data.content !== 'undefined';
        } catch (e) {
          return false;
        }
      },
      apiErrors.productErrors
    );
    
    apiTrends.productSearch.add(productSearchRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 3. 인기 상품 조회
    const popularProductsRes = http.get(
      `${BASE_URL}/products/popular`, 
      { headers }
    );
    
    checkResponse(
      popularProductsRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && Array.isArray(body.data);
        } catch (e) {
          return false;
        }
      },
      apiErrors.productErrors
    );
    
    apiTrends.productPopular.add(popularProductsRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 주문 API 테스트
function testOrderApis() {
  group('Order APIs', function() {
    // 주문 생성
    const userId = randomItem(users);
    const numItems = randomIntBetween(1, 3);
    let orderItems = [];
    
    for (let i = 0; i < numItems; i++) {
      const product = randomItem(products);
      orderItems.push({
        productId: product.id,
        variantId: product.variantId,
        quantity: randomIntBetween(1, 3)
      });
    }
    
    let userCouponIds = [];
    if (Math.random() > 0.3) {
      const numCoupons = randomIntBetween(0, 2);
      for (let i = 0; i < numCoupons; i++) {
        const coupon = randomItem(coupons);
        if (!userCouponIds.includes(coupon)) {
          userCouponIds.push(coupon);
        }
      }
    }
    
    const totalAmount = orderItems.reduce((sum, item) => {
      const product = products.find(p => p.id === item.productId);
      return sum + (product ? product.price * item.quantity : 10000 * item.quantity);
    }, 0);
    
    const orderPayload = {
      userId: userId,
      items: orderItems,
      userCouponIds: userCouponIds,
      payMethods: [{
        method: "POINT",
        amount: totalAmount
      }]
    };
    
    const createOrderRes = http.post(
      `${BASE_URL}/orders`, 
      JSON.stringify(orderPayload), 
      { headers }
    );
    
    checkResponse(
      createOrderRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && body.data.orderId;
        } catch (e) {
          return false;
        }
      },
      apiErrors.orderErrors
    );
    
    apiTrends.orderCreate.add(createOrderRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 사용자 포인트 API 테스트
function testUserPointApis() {
  group('User Point APIs', function() {
    const userId = randomItem(users);
    
    // 1. 포인트 조회
    const pointRetrieveRes = http.get(
      `${BASE_URL}/users/${userId}/balance`, 
      { headers }
    );
    
    checkResponse(
      pointRetrieveRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && typeof body.data.point !== 'undefined';
        } catch (e) {
          return false;
        }
      },
      apiErrors.pointErrors
    );
    
    apiTrends.pointRetrieve.add(pointRetrieveRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 2. 포인트 충전
    const chargeAmount = randomIntBetween(1000, 50000);
    
    const chargePayload = {
      amount: chargeAmount
    };
    
    const pointChargeRes = http.post(
      `${BASE_URL}/users/${userId}/balance`, 
      JSON.stringify(chargePayload), 
      { headers }
    );
    
    checkResponse(
      pointChargeRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && typeof body.data.point !== 'undefined';
        } catch (e) {
          return false;
        }
      },
      apiErrors.pointErrors
    );
    
    apiTrends.pointCharge.add(pointChargeRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 쿠폰 API 테스트
function testCouponApis() {
  group('Coupon APIs', function() {
    const userId = randomItem(users);
    
    // 1. 쿠폰 목록 조회
    const couponListRes = http.get(
      `${BASE_URL}/users/${userId}/coupons?page=0&size=10&sort=id,desc`, 
      { headers }
    );
    
    checkResponse(
      couponListRes, 
      200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.data && typeof body.data.content !== 'undefined';
        } catch (e) {
          return false;
        }
      },
      apiErrors.couponErrors
    );
    
    apiTrends.couponList.add(couponListRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
    
    // 2. 쿠폰 발급
    const couponId = randomItem(coupons);
    
    const issuePayload = {
      couponId: couponId
    };
    
    const couponIssueRes = http.post(
      `${BASE_URL}/users/${userId}/coupons`, 
      JSON.stringify(issuePayload), 
      { headers }
    );
    
    // 중복 발급 등의 이유로 400 응답도
    const validStatus = (couponIssueRes.status === 200 || couponIssueRes.status === 400);
    checkResponse(
      couponIssueRes, 
      validStatus ? couponIssueRes.status : 200, 
      (r) => {
        try {
          const body = JSON.parse(r.body);
          return (couponIssueRes.status === 200 && body.data) || 
                 (couponIssueRes.status === 400 && body.message);
        } catch (e) {
          return false;
        }
      },
      apiErrors.couponErrors
    );
    
    apiTrends.couponIssue.add(couponIssueRes.timings.duration);
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 기본 테스트 함수 (랜덤 시나리오 선택)
export default function() {
  const scenario = Math.floor(Math.random() * 4);
  
  switch(scenario) {
    case 0:
      testProductApis();
      break;
    case 1:
      testOrderApis();
      break;
    case 2:
      testUserPointApis();
      break;
    case 3:
      testCouponApis();
      break;
  }
}

// 모든 API 테스트 종합 실행
export function comprehensive() {
  testProductApis();
  testUserPointApis();
  testCouponApis();
  testOrderApis();
}

// 개별 API 그룹 테스트 함수 (선택적 실행용)
export function products() {
  testProductApis();
}

export function orders() {
  testOrderApis();
}

export function points() {
  testUserPointApis();
}

export function coupons() {
  testCouponApis();
}
