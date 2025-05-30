import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const HOST = 'host.docker.internal';
const BASE_URL = 'http://' + HOST + ':8080';

export const options = {
    tags: { testid: 'redis_coupon_issue' },
    vus: 100,
    duration: '30s',
};

export function setup() {
    // 쿠폰 생성 (plain text 응답으로 id 반환)
    const couponCreateRes = http.post(`${BASE_URL}/api/test/coupons`, JSON.stringify({
        name: 'test-coupon',
        quantity: 1000,
        type: 'first-come-first-served'
    }), { headers: { 'Content-Type': 'application/json' } });

    check(couponCreateRes, {
        '쿠폰 생성 성공': (res) => res.status === 201,
    });

    const couponId = couponCreateRes.body.trim(); // plain text 응답 처리

    // 초기 발급 요청으로 캐싱 유도
    const preloadUserId = 1;
    const preloadRes = http.post(`${BASE_URL}/api/v2/users/${preloadUserId}/coupons`, JSON.stringify({
        couponId: couponId
    }), { headers: { 'Content-Type': 'application/json' } });

    check(preloadRes, {
        '쿠폰 캐싱 요청 성공': (res) => res.status === 200 || res.status === 409,
    });

    return { couponId: couponId };
}

export default function (data) {
    const userId = randomIntBetween(1, 100000);
    const res = http.post(`${BASE_URL}/api/v2/users/${userId}/coupons`, JSON.stringify({
        couponId: data.couponId
    }), { headers: { 'Content-Type': 'application/json' } });

    check(res, {
        '상태코드 200 또는 409': (r) => r.status === 200 || r.status === 409,
    });

    sleep(0.1);
}
