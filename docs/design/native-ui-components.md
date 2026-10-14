# Shared native UI for gym assystant

Integration contract for the revision-34 native implementation. All declarations below are in `it.matteogrechi.gymassystant.ui`; screen state, validation and navigation remain with their callers. Compose Material 3 uses the coordinator's BOM `2025.10.00` and Kotlin `2.2.20`.

```kotlin
@Composable fun GymTheme(mode: String, content: @Composable () -> Unit)

enum class ButtonEmphasis { Filled, Tonal, Outlined, Text }
@Composable fun GymButton(
    label: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: ButtonEmphasis = ButtonEmphasis.Filled,
    enabled: Boolean = true, icon: ImageVector? = null,
    horizontalPadding: Dp? = null,
)

@Composable fun Heading(
    title: String, back: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backLabel: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge,
)

@Composable fun Selector(
    labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
)

@Composable fun PlanningTabs(
    selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
)

@Composable fun LibraryRow(
    title: String, subtitle: String, badge: String? = null,
    onClick: () -> Unit, modifier: Modifier = Modifier,
)

@Composable fun Disclosure(
    title: String, modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable fun Field(
    label: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    readOnly: Boolean = false, singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: String? = null, isError: Boolean = false,
    placeholder: String? = null,
    containerColor: Color? = null,
)

@Composable fun DropdownField(
    label: String, options: List<String>, selected: Int,
    onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false,
    supportingText: String? = null, isError: Boolean = false,
)
```

`mode` accepts `system`, `light`, or `dark`; unknown modes follow the system. `PlanningTabs` uses common localized Plans / Exercises resources and indices 0 / 1. Timer modes and metrics must both call `Selector` rather than create their own selected styling. Selection indices are caller-owned; an out-of-range index shows no selected value. Empty selectors render nothing; empty dropdowns are disabled.

Use `GymColors` inside composables for the CSS roles beyond Material's color scheme, and `GymIcons` for the small bundled stroke-vector icon vocabulary. `GymButton(icon = GymIcons.Add, emphasis = ButtonEmphasis.Tonal, …)` supplies the identical Add action for both planning libraries. Timer actions pass `horizontalPadding = 12.dp`; null preserves the default emphasis-specific padding. Decorative icons have no separate spoken label. Supply localized destination-specific `backLabel` when needed; its default is the common Back resource. `Heading` supplies no page padding or bottom margin; the screen sets placement and spacing. It handles the mockup's negative 12dp Back inset internally with an extended row, keeping trailing actions aligned with the normal content edge. At 390dp with 24dp screen padding, the Back target starts at 12dp and the title at 64dp. Below 350dp with 16dp padding, the Back target starts at 4dp. Title-only headings keep their normal alignment. Pass `containerColor = GymColors.surfaceContainer` for the seat/setup field; normal plan fields default to `surfaceBright`.

The fixed design measurements are minimums: 48dp controls, 24dp icons, 56dp disclosure summaries, 8/12/16/28dp shapes, full pill rounding, 28sp/35sp Manrope page headings (25sp below 350dp), 15sp section headings and 14sp/20sp DM Sans actions. Text wraps and grows with system font scaling. Read-only fields render plain selectable values instead of editable outlines. Labels remain outside fields, matching the mockup. No dynamic wallpaper colors or runtime font loading are used.

`GymColors` preserves every CSS palette role, including the separate dark `primary` (amber), light `primary` (teal), amber `accent`, fixed teal `hero`, success/warm-up/reps colors and scrim alpha. Material's `primary` maps to `accent`, so native filled buttons also use amber; Material `background` maps to the phone's `surface`, while `GymColors.background` retains the outer mockup background for callers that need it. Both schemes explicitly populate surface container roles, with no tonal elevation tint. Error colors use Material's standard accessible error palette; the mockup has no separate error-color token.

Actions use 24dp horizontal / 10dp vertical padding (text buttons: 12dp horizontal), and selectors use 4dp horizontal / 8dp vertical padding with a 2dp underline separated from the label by 4dp. Library rows use 12dp vertical padding, 6dp title-to-subtitle spacing, 15dp text-to-chevron spacing and a 1dp divider. Their natural DM Sans line heights are 18.2sp for the 14sp title and 14.3sp for the 11sp metadata; badges use 10dp/4dp padding and 8dp top spacing. Fields use a Material decoration box with 12dp padding (dropdown text: 16dp), a 1dp unfocused border and an 8dp shape, rather than the default 56dp Material field. Dropdowns use anchored native menus with 48dp option targets. Disclosures use one full-row native click action, localized expanded/collapsed semantics and a decorative circular 48dp trailing indicator. Timer and metric selectors retain native single-choice semantics and share Left/Right/Home/End keyboard selection.

The font files are unmodified upstream TrueType variable fonts fetched on 13 October 2026 from the official [Google Fonts DM Sans directory](https://github.com/google/fonts/tree/main/ofl/dmsans) and [Manrope directory](https://github.com/google/fonts/tree/main/ofl/manrope). The actual registered weights are 400/500/600/700/800; DM Sans's optical size is set to 14sp. Fonts are local Android resources, and their original OFL 1.1 notices are bundled independently as raw resources:

| Asset | Bundled location | SHA-256 |
| --- | --- | --- |
| DM Sans | [dm_sans.ttf](../../android/app/src/main/res/font/dm_sans.ttf) | `8cd08d97e89c24d0aa92edd2f0f4c8ee6195eee9b7c9f154865a58b02f0c1c0d` |
| Manrope | [manrope.ttf](../../android/app/src/main/res/font/manrope.ttf) | `3ae11c49db0455a3cc33e37d380f20fdb8c7f8b41dc07625c177e3d87a9d6ae6` |
| DM Sans license | [dm_sans_ofl.txt](../../android/app/src/main/res/raw/dm_sans_ofl.txt) | Original upstream OFL notice |
| Manrope license | [manrope_ofl.txt](../../android/app/src/main/res/raw/manrope_ofl.txt) | Original upstream OFL notice |

Validation on 13 October 2026: the integrated app builds with Kotlin 2.2.20 and Compose BOM 2025.10.00 (Material 3 1.4.0). Bundled fonts have valid TrueType tables, required weight ranges and Italian/Spanish accented glyphs; their OFL licenses are included. TTF entries are uncompressed in the APK to avoid slow resource reads. Common XML resources have matching nonempty keys in English, Italian and Spanish. Actual visual/device evidence and remaining platform checks are recorded in [the native review](../reviews/native-implementation-review.md); compilation alone does not establish pixel parity. The implementation uses the official [segmented-button API](https://developer.android.com/develop/ui/compose/components/segmented-button) and [secondary tab API](https://developer.android.com/develop/ui/compose/components/tabs).
