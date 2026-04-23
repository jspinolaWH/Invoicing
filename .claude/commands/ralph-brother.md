---
description: UI-gap fix loop. Reads ralph/ui/ reports, merges with a fresh gap-check, and runs code-fix on every requirement with PARTIAL_UI or NO_UI until all pass or max iterations is reached.
argument-hint: [max-iterations-per-requirement]
allowed-tools: [Read, Write, Bash]
---

# Ralph Brother

You are the UI-gap fix orchestrator. You read UI flow reports produced by `/ui-check`, merge them with a fresh backend/frontend gap-check scan, and drive code-fix until every requirement's gaps — both code-level and UI-level — are resolved. You do not ask questions. You drive everything to completion and log it all.

---

## Setup

### 1. Detect project root

```bash
PROJECT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
```

### 2. Verify folders

Check that `$PROJECT_ROOT/ralph/prd.json` exists. If not, output:
```
ERROR: ralph/prd.json not found.
```
and stop.

Check that `$PROJECT_ROOT/ralph/ui/` exists and contains at least one `.md` file. If not, output:
```
ERROR: No UI reports found in ralph/ui/. Run /ui-loop first.
```
and stop.

Ensure the gaps folder exists:
```bash
mkdir -p $PROJECT_ROOT/ralph/gaps
```

### 3. Read configuration

The argument is `$ARGUMENTS`.
- If a number is provided, use it as `MAX_ITERATIONS` per requirement (default: 3)

### 4. Read prd.json and build the work queue

Load all requirements. The work queue is every requirement where:
- `ui_partial: true` — UI report found gaps in the frontend
- `ui_no_frontend: true` — no frontend surface exists for this requirement
- `passes: false` — code-level gaps exist (regardless of UI status)

Exclude requirements where `ui_mapped: true` AND `passes: true` — these are fully done.

Count:
- Total requirements
- Fully done (skip)
- PARTIAL_UI / NO_UI only (UI fixes needed)
- Code gaps only (backend/frontend wiring missing)
- Both code and UI gaps

Output the opening banner:
```
╔══════════════════════════════════════════╗
║        RALPH BROTHER STARTING            ║
╠══════════════════════════════════════════╣
║ Project:      $PROJECT_ROOT              ║
║ Fully done:   [n] (skipping)             ║
║ UI gaps:      [n]                        ║
║ Code gaps:    [n]                        ║
║ To process:   [n]                        ║
║ Max iters:    [n] per requirement        ║
╚══════════════════════════════════════════╝
```

---

## The Loop

For each requirement in the work queue, in order by requirement ID:

### Iteration start

Log to `$PROJECT_ROOT/ralph/ui/ui-loop.log`:
```
[timestamp] BROTHER_START [requirement-id] "[summary truncated to 50 chars]" — iteration [n]
```

Output to stdout:
```
──────────────────────────────────────────
[requirement-id]  [summary truncated to 50 chars]
Iteration: [n] / [MAX_ITERATIONS]
──────────────────────────────────────────
```

---

### Step 1 — Run gap-check (backend + frontend code scan)

Run a standard code gap-check regardless of UI status:
```bash
claude -p "/gap-check $REQUIREMENT_ID" --cwd $PROJECT_ROOT
```

Capture stdout. Read the last line starting with `GAP_CHECK_RESULT:`.

- `GAP_CHECK_RESULT: PASSES` → no code gaps; proceed to Step 2 (UI synthesis only)
- `GAP_CHECK_RESULT: GAPS_FOUND` → code gaps exist; proceed to Step 2 (merge with UI)
- No result line → log WARNING, count as iteration, retry if iterations remain

After this step, `ralph/gaps/$REQUIREMENT_ID.md` will exist (written by gap-check).

---

### Step 2 — Synthesise UI gaps and merge into gap report

Find the UI report:
```bash
find $PROJECT_ROOT/ralph/ui -name "$REQUIREMENT_ID.md" | head -1
```

**If no UI report exists:** skip this step and proceed directly to Step 3 with whatever gap-check wrote.

**If a UI report exists:**

Read it fully. Extract every `### AC[n]` block where `**UI Status:**` is `⚠️ PARTIAL` or `❌ NOT VISIBLE`. For each:
- AC title and description
- What is missing (from the "not visible" explanation)
- Page route and component files (from "Files Inspected")

Now read the existing gap report at `$PROJECT_ROOT/ralph/gaps/$REQUIREMENT_ID.md`.

**Merge strategy:**

For each UI gap AC:

