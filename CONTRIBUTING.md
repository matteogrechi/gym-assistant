# Contributing to gym assystant

The current phase defines the Android product through a disposable web mockup. Native implementation begins after the user authorizes that phase. [The specification](docs/product/spec.md) is the feature source of truth; [traceability](docs/product/traceability.md) records actual coverage and the reverse source inventory. Write or refine acceptance criteria before feature code and keep both directions current. Research is evidence, not automatic permission to add features.

## Repository conventions

| Location | Owns |
| --- | --- |
| `prototypes/web/` | Vanilla browser source, fictional fixtures, Node tooling, tests and its lockfile. |
| `android/` | Future native project; documentation only until implementation is authorized. |
| `docs/product/` | Requirements, acceptance criteria and bidirectional traceability. |
| `docs/research/`, `docs/design/`, `docs/reviews/` | Research evidence, Android UI guidance and measured interface reviews. |
| `docs/architecture/` | Repository boundaries and the durable data contract. |
| `.agents/skills/` | Project workflow skills, linked from root `AGENTS.md`. |

Follow the existing prototype's two-space indentation, browser `Gym…` module namespaces and CommonJS exports for logic-test access. Reuse shared CSS roles/buttons/icons; maintain script dependency order and escape user text in generated HTML. The prototype has no build or runtime package dependency. Keep dependency installations, generated files, local SDK/IDE configuration and build caches out of commits; track source, specifications, meaningful tests and lockfiles. Preserve the [license](LICENSE) and website attribution. See [repository layout](docs/architecture/repository-layout.md) for future Android boundaries and [data formats](docs/architecture/data-formats.md) before changing status/statistics files.

## Conventional Commits

All new commits use [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/). Existing published messages remain unchanged. The header is:

```text
type(scope): short description
```

Scope is optional; use a concise area such as `mockup`, `spec`, `data`, `design`, `android`, `workflow` or `skills`. Use lowercase types and scopes, an imperative description and no trailing period. Keep commits focused; their related specification, implementation and verification can travel together.

| Type | Purpose in this repository |
| --- | --- |
| `feat` | New authorized product behavior. |
| `fix` | Correct existing behavior or UI design. |
| `docs` | Specification, research, reviews or contributor/skill instructions. |
| `refactor` | Source restructuring without a behavior change. |
| `test` | Meaningful verification changes alone. |
| `style` | Source formatting without behavior or visible design changes. |
| `perf` | Performance improvement preserving intended behavior. |
| `build`, `ci`, `chore` | Build/dependency work, automation or other maintenance, respectively. |
| `revert` | Revert an earlier change; identify its commit in the body. |

For product changes, include affected `GYM-###` IDs and relevant validation in the optional body, separated from the header by a blank line. Documentation or tooling maintenance needs no invented requirement ID. Mark incompatible public contracts with `!` before the colon or a `BREAKING CHANGE:` footer, and explain migration/recovery. A format-version change also needs the data-contract documentation; the marker alone is insufficient. No automatic release/versioning policy is introduced by this convention.

Examples:

```text
docs(workflow): add repository skills and commit conventions

fix(mockup): keep dialog surfaces inside the phone

Keep the card and scrim within the simulated screen.
Verify desktop containment, short-height scrolling and keyboard dismissal.

Refs: GYM-006
```

If changes are delivered through a squash-merged PR, its final squash title must also follow this convention. Describe the resulting behavior and actual checks in the PR, rather than conversation history.

## Validation and delivery

Choose verification appropriate to the change:

| Change | Verification |
| --- | --- |
| Mockup data, metrics, sessions or timers | Existing logic checks plus browser flows affected by the behavior. |
| UI or interaction | Chromium browser suite and visual inspection of affected screens/states; refresh affected images in `docs/reviews/previews/`. Keep historical comparisons under `docs/reviews/history/`. |
| Durable file contract or export | Relevant logic/browser checks and recovery with standard JSON/CSV readers. |
| Documentation, file moves or skills | Resolve links/source paths; validate skill frontmatter/UI YAML where affected; check routing and removal of placeholders. Runtime moves also need affected runtime checks. |
| Native Android, once authorized | Checks provided by the actual Android project; document device/emulator coverage and gaps. |

Run logic checks from the repository root:

```sh
node prototypes/web/tests/logic.test.cjs
```

Run browser checks in the prototype project:

```sh
cd prototypes/web
npm ci
CHROMIUM_PATH=/usr/bin/chromium npm run test:browser
```

Install dependencies only when needed. Alternative browser setup and temporary screenshot output are documented in the [prototype README](prototypes/web/README.md). Add tests for meaningful changed behavior when necessary; do not add tests that merely duplicate implementation or rerun unrelated app suites for documentation-only work.

Before committing, inspect the actual worktree, branch and remote, review the diff, run `git diff --check` and stage explicit files. Preserve unrelated user changes and protected metadata. Push within existing user authorization to the verified `matteogrechi/gym-assistant` remote; use ordinary pushes and integrate remote advances without dropping work. Report the commit, delivery result, actual checks and any material gaps.

## Project skills

The [AGENTS.md routing table](AGENTS.md) links three self-contained skills stored in `.agents/skills/`. Each has `SKILL.md` and optional UI metadata in `agents/openai.yaml`; the repository is their source of truth. Agents can read the linked instructions directly even when their client does not expose repository skills in its picker. No global installation is required. Skill structure follows the [official skill documentation](https://developers.openai.com/codex/skills/).
