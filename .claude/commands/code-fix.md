---
description: Read a scope-issue MD file and implement the fixes in the backend and frontend code
argument-hint: <md-file> [folder]
allowed-tools: [Read, Edit, Write, Glob, Grep, Bash]
---

# Code Fix

You are implementing fixes identified by a scope-review agent for this MVP project.

The project has:
- **Backend**: Spring Boot Java at `invoicing/src/main/java/com/example/invoicing/`
- **Frontend**: React/Vite at `invoicing-fe/src/`

## Input

Arguments: `$ARGUMENTS`

Parse the arguments:
- First token is the MD filename (e.g. `missing-scope.md`)
- Optional second token is the folder to look in (defaults to `Docs/scope-issues/`)

Resolve the file path:
1. If the argument is an absolute path, use it directly.
2. If it contains a `/`, treat it as relative to the project root `/mnt/c/Users/drasm/Desktop/Invoicing/`.
3. Otherwise look for it in `issues2fix/` first, then the project root.

Read the resolved MD file. If it cannot be found, tell the user the resolved path you tried and stop.

If the resolved filename contains `.done.` (e.g. `my-file.done.md`), stop immediately and tell the user this file has already been processed.

## How to work through the file

The MD file will contain a list of missing or broken scope items. Each item typically describes:
- What feature or endpoint is missing or incomplete
- Whether it is Backend, Frontend, or Both
- File paths or class/component names involved
- Expected behavior

Work through each issue one at a time:

1. **Understand** — Read the issue description carefully.
2. **Locate** — Use Grep and Glob to find the relevant existing files. Don't guess paths; verify.
3. **Fix** — Implement the full end-to-end change. Every fix must cover **both** layers unless the issue explicitly states it is backend-only or frontend-only:
   - Backend: add/edit Java files (entity, repository, service, controller, DTO, endpoint).
   - Frontend: add/edit React components, API helper calls, route wiring, and any UI that exposes the backend change to the user.
   - Never leave a backend change without a corresponding frontend that surfaces it, and vice versa. If only one side is described in the issue, infer and implement the other side based on the existing code conventions in the project.
4. **Verify** — After editing, re-read the changed file section to confirm the change is correct.
5. **Mark done** — After finishing each issue, output a one-line summary: `✓ Fixed: <short description>`.

## Rules

- Fix only what the MD file describes. Do not refactor surrounding code or add unrequested features.
- Follow the existing code conventions in each layer (package names, naming style, annotation patterns).
- If an issue is unclear or requires a decision, ask the user before implementing — don't guess.
- If an issue depends on another issue in the same file, fix the dependency first.
- Do not add comments unless the logic is non-obvious.
- After all issues are done, output a summary table:

```
## Fix Summary
| # | Issue | Status |
|---|-------|--------|
| 1 | <description> | ✓ Fixed |
| 2 | <description> | ✓ Fixed |
...
```

## UI Navigation Guide

After the summary table, output a **"Where to see these changes"** section. For each fix, describe concretely where the user can verify it in the running app:

- Which page or route to navigate to (e.g. `/billing-events`, `/invoices`, `/products`)
- What UI element to interact with (e.g. "open the Create Invoice modal", "click the Transfer button on a billing event")
- What the user should now see that was missing or broken before

If the fix is backend-only with no direct UI surface (e.g. a background service or validation rule), say so explicitly and describe how to trigger it indirectly (e.g. "submit a form that would have failed before" or "check the response in the network tab").

Assume the frontend dev server runs at `http://localhost:5173` and the backend at `http://localhost:8080`.

## After the summary

Ask the user: "All fixes are done. Should I mark this file as processed? (yes/no)"

- If the user confirms, rename the file by inserting `.done` before `.md` — e.g. `my-file.md` → `my-file.done.md` — using `mv` via Bash.
- If the user declines, leave the file as-is.
