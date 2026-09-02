# Android APK automation

`.github/workflows/android-apk.yml` validates and builds the Android app on
every push and pull request. It deliberately separates three trust levels:

1. **Debug** — every push/PR; uses the runner's disposable debug key and is not
   guaranteed to update a debug APK from another run.
2. **Stable main** — only the repository default branch; uses the production
   app ID and signing certificate, so every later main or release APK updates it.
3. **Release** — only protected `vMAJOR.MINOR.PATCH` tags; uses the same certificate
   and publishes the verified APK plus SHA-256 file to a GitHub Release.

Disposable debug APKs are not update-compatible with stable main/release APKs.
The application is unreleased, so the first stable main APK establishes its
permanent update identity without a data migration.

## GitHub environments and secrets

Create `internal` (default branch only) and `release` (protected `v*` tags,
required reviewers) environments. Add the same signing identity to both:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_SHA256`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Base64-encode the binary keystore
without line wrapping, store its lowercase SHA-256 separately, retain an
offline backup, and never commit either key. The workflow reconstructs keys only
under the ephemeral runner's temporary directory and verifies the digest before
Gradle sees them.

The internal environment may run without human approval to remain automatic,
but its branch restriction is required. Keep reviewer approval on the release
environment so a pushed tag cannot silently expose or misuse the release key.

## Repository settings

- Enable GitHub Actions and allow the release job's scoped `contents: write`
  permission.
- Protect the default branch and require `Verify and build debug APK`.
- Protect `v*` tags so only release maintainers can create them.
- Keep Dependabot enabled for Gradle and GitHub Actions updates.
- Do not add private packs to Actions caches or artifacts. CI fails if a tracked
  private-content directory or `.dndpack` is found.

Before the first workflow commit, review the complete staged project and run
`git diff --cached --check`. The local tree is connected to the existing
`origin/main` history; no force push or history replacement is required.

## Releasing

Create a tag such as `v0.2.0`; the tag supplies the release version name. Both
default-branch and tagged stable APKs use this workflow's monotonic GitHub run number as
their Android version code. Keeping both deliveries in the same workflow prevents a main
build after a release from being rejected as an older version.

The workflow verifies the signing certificate, checks that no APK requests
`android.permission.INTERNET`, rejects private-pack paths inside the APK, generates a
SHA-256 checksum, and publishes both files. Branch/PR debug artifacts and default-branch
internal artifacts are available from the corresponding Actions run.
