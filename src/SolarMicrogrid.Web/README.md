# React staff portal

The web UI is React JSX with Tailwind CSS, built by Vite. ASP.NET Core hosts the
compiled assets and keeps the existing controller routes, server-side validation,
cookie authentication, antiforgery protection, and API client. The REST API,
MongoDB rules, shared DTOs, and Android application are unchanged.

## Run

Install Node.js 20.19+ (or 22.12+) and npm, alongside .NET 10 and the existing API
prerequisites. From the repository root:

```sh
python3 scripts/run_dev.py
```

`dotnet build` installs the locked npm dependencies when needed and builds the
frontend automatically. `dotnet publish` includes the generated UI. The staff
portal remains at <http://localhost:5081>.

For frontend development, keep the API and web application running, then use:

```sh
cd src/SolarMicrogrid.Web/ClientApp
npm run dev
```

This watches JSX/CSS and rebuilds the assets served by ASP.NET Core. Refresh the
browser after a change; there is no separate frontend origin or CORS setup.

## Structure

- `ClientApp/src/main.jsx` mounts React and the rendering error boundary.
- `ClientApp/src/App.jsx` provides navigation, session-aware layout, feedback,
  and form submission. All screens use existing `/Account`, `/Staff`,
  `/Prosumers`, `/Stations`, and `/Bookings` URLs, including direct links.
- `ClientApp/src/pages/` contains account, station, and booking JSX components.
- `ClientApp/src/components.jsx` shares accessible forms, dialogs, tables,
  pagination, notices, and cards.
- `ClientApp/src/styles.css` imports Tailwind and defines the blue/green theme.
- `Presentation/ReactPageResult.cs` sends controller page data in a React
  bootstrap document or JSON when `Accept: application/json` is requested.
- `Controllers/` and `ViewModels/` retain existing form binding, validation,
  version checks, and API calls. Controllers inherit `ControllerBase` through
  `PortalController` and return explicit `ReactPageResult` responses. There are
  no `View(...)` calls, view engines, or Razor services. Razor compilation is
  disabled for both build and publish.
- `Security/PortalAntiforgeryFilter.cs` validates unsafe HTTP requests without
  depending on the view framework. Encrypted redirect notices use the standard
  cookie TempData provider, registered independently of view services.
- `Security/AuthenticationExtensions.cs` checks the server-held API token and
  rejects revoked browser sessions. Passwords are excluded from page JSON;
  bearer tokens remain inside the encrypted HTTP-only session cookie.

Initial documents contain safely escaped JSON, an antiforgery token, and the
compiled module/CSS references. React renders all UI markup. Form submissions
use `FormData`, preserve repeated schedule-day values and optimistic concurrency
versions, and request JSON. Redirects return fresh page data and tokens. Invalid
forms retain non-secret server values and display validation messages. Navigation
links load their server route, so refresh, bookmarks, and back/forward work.

All station date/time inputs and displays use Sri Lanka time. Protected staff
access controls, Backoffice-only actions, and server authorization remain intact.
The old `.cshtml`, Bootstrap grid, and imperative station DOM scripts are removed.

## Verify

```sh
dotnet build SolarMicrogrid.sln --disable-build-servers -m:1 /p:UseSharedCompilation=false
python3 scripts/verify.py --no-build
cd src/SolarMicrogrid.Web/ClientApp
npm test
npm run build
```

Python tests exercise the real API and isolated MongoDB, including both HTML
bootstrap and JSON form responses, authentication, permissions, concurrency,
staff/prosumer lifecycle, and station operations. React interaction tests cover
form submissions, validation, role-specific controls, booking approval, network
failures, and pagination filters.

Frontend references: [React root API](https://react.dev/reference/react-dom/client/createRoot)
and [Tailwind with Vite](https://tailwindcss.com/docs/installation/using-vite).
