---
description: Orchestrate the full UI check loop across all requirements in ralph/prd.json. Runs ui-check for each requirement individually, then performs a full orphaned-UI audit across the entire frontend. No questions asked — fully autonomous.
argument-hint: [none needed]
allowed-tools: [Read, Write, Bash]
---

# UI Loop

You are the UI loop orchestrator. You run `ui-check` for every requirement in `ralph/prd.json` one at a time, collect the results, then perform a full orphaned-UI audit comparing every frontend route against every requirement. You do not interact with the user. You do not ask questions. You drive everything to completion and write a full coverage map.

---

## Setup

### 1. Detect project root

```bash
PROJECT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
```

### 2. Verify ralph folder

Check that `$PROJECT_ROOT/ralph/prd.json` exists. If not, output:
```
ERROR: ralph/prd.json not found.
Run this command from your project root, or create ralph/prd.json first.
```
and stop.

Create output folders:
```bash
mkdir -p $PROJECT_ROOT/ralph/ui/billing-events
mkdir -p $PROJECT_ROOT/ralph/ui/invoices
mkdir -p $PROJECT_ROOT/ralph/ui/invoice-runs
mkdir -p $PROJECT_ROOT/ralph/ui/service-responsibility
mkdir -p $PROJECT_ROOT/ralph/ui/reporting
mkdir -p $PROJECT_ROOT/ralph/ui/integration
mkdir -p $PROJECT_ROOT/ralph/ui/general
```

### 3. Read prd.json

Load all requirements. Count:
- Total requirements
- Already mapped (`ui_mapped: true`)
- Remaining

### 4. Locate the frontend root

Apply the same heuristics as ui-check to find `$FE_ROOT`.

Output the opening banner:
```
╔══════════════════════════════════════════╗
║          UI LOOP STARTING                ║
╠══════════════════════════════════════════╣
║ Project:     $PROJECT_ROOT               ║
║ Frontend:    $FE_ROOT                    ║
║ Total reqs:  [n]                         ║
║ Already mapped: [n]                      ║
║ To process:  [n]                         ║
╚══════════════════════════════════════════╝
```

---

## Phase 1 — Per-requirement UI checks

For each requirement in `prd.json`, in order (do not skip already-mapped ones — always re-run to keep reports fresh):

### For each requirement

Log to `$PROJECT_ROOT/ralph/ui/ui-loop.log`:
```
[timestamp] START [requirement-id] "[summary truncated to 50 chars]"
```

Output to stdout:
```
──────────────────────────────────────────
[requirement-id]  [summary truncated to 50 chars]
──────────────────────────────────────────
```

Run:
```bash
claude -p "/ui-check $REQUIREMENT_ID" --cwd $PROJECT_ROOT
```

Capture stdout. Read the last line starting with `UI_CHECK_RESULT:`.

Map the result:
- `UI_CHECK_RESULT: UI_MAPPED` → log `MAPPED`, update prd.json: set `ui_mapped: true`
- `UI_CHECK_RESULT: PARTIAL_UI` → log `PARTIAL`, update prd.json: set `ui_mapped: false`, `ui_partial: true`
- `UI_CHECK_RESULT: NO_UI` → log `NO_UI`, update prd.json: set `ui_mapped: false`, `ui_no_frontend: true`
- No result line found → log `WARNING`, set `ui_mapped: false`

Output the per-requirement result:
```
  ✅ UI_MAPPED   [requirement-id]
```
or `⚠️ PARTIAL_UI` or `❌ NO_UI`.

After each requirement, write the updated `prd.json` back to disk.

---

## Phase 2 — Full orphaned-UI audit

After all requirements are processed, perform the full orphan audit. This finds UI pages that exist in the frontend but cannot be connected to any requirement.

### Step 1 — Collect all routes

Read `$FE_ROOT/src/App.jsx`. Extract every `path` from every `<Route>` element. This is your list of all frontend pages.

### Step 2 — Collect all requirement references

Read every report file in `$PROJECT_ROOT/ralph/ui/**/*.md`. For each file, extract every page component name and route path mentioned in the "Files Inspected" and "User Flows" sections.

