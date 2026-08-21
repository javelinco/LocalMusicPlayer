# Samsung acceptance checklist

Candidate device: Samsung SM-S928U (Galaxy S24 Ultra), Android 16.

| Check | Result | Notes |
|---|---:|---|
| Launch without requesting audio permission | Pass | Confirmed with runtime permission test and fresh launch. |
| Main UI fits phone and exposes library/search/playlists/sources/more | Pass | Inspected live UI hierarchy on SM-S928U. |
| Library, dedicated scan, playback, and backup Compose suites | Pass | Four tests passed on device. |
| MediaSession service connects | Pass | Instrumentation connected to the background service. |
| Folder-only and file-only source flows | Automated pass | SAF read-only grant tests pass; interactive folder choice remains a user acceptance step. |
| Whole-device denial preserves scoped sources | Automated pass | Permission-flow test. |
| Dedicated scan pauses playback, stays awake, checkpoints on exit | Automated pass | UI/runtime contracts tested; long personal-library throughput remains a user acceptance step. |
| Screen-lock/background playback | Pending personal-library trial | Requires selecting a personal MP3 source. |
| Galaxy Buds previous/next and 3-second restart | Pending earbuds trial | Direct and MediaSession controls are present. |
| USB backup folder visibility, manual backup, restore | Pending folder selection | SAF ZIP logic is covered by unit tests. |
| Cross-phone relinking | Pending second Samsung | Exact-path and ambiguous-match behavior is covered by unit tests. |

Pending rows deliberately require user-selected music, earbuds, or a second physical phone; the test build never grants itself broader access.
