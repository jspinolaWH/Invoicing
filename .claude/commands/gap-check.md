---
description: Scan the codebase against a single PD requirement and write a structured gap report. No questions asked — fully autonomous.
argument-hint: <requirement-id>
allowed-tools: [Read, Glob, Grep, Bash, Write]
---

# Gap Check

You are an autonomous code-scanning agent. Your job is to take a single requirement from `prd.json`, scan the codebase, and write a structured gap report. You do not ask questions. You do not create Linear or Jira tasks. You produce one output file and exit.

## Setup

### 1. Detect project root

```bash
PROJECT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
```

All paths below are relative to `$PROJECT_ROOT`.

### 2. Load the requirement

Read `$PROJECT_ROOT/ralph/prd.json`.

The argument is `$ARGUMENTS` — this is a requirement ID (e.g. `PD-364`).

Find the requirement object where `id == $ARGUMENTS`. If not found, output:
```
ERROR: Requirement $ARGUMENTS not found in prd.json
```
and stop.

If the requirement already has `passes: true`, output:
```
SKIP: $ARGUMENTS already passes — nothing to check
```
and stop.

### 3. Locate the codebase

Scan `$PROJECT_ROOT` for the backend and frontend roots using these heuristics — in order, stop at first match:

**Backend:**
- Look for `src/main/java` → Spring Boot project
- Look for `*.py` with `requirements.txt` or `pyproject.toml` → Python project
- Look for `package.json` with a `main` or `server` entry → Node backend

**Frontend:**
- Look for `src/` with `index.html` and `package.json` containing `react` or `vite` → React/Vite
- Look for `pages/` with `package.json` containing `next` → Next.js

Store the detected roots. If nothing is found, note "codebase structure unknown" in the report and do your best with `$PROJECT_ROOT`.

## Scanning

With the requirement loaded and codebase located, scan for coverage of each acceptance criterion.

For each acceptance criterion in the requirement:

1. **Extract keywords** — pull 3-5 domain terms from the criterion (entity names, action verbs, field names)
2. **Search the codebase** — use Grep to find those terms across backend and frontend files
3. **Read relevant files** — open any files that match and read the relevant sections
4. **Make a verdict** — for this criterion, is it:
   - `IMPLEMENTED` — code exists that clearly covers this criterion
   - `PARTIAL` — some code exists but coverage is incomplete
   - `MISSING` — no relevant code found

Also check for:
- Backend endpoint exists but no frontend surface
- Frontend UI exists but no backend wiring
- Data model exists but no business logic

## Output

Write the gap report to:
```
$PROJECT_ROOT/ralph/gaps/$ARGUMENTS.md
```

Use exactly this structure:

```markdown
# Gap Report: [requirement-id]

**Requirement:** [summary from prd.json]
**Scanned:** [ISO timestamp]
**Verdict:** PASSES | GAPS_FOUND

## Acceptance Criteria Coverage

| # | Criterion | Status | Notes |
|---|-----------|--------|-------|
| 1 | [criterion text truncated to 80 chars] | IMPLEMENTED / PARTIAL / MISSING | [brief note] |
| 2 | ... | ... | ... |

## Gaps Found

[Only present if verdict is GAPS_FOUND]

### Gap 1: [short title]
- **Layer**: Backend / Frontend / Both
- **Criterion**: [which AC this relates to]
- **What exists**: [what code was found, if anything]
- **What is missing**: [specific description of what needs to be built]
- **Files to change**: [file paths if known, otherwise best guess]

### Gap 2: ...

## Files Checked

- [list of files that were read during scanning]

## Verdict

PASSES
```
or
```
GAPS_FOUND
```

The final line of the file must be exactly `PASSES` or `GAPS_FOUND` — this is what the orchestrator reads.

## After writing the report

Update `$PROJECT_ROOT/ralph/prd.json`:
- If verdict is `PASSES`: set `passes: true` and `notes: "Verified [timestamp]"`
- If verdict is `GAPS_FOUND`: set `passes: false` and `notes: "Gaps found [timestamp] — see ralph/gaps/$ARGUMENTS.md"`

Write the updated `prd.json` back to disk.

Then output to stdout:
```
GAP_CHECK_RESULT: PASSES
```
or
```
GAP_CHECK_RESULT: GAPS_FOUND
```

Nothing else. The orchestrator reads this line.
