# Work Notes

This directory contains lightweight, temporary notes for possible, intended, or parked work. Each
work note is a self-contained directory for one thread of work, such as an idea, investigation,
change, issue, feature, or spike, and may hold planning, implementation, and agent handoff material.
Durable current-state knowledge belongs in [`docs/`](../docs).

The README closest to a directory governs how that directory is used; this file is the single
authoritative guide for `work/` and its state directories. Agents follow the operating procedure
in [`manage-work-notes`](../.agents/skills/manage-work-notes/SKILL.md) for all work-note
operations.

## States

The states communicate current intent, not priority, maturity, or sequence. A note may be created
directly in any state and moved only when its intent changes. There is no required flow between
states and no implied ordering within a state.

When intent changes, move the entire note directory to whichever state is now accurate. Moving
does not imply progress, and a note need never move. After a move, review the overview and
optional plan so they describe the current understanding.

### `ideas/`

Work that may be worth pursuing but that the project does not currently intend to pursue: possible
features, questions, investigations, or early design thoughts. Natural instructions include
**Capture this as an idea.**

### `todo/`

Work the project currently intends to pursue. A note here may be actively underway or ready to
begin; the directory is not necessarily a prioritized queue. Natural instructions include **Add
this to todo** and **Continue working on this note**.

### `parked/`

Work the project is deliberately not pursuing now but still considers valuable enough to preserve.
Work may be parked because it depends on future work, belongs in a later version, is too difficult
for the current phase, lost priority, or requires more information. Parking does not mean
permanent rejection, and an existing plan may remain while it still provides useful context.
Natural instructions include **Capture this as parked work** and **Park this work note**.

## Work Note Structure

Each work note is stored in a directory with a concise, stable, lowercase, hyphen-separated name.
External issue identifiers may be used alone or with a description, such as `issue-25/` or
`issue-25-browser-auth/`.

Each state directory contains a hidden `.template.md` holding the template for a new note's
`overview.md` in that state; it also keeps an otherwise-empty state directory present in Git.
`work/` holds the state-independent `.template-plan.md` for the optional execution plan:

```text
work/
├── .template-plan.md
└── todo/
    ├── .template.md
    └── browser-gateway/
        ├── overview.md
        └── plan.md   (optional)
```

The directory name is the identifier, its parent directory is the state, and Git records history.
Do not add IDs, state fields, dates, priorities, or tags until repeated practical need requires
them.

### `overview.md`

Every work note requires `overview.md`, the neutral entry point for a note in any state. Start a
new note by copying the state directory's `.template.md` to the new note directory's `overview.md`
and replacing every `{{ placeholder }}`.

Two YAML front-matter fields are required:

```yaml
---
title: "Browser gateway authentication"
summary: "Add authentication boundaries before exposing the gateway to more clients."
---
```

- `title` is a short descriptive label, readable in a table and clearer than the directory name.
- `summary` is a brief explanation that adds information beyond the title and stands alone in a
  work summary table. Prefer one clear sentence, use two when that reads better, and never make it
  a paragraph.

The Markdown body adds only useful context not already clear from the summary; when there is
none, remove the template's body prompt and keep the title heading. The body does not require
fixed sections and is not an execution plan. A note may hold additional supporting files, such as
research notes or experiment material; `overview.md` remains its entry point.

### Optional `plan.md`

Add `plan.md` only when work is concrete enough to plan. It tells a coding agent how the work is
expected to be implemented and verified: `Context` carries the stable planning narrative and
`Phases` carries checkbox steps grouped under named phases.

Keep it as the current plan rather than a diary of previous plans. Completed steps are checked off
as work proceeds, while the planning context changes only when the plan itself changes. Add or
remove phases to match the work. Small or exploratory work may never need a plan.

Start `plan.md` by copying [`.template-plan.md`](.template-plan.md) and replacing every
`{{ placeholder }}`.

## Completion Checklist

Completion is an outcome available from any state, not a state change. Before deleting a completed
work note:

1. Complete the intended outcome, including implementation, documentation, and tests when
   applicable.
2. Update every affected durable document.
3. Create any newly needed durable document under `docs/`.
4. Run the verification CI runs: `./gradlew build` from the repository root, then `npm ci &&
   npm run build` in `applications/frontend`.

Delete the note only after every step has succeeded.

Abandonment is the other terminal outcome: when the project explicitly discards a note, delete it
without the checklist after moving any durable knowledge to `docs/`; committed history remains
recoverable through Git.

## Relationship to Issue Trackers

An external tracker such as GitHub Issues or Jira may coexist with work notes: the tracker
handles coordination, discussion, and reporting, while the note holds local reasoning, planning,
and preserved agent context. A note may carry an external identifier in its name without
duplicating tracker metadata; neither system replaces the other.

## Discovering Work

To list work without loading every note, inspect only the YAML front matter in:

```text
work/ideas/*/overview.md
work/todo/*/overview.md
work/parked/*/overview.md
```

A work listing may report each note's state, directory, title, and summary.
