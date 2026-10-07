---
name: chaekchaek-design-system
description: "책췍 UI 디자인 생성과 코드 적용 전에 Figma 화면, 공통 컴포넌트, 레퍼런스를 확인한다. 누락된 화면과 상태, 재사용 컴포넌트 및 레퍼런스 인스턴스를 먼저 등록하고 검증한다. Android, iOS, frontend UI와 기존 구현의 Figma 역반영에 사용한다."
---

# Chaekchaek Design System

원본은 [책췍 Figma](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr)이며 공통 컴포넌트와 변수는 [책췍 디자인 시스템](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=901-300)에 있다. 이번 작업에서 사용자가 지정한 최신 승인 화면을 우선한다. Pencil과 `designs.pen`은 과거 이력이며 현재 원본 또는 구현 게이트가 아니다.

노드는 보이는 이름과 Figma 링크로 식별한다. 공통 컨트롤 레퍼런스는 [02 Controls](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=905-153)이다. 확인 방법과 기존 선택 컨트롤 매핑은 [Figma 등록 확인](references/figma-registration.md)을 읽는다.

## 현재 승인 시각 기준

현재 승인된 디자인 시스템 원본은 Figma 파일 `tn59Thk2GRcVLkzoO8k9Sr`의 `901:300`이다. 다른 노드나 후속 사용자 결정이 지정되면 그 범위에서만 새 기준을 우선한다.

- 캔버스는 `#FFFFFF`, 본문은 `#191919`, 주요 행동 표면은 `#1C1C1C`를 사용한다.
- 강조색은 `#FF8500`, 보조 표면은 `#F3F3F5`, 보조 글자는 `#666666`, 메타 글자는 `#777777`, 구분선은 `#DDDDE1`을 사용한다.
- 주요 간격은 4, 8, 12, 16, 24, 32이며 화면 좌우 기본 여백은 24이다.
- 승인 모서리 반경은 용도별로 12, 15, 16, 18, 20, 22이다. 임의의 장식 반경을 추가하지 않는다.
- 한글 본문은 Gowun Dodum, 문자로 표현한 아이콘은 Pretendard를 사용한다. 공급업체 소유 네이티브 컨트롤은 아래 예외 규칙을 따른다.
- 그림자와 블러는 표지, 이어서 읽기 스티키, 하단 내비게이션처럼 원본이 지정한 표면에만 사용한다.

## 브랜드와 앱 아이콘

- 흰색 새와 주황색 책으로 구성된 `android/shared/src/commonMain/composeResources/drawable/mascot_outline_b.png`를 현재 저장소의 브랜드 마스코트 원본으로 사용한다. 사용자가 더 최신 원본을 지정하면 그 자산을 우선한다.
- 은은한 크림색 종이 배경과 갈색 참새가 들어간 이전 `app_logo_square` 계열을 새 아이콘 시안의 기준으로 사용하지 않는다.
- 앱 아이콘 시안은 마스코트의 흰 몸, 검정 윤곽과 부리, 주황색 책을 바꾸지 않고 승인 팔레트의 단색 표면에서만 배경을 탐색한다.
- 앱 아이콘에는 글자, 추가 물체, 종이 질감, 새 그림자, 그라데이션을 임의로 넣지 않는다.
- 플랫폼 마스크가 모서리를 자르므로 시안 파일 자체에 둥근 모서리를 굽지 않는다. 핵심 실루엣과 책이 안전 영역 안에서 잘리지 않는지 정사각형 원본과 실제 플랫폼 렌더에서 확인한다.
- Figma `901:300`에는 앱 아이콘 자체의 최종 배경과 배율이 정의되어 있지 않다. 팔레트를 활용한 후보는 탐색안이며, 사용자가 선택하기 전에는 실제 Android 또는 iOS 앱 아이콘 자산에 반영하지 않는다.

## 글로벌 타이포그래피 기준

Figma, Android, iOS, frontend에서 앱이 소유한 모든 사용자 표시 텍스트는 같은 의미 기반 역할을 사용한다. 플랫폼별 렌더링과 접근성 동작은 각 플랫폼 규칙을 따른다.

최신 승인 Figma의 기존 스타일을 먼저 확인하고 아래 과거 수치와 다르면 임의로 덮어쓰지 않는다. 아래 공식 기준은 2026-08-29에 확인한 기록이다.

