---
name: chaekchaek-design-system
description: "Enforce design-first UI work with the existing Chaekchaek design system. Use for Pencil designs.pen work, Node ID or screen edits, Android and iOS-reachable Compose UI, native iOS UI, frontend UI, colors, typography, spacing, layout, and component changes. Use Apple HIG semantic text roles for user-facing typography. Never implement UI directly from prose: inspect SxMn5, create or reuse a design draft, screenshot the target, then implement with existing tokens and components."
---

# Chaekchaek Design System

Use `designs.pen` frame `SxMn5` as the canonical design source.

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
3. Find a matching target design in `designs.pen`. If none exists, create the smallest complete design draft before touching Android or frontend implementation files.
4. Reuse an existing component or instance first, then existing tokens. Do not recreate an equivalent component or introduce a visual value already covered by the system.
   For user-facing text, reuse one of the 11 semantic typography roles; do not reuse legacy S/M/L typography tokens or add a raw size outside those roles.
5. If the system has no matching value and the choice changes the result, ask the user before adding it. Add an approved reusable value to the design system before using it elsewhere.
6. Verify the smallest meaningful target with a screenshot in the same task turn before implementation. A screenshot of only `SxMn5` or the whole document does not count as a target draft.
7. Only after that screenshot, implement the UI by mapping the draft to existing project theme and components. Never create or modify UI directly from prose alone.
8. For an iOS surface, verify the default Dynamic Type size and AX5 with `$ios-simulator-validation`. Include `performAccessibilityAudit()` for the reached screen when the test environment supports it.
9. Modify only the requested scope. Keep a modified root frame in placeholder mode until the work is complete. Check alignment, spacing, contrast, clipping, hit targets, and requested default states.

The hook blocks UI implementation patches until a target screenshot from `designs.pen` has been produced in the current turn. Do not bypass it. If an approved new color or font is required, update `SxMn5` and the guard together.

Do not claim HIG compliance when the required Simulator state, screenshot, or accessibility audit could not be completed. Report the affected check as unverified and include the blocking reason.
