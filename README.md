# Concert Logify

Concert Logify is an Android app for keeping track of concerts, setlists, and photos.

## Getting Started

1. Clone or download this repository and extract it if needed.
2. Open the folder containing `settings.gradle.kts` in Android Studio.
3. Let Gradle sync and install any required SDK components.
4. Start an Android emulator, select the `app` configuration, and click Run.
5. Create an account on the login screen to start using the app. Passwords must be at least six characters long.

## Spotify Setup (Optional)

Spotify is used to search for artists and retrieve their pictures.

1. Install Python 3.9 or newer.
2. Create a Web API app at the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard). The developer app owner needs Spotify Premium. If a redirect URI is required, use `http://127.0.0.1:8765/callback`.
3. Copy the Client ID and Client Secret from the app's settings.
4. Open the `spotify_backend` folder and run `start_spotify.bat` on Windows. On macOS or Linux, run `python3 spotify_backend/server.py` from the project root.
5. Enter your Client ID and Client Secret when prompted. Leave the terminal open while using Spotify search in the Android Studio emulator.
6. In Concert Logify, tap **Logify It!**, enter an artist's name, and tap **Search Spotify**.

The app can still be used without the Spotify service. Concerts, setlists, and photos are saved locally using SQLite and device storage. Do not commit your Spotify credentials to the repository.

For additional setup information, see [SPOTIFY_SETUP.md](SPOTIFY_SETUP.md).
