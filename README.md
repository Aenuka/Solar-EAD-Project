# Smart Solar Microgrid Trading System

Component 1: user authentication, staff access and prosumer account management.

Component 4 (Chamithu): station management, operating schedules, energy window inventory and native Android Google Maps discovery. See [station setup, Visual Studio/Android Studio checks and integration contracts](docs/stations.md). Staff can open **Microgrid stations** in the web portal; signed-in prosumers can open **Explore microgrid stations** in Android.

## Projects

| Project | Responsibility |
|---|---|
| `src/SolarMicrogrid.Api` | ASP.NET Core REST API, account rules, authorization and MongoDB access |
| `src/SolarMicrogrid.Contracts` | API request/response DTOs shared by the .NET applications; no database dependencies |
| `src/SolarMicrogrid.Web` | ASP.NET Core MVC staff portal; calls the API over HTTP |
| `mobile/SolarMicrogrid.Android` | Native Java Android application using Android widgets, HTTP and SQLite |
| `tests` | HTTP integration tests against a real, isolated MongoDB instance |

```text
MVC controllers → API client ─────────────┐
                                        ├→ API endpoints → Services → Repositories → MongoDB
Android UI → AccountRepository → HTTP ───┘
                  ↕
             SQLite cache
```

MongoDB is authoritative. SQLite stores only the signed-in prosumer's last server-confirmed profile. Clients do not connect to MongoDB.

## Run the API and web application

Prerequisites: .NET SDK 10.0.300 or later in the .NET 10 family, Python 3, and a MongoDB connection (Atlas in the cloud, `mongod` on PATH, or Docker).

### MongoDB Atlas with local Android SQLite

```text
Android app → local .NET API → MongoDB Atlas (solar_microgrid)
     ↕
SQLite profile cache on the Android device
```

Run `python3 scripts/init_dev.py` once if local settings do not exist. Add a `Mongo` section to `src/SolarMicrogrid.Api/appsettings.Local.json`, preserving its existing `Jwt` and `Bootstrap` sections:

```json
"Mongo": {
  "ConnectionString": "mongodb+srv://<database-user>:<encoded-password>@<cluster-host>/?appName=SolarMicrogrid",
  "DatabaseName": "solar_microgrid"
}
```

Use the connection string from Atlas **Connect → Drivers**, a database user with read/write access to `solar_microgrid`, and allow your computer's current IP in the Atlas project's Network Access list. See the [Atlas connection prerequisites](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/). The local settings file is ignored by Git; keep the real URL there or in the API process's `Mongo__ConnectionString` environment variable. Putting it in the Web launch profile does not configure the API.

Start with `python3 scripts/run_dev.py`. With Atlas configured, the script does not start or require local MongoDB or Docker. Restart an already-running API after changing its connection string. The API creates the collections and initial staff account on first startup. Existing records in local MongoDB are not automatically copied to Atlas.

Android saves the server-confirmed profile to SQLite after sign-in, registration, successful edits, and Refresh. During a network failure or temporary server/database outage, Refresh can show the cached profile in an existing valid session. Tap Refresh when the connection returns to fetch the latest cloud data. This is a profile cache: offline edits are not queued, and restarting the app process requires online sign-in. Keep the API running on your computer while testing Android; using Atlas does not host the API itself.

### Start the API and web portal

If using Docker, start MongoDB first:

```sh
docker compose up -d mongo
```

Then, from the repository root:

```sh
python3 scripts/run_dev.py
```

The script creates local credentials, builds the solution, and starts the API and MVC app. When configured for the default local MongoDB connection, it starts an installed `mongod` with data in `.local/mongodb` if port 27017 is not already listening. Atlas connections skip that step. It only stops processes it started.

- Staff portal: <http://localhost:5081>
- API: <http://localhost:5080/api/v1>
- OpenAPI in Development: <http://localhost:5080/openapi/v1.json>
- Database health: <http://localhost:5080/health>
- Generated staff login: `.local/development-credentials.txt`
- Logs: `.local/api.log`, `.local/web.log`, `.local/mongo.log`

The password and JWT key are randomly generated, never hard-coded. `appsettings.Local.json` and `.local/` are ignored by Git. Initialization never resets an existing account's password.

To run projects separately:

```sh
python3 scripts/init_dev.py
dotnet run --project src/SolarMicrogrid.Api
# In another terminal:
dotnet run --project src/SolarMicrogrid.Web
```

For your own environment, configure `Mongo__ConnectionString`, `Mongo__DatabaseName`, `Jwt__SigningKey` (at least 32 characters), `Jwt__Issuer`, `Jwt__Audience`, and the web application's `Api__BaseUrl`. For first-time staff provisioning, provide `Bootstrap__Username`, `Bootstrap__Password` (12–128 characters), `Bootstrap__Email`, and optionally `Bootstrap__FullName`. Keep these in environment settings or user secrets.