Also scan `ralph/prd.json` — some requirements mention route paths or feature names in their acceptance criteria text. Extract those too.

### Step 3 — Cross-reference

For each route from Step 1, determine:
- Does any requirement report reference this route or the component at this route?
- Does any requirement's acceptance criteria text mention a feature clearly housed at this route?

Classify each route as:
- `COVERED` — clearly linked to at least one requirement
- `ORPHANED` — no requirement references this route or its feature

### Step 4 — Deep-check orphaned routes

For each `ORPHANED` route, read the page component file. Quickly assess:
- What does this page do?
- Is it a supporting page (e.g. a detail page for a list that IS covered)?
- Is it a genuinely unlinked feature?

Mark supporting pages (detail pages, nested forms reachable from covered pages) as `SUPPORTING — [parent route]` rather than orphaned.

Only flag as truly `ORPHANED` if the page:
- Cannot be reached from any covered page via normal navigation, OR
- Provides functionality with no clear requirement backing it

---

## Phase 3 — Write outputs

### UI coverage summary

Write to `$PROJECT_ROOT/ralph/ui/ui-coverage-summary.md`:

```markdown
# UI Coverage Summary

**Generated:** [ISO timestamp]
**Project:** [PROJECT_ROOT]
**Frontend:** [FE_ROOT]

---

## Requirements → UI Coverage

| Requirement | Summary | UI Status | Domain | Report |
|-------------|---------|-----------|--------|--------|
| PD-xxx | [summary truncated] | ✅ UI_MAPPED | billing-events | [link] |
| PD-yyy | [summary truncated] | ⚠️ PARTIAL_UI | invoices | [link] |
| PD-zzz | [summary truncated] | ❌ NO_UI | integration | [link] |

---

## Statistics

| Status | Count |
|--------|-------|
| ✅ UI_MAPPED (all ACs visible) | [n] |
| ⚠️ PARTIAL_UI (some ACs missing) | [n] |
| ❌ NO_UI (backend only) | [n] |
| ⚠️ WARNING (check failed) | [n] |

---

## Orphaned UI Audit

### Truly Orphaned Pages

These frontend routes exist and provide functionality that cannot be linked to any requirement in prd.json:

| Route | Page Component | What it does | Severity |
|-------|---------------|--------------|----------|
| /some/route | SomePage | [plain English description] | 🔴 UNLINKED |

### Supporting Pages (not orphaned)

These routes have no direct requirement mapping but are naturally reached from covered pages:

| Route | Reachable from |
|-------|---------------|
| /billing-events/:id | Billing Events list (PD-318, PD-277) |

---

## Recommendations

### Requirements needing UI attention (PARTIAL or NO_UI)

[For each PARTIAL_UI or NO_UI requirement, one bullet explaining what is missing or why there is no UI]

### Orphaned pages needing requirements

[For each truly orphaned page, one bullet suggesting which requirement it might belong to or whether a new requirement should be written]
```

### Loop log entry

Append to `$PROJECT_ROOT/ralph/ui/ui-loop.log`:
```
[timestamp] COMPLETE — [n] mapped, [n] partial, [n] no_ui, [n] orphaned routes found
```

---

## Final output to stdout

```
╔══════════════════════════════════════════╗
║          UI LOOP COMPLETE                ║
╠══════════════════════════════════════════╣
║ ✅ UI_MAPPED:   [n] requirements         ║
║ ⚠️  PARTIAL_UI:  [n] requirements         ║
║ ❌ NO_UI:       [n] requirements         ║
║                                          ║
║ 🔴 Orphaned pages:  [n]                  ║
║ 🟡 Supporting pages: [n]                 ║
╚══════════════════════════════════════════╝

See ralph/ui/ui-coverage-summary.md for the full map.
```

---

## Logging rules

Every action appends to `$PROJECT_ROOT/ralph/ui/ui-loop.log`. Never overwrite. Format:
```
[ISO timestamp] [LEVEL] [requirement-id or ORPHAN] [message]
```
Levels: `START`, `MAPPED`, `PARTIAL`, `NO_UI`, `WARNING`, `ORPHAN_FOUND`, `COMPLETE`
