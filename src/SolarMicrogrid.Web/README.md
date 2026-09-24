# Understanding the staff portal

This project is the MVC client for the API. It uses the same readable C# and
one-type-per-file organization as the supplied Julio Casal examples. The
examples use Minimal APIs rather than MVC, so this portal keeps its existing
controllers, views, cookie authentication, and antiforgery protection.

Read these files in order:

1. `Program.cs` configures MVC, registers the API client and authentication, then
   runs the middleware and the existing MVC route.
2. `ApiClients/ApiClientExtensions.cs` configures the API address and HTTP timeouts.
   Its registration extension follows the tutorial's short-startup approach.
3. `Security/AuthenticationExtensions.cs` configures the browser session cookie.
   It calls `auth/me` to check whether the stored API token is still valid. When
   the API revokes a session, the portal also signs the browser out.
4. `Controllers/` validates forms, calls the API, and chooses a view or redirect.
   Business rules such as station capacity and deactivation decisions stay in
   the API. Controllers do not connect to MongoDB.
5. `ViewModels/` contains one class per form or display model. Required/range
   attributes validate submitted forms; `StationSlotForm` converts Sri Lanka
   local times to timestamps containing a UTC offset.
6. `Views/` contains the existing Razor pages, with expanded layout and control
   blocks. Form names, actions, hidden version fields, and page content remain
   the same. MVC still generates antiforgery tokens for POST forms.
7. `ApiClients/MicrogridApiClient.cs` sends requests with the API token and reads
   responses. `ApiFailureException` carries errors; `ApiExceptionFilter` handles
   failures that a controller has not already displayed in the form.

For example, creating a staff account starts in `StaffController.Create`.
Invalid form values return the same view with validation messages. A valid form
becomes a `CreateStaffRequest` and is posted to `staff-users`. The API checks the
Backoffice role, validates the request, hashes the password, and writes MongoDB.
The portal redirects to the staff list only after the API succeeds.

The browser uses an encrypted HTTP-only cookie, while the API uses a JWT bearer
token. These are the existing two parts of sign-in, not alternative mechanisms.
Logging out calls the API to revoke the account's sessions and then clears the
browser cookie. Neither passwords nor JWTs are rendered into the HTML.

The shared DTO names, namespaces, constructors, annotations, and JSON fields are
unchanged. The [backend guide](../SolarMicrogrid.Api/README.md) explains the API
structure and how it relates to the tutorials.

From the repository root, verify the complete .NET flow with:

```sh
dotnet build SolarMicrogrid.sln
python3 scripts/verify.py --no-build
```

`tests/test_01_web_contract.py` covers failed sign-in, invalid forms, logout, and
revoked browser sessions. The existing integration tests also cover role access,
antiforgery, staff/profile changes, decisions, and station forms.
