# Architecture

## Components

1. **Domino Core** — the 28-tile model, validation, visible-tile tracking, and unknown-tile calculation.
2. **Screen Observer** — Android MediaProjection capture with explicit system consent.
3. **Tile Recognition** — visual recognition that proposes tile readings; uncertain readings require user confirmation.
4. **Game State** — the confirmed table chain and its open ends.
5. **Analysis View** — transparent results with clear indication of the data used.
6. **Floating Panel** — optional compact overlay, enabled only with the user's Android overlay permission.

## Data flow

User starts a session → Android requests screen-capture consent → visible frames are processed → recognition proposes tiles → user confirms or corrects → core logic updates visible tiles → analysis view displays verifiable results.

## Privacy requirements

- No silent screen observation.
- Start and stop controls remain accessible.
- Never infer concealed tiles or assign unknown tiles to a particular player without legitimate evidence.
- No access to another app's private storage, internal process, or game server.
- Session data is stored locally and can be cleared by the user.
- Low-confidence OCR results must not silently alter game state.

## Implementation stages

- **Stage 1:** documentation, tile model, and core logic tests.
- **Stage 2:** Android Kotlin project, manual tile-entry UI, and local session storage.
- **Stage 3:** MediaProjection permission flow and recognition of visible tiles.
- **Stage 4:** optional floating analysis panel, device testing, and optimization.

## Initial-version limitation

Automatic recognition will not be assumed perfect. OCR results are proposals that users can correct.
