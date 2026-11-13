# HSM Desktop Implementation Plan

> **For agentic workers:** Execute inline with executing-plans; use a fresh reviewer at completion.

**Goal:** Deliver the branded HSM desktop app with an Apple Music inspired
artwork/lyrics layout and the upstream playback foundation.

**Architecture:** Preserve upstream playback, repository and player state.
Vendor core, isolate the HSM identity, compose a desktop layout from existing
controls/lyrics, and extend import at the metadata boundary.

**Tech stack:** Kotlin, Compose Multiplatform, Gradle, Room, libmpv, Python/Pillow
for reproducible icon assets, GitHub Actions for platform verification.

**Spec:** `docs/superpowers/specs/2026-11-13-hsm-desktop-design.md`

## Global constraints

- Brand: Hngoos Simple Music (HSM), release 0.1.0.
- Windows x64; macOS 15+ Intel and Apple Silicon.
- Audio: YouTube/YouTube Music and local files; Spotify/Apple playlist metadata.
- Keep GPL and upstream attribution. Never silently discard unmatched tracks.
- Work in the user-requested dev directory; publish from the repo directory.

## Review focus

- HSM data/update/protocol settings must not collide with installed SimpMusic.
- Artwork and lyric failures must leave transport controls usable.
- Narrow/short windows and long Unicode titles must retain usable controls.
- Imported quoted fields, Apple TSV, missing matches and duplicates preserve data.
- Release claims require actual successful native build evidence.

## Tasks

### 1. Establish independent HSM source and identity

- [x] Vendor pinned core, retain provenance/license, no nested Git link.
- [x] Add HSM identity defaults and rebrand UI strings without renaming providers.
- [x] Isolate desktop storage, protocol, updates and package metadata.
- [x] Generate HSM SVG, PNG, ICO, ICNS from HDM's original H.
- [x] Review diff, validate assets and commit the identity slice.

### 2. Implement the desktop listening experience

- [x] Reuse existing Apple player/lyric components and actions.
- [x] Introduce a responsive two-column desktop Now Playing layout as default.
- [x] Keep seek, queue, volume, favorite, shuffle, repeat and lyric fallback usable.
- [x] Test layout decisions and verify compilation/launch/screenshots.
- [x] Commit the player slice (included in the reviewed desktop implementation commit).

### 3. Complete playlist and local/offline flows

- [x] Inspect existing desktop local-file and download support.
- [x] Add tested CSV/TSV normalization and conservative YouTube matching.
- [x] Expose import progress, cancellation and unmatched-track recovery.
- [x] Preserve native YouTube and converted JSON imports.
- [x] Verify parser/matcher cases and integration, commit the import slice.

### 4. Publish reviewable source and build distributions

- [x] Replace inherited workflows with Windows/macOS checks and release packaging.
- [x] Write HSM README, notices and truthful build/release instructions.
- [x] Run focused tests and final release verification; fix evidenced failures.
- [x] Review whole change with a fresh reviewer and resolve actionable findings.
- [x] Create GitHub repo, push verified source, clone repo directory and verify parity.
- [x] Build Windows x64 and macOS Intel/Apple Silicon distributions on native runners; retain draft status and report runtime/signing limits.

Execution evidence is recorded in `docs/VERIFICATION.md`. All three native installer builds succeeded. Windows playback and UI were exercised directly; macOS runtime testing and official signing/notarization remain unverified or unconfigured, as recorded in the ledger.