- [Apple Human Interface Guidelines](https://developer.apple.com/kr/design/human-interface-guidelines)
- [Typography](https://developer.apple.com/kr/design/human-interface-guidelines/typography)
- [Accessibility](https://developer.apple.com/kr/design/human-interface-guidelines/accessibility)
- [UI Design Tips](https://developer.apple.com/design/tips/)

앱이 소유한 사용자 표시 텍스트는 Large Title 34/41, Title 1 28/34, Title 2 22/28, Title 3 20/25, Headline 17/22 Semibold, Body 17/22, Callout 16/21, Subhead 15/20, Footnote 13/18, Caption 1 12/16, Caption 2 11/13 중 하나의 의미 역할에 연결한다. 숫자는 기본 크기의 font size/line height pt이다. 기존 S/M/L 이름과 역할 밖의 raw 크기를 새 디자인이나 구현에 사용하지 않는다. 직접 누르는 컨트롤은 시각 크기와 별개로 최소 44x44pt hit target을 확보한다. 텍스트와 레이아웃은 Dynamic Type 기본 크기부터 AX5까지 유지되어야 한다.

Apple 또는 Google 등 공급업체가 소유한 네이티브 로그인 컨트롤의 내부 글꼴, 크기, 굵기, 로고와 색상은 임의로 재구현하거나 덮어쓰지 않는다. 화면 배치, 외부 여백, 가시성과 접근 가능한 hit target만 앱 디자인 시스템에서 다룬다.

## Workflow

1. 해결할 문제, 대상 화면과 상태, 생성 또는 적용할 UI를 짧게 알린다. 결과를 바꿀 미확정 사항은 먼저 확인한다.
2. 전용 Figma MCP와 해당 도구의 필수 스킬을 사용해 최신 대상 화면 및 `901:300` 디자인 시스템을 조회한다. 원본 조회 없이 코드나 과거 캡처만 보고 새 UI를 적용하지 않는다.
3. 화면 또는 필요한 상태가 없으면 먼저 Figma에 생성한다. 현재 구현을 역반영하는 요청은 지정된 코드와 실행 캡처를 기준으로 하되, 플랫폼 렌더링 차이는 명시한다.
4. 공통 요소마다 메인 컴포넌트/컴포넌트 세트와 공통 레퍼런스를 별도로 확인한다. 라이브러리 검색에 없더라도 디자인 시스템 페이지의 로컬 노드를 확인한다. 기존 요소가 있으면 재사용하며, 누락된 상태는 기존 세트에 추가한다.
5. 없는 공통 요소는 변수, 텍스트 스타일, 기존 아이콘을 재사용해 메인 컴포넌트로 만든다. 공통 레퍼런스에도 연결된 인스턴스로 상태별 예시를 등록한다. 화면 속 단순 프레임이나 전체 UI 이미지는 컴포넌트 등록으로 인정하지 않는다.
6. 대상 화면과 공통 레퍼런스를 캡처하고 인스턴스의 메인 컴포넌트 연결, 상태, 변수/스타일, 글꼴, 잘림과 겹침을 확인한다. 영향을 받지 않은 영역까지 다시 만드는 작업은 하지 않는다.
7. 화면, 메인 컴포넌트, 레퍼런스의 링크와 검증 결과를 확보한 뒤에만 코드를 적용한다. 접근 또는 등록 실패 시 해당 UI 적용을 멈추고 미완료 항목과 원인을 알린다.
8. iOS 구현 검증에는 `$ios-simulator-validation`으로 기본 Dynamic Type과 AX5를 확인한다. 지원 환경에서는 `performAccessibilityAudit()`도 실행한다. Figma 등록만 요청받았다면 앱 빌드나 배포를 완료 범위에 추가하지 않는다.
9. 변경한 placeholder는 완료 시 해제한다. Figma 등록, 코드 적용, 디바이스 검증, 팀 라이브러리 게시를 각각 구분해 보고한다.

자동 디자인 스크린샷 훅은 폐기된 상태를 유지한다. 위 절차는 에이전트의 필수 작업 규칙이며 도구 호출을 자동 차단하는 훅이 아니다. 승인된 새 토큰이 필요하면 Figma 디자인 시스템에 먼저 등록한다.

Do not claim HIG compliance when the required Simulator state, screenshot, or accessibility audit could not be completed. Report the affected check as unverified and include the blocking reason.
