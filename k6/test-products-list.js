import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// 메트릭 정의
const productListTrend = new Trend('product_list_trend');
const productSearchTrend = new Trend('product_search_trend');
const apiCallErrors = new Rate('api_call_errors');

// 기본 설정
const BASE_URL = 'http://localhost:8080/api/v1';
const THINK_TIME_MIN = 1;
const THINK_TIME_MAX = 3;

// 테스트 구성
export let options = {
  scenarios: {
    product_list: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95%의 요청이 1초 이내에 완료되어야 함
    'product_list_trend': ['p(95)<800'],  // 상품 목록 조회 95%가 800ms 이내
    'product_search_trend': ['p(95)<1000'], // 상품 검색 95%가 1초 이내
    'api_call_errors': ['rate<0.05']      // API 호출 오류 5% 미만
  }
};

// 헤더
const headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
};

// 상품 목록 조회 테스트
export default function() {
  // 1. 기본 상품 목록 조회 (페이징)
  let productsResponse = http.get(
    `${BASE_URL}/products?page=0&size=20&sort=id,desc`, 
    { headers }
  );
  
  check(productsResponse, {
    'Product List - Status 200': (r) => r.status === 200,
    'Product List - Has Products': (r) => {
      let body = JSON.parse(r.body);
      return body.data && body.data.content && body.data.content.length > 0;
    }
  }) || apiCallErrors.add(1);
  
  productListTrend.add(productsResponse.timings.duration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
  
  // 2. 키워드로 상품 검색
  const keywords = ['shirt', 'phone', 'book', 'laptop', ''];
  const randomKeyword = keywords[Math.floor(Math.random() * keywords.length)];
  
  let searchResponse = http.get(
    `${BASE_URL}/products?page=0&size=20&sort=id,desc&keyword=${randomKeyword}`, 
    { headers }
  );
  
  check(searchResponse, {
    'Product Search - Status 200': (r) => r.status === 200
  }) || apiCallErrors.add(1);
  
  productSearchTrend.add(searchResponse.timings.duration);
  
  sleep(randomIntBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}
