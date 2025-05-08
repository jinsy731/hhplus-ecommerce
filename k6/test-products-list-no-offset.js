import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const productSearchTrend = new Trend('product_search_trend');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 구성
export let options = {
  scenarios: {
    product_search: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '30s', target: 100 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<200'], // 전체 요청 95%가 200ms 미만
    'product_search_trend': ['p(95)<200'], // 검색 API 응답 95%가 200ms 미만
    'api_call_errors': ['rate<0.05'] // 오류율 5% 이하
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 테스트 시나리오 (검색 요청만)
export default function () {
  const keywords = ['셔츠', '후디', '자켓', '점퍼', '바지', '코트', '맨투맨', '청바지', '슬랙스'];
  const randomKeyword = keywords[Math.floor(Math.random() * keywords.length)];

  let searchResponse = http.get(
    `${BASE_URL}/products?lastId=80000&size=20&sort=id,desc&keyword=${encodeURIComponent(randomKeyword)}`,
    { headers }
  );

  check(searchResponse, {
    'Product Search - Status 200': (r) => r.status === 200,
    'Product Search - Has Results': (r) => {
        try {
          const body = JSON.parse(r.body);
          const products = body?.data?.products;
          const isArray = Array.isArray(products);
          if (!isArray) {
            console.log("❌ products가 배열이 아님:", products);
          }
          return isArray;
        } catch (err) {
          console.log("❌ JSON 파싱 에러:", err);
          return false;
        }
      }
  }) || apiCallErrors.add(1);

  productSearchTrend.add(searchResponse.timings.duration);

  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
