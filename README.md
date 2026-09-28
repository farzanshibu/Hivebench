<div align="center">

  # Hivebench

  ### A pocket workbench for AI coding agents on Android.

  **Run Claude Code, Codex, OpenCode and other coding agents in their native terminal UI, on a real Ubuntu userland, right on your phone.**

  [![Build release APK](https://github.com/farzanshibu/Hivebench/actions/workflows/release-apk.yml/badge.svg)](https://github.com/farzanshibu/Hivebench/actions/workflows/release-apk.yml)
  [![Android 9+](https://img.shields.io/badge/Android-9%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#requirements)
  [![ARM64](https://img.shields.io/badge/CPU-ARM64-4B8BFF?style=flat-square)](#requirements)
  [![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-A98BFF?style=flat-square&logo=kotlin&logoColor=white)](#build-from-source)
  [![MIT License](https://img.shields.io/badge/License-MIT-FFD000?style=flat-square)](LICENSE)

  [**Download the latest APK**](https://github.com/farzanshibu/Hivebench/releases/latest)

</div>

---

## Features

- **Native agent TUIs**: every agent runs in its own terminal UI over a real PTY, so slash commands, modes and sign-in flows work exactly as they do on a desktop.
- **Agents hub**: detects which agents are installed, installs or updates them in one tap, and handles sign-in by OAuth or API key. You can also check usage from here.
- **Parallel sessions**: run several agent sessions side by side in one project, switch between them, and resume them later.
- **Project workspace**: Session, Office, Files, Git, Terminal and Browser tabs for each project.
- **Browser inspector**: preview local dev servers, inspect elements, and capture console and network traffic. You can annotate or draw on the page and push that context straight to an agent.
- **Linux runtime**: a PRoot-based Ubuntu userland with Node.js, npm, Git and optional toolchains (Python, Docker and more).
- **GitHub**: device-code sign-in, clone, and issue import.

## Requirements

- Android 9.0 (API 28) or newer
- ARM64 (`arm64-v8a`) device
- About 2 GB of free storage for the runtime and agents

## Install

Download `Hivebench-*.apk` from [Releases](https://github.com/farzanshibu/Hivebench/releases/latest) and open it on your phone. Android will ask you to allow installs from that source.

## Build from source

Prerequisites: JDK 17, Android SDK platform 37, NDK `26.1.10909125`, CMake `3.22.1`.

```bash
git clone https://github.com/farzanshibu/Hivebench.git
cd Hivebench

# Debug build, installed and launched on a connected device
sh scripts/install-phone.sh

# Signed release APK
./gradlew :app:assembleOnlineRelease
```

### Release signing

Release builds read the signing key from environment variables:

| Variable | Meaning |
| --- | --- |
| `HB_UPLOAD_STORE_FILE` | Path to the `.jks` keystore |
| `HB_UPLOAD_STORE_PASSWORD` | Keystore password |
| `HB_UPLOAD_KEY_ALIAS` | Key alias |
| `HB_UPLOAD_KEY_PASSWORD` | Key password |

If the variables aren't set, the build falls back to a git-ignored `keystore.properties` in the project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Never commit the keystore or its passwords.

### CI

[`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml) builds a signed APK on every push to `main`; you can download it from the workflow run's artifacts. Pushing a `v*` tag (e.g. `v1.2.3`, which becomes version code `10203`) also publishes a GitHub Release with the APK and `hivebench-update.json` attached. Installed apps read that manifest from the latest release to offer in-app updates. The workflow needs these repository secrets:

- `HB_KEYSTORE_BASE64`: the keystore, base64-encoded
- `HB_UPLOAD_STORE_PASSWORD`, `HB_UPLOAD_KEY_ALIAS`, `HB_UPLOAD_KEY_PASSWORD`

```bash
git tag v1.0.0 && git push origin v1.0.0
```

## Tech stack

Kotlin and Jetpack Compose with a custom neo-brutalist design system; Termux `terminal-view` with our own 16 KB-aligned PTY library; PRoot; native C for the process spawner and loader.

## Credits

Hivebench started from **[Mobile Harness](https://github.com/techjarves/Mobile-Harness)** by [Tech Jarves](https://github.com/techjarves). That project provided the starting point: the PRoot Ubuntu runtime, runtime bundles and the original Android workspace. Hivebench has since been rebranded and heavily reworked, including the native-TUI agent harness, browser inspector, parallel sessions and a new UI. The prebuilt runtime bundles are still downloaded from the original project's releases. Thank you to the Mobile Harness contributors.

Hivebench also builds on [Termux terminal-view / terminal-emulator](https://github.com/termux/termux-app) and [PRoot](https://github.com/proot-me/proot); see `third_party/` for their licenses.

## License

[MIT](LICENSE). The original Mobile Harness copyright notice is kept, as the license requires.
