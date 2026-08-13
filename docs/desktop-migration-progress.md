# Desktop migration progress

This log tracks the incremental Android-to-Compose-Desktop migration. The
Android `:app` module is reference-only and is not modified by this work.

## Phase 0 — audit

Status: complete on 2026-08-13.

### Existing Compose Multiplatform baseline

- `:composeApp` is a Kotlin Multiplatform library with `commonMain`,
  `androidMain`, and `desktopMain` source sets. It targets Android (min SDK 25)
  and JVM 17 desktop.
- The desktop entry point is `desktop/Main.kt`; it opens a 1280×800 Compose
  window and renders `DesktopSpatialFlowApp`.
- The desktop implementation currently works independently of shared UI. It
  scans a selected directory recursively, reads tags using jaudiotagger, keeps
  library/queue/favourites/history in preferences, and plays compatible files
  through Java Sound (falling back to the operating system player).
- `commonMain` currently contains a small static theme, a settings-only
  adaptive shell, a `SettingsViewModel`, bundled Google Sans Flex font, and a
  Ktor client factory. It is a scaffold, not a port of the Android UI.
- Gradle configuration uses Compose Multiplatform 1.11.0-alpha02, Ktor 3.2.3,
  JetBrains Lifecycle/Navigation, CIO on desktop, and OkHttp on Android. No
  common database, DI, playback, local-library, artwork loader, or platform
  abstraction is wired yet.

### Distribution audit

- `packaging/flatpak/com.codetrio.SpatialFlow.yml` targets the Freedesktop 25.08
  runtime and expects a JVM Compose Desktop distributable named `spatialflow`.
- It grants network access, PulseAudio access, and read-only XDG Music access.
  The manifest is intentionally a packaging template; its README confirms the
  distributable must first be produced with `:composeApp:createDistributable`.
- Native JVM desktop is therefore the intended target; this is not an
  Electron/web migration.

### Android UI and presentation inventory

`PORTABLE` means the code is Compose/state-only once its shared models are
available. `NEEDS-ABSTRACTION` means the UI or logic is portable but currently
depends on Android APIs/services, Room, Coil Android, or an Android wrapper.
`ANDROID-ONLY` means no desktop equivalent is planned.

