---
name: screen-specification
description: 책췍 Android와 iOS의 화면 기획서 또는 화면 상세 명세를 작성할 때 현재 승인 Figma와 상태별 데이터 및 동작을 확인한다.
---

# 화면 기획서

작업 범위는 저장소의 `android/`이다. 먼저 `android/AGENTS.md`와 같은 디렉터리의 `chaekchaek-design-system` 스킬을 읽는다.

## 현재 기준

- 원본은 최신 승인 Figma다. Figma 전용 MCP와 필수 스킬로 대상 화면과 상태를 실제 조회한다.
- 탐색 구조는 홈, 피드, 발견, 내 서재 4탭이다. 사용자가 최신 화면을 따로 지정하면 그 범위에서 우선한다.
- 화면, 모달, 팝업별로 표시 데이터, 사용자 행동, 실행 조건, 도착 화면과 복귀 상태를 기록한다.
- API 필드는 최신 서버 OpenAPI와 `android/openapi3.yaml`을 대조한다. 미확인 필드를 지어내지 않는다.
- 지속 보관할 명세는 `android/docs/`에 작성하고 AGENTS.md와 문서 인덱스에서 읽을 조건을 연결한다.
- 새 UI나 상태가 필요하면 디자인 시스템 스킬의 Figma 등록과 캡처 확인 절차를 따른다. 문서 작성만으로 앱 구현 완료를 주장하지 않는다.

## 과거 템플릿

[과거 Pencil 표 템플릿](references/template.md)은 기존 산출물의 구조를 해석하기 위한 보관 자료다. 개인 경로의 designs.pen, MvCYw 노드와 3탭 지침은 현재 원본이나 작업 게이트로 사용하지 않는다.
