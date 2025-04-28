import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const couponRetrieveTrend = new Trend('coupon_retrieve_trend');
const couponIssueTrend = new Trend('coupon_issue_trend');
const couponIssueErrors = new Counter('coupon_issue_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 데이터
const userIdRange = { min: 1, max: 100000 }; // 사용자 ID 범위
const coupons = [1, 2, 3, 4, 5]; // 테스트할 쿠폰 ID 목록
const paginationOptions = [
  { page: 0, size: 10, sort: 'expiredAt,desc' },
  { page: 0, size: 20, sort: 'expiredAt,desc' },
  { page: 0, size: 10, sort: 'id,desc' },
  { page: 1, size: 10, sort: 'id,desc' }
];

// 테스트 구성
export let options = {
  scenarios: {
    // 시나리오 1: 일반적인 쿠폰 조회 (읽기 작업 위주)
    normal_coupon_retrieval: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 50 },
        { duration: '2m', target: 50 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'retrievalScenario'
    },
    
    // 시나리오 2: 쿠폰 발급 (쓰기 작업 위주)
    coupon_issuance: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },
        { duration: '2m', target: 30 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'issuanceScenario'
    },
    
    // 시나리오 3: 고부하 혼합 시나리오 (읽기 + 쓰기)
    mixed_high_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s',
      exec: 'mixedScenario'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 이내에 완료되어야 함
    'coupon_retrieve_trend': ['p(95)<1000'],  // 쿠폰 목록 조회 95%가 1초 이내
    'coupon_issue_trend': ['p(95)<1500'],    // 쿠폰 발급 95%가 1.5초 이내
    'api_call_errors': ['rate<0.05']         // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 헬퍼 함수들
function getRandomUserId() {
  return randomIntBetween(userIdRange.min, userIdRange.max);
}

function getRandomCouponId() {
  return coupons[Math.floor(Math.random() * coupons.length)];
}

function getRandomPaginationOption() {
  return paginationOptions[Math.floor(Math.random() * paginationOptions.length)];
}

// 쿠폰 조회 시나리오
export function retrievalScenario() {
  group('Coupon Retrieval Operations', () => {
    const userId = getRandomUserId();
    const pagination = getRandomPaginationOption();
    
    let couponListResponse = http.get(
      `${BASE_URL}/users/${userId}/coupons?page=${pagination.page}&size=${pagination.size}&sort=${pagination.sort}`, 
      { headers }
    );
    
    check(couponListResponse, {
      'Coupon List - Status 200': (r) => r.status === 200,
      'Coupon List - Valid Response': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data && Array.isArray(body.data.coupons) && body.data.pageInfo;
        } catch (e) {
          return false;
        }
      }
    }) || apiCallErrors.add(1);
    
    couponRetrieveTrend.add(couponListResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 쿠폰 발급 시나리오
export function issuanceScenario() {
  group('Coupon Issuance Operations', () => {
    const userId = getRandomUserId();
    const couponId = getRandomCouponId();
    
    const issuePayload = {
      couponId: couponId
    };
    
    let issueResponse = http.post(
      `${BASE_URL}/users/${userId}/coupons`, 
      JSON.stringify(issuePayload), 
      { headers }
    );
    
    check(issueResponse, {
      'Coupon Issue - Success or Already Issued': (r) => r.status === 200 || r.status === 400,
      'Coupon Issue - Valid Response': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.data || body.errorCode;
        } catch (e) {
          return false;
        }
      }
    }) || apiCallErrors.add(1);
    
    if (issueResponse.status === 400) {
      couponIssueErrors.add(1);
    }
    
    couponIssueTrend.add(issueResponse.timings.duration);
    
    sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  });
}

// 혼합 시나리오
export function mixedScenario() {
  const userId = getRandomUserId();
  
  // 75% 확률로 조회, 25% 확률로 발급
  if (Math.random() < 0.75) {
    group('Mixed - Coupon Retrieval', () => {
      const pagination = getRandomPaginationOption();
      
      let couponListResponse = http.get(
        `${BASE_URL}/users/${userId}/coupons?page=${pagination.page}&size=${pagination.size}&sort=${pagination.sort}`, 
        { headers }
      );
      
      check(couponListResponse, {
        'Mixed Retrieval - Status 200': (r) => r.status === 200
      }) || apiCallErrors.add(1);
      
      couponRetrieveTrend.add(couponListResponse.timings.duration);
    });
  } else {
    group('Mixed - Coupon Issuance', () => {
      const couponId = getRandomCouponId();
      
      const issuePayload = {
        couponId: couponId
      };
      
      let issueResponse = http.post(
        `${BASE_URL}/users/${userId}/coupons`, 
        JSON.stringify(issuePayload), 
        { headers }
      );
      
      check(issueResponse, {
        'Mixed Issuance - Success or Already Issued': (r) => r.status === 200 || r.status === 400
      }) || apiCallErrors.add(1);
      
      if (issueResponse.status === 400) {
        couponIssueErrors.add(1);
      }
      
      couponIssueTrend.add(issueResponse.timings.duration);
    });
  }
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}

// 기본 시나리오 (개별 실행용)
export default function() {
  // 혼합 시나리오 실행
  mixedScenario();
}