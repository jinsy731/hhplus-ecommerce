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
echo "🚀 이커머스 API 성능 테스트 시작 🚀"
echo "=========================================================="

# 테스트 종류 선택
echo "수행할 테스트 유형을 선택하세요:"
echo "1) 기본 API 테스트 (test-comprehensive.js)"
echo "2) 사용자 시나리오 테스트 (test-specific-scenarios.js)"
echo "3) 고부하 스파이크 테스트 (high-load-tests.js)"
echo "4) 모든 테스트 실행"
read -p "선택 (1-4): " test_choice

# 테스트 결과 디렉토리 생성
RESULTS_DIR="k6/summary/test-results-$(date +%Y%m%d-%H%M%S)"
mkdir -p $RESULTS_DIR

# 테스트 실행 함수
run_comprehensive_test() {
    local function_name=$1
    local function_display=$2
    
    echo "⚡ $function_display 테스트 실행 중... ⚡"
    k6 run --summary-export=$RESULTS_DIR/comprehensive-$function_name-results.json -e FUNCTION=$function_name k6/test-comprehensive.js
    echo "✅ $function_display 테스트 완료"
    echo ""
}

# 선택에 따른 테스트 실행
case $test_choice in
    1)
        echo "기본 API 테스트 실행 - 테스트할 기능을 선택하세요:"
        echo "a) 상품 API 테스트"
        echo "b) 주문 API 테스트"
        echo "c) 포인트 API 테스트"
        echo "d) 쿠폰 API 테스트"
        echo "e) 모든 API 종합 테스트"
        read -p "선택 (a-e): " api_choice
        
        case $api_choice in
            a) run_comprehensive_test "products" "상품 API" ;;
            b) run_comprehensive_test "orders" "주문 API" ;;
            c) run_comprehensive_test "points" "포인트 API" ;;
            d) run_comprehensive_test "coupons" "쿠폰 API" ;;
            e) run_comprehensive_test "comprehensive" "종합 API" ;;
            *) echo "잘못된 선택입니다."; exit 1 ;;
        esac
        ;;
    2)
        echo "⚡ 사용자 시나리오 테스트 실행 중... ⚡"
        k6 run --summary-export=$RESULTS_DIR/specific-scenarios-results.json k6/test-specific-scenarios.js
        echo "✅ 사용자 시나리오 테스트 완료"
        echo ""
        ;;
    3)
        echo "⚡ 고부하 스파이크 테스트 실행 중... ⚡"
        echo "⚠️ 주의: 이 테스트는 시스템에 높은 부하를 줍니다. 테스트 환경에서만 실행하세요."
        read -p "계속하려면 Enter 키를 누르세요 (취소하려면 Ctrl+C)..."
        k6 run --summary-export=$RESULTS_DIR/high-load-results.json k6/high-load-tests.js
        echo "✅ 고부하 스파이크 테스트 완료"
        echo ""
        ;;
    4)
        echo "⚡ 모든 테스트 순차 실행 중... ⚡"
        
        # 기본 API 테스트
        run_comprehensive_test "products" "상품 API"
        run_comprehensive_test "orders" "주문 API"
        run_comprehensive_test "points" "포인트 API"
        run_comprehensive_test "coupons" "쿠폰 API"
        run_comprehensive_test "comprehensive" "종합 API"
        
        # 사용자 시나리오 테스트
        echo "⚡ 사용자 시나리오 테스트 실행 중... ⚡"
        k6 run --summary-export=$RESULTS_DIR/specific-scenarios-results.json k6/test-specific-scenarios.js
        echo "✅ 사용자 시나리오 테스트 완료"
        echo ""
        
        # 고부하 테스트
        echo "⚡ 고부하 스파이크 테스트 실행 중... ⚡"
        echo "⚠️ 주의: 이 테스트는 시스템에 높은 부하를 줍니다."
        read -p "고부하 테스트를 실행하려면 Enter 키를 누르세요 (건너뛰려면 's' 입력): " high_load_choice
        if [[ "$high_load_choice" != "s" ]]; then
            k6 run --summary-export=$RESULTS_DIR/high-load-results.json k6/high-load-tests.js
            echo "✅ 고부하 스파이크 테스트 완료"
        else
            echo "고부하 테스트를 건너뜁니다."
        fi
        echo ""
        ;;
    *)
        echo "잘못된 선택입니다."
        exit 1
        ;;
esac

# 결과 분석 및 요약
echo "=========================================================="
echo "📊 성능 테스트 결과 분석 📊"
echo "=========================================================="
echo "테스트 결과가 $RESULTS_DIR 디렉토리에 저장되었습니다."

# 결과 파일이 있는 경우 간단한 분석 정보 제공
if [ -n "$(ls -A $RESULTS_DIR)" ]; then
    echo "테스트 결과 요약:"
    echo "--------------------------------------------------------"
    
    for result_file in $RESULTS_DIR/*.json; do
        echo "$(basename "$result_file") 분석:"
        jq -r '.metrics.http_req_duration.values | "평균 응답 시간: \(.avg | round)ms, 최대: \(.max | round)ms, p95: \(.p(95) | round)ms"' "$result_file" 2>/dev/null || echo "JSON 파싱 오류"
        jq -r '.metrics.http_req_failed | "요청 실패율: \(.rate | round * 100)%"' "$result_file" 2>/dev/null
        jq -r '.metrics.iterations | "총 반복 횟수: \(.count)"' "$result_file" 2>/dev/null
        echo ""
    done
    
    # 임계값 초과 확인
    echo "임계값 초과 여부 확인:"
    echo "--------------------------------------------------------"
    for result_file in $RESULTS_DIR/*.json; do
        thresholds=$(jq -r '.root_group.checks[] | select(.passes == false) | .name' "$result_file" 2>/dev/null)
        if [ -n "$thresholds" ]; then
            echo "$(basename "$result_file")에서 다음 임계값이 초과되었습니다:"
            echo "$thresholds"
            echo ""
        else
            echo "$(basename "$result_file"): 모든 임계값을 충족했습니다! 🎉"
        fi
    done
else
    echo "테스트 결과 파일이 생성되지 않았습니다."
fi

echo ""
echo "✅ 모든 테스트가 완료되었습니다."
echo "자세한 분석은 각 JSON 결과 파일을 확인하거나,"
echo "다음 명령어로 Grafana에 결과를 시각화할 수 있습니다 (InfluxDB 연동 시):"
echo "  k6 run --out influxdb=http://localhost:8086/k6 <테스트 스크립트>"
echo "=========================================================="
