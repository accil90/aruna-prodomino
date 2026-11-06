# ARUNA Domino Pro

ARUNA Domino Pro is an Android learning and analysis project for standard four-player Gaple/domino.

## Initial goals

- Model all 28 tiles in a standard double-six domino set.
- Let the user manually record tiles that are explicitly visible.
- Calculate which tiles remain unknown without assigning hidden tiles to other players.
- Analyze legal connections using a confirmed table chain and a hand entered by the user.
- Add user-controlled screen observation, on-device tile recognition, and an optional recommendation panel.

## Current implementation

- Android application module with Kotlin and Java 17.
- First manual tile-tracking screen with all 28 double-six tiles.
- Core model for normalized domino tiles.
- Public table-chain model with left/right endpoint handling.
- Legal-move analyzer for a user-entered hand.
- Four-player round state engine with turn order, play/pass events, duplicate-play prevention, and blocked-round detection.
- Round statistics limited to verifiable facts: recorded plays/passes, own remaining tiles/pips, and known/unknown tile counts.
- Dependency-free tile-set codec and local preference persistence for the hand and visible-tile selections.
- Unit tests for tile rules, chain orientation, move legality, session events, statistics, and tile-state encoding.
- GitHub Actions workflow configured to build a debug APK and upload it as an artifact.
- First screen-observer milestone: an Android MediaProjection foreground service with explicit system consent, start/stop controls, and a persistent notification. Captured frames are immediately discarded; no screenshot is saved, uploaded, or analyzed in this milestone.

## Privacy and safety

Screen observation must use Android MediaProjection and its system consent prompt. The user can start and stop observation at any time. The app does not inspect private data from another app, bypass protections, or access game servers.

Only tiles that are visible or explicitly entered are treated as known. All other tiles remain unknown; the app does not claim to know opponents' concealed hands.

## Build

The project uses Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, and JDK 17.

On a machine with Android SDK and Gradle installed:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions is configured to run the build on pushes to `main`. The downloadable APK, when a run succeeds, is available under the workflow's Artifacts section.

## Next stages

1. Complete and verify the first Android build.
2. Add manual hand entry, table-chain entry, and visible-tile history.
3. Complete device testing of MediaProjection capture on the POCO X3 NFC / Android 12.
4. Add in-memory frame processing, user-calibrated game regions, and on-device tile recognition with confidence checks.
5. Connect confirmed observations to the move analyzer, then consider an optional recommendation panel.

This repository is under active development. A successful source commit is not the same as a tested release APK; builds and device testing must be verified separately.