| Reference | Classification | Desktop migration note |
| --- | --- | --- |
| `ui/theme/Color.kt` | PORTABLE | Move verbatim to commonMain. |
| `ui/theme/Type.kt` | PORTABLE | Move verbatim; the current shared theme is only a partial copy. |
| `ui/theme/Theme.kt` | NEEDS-ABSTRACTION | SharedPreferences, Android dynamic colour, and system APIs need platform providers. |
| `ui/onboarding/OnboardingScreen.kt` | NEEDS-ABSTRACTION | Nine-step Compose flow, but Android permissions, preferences, haptics, drawables, Coil, and account APIs need platform/shared replacements. |
| `ui/library/HistoryScreen.kt` | PORTABLE | Compose-only screen after history repository/model migration. |
| `ui/library/LibraryScreen.kt` | NEEDS-ABSTRACTION | Android activity/context actions and Room/local-library data require desktop services. |
| `ui/explore/ExploreComponents.kt` | NEEDS-ABSTRACTION | Compose UI, but Coil, favourites, downloads, and shared streaming models must move first. |
| `ui/explore/ExploreDetails.kt` | NEEDS-ABSTRACTION | Compose UI using streaming models and Android image/context APIs. |
| `ui/explore/ExploreScreen.kt` | NEEDS-ABSTRACTION | Has Android configuration imports and requires shared ExploreViewModel/streaming. |
| `ui/explore/AccountScreen.kt` | NEEDS-ABSTRACTION | Compose UI with Android context/image loading and account service. |
| `ui/explore/GoogleSignInScreen.kt` | NEEDS-ABSTRACTION | Android WebView/CookieManager; desktop OAuth flow requires an explicit product decision. |
| `ui/player/PlayerUiState.kt` | PORTABLE | Pure state once `SongItem` moves. |
| `ui/player/PlayerUiComponents.kt` | PORTABLE | Compose helpers dependent only on shared music/lyrics models. |
| `ui/player/SyncedLyricsCompose.kt` | PORTABLE | Compose karaoke rendering after lyrics model migration. |
| `ui/player/FullScreenLyricsOverlay.kt` | PORTABLE | Compose-only overlay after lyrics model migration. |
| `ui/player/SlidingQueueDrawer.kt` | PORTABLE | Compose-only queue presentation after `SongItem` migration. |
| `ui/player/SleepTimerBottomSheet.kt` | PORTABLE | Compose-only controls; timer execution belongs to playback abstraction. |
| `ui/player/WavyMusicSlider.kt` | NEEDS-ABSTRACTION | Uses an Android-only wavy-slider AAR; reproduce its output with Compose Canvas. |
| `ui/player/ArtworkPager.kt` | NEEDS-ABSTRACTION | Android image loading/context calls need a multiplatform artwork loader. |
| `ui/player/FullPlayer.kt` | NEEDS-ABSTRACTION | Compose layout is reusable; Android configuration/image dependencies and playback contract must be split. |
| `ui/PlayerBottomSheetCompose.kt` | NEEDS-ABSTRACTION | Compose interaction code with Android context/configuration/haptics; use window metrics and a no-op haptic actual. |
| `ui/QueueBottomSheet.kt` | NEEDS-ABSTRACTION | BottomSheetDialogFragment wrapper must become a Compose host; content/drag state is reusable. |
| `ui/SongActionsBottomSheet.kt` | NEEDS-ABSTRACTION | Fragment wrapper, playlist persistence, downloads, and Android actions require interfaces. |
| `ui/EffectsScreen.kt` | NEEDS-ABSTRACTION | UI is mostly Compose; Media3 effects must map to the desktop playback DSP/equalizer interface. |
| `ui/SettingsFragment.kt` | NEEDS-ABSTRACTION | Despite its name it contains Compose UI, but uses Android preferences, WebView, vibration, URI/activity APIs, and updater wiring. |
| `ui/TagEditorFragment.kt` | NEEDS-ABSTRACTION | Android document/activity APIs and FFmpeg integration need JVM file picker/process actuals. |
| `ui/SnackbarController.kt` | PORTABLE | Pure Compose/coroutine event controller. |
| `ui/custom/AnimatedMeshGradientView.java` | NEEDS-ABSTRACTION | Reimplement visual output as a Compose Canvas animation. |
| `ui/ExternalPlayerActivity.kt` | ANDROID-ONLY | Activity-specific external player surface; desktop full-player window can be considered later, but is not a direct port. |
| `ui/widget/SpatialFlowWidgetProvider.java` | ANDROID-ONLY | Android home-screen widgets are explicitly out of desktop scope. |
| `ui/widget/WidgetSmallProvider.java` | ANDROID-ONLY | Android widget provider. |
| `ui/widget/WidgetMediumProvider.java` | ANDROID-ONLY | Android widget provider. |
| `ui/widget/WidgetLargeProvider.java` | ANDROID-ONLY | Android widget provider. |
| `viewmodel/PlayerSharedViewModel.kt` | NEEDS-ABSTRACTION | AndroidViewModel, Media3 service, Room, preferences, haptics, downloads, artwork palette, and Context must be replaced by shared contracts. |
| `viewmodel/lyrics/PlayerLyricsStateController.kt` | NEEDS-ABSTRACTION | State logic is portable, but constructor logging/context and lyrics repository dependencies must be made common. |
| `viewmodel/ExploreViewModel.kt` | NEEDS-ABSTRACTION | Networking/state is transferable after InnerTube migration; AndroidViewModel, preferences, Coil, and context must be removed. |
| `viewmodel/AccountViewModel.kt` | NEEDS-ABSTRACTION | Account state is transferable after account/OAuth contract is defined; currently an AndroidViewModel. |

### Feature and infrastructure inventory

| Area | Classification | Migration dependency |
| --- | --- | --- |
| `model/SongItem.kt` | NEEDS-ABSTRACTION | Likely transferable model, but URI/Android parcelable usage must be isolated. |
| Lyrics parsing/models/scoring | PORTABLE | Move parser, normalizer, semantic utilities, and data classes to commonMain. |
| Lyrics repository/providers | NEEDS-ABSTRACTION | HTTP work can move after Retrofit/Android storage/logging are replaced with Ktor/common interfaces. |
| InnerTube models/parser/client/YouTubeMusic | NEEDS-ABSTRACTION | Transfer HTTP/domain code after Android/NewPipe and persistence edges are separated. |
| Audio playback service | NEEDS-ABSTRACTION | Define common playback contract before selecting the required desktop engine. |
| Local scanning and metadata | NEEDS-ABSTRACTION | Existing desktop walker/jaudiotagger is a starting point; expose it through a common library interface. |
| Loudness analysis/normalization | NEEDS-ABSTRACTION | Port pure DSP; desktop audio-buffer source belongs to player actual. |
| FFmpeg tag/transcode/download tasks | NEEDS-ABSTRACTION | Share command construction; desktop actual invokes a validated system/bundled binary. |
| Room playlists/history | NEEDS-ABSTRACTION | Replace with a common database contract and selected desktop-capable persistence implementation. |
| Koin modules | NEEDS-ABSTRACTION | `:composeApp` has no Koin dependency/module yet; split only after contracts exist. |
| Haptics | ANDROID-ONLY | Desktop actual is a no-op. |
| Media session/notification | NEEDS-ABSTRACTION | Stub after core playback; desktop tray/media integration is lower priority. |
| Update manager | NEEDS-ABSTRACTION | GitHub release checking is portable; installation flow is desktop-specific. |

