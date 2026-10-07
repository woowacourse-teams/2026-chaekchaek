# Approved visual direction

## Product character

ChaekChaek is a mobile reading app with a small white sparrow mascot. The interface should feel calm, literary, direct, and slightly playful. The mascot provides warmth while book covers and readers' words remain the main content.

## Foundation

- Design mobile app screens, not responsive marketing webpages.
- Work in the light theme first.
- Use white or near-white as the page base, true near-black for text and dark surfaces, and vivid orange as the single active accent.
- Use Gowun Dodum Regular for text and Pretendard for text-based icons, following the current Figma styles. Preserve vendor-owned login controls.
- Use rounded forms selectively. Cards, search fields, sticky reading state, and the mascot may be rounded, but avoid applying one radius to every element.
- Use real book-cover proportions. Do not shrink covers until they become decorative thumbnails.

## Accepted patterns

- Keep the sparrow logo visible in the app bar, but small enough that it does not compete with content.
- On the home screen, use a physical-looking book collage. The selected book stays centered and upright while surrounding books may rotate.
- Keep vertical space compact, especially above and below the collage.
- Put a book cover beside its title, reader identity, time, and reflection rather than separating the book from the writing.
- Keep repeated reflection cards the same height. Clamp reflection copy to three lines and use an ellipsis for overflow.
- Place continuation state in a dark sticky bar above the bottom navigation. Use small overlapping reader avatars and a direct phrase to show that new reflections appeared on the book being read.
- Use the current Figma four-tab navigation: 홈, 피드, 발견, 내 서재. Inspect its selected state rather than carrying forward the old orange-point prototype.
- Use orange for progress, new activity, and the primary next action.

## Rejected patterns

- Dark-theme-first exploration.
- Website or editorial landing-page references that do not map to an app screen.
- Large mascot marks that overpower book content.
- Decorative section subtitles such as mood copy, category explanations, or slogans.
- Recent-reflection layouts where the cover is either too small to recognize or so large that it dominates the writing.
- Cards whose height changes freely with reflection length.
- Ambiguous social sections that look like replies to the user's own latest reflection.
- Large gaps, widely spaced page indicators, and metadata broken into unnecessary rows.

## Current canonical home artifact

Use the latest approved screen in the [책췍 Figma file](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr) as the source of truth. Local HTML mockups are historical references. The accepted home direction includes:

- compact top branding with the white sparrow;
- compressed book collage with an upright center cover;
- cover-led recent reflections with title and reader metadata beside the cover;
- three-line reflection clamps and equal card height;
- the reader-avatar version of the sticky continuation bar;
- compact four-tab navigation matching the current Figma selected state.

## Design review questions

- Does every element describe a real book, reader, reading state, or action?
- Is the book cover large enough to recognize without crowding the text?
- Is the orange accent carrying a useful state or action?
- Can repeated rows be scanned without their height jumping?
- Does the screen still work at 320px without losing titles, metadata, or touch targets?
- Does the result look like the same app as the approved home screen?
