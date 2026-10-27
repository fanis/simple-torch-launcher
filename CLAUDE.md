# CLAUDE.md

## Build Commands

```bash
# Installable debug build
./gradlew assembleDebug

# Unsigned release build
./gradlew assembleRelease

# Lint
./gradlew lint
```

Needs JDK 17 or newer and the Android SDK with `platforms;android-37.0` and `build-tools;37.0.0`.
There are no tests and no third-party dependencies.

## Workflow Rules

- Never add AI attribution anywhere: no `Co-Authored-By: Claude`, no `Claude-Session:`/session links, and no "Generated with Claude Code" footers in commit messages, PR titles/descriptions, release notes, or code comments
- Commit as the repository owner: `Fanis Hatzidakis <562545+fanis@users.noreply.github.com>`
- Add user-facing changes to the `## [Unreleased]` section of `CHANGELOG.md` as they land. The release workflow publishes that section, and F-Droid shows it as the version's release notes
- Don't change `versionCode` or `versionName` by hand: the Cut Release workflow bumps both
- Only build and lint when code or resources change (`app/`, Gradle files), not for documentation-only changes
- Keep the app small: no dependencies, no internet permission, and no accessibility capabilities beyond key filtering. The setup screen, README and the service description promise this

## Architecture Notes

- `TorchService` is the accessibility service. While the screen is on it filters key events and holds back presses of the chosen volume button for 400 ms to detect a double-press, replaying single presses as volume changes. When the app's switch is off it drops the key filter flag
- `ScreenOffVolumeKeys` is the best-effort screen-off path: a remote-playback media session that exists only while the screen is off, no audio plays and no call is on
- `SetupActivity` walks the user through turning on accessibility, and `GuideActivity` floats a looping mock-up of the steps over Android's settings in picture-in-picture
- Screens use framework widgets only (no AndroidX), with colours from the device theme

## Releasing

- `Cut Release` GitHub Action (`.github/workflows/cut-release.yml`, `workflow_dispatch`) cuts a release: bumps `versionName` and `versionCode` in `app/build.gradle.kts`, moves the `CHANGELOG.md` `[Unreleased]` section under the new version (leaving a fresh empty one above it), writes F-Droid's release notes to `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, builds and lints, commits, tags `X.Y.Z`, and publishes a GitHub Release with the APK attached
- It requires a non-empty `[Unreleased]` section in `CHANGELOG.md`, and only runs on the default branch
- Inputs: `bump` (patch/minor/major), optional explicit `version` (overrides `bump`), and optional `summary` (appended to the commit message)
- Trigger it from Claude Code on the web with the GitHub Actions trigger tool on `cut-release.yml`, or from the GitHub Actions tab
- Publishing is done by `release.yml`, which `Cut Release` calls directly (tags pushed by Actions with GITHUB_TOKEN don't trigger other workflows). `release.yml` also runs on its own for tags pushed by hand
- The APK is signed with the key in the `SIGNING_*` repository secrets (see README); without them an unsigned APK is attached