### Phase gate and next increment

- [x] Audit `composeApp`, Gradle targets, and Flatpak packaging.
- [x] Record all Android UI/viewmodel references and classifications.
- [x] Build/compile the desktop baseline. `:composeApp:compileKotlinDesktop`
  passes after updating the deprecated Compose Gradle dependency declarations
  for Kotlin 2.3.21 and correcting existing portable-source imports/API calls.
- [x] Phase 1 (theme increment): move the exact colour and typography tokens
  into `commonMain`; use an Android/desktop `expect`/`actual` boundary for
  platform dynamic colours.
- [x] Phase 1 (lyrics increment): add common lyric timing/result/state and
  track metadata models, a standard/enhanced LRC parser, and metadata repair.
  The Android Media3/Gramophone parser remains in `:app` pending a complete
  cross-platform parser parity suite.
- [x] Phase 1 (domain increment): add shared song metadata and portable lyric
  provider query/confidence logic. Android URI/MediaStore and database mapping
  remain platform adapters rather than properties of the shared song model.
- [x] Phase 1 (streaming-model increment): move InnerTube browse/search/stream
  models into commonMain and introduce the `MusicCatalog` UI-facing contract.
  The current Retrofit/OkHttp/Gson/NewPipe client is Android-bound and remains
  reference code until its Ktor implementation is introduced.
- [x] Phase 1 (player-state increment): move player presentation state, queue
  commands, repeat and sleep-timer modes into commonMain. A platform playback
  controller will implement this contract once the desktop engine is selected.
- [x] Phase 2 (onboarding-state increment): correct onboarding's classification
  and add its shared nine-step state, preference, permission, and platform
  contract. Its visual composables remain pending cross-platform asset/account
  and permission adapters.
- [x] Phase 1 (lyrics networking increment): add a Ktor LRCLIB client in
  commonMain, replacing the equivalent Retrofit API surface for direct lookup
  and search. Remaining lyrics providers and the routing/cache engine are
  still Android-only reference code.
- [x] Phase 1 (HTTP bootstrap increment): supply common repositories with a
  platform-selected Ktor client (OkHttp on Android and CIO on desktop).
- [x] Phase 1 (SyncLRC increment): add the common Ktor SyncLRC lookup client
  and map its synced, karaoke, and plain lyric response forms.
- [x] Phase 1 (audio/FFmpeg increment): move volume gain, PCM loudness math,
  and 8D FFmpeg argument construction to commonMain. Audio decoding and command
  execution remain platform responsibilities.
- [x] Phase 1 (lyrics catalog increment): compose shared SyncLRC and LRCLIB
  clients into a scored fallback catalog. Paxsenix, embedded tags, caching, and
  telemetry remain to be migrated.
- [x] Phase 1 (settings-state increment): introduce a common settings store
  contract and route the scaffold ViewModel through it. Android SharedPreferences
  and JVM Preferences adapters now implement the contract.
- [x] Phase 1 (DI increment): add Koin core to the KMP module and split initial
  `commonModule`/`desktopModule` bindings for settings and HTTP. Android module
  wiring will be added when the shared Android entry point is migrated.
- [x] Phase 1 (Paxsenix transport increment): replace the Retrofit endpoint
  interface with a Ktor transport covering Spotify, YouTube, Musixmatch, and
  Apple Music request surfaces. Provider-specific response parsing remains
  Android reference code pending its shared migration.
- [x] Phase 1 (Paxsenix parser increment): add shared kotlinx-serialization
  parsing plus Spotify/YouTube fallback extraction, including plain, LRC, line
  array, and enhanced-LRC/word-by-word response forms.
- [x] Phase 1 (lyrics decision/cache increment): move the result replacement
  policy and cache contract to commonMain; an in-memory implementation is
  available pending a persistent cross-platform cache adapter.
