# Simple Torch Launcher

Double-press a volume button to switch the torch (flashlight) on. Double-press it again to switch it off.

- Pick volume up or volume down as the trigger.
- Single presses still change the volume as usual, after a short delay (400 ms).
- Works on the lock screen, and optionally while the screen is off (experimental).
- An on/off switch in the app, and a switch to test the torch.
- Free and open source (MIT). No ads, no tracking, no internet permission, no dependencies.
- Android 6.0 (API 23) and newer, targeting Android 17 (API 37).

## Setup

1. Install and open the app. A setup screen explains why it needs an accessibility service, what it
   promises not to do, and what Android's "full control" warning means.
2. Turn on **Simple Torch Launcher** in the accessibility settings. The app brings you back to its
   setup screen, where you can try a double-press straight away.
3. Pick the button you want to use.

On Android 13 and newer, Android may block the switch with "Restricted setting". Open the app's
**App info** page, tap **⋮** and choose **Allow restricted settings**, then try again. The app shows
these steps, with a button to App info, if accessibility is still off after you've been to the
settings. Android has no public way for an app to check whether it is restricted.

## Privacy

- The app only reacts to the volume button you choose. Every other key goes straight back to
  Android, untouched.
- It asks Android for key events only: no screen content, no accessibility events, no gestures.
- It keeps nothing but its own settings, and has no internet permission.
- It's open source, so all of this can be checked.

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
./gradlew assembleRelease
```

Requires JDK 17 or newer and the Android SDK (platform 37). The release APK is unsigned.

## License

[MIT](LICENSE) © 2026 Fanis
