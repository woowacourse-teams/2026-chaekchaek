---
name: code-quality-review
description: >
  현재 브랜치의 커밋을 push하고, 이번 push에 포함된 변경사항을 기준으로
  코드 리뷰를 수행한 후 리뷰 결과를 GitHub에 기록한다.
  개발자가 수동으로 코드 리뷰를 요청할 때 사용한다.
---

# Code Quality Review
## 참고할 코드 리뷰 지침
- frontend/AGENTS.md 의 ## Code Review 를 코드 리뷰시 지침으로 삼는다

## 프로세스
  - push 이전의 원격 브랜치 커밋을 확인한다
  - 현재 브랜치의 commit 을 push 한다
  - push 이전과 이후의 commit 사이의 변경사항을 리뷰 대상으로 삼는다
  - 여러 commit이 포함된 경우 개별적으로 리뷰하지 않고 누적된 변경사항을 기준으로 리뷰한다
  - 리뷰 결과를 GitHub의 commit comment 로 기록한다
    - 특정 코드에 대한 리뷰는 해당 코드가 변경된 commit에 inline comment로 남긴다
    - 전반적인 설계 및 구현 접근 방식에 대한 리뷰는 이번 push의 마지막 commit에 남긴다
