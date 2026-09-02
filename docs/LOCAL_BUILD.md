# Local Android build

## Requirements

- JDK 17
- Android Studio with Android SDK Platform 36 and matching build tools
- Git
- an Android 6/API 23 or newer device or emulator

The first build may need network access to download Gradle and Maven
dependencies. The installed application itself is designed to run fully offline
and should request no internet permission.

## Debug APK

From the repository root on Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat :androidApp:assembleDebug
```

On macOS or Linux, use `./gradlew` instead of `.\gradlew.bat`. The APK is
normally written below `androidApp/build/outputs/apk/debug/`. Install it with
Android Studio, or with:

```powershell
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

The debug APK is signed with the standard local Android debug key and is suitable
for development sideloading only.

Launcher, adaptive, round, themed, and loading artwork is derived from the checked-in
`logo.png`. After replacing that source file, run `node scripts/generate-logo-assets.mjs`;
use `node scripts/generate-logo-assets.mjs --check` to verify the checked-in resources
without rewriting them.

`assembleInternal` is reserved for the stable GitHub default-branch artifact.
It requires the `ANDROID_*` signing environment described in the repository
workflow. It uses the production application ID and certificate, so later stable
main and tagged release APKs update it in place.

## Verification before sharing a build

Run the project checks and inspect the merged release manifest:

```powershell
.\gradlew.bat test
.\gradlew.bat :androidApp:lint
.\gradlew.bat :androidApp:assembleRelease
```

Confirm that:

- no public release embeds a manifest with `distributionReady: false`;
- English and German entry ID sets match;
- all included packs have an offline notice screen;
- no `android.permission.INTERNET` or broad media permission is present;
- the APK installs and creates/opens a character in airplane mode; and
- the checksum and content-pack versions are recorded with the APK.

Release signing requires a private keystore configuration supplied outside the
repository. Never commit keystores, passwords, or generated private packs. If
release signing is not configured, distribute the installable debug APK only to
development testers and label it accordingly.

CI reads version codes from `DND_VERSION_CODE`. Tagged releases additionally
require `DND_RELEASE_TAG` and the `ANDROID_*` release-signing environment; the
tag itself supplies `versionName`.

## Private flavor

Do not create a `privateContent` directory merely to make the normal build pass.
Private content is opt-in. After following `docs/PRIVATE_CONTENT.md`, use only the
dedicated private build task documented by the Gradle module once implemented.
The absence of that task means private-pack compilation is not yet enabled.
