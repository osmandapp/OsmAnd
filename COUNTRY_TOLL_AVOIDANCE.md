# Country-specific toll-road avoidance

## Usage

For an OsmAnd offline routing profile that supports **No toll roads**, open
**Avoid roads** and scroll to **Avoid toll roads by country**. Only countries with
installed offline maps in this app are shown, each with an independent
**No toll roads** switch. Country names are localized and sorted alphabetically.
Downloading a regional map or a roads-only map is enough to show its country;
multiple maps from the same country produce just one section. World basemaps,
Wikipedia, terrain, voice, and unrecognized custom map filenames do not add
countries. The list uses indexed map filenames and OsmAnd's region catalog,
and refreshes when the sheet is reopened after a map download or removal.
If no matching maps are installed, the country section is hidden.

- Switch on a country to discourage toll roads and toll booths there.
- Leave other countries switched off to keep their normal routing behavior.
- Tap **Apply** to save the selection for the profile and recalculate the route.
- Dismiss the sheet without applying to discard changes. Pending selections
  survive recreation of the sheet, such as a device rotation.
- The existing global **No toll roads** setting still takes precedence: when it
  is on, toll-road avoidance applies everywhere. Country selections are retained
  for when the global setting is turned off again.
- Removing all maps for a country hides its switch but does not erase its saved
  selection. Applying changes to other countries preserves hidden selections;
  downloading that country's maps again restores its switch and saved state.

## Routing behavior and scope

Country-specific avoidance reuses the routing profile's toll-road priority and
toll-booth penalties rather than forbidding every toll road. Like the existing
global option, a toll road may still be chosen when alternatives are unsuitable.
Other road restrictions and vehicle profile parameters remain in effect.

Countries are identified using OsmAnd's offline country-boundary data, including
regional maps belonging to a larger country. Road priority is evaluated once per
map road using its middle geometry point. A road crossing a country boundary
therefore uses the country at that point; toll booths are checked at the booth's
own location. This feature does not split a map road at a country boundary.
The selected countries' exact boundary polygons are loaded once per routing
configuration into a private spatial index. Subsequent toll-road and toll-booth
checks use that in-memory snapshot, not point searches of the shared boundary
file. Rebuilding the profile for access checks shares the snapshot.

For ordinary navigation with country-specific avoidance active, the first search
uses the profile's usual routing engine, including native/fast routing when
configured. The completed route is checked for toll-road priorities and traversed
toll-booth penalties affected by the selected countries. If none change, that
route is returned without a country-aware Java search. For example, selecting
Switzerland does not switch a Vienna–Bratislava route to the slow Java engine.
This check uses the actual route, not just its endpoints or a straight-line box,
so tolls in a selected country passed through en route are also checked.

If selected tolls affect the first route, it is discarded and the route is
recalculated using country penalties. With the locally modified native library,
the polygons and profile's existing toll rules are passed to C++ once per search.
Native routing evaluates toll-road priorities and toll-booth costs at the same
locations as the Java router. Native hierarchical (HH) routing checks detailed
road costs, corrects underestimated stored edge costs, and repeats the graph
search as needed. Detailed paths are cached only within that HH calculation.
The candidate is not treated as a fixed corridor that could prevent finding
better alternatives. This avoids the previous full Java graph expansion but
does not make affected long routes as fast as ordinary routing.

Capability checks prevent older published native libraries from silently
ignoring the selected countries. Without native country support, the fallback
uses Java with a restricted routing budget and periodic heap-headroom checks;
it can report insufficient memory rather than exhausting the heap. These
safeguards reduce risk, not a guarantee against all allocation failures.
GPX following/approximation remains on the country-aware Java path. With no
countries enabled, or global toll avoidance enabled, routing-engine selection
is unchanged. Online routing services and the public-transport avoidance sheet
do not expose these switches.

The latest test APK is ARM64 only and includes the custom native library:
`/home/bucek/git/OsmAnd/build/apks/OsmAnd-country-tolls-dev-cleanup-arm64.apk`.
Its version is `5.5.0-country-tolls-dev-cleanup`. It was installed over the
previous test app before the phone disconnected, retaining the same application ID
and signing certificate. Build/package notes are in
`/home/bucek/git/OsmAnd/build/apks/README.txt`. Both
repositories contain necessary source changes:
- `/home/bucek/git/OsmAnd`: application, Java routing, and JNI method declarations.
- `/home/bucek/git/core-legacy`: country geometry, native toll rules, JNI bridge,
  and HH detailed-cost validation.
Replacing `libosmand.so` with an unmodified published library loses native
country support and uses the guarded Java fallback instead.

## Verification

