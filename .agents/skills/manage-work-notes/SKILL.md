---
name: manage-work-notes
description: Manage lightweight repository work notes under work/ideas, work/todo, and work/parked. Use when asked to capture an idea, add something to todo, park work, list or inspect work, update a note's content, add or revise a plan, continue work from a note, reclassify a note when intent changes, discard an abandoned note, or complete and remove a note after preserving durable knowledge.
---

# Manage Work Notes

Manage lightweight repository work notes according to current intent. The checked-in guidance in
`work/README.md` is the source of truth for the note structure, templates, and state model; this
skill adds the operational procedure. Perform the preparation, then the requested operations,
then report the result. When writing note content, clearly distinguish user-provided facts from
assumptions and open questions.

## Prepare

1. Read `work/README.md`.
2. For an operation on an existing note, locate it; if multiple notes plausibly match, ask before
   changing anything. Read the selected note's `overview.md` and optional `plan.md`.
3. Before writing, inspect the working tree and preserve unrelated changes.

## Find Work

Follow the discovery procedure in `work/README.md`. Read note bodies only when the user asks for
details or when needed to disambiguate a match.

## Create a Note

1. Search all three states for the same or equivalent note before creating one.
2. When an equivalent note exists, update it only when the request authorizes the update;
   otherwise report the match and ask instead of creating a duplicate. Use a distinct name only
   when unrelated work collides on a similar name.
3. Choose the state that matches current intent and start the note's `overview.md` by copying that
   state directory's `.template.md` and replacing every `{{ placeholder }}`.

## Add a Plan to a Note

Add `plan.md` to a new or existing note by copying `work/.template-plan.md` and replacing every
`{{ placeholder }}`. Add it only when the work is concrete enough to plan. If the user requests a
plan before then, ask for the missing details instead of inventing them.

## Update a Note's Content

Rewrite the note to reflect the current understanding instead of appending a chronological log.
Keep the overview neutral and the plan actionable.

## Reclassify a Note

When the note's intent changes, move the entire note directory to the state that now matches.
Check for a destination collision and never merge two directories implicitly. Revise the note for
its new state and remove obsolete statements. Do not move a note merely to satisfy an assumed
workflow.

## Continue Work on a Note

Use the selected note as task context and follow the repository's normal implementation and
verification instructions. Update the plan when the approach materially changes; do not use it as
a progress diary.

## Complete and Remove a Note

1. Confirm the requested outcome is actually implemented.
2. Follow the completion checklist in `work/README.md`, including the
   verification it specifies.
3. Delete the note directory only after those checks succeed and the user's request authorizes
   completion.

If work remains or verification fails, retain the note in the state matching current intent and
report what is unfinished.

## Discard a Note

Discard only when the user explicitly asks to abandon or discard the selected note. Move any
durable knowledge to its `docs/` home, then delete the note directory without the completion
checklist. Treat that explicit request as authorization to delete the selected note's uncommitted
content; only committed content remains recoverable through Git. If the request or its target is
ambiguous, report the affected uncommitted changes and ask before deleting anything.

## Report the Result

State what was created, updated, reclassified, discarded, or completed and give the resulting
repository path. Mention meaningful assumptions, unresolved questions, and verification performed.
Do not commit, push, or alter remote systems unless the user explicitly requests it.
