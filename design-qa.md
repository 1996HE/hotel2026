# Design QA — 白馬樹海 客向けWebサイト

## Visual truth and test state

- Source visual truth: `/Users/heniantong/.codex/generated_images/019fd021-9e6c-7782-984f-d5579297b653/exec-fd847f8f-c1a2-4f81-ac54-f3f5cec8d90a.png`
- Implementation evidence: transient desktop capture reviewed during QA and deleted afterward
- Source image: 1487 × 1058 px
- Implementation capture: 1521 × 1014 px at a 1536 × 1024 CSS viewport
- Mobile evidence: transient 375 × 812 px capture reviewed at a 390 × 844 CSS viewport and deleted afterward
- State: `/stay`, Japanese, scroll position 0, no hover or keyboard focus

## Full-view comparison

The final home page preserves the selected “Alpine Pulse” direction: a snow-mountain lodge hero, dark alpine-navy navigation, coral reservation actions, a compact booking bar overlapping the hero edge, and a two-column room introduction followed by three photographic room cards. The hierarchy, visual rhythm, and youthful ski-resort character match the selected reference closely while remaining responsive and readable.

## Focused evidence

- Hero: full-width generated lodge image, dark gradient overlay, oversized condensed heading, and a clear coral CTA.
- Header: compact dark navigation, Japanese/Chinese/English controls, and a coral reservation button. The coral `A` in `HAKUBA` reproduces the selected brand accent.
- Booking bar: arrival, departure, guests, and submit action remain visible as a single high-priority block on desktop and stack cleanly on mobile.
- Rooms: navy editorial copy block and three real generated room images reproduce the reference's split hierarchy without image distortion.
- Image quality: all six site images are locally served original generated assets; no missing, stretched, or low-resolution images were observed.

## Comparison history

1. Initial comparison found P2 differences: the hero was too tall, the room section began below the first viewport, and the room heading sat above rather than beside the cards.
2. The hero was reduced from 620 px to 520 px, the room introduction was changed to a two-column composition, card alignment was tightened, and the coral brand `A` was added.
3. Final desktop and mobile comparisons found no remaining P0, P1, or P2 fidelity issues.

## Fidelity checklist

| Area                           | Result | Evidence                                                                             |
| ------------------------------ | ------ | ------------------------------------------------------------------------------------ |
| Typography                     | Passed | Strong condensed display hierarchy with readable system-font multilingual body copy  |
| Spacing and layout             | Passed | Hero, booking bar, editorial split, and room cards align with the reference rhythm   |
| Color                          | Passed | Alpine navy, glacier blue, snow white, and coral CTA values are consistently applied |
| Image quality                  | Passed | Generated assets load at appropriate crops without distortion                        |
| Copy and information hierarchy | Passed | Japanese default plus complete Chinese and English switching across all nine pages   |

## Interaction and responsive checks

- All nine public routes open without authentication.
- Japanese, Chinese, and English switch immediately; the selected language persists across pages and updates the document language.
- Home search parameters carry into the reservation demo.
- Reservation form validation and the demo-completion state both work.
- Mobile menu opens and closes correctly.
- The booking lookup page verifies request number plus email, shows two-room totals and status, and keeps credentials out of the URL.
- Whole-request cancellation remains disabled until a reason and acknowledgement are entered, then opens a separate final-confirmation dialog.
- Booking lookup and cancellation controls were checked in Japanese, Chinese, and English; singular and plural English guest labels render correctly.
- No horizontal overflow was found at 1536 × 1024 or 390 × 844.
- Browser console errors and warnings: none.

## Residual P3 differences

- The reference uses distressed display lettering and more aggressive diagonal cuts around the booking and room sections. The implementation uses clean system-compatible typography and rectangular, accessible controls so Japanese, Chinese, and English remain legible and maintainable.
- These are intentional polish differences and do not affect the selected visual direction or primary user journey.

final result: passed
