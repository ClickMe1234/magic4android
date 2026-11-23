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
- External LG TV display verified at 1920x1080, 60Hz. HDMI appeared after the
  phone-side USB-C connector was reversed; the reason for that sensitivity is unknown.
- Selected the external DeX display and dismissed its welcome dialog with a
  synthetic click through the TV service. User confirmed physical remote pointer
  movement after Magic4PC was brought back to the foreground.
- TV sensor subscriptions now retry after remote-not-ready failures, ignore
  acknowledgement-only responses, and report last coordinates and sample time.
  Three Node regression tests cover acknowledgement, recovery and stopped sessions.
- Diagnosed black TV app screen via its web inspector: I18nDecorator crashed
  because the build omitted iLib locale assets. Enabled asset emission, rebuilt
  and installed. Verified the rendered app root and HDMI video readyState=4,
  error=null, source HDMI 1. Packaging now rejects missing iLib runtime assets.
- Navigation update: ten JVM tests pass (six protocol, four spatial navigation),
  APK build and Android lint pass, and TV rebuild/package asset checks pass.
  Both updated packages installed on the test devices.
- Injected DOM key events into the running TV web app (not directly into Android):
  arrows moved focus across ATV app tiles and between widgets/app grid; OK opened
  Plex on display 32; Back returned from Plex to ATV while Magic4PC stayed visible.
  TV navigation events were default-prevented and no web runtime errors occurred.
- DeX caption windows, missing initial focus, virtual-list focus and focus zoom
  animation are handled by target-window selection and spatial fallback.
- 4K scaling fix: device reports 3840x2160 at 60Hz while the TV web app reports
  1920x1080. Legacy source-size preferences were 3840x2160, incorrectly limiting
  movement to one quarter of the output. Removed manual source-size controls and
  map the TV app's coordinate contract to the current target display dimensions.
- Installed APK verified mapping TV (1919,1079) to Android overlay (3839,2159).
  Temporarily changed logical output size to 1920x1080 and reset it; display
  listener logged both transitions and restored 3840x2160 without reconnecting.
  Eleven JVM tests and lint/build pass, including a full-4K endpoint regression.
  Automatic reattachment after a physical/logical display replacement is implemented
  but not yet exercised by an unplug/replug test.

Not validated: physical remote navigation reliability after this update, precise pointer
alignment, background persistence, Wi-Fi reconnect, and reliable app navigation.

The TV side includes sensor recovery changes. A successful pointer test does not
establish complete remote-control compatibility across Android apps.
