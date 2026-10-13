---
name: gym-repo-delivery
description: "Use when organizing gym assystant repository files, changing contributor guidance or project skills, managing task context/delegation, selecting validation, or preparing commits and delivery. Follow project boundaries, protect existing work, use Conventional Commits and publish within existing user authorization."
---

# Gym repository delivery

Read relevant sections of [CONTRIBUTING.md](../../../CONTRIBUTING.md) for commit syntax, types and verification rules, and [repository-layout.md](../../../docs/architecture/repository-layout.md) for file ownership. Paths assume installation at `.agents/skills/gym-repo-delivery/`.

## Keep task context focused

Build a brief from the current request, relevant repository evidence and standing constraints; retain unfinished work and authorization. Load only applicable sections/skills. For a distinct request after a long conversation, delegation or a context handoff, read [task-context.md](references/task-context.md). Prefer fresh isolated workers for independent bounded work, disjoint writes and one coordinator; simple tightly coupled edits remain local. Do not claim to clear chat history without runtime support.

## Work within repository boundaries

- Keep browser HTML/CSS/JavaScript, tests and Node configuration in `prototypes/web/`; keep shared specification, research, design, reviews and architecture in their `docs/` sections. Skills belong in `.agents/skills/`, with root `AGENTS.md` routing to them.
- Reserve `android/` for the separately authorized native phase. It currently contains documentation only. Do not create scaffolding, choose a framework or copy browser sources into Android merely to prepare for later work. Once native implementation is authorized, follow the documented Android project/module boundaries and add actual native traceability and validation.
- Match the existing prototype style: vanilla browser scripts, `Gym…` module namespaces with small responsibility-based modules, CommonJS exports where needed by Node logic checks, and two-space indentation. Keep load order correct in `index.html`. Use existing CSS tokens, shared controls and escaping for user-provided text.
- Commit source, documentation, tests and lockfiles. Keep dependency directories, generated previews/downloads, SDK paths, IDE state and build caches untracked. Preserve the MIT license and Matteo Grechi's website attribution.

## Verify and deliver

1. Inspect the actual Git worktree, branch, remotes and status before editing or staging. Preserve unrelated user changes. If workspace Git metadata is unavailable or protected, use an authorized checkout/worktree and sync only explicitly changed files; never replace protected metadata or use a destructive directory sync.
2. Choose checks from CONTRIBUTING based on the changed artifact. Run the relevant existing logic/browser checks for product changes; for documentation and skills, validate links, source paths, YAML/frontmatter and routing. Do not rerun unrelated app suites for a prose-only change. For authorized native code, run the actual configured Android checks and state device/emulator limits.
3. Inspect the final diff and `git diff --check`. Ensure linked files and moved assets resolve, no generated/private files are staged, and feature changes include their specification and both traceability directions.
4. Stage explicit files and commit using `type(scope): description`, with optional scope. Use the main change to select the type; a UI behavior or design correction is `fix`, not formatting-only `style`. Include affected `GYM-###` IDs and meaningful validation in the body for product changes. Workflow-only commits need no invented product IDs. Mark incompatible public data-contract changes and explain recovery.
5. Deliver within the user's existing authorization. This project has a request to push work to `matteogrechi/gym-assistant`; verify that target and push the intended branch without force. If the remote advanced, integrate without dropping work, inspect conflicts and repeat only checks justified by the resolution. Do not rewrite previously published messages to retrofit the convention.
6. Report the concrete result, relevant validation, material limitations and commit/delivery status. If blocked by tool permissions, prepare reviewable work first and explain the specific blocked operation; do not add a separate approval gate through this skill.
