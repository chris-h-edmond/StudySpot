# StudySpot

StudySpot is an educational Android app for browsing library study spaces, selecting a seat from a visual map, and keeping local reservations on the device.

## Stack

- Kotlin, Jetpack Compose and Material 3
- MVVM, ViewModel, Coroutines, Flow and StateFlow
- Room for local users, libraries, seats and bookings
- DataStore Preferences for the local signed-in user ID
- PBKDF2-HMAC-SHA256 salted password hashes
- Navigation Compose
- Google Maps Compose when a Maps API key is configured
- A booking-only Seat Guide powered by Groq Chat Completions

## Run

Open the project in Android Studio or run `gradlew :app:assembleDebug`. On first launch, the app seeds two demonstration libraries and their seats into Room. Register using a name, email and password of at least eight characters.

To enable the optional library map, add a restricted Android Maps API key to the untracked `local.properties` file:

```properties
MAPS_API_KEY=your_restricted_android_maps_key
```

The app shows a manual library list when this value is absent. The sample library coordinates are illustrative and should be replaced for a real campus. Location permission and current-device location are not requested in this MVP.

To enable Seat Guide, add `GROQ_API_KEY` to the same ignored `local.properties` file. The key supplied for this workspace is configured locally. The app calls Groq's Chat Completions API directly using the `openai/gpt-oss-20b` model. Seat Guide is available from the seat map only, receives the selected interval and a locally calculated list of free seat IDs, asks one focused follow-up at a time, stays on seat planning, and cannot create a booking. It will not claim seat-level quietness because noise data is not stored.

## Scope and privacy

StudySpot is a local-first student demonstration. Accounts, password hashes and reservations are stored in Room on this device. Seat Guide sends chat text and limited library/time/available-seat context to Groq; it does not send account credentials or booking records. Do not enter personal information in assistant chat. Reservations are not synchronized or visible to other devices, so availability is not live campus-wide availability. Local authentication is not a production university identity system. Logging out clears the local session and retains the account and booking history.

The Groq key is read from ignored `local.properties` and compiled into the debug APK for this classroom demo. Anyone who receives that debug APK could extract it. The release variant deliberately does not contain the key. Do not distribute the debug APK; revoke/rotate the key if it has been shared, and use a server-side proxy for a public release.

## Build and tests

- `gradlew :app:assembleDebug`
- `gradlew :app:testDebugUnitTest`
- `gradlew :app:connectedDebugAndroidTest` (requires an emulator or device)

The application ID is currently `com.example.studyspot`; configure the final release ID, signing, policy disclosures and store listing before publication. No signed AAB or Play Store publication is claimed by this repository setup.
