# HSM verification ledger

Last updated: November 13, 2026. Initial version: 0.1.0.

## Evidence so far

- `:domain:jvmTest :desktopApp:compileKotlinJvm`: BUILD SUCCESSFUL on Windows (3m36s), before final review fixes.
- Six CSV/TSV parsing/recording-matching cases passed.
- Three managed local audio file tests passed: copy independence, content identity, unsupported/missing files.
- Three encoding cases passed: UTF-8 BOM, UTF-16 LE/BE including Vietnamese and surrogate pairs, truncated UTF-16 rejection.
- Logo SVG rendered and PNG/ICO/ICNS generated; PNG visually inspected.

- Full-window player launched on Windows; YouTube audio played, timed lyrics rendered and lyric clicks sought playback ("a thousand years").
- Imported a real 30-second WAV through the native file picker and played the managed copy after moving its source.
- `:domain:jvmTest :desktopApp:packageMsi`: BUILD SUCCESSFUL (5m35s); 42 JVM tests, zero failures/errors. A final rebuild includes the last local-provider guards.

- Native UTF-16 Apple-format TSV imported through the file picker: three ordered entries including a duplicate, one intentionally unavailable track retained and reported; two known entries matched YouTube with no lookup failures.

- Read-only SQLite inspection confirmed imported playlist positions 0, 1, 2 and both duplicate recording IDs remained in order, including the unresolved middle entry.

- Final `:domain:jvmTest :desktopApp:createDistributable :desktopApp:packageMsi`: BUILD SUCCESSFUL (4m37s), 42 JVM tests, zero failures/errors.
- Final native launch and slider dragging checked; the listening overlay owns hit testing without consuming child gestures.

## Remaining platform verification

- Windows MSI and macOS Intel/Apple Silicon packaging through native GitHub runners.
- Fresh source review completed; all important findings resolved, including pointer handling and local-provider URI boundaries.

## Capability boundaries

- Spotify/Apple imports use exported playlist metadata; account OAuth and arbitrary private playlist URLs are not implemented.
- Local audio metadata comes from filenames; embedded tags/artwork and directory scanning are not implemented.
- Native YouTube sign-in/playlist access, network playback, downloads and lyric providers are inherited integrations and require live service verification.
- macOS cannot be launched or visually verified on this Windows host. Packaging CI is separate evidence from runtime testing on a Mac.
- Installers are unsigned/not notarized until distribution certificates are configured.

Do not infer a completed installer or verified service from the presence of source or workflow files. This ledger is updated with final build results before publishing source.
