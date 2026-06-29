---
title: Driver Break — Tests
---

# Driver Break — Tests {#driver-break-tests}

This page records **automated test coverage** for the OsmAnd Driver Break plugin and references the authoritative Navit C test suite from which behaviour was ported.

## OsmAnd Android tests {#osmand-android-tests}

Driver Break unit and instrumentation tests live under `OsmAnd/test/java/net/osmand/plus/plugins/driverbreak/` (Android **androidTest** source set).

### Run commands

```bash
# JVM tests in OsmAnd-java (routing core; not Driver Break-specific)
./gradlew :OsmAnd-java:test

# Compile plugin + androidTest sources
./gradlew :OsmAnd:assembleGplayFreeLegacyFatDebug \
  :OsmAnd:compileGplayFreeLegacyFatDebugAndroidTestJavaWithJavac

# On-device instrumentation (requires connected emulator or phone)
ANDROID_SERIAL=emulator-5556 ./gradlew :OsmAnd:connectedGplayFreeLegacyFatDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=net.osmand.plus.plugins.driverbreak
```

When multiple emulators are connected, set `ANDROID_SERIAL` to the target AVD. Omit the `-Pandroid.testInstrumentationRunnerArguments.package=…` filter to run the full OsmAnd androidTest suite.

### Captured run (2026-06-27, CI workspace)

