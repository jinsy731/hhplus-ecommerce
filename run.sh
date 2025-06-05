#!/bin/bash

# ========== 설정 ==========
APP_NAME="spring-app"
JAR_PATH="build/libs"
DOCKERFILE_NAME="Dockerfile-app"
COMPOSE_FILE="docker-compose.yml"

# ========== Step 1: Gradle 빌드 ==========
echo "📦 Gradle 빌드 시작..."
./gradlew clean build -x test

if [ $? -ne 0 ]; then
  echo "❌ 빌드 실패"
  exit 1
fi

# ========== Step 2: spring-app만 재빌드 ==========
echo "🐳 spring-app 이미지 재빌드 중..."
docker-compose -f $COMPOSE_FILE build --no-cache $APP_NAME

if [ $? -ne 0 ]; then
  echo "❌ spring-app 이미지 빌드 실패"
  exit 1
fi

# ========== Step 3: 기존 spring-app 종료 ==========
echo "🛑 spring-app 컨테이너 종료 중..."
docker-compose -f $COMPOSE_FILE stop $APP_NAME

# ========== Step 4: spring-app 컨테이너 제거 ==========
echo "🧹 spring-app 컨테이너 제거 중..."
docker-compose -f $COMPOSE_FILE rm -f $APP_NAME

# ========== Step 5: spring-app만 재시작 ==========
echo "🚀 spring-app 컨테이너 시작 중..."
docker-compose -f $COMPOSE_FILE up -d $APP_NAME

if [ $? -eq 0 ]; then
  echo "✅ spring-app 컨테이너가 성공적으로 실행되었습니다."
  docker ps | grep $APP_NAME
else
  echo "❌ spring-app 시작 실패"
  exit 1
fi
