# Prototype validation

Local validation on Windows 11, JDK 17, Gradle 8.13, AGP 8.13.2, Android SDK 36:

- Debug APK compilation passed.
- Six JVM protocol tests passed: little-endian sensor coordinates, exact payload
  size enforcement, key-release preservation, bounded coordinate conversion,
  unsupported message rejection, and wheel direction.
- Android lint passed with warnings (including English-only prototype labels,
  dependency-update notices and deliberately physical-left pointer positioning).
- Debug APK installed on a Samsung Galaxy S22 Ultra SM-S908B running Android 16.
- Main activity opened and showed connection controls and built-in display choice.
- User enabled Accessibility access on the phone.
- Installed and launched the matching TV package on a rooted LG C4 (webOS SDK
  9.2.4); its service starts and accepts the Android UDP subscription.
- Live TV-to-phone keepalive exchange verified. A synthetic mouse release sent
  through the TV service opened the phone's display selector via Accessibility.
- Moved UDP connect off the Android main thread after device testing exposed
  the connection failure. Rebuilt, reran protocol tests and lint, and installed.

Not validated: physical Magic Remote sensor/button events, external DeX display mapping, pointer
alignment, background persistence, Wi-Fi reconnect, and reliable app navigation.

The TV-side code is unchanged from upstream. Synthetic tests and successful APK
installation are not evidence of end-to-end remote-control compatibility.
