# Forge Domain — Business Description

## What this domain is about

The Forge domain manages identity, access, and communication across the WasteHero platform. It handles everything required to onboard users to companies, authenticate them securely, grant permissions based on roles, and send notifications. Forge is the foundation for user management, multi-tenant isolation, and role-based access control across all services. No other domain can distinguish who a user is, what company they belong to, or what they are allowed to do without Forge.

## Core business entities

**User** — A unique individual in the WasteHero system. Users are completely independent of companies — they can belong to multiple companies simultaneously, transition between companies, or work for several at once. Each user has a username, email, and one or more authentication methods.

**Company** — A tenant/organisation in the WasteHero platform. Companies are fully isolated from each other: their users, roles, settings, and branding are all scoped to the company. A company has a business ID (e.g., Y-tunnus), timezone, active status, and a demo flag.

**CompanyUser** — The employment relationship between a user and a company. Tracks when the user joined, when they left (null = still employed), and whether the employment is currently active. Users can rejoin companies; history is preserved.

**UserProfile** — Display information for a user: first/last name, preferred language, timezone, date/time format preferences, and avatar.

**UserPrivacy** — Security and privacy block settings. A user can be blocked from accessing the system for regulatory reasons (Finnish turvakielto / privacy block). Includes reason, activation date, and who authorised the block.

