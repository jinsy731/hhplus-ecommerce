#!/bin/bash

# 현재 디렉토리 설정
TEST_DIR=$(pwd)

# k6 설치 확인
if ! command -v k6 &> /dev/null
then
    echo "k6가 설치되어 있지 않습니다. 먼저 k6를 설치해주세요."
    echo "설치 방법: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# 애플리케이션 실행 확인
echo "테스트 전 애플리케이션이 실행 중인지 확인하세요 (http://localhost:8080)"
read -p "계속하려면 Enter 키를 누르세요..."

# 시작 메시지
echo "=========================================================="
echo "🚀 이커머스 API 성능 테스트 자동화 스크립트 🚀"
echo "=========================================================="

# 테스트 결과 디렉토리 생성
RESULTS_DIR="test-results-$(date +%Y%m%d-%H%M%S)"
mkdir -p $RESULTS_DIR

# 모든 테스트 실행 및 결과 저장
echo "⚡ 1. 상품 목록 API 테스트 실행 ⚡"
k6 run --summary-export=$RESULTS_DIR/products-list-results.json test-products-list.js
echo "✅ 상품 목록 API 테스트 완료"
echo ""

echo "⚡ 2. 인기 상품 API 테스트 실행 ⚡"
k6 run --summary-export=$RESULTS_DIR/products-popular-results.json test-products-popular.js
echo "✅ 인기 상품 API 테스트 완료"
echo ""

echo "⚡ 3. 주문 생성 API 테스트 실행 ⚡"
k6 run --summary-export=$RESULTS_DIR/order-creation-results.json test-order-creation.js
echo "✅ 주문 생성 API 테스트 완료"
echo ""

echo "⚡ 4. 사용자 포인트 API 테스트 실행 ⚡"
k6 run --summary-export=$RESULTS_DIR/user-point-results.json test-user-point.js
echo "✅ 사용자 포인트 API 테스트 완료"
echo ""

echo "⚡ 5. 쿠폰 API 테스트 실행 ⚡"
k6 run --summary-export=$RESULTS_DIR/user-coupon-results.json test-user-coupon.js
echo "✅ 쿠폰 API 테스트 완료"
echo ""

echo "⚡ 6. 종합 API 테스트 실행 ⚡"
k6 run --summary-export=$RESULTS_DIR/comprehensive-results.json -e FUNCTION=comprehensive test-comprehensive.js
echo "✅ 종합 API 테스트 완료"
echo ""

# 결과 분석 및 요약
echo "=========================================================="
echo "📊 성능 테스트 결과 분석 📊"
echo "=========================================================="
echo "테스트 결과가 $RESULTS_DIR 디렉토리에 저장되었습니다."

# 결과 파일이 있는 경우 간단한 분석 정보 제공
if [ -f "$RESULTS_DIR/comprehensive-results.json" ]; then
    echo "종합 테스트 결과 요약:"
    echo "--------------------------------------------------------"
    cat $RESULTS_DIR/comprehensive-results.json | grep -E 'vus_max|iterations|http_req_duration|checks|http_reqs'
    echo ""
    
    # 임계값 초과 확인
    echo "임계값 초과 API 확인:"
    echo "--------------------------------------------------------"
    THRESHOLD_FAILED=$(cat $RESULTS_DIR/comprehensive-results.json | grep -E 'threshold_exceeded')
    if [ -z "$THRESHOLD_FAILED" ]; then
        echo "모든 API가 성능 기준을 충족했습니다! 🎉"
    else
        echo "$THRESHOLD_FAILED"
    fi
fi

echo ""
echo "✅ 모든 테스트가 완료되었습니다."
echo "자세한 분석은 각 JSON 결과 파일을 확인하세요."
echo "=========================================================="