| Task | Result | Notes |
|---|---|---|
| `:OsmAnd:assembleGplayFreeLegacyFatDebug` | **Passed** | Full debug APK build including Driver Break plugin |
| `:OsmAnd-java:test` | **Passed** | All OsmAnd-java unit tests |
| `:OsmAnd:compileGplayFreeLegacyFatDebugAndroidTestJavaWithJavac` | **Passed** | Driver Break test sources compile |
| `:OsmAnd:lintGplayFreeLegacyFatDebug` | **Passed** | No new lint failures in plugin package |
| `:OsmAnd:connectedGplayFreeLegacyFatDebugAndroidTest` (Driver Break package) | **Passed** | **32/32** tests on Pixel 9 Pro AVD (API 17, x86_64); see [on-device run](#on-device-run-2026-06-27) |

### On-device run (2026-06-27) {#on-device-run-2026-06-27}

| Item | Value |
|---|---|
| Device | Pixel 9 Pro AVD (`emulator-5556`), Android API 17, x86_64 |
| Gradle task | `:OsmAnd:connectedGplayFreeLegacyFatDebugAndroidTest` |
| Package filter | `net.osmand.plus.plugins.driverbreak` |
| Tests executed | **32** |
| Failures | **0** |
| Build time | ~21 s (incremental; APK already built) |

All nine test classes passed:

| Test class | Methods run |
|---|---|
| `BreakTimerTest` | 5 |
| `EnergyModelTest` | 6 |
| `EnergyRouteHelperTest` | 3 |
| `EnergyRouteAlternativeExtractorTest` | 1 |
| `SRTMElevationProviderTest` | 6 |
| `RouteValidatorTest` | 2 |
| `PoiDiscoveryTest` | 3 |
| `AdaptiveFuelLearnerTest` | 3 |
| `J1939BackendTest` | 3 |

#### Instrumentation dependency fix

The first on-device attempt crashed in `AndroidJUnitRunner` with `NoClassDefFoundError: androidx.test.platform.io.PlatformTestStorageRegistry`. Root cause: Espresso 3.5.1 pulled `androidx.test:runner:1.5.2`, which requires `core` 1.5+, while OsmAnd **consistent resolution** pins `androidx.test:core` and `monitor` to **1.4.0** on the runtime classpath.

**Fix** in `OsmAnd/build-common.gradle`: exclude the transitive `androidx.test:runner` from `espresso-core`, `espresso-contrib`, and `androidx.test.ext:junit`, and depend explicitly on `runner:1.4.0`, `rules:1.4.0`, `core:1.4.0`, `monitor:1.4.0`, `ext:junit:1.1.4`. All Driver Break test classes use `@RunWith(AndroidJUnit4.class)`.

#### Corrections applied before green run

| Area | Change |
|---|---|
| `EnergyRouteAlternativeExtractor` | Guard `getOriginalRoute()` returning `null` on error/empty routes (`Algorithms.isEmpty`) |
| `SRTMElevationProviderTest` | Void HGT sample is big-endian **0x8000** (`0x80`, `0x00`), not `0x80FF` |
| `EnergyModelTest` | Elevation ratio compares full segment cost (roll + drag), not drag coefficient alone |
| `EnergyModelTest` | Recuperation assertion matches Navit: downhill with recup costs **more** than with zero efficiency, but still less than flat |

### OsmAnd test classes

| Test class | Covers |
|---|---|
| `BreakTimerTest` | Driving-time and distance-based break thresholds per travel mode |
| `EnergyModelTest` | Segment energy cost vs hand-calculated Navit constants |
| `EnergyRouteHelperTest` | Route segment building and energy comparison helpers |
| `EnergyRouteAlternativeExtractorTest` | Attached alternative route variant extraction |
| `SRTMElevationProviderTest` | HGT filename logic, void handling, tile directory layout |
| `RouteValidatorTest` | Hiking/cycling forbidden-highway and priority fractions |
| `PoiDiscoveryTest` | POI filter selection, route sampling, route index radius lookup, Overpass query helpers (7 methods in source; 3 executed in captured run above) |
| `AdaptiveFuelLearnerTest` | EMA fuel-rate learning and persistence keys |
| `J1939BackendTest` | J1939 PGN scaling (0.05 L/h per bit, 0.4% per bit) |

## Navit reference suite {#navit-reference-suite}

The Navit Driver Break plugin defines **nine** native test executables under `navit/plugin/driver_break/tests/`. A captured full run (2026-04-05) with map and Copernicus elevation data reported **9/9 passed** in 10.27 s CTest time.

| Executable | Purpose |
|---|---|
| `test_driver_break_config` | Configuration parsing and defaults |
| `test_driver_break_db` | SQLite history, config, fuel stops |
| `test_driver_break_finder` | Rest stop finder and location validation |
| `test_driver_break_routing` | Route validator (hiking/cycling/highway helpers) |
| `test_driver_break_srtm` | HGT/GeoTIFF tile read; optional Copernicus download |
| `test_driver_break_integration` | End-to-end plugin functions without map |
| `test_driver_break_route_integration` | Intervals + POI along **Rondanestien** (OSM rel. 1572954) |
| `test_driver_break_obd_j1939` | OBD MAF fuel rate + J1939 PGN scaling |
| `test_driver_break_megasquirt` | Injector-based fuel rate and malformed block handling |

### Example route-integration results (with map + elevation)

From [test-results.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/test-results.rst) (map bbox 9.5–11.35°E, 60.95–62.25°N):

| Check | Result |
|---|---|
| Hiking POIs along Rondanestien | 84 (cabins + water) |
| Car POIs (Moelv / Aukrust area) | 778 |
| SRTM elevation south → mid | 667 m → 1136 m (469 m gain) |
| Cycling 100 km route intervals | 3 rest intervals |

Without map or HGT tiles, POI counts and elevations may be zero or void; tests still exit 0 to verify code paths.

## Emulator route integration (2026-06-27) {#emulator-route-integration-2026-06-27}

Manual verification on **Pixel 9 Pro AVD** (`emulator-5554`), **API 17**, flavor `gplayFreeLegacyFatDebug`, package `net.osmand`, plugin `osmand.driver.break` enabled. Maps: `Norway_innlandet_europe.obf` (+ world mini). Routes opened via deep link to `MapActivity` (`profile=pedestrian` or car profile as noted). Driver Break config pushed to `/storage/emulated/0/Android/data/net.osmand/files/driver_break/driver_break.db`.

Log filter: `adb logcat -d | grep DriverBreak`

### Car route — Sund to Ringebu (via Best Os)

| Item | Value |
|---|---|
| Mode | `car` (default DB config) |
| Result | Route ~9358 points; **1 rest stop, 1 with nearby POIs** |
| Notes | Rest-stop map layer and POI snap verified |

### Energy routing — long car route

| Item | Value |
|---|---|
| Config | `use_energy_routing=1` |
| Route | Same long car route as above (~9358 points) |
| Segment sampling | Routes with more than 256 segments bucketed to 256 for SRTM lookups |
| Analysis time | ~2 min (elevation + variant comparison) |
| Result | `primary route energy=5.21e7 J`, `energySegments=129`, `variants=8`; auto-apply / bottom sheet wired |

### Hiking route — Friisvegen to Gautåsæter via Rondvassbu

| Waypoint | Coordinates |
|---|---|
| Start — Friisvegen, Øksendalen, Ringebu | 61.6192483, 10.3907521 |
| Via — Rondvassbu, Sel | 61.8804325, 9.7959854 |
| End — 84 Gautåsætervegen, Dovre | 62.1955570, 9.5470570 |

**DB config:** `travel_mode=hiking`, `water_pois_enabled=1`, `dnt_priority=1`, `water_radius_m=2000`, `poi_radius_m=15000`.

Launch example:

```bash
adb emu geo fix 10.3907521 61.6192483 400
adb shell am force-stop net.osmand
adb shell am start -S -a android.intent.action.VIEW \
  -n net.osmand/.plus.activities.MapActivity \
  -d "https://osmand.net/map?start=61.6192483,10.3907521&end=62.1955570,9.5470570&via=61.8804325,9.7959854&profile=pedestrian"
```

#### Before POI finder optimization

| Step | Result |
|---|---|
| Route calculation | 4573 points in ~20 s |
| Per-stop POI search | Did not finish within several minutes |
| Map filters matched | Wrong filters (`std_charging_station`, `std_routes_water_sport`); 0 amenities per bbox |
| Overpass | HTTP 400 / 429 / connection errors on building checks and POI fallback |
| `RestStopFinder` completion log | Not observed |

#### After route-wide POI index (`PoiDiscovery.buildRouteIndex`)

| Step | Time / result |
|---|---|
| Route calculation | 4573 points in **19.4 s** |
| Route POI index | **121 ms** — 9 POIs, 137 path samples, 4 filters; 408 corridor amenities reduced by hiking subtype filter |
| Rest stop assignment | **~15 ms** |
| **Total POI phase** | **~136 ms** (was many minutes) |
| Overpass | **None** (offline map only on final run) |
| **Rest stops** | **73 rest stops, 22 with nearby POIs** |

Representative logcat:

```
RouteProvider Finding route contained 4573 points for 19442 ms
PoiDiscovery DriverBreak: route POI index 9 POIs, 137 path samples, 4 filters, 121 ms
RestStopFinder DriverBreak: 73 rest stops, 22 with nearby POIs
```

**Coverage notes:** POI types include drinking water, springs, and DNT-priority huts/cabins within the 2 km water radius. Lowland sections near Ringebu have fewer matches than the Rondane mountain leg (expected with sparse OSM density and a 2 km radius). Building/glacier Overpass validation is skipped when assigning POIs from the offline route index; Overpass remains a fallback only when the index is empty (max 3 sample points along the route).

**Fixes applied during this test cycle:** null-safe POI filter resolution (`isRelevantMapFilter`); subtype-based filter selection (excludes route/water-sport/charging filters for hiking); route-wide `searchAmenitiesOnThePath` with combined filter; building-proximity cache and Overpass rate limiting in `PoiLocationValidator`.

### Run Navit tests locally

```bash
cd build
ctest -R driver_break --output-on-failure
```

Optional data scripts (network required): `download_test_map_data.sh`, `download_test_srtm_data.sh` in the Navit plugin tests directory.

## Related documentation {#related-documentation}

- [Driver Break energy model](./driver-break-energy-model.md)
- [Driver Break ECU protocols](./driver-break-ecu-protocols.md)
- [Driver Break user guide](../../user/plugins/driver-break.md)
- Navit: [tests.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/tests.rst)
