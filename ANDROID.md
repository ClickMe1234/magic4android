# Magic4Android — experimental native receiver

Fork of Wouter De Keersmaecker's MIT-licensed Magic4PC. The original Windows client
and webOS application remain intact. The new `android/` module receives the same
UDP protocol directly on an Android 11+ device. No PC relay or Bluetooth sniffer is
required by this design during use.

## Current scope

This is a prototype, **not confirmed compatible with the LG C4/MR24GA or DeX**.
It requests Android Accessibility access explicitly; no root, firmware changes,
shell input permission, or automatic Accessibility enabling is used.

Implemented:

- Manual private IPv4 TV selection, upstream UDP subscription on port 42831,
  keepalives, timeout status and resubscription attempts.
- Only the explicitly selected TV endpoint is accepted by the connected socket.
- Coordinate-only sensor subscription, little-endian payload decoding and scaling.
- Cyan pointer overlay and taps/scroll gestures on the chosen Android display.
- OK and arrows through accessible app focus, red for pointer click,
  Back/green for Android global Back.
- Explicit Connect/Stop controls; Stop closes the socket and removes the pointer.

Limitations:

- Android Accessibility cannot inject arbitrary hardware keys or gamepad events.
  Directional focus works only where an app exposes focusable accessibility nodes.
- Global Back is Android's system action; targeting the correct DeX app is untested.
- The cyan pointer represents touch coordinates, not Android's native mouse cursor.
  Continuous native hover, dragging, long presses, media-key injection and gaming
  button holds are not implemented.
- Display layout/rotation changes require Stop and reconnect. Display selection is
  refreshed when opening the receiver activity. TV coordinates default to 1920x1080;
  confirm/calibrate these against the actual TV app rather than panel resolution.
- Background persistence and Samsung battery-management behaviour need testing.
- Upstream TV UDP traffic has no cryptographic authentication. Use only on a trusted
  home LAN; do not port-forward these services. Choosing an IP is not authentication.
- The webOS code calls LG-specific external-video and remote-sensor services. Their
  availability and permissions on C4 firmware must be verified in Developer Mode.

## Build

Use JDK 17, Android SDK platform 36 and the Gradle wrapper:

```text
gradlew :android:testDebugUnitTest :android:lintDebug :android:assembleDebug
```

APK: `android/build/outputs/apk/debug/android-debug.apk`.
The debug APK is for local testing, not a production release.

## Setup / validation

1. Install the APK on the phone.
2. Install upstream's webOS IPK using LG Developer Mode and open it on the TV.
   Select the HDMI input connected to the phone's powered USB-C hub.
3. Open Magic4Android and enable its service manually in Android Accessibility.
   This permits observing app UI and performing gestures; enable it only while
   you want this remote-control functionality. Disable the service to revoke access.
4. Enter the TV's LAN IP and choose the desired phone/external display.
5. Connect, then test red/OK click, each direction, wheel and Back separately in
   Daijishou and Plex. Check cursor alignment at all four corners.
6. Test Stop, TV app closure, Wi-Fi loss/reconnect, phone restart and display unplug.

Without the TV app running there should be no input; status should show connecting.
Protocol unit tests are synthetic and do not establish TV compatibility.

## Architecture

MR24GA -> TV Bluetooth receiver -> original webOS app + service -> LAN UDP ->
native Android receiver -> explicitly enabled Accessibility service -> target app.

Phone video travels independently through USB-C HDMI to the TV. Upstream's
MainPanel embeds that HDMI source underneath its input overlay, rather than
depending on a background webOS app capturing the remote.

Original attribution and MIT license are retained in `LICENSE`.
