import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = 'http://spring-app:8080';
const USER_ID_RANGE = 100000;
const TEST_ID = 'Order-payment-load-test';

// 메트릭
const orderCreateDuration = new Trend('order_create_duration', true);
const orderPaymentDuration = new Trend('order_payment_duration', true);
const errorRate = new Rate('popular_order_error_rate');

export let options = {
  tags: { testid: TEST_ID },
  stages: [
    { duration: '1m', target: 3 },   // Warm-up
    { duration: '2m', target: 5 },   // 목표 TPS 도달
    { duration: '3m', target: 5 },   // 부하 유지
    { duration: '30s', target: 0 }   // 종료
  ],
  summaryTrendStats: ['avg', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)', 'max'],
};

// === Setup: 상품 생성 ===
export function setup() {
    const payload = JSON.stringify({
        name: 'test-product' + Math.random().toString(36).substring(2, 15),
        basePrice: 10000,
        stock: 10000,
    })
  const res = http.post(`${BASE_URL}/api/test/products/simple`, payload, {
    headers: { 'Content-Type': 'application/json'},
  });
  
  check(res, { '상품 생성 성공': (r) => r.status === 200 });

  // 응답 파싱 시도
  let responseData;
  try {
    responseData = typeof res.body === 'string' ? JSON.parse(res.body) : res.body;
  } catch (e) {
    console.error('JSON 파싱 오류:', e.message);
    return null;
  }

  // 안전하게 데이터 접근
  if (!responseData || !responseData.data) {
    console.error('응답 데이터 구조가 예상과 다릅니다:', responseData);
    return null;
  }

  const productId = responseData.data.productId;
  const variantId = responseData.data.variantId;
  
  if (!productId || !variantId) {
    console.error('productId 또는 variantId가 없습니다:', { productId, variantId });
    return null;
  }
  
  console.log('🔥 상품 생성 성공:', { productId, variantId });
  return { productId, variantId };
}

// === 헬퍼 ===
function getRandomUserId() {
  return Math.floor(Math.random() * USER_ID_RANGE) + 1;
}

function createOrder(userId, productId, variantId) {
  const payload = JSON.stringify({
    userId,
    items: [{ 
        productId, variantId, quantity: 1
    }],
    userCouponIds: [],
  });

  const res = http.post(`${BASE_URL}/api/v1/orders/sheet`, payload, {
    headers: { 'Content-Type': 'application/json'},
    tags: { name: 'createOrder' }
  });

  orderCreateDuration.add(res.timings.duration);

  if (res.status >= 500 || res.status === 0) {
    errorRate.add(true);
    console.error('❌ 주문 생성 실패:', {
      status: res.status,
      statusText: res.status_text,
      body: res.body,
      userId: userId,
      productId: productId,
      variantId: variantId
    });
  } else {
    errorRate.add(false);
  }

  const json = res.status === 200 ? res.json() : null;

  return json?.data?.orderId || null;
}

function payOrder(userId, orderId) {
  const payload = JSON.stringify({
    pgPaymentId: 'test-payment-id',
    paymentMethod: 'CARD'
  });

  const res = http.post(`${BASE_URL}/api/v1/orders/${orderId}/payment`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'payOrder' }
  });

  orderPaymentDuration.add(res.timings.duration);

  if (res.status >= 500 || res.status === 0) {
    errorRate.add(true);
    console.error('❌ 결제 실패:', {
      status: res.status,
      statusText: res.status_text,
      body: res.body,
      userId: userId,
      orderId: orderId
    });
  } else {
    errorRate.add(false);
  }

  const success = res.status === 200;
  
  return success;
}

// === 부하 시나리오 ===
export default function (data) {
  const userId = getRandomUserId();
  
  // setup에서 데이터를 받지 못한 경우 처리
  if (!data || !data.productId || !data.variantId) {
    console.error('setup에서 유효한 데이터를 받지 못했습니다:', data);
    return;
  }
  
  const orderId = createOrder(userId, data.productId, data.variantId);

  check(orderId, { '주문 생성 성공': (id) => id !== null });

  if (orderId) {
    const success = payOrder(userId, orderId);
    check(success, { '결제 성공': (v) => v === true });
  }
}
