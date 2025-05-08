# 이커머스 백엔드 서비스

> Spring Boot와 Kotlin으로 개발된 이커머스 서비스 백엔드 서버 프로젝트입니다.

## 1. 프로젝트 소개

### 개요
이 프로젝트는 이커머스 서비스의 백엔드 서버를 Spring Boot와 Kotlin을 사용하여 구현한 것입니다. 사용자 관리, 상품 조회, 주문 처리, 결제, 쿠폰 기능 등 이커머스 서비스에 필요한 핵심 기능을 제공합니다.

### 주요 기능
- **잔액 충전/조회**: 사용자 잔액 관리 기능
- **상품 조회**: 상품 목록 및 재고 조회 기능
- **주문 및 결제**: 사용자 주문 생성 및 결제 처리
- **쿠폰 관리**: 선착순 쿠폰 발급 및 할인 적용
- **인기 상품 통계**: 최근 판매 데이터 기반 인기 상품 제공

### 기술 스택
- **프레임워크**: Spring Boot 3.4.1
- **언어**: Kotlin 1.9.0
- **데이터베이스**: MySQL 8.0
- **ORM**: Spring Data JPA + Hibernate
- **API 문서화**: Swagger/SpringDoc OpenAPI
- **테스트**: JUnit 5, Mockito, Kotest, Testcontainers
- **성능 테스트**: k6

## 2. 시작하기

### 사전 요구사항
- JDK 17 이상
- Docker 및 Docker Compose
- MySQL 8.0 (Docker Compose로 실행 가능)

### 설치 방법

1. 프로젝트 클론
```bash
git clone <repository-url>
cd hhplus-ecommerce
```

2. Docker Compose로 MySQL 실행
```bash
docker-compose up -d
```

3. 애플리케이션 빌드
```bash
./gradlew build
```

4. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 환경 설정
`application.yml` 파일에서 다음 설정을 확인할 수 있습니다:

```yaml
spring:
  profiles:
    active: local
  datasource:
    url: jdbc:mysql://localhost:3306/hhplus?characterEncoding=UTF-8&serverTimezone=UTC
    username: application
    password: application
```

### 실행 방법
1. 로컬 개발 환경에서 실행: `./gradlew bootRun`
2. JAR 파일 실행: `java -jar build/libs/server-{version}.jar`

## 3. 프로젝트 구조

### 패키지 구조
```
kr.hhplus.be.server/
├── common/            # 공통 유틸리티, 상수, 예외 처리
├── product/           # 상품 관련 도메인 및 기능
├── order/             # 주문 관련 도메인 및 기능
├── payment/           # 결제 관련 도메인 및 기능
├── user/              # 사용자 관련 도메인 및 기능
└── coupon/            # 쿠폰 관련 도메인 및 기능
```

### 아키텍처
이 프로젝트는 도메인 중심의 계층형 아키텍처를 따릅니다:

- **API Layer**: 컨트롤러 및 요청/응답 DTO
- **Service Layer**: 비즈니스 로직 처리
- **Repository Layer**: 데이터 접근 및 관리
- **Domain Layer**: 핵심 도메인 모델 정의

### 도메인 모델
주요 도메인 모델:
- **User/UserBalance**: 사용자 및 잔액 정보
- **Product/ProductVariant**: 상품 및 옵션별 상품 정보
- **Order/OrderItem**: 주문 및 주문 항목
- **Coupon/UserCoupon**: 쿠폰 및 사용자별 발급된 쿠폰
- **Payment**: 결제 정보

## 4. API 문서

### Swagger 접근 방법
애플리케이션이 실행된 후, 다음 URL에서 Swagger UI를 통해 API 문서에 접근할 수 있습니다:
```
http://localhost:8080/swagger-ui/index.html
```

### API 개요
- **/api/v1/users/**: 사용자 관련 API (잔액 충전/조회)
- **/api/v1/products/**: 상품 관련 API (목록 조회, 인기 상품)
- **/api/v1/orders/**: 주문 관련 API (주문 생성, 조회)
- **/api/v1/coupons/**: 쿠폰 관련 API (쿠폰 발급, 목록 조회)

## 5. 개발 가이드

### 코딩 컨벤션
- Kotlin 공식 코딩 컨벤션 준수
- 네이밍 규칙:
    - 클래스: PascalCase
    - 함수/변수: camelCase
    - 상수: UPPER_SNAKE_CASE

### 개발 환경 설정
1. IntelliJ IDEA 권장
2. Kotlin 플러그인 설치
3. 프로젝트 import 후 Gradle 설정 확인

### 테스트 방법
단위 테스트 실행:
```bash
./gradlew test
```

통합 테스트 실행:
```bash
./gradlew integrationTest
```

## 6. 성능 테스트

### k6 테스트 실행 방법
k6 디렉토리 내의 스크립트를 사용하여 성능 테스트를 실행할 수 있습니다:

```bash
cd k6
./run-performance-test.sh
```

### 성능 테스트 결과
성능 테스트 결과는 `k6/summary` 디렉토리에 저장됩니다. 주요 지표:
- 초당 요청 처리량
- 응답 시간 (평균/P95/P99)
- 오류율

## 7. 데이터베이스

### 스키마 설명
주요 테이블:
- **users**: 사용자 정보
- **user_balances**: 사용자 잔액 정보
- **products**: 상품 정보
- **product_variants**: 상품 옵션별 정보
- **orders**: 주문 정보
- **order_items**: 주문별 상품 항목
- **coupons**: 쿠폰 정보
- **user_coupons**: 사용자별 발급된 쿠폰
- **payments**: 결제 정보

### ER 다이어그램
ER 다이어그램은 `docs/05_ER_diagram.md` 파일에서, 데이터 모델 상세 정보는 `docs/02_Domain_modeling.md` 파일에서 확인할 수 있습니다.