With the usual OsmAnd external resources available in the sibling `resources`
checkout, run from the project root:

```sh
./gradlew :OsmAnd-java:test --tests net.osmand.router.CountryTollAvoidanceRouterTest \
  --tests net.osmand.map.OsmandRegionsTest \
  --no-daemon -Dorg.gradle.jvmargs=-Xmx4g --max-workers=2
```

The tests cover country isolation for identical road tags, switching avoidance
off, toll booths, global avoidance, preservation of profile parameters and road
restrictions, caching, router rebuilding, unreadable boundary data, and real
country lookups both before and after loading the region spatial cache. Routing
regressions check that there are no per-road boundary-file searches, profile
rebuilds reuse one snapshot, and lookups work after closing the source reader.
A real toll-routing fixture compares normal routing, global avoidance, and
country selections before and after switching one country off. A separate phase
dispatch test verifies that a long-route basemap search retains the same
country-aware router; it does not calculate a complete long-distance route.
An engine-dispatch regression uses Vienna and Bratislava toll-road coordinates
and Switzerland selected, asserting that the native HH entry point is used once
and Java fallback is not entered. The native engine is stubbed in that test;
it is not a full native Vienna–Bratislava calculation. Additional checks cover
selected transit countries, traversed toll booths, and preservation of other
preferences/avoided roads in the plain candidate router. Region tests also
cover downloaded-map filtering, deduplication, roads-only and nested
regional maps, countries outside continents, ignored non-map resources, empty
lists after removing maps, and filename matching in different locales.

The Android instrumentation test is at
`/home/bucek/git/OsmAnd/OsmAnd/test/java/net/osmand/plus/routing/CountryTollFastRoutingTest.kt`.
It compares normal routing against Switzerland selected on Vienna–Bratislava,
Bratislava–Vienna, and Vienna–Prague using the installed maps and real native
library. It asserts identical route roads, native calls, and no Java graph search.
The longer route requires Czech maps. A separate application-level check uses
the saved car profile with only Switzerland selected, global avoidance off,
and safe mode off, and calls `RouteProvider` through navigation-result preparation.
These tests never write saved routing settings and skip when their map/native
library/profile prerequisites are missing.

Before the cleanup, the native integration suite ran on the Samsung SM-A515F
using its installed maps and the custom ARM64 library: all six tests passed.
The two affected long-route tests call `RouteProvider`, prepare navigation
directions, assert native country/HH calls and Java-heap headroom, and compare
selected-country toll costs in the normal candidate and returned route.
Their country overrides exist only in memory, never in saved preferences.

Bratislava–Zürich measured results:

| Country selected | Elapsed | Peak Java heap | Selected-country toll distance, before → after |
| --- | --- | --- | --- |
| Slovakia (saved selection) | 76.9 s | 101 MiB | 2.49 km → 0 km |
| Switzerland (in-memory override) | 167.3 s | 101 MiB | 113.84 km → 0 km |

Both completed without the prior 512 MiB Java-heap failure. They still used
toll roads outside the selected country. These are penalties, not absolute
prohibitions; zero selected-country toll use is a result for these routes,
not a guarantee for every route. Nearly three minutes for the Switzerland
case remains slow. Peak Java heap is not total app memory: a post-calculation
`dumpsys meminfo` sample showed about 1.0 GiB total PSS, including roughly
750 MiB of native heap pages. Native memory and long-route speed still need
further optimization; tests do not establish safety for every longer route.

Unaffected normal routing versus Switzerland selected was 3.0 s versus
3.5 s for Vienna–Bratislava, 3.0 s versus 3.6 s for Bratislava–Vienna,
and 12.7 s versus 10.4 s for Vienna–Prague. Each comparison returned
identical roads with native routing and no Java graph search. The application
test prepared navigation directions for Vienna–Bratislava in 4.8 s.
Timing differences can reflect caching.

All 19 navigation-profile files remained byte-for-byte unchanged during those
update/tests. Standalone native geometry tests also passed 37,715
comparisons against Java's boundary containment results. The Java suite had
2,352 total tests, 28 skipped, and no failures/errors (2,324 passed).

### Cleanup and expanded regression coverage

The cleanup preserves the working routing algorithm. It removes an unnecessary
reference to a second base router, consolidates copying of explicitly avoided
roads, shares native dispatch arguments, and consolidates HH detailed-result
conversion without changing when shortcut costs are corrected. Comments explain
why avoided roads need copying, why ordinary-road rules can be skipped, and why
country-aware detailed paths must survive HH retries. The production files saved
in the working baseline have a net reduction of five lines; the larger test suite
adds code intentionally.