- [x] Phase 1 (InnerTube transport increment): add the Ktor request client and
  shared WEB_REMIX contexts for search, browse, player, and suggestions. The
  API key/session values are injected configuration, not embedded in code.
- [x] Phase 1 (InnerTube parser increment): add common JSON parsing for stable
  search and player response branches plus artwork URL normalization. Broader
  browse, account, library, and cipher response variants remain to migrate.
- [x] Phase 1 (InnerTube catalog increment): add the shared Ktor-backed
  `MusicCatalog` and parsers for suggestions, home/explore shelves, album,
  artist, and playlist response branches. Authenticated account/library and
  ciphered stream handling remain desktop platform work.
- [x] Phase 1 (library contract increment): add the common DAO-shaped playlist
  and history repository contract. Selecting its persistent implementation is
  intentionally deferred; the brief requires a decision between SQLDelight and
  a desktop Room JDBC driver before adding either dependency.
- [x] Phase 1 (SQLDelight persistence increment): select SQLDelight, add the
  shared playlist/history schema and desktop SQLite driver, and bind a
  `DesktopLibraryRepository` into Koin. The repository persists queue-ready
  song metadata, playlist ordering, and the latest 200 history entries.
- [ ] Phase 1 remaining: implement the selected SQLDelight persistent
  playlist/history backend, then migrate the remaining InnerTube
  browse/account/library parser branches and provider-specific lyrics parsers.
- [x] Phase 1 (artwork palette increment): add shared HSL artwork palette
  derivation and expose it as an optional `SpatialFlowTheme` seed.
- [ ] Phase 1 remaining: connect the shared foundation into the existing
  desktop shell once persistence and streaming implementations are complete.
- [x] Phase 1 (desktop persistence shell increment): route desktop playback
  history writes through the shared `LibraryRepository`/SQLDelight binding.
  Library scan, favourites, queue, and streaming presentation still use the
  migration shell and must be replaced by shared state before Phase 1 closes.
- [x] Phase 1 (desktop library shell increment): route selected-folder scans
  through the shared `LocalMusicLibrary` contract and its desktop metadata
  adapter, keeping the existing UI rendering while removing duplicate scan
  logic from the desktop shell.

## Phase 2 — platform abstraction layer

Status: in progress.

- [x] Local library scanning: define the shared `LocalMusicLibrary` contract
  and add a desktop JVM recursive scanner using jaudiotagger for MP3/FLAC/AAC/
  WAV-family metadata, plus an Android MediaStore adapter.
- [x] FFmpeg tasks: define a shared runner contract and add a desktop actual
  that executes internally-built argument lists with `ProcessBuilder`, without
  invoking a shell. It uses the configured/system `ffmpeg` executable.
- [x] Haptics and media controls: add shared boundaries with intentional desktop
  no-op stubs, Android haptic feedback, and migration-safe Android control
  placeholders. System tray/media-session integration remains lower priority.
- [x] Update checks: add a shared GitHub Releases latest-release checker. The
  desktop installer/update application flow remains platform-specific.
- [x] Desktop onboarding platform: implement no-prompt desktop permissions,
  no-op haptics preview, and JVM preference persistence behind the shared
  onboarding contract.
- [x] Google Sign-In: desktop OAuth via a system browser plus loopback redirect
  selected on 2026-08-13; implemented as `DesktopGoogleAuthClient` with an
  ephemeral loopback callback, state validation, PKCE S256, and system-browser
  launch. A registered desktop client ID is intentionally supplied by the app
  configuration rather than embedded in source.
- [x] Audio playback: add a `PlaybackController` common contract and desktop
  `VlcjPlaybackController` actual. It maps play/pause/seek/queue/repeat state,
  duration/time callbacks, LUFS gain (-14 LUFS target), and libVLC's ten-band
  equalizer into shared state. The desktop shell now uses this controller rather
  than Java Sound.
- [x] Crossfade execution: implement a dual-libVLC-player overlap in
  `VlcjPlaybackController`. It begins the next eligible queue item during the
  configured end window, ramps outgoing/incoming volumes over 24 steps, then
  releases the outgoing native player and commits the next shared queue state.
- [ ] Desktop distribution: libVLC must be bundled into the Compose package and
  Flatpak manifest. The `vlcj` binding compiles without native binaries but
  discovers libVLC at runtime; the current Flatpak template does not yet ship it.
- [x] Update hand-off: add `UpdateInstaller` expect/actuals. Desktop opens the
  verified GitHub release URL in the system browser; installation is delegated
  to the platform package/installer.
