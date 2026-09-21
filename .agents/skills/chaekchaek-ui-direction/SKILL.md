---
name: chaekchaek-ui-direction
description: Apply the approved ChaekChaek light-theme visual direction when creating or revising mobile app screen concepts, static HTML mockups, or design handoff guidance. Use for the current rebrand exploration, not the legacy ChaekChaek design system.
---

# ChaekChaek UI Direction

Use this as the visual decision layer for ChaekChaek mobile UI work. It records choices the user has already accepted or rejected so later screens feel like the same app.

Before designing, read [references/visual-direction.md](references/visual-direction.md). Also inspect the current approved mockup when it is available, because the latest accepted artifact takes precedence over this written summary.

## Workflow

1. Identify the screen's actual content and actions from product specs or implementation. Preserve all real content that must remain available.
2. Make a light-theme static HTML mockup first when exploring a new screen. Do not import the production design system, theme hooks, or legacy layout components into that prototype.
3. Carry forward the approved shell: compact mascot branding, black and white foundation, orange used for action or state, restrained typography, and the three-item bottom navigation.
4. Spend visual emphasis on one screen-specific device. Keep the surrounding structure quiet and compact.
5. Review at 390px and 320px widths. Check overflow, image loading, fixed or sticky elements, text clamping, and the relative size of repeated cards.
6. Show alternatives only for a genuine unresolved decision. Group them into one comparison page, label the behavioral difference plainly, and promote the selected option into the canonical screen.

## Boundaries

- Treat this skill as the current redesign direction. Do not invoke or copy the legacy `chaekchaek-design-system` rules unless the user explicitly asks to translate a chosen mockup into production code.
- Keep prototypes outside the application source tree unless the user asks for implementation.
- Do not add decorative subtitles or explanatory copy merely to fill space.
- Do not turn the screen into a marketing webpage, dashboard, or generic card gallery.