The initial candidate inspection now evaluates profile rules only for toll roads
and traversed toll booths, rather than every road and geometry point. In an
isolated JVM benchmark (200 synthetic roads, 500 ordinary points per road,
25 warmups and 100 iterations), the working baseline took 15.6–20.1 ms and the
cleanup took 4.7–5.6 ms: approximately three times faster for that inspection.
This is **not** a full-route or phone benchmark, and does not establish an
improvement to the long-route graph search or total memory use.

Validation of the cleaned-up build:

- Fresh complete Java suite: **2,358 tests total, 2,330 passed, 28 skipped**,
  no failures or errors. New regression checks cover skipping ordinary-road rule
  evaluation, rebuilt global-avoidance behavior, bounded-cache eviction, empty
  selections, and native geometry/profile export and heap safeguards.
- Native boundary, bounded-cache and geometry tests pass with AddressSanitizer
  and UndefinedBehaviorSanitizer, including **37,715 containment comparisons**
  against Java.
- The ARM64 app and instrumentation APKs compile. APK signature, previous-build
  certificate match, ZIP integrity, 16 KiB ZIP alignment, packaged feature code,
  and an exact match to the rebuilt native library are verified.
- The expanded phone suite covers unaffected routes, both affected Zürich cases,
  multiple selections, a transit country, global avoidance, and cancellation
  followed by a fresh calculation. All ten cases have a recorded pass **across
  runs**, not in one completed all-green suite. The first completed run passed
  nine of ten; its transit test incorrectly assumed a direct Bratislava–Prague
  route would use Austrian tolls. The corrected test supplies a Vienna waypoint
  with both endpoints outside Austria. The final rerun recorded six passes,
  including that corrected case and both Zürich routes, before the phone
  disconnected during the seventh test. That interrupted run is not a complete
  suite pass, and the remaining four tests were not completed in that rerun.
- The 19 navigation-profile files are byte-for-byte unchanged between the backup
  before the cleanup and the latest backup before the final rerun. Only ordinary
  app version/startup metadata and the last-calculated itinerary hash changed in
  common/activity preferences. No after-disconnection snapshot was possible;
  the final interrupted run's saved settings cannot be freshly compared.

The cleaned-up APK was installed before the final rerun. Download access,
downloaded-country filtering, package ID and signing key are retained. The
previous native-HH APK is preserved for rollback. No further claim is made about
affected long-route speed, native memory costs, other devices, or GPX following.

The verified cleanup APK is
`/home/bucek/git/OsmAnd/build/apks/OsmAnd-country-tolls-dev-cleanup-arm64.apk`
(version `5.5.0-country-tolls-dev-cleanup`, ARM64 only). Final handoff checks
rebuilt the standalone native tests with sanitizers and repeated the APK
signature, alignment, checksum, packaged-code and native-library comparisons.
Build and test evidence, reproduction commands, and the rollback APK are listed
in `/home/bucek/git/OsmAnd/build/apks/README.txt`.

For on-device verification:

1. Download maps for Austria and Slovakia in the test app. With global
   **No toll roads** off, enable Austria and leave Slovakia off.
   Apply and confirm the route recalculates. Check toll-road alternatives in
   each country and on a route crossing the border.
2. Reopen the sheet and confirm the selection is retained. Change the selection,
   rotate the device, and confirm the pending change remains. Cancel and confirm
   the last applied selection is unchanged.
3. Use another navigation profile and confirm its country selection is separate.
4. Enable global **No toll roads** and confirm it applies in both countries.
5. Disable all country switches and the global option; confirm ordinary routing
   behavior is restored.
6. Confirm only downloaded countries appear. Download a regional map from a
   third country, reopen the sheet, and confirm that country appears only once.
7. Enable a country, apply, and remove all its maps. Reopen the sheet and confirm
   the country is hidden. Apply a change to another country, then download the
   removed country's map again and confirm its previous switch state returns.
8. With no matching offline maps installed, confirm the country section is
   hidden while the original global avoidance options remain available.
9. Repeat the previously stalled route with one country enabled and the other
   disabled. If it remains slow, record the start/end coordinates, navigation
   profile, selected countries, map versions, device model, and elapsed time.
10. With only Switzerland selected, calculate Vienna–Bratislava in both
    directions, then Vienna–Prague with the necessary maps installed. Check that
    the logs say `candidate unaffected; keeping the configured routing engine`
    and that timing is comparable to having all country switches off.
11. With global avoidance and safe mode off, calculate Bratislava–Zürich with
    Switzerland selected, then with Slovakia selected. Check that the logs say
    `using native HH country-aware routing` and that selected-country toll
    exposure is reduced. These long-route cases may still take several minutes.