---
name: chaekchaek-design-system
description: "Enforce design-first UI work with the existing Chaekchaek design system. Use for Pencil designs.pen work, Node ID or screen edits, Android and iOS-reachable Compose UI, native iOS UI, frontend UI, colors, typography, spacing, layout, and component changes. Use Apple HIG semantic text roles for user-facing typography. Never implement UI directly from prose: inspect SxMn5, create or reuse a design draft, screenshot the target, then implement with existing tokens and components."
---

# Chaekchaek Design System

Use the `designs.pen` frame named `책췍 디자인 시스템` (Node ID: `SxMn5`) as the canonical design source.

When communicating with the user, refer to Pencil nodes by their visible names. Include a Node ID in parentheses only when it is needed for identification or an operation; never use the ID alone when a visible name is available.

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

`designs.pen`, Android, iOS, frontend에서 앱이 소유한 모든 사용자 표시 텍스트는 같은 의미 기반 역할을 사용한다. 플랫폼별 렌더링과 접근성 동작은 각 플랫폼 규칙을 따른다.

공식 기준은 2026-08-29에 확인했다.

- [Apple Human Interface Guidelines](https://developer.apple.com/kr/design/human-interface-guidelines)
- [Typography](https://developer.apple.com/kr/design/human-interface-guidelines/typography)
- [Accessibility](https://developer.apple.com/kr/design/human-interface-guidelines/accessibility)
- [UI Design Tips](https://developer.apple.com/design/tips/)

앱이 소유한 사용자 표시 텍스트는 Large Title 34/41, Title 1 28/34, Title 2 22/28, Title 3 20/25, Headline 17/22 Semibold, Body 17/22, Callout 16/21, Subhead 15/20, Footnote 13/18, Caption 1 12/16, Caption 2 11/13 중 하나의 의미 역할에 연결한다. 숫자는 기본 크기의 font size/line height pt이다. 기존 S/M/L 이름과 역할 밖의 raw 크기를 새 디자인이나 구현에 사용하지 않는다. 직접 누르는 컨트롤은 시각 크기와 별개로 최소 44x44pt hit target을 확보한다. 텍스트와 레이아웃은 Dynamic Type 기본 크기부터 AX5까지 유지되어야 한다.

Apple 또는 Google 등 공급업체가 소유한 네이티브 로그인 컨트롤의 내부 글꼴, 크기, 굵기, 로고와 색상은 임의로 재구현하거나 덮어쓰지 않는다. 화면 배치, 외부 여백, 가시성과 접근 가능한 hit target만 앱 디자인 시스템에서 다룬다.

## Workflow

1. Restate the requested UI change and identify its scope.
2. Inspect `SxMn5` and the target with the Pencil tools before editing a `.pen` file. Never read or edit `.pen` files through shell tools.
   Before inserting a new top-level screen or component, inspect the document order and bounds of its same-kind peers. Append it immediately to the right of the last peer using the sequence's existing gap. Never place it at an arbitrary empty canvas position; only start the next aligned row when the right side would overlap existing content.
3. Find a matching target design in `designs.pen`. If none exists, create the smallest complete design draft before touching Android or frontend implementation files.
4. Reuse an existing component or instance first, then existing tokens. Do not recreate an equivalent component or introduce a visual value already covered by the system.
   For user-facing text, reuse one of the 11 semantic typography roles; do not reuse legacy S/M/L typography tokens or add a raw size outside those roles.
5. If the system has no matching value and the choice changes the result, ask the user before adding it. Add an approved reusable value to the design system before using it elsewhere.
6. Verify the smallest meaningful target with a screenshot in the same task turn before implementation. A screenshot of only `SxMn5` or the whole document does not count as a target draft.
7. Only after that screenshot, implement the UI by mapping the draft to existing project theme and components. Never create or modify UI directly from prose alone.
8. For an iOS surface, verify the default Dynamic Type size and AX5 with `$ios-simulator-validation`. Include `performAccessibilityAudit()` for the reached screen when the test environment supports it.
9. Modify only the requested scope. Keep a modified root frame in placeholder mode until the work is complete. Check alignment, spacing, contrast, clipping, hit targets, and requested default states.

The automatic design screenshot guard was retired at the user's request. Review applicable designs and screenshots without a hook-based implementation gate. If an approved new color or font is required, update SxMn5.

Do not claim HIG compliance when the required Simulator state, screenshot, or accessibility audit could not be completed. Report the affected check as unverified and include the blocking reason.
