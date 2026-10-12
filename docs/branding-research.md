# gym assystant — motivating brand and psychological evidence

Research date: 12 October 2026. Requirement: GYM-013. Selected direction: a welcoming training companion, with the promise **“Your pace. Your progress.”** The name remains **gym assystant**, exactly as requested.

## Evidence review

This focused review searched PubMed and original research repositories for self-determination theory and exercise, progress monitoring, physical-activity behavior change, and affect during exercise. It is not a systematic review. The studies concern motivational processes or multi-component interventions; none directly establishes the effectiveness of this app name, palette, or slogan.

| Source | Relevant evidence | Design implication and boundary |
| --- | --- | --- |
| [Teixeira et al., 2012 — exercise and self-determination theory](https://pmc.ncbi.nlm.nih.gov/articles/PMC3441783/) | Review of 66 studies found support for autonomous motivation and exercise participation; much of the evidence was observational. | Give the user choice and a sense of competence. “Choose a session” is an invitation. This is a design inference, not proof that a particular sentence improves adherence. |
| [Sheeran et al., 2020 — randomized SDT interventions](https://pubmed.ncbi.nlm.nih.gov/32437175/) | Meta-analysis of 65 independent tests reported small effects on health behavior. Autonomous motivation and perceived competence mediated effects; publication and small-sample biases reduced estimated effects. | Use clear achievable actions and personal feedback. Branding alone cannot reproduce a complete intervention. |
| [Harkin et al., 2016 — monitoring goal progress](https://pubmed.ncbi.nlm.nih.gov/26479070/) | Meta-analysis of 138 randomized studies involving 19,951 participants found improved goal attainment with monitoring interventions, averaging an effect size of 0.40. The studies span multiple goals. | Make actual reps, weight, and progress visible. Monitoring is supported; a compulsory dashboard, competitive score, or public sharing is not required by this finding. |
| [Michie et al., 2009 — behavior-change techniques](https://pubmed.ncbi.nlm.nih.gov/19916637/) | Across 122 physical-activity/healthy-eating intervention evaluations, self-monitoring with additional self-regulation techniques was associated with greater effectiveness. Meta-regression cannot isolate a causal effect of individual UI components. | Put program targets beside actual performance and keep logging easy. This supports a coherent workflow, without promising that adding controls will increase exercise. |
| [Rhodes & Kates, 2015 — affect during exercise](https://pubmed.ncbi.nlm.nih.gov/25921307/) | Review of 24 studies linked more positive affect during moderate-intensity exercise to subsequent activity; postexercise affect did not show the same relationship. | Make the experience comfortable and respectful of pace, including recovery. This association does not demonstrate that a calm color palette changes exercise enjoyment. |

## Chosen identity

- **Name:** gym assystant. A lowercase wordmark with a small amber dot keeps the existing requested name friendly and legible.
- **Promise:** Your pace. Your progress.
- **Personality:** capable, warm, direct. Helpful language, concise instructions, factual feedback.
- **Palette:** sunrise amber `#ffbf69` for start actions and selected navigation; deep teal `#143f46` for text and structure; ivory `#fffaf2` for the main surface; pale aqua `#e6f1f0` for timer and progress panels. Completed sets use teal `#24665f` with white checkmarks, and warm-ups use a soft apricot tint. The color-emotion literature informs this direction; the exact values are design choices, not experimentally validated exercise motivators. See the palette rationale below.
- **Typography:** Manrope headings and DM Sans body/numerals, with local system-font fallbacks. Prominent reps and tabular timer digits support quick reading. External font loading is optional.
- **Shape:** soft corners, generous spacing, restrained upward arrows. No animated urgency, physique photography, or ideal-body comparison.

## Palette rationale — updated color research

The goal is to make entering a workout feel welcoming and the next action clear. A warmer accent replaces the earlier lime/sage direction, while calm surfaces keep dense rep logging readable. The app's supportive language and user control remain the more directly supported motivational mechanisms.

| Source | Relevant finding | Application and limit |
| --- | --- | --- |
| [Jonauskaite & Mohr, 2025 — systematic review of color and emotion](https://link.springer.com/article/10.3758/s13423-024-02615-z) | Reviewed 132 articles with 42,266 participants across 64 countries. Yellow/orange tended to correspond with positive high-arousal emotions; blue/green/blue-green with positive low-arousal emotions. Lightness and saturation also mattered. | Warm amber emphasizes a welcoming next action; pale teal supports quiet logging/recovery. Most studies measured abstract associations, not induced emotion or gym attendance. These UI applications are design inferences. |
| [Jonauskaite et al., 2020 — associations across 30 nations](https://pubmed.ncbi.nlm.nih.gov/32900287/) | In 4,598 participants, color-emotion patterns showed shared features and variation related to geography/language. Yellow and orange were associated with joy; orange also with amusement. | Use a warm, positive accent while treating personal preference as relevant. A category association does not validate a particular hex code or predict an individual's response. |
| [Elliot, 2015 — color-in-context review](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2015.00368/full) | Color effects depend on context; methodological limitations restrict strong applied conclusions. Hue, lightness, chroma, and viewing conditions are intertwined. | Apply color consistently with the action's meaning and test the whole interface. Avoid claims that one color inherently produces motivation, trust, or increased performance. |

Supplementary search on 12 October 2026 used “color emotion systematic review”, “Jonauskaite yellow orange joy”, and “color psychology context exercise motivation”. The focused search did not establish that changing an exercise app's palette increases gym attendance. No attendance effect size or causal claim is assigned to this palette.

| Role | Color | UI use |
| --- | --- | --- |
| Action accent | Sunrise amber `#ffbf69` | Timer start/resume, selected bottom destination, program accent |
| Main content | Deep teal `#143f46` | Headings, numbers, active segmented choices, program card |
| Main surface | Ivory `#fffaf2` | Screen background and dialog |
| Quiet surface | Pale aqua `#e6f1f0` | Timer, progress card, prescription |
| Completed action | Teal `#24665f` + white | Done working/warm-up sets; always accompanied by check and pressed state |
| Warm-up context | Apricot `#fff0db` | Warm-up label and rep field, with explicit text label |
| Supporting text | Slate teal `#51676a` | Helper copy and secondary video access |
| Focus | Burnt amber `#9b480f` | Visible keyboard focus and small brand dot |

Use warm accents sparingly around actions, a light base, and teal for readable numbers. Completion uses its own semantic role instead of sharing the action accent. Warm-up and completion are still identifiable by text and state, not color alone. Android component shapes and minimum target sizes are preserved.

### Palette verification

Calculated sRGB contrast for the applied role pairs: main text on ivory 11.05:1; helper text on ivory 5.78:1 and pale aqua 5.20:1; deep teal on amber 7.06:1; white on completed-set teal 6.69:1; control outlines on pale aqua 3.02:1. These calculations check the specified pairs, not full accessibility conformance. The existing browser checks passed and refreshed mobile/desktop screenshots were visually reviewed.

## Applied voice

| Location | Selected copy | Intended support |
| --- | --- | --- |
| Brand promise | Your pace. Your progress. | Autonomy and personal reference point |
| Program home | Ready when you are. | A low-pressure invitation |
| Session action | Choose a session | Clear choice |
| Working sets | Log your sets | Specific next action |
| Progress view | See your progress. | Feedback from recorded performance |
| Recovery completion | Recovery complete · continue when ready | Factual status with user control |
| Unfinished sets | 1 of 3 complete | Neutral information; no failure label |

Keep labeled warm-ups in the shared set list and recovery visible as legitimate parts of training. Do not label rest as laziness or a lower-volume session as a failure. Completion color indicates a recorded action; it does not claim improved fitness. No unearned “personal best” messages are included.

## Validation and scope

The psychological rationale is strongest for supporting choice, competence, and useful monitoring. The exact language and visuals should be evaluated with the user while exploring the mockup: does choosing a session feel straightforward, can they log a set without friction, and does progress feedback feel useful? A future adherence study would require consent, a clear outcome, and actual longitudinal use; none is part of this prototype.

GYM-013 authorizes the identity and tone in `mockup/index.html`, `mockup/style.css`, and timer status copy in `mockup/app.js`. Reminders, streaks, rewards, social comparison, goal creation, mood collection, and gamification remain outside the current scope.
