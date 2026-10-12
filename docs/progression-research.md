# Progression metrics for gym assystant

Research date: 12 October 2026. Supports GYM-010 and GYM-011. This is a focused literature review for product design, not an exhaustive systematic review or an individualized training prescription.

## Recommendation

Lead with **reps at the same weight** and **weight at the same reps** for the same exercise and machine. Show workload separately. Consider optional effort logging before introducing estimated maximal strength. Do not present increasing tonnage or program completion as proof of muscle growth.

This ordering is a product-design inference from the evidence below, not a clinically validated scoring system. The current mockup includes the two performance metrics, exercise volume load, and completed working-set counts. All historical data is fictional.

## Search approach and evidence boundaries

Searched the web for PubMed-indexed research and author/publisher full texts on 12 October 2026 using terms including “resistance training progression repetitions load”, “prediction one repetition maximum multiple repetition maximum Reynolds”, “repetitions in reserve accuracy”, “resistance training volume load”, and “2026 ACSM resistance training position stand”. Prioritized direct measurement studies and interventions, complemented by recent systematic reviews and consensus evidence synthesis. The 2026 ACSM stand supersedes its 2009 predecessor; its underlying searches end in October 2024. Later 2026 literature was considered separately.

Eight relevant sources are summarized below. ACSM and Plotkin full texts were accessed where available; Reynolds was available through an author's university-hosted paper; other summaries rely on indexed abstracts. No formal risk-of-bias assessment or meta-analysis was performed here. Most evidence concerns healthy adults under controlled conditions; an app's ordinary machine logs have more uncontrolled variation.

## Evidence and implications

