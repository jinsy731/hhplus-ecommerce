name: Feature / 설계 제안
description: 새로운 기능 또는 설계 요소를 정의하고 논의합니다.
title: "[Feature] "
labels: ["feature", "discussion", "design"]
assignees: []

body:
  - type: textarea
    attributes:
      label: 🧩 개요
      description: "무엇을 위한 작업인지 간단히 설명해주세요."
      placeholder: "이 기능/설계는 어떤 문제를 해결하기 위함인가요?"
    validations:
      required: true

  - type: textarea
    attributes:
      label: 🎯 목적 및 배경
      description: |
        - 어떤 문제/요구사항을 해결하려는 건가요?
        - 왜 지금 이 시점에 설계가 필요한가요?
      placeholder: |
        - 예: 유저의 주문 흐름에 쿠폰 적용을 도입하기 위함
        - 예: 현재 상태 전이가 복잡해져 명확한 모델링 필요
    validations:
      required: true

  - type: textarea
    attributes:
      label: 🛠 주요 변경 사항 / 고려 대상
      description: 설계 또는 기능 구현 시 고려할 핵심 요소를 기술해주세요.
      placeholder: |
        - 예: 쿠폰의 상태관리 방식
        - 예: 주문 시점의 재고 보장 처리
        - 예: 트랜잭션 처리 범위
    validations:
      required: false

  - type: textarea
    attributes:
      label: 🧪 예상되는 사이드 이펙트
      description: 이슈 해결 시 영향을 받는 영역이 있다면 미리 적어주세요.
      placeholder: |
        - 기존 API 응답 포맷 변경 가능성
        - 주문 통계 갱신 방식 변경
    validations:
      required: false

  - type: textarea
    attributes:
      label: 📝 TODO
      description: 작업 단위를 나눠서 관리해주세요.
      value: |
        - [ ] 설계 구조 초안 작성
        - [ ] 관련 도메인/엔티티 정의
        - [ ] 유즈케이스 흐름 정리
        - [ ] 리뷰 요청 및 피드백 반영
    validations:
      required: false

  - type: textarea
    attributes:
      label: 🔗 관련 문서 / PR
      description: 관련된 문서나 기존 PR, 이슈가 있다면 링크해주세요.
      placeholder: |
        - 설계 정리 문서: [Notion/Docs 링크]
        - 관련 PR: #123
        - 관련 이슈: #456
    validations:
      required: false
