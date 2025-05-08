import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const pointRetrieveTrend = new Trend('point_retrieve_trend');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 구성
export let options = {
  scenarios: {
    user_point_retrieve: {
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
    http_req_duration: ['p(95)<1500'],
    'point_retrieve_trend': ['p(95)<800'],
    'api_call_errors': ['rate<0.05']
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 포인트 조회 테스트
export default function() {
  const userId = randomIntBetween(1, 100000); // 1~100000 중 하나

  let retrieveResponse = http.get(
    `${BASE_URL}/users/${userId}/balance`,
    { headers }
  );

  const checkResult = check(retrieveResponse, {
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

  if (!checkResult) {
    apiCallErrors.add(1);
  }

  pointRetrieveTrend.add(retrieveResponse.timings.duration);

  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
