---
name: chaekchaek-ui-direction
description: Apply the approved 책췍 light-theme visual direction to mobile screen concepts and handoffs after checking the current Figma source and shared components.
---

# ChaekChaek UI Direction

Use this as the visual decision layer for ChaekChaek mobile UI work. It records choices the user has already accepted or rejected so later screens feel like the same app.

Before designing, read [references/visual-direction.md](references/visual-direction.md) and [chaekchaek-design-system](../chaekchaek-design-system/SKILL.md). Inspect the current approved Figma screen and shared component reference. Register missing designs, shared components, and reference instances in Figma before code application.

## Workflow

1. Identify the screen's actual content and actions from product specs or implementation. Preserve all real content that must remain available.
2. Create or reuse a light-theme Figma draft with the current variables and components. HTML exploration, when requested, does not replace the Figma registration check.
3. Carry forward the approved shell: compact mascot branding, black and white foundation, orange used for action or state, Gowun Dodum text, and the four tabs 홈, 피드, 발견, 내 서재.
4. Spend visual emphasis on one screen-specific device. Keep the surrounding structure quiet and compact.
5. Review at 390px and 320px widths. Check overflow, image loading, fixed or sticky elements, text clamping, and the relative size of repeated cards.
6. Show alternatives only for a genuine unresolved decision. Group them into one comparison page, label the behavioral difference plainly, and promote the selected option into the canonical screen.

## Boundaries

- The current Figma source and `chaekchaek-design-system` registration workflow take precedence over older mockups and exploration notes.
- Keep prototypes outside the application source tree unless the user asks for implementation.
- Do not add decorative subtitles or explanatory copy merely to fill space.
- Do not turn the screen into a marketing webpage, dashboard, or generic card gallery.