## Run Android

Open `mobile/SolarMicrogrid.Android` in Android Studio. The project uses Java 17, Gradle 8.13, Android Gradle Plugin 8.11.1, compile/target SDK 36 and minimum SDK 26.

Create `local.properties` with your SDK location if Android Studio has not done so. On macOS, this is typically:

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
```

With the API running, launch an Android emulator and run the `app` configuration. Without an override, the debug app connects to `http://10.0.2.2:5080/api/v1/`, which reaches the host computer from the emulator. This address does not reach your computer from a physical phone.

```sh
cd mobile/SolarMicrogrid.Android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

For a USB-connected phone:

1. Start the API in Rider and check `http://localhost:5080/health` returns `{"status":"healthy"}` on your computer.
2. Enable USB debugging, connect the phone and accept its debugging prompt. Run `adb devices` to check it is listed as `device`.
3. Run `adb reverse tcp:5080 tcp:5080`. On macOS, if `adb` is not in PATH, use `"$HOME/Library/Android/sdk/platform-tools/adb" reverse tcp:5080 tcp:5080`.
4. Copy `development.properties.example` to `development.properties` in `mobile/SolarMicrogrid.Android`. It sets `apiBaseUrl=http://127.0.0.1:5080/api/v1/`. This file is ignored by Git and applies only to debug builds.
5. Sync Gradle and Run `app` in Android Studio to rebuild and install the updated app. Keep the API running and USB connected. Re-run the reverse command after reconnecting or restarting the phone.

To verify forwarding, open `http://127.0.0.1:5080/health` in the phone browser; it should also return `{"status":"healthy"}`. A timeout at registration/login usually means the API address, forwarding or running API needs checking. If registration timed out after reaching the server, try signing in first: the account might already have been created.

To return to an emulator, remove the local `development.properties` override or set its URL to `http://10.0.2.2:5080/api/v1/`, then rebuild. Command-line `-PapiBaseUrl=...` overrides the local file. Debug cleartext access is restricted to emulator/loopback addresses. Release builds require an explicit HTTPS `-PapiBaseUrl=https://your-api.example/api/v1/` and your signing configuration; local debug settings are never used for release builds.

Android keeps the bearer token in memory, so restarting the process requires an online sign-in. SQLite supports cached profile viewing during a connection failure or HTTP 5xx server/database outage in an existing session. Authentication and authorization errors do not fall back to cached data. Cached data is labelled and writes require a successful refresh. Logging out clears the cache; passwords and tokens are never stored in SQLite or preferences.

## Component 1 behavior

- Backoffice and Grid Operator share staff login. The API determines their role from MongoDB.
- Only Backoffice can create/manage staff, inspect/edit prosumer profiles, decide deactivation requests, and reactivate accounts.
- Grid Operators have a protected landing page; trading/grid operations belong to later components.
- Prosumers register and log in using NIC and password, view/edit their profile, and submit deactivation requests in Android.
- NIC is the MongoDB `_id` of a prosumer and is immutable through profile updates. Legacy NICs normalize to the corresponding 12-digit identifier, preventing duplicate accounts across formats. Validation checks format/year/day-code plausibility; it does not verify official issuance or ownership.
- A pending request leaves the account active. Approval makes it inactive and revokes existing sessions. Rejection leaves it active. Backoffice reactivation requires a new login.
- Every write to an existing profile/account includes its `version`. Stale writes return `409`; clients must reload before retrying.
- Role/status changes and API logout invalidate existing tokens through a server-side security version checked on every authenticated request. API logout affects all sessions for that account.
- The bootstrap administrator cannot be demoted/deactivated. Staff cannot change their own role/status. Usernames are immutable.

## Verify

```sh
dotnet build SolarMicrogrid.sln
python3 scripts/verify.py --no-build
cd mobile/SolarMicrogrid.Android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The Python suite starts temporary API/MVC/MongoDB processes and cleans them up afterward. It tests authorization, forged tokens, uniqueness under concurrent registration, optimistic concurrency, opposing deactivation decisions, session revocation, account reactivation, MVC login/antiforgery and rate limiting. MongoDB data is isolated from development data. If `mongod` is unavailable, use `TEST_MONGO_URL` for a disposable server; each test run creates a uniquely named database there and leaves that database for inspection.

The test runner also refreshes `docs/openapi.json` from the running API. Native Android unit tests cover request shapes, profile parsing and validation error parsing. `assembleDebug` produces `mobile/SolarMicrogrid.Android/app/build/outputs/apk/debug/app-debug.apk`.

See [architecture and authorization](docs/architecture.md) and [API examples](docs/api.md).
