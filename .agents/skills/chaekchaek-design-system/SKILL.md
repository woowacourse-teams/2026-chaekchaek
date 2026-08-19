---
name: chaekchaek-design-system
description: "Enforce design-first UI work with the existing Chaekchaek design system. Use for Pencil designs.pen work, Node ID or screen edits, Android Compose UI, frontend UI, colors, typography, spacing, layout, and component changes. Never implement UI directly from prose: inspect SxMn5, create or reuse a design draft, screenshot the target, then implement with existing tokens and components."
---

# Chaekchaek Design System

Use `designs.pen` frame `SxMn5` as the canonical design source.

## Workflow

1. Restate the requested UI change and identify its scope.
2. Inspect `SxMn5` and the target with the Pencil tools before editing a `.pen` file. Never read or edit `.pen` files through shell tools.
3. Find a matching target design in `designs.pen`. If none exists, create the smallest complete design draft before touching Android or frontend implementation files.
4. Reuse an existing component or instance first, then existing tokens. Do not recreate an equivalent component or introduce a visual value already covered by the system.
5. If the system has no matching value and the choice changes the result, ask the user before adding it. Add an approved reusable value to the design system before using it elsewhere.
6. Verify the smallest meaningful target with a screenshot in the same task turn before implementation. A screenshot of only `SxMn5` or the whole document does not count as a target draft.
7. Only after that screenshot, implement the UI by mapping the draft to existing project theme and components. Never create or modify UI directly from prose alone.
8. Modify only the requested scope. Keep a modified root frame in placeholder mode until the work is complete. Check alignment, spacing, contrast, clipping, and requested default states.

The hook blocks UI implementation patches until a target screenshot from `designs.pen` has been produced in the current turn. Do not bypass it. If an approved new color or font is required, update `SxMn5` and the guard together.
