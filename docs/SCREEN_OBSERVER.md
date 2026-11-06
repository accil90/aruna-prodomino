# ARUNA DOMINO Screen Observer

## Current implementation

The observer uses Android MediaProjection only after the user grants the system screen-capture permission.

- The user enables **ANALISIS** from the app and approves Android's screen-capture prompt.
- Capture runs in a foreground service with a persistent notification.
- Frames are received and immediately closed; they are not saved, uploaded, or interpreted yet.
- The user can disable analysis from the app. Android can revoke capture at any time.
- The service is not exported and is started only by the app.
- A draggable floating recommendation window is implemented. It remains hidden until the analysis pipeline supplies a confirmed recommendation; it is designed to show only the recommended tile and, when relevant, its legal side(s).
- Android's **Display over other apps** permission is requested before starting capture.
- **Akhiri & reset permainan** stops capture and clears the current in-memory and locally persisted session.

## Not implemented yet

The floating window and permission flow are foundations only. Automatic domino recognition, reading the player's hand, public table tiles, chain endpoints, turn detection, and automatic recommendation generation are not implemented yet. The current capture service does not analyze frames.

## Next milestones

1. **Frame processing:** when analysis is active, convert frames to downscaled/cropped in-memory images for local processing and release buffers promptly.
2. **Board calibration:** let the user identify the game area, own-hand area, public table area, and turn indicator. Recalibrate when the game layout changes.
3. **Tile recognition:** recognize double-six tile faces and orientation, returning confidence scores.
4. **Game-state tracking:** compare frames over time; update only confirmed public tiles and visible own-hand tiles.
5. **Recommendation bridge:** pass confirmed state into the existing MoveAnalyzer; ask the user to confirm uncertain readings.
6. **Floating recommendation output:** send only confirmed recommended tiles and applicable side choices to the floating window.

## Privacy and correctness rules

- Use only the Android MediaProjection consent flow; do not access another app's private storage, internal state, or network traffic.
- Process captured frames locally. Do not save or upload screenshots by default.
- Never infer the concealed tiles held by opponents.
- Treat uncertain recognition as unknown and request confirmation.
- Keep a clear stop control and the Android foreground-service notification visible while capture is active.

## Device test checklist

- Android 12 / MIUI 14: grant overlay and screen-capture permissions, enable analysis, switch to a domino game, verify the foreground notification, and disable analysis.
- Revoke screen capture from Android and verify the service stops cleanly.
- Lock the screen or rotate the device and verify that capture does not crash the app.
- Confirm no screenshot files are created.
- Confirm the floating panel can be dragged and does not obscure important game controls.
- Measure battery and memory use before enabling image recognition.
