# Hngoos Simple Music desktop design

## Intent and decisions supplied by the user

Build Hngoos Simple Music (HSM), derived from SimpMusic, for Windows and macOS.
Keep separate sibling directories `dev-hngoos-simple-music` (development) and
`repo-hngoos-simple-music` (published source), following HDM. Create the GitHub
repository under the authenticated `hngoos` account. Reuse HDM's supplied H and
overlapping monochrome squares, replacing the rear download symbol with music.

Playback uses the same YouTube/YouTube Music sources as SimpMusic. Support local
audio and offline downloads, with playlist import from YouTube, Spotify and Apple
Music. Imported Spotify/Apple entries are matched to playable YouTube versions;
these services are metadata sources, not additional audio catalogs. Preserve
unmatched entries and report them instead of silently dropping them.

The user explicitly selected an Apple Music visual direction and clarified that
the large image on the left is the currently playing song/album artwork. Lyrics
occupy the right side. HSM retains its own logo and identity.

## Implementation choices

Reuse Kotlin/Compose Multiplatform, Room, libmpv and the upstream player/lyrics
state. Avoid rewriting the extraction engine or introducing a second player.
Retain upstream namespaces to avoid a disruptive migration of serialization and
generated-resource references; use independent HSM distribution identity, data
directories, user agent, update repository and custom protocol.

Vendor the pinned upstream core into the same repository, preserving its license
and recording both original commit IDs. This permits a normal clone without a
second project. Disable inherited Android/release/triage automations and replace
them with Windows/macOS checks and explicit release workflows.

Default to the Apple-style player and Apple-style synchronized lyric renderer.
At desktop widths >= 900dp use album art and transport on the left, synchronized
lyrics on the right. Narrow windows use a stacked/scrollable layout with controls
remaining reachable. Use existing seeking, shuffle, repeat, favorite, queue and
volume actions. Retain plain/unsynchronized lyric and missing lyric states.
Album art failures fall back to the HSM mark; no fake songs or mock lyrics.

Visual system: light surfaces #FAFAFA/#F2F2F7, dark surfaces #171719/#232326,
accent #FA2D48, primary dark text #1D1D1F, primary light text #F5F5F7. Use the
existing UI font with clear 13/15/20/28/34 sizes; large left-aligned lyric lines.
Artwork-derived backgrounds are restricted to the immersive player; library and
settings stay calm. Artwork corners 12dp, controls >= 40dp, soft artwork shadow.
Avoid continuous decorative animation; keep idle-player redraws bounded.

```
┌ HSM · Back ─────────────────────────── Volume · Queue ┐
│                                                      │
│     ┌─────────────────┐     Previous lyric            │
│     │                 │                              │
│     │   Album cover   │     Current lyric            │
│     │                 │     large, synchronized      │
│     └─────────────────┘                              │
│     Song · Artist           Next lyric               │
│     ────────●────────                                │
│       Shuffle  ◀  ▶  ▶  Repeat                        │
└──────────────────────────────────────────────────────┘
```

## Import and library

Reuse upstream native YouTube playlist/library integration. Add a first-party
desktop import flow for exported CSV/TSV playlist metadata from Spotify and Apple
Music, alongside upstream converted JSON import. Parse quoted titles, Unicode,
BOM, CRLF, embedded commas/newlines and Apple TSV headers. Resolve candidates
using title, artist and duration; preserve source order and duplicates. Show
progress/cancellation, totals and unresolved tracks; allow retry/search for
unresolved entries. A private playlist must be exported or accessed with the
user's authorization; do not imply all private URLs are anonymously readable.

Local audio should use the existing media pipeline where supported. Verify the
upstream desktop path before claiming support; record any absent capability as
remaining implementation work, not a working feature.

## Packaging and limits

Windows x64 and macOS 15+ Intel/Apple Silicon are the initial distribution targets,
matching available upstream native playback dependencies. Linux/mobile modules
may remain in source but are not initial release targets. Version HSM separately
starting at 0.1.0. Keep GPL-3.0 and explicit upstream attribution in README,
notices and About. No inherited upstream signing keys, credentials or sponsorship
identity. Describe unsigned/ad-hoc macOS packaging honestly.

## Verification and completion

Verify changes with focused Kotlin tests, desktop compilation and actual launch
where the host permits it; use image inspection for the icon and screenshot
inspection for the player. Check both cloned directories, tracked core sources,
new GitHub origin, release asset naming and Windows/macOS workflow structure.
Test import parsing/matching with malformed input, missing matches and duplicate
tracks. Never report an installer, import capability or operating system as
verified solely because files or workflow YAML exist. Record build blockers and
the exact last successful verification. A release requires native packages to
build successfully before it is presented as available.

## Approaches

Selected: evolve the upstream Compose app, preserving its mature playback and
lyrics paths. A complete UI/player rewrite would discard useful functionality
and increase platform risk. A browser mockup would not satisfy the requested
native player, offline library or installer requirements. Decisions not fixed
above are implementation choices delegated to the builder by the user's request
to make the best complete app possible.
