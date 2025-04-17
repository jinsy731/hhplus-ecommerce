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

# 개별 API 테스트 실행 함수
run_test() {
    TEST_NAME=$1
    TEST_FILE=$2
    VUS=$3
    DURATION=$4
    
    echo "⚡ $TEST_NAME 테스트 시작 (VUs: $VUS, Duration: $DURATION) ⚡"
    k6 run --tag testType=$TEST_NAME --vus $VUS --duration $DURATION $TEST_FILE
    echo "✅ $TEST_NAME 테스트 완료"
    echo ""
    sleep 2
}

# 테스트 메뉴 표시
show_menu() {
    clear
    echo "======================================================="
    echo "🚀 이커머스 API 성능 테스트 🚀"
    echo "======================================================="
    echo "1. 상품 목록 조회 API 테스트"
    echo "2. 인기 상품 조회 API 테스트"
    echo "3. 주문 생성 API 테스트"
    echo "4. 사용자 포인트 API 테스트"
    echo "5. 쿠폰 API 테스트"
    echo "6. 모든 API 테스트 순차 실행"
    echo "7. 부하 테스트 (고부하 주문 생성)"
    echo "8. 스트레스 테스트 (모든 API 동시 실행)"
    echo "9. 종료"
    echo "======================================================="
    echo "선택: "
}

# 테스트 실행
execute_test() {
    case $1 in
        1)
            run_test "products-list" "$TEST_DIR/test-products-list.js" 20 "1m"
            ;;
        2)
            run_test "products-popular" "$TEST_DIR/test-products-popular.js" 30 "1m"
            ;;
        3)
            run_test "order-creation" "$TEST_DIR/test-order-creation.js" 10 "1m"
            ;;
        4)
            run_test "user-point" "$TEST_DIR/test-user-point.js" 15 "1m"
            ;;
        5)
            run_test "user-coupon" "$TEST_DIR/test-user-coupon.js" 20 "1m"
            ;;
        6)
            echo "⚡ 모든 API 테스트 순차 실행 ⚡"
            run_test "products-list" "$TEST_DIR/test-products-list.js" 10 "30s"
            run_test "products-popular" "$TEST_DIR/test-products-popular.js" 15 "30s"
            run_test "user-point" "$TEST_DIR/test-user-point.js" 8 "30s"
            run_test "user-coupon" "$TEST_DIR/test-user-coupon.js" 10 "30s"
            run_test "order-creation" "$TEST_DIR/test-order-creation.js" 5 "30s"
            echo "✅ 모든 API 테스트 완료"
            ;;
        7)
            echo "⚡ 부하 테스트 - 고부하 주문 생성 (50 VUs, 2분) ⚡"
            run_test "order-creation-heavy" "$TEST_DIR/test-order-creation.js" 50 "2m"
            ;;
        8)
            echo "⚡ 스트레스 테스트 - 모든 API 동시 실행 ⚡"
            
            # 백그라운드로 실행
            k6 run --tag testType="stress-products-list" "$TEST_DIR/test-products-list.js" --vus 30 --duration 1m &
            sleep 2
            k6 run --tag testType="stress-products-popular" "$TEST_DIR/test-products-popular.js" --vus 20 --duration 1m &
            sleep 2
            k6 run --tag testType="stress-user-point" "$TEST_DIR/test-user-point.js" --vus 15 --duration 1m &
            sleep 2
            k6 run --tag testType="stress-user-coupon" "$TEST_DIR/test-user-coupon.js" --vus 15 --duration 1m &
            sleep 2
            k6 run --tag testType="stress-order-creation" "$TEST_DIR/test-order-creation.js" --vus 10 --duration 1m
            
            echo "✅ 스트레스 테스트 완료"
            ;;
        9)
            echo "테스트를 종료합니다."
            exit 0
            ;;
        *)
            echo "잘못된 선택입니다. 다시 시도해주세요."
            ;;
    esac
    
    # 메뉴로 돌아가기 전 대기
    read -p "메뉴로 돌아가려면 Enter 키를 누르세요..."
}

# 메인 루프
while true; do
    show_menu
    read choice
    execute_test $choice
done