| Source | Findings relevant to this app | Limitation / design consequence |
| --- | --- | --- |
| [Plotkin et al., 2022, PeerJ — load versus repetition progression](https://pmc.ncbi.nlm.nih.gov/articles/PMC9528903/) | An 8-week randomized study of 43 trained adults compared increasing load with increasing repetitions. Both approaches supported lower-body adaptations; results support recognizing repetition progression as well as load progression. | Short study, limited exercises; it does not validate an app score or prove that every extra rep means muscle growth. Use two understandable performance measures. |
| [Reynolds, Gordon & Robergs, 2006 — predicting 1RM](https://www.unm.edu/~rrobergs/478RMStrengthPrediction.pdf) | Seventy adults performed bench press and leg press 1, 5, 10, and 20RM tests. Prediction was best from 5RM, with exercise-specific error; authors cautioned against using more than 10 repetitions in linear predictions for those movements. | Repetition-maximum tests are not arbitrary submaximal gym sets. Their results do not validate a universal equation for every machine. Keep estimated 1RM secondary and explicitly approximate. |
| [Zourdos et al., 2016 — RIR-based exertion scale](https://pubmed.ncbi.nlm.nih.gov/26049792/) | A study of 29 novice/experienced squatters examined a scale linking perceived exertion to repetitions in reserve (RIR), with an inverse association between exertion and velocity. | Supports effort context, not objective measurement of fatigue or readiness. Consider an optional “reps left” field instead of making it mandatory. |
| [Hackett et al., 2017 — accuracy of estimated repetitions remaining](https://pubmed.ncbi.nlm.nih.gov/27787474/) | In 81 adults performing chest and leg press, estimates were more accurate closer to failure and varied by exercise. Error was roughly one rep with 0–5 actual reps remaining versus more than two with 7–10 remaining. | RIR is subjective. Do not silently treat a one-point change as a precise physiological improvement. |
| [Schoenfeld et al., 2016 — volume-load accumulation](https://pubmed.ncbi.nlm.nih.gov/27625750/) | Eighteen trained men undertook high- or low-load training for eight weeks. The lower-load, higher-repetition condition accumulated more volume load. | Volume load depends on rep scheme. More kg·reps alone cannot rank strength development or training quality. |
| [Grgic et al., 2020 — reliability of measured 1RM](https://pubmed.ncbi.nlm.nih.gov/32681399/) | A systematic review found generally high test–retest reliability of standardized 1RM testing. | Reliability of a measured maximal test does not transfer automatically to a formula derived from routine sets. No mandatory maximal testing in the app. |
| [Currier et al., 2026 — ACSM position stand](https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/) | Synthesized 137 reviews involving over 30,000 participants. Resistance training improved multiple outcomes; variables supporting maximal strength and hypertrophy were not identical. Heavier loads favored strength and higher volumes favored hypertrophy. | Training dose and adaptation are different constructs. The app should separate performance from workload and avoid one universal progression score. Group-level findings are not individual prescriptions. |
| [Pelland et al., 2026 — volume/frequency dose response](https://pubmed.ncbi.nlm.nih.gov/41343037/) | Meta-regressions of 67 studies with 2,058 participants suggested positive volume relationships with diminishing returns and different responses for strength and hypertrophy. Distinguishing direct/indirect sets affected the analysis. | Weekly sets can describe exposure, but an app cannot infer individual muscle gain from a set count. Muscle-level totals require an explicit policy for compound exercises; avoid double counting. |

## Candidate metrics and formulas

| Metric | Calculation and interpretation | Product decision |
| --- | --- | --- |
| Reps at fixed load | For each occurrence, `max(reps)` over comparable completed working sets at reference kg. Example: 60 kg × 10 → 60 kg × 12 gives +2 reps. | **Included in mockup.** Main rep-performance trend. Not proof of greater 1RM or hypertrophy. |
| Load at fixed reps | `max(kg)` over comparable completed working sets with exactly the selected rep count. Example: 60 kg × 10 → 65 kg × 10 gives +5 kg. | **Included**, with a fixed 10-rep comparison for exploration. A performed 10-rep set is not necessarily a 10RM. |
| Volume load | `Σ(kg × reps)` over completed working sets for one exercise/occurrence. E.g. 3 × 10 × 60 = 1,800 kg·reps. | **Included as workload.** Does not account for displacement, machine leverage, effort, or range of motion. Do not label it mechanical work or muscle growth. |
| Working-set count | Count completed non-warmup sets per exercise and stated date window. Later, weekly muscle-level counts would require direct/indirect-set rules. | **Included per exercise over displayed history.** Weekly muscle breakdown remains proposed. |
| Estimated 1RM | Candidate Epley expression: `kg × (1 + reps / 30)` for eligible multi-rep sets. Measured single-rep maxima must remain separate. | **Proposed, not implemented.** Choose/validate equation and supported exercises first. Require known near-maximal effort and a conservative rep range; do not extrapolate arbitrary easy or high-rep sets. Formula convention is not validation. |
| Effort context | Optional RIR: how many more technically comparable reps the user estimates they could perform. Compare similar effort; missing RIR stays unknown. | **Proposed, not implemented.** Useful context, subject to estimation error. Do not add estimated RIR into an e1RM formula without validating that model. |
| Program adherence | Completed scheduled occurrences ÷ planned occurrences in an explicitly defined period. Show numerator/denominator; no scheduled sessions means N/A. | **Proposed, not implemented.** A consistency measure, not a physiological progression metric. Requires scheduling and a session-completion definition. |
| Direct muscle-size measurement | Standardized longitudinal measurements, such as muscle ultrasound in research. Routine scale weight or kg·reps cannot substitute for this. | **Deferred.** The proposed app has no validated direct hypertrophy outcome. |

The example formulas and display policies are product definitions. The studies motivate their interpretation and limitations; they do not validate these exact UI aggregation rules. In particular, picking the best set can be influenced by doing more attempts, so the accompanying working-set count matters.

## Comparison rules for the eventual app

1. Retain exercise variation, machine identity, seat/setup, load convention, units, range of motion, session date, working/warmup status, and completion. Preserve the historical program prescription separately from actual performance.
2. Compare like with like. Rest, execution tempo, effort, exercise order, and fatigue can change performance; show available context. Execution/recovery timer durations could later provide context, but this mockup does not store timer history or use it to correct statistics.
3. More weight with fewer reps is not automatically an improvement. Show separate matched comparisons; use an explicitly qualified estimate only if its assumptions are met.
4. An unmatched load or rep target is missing comparative data, not zero performance. Show gaps and keep the underlying log inspectable. Changed equipment starts a distinct series.
5. Use repeated observations and visible raw values. Avoid calling small fluctuations a plateau, assigning an arbitrary “fitness score”, or treating a deload as regression. No universal smallest meaningful change was established by this review.
6. Percentage change is `(new − baseline) / baseline × 100` only with comparable, positive baselines. No eligible baseline means N/A. The current mockup simply shows actual historical values without percentage claims.

## Scope trace

GYM-010 implements only the four explicitly included mockup metrics. GYM-011 covers this research artifact. GYM-012 introduces timers but not duration-based analytics. RIR entry, e1RM, adherence, muscle-level volume, automatic progression recommendations, and body measurements remain proposals until the specification is explicitly expanded.
