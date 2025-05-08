import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const pointChargeTrend = new Trend('point_charge_trend');
const pointChargeErrors = new Counter('point_charge_errors');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 구성
export let options = {
  scenarios: {
    user_point_charge: {
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
    'point_charge_trend': ['p(95)<1200'],
    'api_call_errors': ['rate<0.05']
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 포인트 충전 테스트
export default function() {
  const userId = randomIntBetween(1, 100000); // 1~100000 중 하나
  const chargeAmount = randomIntBetween(1000, 100000);

  const chargePayload = {
    amount: chargeAmount
  };

  let chargeResponse = http.post(
    `${BASE_URL}/users/${userId}/balance`,
    JSON.stringify(chargePayload),
    { headers }
  );

  const checkResult = check(chargeResponse, {
    'User Point Charge - Status 200': (r) => r.status === 200,
    'User Point Charge - Updated Point': (r) => {
      try {
        let body = JSON.parse(r.body);
        return body.data && typeof body.data.point !== 'undefined';
      } catch (e) {
        return false;
      }
    }
  });

  if (!checkResult) {
    pointChargeErrors.add(1);
    apiCallErrors.add(1);
  }

  pointChargeTrend.add(chargeResponse.timings.duration);

  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
