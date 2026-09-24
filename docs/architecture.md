# Component 1 architecture

Station, energy-window and map architecture (Component 4) is documented in [stations.md](stations.md), including the shared atomic inventory contract for booking and QR integrations.

For a tutorial comparison and guided reading order, see [Understanding the backend](../src/SolarMicrogrid.Api/README.md).

## Boundaries

The .NET solution contains API, MVC and Contracts projects. Contracts has only DTOs, role/status values and input annotations. MVC references Contracts but never the API implementation or MongoDB driver. Android has independent Java DTOs matching the same JSON contract.

API endpoint groups own HTTP binding, endpoint authorization and response status. Services own registration, immutable identity, allowed account transitions and concurrency rules. Concrete repositories in `Data/` own MongoDB filters and persistence. Password hashing and signed-token creation are in the API Security layer. JSON rejects unknown fields to prevent accidentally accepting administrative properties in public requests.

MVC uses a typed HTTP client. The signed API token is contained in an encrypted, HttpOnly authentication ticket, never rendered into a page. MVC checks its session with the API, applies page-level role checks and validates antiforgery tokens on POSTs. These checks supplement API enforcement. Android Activities handle forms; AccountRepository coordinates network and cache; ApiClient handles HTTP; ProfileCache uses Android SQLiteOpenHelper.

## Permission matrix

| Action | Anonymous | Prosumer | Grid Operator | Backoffice |
|---|---|---|---|---|
| Register prosumer / submit login credentials | Yes | Yes | Yes | Yes |
| Read current authentication identity / logout | No | Yes | Yes | Yes |
| Read/edit own prosumer profile | No | Yes | No | No |
| Request own deactivation | No | Yes | No | No |
| Staff management | No | No | No | Yes |
| Prosumer administration / request decisions / reactivation | No | No | No | Yes |
| Grid Operator landing page | No | No | Yes | Separate overview |

Self-profile endpoints derive the NIC from the authenticated subject, not request data. Administrative routes require the Backoffice role. The service additionally protects bootstrap/self access settings and enforces valid transitions. A bearer token alone does not prove an account remains active: validation reads the authoritative account status, role and security version on every request.

## MongoDB documents

- `staffUsers`: string `_id`, normalized unique `username`, `fullName`, `email`, password hash, role, status, version, securityVersion, timestamps, protected-bootstrap flag, recent events.
- `prosumers`: canonical NIC string `_id`, profile fields, password hash, fixed Prosumer role, status, version, securityVersion, timestamps, latest deactivation request, recent events.

Staff usernames have a unique index. Prosumer uniqueness is guaranteed by MongoDB's `_id` index. Status and latest-request status have additional indexes. On initial collection creation, JSON schema validators constrain required account metadata, types and role/status values. Updating validators for an already deployed collection requires a deliberate migration.

The latest deactivation request is embedded with its reason, decision status, reviewer, timestamps and decision note. Account status, decision, security version and recent event are written together in one atomic document replacement guarded by the expected version. This works on a standalone MongoDB server and avoids a multi-document transaction requirement.

Recent events are explicitly bounded to the last 100 per account. The API returns this recent history, and MVC displays the latest 10. This is not a permanent compliance audit archive; a full retention policy and archive can be added independently. A later request replaces the embedded latest request, while its preceding events remain within that bounded history.

All API timestamps are UTC ISO-8601 with MongoDB millisecond precision. Names/addresses are trimmed; passwords are never trimmed. NIC normalization follows the [Department for Registration of Persons FAQ](https://drp.gov.lk/en/faq.php). Account validation does not establish a person's legal identity.

## Account lifecycle

```text
Registration → Active
Active + request → Active / Pending
Pending + rejection → Active / Rejected
Pending + approval → Inactive / Approved + revoke sessions
Inactive + Backoffice reactivation → Active + require fresh login
```

A pending request cannot be duplicated. Reviewed/stale requests cannot be decided again. Reactivation of an already-active account returns a conflict. The first implementation does not enforce trading balances or open-booking rules because those components do not yet exist; add their eligibility checks inside ProsumerService before enabling those workflows.

## Sessions and deployment configuration

JWT access tokens last 30 minutes by default (configurable 5–60 minutes). No refresh-token or password-recovery workflow is implemented. Current-user profile edits do not revoke sessions; role/status changes and logout do. Bootstrap creation is idempotent and never resets an existing password.

Auth endpoints are rate limited by client IP (30 requests/minute by default). MVC server-originated logins share its API-facing address. For a multi-instance deployment, use a shared rate-limit store and configure trusted proxy handling deliberately. Inactive/revoked sessions fail closed if account state cannot be confirmed.

Before deployment, provide HTTPS endpoints, private MongoDB credentials, persistent protected MVC data-protection keys, appropriate AllowedHosts, and externally managed JWT/bootstrap secrets. Local development intentionally uses loopback HTTP. Do not deploy local settings or the unauthenticated development MongoDB container to a public interface. Android release configuration refuses an HTTP API URL.

## Extension points

Trading/booking eligibility belongs in API services. Email/NIC ownership verification, password change/recovery, MFA, refresh sessions and permanent audit retention are separate additions. The Grid Operator UI intentionally exposes no invented trading data. There are no shared database services in MVC or MongoDB credentials in Android.
