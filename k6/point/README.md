# k6 포인트 API 성능 테스트

이 디렉토리에는 이커머스 서비스의 포인트 API에 대한 성능 테스트 스크립트가 포함되어 있습니다.

## 테스트 시나리오

성능 테스트는 다음 두 가지 주요 시나리오로 구성되어 있습니다:

1. **포인트 조회 테스트** (`point_retrieve_test.js`)
   - 사용자의 포인트 잔액 조회 요청을 시뮬레이션
   - 다수의 사용자가 포인트 잔액을 동시에 조회하는 상황 테스트
   - 1~100,000 사이의 무작위 사용자 ID에 대해 포인트 조회 요청 생성

2. **포인트 충전 테스트** (`point_charge_test.js`)
   - 사용자의 포인트 충전 요청을 시뮬레이션
   - 다수의 사용자가 포인트를 동시에 충전하는 상황 테스트
   - 1~100,000 사이의 무작위 사용자 ID에 대해 1,000~100,000 범위의 충전 금액으로 요청 생성

## 모니터링 지표

각 테스트는 다음 핵심 지표를 측정하고 보고합니다:

- **P95 응답 시간**: 95%의 요청이 이 시간 이내에 완료됨
- **API 호출 에러율**: 실패한 API 요청의 비율
- **포인트 조회 응답 시간**: 포인트 잔액 조회 API 호출의 응답 시간 추세
- **포인트 충전 응답 시간**: 포인트 충전 API 호출의 응답 시간 추세
- **포인트 충전 에러 수**: 포인트 충전 중 발생한 오류 횟수

## 테스트 실행 방법

### 개별 테스트 실행:

```bash
# 포인트 조회 테스트
k6 run point_retrieve_test.js

# 포인트 충전 테스트
k6 run point_charge_test.js
```

### 동시 또는 순차적 테스트 실행:

```bash
# 모든 테스트 순차적 실행
bash run-point-tests.sh
```

## 테스트 결과 해석

테스트 결과는 콘솔 출력으로 확인할 수 있으며, 필요에 따라 JSON 또는 CSV 형식으로 저장할 수 있습니다. 다음 지표를 중점적으로 확인하세요:

- **포인트 조회 vs 충전 성능**: 읽기 작업과 쓰기 작업 간의 성능 차이를 비교하여 시스템의 IO 처리 능력 평가
- **P95 응답 시간**: 대부분의 요청이 처리되는 시간으로, 사용자 경험에 직접적인 영향을 미침
- **에러율 증가**: 부하가 증가함에 따라 에러율이 급격히 증가하는 지점이 시스템의 한계 용량을 나타냄
- **동시 사용자 증가에 따른 영향**: 동시 접속자 수 증가에 따른 시스템 성능 변화 파악

## 테스트 커스터마이징

각 스크립트 내에서 다음 파라미터를 조정하여 테스트를 커스터마이징할 수 있습니다:

- VU(Virtual User) 수 및 증가 패턴
- 테스트 단계 및 기간
- 사용자 ID 범위
- 충전 금액 범위
- 임계값(thresholds) 설정
- 요청 간 대기 시간

## 주의 사항

- 테스트는 로컬 개발 환경이 아닌 테스트 환경에서 실행하는 것이 좋습니다.
- 실제 프로덕션 환경에 대한 테스트는 충분한 주의를 기울여 진행해야 합니다.
- 테스트 전에 적절한 테스트 데이터(사용자 계정, 초기 포인트 잔액 등)가 준비되어 있어야 합니다.
- 충전 테스트는 실제 금액과 연결될 수 있으므로 테스트 환경에서만 실행하세요.
