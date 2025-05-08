#!/bin/bash

# 쿠폰 API 테스트 실행 스크립트

echo "===== 쿠폰 API 성능 테스트 시작 ====="

# 1. 쿠폰 목록 조회 테스트
echo "1. 쿠폰 목록 조회 테스트 실행 중..."
k6 run coupon_retrieve_test.js

# 잠시 대기
sleep 3

# 2. 쿠폰 발급 테스트
echo "2. 쿠폰 발급 테스트 실행 중..."
k6 run coupon_issue_test.js

# 잠시 대기
sleep 3

# 3. 통합 시나리오 테스트
echo "3. 통합 시나리오 테스트 실행 중..."
k6 run coupon_test_suite.js

echo "===== 쿠폰 API 성능 테스트 완료 ====="
