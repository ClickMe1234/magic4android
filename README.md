# Magic4Android

Use an LG Magic Remote to control Android apps and Samsung DeX through your webOS TV.

**An experimental, AI-developed fork of [Wouterdek/Magic4PC](https://github.com/Wouterdek/magic4pc).** The Android receiver and this fork's changes were developed using **OpenAI Codex**, with human direction and hands-on device testing by the repository owner. Significant portions of the new code and documentation were generated and iterated by AI. Testing is limited to the setup below; it is not a general compatibility or security audit.

Magic4PC was created by **Wouter De Keersmaecker** to use an LG Magic Remote as a PC input device. This fork builds on its TV app, service and network protocol, adds a native Android receiver, and fixes issues encountered on a newer TV. The original [MIT license and copyright notice](LICENSE) are preserved. This project is not affiliated with LG, Samsung or OpenAI.

## How it works

```text
LG Magic Remote --Bluetooth--> LG TV / Magic4Android TV
                                        |
                                  local-network UDP
                                        |
                                        v
                                Magic4Android receiver
                                        |
                              Android Accessibility
                                        |
                                        v
                                Android apps / DeX

Phone --USB-C video adapter + HDMI--> TV picture and sound
```

The remote stays paired with the TV. **Magic4Android TV** displays the phone's HDMI picture underneath an input overlay and forwards events to **Magic4Android** on the phone. A PC is needed for building/installing, but is not part of the remote-control path during use. The Android phone does not need root.

**Keep Magic4Android TV open on the TV.** Ordinary HDMI mode shows the same picture but bypasses button forwarding. The background service can still send pointer samples, so a moving dot alone does not establish that clicks are being captured.

## Features and controls

| Remote action | Android behavior |
| --- | --- |
| Move/wake the LG pointer | Move the cyan touch-position indicator |
| Centre OK / wheel press | Click at the pointer, or activate an item selected with arrows |
| Red coloured button | Click at the cyan dot |
| Directional ring | Navigate accessible controls, with spatial fallback |
| Wheel scroll | Send a touch-scroll gesture near the pointer |
| Back / green coloured button | Android Back on the focused selected display |
| Five seconds without interaction | Hide the cyan dot; movement/input shows it again |

Select the phone screen or an external display in the receiver. Scaling maps the TV app's **1920 × 1080 coordinate space** to the live Android display size, including **3840 × 2160**. Resolution changes update automatically. If DeX recreates a display, reattachment is attempted only when exactly one display matches the selected target's name and built-in/external type.

Arrows are an Accessibility-based approximation of keyboard navigation, **not injected hardware keys**. They remain inconsistent in some apps. Waking the LG pointer returns to point-and-click. In the TV settings popup, arrows operate that popup and Back closes it.

## Requirements and tested setup

- An LG webOS TV with a Magic Remote and a way to sideload the TV app: Developer Mode or a rooted/homebrew setup.
- An Android 11+ device that can run an Accessibility service. Wired TV output also requires USB-C video-output support and a compatible adapter.
- Phone and TV on the same trusted LAN, with UDP allowed between them.
- For the tested wired setup: a powered USB-C HDMI hub, HDMI cable and DeX-capable Samsung phone.

Tested during development on **23 November 2026**:

| Component | Observed setup |
| --- | --- |
| Phone | Galaxy S22 Ultra SM-S908B, Android 16 / One UI 8 |
| TV | LG C4 OLED42C44LA, rooted, webOS SDK 9.2.4 |
| Remote | LG MR24GA |
| Video | DeX at 1080p/60 Hz and 4K/60 Hz through a powered Anker hub |
| Remote input exercised | ATV Launcher Pro and Plex; not exhaustive app testing |

Physical pointer movement and red/OK clicks were confirmed. Back/arrows were exercised through the running TV app and on-device apps; the owner reports that arrows mostly work but remain imperfect. 4K scaling, idle hiding and complete-circle rendering at edges were checked on-device. **Unrooted TV permissions, other models, unattended restart recovery and long-term background reliability are not established.** See [VALIDATION.md](VALIDATION.md).

## Source and build artifacts

```sh
git clone --branch android-receiver https://github.com/ClickMe1234/magic4android.git
cd magic4android
```

The default branch is `android-receiver`. [GitHub Actions](https://github.com/ClickMe1234/magic4android/actions) produces Android and TV artifacts when the corresponding workflows succeed. These are development builds, not a stable signed release. Upstream Magic4PC releases do not contain this Android receiver.

## Build the Android APK

### Prerequisites

- **JDK 17**, with `JAVA_HOME` pointing to its installation directory.
- [Android SDK command-line tools or Android Studio](https://developer.android.com/studio). For a manual installation, place command-line tools under `ANDROID_HOME/cmdline-tools/latest/`.
- Android SDK platform **36**, Build Tools **35.0.0**, and Platform Tools (`adb`).
- Git and internet access for initial dependency downloads.

The included **Gradle 8.13 wrapper** uses **Android Gradle Plugin 8.13.2**, compile/target SDK 36 and minimum SDK 30. No separate Gradle installation is needed. [AGP compatibility documentation](https://developer.android.com/build/releases/agp-8-13-0-release-notes) lists JDK 17 and Build Tools 35.0.0.

### Windows / PowerShell

Run from the repository root. Set `JAVA_HOME` to your actual JDK 17 installation first; adjust the SDK path if needed.

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
java -version

& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" `
  "platform-tools" "platforms;android-36" "build-tools;35.0.0"

.\gradlew.bat --no-daemon :android:testDebugUnitTest :android:lintDebug :android:assembleDebug
```

### Linux / macOS

Use your actual SDK location; Android Studio commonly uses a different path on macOS. `JAVA_HOME` must point to JDK 17.

```sh
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
java -version

"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;android-36" "build-tools;35.0.0"

chmod +x gradlew
./gradlew --no-daemon :android:testDebugUnitTest :android:lintDebug :android:assembleDebug
```

Review and accept SDK licenses when prompted. Alternatively, supply the SDK path through an untracked `local.properties` file containing `sdk.dir=...`.

**APK:** `android/build/outputs/apk/debug/android-debug.apk`.

Debug APKs use the local debug signing key. Keep the same key for updates: a build from another machine or CI may have a different signature and cannot necessarily upgrade an existing installation. Production signing/store distribution are not configured; see [Android app signing](https://developer.android.com/studio/publish/app-signing) for distributable releases.

### Install on the phone

Enable USB debugging, connect the phone, and approve your computer. From the repository root:

```sh
adb devices
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

With multiple devices, use `adb -s DEVICE_SERIAL install -r ...`. Already-paired wireless ADB also works. Debugging is for installation/development; it is not needed for the phone-to-TV connection during use.

## Build the webOS TV IPK

### Prerequisites

Use **Node.js 24 LTS** and npm. Local Windows development was tested with Node 24.18.0 / npm 11.16.0. The inherited Enact/Webpack toolchain needs the OpenSSL compatibility option below on modern Node. Locked dependencies include `ares-package`; no global packaging CLI is needed.

### Windows / PowerShell

From the repository root:

```powershell
Set-Location webos
npm.cmd ci --ignore-scripts --no-audit --no-fund
$env:NODE_OPTIONS = '--openssl-legacy-provider'
node --test test/sensor-recovery.test.cjs
npm.cmd run build-p
npm.cmd run package
```

### Linux / macOS

```sh
cd webos
npm ci --ignore-scripts --no-audit --no-fund
export NODE_OPTIONS=--openssl-legacy-provider
node --test test/sensor-recovery.test.cjs
npm run build-p
npm run package
```

`build` makes a development bundle with readable diagnostics; `build-p` makes a production-mode web bundle. Both use `npm run package` afterward. Dependency deprecation notices reflect the inherited toolchain, which has not undergone a general dependency modernization.

**IPK:** `webos/me.wouterdek.magic4pc_1.0.0_all.ipk`, relative to the repository root.

The filename, app ID `me.wouterdek.magic4pc`, service ID `me.wouterdek.magic4pc.service` and inherited TV version are retained for upgrade compatibility. The **visible name is Magic4Android TV**. It updates/replaces the upstream TV app with that ID; changing its visible name does not allow side-by-side installation.

Keep iLib asset emission enabled in the Webpack configuration. Missing locale assets can allow compilation to pass while the app crashes to a black screen. The package command checks required runtime files.

### Install and launch on the TV

Follow LG's [Developer Mode setup](https://webostv.developer.lge.com/develop/getting-started/developer-mode-app) or the [webOS Homebrew environment guide](https://www.webosbrew.org/develop/guides/env-setup/) for an already-rooted TV. Developer Mode sessions must remain valid. Only the rooted C4 path has been tested here; installation does not establish access to LG's sensor/external-video services elsewhere.

The older CLI bundled with the project is used for reproducible packaging. For TV installation and Developer Mode key retrieval, install LG's [current webOS CLI](https://github.com/webos-tools/cli), which includes `ares-novacom`. Follow its migration guidance if an older webOS CLI is already installed globally. These commands use the global CLI, not `npx`'s project-local copy. In PowerShell, use the `.cmd` variants if script execution is restricted (`npm.cmd`, `ares-config.cmd`, and so on).

```sh
npm install --global @webos-tools/cli
ares-config --profile tv
ares-setup-device
```

Create a target such as `livingroom` with your TV's address and authentication. Developer Mode uses the `prisoner` user, port 9922 and the Key Server/key-passphrase steps in LG's guide. A rooted TV uses its configured root SSH connection instead.

For a Developer Mode target, retrieve its key when LG's instructions direct you to:

```sh
ares-novacom --device livingroom --getkey
```

Install and launch:

```sh
ares-install --device livingroom me.wouterdek.magic4pc_1.0.0_all.ipk
ares-launch --device livingroom me.wouterdek.magic4pc
```

Run the install command from `webos/`, or supply the IPK's full path. See the [webOS CLI guide](https://webostv.developer.lge.com/develop/tools/cli-dev-guide) for device configuration and troubleshooting. Keep root credentials, pairing keys and signing keys outside the repository.

## Connect and use

1. Connect the phone to the TV with a video-capable USB-C adapter/hub and HDMI. Power the hub's PD input if present. Start DeX for a separate external desktop.
2. Open **Magic4Android TV** on the TV and choose the phone's HDMI input using its settings gear.
3. On the phone, open **Magic4Android**, choose **Open Accessibility settings**, and manually enable **Magic4Android remote control**. Android may require allowing restricted settings for a sideloaded app first.
4. Return to the receiver, enter the TV's local IPv4 address, select a display (for example, **HDMI Screen**), and press **Connect to TV**.
5. Check alignment/clicks, then arrows, OK, Back and wheel in your apps. After using the phone screen, click the intended TV app before using Back.
6. **Stop remote control** disconnects and removes the dot. Disable the Accessibility service in Android Settings to revoke its access.

ATV Launcher can run full-screen inside DeX; it does not replace Samsung's external home screen.

## Limitations and troubleshooting

- **Experimental navigation:** Accessibility is not arbitrary keyboard/gamepad injection. Games, custom canvases and some app layouts will not behave like a keyboard. This is not a game controller.
- **Back/focus:** Android Back is global. The receiver refuses it if another display is focused, to avoid navigating the phone. Click the desired TV app first.
- **Touch indicator:** Native hover, dragging, long presses and general media-key injection are not implemented. The drawn circle stays inside screen edges; click coordinates still cover the full output.
- **Black TV screen / missing clicks:** Install this fork's rebuilt IPK with its locale assets, and keep the TV app foreground. Ordinary HDMI mode does not forward buttons.
- **Dot confined to one quarter of 4K:** Use the updated receiver. TV source coordinates stay 1080p even with 4K output; no manual resolution entry is needed.
- **Quiet HDMI audio:** Check phone HDMI volume and the TV's input-specific sound mode. Matching the HDMI mode to the internal apps resolved the tested setup's difference. Surround downmixing can also affect loudness. The remote bridge does not process phone audio.
- **No HDMI detection:** Check video-output support, hub power, connector seating, cable and TV input. USB data/charging does not establish a working video link.
- **Reconnection:** Unique matching displays can be reattached, but unplug/replug, phone/TV restart recovery and long-term background behavior need broader testing. Samsung battery management can stop background services.
- **Network:** UDP 42831 carries subscriptions, keepalives and input; upstream discovery uses 42830. There is no cryptographic authentication/encryption. Use a trusted LAN and do not expose these ports to the internet. Restricting the socket to a chosen TV IP is not authentication.
- **Permissions:** Accessibility permits UI inspection and gestures. TV root is separate from Android permissions. An unrooted TV may not provide the rooted test TV's service access.

## Layout and legacy Windows client

| Path | Purpose |
| --- | --- |
| `android/` | Native Android Accessibility receiver |
| `webos/` | TV HDMI overlay, remote forwarding and service |
| `pc/` | Retained upstream Windows client; unnecessary for Android operation |
| `ANDROID.md` | Additional receiver/architecture notes |
| `VALIDATION.md` | Device observations and unverified behavior |
| `LICENSE` | Original MIT license and copyright |

The legacy Windows solution is `pc/magic4pc.sln`. Upstream's guidance uses Visual Studio 2019 with WinUI/Windows App SDK support; the project targets .NET 5 and Windows SDK 10.0.19041. That historical toolchain has not been rebuilt or validated for this fork. Refer to [upstream](https://github.com/Wouterdek/magic4pc) for its PC build/install instructions.

## Contributing and AI provenance

Report phone/TV/remote models, OS versions, selected display resolution, affected app, and whether the issue occurs in ordinary HDMI mode. Remove credentials, tokens and personal media information from logs before sharing them.

The Android implementation and fork-specific TV fixes were **AI-developed with OpenAI Codex**, directed by the owner and tested iteratively on real hardware. Upstream code remains the work of its original authors. AI generation does not establish correctness: contributions should explain changes and provide relevant build, test and device evidence.

Licensed under [MIT](LICENSE). Original copyright: **© 2021 Wouter De Keersmaecker**. Preserve that notice in copies and derivatives.
