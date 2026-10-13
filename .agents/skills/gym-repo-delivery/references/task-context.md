# Focused task context and delegation

Use this reference when starting a distinct request after a long conversation, delegating work, or handing off an unfinished task. Ordinary small edits need only the short rules in AGENTS.md and their relevant skill.

## Establish the working brief

Distinguish a continuation/correction of unfinished work from a new request after delivery. Treat mid-task messages as steering unless they clearly replace the objective; retain unfinished obligations rather than silently dropping them. For a distinct request, stop carrying the previous task's investigations into the working brief.

Keep a short brief in working memory: requested outcome, affected view/requirements/files, acceptance criteria, standing constraints, existing authorization, actual branch/worktree state, and any relevant unfinished work. A few bullets usually suffice; no committed per-request paperwork is needed.

Preserve the exact app name, mockup-only phase until Android is authorized, specification-first product changes, bidirectional traceability, Conventional Commits, publishing authorization/verified target, and any user instruction that affects the current task. The current specification is the source of implemented product decisions, but newer user corrections take precedence. Context reduction never revokes existing authorization or grants new permissions. An explicit review-only or no-push request limits that task despite standing publishing authorization.

Search with `rg` to locate the needed requirement IDs, source functions, current preview and relevant validation commands. Read those sections, not entire specifications, historical reviews, test files, research or all skills by default. Broaden only when an unresolved dependency or failure requires it. Prefer paths/line references and concise test results to copied documents or tool logs; cap outputs and inspect omitted material through targeted follow-ups. Reuse already-read guidance while it remains current.

## Decide whether delegation pays for itself

The user authorizes appropriate subagent use in this project. Check that delegation tools are actually available. Keep the immediate critical-path decision local and identify useful independent work before spawning.

- Handle a small, tightly coupled edit directly when briefing/reviewing another agent would cost more than it saves.
- Use a fresh worker for a bounded implementation slice once its acceptance criteria are stable, while the coordinator handles disjoint specification, tests or delivery work. Use an independent reviewer for a concrete scope, traceability or interaction risk when that review can run alongside other work.
- Parallelize only independent tasks with disjoint write ownership. Do not have the coordinator and worker solve or edit the same unresolved task, or send the same question to several agents without a distinct purpose.
- Reuse an agent only within a dependent workstream that benefits from its context. Close completed agents; create a fresh one for an unrelated workstream. Avoid recursive delegation unless its benefit and ownership are explicit.

For fresh workers, omit history inheritance or set `fork_context: false` when the tool supports it. Pass only the working brief, applicable instructions, exact source/reference paths, owned files and exclusions, acceptance criteria, verification and requested output. Inherit conversation history only when a dependency cannot be expressed economically in a bounded brief. Do not request a different model without user direction.

Require editing workers to write within their assigned workspace/files and return changed paths, concise outcomes, checks and unresolved risks. Reviewers receive raw relevant artifacts and the request, rather than the coordinator's preferred answer. A reviewer cannot edit, publish or expand scope unless explicitly assigned. Each agent respects the same user authorization and tool permissions. Keep one coordinator responsible for integration, verification claims, commits and publishing.

Work on a non-overlapping task while workers run. Wait only when the result is needed for the next step. Review returned diffs and evidence, resolve conflicts without discarding unrelated work, and validate the integrated result. Reuse valid checks instead of rerunning them just because work was delegated; repeat only for integration changes, failures or unresolved concerns. Summarize results rather than pasting full agent transcripts.

## Handoff and finish

Before an actual context transition, leave a compact handoff in the session summary or a temporary file: objective and latest correction; standing constraints/authorization; actual worktree/branch and changed files; decisions and completed checks; pending agent/process IDs; next action and blockers. Reference repository files for detail. Preserve unfinished work across compaction and do not treat elapsed time as approval. Keep ephemeral handoffs outside tracked source and without secrets.

An agent cannot erase the coordinator's chat history or guarantee a fresh parent context. Use a bounded working brief and isolated workers; use compaction/new-context facilities only when the runtime supplies them. If delegation or compaction is unavailable, continue locally with targeted reads and state that limitation when material; never claim a reset or delegation that did not occur.

At delivery, close workers, report the outcome/checks/commit concisely, and leave durable product decisions in the specification/traceability or durable workflow decisions in contributor guidance/skills. Do not copy abandoned investigations or unrelated chat history into these files. Future tasks rebuild their brief from the current request and repository evidence.
