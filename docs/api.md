# Component 1 REST contract

Base path: `/api/v1`. See [generated OpenAPI](openapi.json) for schemas. JSON uses camelCase properties, string enums, NICs as strings and numeric versions. Unknown JSON fields and numeric enum bodies are rejected.

Protected endpoints require `Authorization: Bearer <accessToken>`. Staff accounts use username/password; prosumers use NIC/password. Login returns `accessToken`, `expiresAt`, `tokenType`, `id`, `fullName`, and `role`.

| Method | Path | Access |
|---|---|---|
| POST | `/auth/staff/login` | Public |
| POST | `/auth/prosumers/login` | Public |
| GET | `/auth/me` | Authenticated |
| POST | `/auth/logout` | Authenticated; invalidates all account sessions |
| POST | `/prosumers` | Public registration |
| GET / PATCH | `/prosumers/me` | Prosumer |
| POST | `/prosumers/me/deactivation-requests` | Prosumer |
| GET / POST | `/staff-users` | Backoffice |
| GET / PATCH | `/staff-users/{id}` | Backoffice |
| GET | `/prosumers` | Backoffice |
| GET / PATCH | `/prosumers/{nic}` | Backoffice |
| POST | `/prosumers/{nic}/deactivation-requests/{requestId}/decision` | Backoffice |
| POST | `/prosumers/{nic}/reactivation` | Backoffice |
| GET | `/dashboard` | Backoffice |

Pagination uses `page` (default 1) and `pageSize` (default 20, maximum 100), returning `{ items, total, page, pageSize }`. Prosumer lists support `search` (literal name/NIC search), `status=Active|Inactive`, and `requestStatus=Pending|Approved|Rejected`. Use `GET /prosumers?requestStatus=Pending` for the review queue.

## Register

```json
{
  "nic": "199012304567",
  "fullName": "Example Prosumer",
  "email": "prosumer@example.test",
  "phone": "+94771234567",
  "address": "10 Solar Road, Colombo",
  "password": "use-your-own-unique-password"
}
```

Returns `201` and the profile. This is sample data, not a pre-created account. Sign in using `POST /auth/prosumers/login` with `nic` and `password`.

## Edit profile

Read the latest profile, then send all editable fields to `PATCH /prosumers/me`. NIC, role and status are not editable here. The API uses PATCH to update only this defined profile portion of the account; all fields of this profile update DTO are required.

```json
{
  "fullName": "Example Prosumer",
  "email": "new-address@example.test",
  "phone": "+94771234567",
  "address": "20 Solar Road, Colombo",
  "version": 1
}
```

Returns `200` with the updated profile and new version. On `409`, reload; do not silently retry with a new version and overwrite someone else's changes.

## Deactivation and reactivation

Submit to `POST /prosumers/me/deactivation-requests`:

```json
{ "reason": "Moving to a new home", "version": 2 }
```

Returns `201` with the complete profile and its `deactivationRequest`. Use the resulting `version` and request `id` for Backoffice review:

```json
{ "decision": "Approved", "note": "Reviewed with the member", "version": 3 }
```

`decision` is `Approved` or `Rejected`. After approval, Backoffice may post to `/prosumers/{nic}/reactivation`:

```json
{ "note": "Member requested reactivation", "version": 4 }
```

## Errors

Errors use `application/problem+json`: `status`, `title`, optional `detail`, `traceId`, and either `errors` for field validation or `code` for a domain error. Authentication/authorization responses may omit `detail`; clients must fall back to `title`.

- `400`: invalid payload, missing fields, invalid NIC/enum, unknown fields, invalid pagination.
- `401`: missing, expired, forged or revoked session; invalid/unavailable login account.
- `403`: authenticated caller lacks the required role, or attempted a protected access change.
- `404`: account/request not found.
- `409`: duplicate identity/username, stale version or invalid lifecycle transition.
- `429`: too many login/registration attempts; includes `Retry-After`.
- `503`: database/account service unavailable.

An interrupted write can have succeeded even if a client did not receive the response. Refresh and inspect the account before retrying. The version check prevents duplicate accepted transitions and lost updates.
