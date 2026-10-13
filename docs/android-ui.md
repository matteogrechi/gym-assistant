# Android UI conventions — GYM-015

Checked against official guidance on 13 October 2026. This browser mockup approximates Material 3; it is not a native Android implementation or a claim of complete platform compliance.

Android defines Material 3 around color, typography, shapes, and standard components. A coherent branded theme can customize these together. The current sunrise-amber/deep-teal theme follows this approach. [Android: Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)

Android distinguishes filled, filled tonal, outlined, and text buttons by emphasis. The shared mockup components use centered, sentence-case 14-pixel labels, medium weight, consistent padding, and full rounding. Amber remains the branded filled-action color; teal tonal fills distinguish supporting actions. Hover, press, keyboard focus, and disabled states are explicit. These are browser approximations of Material components. [Android: Buttons](https://developer.android.com/develop/ui/compose/components/button)

| App action | Material component choice |
| --- | --- |
| Open workout, Start/Resume/Finish session (Finish is outlined while Paused), timer Start/Pause/Resume, Save plan | Filled button |
| Add warm-up, session, or catalog exercise | Filled tonal button |
| Pause session; Finish while Paused | Outlined button |
| Back, Today, Reset, remove warm-up/exercise text, video access | Text button |
| Previous/next week and exercise move earlier/later | Outlined icon button |
| Remove session template | Standard icon button with a trash icon |
| Set completion | Toggle icon button with checked fill and pressed semantics |
| Execution/Recovery and progress metric selection | Joined single-choice segmented buttons with a selected check and tonal fill |

Icon actions use consistent 24-pixel SVGs, 48-pixel targets, and accessible names. The completion control preserves its binary pressed state. [Android: Icon buttons](https://developer.android.com/develop/ui/compose/components/icon-button)

Timer and metric choices form single-choice groups. Each selected segment shows a check as well as a tonal background, so selection is not conveyed by color alone. [Android: Segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button)

Lists, the calendar, and bottom navigation retain their own component shapes rather than adopting action-button pills. The theme scale is 8, 12, 16, and 28 CSS pixels plus full rounding. [Android: Material components](https://developer.android.com/design/ui/mobile/guides/components/material-overview)

Train, Plan, and Progress are bottom navigation destinations, with persistent labels, 24-pixel icons, a rounded selected indicator, and an 80-pixel bar outside the scroll area. Keep the top app identity and bottom navigation visible while the content scrolls. The three destinations distinguish execution, plan management, and performance review. [Android: Navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)

Standard interactive targets have a minimum of 48 × 48 CSS pixels for this phone-sized preview, including completion buttons, timer controls, and selectors. Per the user’s request to show the entire week without horizontal scrolling, calendar date cells use seven equal columns: their width can be below 48 CSS pixels on narrow previews, while height stays at least 48 pixels. This is a documented prototype exception; touch areas do not overlap. Native Android implementation must measure in dp and use platform accessibility semantics. The preview's CSS pixels are a design approximation rather than a physical-size guarantee on arbitrary browsers. [Android: Accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

The future Android app should use native Material components and verify screen-reader behavior, display/font scaling, insets, and system Back. These checks are deferred with Android implementation. The mockup already provides focus states, labels, programmatic selected/pressed states, textual warm-up labels, and a native browser dialog.
