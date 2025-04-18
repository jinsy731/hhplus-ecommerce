# 이커머스 API 성능 테스트 종합 가이드

이 가이드는 Spring Boot + Kotlin으로 구현된 이커머스 백엔드 서버의 API 성능 테스트를 수행하는 방법을 설명합니다.

## 준비 사항

1. **k6 설치**
   ```bash
   # Linux/Mac
   brew install k6  # Mac with Homebrew
   
   # Windows
   choco install k6  # with Chocolatey
   
   # 또는 공식 웹사이트에서 바이너리 다운로드
   # https://k6.io/docs/getting-started/installation/
   ```

2. **테스트 환경 설정**
   - 이커머스 서버가 로컬 또는 테스트 환경에서 실행 중이어야 합니다
   - 기본 URL은 `http://localhost:8080/api/v1`로 설정됨
   - 테스트 데이터는 실제 환경에 맞게 수정 필요

## 포함된 테스트 스크립트

### 개별 API 테스트

1. **test-products-list.js**
   - 상품 목록 조회 API 테스트
   - 키워드 검색 기능 테스트

2. **test-products-popular.js**
   - 인기 상품 조회 API 테스트

3. **test-order-creation.js**
   - 주문 생성 API 테스트
   - 다양한 주문 패턴 시뮬레이션

4. **test-user-point.js**
   - 사용자 포인트 조회 API 테스트
   - 포인트 충전 API 테스트

5. **test-user-coupon.js**
   - 쿠폰 목록 조회 API 테스트
   - 쿠폰 발급 API 테스트

### 종합 테스트

**test-comprehensive.js**
- 모든 API에 대한 종합 테스트
- 실제 사용자 시나리오 시뮬레이션
- 다양한 실행 모드 지원:
  - 기본 모드: 랜덤 API 호출
  - `comprehensive` 함수: 모든 API 순차 호출
  - 개별 API 함수: `products`, `orders`, `points`, `coupons`

## 테스트 실행 방법

### 개별 테스트 실행

```bash
# 각 API 테스트 개별 실행
k6 run test-products-list.js
k6 run test-products-popular.js
k6 run test-order-creation.js
k6 run test-user-point.js
k6 run test-user-coupon.js
```

### 자동화된 테스트 실행

```bash
# 실행 권한 부여
chmod +x run-all-tests.sh

# 자동화된 테스트 실행
./run-all-tests.sh
```

### 사용자 정의 테스트 실행

```bash
# 부하 강도 조정
k6 run --vus 50 --duration 2m test-order-creation.js

# 특정 함수 실행
k6 run -e FUNCTION=comprehensive test-comprehensive.js
k6 run -e FUNCTION=products test-comprehensive.js

# 결과 저장
k6 run --summary-export=results.json test-comprehensive.js
```

## 주요 테스트 시나리오

### 1. 상품 API 테스트

- **test-products-list.js**
  - 상품 목록 조회 (페이징 적용)
  - 키워드 검색 (다양한 키워드)
  - 성능 요구사항: 95%의 요청이 1초 이내 처리

- **test-products-popular.js**
  - 인기 상품 조회
  - 성능 요구사항: 95%의 요청이 800ms 이내 처리

### 2. 주문 API 테스트

- **test-order-creation.js**
  - 복잡한 주문 생성 (다수 상품, 쿠폰 적용)
  - 결제 수단 선택
  - 성능 요구사항: 95%의 요청이 3초 이내 처리

### 3. 사용자 포인트 API 테스트

- **test-user-point.js**
  - 포인트 잔액 조회
  - 포인트 충전
  - 성능 요구사항: 95%의 조회가 800ms 이내, 충전이 1.2초 이내 처리

### 4. 쿠폰 API 테스트

- **test-user-coupon.js**
  - 쿠폰 목록 조회 (페이징 포함)
  - 쿠폰 발급
  - 중복 발급 등의 에러 케이스 처리
  - 성능 요구사항: 95%의 조회가 800ms 이내, 발급이 1.2초 이내 처리

## 측정 지표

테스트 실행 시 다음 지표들이 측정됩니다:

1. **응답 시간 (Response Time)**
   - 평균 (avg)
   - 중앙값 (med)
   - 95 백분위수 (p95)
   - 99 백분위수 (p99)
   - 최대값 (max)

