# HSM 0.1.0 verification

Last updated: November 13, 2026. Verified application source: `3076a4f585447945e32c4e1952939f426d58cf09`.

Current scope: **Windows x64 only**, following the owner's November 13 decision. macOS development, CI and distribution are paused. Earlier Mac build evidence below is historical; the DMGs have been archived locally and removed from the draft release. Platform source is retained.

## Automated and packaging evidence

- Final Windows `:domain:jvmTest :media-jvm:compileKotlin`: **BUILD SUCCESSFUL** (10 seconds); 43 JVM tests, zero failures/errors. Coverage includes quoted CSV/TSV, Unicode and UTF-16 documents, recording matching, duplicate/unmatched handling, managed audio copies and local download state.
- Final Windows `:desktopApp:createDistributable :desktopApp:packageMsi`: **BUILD SUCCESSFUL** (2 minutes 1 second), including the final guard against sending local download requests to YouTube.
- Windows MSI metadata inspected read-only: product HSM, version 0.1.0, manufacturer hngoos, independent upgrade code `{66E00F47-54F0-4ABE-AF27-6D955C648BCD}`.
- Local Windows installer SHA-256: `1033eff1144272f59442da907a31010395c0b76e5a79819660b7bec4e9c2bfac`. The uploaded draft asset has the same GitHub-reported digest.
- [Native build run 43434019263](https://github.com/hngoos/hngoos-simple-music/actions/runs/43434019263): tests and native installer builds succeeded on Windows x64, macOS Apple Silicon and macOS Intel. Each platform produced its installer and test report artifacts.
- Resource XML files parsed; generated PNG/ICO/ICNS assets validated; logo and actual player screenshot visually inspected. Source diff whitespace checks passed.
- Independent source review completed and actionable findings resolved, including overlay gesture handling and local-provider URI boundaries.

## Actual Windows application checks

- YouTube audio played; synchronized lyrics appeared for “a thousand years”; clicking a lyric sought playback.
- Large current artwork rendered on the left and lyrics on the right. Seek dragging worked, and the full-window listening overlay prevented clicks reaching underlying navigation.
- Controls remained usable at 1000×720; the compact 780×700 layout scrolled to transport and lyrics.
- A real 30-second WAV imported through the native file picker played from HSM's managed copy after its original file moved.
- A native UTF-16 Apple-format TSV imported three ordered entries with a duplicate and an intentionally unavailable track. Both known entries matched YouTube; the unmatched entry remained visible and was reported; there were no network lookup failures.
- Read-only SQLite inspection confirmed positions 0, 1, 2, the duplicate recording IDs and the unresolved middle entry.

## Publication and archived artifacts

- [GitHub source](https://github.com/hngoos/hngoos-simple-music) contains the vendored core and retained upstream history/licenses.
- Separate `dev-hngoos-simple-music` and `repo-hngoos-simple-music` checkouts exist. The publication checkout was verified against the pushed application commit.
- HSM 0.1.0 is a **Windows-only draft prerelease**. It has not been published publicly. The Windows MSI remains attached; previous Mac artifacts are archived locally. Original artifact digests are listed below.

| Built artifact (Mac artifacts archived) | SHA-256 |
| --- | --- |
| `HSM-0.1.0-windows-x64.msi` | `1033eff1144272f59442da907a31010395c0b76e5a79819660b7bec4e9c2bfac` |
| `HSM-0.1.0-macos-arm64.dmg` | `c0b6e5d620ded79e2295bd04ae17478ba3bc4045cfc0d6b8f6fa79f8a3c1da94` |
| `HSM-0.1.0-macos-x64.dmg` | `ce48c3e1238c0dc19fd681b7c901681aa0c59e21099d754bb63aae122791f5ba` |

## Capability boundaries

- Spotify/Apple imports use exported playlist metadata; account OAuth and arbitrary private playlist URLs are not implemented. Imported entries are matched to YouTube recordings; catalog equivalence is not guaranteed.
- Local audio metadata comes from filenames; embedded tags/artwork and directory scanning are not implemented.
- Native YouTube sign-in/library, downloads and lyric providers are inherited integrations. Windows playback and the lyric/import checks above are live evidence; not every upstream service or account flow was independently exercised.
- macOS cannot be launched or visually verified on this Windows host. Native packaging CI is separate evidence from runtime testing on a Mac.
- Official signing/notarization is not configured.
