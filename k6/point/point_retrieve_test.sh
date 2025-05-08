#!/bin/bash

# 1. 시작
echo "========== 시작: DB 초기화 =========="
# DB 초기화 스크립트 실행
mysql -h 127.0.0.1 -u root -proot hhplus < ./setup_user_point.sql
if [ $? -ne 0 ]; then
  echo "❌ DB 초기화 실패"
  exit 1
fi

echo "✅ DB 초기화 완료"
echo ""

# 2. k6 테스트 실행
echo "========== 시작: k6 테스트 =========="
k6 run ./point_retrieve_test.js
TEST_RESULT=$?

echo ""

# 3. 테스트 종료 후
echo "========== 종료: DB 정리 =========="
mysql -h 127.0.0.1 -u root -proot hhplus < ../truncate.sql
if [ $? -ne 0 ]; then
  echo "❌ DB 정리 실패"
  exit 1
fi

echo "✅ DB 정리 완료"

# 4. 테스트 결과 리포트
if [ $TEST_RESULT -eq 0 ]; then
  echo "🎉 테스트 성공!"
else
  echo "❌ 테스트 실패! (k6 run 실패)"
  exit 1
fi
