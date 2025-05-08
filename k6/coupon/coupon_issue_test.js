import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Counter, Rate } from 'k6/metrics';
import { randomItem, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import { SharedArray } from 'k6/data';

const couponIssueTrend = new Trend('coupon_issue_trend');
const couponIssueErrors = new Counter('coupon_issue_errors');
const apiCallErrors = new Rate('api_call_errors');

const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

const coupons = [1];

export let options = {
  stages: [
    { duration: '0s', target: 500 },
    { duration: '30s', target: 500 },
    { duration: '0s', target: 0 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    'coupon_issue_trend': ['p(95)<1200'],
    'api_call_errors': ['rate<0.05']
  }
};

const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 전역 userId 리스트
const sharedUserIds = new SharedArray('userIds', function () {
  let ids = [];
  for (let i = 1; i <= 100000; i++) {
    ids.push(i);
  }
  return ids;
});

// 이건 VU별로 관리할 복제본
let availableUserIds;

export function setup() {
  console.log('📦 Setup complete.');
}

export default function () {
  // VU별 독립 복제본 만들기 (제일 처음에만 복제)
  if (!availableUserIds) {
    availableUserIds = [...sharedUserIds];
  }

  if (availableUserIds.length === 0) {
    console.warn(`⚠️ No more userIds left for VU ${__VU}`);
    return;
  }

  // 랜덤 userId 하나 뽑고 제거
  const userIndex = Math.floor(Math.random() * availableUserIds.length);
  const userId = availableUserIds.splice(userIndex, 1)[0];

  const couponId = coupons[Math.floor(Math.random() * coupons.length)];
  const issuePayload = { couponId: couponId };

  let issueResponse = http.post(
    `${BASE_URL}/users/${userId}/coupons`,
    JSON.stringify(issuePayload),
    { headers }
  );

  check(issueResponse, {
    'Coupon Issue - Status 200': (r) => r.status === 200,
    'Coupon Issue - Valid Response': (r) => {
      try {
        if (r.status === 200) {
          let body = JSON.parse(r.body);
          return body.data && body.data.userCouponId;
        }
        return false;
      } catch (e) {
        return false;
      }
    }
  }) || apiCallErrors.add(1);

  if (issueResponse.status === 400) {
    check(issueResponse, {
      'Coupon Issue - Already Issued (400)': (r) => {
        try {
          let body = JSON.parse(r.body);
          return body.errorCode && body.message;
        } catch (e) {
          return false;
        }
      }
    });
    couponIssueErrors.add(1);
  }

  couponIssueTrend.add(issueResponse.timings.duration);

  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}

export function teardown() {
  console.log('🧹 Test completed.');
}
