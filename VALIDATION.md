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
- Accessibility permission remains disabled until explicitly enabled by the user.

Not validated: TV installation, C4 sensor-service permissions, live protocol
exchange, Accessibility input execution, external DeX display mapping, pointer
alignment, background persistence, Wi-Fi reconnect, and reliable app navigation.

The TV-side code is unchanged from upstream. Synthetic tests and successful APK
installation are not evidence of end-to-end remote-control compatibility.
