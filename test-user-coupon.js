import http from 'k6/http';
import { sleep, check } from 'k6';
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

// 테스트 데이터 (실제 환경에 맞게 수정 필요)
const users = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
const coupons = [1, 2, 3, 4, 5];

// 테스트 구성
export let options = {
  scenarios: {
    coupon_operations: {
      executor: 'ramping-vus',
      startVUs: 3,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<1500'], // 95%의 요청이 1.5초 이내에 완료되어야 함
    'coupon_retrieve_trend': ['p(95)<800'],  // 쿠폰 목록 조회 95%가 800ms 이내
    'coupon_issue_trend': ['p(95)<1200'],    // 쿠폰 발급 95%가 1.2초 이내
    'api_call_errors': ['rate<0.05']         // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 쿠폰 발급 및 조회 테스트
export default function() {
  // 1. 사용자 선택
  const userId = users[Math.floor(Math.random() * users.length)];
  
  // 2. 쿠폰 목록 조회
  let couponListResponse = http.get(
    `${BASE_URL}/users/${userId}/coupons?page=0&size=10&sort=id,desc`, 
    { headers }
  );
  
  check(couponListResponse, {
    'Coupon List - Status 200': (r) => r.status === 200,
    'Coupon List - Valid Response': (r) => {
      try {
        let body = JSON.parse(r.body);
        return body.data && typeof body.data.content !== 'undefined';
      } catch (e) {
        return false;
      }
    }
  }) || apiCallErrors.add(1);
  
  couponRetrieveTrend.add(couponListResponse.timings.duration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  
  // 3. 쿠폰 발급
  const couponId = coupons[Math.floor(Math.random() * coupons.length)];
  
  const issuePayload = {
    couponId: couponId
  };
  
  let issueResponse = http.post(
    `${BASE_URL}/users/${userId}/coupons`, 
    JSON.stringify(issuePayload), 
    { headers }
  );
  
  // 성공 또는 이미 발급된 쿠폰 (400 - 중복 발급) 케이스 모두 처리
  check(issueResponse, {
    'Coupon Issue - Success or Already Issued': (r) => r.status === 200 || r.status === 400,
    'Coupon Issue - Valid Response': (r) => {
      try {
        if (r.status === 200) {
          let body = JSON.parse(r.body);
          return body.data && body.data.couponId;
        }
        return true; // 400인 경우도 유효한 응답으로 처리
      } catch (e) {
        return false;
      }
    }
  }) || couponIssueErrors.add(1);
  
  couponIssueTrend.add(issueResponse.timings.duration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
