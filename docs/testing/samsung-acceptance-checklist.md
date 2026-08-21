# Samsung acceptance checklist

Candidate device: Samsung SM-S928U (Galaxy S24 Ultra), Android 16.

| Check | Result | Notes |
|---|---:|---|
| Launch without requesting audio permission | Pass | Cold-launched the redesigned build after explicitly revoking `READ_MEDIA_AUDIO`; no permission dialog or runtime crash. |
| First-run setup and primary navigation fit the phone | Pass | Visually inspected the live dark-theme screen on SM-S928U: source choices fit cleanly and persistent navigation contains only Home, Library, and More. |
| Full Android instrumentation suite | Pass | All 21 tests passed together on SM-S928U / Android 16, including branding identity, Room migration, Library, navigation, playback controls, backup behavior, separate clickable track cards, artist/genre/playlist drill-in, direct and bulk playlist additions, dismissible scan results, and a dedicated scan screen that visibly identifies Music, Please! while preserving its mode title, progress, and exit control. |
| Music, Please! signed update and state preservation | Pass | Installed the prior signed release, selected Light appearance, then installed the renamed signed release with `adb install -r`. First-install time remained unchanged, Light stayed selected, the new label/copy appeared, `READ_MEDIA_AUDIO` was revoked, cold launch succeeded, and the crash log was empty. |
| MediaSession service connects | Pass | Instrumentation connected to the background service. |
| Folder-only and file-only source flows | Automated pass | SAF read-only grant tests pass; interactive folder choice remains a user acceptance step. |
| Whole-device denial preserves scoped sources | Automated pass | Permission-flow test. |
| First source starts dedicated scan; later sources scan quietly | Automated pass | Source-addition policy tests cover folder, file, and optional whole-device entry points; personal-library throughput remains a user acceptance step. |
| Dedicated scan pauses playback, checkpoints on exit, and closes when finished | Automated pass | Scan-session and UI/runtime contracts pass; long personal-library throughput remains a user acceptance step. |
| Library view memory, contextual search, and two-row playback controls | Pass | Host state/search tests and on-device Compose tests pass; Favorites and the decorative level indicator are absent as designed. |
| Screen-lock/background playback | Pending personal-library trial | Requires selecting a personal MP3 source. |
| Galaxy Buds previous/next and 3-second restart | Pending earbuds trial | Direct and MediaSession controls are present. |
| USB backup folder visibility, manual backup, restore | Pending folder selection | SAF ZIP logic is covered by unit tests. |
| Cross-phone relinking | Pending second Samsung | Exact-path and ambiguous-match behavior is covered by unit tests. |

Pending rows deliberately require user-selected music, earbuds, or a second physical phone; the test build never grants itself broader access.
