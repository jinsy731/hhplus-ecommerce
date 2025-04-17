#!/bin/bash

# k6 설치 확인
if ! command -v k6 &> /dev/null
then
    echo "k6가 설치되어 있지 않습니다. 먼저 k6를 설치해주세요."
    echo "설치 방법: https://k6.io/docs/getting-started/installation/"
    exit
fi

# 테스트 실행
echo "Performance 테스트를 시작합니다..."

echo "1. 기본 테스트 실행 (모든 시나리오 포함)"
k6 run performance-test.js

echo "2. 상품 조회 API만 테스트"
k6 run --tag testType=productBrowsing --scenario product_browse performance-test.js

echo "3. 주문 생성 API만 테스트 (고부하)"
k6 run --tag testType=orderCreation --scenario order_creation -e STRESS_TEST=true performance-test.js --vus 50 --duration 2m

echo "4. 포인트 관련 API만 테스트"
k6 run --tag testType=pointOperations --scenario user_point performance-test.js

echo "5. 쿠폰 관련 API만 테스트"
k6 run --tag testType=couponOperations --scenario coupon_operations performance-test.js

echo "테스트 완료!"
