# k6 주문 API 성능 테스트

이 디렉토리에는 이커머스 서비스의 주문 API에 대한 성능 테스트 스크립트가 포함되어 있습니다.

## 테스트 시나리오

성능 테스트는 다음 세 가지 주요 시나리오로 구성되어 있습니다:

1. **분산된 주문 패턴 테스트** (`test-sparse-order-load.js`)
   - 다양한 상품에 분산된 주문 요청을 시뮬레이션
   - 상품 락(lock) 경합이 적은 상황 테스트
   - 100개의 다양한 상품에 대해 랜덤하게 주문 생성

2. **집중된 주문 패턴 테스트** (`test-concentrated-order-load.js`)
   - 소수의 인기 상품에 집중된 주문 요청을 시뮬레이션
   - 상품 락(lock) 경합이 많이 발생하는 상황 테스트
   - 10개 이하의 인기 상품에 대해 집중적인 주문 생성

3. **통합 시나리오 테스트** (`order-test-suite.js`)
   - 위 두 가지 패턴과 현실적인 혼합 시나리오를 순차적으로 실행
   - 순차적으로 각 시나리오를 실행하여 시스템 동작 확인
   - 다양한 부하 프로필로 시스템 한계 테스트

## 모니터링 지표

각 테스트는 다음 핵심 지표를 측정하고 보고합니다:

- **P99 응답 시간**: 99%의 요청이 이 시간 이내에 완료됨
- **P95 응답 시간**: 95%의 요청이 이 시간 이내에 완료됨
- **P50 응답 시간**: 중간값 응답 시간(50%의 요청이 이 시간 이내에 완료됨)
- **에러율**: 실패한 요청의 비율
- **락 경합률**: DB 락으로 인한 충돌이 발생한 요청의 비율(집중 테스트에서만)

## 테스트 실행 방법

### 전체 테스트 스위트 실행:

```bash
bash run-order-tests.sh
```

### 개별 테스트 실행:

```bash
# 분산 주문 테스트
k6 run test-sparse-order-load.js

# 집중 주문 테스트
k6 run test-concentrated-order-load.js

# 통합 시나리오 테스트
k6 run order-test-suite.js
```

## 테스트 결과 해석

테스트 결과는 `reports` 디렉토리에 JSON 형식으로 저장됩니다. 다음 지표를 중점적으로 확인하세요:

- **분산 주문 vs 집중 주문**: 두 시나리오 간의 성능 차이는 DB 락 경합 메커니즘의 효율성을 나타냅니다.
- **P99 vs P50**: 극단적인 값(P99)과 일반적인 경우(P50)의 차이가 클수록 시스템 응답 시간의 변동성이 큰 것을 의미합니다.
- **에러율 증가**: 부하가 증가함에 따라 에러율이 급격히 증가하는 지점이 시스템의 한계 용량을 나타냅니다.

## 테스트 커스터마이징

각 스크립트 내에서 다음 파라미터를 조정하여 테스트를 커스터마이징할 수 있습니다:

- VU(Virtual User) 수
- 테스트 기간
- 상품 및 사용자 데이터 세트
- 요청 빈도
- 임계값(thresholds)

## 주의 사항

- 테스트는 로컬 개발 환경이 아닌 테스트 환경에서 실행하는 것이 좋습니다.
- 실제 프로덕션 환경에 대한 테스트는 충분한 주의를 기울여 진행해야 합니다.
- 테스트 전에 적절한 테스트 데이터(상품, 사용자, 쿠폰 등)가 준비되어 있어야 합니다.
