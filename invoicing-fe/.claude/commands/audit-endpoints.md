You are auditing all API endpoints used by this frontend against the running backend at http://localhost:8080.

## Step 1 — Discover all endpoints

Read every file in `src/api/`. For each file extract:
- The HTTP method (get/post/put/patch/delete)
- The URL path (e.g. `/api/v1/billing-events`)
- Any path parameters (e.g. `${id}`) — substitute `1` as a placeholder
- The file name and line number

Build a flat list like:
```
GET  /api/v1/billing-events
POST /api/v1/billing-events
GET  /api/v1/billing-events/1
PATCH /api/v1/billing-events/1/components
...
```

## Step 2 — Test every endpoint

Use `curl` to hit each endpoint against `http://localhost:8080`. Always include the required headers:
```
-H "X-Role: INVOICING"
-H "X-User: jack.spinola"
-H "Content-Type: application/json"
```

For POST/PUT/PATCH requests send a minimal valid body `{}` unless you can infer the required shape from the request DTO in the backend (`invoicing/src/main/java/.../dto/`).

Collect the HTTP status code for each request. Example curl command:
```bash
curl -s -o /dev/null -w "%{http_code}" -X GET http://localhost:8080/api/v1/billing-events \
  -H "X-Role: INVOICING" -H "X-User: jack.spinola"
```

## Step 3 — Classify results

- **OK**: 2xx responses
- **Expected failure**: 404 when using placeholder ID `1` that doesn't exist — skip these
- **Broken**: 5xx responses, or 4xx that are NOT 404-with-placeholder-id
- **Skipped**: endpoints that require a real file upload or complex binary body

## Step 4 — Diagnose broken endpoints

For each broken endpoint:
1. Find the corresponding `@RestController` method in `invoicing/src/main/java/`
2. Find the `@Service` method it calls
3. Look at the stack trace (if available from the curl response body) or reason through the code
4. Identify the root cause (missing DB column, null passed where NOT NULL expected, wrong status transition, missing migration, etc.)

## Step 5 — Fix

Apply the fix. Common patterns in this codebase:
- `null` passed as `reason` to `buildAudit()` when `billing_event_audit_log.reason` is NOT NULL → replace with a descriptive string
- Missing DB columns → add an `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` to a new migration file in `invoicing/src/main/resources/db/migration/`
- Status constraint violations → check `V27__fix_billing_event_status_check.sql` and add missing statuses
- Wrong import (`import axios from 'axios'` instead of `import axios from './axios'`) in frontend API files

After fixing, re-run the curl for that endpoint to confirm it now returns 2xx.

## Step 6 — Report

Produce a table:

| Method | Path | Status | Result |
|--------|------|--------|--------|
| GET | /api/v1/billing-events | 200 | OK |
| PATCH | /api/v1/billing-events/1/components | 500 → 200 | Fixed: null reason in audit log |
| ... | ... | ... | ... |

List any endpoints you could not fully test and why.
