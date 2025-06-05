import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';

const BASE_URL = 'http://spring-app:8080';
const USER_ID_RANGE = 100000;
const POLL_INTERVAL = 0.3;
const MAX_POLL_ATTEMPTS = 10;

const issueDuration = new Trend('issue_duration', true);
const serverErrorRate = new Rate('server_error_rate');

export let options = {
  tags: { testid: 'coupon-issue-stress-test' },
  stages: [
    { duration: '10s', target: 50 },
    { duration: '10s', target: 100 },
    { duration: '10s', target: 150 },
    { duration: '10s', target: 200 },
    { duration: '10s', target: 250 },
    { duration: '10s', target: 500 },
    { duration: '10s', target: 700 },
    { duration: '10s', target: 900 },
    { duration: '10s', target: 1500 },
    { duration: '10s', target: 2000 },
    { duration: '10s', target: 0 }
  ],
  thresholds: {
    'issue_duration': ['p(95)<800', 'p(99.9)<1500'],
    'server_error_rate': ['rate<0.001'],
    // 'issued_coupon_total': ['count==50000'],
  },
  // 커스텀 메트릭을 summary에 포함하기 위한 설정
  summaryTrendStats: ['avg', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)', 'max'],
};

export function setup() {
  const res = http.post(`${BASE_URL}/api/test/coupons`);
  check(res, { '쿠폰 생성 성공': (r) => r.status === 200 });
  console.log('✅ 쿠폰 생성 성공:', res.body);
  return { couponId: parseInt(res.body) };
}

function getRandomUserId() {
  return Math.floor(Math.random() * USER_ID_RANGE) + 1;
}

function issueCoupon(userId, couponId) {
  return http.post(
    `${BASE_URL}/api/v3/users/${userId}/coupons`,
    JSON.stringify({ couponId }),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );
}

function pollCouponStatus(userId, couponId) {
  for (let i = 0; i < MAX_POLL_ATTEMPTS; i++) {
    sleep(POLL_INTERVAL);
    const res = http.get(`${BASE_URL}/api/v2/users/${userId}/coupons/${couponId}/status`);

    if (res.status >= 500 || res.status === 0) {
      serverErrorRate.add(true);
      return false;
    } else {
      serverErrorRate.add(false);
    }

    let json;
    try {
      json = res.json();
    } catch (_) {
      return false;
    }

    if (json?.data?.status === 'ISSUED') {
      return true;
    }
  }
  return false;
}

// 커스텀 수량 검증을 위한 메트릭 등록
const issuedCouponTotal = new Counter('issued_coupon_total');

export default function (data) {
  const userId = getRandomUserId();
  const couponId = data.couponId;

  const res = issueCoupon(userId, couponId);
  issueDuration.add(res.timings.duration);

  if (res.status >= 500 || res.status === 0) {
    serverErrorRate.add(true);
    fail(`서버 오류 발생: ${res.status}`);
  } else {
    serverErrorRate.add(false);
  }

  if (res.status === 409) {
    return; // 중복 발급/소진 → 정상 처리지만 무시
  }

  // 응답이 200인 경우에만 polling 수행
  if (res.status === 200) {
    const issued = pollCouponStatus(userId, couponId);
    if (issued) {
      issuedCouponTotal.add(1);
    }
  }
}
