# ARUNA DOMINO Screen Observer

## Current implementation

The first observer milestone adds an Android MediaProjection capture service.

- The user must press **Mulai pengamat** and approve Android's system screen-capture prompt.
- Capture runs in a foreground service with a persistent notification.
- The service receives display frames through ImageReader and immediately closes each frame.
- Frames are not written to storage, transmitted, or analyzed yet.
- The user can stop capture from the app. Android can also revoke the projection at any time.
- The capture service is not exported and is started only by the app.

## Not implemented yet

This milestone does not recognize domino tiles, read game turns, identify table endpoints, or generate automatic real-time recommendations. Those require a separate image-analysis pipeline and validation against screenshots of the actual game interface.

## Next milestones

1. **Frame processing:** safely convert captured frames into downscaled/cropped in-memory images and release buffers promptly.
2. **Board calibration:** let the user identify the game area, own-hand area, public table area, and turn indicator. Recalibrate when the game layout changes.
3. **Tile recognition:** recognize double-six tile faces and orientation, returning confidence scores.
4. **Game-state tracking:** compare frames over time; update only confirmed public tiles and visible own-hand tiles.
5. **Recommendation bridge:** pass confirmed state into the existing MoveAnalyzer; ask the user to confirm uncertain readings.
6. **Optional recommendation panel:** add a user-controlled display after recognition is reliable.

## Privacy and correctness rules

- Use only the Android MediaProjection consent flow; do not access another app's private storage, internal state, or network traffic.
- Process captured frames locally. Do not save or upload screenshots by default.
- Never infer the concealed tiles held by opponents.
- Treat uncertain recognition as unknown and request confirmation.
- Keep a clear stop control and the Android foreground-service notification visible while capture is active.

## Device test checklist

- Android 12 / MIUI 14: grant permission, start capture, switch to a domino game, verify the foreground notification, and stop capture.
- Revoke screen capture from Android and verify the service stops cleanly.
- Lock the screen or rotate the device and verify that capture does not crash the app.
- Confirm no screenshot files are created.
- Measure battery and memory use before enabling image recognition.
