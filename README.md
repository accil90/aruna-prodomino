# ARUNA Domino Pro

ARUNA Domino Pro is an Android learning and analysis project for standard four-player Gaple/domino.

## Initial goals

- Model all 28 tiles in a standard double-six domino set.
- Record tiles that are explicitly visible or entered by the user.
- Calculate which tiles remain unknown without assigning hidden tiles to other players.
- Analyze legal connections using confirmed game information.
- Provide an optional, user-controlled screen observer and floating analysis panel.

## Privacy and safety

Screen observation must use Android MediaProjection and its system consent prompt. The user can start and stop observation at any time. The app does not inspect private data from another app, bypass protections, or access game servers.

## Current status

**Foundation stage:** project documentation and pure Kotlin core logic. This repository is not yet a ready-to-install APK. Android UI, OCR, screen-capture permission flow, and optional overlay will be implemented and tested in later stages.

## Project layout

- `docs/ARCHITECTURE.md` — component design and data flow.
- `docs/GAME_RULES.md` — standard double-six assumptions.
- `core/src/main/kotlin/` — Kotlin domain logic.

## Analysis principle

Only tiles that are visible or explicitly entered are treated as known. All other tiles remain unknown; the app does not claim to know opponents' concealed hands.
