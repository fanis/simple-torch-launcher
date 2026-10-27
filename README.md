# Simple Torch Launcher

[![Build](https://github.com/fanis/simple-torch-launcher/actions/workflows/build.yml/badge.svg)](https://github.com/fanis/simple-torch-launcher/actions/workflows/build.yml)

Double-press a volume button to switch the torch (flashlight) on. Double-press it again to switch it off.

- Pick volume up or volume down as the trigger.
- Single presses still change the volume as usual, after a short delay (400 ms).
- Works on the lock screen, and optionally while the screen is off (experimental).
- An on/off switch in the app, and a button to test the torch.
- Free and open source (MIT). No ads, no tracking, no internet permission, no dependencies.
- Android 6.0 (API 23) and newer, targeting Android 17 (API 37).

## Download

Coming to F-Droid. Until then, get the APK from
[Releases](https://github.com/fanis/simple-torch-launcher/releases). See the
[changelog](CHANGELOG.md) for what's new.

## Setup

1. Install and open the app. A setup screen explains why it needs an accessibility service, what it
   promises not to do, and what Android's "full control" warning means.
2. Turn on **Simple Torch Launcher** in the accessibility settings. On Android 8 and newer, a small
   floating guide shows the steps over the settings. Once it's on, the app brings you back to its
   setup screen, where you can try a double-press straight away.
3. Pick the button you want to use.

On Android 13 and newer, Android may block the switch with "Restricted setting". Open the app's
**App info** page, tap **⋮** and choose **Allow restricted settings**, then try again. The app shows
these steps, with a button to App info, if accessibility is still off after you've been to the
settings. Android has no public way for an app to check whether it is restricted.

## Privacy

- Android passes the app every hardware key press, like the volume buttons or a connected keyboard.
  It never gets the power button, touches, or typing on the on-screen keyboard.
- The app only reacts to the volume button you choose. Every other key goes straight back to
  Android, untouched.
- It asks Android for key events only: no screen content, no accessibility events, no gestures.
- It keeps nothing but its own settings, and has no internet permission.
- It's open source, so all of this can be checked.

The switch at the top of the app pauses the shortcut: the app then stops receiving key presses
altogether. To remove its access completely, tap the accessibility row in the app, or turn it off in
Android's accessibility settings.

## How it works

- **Screen on (including the lock screen):** the accessibility service sees every press of the chosen
  button and holds it back for 400 ms. If a second press follows, the torch toggles and the volume stays
  as it was. If not, the press is replayed as a normal volume change. Holding the button still ramps the
  volume.
- **Screen off (experimental):** Android doesn't send key events to accessibility services while the
  screen is off. Instead, while the screen is off and nothing is playing, the app keeps a media session
  that claims to control a remote player's volume, so Android reports volume presses to it. This
  depends on how each device routes volume keys, so it doesn't work on every phone. It also steps aside
  while music or other audio plays and during calls, so your volume buttons keep working normally
  then. It can be turned off in the app.
- **Torch:** uses `CameraManager.setTorchMode`, which needs no camera permission. It tracks the torch
  state, so a double-press also turns off a torch that was switched on from quick settings.

## Battery and resources

Nothing runs in the background: no polling and no wake locks. Android calls the app only when
a hardware key is pressed, and each key press takes a short detour through the app before reaching
other apps. Touches never go through it. When the app's own switch is off, it tells Android to stop
sending it key events at all. Like any accessibility service, the app stays loaded in memory while it
is turned on in the accessibility settings.

## Limitations

- Apps that use the chosen volume button for something else, like a camera shutter or turning pages,
  get a volume change instead while the service is on.
- The torch can't be switched on while another app is using the camera.
- If Android's Advanced Protection mode is on, it may block this service, because it isn't declared as
  an accessibility tool.
- With the screen-off option on, devices and apps that show what your phone is playing, like a
  smartwatch, a car display or some lock screen widgets, may show Simple Torch Launcher as playing
  while the screen is off. Turn the option off if that bothers you.

## Building

```sh
./gradlew assembleDebug     # installable debug build
./gradlew assembleRelease   # release build, signed if keystore.properties is set up
```

Requires JDK 17 or newer and the Android SDK (platform 37). There are no third-party dependencies.
Every push and pull request is built and linted on GitHub Actions.

## Releasing

Releases are cut with the **Cut Release** workflow (Actions tab → Cut Release → Run workflow). Add
the release notes under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md) first, then pick a bump
type or enter a version. The workflow bumps the version, moves the notes under it, writes F-Droid's
release notes, builds, commits, tags `X.Y.Z` and publishes a GitHub Release with the APK.

The APK is signed with your own key, kept in four repository secrets (Settings → Secrets and
variables → Actions): `ANDROID_RELEASE_KEYSTORE_BASE64`, `ANDROID_RELEASE_STORE_PASSWORD`,
`ANDROID_RELEASE_KEY_ALIAS` and `ANDROID_RELEASE_KEY_PASSWORD`. To create the key once, with the
JDK's `keytool`:

```sh
keytool -genkeypair -v -keystore simple-torch-launcher-release.jks -alias simple-torch-launcher -keyalg RSA -keysize 4096 -validity 10000
```

`ANDROID_RELEASE_KEYSTORE_BASE64` is the file, base64-encoded
(`base64 -w0 simple-torch-launcher-release.jks`, or in PowerShell
`[Convert]::ToBase64String([IO.File]::ReadAllBytes("simple-torch-launcher-release.jks"))`), and
`ANDROID_RELEASE_KEY_ALIAS` is `simple-torch-launcher`. Keep a backup of the keystore and its
passwords outside the repo: updates must be signed with the same key. F-Droid builds and signs
its own APKs, so copies from F-Droid and from GitHub can't update each other.

To build a signed release APK locally with the same key, put the keystore in `release/` and copy `keystore.properties.example` to `keystore.properties` at the repo root, with the real passwords. Git ignores both. `./gradlew assembleRelease` then writes a signed `app/build/outputs/apk/release/app-release.apk`. Without `keystore.properties` the release APK stays unsigned, as on GitHub Actions and F-Droid.

## Contributing

Issues and pull requests are welcome. Please keep the app small: no dependencies, no internet
access, and nothing that needs more than the accessibility service already asks for.

## License

[MIT](LICENSE) © 2026 Fanis Hatzidakis
