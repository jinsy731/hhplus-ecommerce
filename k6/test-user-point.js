import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const pointRetrieveTrend = new Trend('point_retrieve_trend');
const pointChargeTrend = new Trend('point_charge_trend');
const pointChargeErrors = new Counter('point_charge_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 데이터 (실제 환경에 맞게 수정 필요)
const users = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// 테스트 구성
export let options = {
  scenarios: {
    user_point: {
      executor: 'ramping-vus',
      startVUs: 3,
      stages: [
        { duration: '30s', target: 15 },
        { duration: '1m', target: 15 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<1500'], // 95%의 요청이 1.5초 이내에 완료되어야 함
    'point_retrieve_trend': ['p(95)<800'],  // 포인트 조회 95%가 800ms 이내
    'point_charge_trend': ['p(95)<1200'],   // 포인트 충전 95%가 1.2초 이내
    'api_call_errors': ['rate<0.05']      // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 사용자 포인트 조회 및 충전 테스트
export default function() {
  // 1. 사용자 선택
  const userId = users[Math.floor(Math.random() * users.length)];
  
  // 2. 포인트 조회
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
  
  // 3. 포인트 충전
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
}
