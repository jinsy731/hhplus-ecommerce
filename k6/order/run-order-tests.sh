#!/bin/bash

# k6 주문 API 성능 테스트 실행 스크립트
# 이 스크립트는 주문 API에 대한 다양한 성능 테스트를 순차적으로 실행합니다.

BASE_DIR="$(dirname "$0")"
REPORTS_DIR="${BASE_DIR}/reports"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 결과 저장 디렉토리 생성
mkdir -p "${REPORTS_DIR}"

echo "========================================================"
echo "🔍 주문 API 성능 테스트 시작 - $(date)"
echo "========================================================"

# 1. 분산 주문 테스트 (여러 상품에 분산된 주문 요청)
echo "📊 테스트 1/3: 분산 주문 테스트 시작 - $(date)"
k6 run "${BASE_DIR}/test-sparse-order-load.js" \
    --summary-export="${REPORTS_DIR}/sparse_order_${TIMESTAMP}.json" \
    --out json="${REPORTS_DIR}/sparse_order_metrics_${TIMESTAMP}.json"

echo "✅ 분산 주문 테스트 완료 - $(date)"
echo ""

# 잠시 대기 (시스템 안정화)
sleep 10

# 2. 집중 주문 테스트 (소수의 상품에 집중적인 주문 요청)
echo "📊 테스트 2/3: 집중 주문 테스트 시작 - $(date)"
k6 run "${BASE_DIR}/test-concentrated-order-load.js" \
    --summary-export="${REPORTS_DIR}/concentrated_order_${TIMESTAMP}.json" \
    --out json="${REPORTS_DIR}/concentrated_order_metrics_${TIMESTAMP}.json"

echo "✅ 집중 주문 테스트 완료 - $(date)"
echo ""

# 잠시 대기 (시스템 안정화)
sleep 10

# 3. 통합 테스트 스위트 (모든 시나리오를 순차적으로 실행)
echo "📊 테스트 3/3: 통합 시나리오 테스트 시작 - $(date)"
k6 run "${BASE_DIR}/order-test-suite.js" \
    --summary-export="${REPORTS_DIR}/order_suite_${TIMESTAMP}.json" \
    --out json="${REPORTS_DIR}/order_suite_metrics_${TIMESTAMP}.json"

echo "✅ 통합 시나리오 테스트 완료 - $(date)"
echo ""

echo "========================================================"
echo "✅ 모든 주문 API 성능 테스트 완료 - $(date)"
echo "결과 리포트: ${REPORTS_DIR}"
echo "========================================================"

# P99, P95, P50 지표 요약 출력
echo "📋 성능 지표 요약:"
echo "------------------------------------------------------"
echo "분산 주문 테스트:"
jq '.metrics."http_req_duration".values.p99, .metrics."http_req_duration".values.p95, .metrics."http_req_duration".values.p50' "${REPORTS_DIR}/sparse_order_${TIMESTAMP}.json"
echo "------------------------------------------------------"
echo "집중 주문 테스트:"
jq '.metrics."http_req_duration".values.p99, .metrics."http_req_duration".values.p95, .metrics."http_req_duration".values.p50' "${REPORTS_DIR}/concentrated_order_${TIMESTAMP}.json"
echo "------------------------------------------------------"
echo "통합 시나리오 테스트:"
jq '.metrics."http_req_duration".values.p99, .metrics."http_req_duration".values.p95, .metrics."http_req_duration".values.p50' "${REPORTS_DIR}/order_suite_${TIMESTAMP}.json"
echo "------------------------------------------------------"