1. Check whether gap-check already identified a gap for this AC (look for matching criterion text or related backend endpoint).
   - If **yes** and gap-check gap has `Layer: Backend` → upgrade to `Layer: Both` and add the missing UI surface to the "What is missing" description.
   - If **yes** and gap-check gap has `Layer: Frontend` → the gap is already captured; append the UI report's description to the "What is missing" field if it adds detail.
   - If **no** → append a new gap block to the report with `Layer: Frontend`.

2. For each new gap block appended, check the backend controllers:
```bash
grep -r "[keyword from AC]" $PROJECT_ROOT/invoicing/src/main/java --include="*.java" -l
```
   If no matching controller/endpoint is found, set `Layer: Both` instead of `Frontend` — the backend needs to be built first.

Rewrite the gap report with the merged content. Keep all original gap-check gaps intact; only append or upgrade — never remove.

Update the final verdict line: if any gaps remain (from either source), ensure it reads `GAPS_FOUND`.

Log: `[timestamp] UI_SYNTHESIS [requirement-id] — merged [n] UI gaps into gap report (layer upgrades: [n])`

---

### Step 3 — Code fix

Run:
```bash
claude -p "/code-fix $REQUIREMENT_ID" --cwd $PROJECT_ROOT
```

Capture stdout. Read the last line starting with `CODE_FIX_RESULT:`.

- `CODE_FIX_RESULT: DONE` → log FIX_APPLIED, increment iteration count, proceed to Step 4
- `CODE_FIX_RESULT: SKIPPED` → log SKIPPED, mark `needs_review: true` in prd.json, move to next requirement
- No result line → log WARNING, count as iteration, retry Step 1 if iterations remain

---

### Step 4 — Verify: re-run UI check

Run:
```bash
claude -p "/ui-check $REQUIREMENT_ID" --cwd $PROJECT_ROOT
```

Capture stdout. Read the last line starting with `UI_CHECK_RESULT:`.

- `UI_CHECK_RESULT: UI_MAPPED` →
  - Update prd.json: `ui_mapped: true`, remove `ui_partial` and `ui_no_frontend`
  - Also re-read gap report — if final line is `GAPS_FOUND.done`, set `passes: true`
  - Log PASS
  - Move to next requirement
- `UI_CHECK_RESULT: PARTIAL_UI` →
  - Update prd.json: `ui_partial: true`
  - Log STILL_PARTIAL, increment iteration count
  - If iterations remain: loop back to Step 1
  - If max iterations reached: flag for review
- `UI_CHECK_RESULT: NO_UI` →
  - Log STILL_NO_UI, increment iteration count
  - If iterations remain: loop back to Step 1
  - If max iterations reached: flag for review
- No result line → log WARNING, treat as STILL_PARTIAL

---

### Max iterations reached

If a requirement hits `MAX_ITERATIONS` without reaching `UI_MAPPED` + `passes: true`:
- Set `needs_review: true` in prd.json
- Set `notes: "UI+code gaps remain after [n] iterations [timestamp] — manual review required"`
- Log: `[timestamp] MAX_ITERATIONS [requirement-id] — flagged for manual review`
- Move to next requirement

---

## After all requirements are processed

Read prd.json and count final state.

Write summary to `$PROJECT_ROOT/ralph/ui/brother-summary.md`:

```markdown
# Ralph Brother Summary

**Completed:** [ISO timestamp]
**Project:** [PROJECT_ROOT]

## Results

| Status | Count |
|--------|-------|
| ✅ Fully done (UI_MAPPED + passes) | [n] |
| ⚠️ Still PARTIAL_UI | [n] |
| ❌ Still NO_UI | [n] |
| 🔍 Needs review | [n] |

## Fixed This Run

[List of requirement IDs that moved to UI_MAPPED this run]

## Still Needs Work

[List of requirement IDs that hit max iterations, with brief reason]
```

Output to stdout:
```
╔══════════════════════════════════════════╗
║        RALPH BROTHER COMPLETE            ║
╠══════════════════════════════════════════╣
║ ✅ Fully done:      [n]                  ║
║ ⚠️  Still PARTIAL:  [n]                  ║
║ ❌ Still NO_UI:     [n]                  ║
║ 🔍 Needs review:   [n]                   ║
╚══════════════════════════════════════════╝

See ralph/ui/brother-summary.md for details.
```

---

## Logging rules

Every action appends to `$PROJECT_ROOT/ralph/ui/ui-loop.log`. Never overwrite. Format:
```
[ISO timestamp] [LEVEL] [requirement-id] [message]
```
Levels: `BROTHER_START`, `UI_SYNTHESIS`, `FIX_APPLIED`, `PASS`, `STILL_PARTIAL`, `STILL_NO_UI`, `SKIPPED`, `WARNING`, `MAX_ITERATIONS`, `COMPLETE`
