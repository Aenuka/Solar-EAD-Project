# Android navigation and interface

The native Java app uses a shared navigation shell in `SolarActivity`, destinations in `SolarNavigation`, and typography, surfaces, buttons, and grouped rows in `SolarStyle`.

Prosumers land on Home after signing in. The persistent tabs are Home, Explore, Bookings, and Account. Grid operators use Overview, Completed, and Account; QR scanning is the primary action on Overview. Booking forms, transaction QR codes, registration, and account editing have a visible Back control. Both Android's legacy Back button and its API 33+ native back dispatcher use the same navigation behavior.

Tab changes return through the role's root activity using `CLEAR_TOP` and `SINGLE_TOP`, then open the destination. This prevents repeated tab switches or the booking confirmation action from accumulating screens. Detail screens return to their originating tab. Account and registration forms retain non-password drafts during activity recreation and ask before discarding changed fields. Passwords are excluded from saved drafts.

Bookings supports All, Upcoming, Pending, and Completed filters, pull-to-refresh, inline network errors, and retry. Approved bookings expose the transaction QR action; editing and cancellation are grouped under Manage booking. Booking dates display in Sri Lanka time. A successful new booking opens Bookings.

Explore is one scrollable page containing location controls, a station map, station rows, and pagination. Open it from the **Explore** tab or **Home → Explore stations**. Map gestures consume touches within the map while the rest of the page scrolls normally. Each station row and its details include **View on Google Maps**, which opens the server-provided coordinates in the Google Maps app, or a browser if Maps is unavailable. These links work without an SDK API key. The embedded map requires `mapsApiKey` in the ignored `development.properties` file, Maps SDK for Android enabled, and the key restricted to the application's package and signing certificate. All stations clears the nearby filter. Account cache notices and restrictions on offline profile changes remain in place.

Build checks:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Device verification checklist (requires a running API and a connected device or emulator):

- Sign in, switch through all tabs repeatedly, and press Android Back. Confirm no duplicate screens accumulate and the selected tab matches the page.
- Open a station, start a booking, cancel using Back, and complete another booking. Confirm View bookings opens the list and editing/QR flows return to Bookings.
- Filter populated and empty booking lists. Test pull-to-refresh and retry with the API temporarily unavailable.
- Edit Account, rotate the device, and check that non-password changes survive. Test unchanged Back, Keep editing, Discard, save conflicts, and cached profile restrictions.
- Test Explore with and without a Maps key, location denial, nearby filtering, All stations, map panning, and pagination.
- Test operator tab switching, scan/verification Back, completed operations, and sign out.
- Check small displays, enlarged system text, TalkBack, gesture and three-button navigation, and keyboard visibility. Tabs must stay clear of system controls; forms must scroll above the keyboard.

See the root README for API, SDK, and phone/emulator configuration.