**UserIdentity** — Flexible identity document storage. Allows a user to have multiple forms of identification (Finnish SSN, passport, national ID, driver's licence, tax ID). Required for regulatory compliance (KYC/AML).

**UserMFASettings** — Multi-factor authentication configuration. Each user can enable multiple MFA methods simultaneously: authenticator app (TOTP), email OTP, or SMS OTP.

**UserAuthentication** — Credential storage by authentication type: password (hashed), Suomi.fi OAuth, Active Directory, SSO, or API key. Separated by type to support multiple login paths per user.

**UserSession** — Active login sessions and tokens. Tracks access tokens, refresh tokens, expiry, the application that requested the token, and the grant type used.

**UserInvitation** — Lifecycle record for pending company invitations. Status progression: Pending → Sent → Clicked → Accepted (or Expired). Token is time-limited (7 days by default) and single-use.

**CompanyRole** — A role defined within a company (e.g., "Main User," "Office User," "Driver"). Roles are company-scoped: the same role code can have different permissions across companies. System roles (predefined) cannot be modified; custom roles can be freely managed.

**Permission** — A specific action right scoped to a service and resource (e.g., `iam.user.create`, `fleet.vehicle.read.assigned`). Permissions use wildcard patterns enabling fine-grained delegation (e.g., `*:read` = read access to everything, `*` = superadmin).

**UserRole** — Assignment of one role to a user within a specific company. One role per user per company; enforced as a constraint.

**Email Template** — A named template (subject + HTML/text body) used to render transactional emails. Stored per company, supports placeholder variables. Used for OTPs, MFA codes, invitations, and notifications.

**Email Log** — Delivery record for every email sent: recipient, template, provider used, status (sent/failed/bounced), timestamp, and idempotency key for deduplication.

**CompanySettings** — Localisation and operational configuration per company: timezone (IANA), supported languages (ISO 639-1), currency (ISO 4217), country (ISO 3166-1), date/time format.

**CompanyBranding** — Visual identity per company: logo, colours, theme. Used when rendering email templates and the frontend UI.

**CompanyMFAPolicy** — MFA requirements per company: which MFA method is mandatory, and for which application component.

## Business rules

- **Multi-tenancy and isolation**: Each company is fully isolated. Users only see data for companies they belong to. Permissions are always company-scoped.
- **User-company independence**: A user is not owned by a company. Users can be invited to multiple companies simultaneously. The CompanyUser junction table manages this relationship, not a direct foreign key.
- **One role per user per company**: Each user has exactly one role within each company.
- **Soft delete with email reuse**: Records are never permanently deleted (soft delete via deleted_at). Partial unique indexes allow email/username reuse after deletion.
- **MFA enforcement**: MFA is verified before issuing tokens. Company policy can require specific MFA methods. Users can have multiple MFA methods enabled simultaneously.
- **Token revocation and blacklisting**: Access tokens can be revoked immediately by blacklisting their token ID (JTI). This enables instant logout and session termination.
- **Invitation expiry**: Invitations expire after 7 days. Invitation tokens are single-use (clicking the link marks it as clicked; accepting marks it as accepted).
- **Privacy block (turvakielto)**: A blocked user cannot access the system until the block is explicitly removed by an authorised administrator. Reason and authoriser are recorded.
- **Settings validation**: Company settings (timezone, language, currency) are validated against international standards before saving.
- **Rate limiting on notifications**: Email and SMS sending is rate-limited per user and per company per hour to prevent abuse.
- **Idempotency on email sends**: Clients provide an idempotency key; duplicate sends within the validity window are detected and skipped.

## Business flows

### Inviting a new user to a company
1. Administrator selects one or more email addresses to invite.
2. System creates an invitation record per email with status Pending.
3. Invitation email is sent to each address with a secure, time-limited link (7 days).
4. Invitation status updates to Sent. Email delivery is logged.
5. User receives email and clicks the link; status updates to Clicked.
6. User completes signup form (username, password, name, language preferences).
7. System verifies the invitation token, creates the User record, sets up authentication credentials, creates the CompanyUser relationship, and assigns the default role for that company.
8. Invitation status updates to Accepted. User can now log in.

### Logging in with password and MFA
1. User submits username/email and password.
2. System verifies the password hash.
3. If MFA is enabled for the company, a one-time code is sent via the configured channel (email or SMS) or the user is prompted for their authenticator app code.
4. User submits the MFA code; system validates it (TOTP: current time window ± 30 seconds; email/SMS: code match).
5. On successful authentication and MFA, system issues a signed access token and a refresh token.
6. Access token contains user ID, company ID, role, permissions, and expiry.
7. User includes the access token in all subsequent requests.

### Granting a role to a user
1. Administrator selects the user and the desired role within the company.
2. System verifies the user is a member of that company.
3. System verifies the role belongs to that company.
4. System updates the UserRole record with the new role, recording who made the change and when.
5. The user's new permissions take effect on their next API call.

### Revoking a user's access
1. Administrator or user initiates logout or access revocation.
2. System adds the access token's JTI (token ID) to a blacklist with TTL equal to the token's remaining validity.
3. Session is marked as revoked in the audit log.
4. All subsequent requests using that token are rejected with 401 Unauthorised.
5. User must authenticate again to obtain a new token.

### Enabling an authenticator app (TOTP)
1. User navigates to security settings and selects "Enable Authenticator App."
2. System generates a TOTP secret (RFC 6238) and returns it with a QR code.
3. User scans the QR code into their authenticator app (Google Authenticator, Authy, etc.).
4. User submits the current 6-digit code from the app.
5. System verifies the code against the secret. On success, TOTP MFA is stored as enabled.
6. Future logins require the TOTP code after the password step.

### Sending a notification (any domain)
1. Any domain publishes a notification request with: recipient, template name, variables, company ID, and an idempotency key.
2. Communication service receives the request and checks for duplicates using the idempotency key.
3. Template is looked up by company and name, rendered with the provided variables.
4. Rate limit is checked (per user and per company per hour).
5. Email is sent via the company's configured provider (primary, with fallback if unavailable).
6. Delivery result (sent/failed/bounced) is logged with the provider's message ID.

## Key business terminology

- **User**: An individual account in WasteHero, independent of any single company.
- **Company (Tenant)**: An isolated organisation. All data and settings are scoped to the company.
- **CompanyUser**: The employment relationship; tracks when a user joined and left a company.
- **Role**: A named set of permissions, scoped to a company.
- **Permission**: A specific action right in format `{service}.{resource}.{action}`. Supports wildcard patterns for delegation.
- **RBAC (Role-Based Access Control)**: Access model where users have a role and roles have permissions.
- **MFA (Multi-Factor Authentication)**: Second verification step after password. Methods: TOTP (authenticator app), Email OTP, SMS OTP.
- **MFA Policy**: Company-wide requirement specifying which MFA method is mandatory.
- **TOTP (Time-based One-Time Password)**: RFC 6238 standard; 6-digit codes valid for ~30 seconds, generated by authenticator apps.
- **Invitation**: A time-limited (7 days), single-use offer to join a company, delivered via email.
- **Soft Delete**: Logical deletion via `deleted_at` timestamp. Records are retained for audit but excluded from active queries.
- **Token Blacklist**: Cache of revoked access token IDs, checked on every request to deny access to logged-out sessions immediately.
- **Privacy Block (Turvakielto)**: Finnish regulatory requirement. A blocked user cannot access the system. Reason and authoriser are recorded.
- **Idempotency Key**: A client-supplied identifier used to detect and skip duplicate notification requests.
- **Email Provider**: External email delivery service (e.g., SendGrid, Nylas). Companies can configure multiple providers for redundancy.
- **Email Template**: Named template (per company) with subject and body, using placeholder variables for dynamic content.
- **Grant Type**: OAuth2 authentication flow type (password, refresh_token, client_credentials, authorization_code).
- **Refresh Token**: Long-lived opaque token used to obtain new access tokens without re-entering credentials.
- **CompanySettings**: Localisation configuration: timezone, languages, currency, country, date/time format.

## What this domain does NOT handle

- **Business domain logic**: CRM, billing, routing, waste management rules — all belong to their respective domains.
- **Customer data**: Customer master data (not to be confused with users) is managed by the Nexus domain.
- **Address and geocoding**: Handled by a shared platform address service.
- **Dynamic metadata storage**: Extended metadata for users and companies is stored in the platform metadata service (EVA pattern).
- **SMS and push notifications**: Currently email only; SMS and push are planned but not yet implemented.
- **Audit trail of business actions**: Each domain logs its own business events; Forge does not centralise business audit logs.
- **User analytics and dashboards**: Handled by analytics and reporting services.
