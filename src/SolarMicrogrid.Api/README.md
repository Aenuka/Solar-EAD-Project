# Understanding the backend

This API follows the organization of the supplied Julio Casal examples in
`Referece code/game-store-source-code/GameStore.Api` and
`Referece code/HowToProtectWebApi_Source_Code`. The tutorial-inspired sections
have a short reference comment linking to https://www.youtube.com/@juliocasal.

## Read the code in this order

1. **`Program.cs`** registers services, configures the HTTP pipeline, maps endpoints,
   and initializes the database.
2. **`Endpoints/`** groups routes by feature. For example, `ProsumersEndpoints.cs`
   maps registration, the signed-in user's profile, and Backoffice actions.
   `MapGet`, `MapPost`, `MapPatch`, and `MapPut` select the HTTP verb.
   `RequireAuthorization` selects the allowed roles. `ClaimsPrincipal` provides
   the authenticated user's ID and role.
3. **`../SolarMicrogrid.Contracts/Dtos/`** defines the request and response DTOs,
   with one type per file as in the tutorial. A DTO
   describes the data sent over HTTP. Its validation attributes check things
   such as required names, email addresses, and version numbers. These shared
   types retain their existing names and namespace because the staff portal uses them too.
   Request classes keep their parameterless constructors and property validation
   for MVC form binding; responses use the tutorial's compact record pattern.
4. **`Services/`** contains the application's rules. For example,
   `ProsumerService.UpdateProfileAsync` finds an account, checks its version,
   changes its properties, and saves it. `StationService` checks capacity,
   operating hours, reservations, and cancellation rules before saving.
5. **`Data/`** contains MongoDB setup and the three concrete repositories.
   A repository reads and writes documents in a collection. `DataExtensions`
   registers them, and `MongoInitializer` creates account indexes and the initial
   staff account. There is no generic repository or extra interface layer.
6. **`Models/`** contains database documents, one type per file. Editable documents
   are ordinary classes with properties. Small event/allocation values remain
   records. These types are separate from response DTOs so password hashes and
   session versions never appear in API responses.
7. **`Security/`** verifies passwords, creates JWTs, and configures bearer
   authentication. `Configuration/` holds settings, error handling, and OpenAPI
   configuration.

## What comes from the tutorials

| Supplied example | Pattern used here |
| --- | --- |
| Game Store `Program.cs` and `Endpoints/` | A short startup file, endpoint extension methods, route groups, dependency injection into handlers, named routes, and creation responses. |
| Game Store `Data/DataExtensions.cs` | Database registration and initialization in a dedicated extension class. |
| Game Store `Models/` | Plain editable model classes and explicit property updates. |
| Game Store `Dtos/` | One DTO per file, validation attributes on inputs, and records for response data. |
| Game Store validation | `AddValidation()` with data annotations on input DTOs and query parameters. |
| JWT example `Program.cs` | JWT bearer authentication, `ClaimsPrincipal`, and role requirements attached to routes. |

The examples are a style reference, not a replacement for this application's
rules. MongoDB remains the database; the Game Store's SQLite/Entity Framework
setup was not copied. Services remain useful because account decisions and
station inventory require more checks than the tutorial's game CRUD operations.
The JWT example validates tokens; this project's existing token issuance,
password hashing, and session revocation are application-specific.

`WithJsonBody<T>()` is a small compatibility helper: it lets the route's
anonymous/role policy run before the JSON binder rejects an unsupported content
type. This preserves `401`/`403`/`415` behavior during the move from controllers.
The API still accepts only JSON bodies, and OpenAPI describes them as JSON.

## Follow one update

For `PATCH /api/v1/prosumers/me`:

1. JWT authentication checks the signature, issuer, audience, expiry, and stored
   account status/security version.
2. The endpoint requires the Prosumer role and gets the NIC from the token.
3. Validation checks the JSON request. Unknown JSON properties are rejected.
4. `ProsumerService` loads the document and checks the submitted `version`.
5. The service assigns the editable fields and adds a recent event.
6. `SaveAsync` remembers the previous version, increments the version, and calls
   `ReplaceAsync`. MongoDB matches **both the ID and previous version**. If another
   request saved first, this write fails with `409` instead of overwriting it.
7. `ToResponse` creates the same profile DTO that Android already understands.

Editing a loaded model only changes that request's in-memory object. Nothing is
written until the repository call, so a rejected capacity/schedule/account check
does not partially update MongoDB. Embedded account decisions and station
allocations are still saved atomically with their parent document.

## Compatibility and verification

The refactor retains the `/api/v1` routes, HTTP methods, DTO/JSON property names,
login response, HS256 JWT claims, role names, MongoDB collection/field names,
validation, and concurrency behavior. Shared Contracts are now organized into
separate files without changing their public declarations. The MVC portal has
also been refactored; see [its reading guide](../SolarMicrogrid.Web/README.md).
The Android application remains unchanged.

From the repository root:

```sh
dotnet build SolarMicrogrid.sln
python3 scripts/verify.py --no-build
```

The integration suite uses an isolated MongoDB database. It exercises account
and station workflows, authorization, revoked/forged tokens, concurrent writes,
the existing staff portal, and additional API contract checks in
`tests/test_api_contract.py`. No Android changes are required.

## Scope of the 6,413 added lines

The initial commit (`4b3e11a`) adds 6,350 lines and the connection fix
(`167996b`) adds 63, giving 6,413 additions. This is a Git contribution count
across the API, MVC portal, contracts, Android app, CSS, generated OpenAPI,
tests, scripts, and documentation. It is not a count of C# backend lines.

The refactor covers all three .NET projects in the current checkout, including
the station functionality added later. API and MVC C# code use clear methods,
model updates and file organization; DTOs/view models each have their own file;
Razor markup is expanded for readability. Android files and static CSS/JavaScript
assets are unchanged. Fewer lines are not the goal: readable blocks replace
compressed one-line statements while preserving behavior.
