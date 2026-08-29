---
name: chaekchaek-design-system
description: "Enforce design-first UI work with the existing Chaekchaek design system. Use for Pencil designs.pen work, Node ID or screen edits, Android and iOS-reachable Compose UI, native iOS UI, frontend UI, colors, typography, spacing, layout, and component changes. Apply Apple HIG to iOS surfaces. Never implement UI directly from prose: inspect SxMn5, create or reuse a design draft, screenshot the target, then implement with existing tokens and components."
---

# Chaekchaek Design System

Use `designs.pen` frame `SxMn5` as the canonical design source.

## iOS HIG 기준

다음 경로에서 렌더링되는 UI에는 Apple HIG를 함께 적용한다.

- `android/shared/src/commonMain/**` 중 iOS에서 사용하는 UI
- `android/shared/src/iosMain/**`
- `android/iosApp/**`
- 위 구현을 위한 `designs.pen`의 iOS 대상 화면과 디자인 시스템

공식 기준은 2026-08-29에 확인했다.

- [Apple Human Interface Guidelines](https://developer.apple.com/kr/design/human-interface-guidelines)
- [Typography](https://developer.apple.com/kr/design/human-interface-guidelines/typography)
- [Accessibility](https://developer.apple.com/kr/design/human-interface-guidelines/accessibility)
- [UI Design Tips](https://developer.apple.com/design/tips/)

iOS의 새 사용자 표시 텍스트는 11pt 미만으로 만들지 않는다. Body 17pt를 기본 본문 크기의 참고점으로 삼되, 모든 역할을 17pt로 통일하지 말고 정보 계층에 맞는 의미 기반 스타일을 사용한다. 기존의 더 작은 토큰은 새 iOS UI로 확산하지 않는다. 직접 누르는 컨트롤은 시각 크기와 별개로 최소 44x44pt hit target을 확보한다. 텍스트와 레이아웃은 Dynamic Type 기본 크기부터 AX5까지 유지되어야 한다.

Apple 또는 Google 등 공급업체가 소유한 네이티브 로그인 컨트롤의 내부 글꼴, 크기, 굵기, 로고와 색상은 임의로 재구현하거나 덮어쓰지 않는다. 화면 배치, 외부 여백, 가시성과 접근 가능한 hit target만 앱 디자인 시스템에서 다룬다.

## Workflow

1. Restate the requested UI change and identify its scope.
2. Inspect `SxMn5` and the target with the Pencil tools before editing a `.pen` file. Never read or edit `.pen` files through shell tools.
3. Find a matching target design in `designs.pen`. If none exists, create the smallest complete design draft before touching Android or frontend implementation files.
4. Reuse an existing component or instance first, then existing tokens. Do not recreate an equivalent component or introduce a visual value already covered by the system.
5. If the system has no matching value and the choice changes the result, ask the user before adding it. Add an approved reusable value to the design system before using it elsewhere.
6. Verify the smallest meaningful target with a screenshot in the same task turn before implementation. A screenshot of only `SxMn5` or the whole document does not count as a target draft.
7. Only after that screenshot, implement the UI by mapping the draft to existing project theme and components. Never create or modify UI directly from prose alone.
8. For an iOS surface, verify the default Dynamic Type size and AX5 with `$ios-simulator-validation`. Include `performAccessibilityAudit()` for the reached screen when the test environment supports it.
9. Modify only the requested scope. Keep a modified root frame in placeholder mode until the work is complete. Check alignment, spacing, contrast, clipping, hit targets, and requested default states.

The hook blocks UI implementation patches until a target screenshot from `designs.pen` has been produced in the current turn. Do not bypass it. If an approved new color or font is required, update `SxMn5` and the guard together.

Do not claim HIG compliance when the required Simulator state, screenshot, or accessibility audit could not be completed. Report the affected check as unverified and include the blocking reason.