2. **처리량 (Throughput)**
   - 초당 요청 수 (RPS)
   - 총 요청 수 (count)

3. **에러율 (Error Rate)**
   - 총 에러 수 (count)
   - 에러율 (rate)

4. **검증 결과 (Checks)**
   - 성공률 (rate)
   - 실패 수 (failures)

## 결과 분석 및 병목 현상 식별

테스트 결과를 바탕으로 다음과 같은 분석을 수행하세요:

1. **API별 응답 시간 분석**
   - 어떤 API가 가장 느린가?
   - 특정 API의 p95가 임계값을 초과하는가?

2. **에러 패턴 분석**
   - 특정 부하 상황에서 에러가 증가하는가?
   - 어떤 유형의 에러가 주로 발생하는가?

3. **성능 병목 지점 식별**
   - 주문 생성 API의 여러 단계 중 어느 부분이 느린가?
   - 데이터베이스 연산이 병목인가, 비즈니스 로직이 병목인가?
   - 동시성 이슈가 발생하는 지점이 있는가?

## 최적화 전략

테스트 결과 병목 현상이 발견된 경우, 다음과 같은 최적화 전략을 고려하세요:

### 1. 주문 생성 API

- **비동기 처리 도입**
  - 주문 처리 후 비동기로 이메일 발송, 로깅 등을 처리
  - 메시지 큐 활용 (RabbitMQ, Kafka 등)

- **데이터베이스 최적화**
  - 트랜잭션 범위 최소화
  - 인덱스 추가
  - 쿼리 최적화

- **동시성 제어**
  - 낙관적 락(Optimistic Lock) 적용
  - 분산 락(Distributed Lock) 도입

### 2. 인기 상품 조회 API

- **캐싱 전략**
  - Redis를 활용한 결과 캐싱
  - 캐시 갱신 주기 최적화

- **집계 작업 최적화**
  - 배치 작업 개선
  - 병렬 처리 도입

### 3. 사용자 포인트 및 쿠폰 API

- **동시성 이슈 해결**
  - 낙관적 락 적용
  - 비관적 락 적용(제한된 쿠폰 수량 등의 경우)

- **데이터 접근 최적화**
  - 쿼리 최적화
  - 인덱스 추가

## 테스트 결과 해석 예시

```
✓ http_req_duration..............: avg=245.12ms min=78.11ms med=198.45ms max=2.85s p(90)=398.12ms p(95)=487.65ms
✓ http_req_failed................: 0.05%    ✓ 5      ✗ 9995
✓ product_list_api...............: avg=134.22ms min=53.45ms med=118.34ms max=987.65ms p(95)=324.56ms
✓ product_popular_api............: avg=187.56ms min=68.23ms med=156.78ms max=1.12s p(95)=456.78ms
✗ order_creation_api.............: avg=658.23ms min=342.12ms med=587.45ms max=2.85s p(95)=1.42s
```

위 결과 예시에서는 `order_creation_api`가 p(95)에서 임계값을 초과하는 것을 확인할 수 있습니다. 이 API의 성능 최적화가 우선적으로 필요합니다.

## 모니터링 통합

성능 테스트를 시스템 모니터링과 통합하면 더 정확한 병목 지점을 식별할 수 있습니다:

1. **k6 + Grafana 연동**
   - k6의 측정 결과를 Grafana에 연동하여 시각화
   - 명령어: `k6 run --out influxdb=http://localhost:8086/k6 test.js`

2. **서버 메트릭 모니터링**
   - CPU, 메모리, 디스크 I/O, 네트워크 사용량
   - JVM 메트릭: GC, 힙 사용량, 스레드 수

3. **데이터베이스 모니터링**
   - 쿼리 실행 시간
   - 연결 풀 사용량
   - 락 경합 상황

## 결론

성능 테스트는 서비스 출시 전에 반드시 수행해야 하는 중요한 단계입니다. 이 가이드에서 제공하는 테스트 스크립트와 방법론을 활용하여 이커머스 서비스의 성능을 검증하고, 병목 현상을 사전에 식별하여 최적화하세요.

테스트 결과를 바탕으로 시스템을 지속적으로 개선하면, 사용자에게 안정적이고 빠른 서비스를 제공할 수 있습니다.
