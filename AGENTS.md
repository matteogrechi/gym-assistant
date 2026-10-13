# gym assystant repository instructions

Read the relevant sections of [CONTRIBUTING.md](CONTRIBUTING.md) before changing this repository. Keep the exact app name **gym assystant**. The current product artifact is a disposable browser mockup; native Android implementation is not started and begins only when the user authorizes that phase.

## Focused context and subagents

- For each distinct task, build a small working brief from the current request, standing constraints, relevant repository evidence and unfinished obligations. Preserve mid-task steering and existing authorization; omit unrelated completed-task history.
- Read only applicable skill entrypoints and affected requirement/source/review sections. Search before expanding context, and keep tool/agent output concise.
- The user authorizes selective subagents: use isolated context for independent, bounded work, disjoint file ownership and one integration/delivery coordinator. Keep trivial or tightly coupled changes local; reuse workers only for dependent follow-ups and close them when done.
- Before a context transition, preserve a short handoff with changed paths, verified results and next actions. Do not claim chat history was cleared when the runtime cannot do that.

For delegation, long-conversation task boundaries or handoffs, read the [context and delegation workflow](.agents/skills/gym-repo-delivery/references/task-context.md).

## Project skills

Open the relevant `SKILL.md` before working on its area. Skills are versioned inside this project under `.agents/skills/`; read only those relevant to the task.

| Work | Skill |
| --- | --- |
| Product requirements, feature changes, traceability or durable data contracts | [gym-spec-workflow](.agents/skills/gym-spec-workflow/SKILL.md) |
| Mockup UI changes, accessibility, responsive behavior or user walkthroughs | [gym-mockup-review](.agents/skills/gym-mockup-review/SKILL.md) |
| Repository organization, process/skill changes, validation, commits or delivery | [gym-repo-delivery](.agents/skills/gym-repo-delivery/SKILL.md) |

Product changes often need the first two skills and delivery guidance. Documentation-only work normally needs delivery guidance alone. Explicit user instructions and existing authorization take precedence over repository guidance; these skills introduce no extra approval requirement.

## Shared rules

- Update the product specification before feature code; maintain requirement-to-source coverage and the reverse inventory. Research suggestions do not authorize extra features.
- Keep the prototype in `prototypes/web/`, future native work in `android/`, and shared evidence in `docs/`. Do not create Android scaffolding during the mockup phase.
- Use Conventional Commits for all new commits, as defined in CONTRIBUTING. Preserve already-published history and unrelated user changes.
- Validate the changed artifact with meaningful checks and report what actually ran. Never claim that mockup coverage proves native Android delivery.
