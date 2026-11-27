# Spotify artist search — setup and final demonstration

The original Android app still runs independently using SQLite. The new Spotify integration adds public artist catalog search and artist images; it does not use Spotify accounts for Concert Logify login, retrieve setlists, or play music. No Spotify secret or token is included in the Android code, source ZIP or Git repository.

## 1. Create a Spotify developer app

- Go to https://developer.spotify.com/dashboard and sign in. The owner of a Development Mode app needs an active Spotify Premium subscription. A teammate can create the developer app if needed.
- Click Create app. Use `Concert Logify` as the app name and `Concert diary with artist catalog search` as its description. Select Web API. If the form requires a redirect URI, enter `http://127.0.0.1:8765/callback`. It is a dashboard placeholder only: the Client Credentials grant does not redirect a browser or use this callback. Accept the developer terms if you agree to them and save.
- In the app's Settings, copy the Client ID and reveal/copy the Client Secret. Keep the secret private. Do not put it in Gradle, Android strings, GitHub, Canvas or your demonstration video.

Spotify documentation: https://developer.spotify.com/documentation/web-api/tutorials/client-credentials-flow ; https://developer.spotify.com/documentation/web-api/concepts/quota-modes ; https://developer.spotify.com/documentation/web-api/reference/search .

## 2. Start the local service (Windows)

- Extract the entire updated project. Install Python 3.9+ from https://www.python.org/downloads/ if you do not have it.
- In File Explorer open `ConcertLogify/spotify_backend`, then double-click `start_spotify.bat`. Alternatively, in a terminal opened at the project root run `py -3 spotify_backend/server.py` (or `python spotify_backend/server.py`).
- Paste the Client ID and press Enter. Paste the Client Secret and press Enter; the secret input is intentionally hidden. The script checks the credentials and should print `Concert Logify Spotify service running at http://127.0.0.1:8765`.
- Leave the window open. The service needs an internet connection. No pip packages, database servers, web hosting or extra Gradle dependencies are needed. Credentials are held in memory; the script asks again next time. Advanced option: set the `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` environment variables privately before launching instead.
- Optional sanity check in your computer browser: `http://127.0.0.1:8765/health` should return `{"status":"ok"}`. Test actual Spotify search with `http://127.0.0.1:8765/search?q=Tyler` (results depend on Spotify). Do not share screenshots containing credentials.

## 3. Run the updated Android project

Open the extracted `ConcertLogify` folder containing `settings.gradle.kts` in Android Studio, sync, and run the default debug `app` on the standard Android Studio emulator. You do not need to make any code edits. The emulator reaches the local service via `10.0.2.2:8765`. On Android 17/API 37 the app may request local network permission when you tap Search Spotify. Allow it if you want to use the integration. The debug network configuration permits cleartext traffic only to the emulator's host alias; upstream Spotify requests are HTTPS.

Do not uninstall the previous app or clear its storage if you want to preserve your test data. The database upgrades from version 1 to version 2 by adding a nullable `spotify_artist_id` column, retaining existing accounts, concert records, songs and photos. The app ID and existing signing configuration are unchanged. If an incompatible debug signing key prevents installing, back up what you need before uninstalling.

## 4. Test the feature

1. Log in to Concert Logify as before. Tap Logify It! and type `Tyler` in the artist field.
2. Tap Search Spotify. Choose the intended artist from the live results. The selected name fills the artist field and its image should appear in the preview. The selection includes an Open on Spotify link. If Spotify supplies no image, the placeholder is shown.
3. Select the concert date, enter the venue, and save. Return from Setlistify to Home: the concert card should display the artist image and link. The fixed plus icon opens the Add song / Add photo menu.
4. Rotate to landscape and back. Close/relaunch the app and log into the same account. The selected artist ID and all local records should remain. Spotify images are fetched online and temporarily cached in memory only; they are not copied into permanent gallery storage.
5. Try editing the artist to another name: the former Spotify association is cleared. You can also choose a personal cover photo, which overrides the Spotify cover while retaining its artist link. Selecting a new Spotify result replaces the previous cover; Remove Picture clears the cover and Spotify association but retains the artist name.
6. Stop the service (Ctrl+C) and search again: the app should display an error and allow manual artist entry, manual cover selection, setlists, photos, and all other SQLite functionality. Existing Spotify-linked concert names and local data remain available, but online images may show placeholders until the service is restored.

## Troubleshooting

| Symptom | Action |
|---|---|
| `py` or `python` not recognized | Install Python 3.9+ and reopen your terminal. |
| Client credentials rejected | Copy the correct Client ID and Client Secret from Spotify dashboard Settings; restart the service. |
| 403/access denied | Check that the developer app has Web API access and its owner has active Premium. |
| 429/quota or rate limit | Stop repeated requests and try later. Development Mode has request limits. |
| Cannot reach Spotify service | Check the service window and the computer browser health endpoint. Use the default Android Studio emulator and debug build. On Android 17, allow Nearby devices/local network permission in app settings. |
| Search works but image missing | Check internet connectivity on the emulator; some artists have no image. Try another artist. |
| Existing records not present | Make sure you installed over the previous build on the same emulator and signed in to the same local account. |

## Submission and limitations

The provided full project ZIP includes the service source and instructions. Record the live Spotify search in your final demo along with the five rubric requirements. Submit the project ZIP and a separate ZIP containing only the demo video. Your grader can run local features without credentials; live Spotify search requires a running service with valid credentials (e.g. the grader's own developer app). Do not include your own Client Secret in the submission. This service is intentionally local to the development computer; a standalone production APK or a physical phone outside the emulator would require a separately deployed authenticated HTTPS service and corresponding client configuration. No production service has been deployed.

Source-level Java and XML parsing, schema migration simulation, and Python unit/HTTP tests using simulated Spotify responses were completed. Live Spotify credentials and an Android SDK/emulator were unavailable to the editor, so you must verify the real API and the updated app on your machine. Run `py -3 -m unittest discover -s spotify_backend -v` to repeat service tests.

Spotify image attribution: artist images are displayed without cropping, with an artist link and Spotify icon. The vector icon was adapted from Font Awesome Free (Fonticons, Inc.), CC BY 4.0, https://fontawesome.com/license/free ; https://creativecommons.org/licenses/by/4.0/ . Spotify trademarks remain the property of their respective owners. Review https://developer.spotify.com/documentation/design before public distribution.
