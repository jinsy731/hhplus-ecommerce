import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const popularProductTrend = new Trend('popular_product_trend');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 구성
export let options = {
  scenarios: {
    popular_products: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 30 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초 이내에 완료되어야 함
    'popular_product_trend': ['p(95)<800'],  // 인기 상품 조회 95%가 800ms 이내
    'api_call_errors': ['rate<0.05']      // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 인기 상품 조회 테스트
export default function() {
  // 인기 상품 조회
  let popularResponse = http.get(
    `${BASE_URL}/products/popular`, 
    { headers }
  );
  
  check(popularResponse, {
    'Popular Products - Status 200': (r) => r.status === 200,
    'Popular Products - Has Content': (r) => {
      let body = JSON.parse(r.body);
      return body.data && Array.isArray(body.data);
    }
  }) || apiCallErrors.add(1);
  
  popularProductTrend.add(popularResponse.timings.duration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
