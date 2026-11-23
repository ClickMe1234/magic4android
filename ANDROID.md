# Magic4Android — experimental native receiver

For complete Android/TV build and installation instructions, see [README.md](README.md).
This fork's Android receiver and fork-specific TV changes were developed with
OpenAI Codex under human direction and tested on the owner's hardware. Upstream
Magic4PC attribution is preserved below and in the main README.

Fork of Wouter De Keersmaecker's MIT-licensed Magic4PC. The original Windows
client is retained; the webOS application includes input and recovery fixes.
The `android/` module receives the same
UDP protocol directly on an Android 11+ device. No PC relay or Bluetooth sniffer is
required by this design during use.

## Current scope

This is a prototype. Basic pointer and click operation has been tested on an LG
C4/MR24GA with an S22 Ultra's DeX display; complete app compatibility is not implied.
It requests Android Accessibility access explicitly; no root, firmware changes,
shell input permission, or automatic Accessibility enabling is used.

Implemented:

- Manual private IPv4 TV selection, upstream UDP subscription on port 42831,
  keepalives, timeout status and resubscription attempts.
- Only the explicitly selected TV endpoint is accepted by the connected socket.
- Coordinate-only sensor subscription, little-endian payload decoding and scaling.
- Cyan pointer overlay and taps/scroll gestures on the chosen Android display.
- Pointer hides after five seconds without movement or button input, and returns
  on use. Repeated sensor samples, keepalives and one-pixel jitter do not keep it awake.
- Arrows select visible accessible controls, with spatial fallback and scrolling
  at boundaries. OK activates that selection; waking the TV pointer returns to
  point-and-click. Red always clicks at the cyan dot.
- Back/green invoke Android Back when the selected display has input focus.
- TV captures navigation keys before its own UI, keeping Back inside Android.
  When the TV settings popup is open, arrows operate that popup and Back closes it.
- Explicit Connect/Stop controls; Stop closes the socket and removes the pointer.

Limitations:

- Android Accessibility cannot inject arbitrary hardware keys or gamepad events.
  Directional focus works only where an app exposes focusable accessibility nodes.
- Back is Android's global action. Click the desired TV app first if you have
  been using the phone screen; the receiver refuses Back when another display is
  focused, to avoid navigating the wrong screen.
- The cyan pointer represents touch coordinates, not Android's native mouse cursor.
  Continuous native hover, dragging, long presses, media-key injection and gaming
  button holds are not implemented.
- Pointer scaling uses the webOS app's 1920x1080 coordinate space and the live
  selected Android display size, including 4K. Legacy manual coordinate settings
  are ignored and removed on Connect. Display-size changes update automatically.
  If DeX replaces its logical display, the receiver reattaches only when exactly
  one display matches the selected display's name and external/built-in type.
  Multiple matching displays require explicit selection and reconnect.
- Background persistence and Samsung battery-management behaviour need testing.
- Upstream TV UDP traffic has no cryptographic authentication. Use only on a trusted
  home LAN; do not port-forward these services. Choosing an IP is not authentication.
- The webOS code calls LG-specific external-video and remote-sensor services.
  These work on the rooted C4 used for testing; unrooted Developer Mode permissions
  and other firmware/model combinations remain unverified.

## Build

Use JDK 17, Android SDK platform 36 and the Gradle wrapper:

```text
gradlew :android:testDebugUnitTest :android:lintDebug :android:assembleDebug
```

APK: `android/build/outputs/apk/debug/android-debug.apk`.
The debug APK is for local testing, not a production release.

## Setup / validation

1. Install the APK on the phone.
2. Install this fork's matching webOS IPK using LG Developer Mode and open it on the TV.
   Its visible name is Magic4Android TV. Select the HDMI input connected to the
   phone's powered USB-C hub. Only the rooted-TV installation path has been tested.
3. Open Magic4Android and enable its service manually in Android Accessibility.
   This permits observing app UI and performing gestures; enable it only while
   you want this remote-control functionality. Disable the service to revoke access.
4. Enter the TV's LAN IP and choose the desired phone/external display.
5. Connect, then test red/OK click, each direction, wheel and Back separately in
   Daijishou and Plex. Check cursor alignment at all four corners.
6. Test Stop, TV app closure, Wi-Fi loss/reconnect, phone restart and display unplug.

Keep the TV app foreground for button forwarding. Its service may keep sending
pointer samples in the background, but ordinary HDMI mode does not forward buttons.
Protocol unit tests are synthetic and do not establish TV compatibility.

## Architecture

MR24GA -> TV Bluetooth receiver -> forked webOS app + service -> LAN UDP ->
native Android receiver -> explicitly enabled Accessibility service -> target app.

Phone video travels independently through USB-C HDMI to the TV. Upstream's
MainPanel embeds that HDMI source underneath its input overlay, rather than
depending on a background webOS app capturing the remote.

Original attribution and MIT license are retained in `LICENSE`.
