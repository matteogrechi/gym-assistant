# Generated UI review evidence

Researched 13 October 2026 for the whole-app information/layout review. This informs verification under GYM-025; it does not authorize new features.

## Model-specific evidence boundary

The official [GPT-6.1 Sol model page](https://developers.openai.com/api/docs/models/gpt-6.1-sol) confirms the named model and High reasoning support. Searches for GPT-6.1 Sol/High frontend errors and limitations did not locate a measured frequency ranking or UI-specific failure study. The [GPT-6 guide](https://developers.openai.com/api/docs/guides/latest-model) describes family prompting recommendations as observations of Astra requiring evaluation on the chosen workload. Neither establishes which UI mistakes Sol High makes most often. No ranking or claim that lowering reasoning fixes this app is justified.

## Documented patterns and application to this mockup

OpenAI's [frontend design article](https://developers.openai.com/blog/designing-delightful-frontends-with-gpt-5-4), explicitly about GPT-5.4, describes generic structure and weak hierarchy under underspecified prompts. It warns against card mosaics, redundant utility copy and ornamental controls, and recommends readable density, minimal chrome and rendered-flow verification. These are cross-model review hypotheses here, not measured Sol High defects.

| Pattern to check | Evidence to seek in gym assystant |
| --- | --- |
| Too many containers and scattered hierarchy | Related prescription, status or file actions split across panels; cards with no grouping/interaction purpose. |
| Repeated explanations and filler | A heading, label and help line all convey the same fact; screen copy describes implementation rather than the next gym action. |
| Disproportionate chrome/spacing | Headers or empty margins displace set rows and schedule controls; distinguish purposeful 48px targets and the requested fixed home card from wasted space. |
| One polished screenshot hiding broken states | Navigate Ready/Paused/Finished, read-only/edit/dirty plans, long names, missing histories, themes and narrow/short windows. |

The official [frontend prompt instructions](https://developers.openai.com/api/docs/guides/frontend-prompt), targeted at GPT-5.5, favor domain-appropriate layouts, predictable workflows and adherence to an existing design system. Applying that advice here means retaining the established Material-style controls, shared selector, palette and logging emphasis. It does not require adding an icon library or adopting marketing-page rules.

Two further review hypotheses are component drift and misleading simplification: compare shared buttons/fields/disclosures across views, and ensure compacted copy still distinguishes prescribed versus logged sets, planned versus actual dates, unsaved versus saved plans and fictional versus persisted data. These checks derive from this project's specification and user requests, rather than a model-specific online study.

## Review process

Four isolated reviewers navigate distinct screen groups and report reproduction steps, screenshots, user consequences and small fixes. The coordinator consolidates overlapping findings, distinguishes defects from intentional context, specifies accepted changes before source edits and tests the integrated result. Review outcomes and retained limitations belong in [interface-review.md](../reviews/interface-review.md); each source change remains traceable through [traceability.md](../product/traceability.md).
