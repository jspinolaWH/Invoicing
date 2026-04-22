---
description: Orchestrate the full Ralph loop across all requirements in ralph/prd.json. Runs gap-check and code-fix for each requirement until all pass or max iterations is reached.
argument-hint: [max-iterations-per-requirement]
allowed-tools: [Read, Write, Bash]
---

# Ralph

You are the loop orchestrator. You run the full autonomous build loop across all requirements in `ralph/prd.json`. You do not interact with the user. You do not ask questions. You drive gap-check and code-fix to completion and log everything.

## Setup

### 1. Detect project root

```bash
PROJECT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
```

### 2. Verify ralph folder exists

Check that `$PROJECT_ROOT/ralph/prd.json` exists. If not, output:
```
ERROR: ralph/prd.json not found in $PROJECT_ROOT
Run this command from your project root, or create ralph/prd.json first.
```
and stop.

Ensure these folders exist, create them if not:
```bash
mkdir -p $PROJECT_ROOT/ralph/gaps
```

### 3. Read configuration

The argument is `$ARGUMENTS`.
- If a number is provided, use it as `MAX_ITERATIONS` per requirement (default: 3)
- Parse from `$ARGUMENTS` or default to 3

### 4. Read prd.json

Load all requirements. Count:
- Total requirements
- Already passing (`passes: true`)
- Remaining (`passes: false`)

Output the opening status:
```
╔══════════════════════════════════════╗
║           RALPH LOOP STARTING        ║
╠══════════════════════════════════════╣
║ Project:    $PROJECT_ROOT            ║
║ Total:      [n] requirements         ║
║ Passing:    [n]                      ║
║ Remaining:  [n]                      ║
║ Max iters:  [n] per requirement      ║
╚══════════════════════════════════════╝
```

## The Loop

For each requirement where `passes: false`, in order of priority (lowest priority number first, then by id):

### Iteration start

Log to `$PROJECT_ROOT/ralph/loop.log`:
```
[timestamp] START [requirement-id] "[summary]" — iteration [n]
```

Output to stdout:
```
──────────────────────────────────────
[requirement-id] [summary truncated to 50 chars]
Iteration: [n] / [MAX_ITERATIONS]
──────────────────────────────────────
```

### Step 1 — Gap check

Run:
```bash
claude -p "/gap-check $REQUIREMENT_ID" --cwd $PROJECT_ROOT
```

Capture stdout. Read the last line starting with `GAP_CHECK_RESULT:`.

- If `GAP_CHECK_RESULT: PASSES` → log PASS, mark requirement `passes: true` in prd.json, move to next requirement
- If `GAP_CHECK_RESULT: GAPS_FOUND` → proceed to Step 2
- If no result line found → log WARNING, count as an iteration, retry if iterations remain

### Step 2 — Code fix

Run:
```bash
claude -p "/code-fix $REQUIREMENT_ID" --cwd $PROJECT_ROOT
```

Capture stdout. Read the last line starting with `CODE_FIX_RESULT:`.

- If `CODE_FIX_RESULT: DONE` → log FIX APPLIED, increment iteration count, loop back to Step 1 for this requirement
- If `CODE_FIX_RESULT: SKIPPED` → log SKIPPED, mark requirement as `needs_review: true` in prd.json, move to next requirement

### Max iterations reached

If a requirement reaches `MAX_ITERATIONS` without passing:
- Set `passes: false` and `needs_review: true` in prd.json
- Set `notes: "Exceeded max iterations [timestamp] — manual review required"`
- Log to loop.log: `[timestamp] MAX_ITERATIONS [requirement-id] — flagged for manual review`
- Move to next requirement

## After all requirements are processed

Read prd.json and count final state:
- Passing
- Needs review
- Still failing

Write a summary to `$PROJECT_ROOT/ralph/loop-summary.md`:

```markdown
# Ralph Loop Summary

**Completed:** [ISO timestamp]
**Project:** [PROJECT_ROOT]

## Results

| Status | Count |
|--------|-------|
| ✅ Passing | [n] |
| ⚠️ Needs review | [n] |
| ❌ Still failing | [n] |

## Needs Review

[List of requirement IDs that hit max iterations or were skipped]

## Still Failing

[List of requirement IDs that are still passes: false]
```

Output to stdout:
```
╔══════════════════════════════════════╗
║           RALPH LOOP COMPLETE        ║
╠══════════════════════════════════════╣
║ ✅ Passing:      [n]                 ║
║ ⚠️  Needs review: [n]                ║
║ ❌ Still failing: [n]                ║
╚══════════════════════════════════════╝

See ralph/loop-summary.md for details.
```

## Logging rules

Every action appends to `$PROJECT_ROOT/ralph/loop.log`. Never overwrite. Format:
```
[ISO timestamp] [LEVEL] [requirement-id] [message]
```
Levels: `START`, `PASS`, `FIX_APPLIED`, `SKIPPED`, `WARNING`, `MAX_ITERATIONS`, `COMPLETE`
