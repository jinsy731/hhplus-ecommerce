import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const couponRetrieveTrend = new Trend('coupon_retrieve_trend');
const apiCallErrors = new Rate('api_call_errors');
const invalidResponses = new Counter('invalid_responses');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 데이터 (실제 환경에 맞게 수정 필요)
const userIdRange = { min: 1, max: 100000 }; // 사용자 ID 범위
const paginationOptions = [
  { page: 0, size: 10, sort: 'expiredAt,desc' },
  { page: 0, size: 20, sort: 'expiredAt,desc' },
  { page: 0, size: 10, sort: 'id,desc' },
  { page: 1, size: 10, sort: 'id,desc' }
];

// 테스트 구성
export let options = {
  stages: [
    { duration: '30s', target: 30 },  // 점진적으로 30명의 사용자로 증가
    { duration: '1m', target: 80 },   // 1분 동안 80명으로 증가
    { duration: '2m', target: 80 },   // 2분 동안 80명 유지
    { duration: '30s', target: 0 }    // 점진적으로 감소
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초 이내에 완료되어야 함
    'coupon_retrieve_trend': ['p(95)<800'],  // 95%가 800ms 이내
    'api_call_errors': ['rate<0.03']         // API 호출 오류 3% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 랜덤 사용자 ID 생성
function getRandomUserId() {
  return randomIntBetween(userIdRange.min, userIdRange.max);
}

// 페이지네이션 옵션 랜덤 선택
function getRandomPaginationOption() {
  return paginationOptions[Math.floor(Math.random() * paginationOptions.length)];
}

// 쿠폰 목록 조회 테스트
export default function() {
  // 1. 사용자 선택
  const userId = getRandomUserId();
  
  // 2. 페이지네이션 옵션 선택
  const pagination = getRandomPaginationOption();
  
  // 3. 쿠폰 목록 조회
  let couponListResponse = http.get(
    `${BASE_URL}/users/${userId}/coupons?page=${pagination.page}&size=${pagination.size}&sort=${pagination.sort}`, 
    { headers }
  );
  
  // 4. 응답 검증
  check(couponListResponse, {
    'Coupon List - Status 200': (r) => r.status === 200,
    'Coupon List - Valid Response': (r) => {
      try {
        let body = JSON.parse(r.body);
        return body.data && Array.isArray(body.data.coupons) && body.data.pageInfo;
      } catch (e) {
        invalidResponses.add(1);
        return false;
      }
    }
  }) || apiCallErrors.add(1);
  
  // 5. 응답 시간 측정
  couponRetrieveTrend.add(couponListResponse.timings.duration);
  
  // 6. 사용자 "생각 시간" 시뮬레이션
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}