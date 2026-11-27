# Android release revisions


| Version | Release builds | Build start (UTC) | android | core-legacy | resources | core (OsmAndCore AAR snapshot) |
|---|---|---|---|---|---|---|
| 5.4.11 | #410 Full, #411 Free (r5.4) | 17.10.2026 16:04 / 16:12 | 13934d83aa | 5a74b81f9c | 7ee9bf7012 | a6a8631756 — snapshot 5.4, core build #112 (17.10 15:56) |
| 5.4.11 huawei | #414 (r5.4) | 27.10.2026 12:32 | 1114cea246 | 5a74b81f9c | 7ee9bf7012 | a6a8631756 — core build #112 |
| 5.5.0 | #416 Full, #417 Free (master) | 05.11.2026 14:39 / 14:44 | 28d919ef79 | 608dbd7145 | d0ed5113a2 | 370eae654b — master-snapshot, core build #482 (05.11 13:46) |
| 5.5.1 | #418 Full, #419 Free (r5.5) | 19.11.2026 14:41 / 14:52 | cbd212b123 | 33179611b2 | 16ae3a3ab7 | a51b5b3600 — snapshot 5.5, core build #115 (19.11 14:20) |
| 5.5.2 | #421 Free, #422 Full (r5.5) | 25.11.2026 16:51 / 16:58 | 948314e915 | 1136ebc1ce | 16ae3a3ab7 | b467ef307e — snapshot 5.5, core build #118 (24.11 21:20) |

Full SHAs are in the JSON. Not releases: #415 (5.5.0 Free, rebuilt as #417), #413 (failed), #420 (aborted).

## How the revisions are collected

- android, core-legacy, resources: the repo manifest of the release build.
- core: OsmAndCore comes as a prebuilt AAR snapshot, not from the manifest. The release build log
  names the snapshot (`Using remote OsmAndCore snapshot (5.4 | 5.5 | master-snapshot)`); it is the
  last successful core snapshot build of that branch that finished before the release build
  started, and its own manifest gives the OsmAnd-core revision. Keep the build dates next to the
  revisions — this match is by time.

Add every new release to the JSON and to the table.
