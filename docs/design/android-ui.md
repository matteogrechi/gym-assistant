# Android UI conventions — GYM-015

Checked against official guidance on 13 October 2026. This browser mockup approximates Material 3; it is not a native Android implementation or a claim of complete platform compliance.

Android defines Material 3 around color, typography, shapes, and standard components. A coherent branded theme can customize these together. The current sunrise-amber/deep-teal theme follows this approach. [Android: Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)

Android distinguishes filled, filled tonal, outlined, and text buttons by emphasis. The shared mockup components use centered, sentence-case 14-pixel labels, medium weight, consistent padding, and full rounding. Amber remains the branded filled-action color; teal tonal fills distinguish supporting actions. Hover, press, keyboard focus, and disabled states are explicit. These are browser approximations of Material components. [Android: Buttons](https://developer.android.com/develop/ui/compose/components/button)

| App action | Material component choice |
| --- | --- |
| Open workout, Start/Resume/Finish session (Finish is outlined while Paused), timer Start/Pause/Resume, Save plan | Filled button |
| Add session or catalog exercise | Filled tonal button |
| Inline Add warm-up | Text button beside the set heading |
| Download status JSON | Filled button |
| Change save folder; download statistics CSV | Outlined button |
| Download recovery guide | Text button |
| Pause session; Finish while Paused | Outlined button |
| Today, Reset, video access, recovery guide | Text button |
| Back on secondary screens | Standard circular icon button |
| Previous/next week | Outlined icon button |
| Exercise sorting in Plan and Train | 48px outlined drag handle with grip icon; touch/mouse drop and ArrowUp/ArrowDown keyboard support |
| Remove session template | Standard icon button with a trash icon |
| Set completion | Toggle icon button with checked fill and pressed semantics |
| Inspect a dated performance point | Circular 48px target with selected ring, pressed semantics and keyboard support |
| Execution/Recovery and progress metric selection | Joined single-choice segmented buttons with a selected check and tonal fill |

Icon actions use consistent 24-pixel SVGs, 48-pixel targets, and accessible names. The completion control preserves its binary pressed state. [Android: Icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button)

Timer and metric choices form single-choice groups. Each selected segment shows a check as well as a tonal background, so selection is not conveyed by color alone. [Android: Segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button)

Lists, the calendar, and bottom navigation retain their own component shapes rather than adopting action-button pills. The theme scale is 8, 12, 16, and 28 CSS pixels plus full rounding. [Android: Material components](https://developer.android.com/design/ui/mobile/guides/components/material-overview)

Train, Plan, and Progress are bottom navigation destinations, with persistent labels, 24-pixel icons, a rounded selected indicator, and an 80-pixel bar outside the scroll area. Keep the top app identity and bottom navigation visible while the content scrolls. The three destinations distinguish execution, plan management, and performance review. Settings is a secondary action in the persistent top-right three-dot overflow menu; its Back action returns to the prior screen. The overflow trigger uses a 24px icon in a 48px target, and its single Settings item stays within the phone width and supports keyboard and outside dismissal. [Android: Navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)

Standard interactive targets have a minimum of 48 × 48 CSS pixels for this phone-sized preview, including completion buttons, timer controls, and selectors. Per the user’s request to show the entire week without horizontal scrolling, calendar date cells use seven equal columns: their width can be below 48 CSS pixels on narrow previews, while height stays at least 48 pixels. This is a documented prototype exception; touch areas do not overlap. Native Android implementation must measure in dp and use platform accessibility semantics. The preview's CSS pixels are a design approximation rather than a physical-size guarantee on arbitrary browsers. [Android: Accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

The future Android app should use native Material components and verify screen-reader behavior, display/font scaling, insets, and system Back. These checks are deferred with Android implementation. The mockup already provides focus states, labels, programmatic selected/pressed states, textual warm-up labels, and a native browser dialog.

GYM-021 keeps timer actions compact, uses short visible Pause/Finish labels with full accessible names, and uses browser disclosure controls for occasional settings and metric definitions. Disclosure summaries retain 48px targets and visible keyboard focus. Invalid collapsed fields expand before validation focus. These disclosures are prototype presentations; native Android equivalents remain undecided.

GYM-022 adds System default/Light/Dark appearance through shared color roles. The dark palette retains warm amber actions and teal surfaces; checked text pairs exceed 4.5:1 and input outlines/chart pairs exceed 3:1 in both palettes. Native behavior follows [Android dark-theme guidance](https://developer.android.com/develop/ui/views/theming/darktheme). Language offers System default/English/Italiano/Español; the browser translates only Settings/navigation with explicit region language metadata and a preview notice. Native full localization follows [Android per-app languages](https://developer.android.com/guide/topics/resources/app-languages). GYM-023's folder picker dialog is a labeled prototype preview; native folder access uses the [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files).

GYM-025 standardizes Back as a 48px circular icon in compact secondary headings, with destination labels. Save retains the filled shape but is disabled until a draft changes. Finished log status uses static check/dash symbols rather than completion buttons; recorded values remain legible without editable-field outlines. Browser number spinners are hidden without changing keyboard input or validation, and the unavailable video frame has no play symbol. Contextual differences between reorder, delete and primary actions are retained.

Revision 13 enlarges every expand/collapse affordance: a full-width row with minimum 56px height, 14px label, and a 24px chevron in a 48px circular indicator at the right. The entire summary remains the native keyboard-accessible toggle; the circle is decorative rather than a second nested button. An upward chevron indicates expansion. Labels wrap, both themes share the treatment, and the indicator remains present after translation.

Revision 16 confines app-owned modal presentation to the simulated phone display. Each native browser dialog supplies modal focus/Escape semantics, but its outer surface matches the phone's inner bounds and acts as the scrim; the page-wide native backdrop is transparent. A centered card with 16px minimum edge spacing scrolls internally on short displays, with shadows clipped to the phone. Resize/page-scroll observers retain alignment. This models the app window on desktop rather than centering the popup over the entire presentation. Native Android components remain future delivery.

Revision 19 improves VIEW-007 with a 180px plot, 12px axis/date labels, a 14px metric heading and a live selected-date/value readout. Touch targets are 48px and keep their spacing through internal panning for dense dates; the value axis stays visible. Arrow keys and native Enter/Space activation provide keyboard inspection without a popup. These are browser plot controls; future native chart implementation remains undecided.