- [x] Effects/DSP execution (supported libVLC subset): ten-band EQ, bass boost
  (low EQ bands), loudness enhancement (EQ preamp), LUFS normalization, and
  playback rate are exposed through the shared playback contract. Reverb, 8D
  panning, and fine stereo balance require a PCM callback/DSP mixer and remain
  explicitly unsupported on desktop until that mixer is introduced.

## Phase 3 — screen-by-screen UI + feature porting

- [x] Onboarding: shared nine-step Compose flow added and wired as the desktop
  first-run route. It persists completion and the Android-equivalent setup
  preferences through `DesktopOnboardingPlatform`.
- [x] Library + history: add shared list/history screens wired to the SQLDelight
  `LibraryRepository`, with shared favourite toggle and clear-history actions.
- [x] Explore / streaming: add desktop Explore route backed by the shared Ktor
  InnerTube search catalog; selecting an online song creates a desktop VLC
  playback queue.
- [x] Mini player + full player: add shared player surfaces and wire them to the
  desktop playback controller, including seeking, repeat, transport, and an
  artwork-safe fallback surface.
- [x] Queue: add the shared desktop queue drawer and play-at queue actions.
- [x] Lyrics: add the shared karaoke line surface; provider fetching and current
  track binding still need to be connected to populate it automatically.
- [x] Effects: add desktop controls for the supported VLC effects contract.
- [x] Settings: route the desktop navigation to the shared adaptive settings UI.
- [x] Tag editor: add a desktop tag editor backed by jaudiotagger for local
  title, artist, and album metadata.
- [x] Song actions / sleep timer: add play-next, favourite, playlist creation,
  playlist insertion, plus custom/end-of-song/end-of-queue sleep behavior.
- [x] Snackbar/global visual chrome: add the Compose Canvas animated mesh
  gradient replacement for Android's custom background view.
- [ ] Artwork loading, account screen, playlist-management screen, remaining
  lyrics providers, and desktop libVLC packaging remain in the active
  Phase 3/Phase 2 queue.

### Verification log

| Date | Command | Result | Notes |
| --- | --- | --- | --- |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop` | blocked | Gradle could not start: `JAVA_HOME` is unset and no `java` executable is available. `/usr/lib/jvm/java-25-openjdk` contains only documentation, not a JDK. |
| 2026-08-13 | `git diff --check` | pass | Theme foundation change has no whitespace errors. Desktop compilation cannot be run until a JDK is available. |
| 2026-08-13 | common-domain import audit | pass | Shared lyrics, song, and InnerTube model packages contain no Android, Media3, Gson, OkHttp, or NewPipe imports. |
| 2026-08-13 | `git diff --check` | pass | Phase 1 shared foundation edits contain no whitespace errors. JVM compilation remains unavailable without a JDK. |
| 2026-08-13 | `:composeApp:compileKotlinDesktop` with temporary Temurin 17 | inconclusive | Gradle 9.3.1 starts successfully, but its launcher/daemon loses the tool connection before task output. No compiler result was produced; this is not a source pass. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | pass | Java 25 is now available. Updated deprecated Compose dependency shortcuts to direct coordinates, then corrected shared Ktor, lyrics, typography, and jaudiotagger source issues revealed by the compiler. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | pass | SQLDelight schema generation, JDBC SQLite driver, desktop library repository, and Koin binding compile successfully. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | pass | Shared Paxsenix Spotify/YouTube result parser compiles successfully. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | pass | Shared Ktor `MusicCatalog` and expanded InnerTube browse parsers compile successfully. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | pass | Desktop playback history now writes through the shared SQLDelight repository. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | pass | Desktop selected-folder scanning now uses the shared local-library service. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon` | inconclusive | The isolated Gradle daemon terminates immediately after build start with no task or compiler output; its daemon log ends at `ExecuteBuild`, so this is an environment failure rather than a source pass. |
| 2026-08-13 | desktop compile retry with 1 GiB heap and parallelism disabled | blocked | Gradle reaches root-project configuration but cannot write its cache: `/tmp` reports `Disk quota exceeded`. No Kotlin compiler result is available. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' -Dorg.gradle.parallel=false` | pass | VLCJ playback controller, dual-player crossfade, and the supported desktop DSP controls compile successfully. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' -Dorg.gradle.parallel=false` | pass | Shared nine-step onboarding and desktop first-run route compile successfully. |
| 2026-08-13 | `./gradlew :composeApp:compileKotlinDesktop --console=plain --no-daemon -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' -Dorg.gradle.parallel=false` | pass | Shared player/queue/lyrics shells, Explore stream hand-off, Effects, Settings route, and desktop tag editor compile successfully. |
