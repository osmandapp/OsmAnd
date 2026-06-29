# Driver Break plugin (Android)

## Plugin overview

The **Driver Break** plugin provides configurable rest period management for car, truck, motorcycle, hiking, and cycling travel modes. It tracks driving time or distance, suggests rest stops using OpenStreetMap POIs, validates hiking/cycling routes, compares route variants for predicted energy use, and optionally reads live fuel data from OBD-II or J1939.

This package is a port of the Navit Driver Break plugin from the [Supermagnum/navit `feature/driver-break` branch](https://github.com/Supermagnum/navit/tree/feature/driver-break/docs/user/plugins/driver-break). Authoritative behaviour and formulas are documented in that repository's RST docs; OsmAnd-specific wiring lives here.

## Package structure

| Class | Responsibility | Key dependencies |
|---|---|---|
| `DriverBreakPlugin` | Plugin entry: widget, route listener, settings screen, elevation prompts | `OsmandPlugin`, `BreakTimer`, `ElevationDownloadCoordinator` |
| `BreakTimer` | Tracks time (car/truck/moto) or distance (hiking/cycling); exposes next-break status | `DriverBreakSettings`, `SystemClock` |
| `BreakStatus` | Immutable snapshot of timer state for UI | `TravelMode` |
| `TravelMode` | Enum: car, truck, hiking, cycling, motorcycle config keys | — |
| `EnergyModel` | Kinematic segment cost: roll + drag + grade + recuperation + standby | Pure Java |
| `EnergyParams` | Mass, Cd, area, recuperation, standby, temperature, speed cap | — |
| `SegmentData` | One route segment inputs for `EnergyModel` | — |
| `EnergyRouteHelper` | Builds segments from `RouteSegmentResult`; compares alternative energy | `SRTMElevationProvider`, `EnergyModel` |
| `EnergyRouteAlternativeExtractor` | Collects primary and attached alternative segment lists | `RouteSegmentResult` |
| `EnergyRouteBottomSheet` | UI prompt when a lower-energy alternative exists | `MenuBottomSheetDialogFragment` |
| `SRTMElevationProvider` | Reads HGT and GeoTIFF 1 degree tiles; returns `VOID_ELEVATION` when missing | `mil.nga.tiff`, app data dir |
| `SRTMTileIndex` | Integer degree tile key for downloads and coverage | — |
| `ElevationDownloadManager` | Sequential tile download: Copernicus, Viewfinder, NASA | `NetworkUtils`, dedicated executor |
| `ElevationDownloadCoordinator` | First-enable and route-crossing download prompts | `ElevationCoverageHelper`, settings |
| `ElevationCoverageHelper` | Missing tiles by region bbox or route sample points | `SRTMElevationProvider` |
| `ElevationRegionResolver` | Point to OsmAnd `WorldRegion` (county/country) | `app.getRegions()` |
| `ElevationAdministrativeRegion` | Region id, display name, bbox for grouped downloads | `QuadRect` |
| `ElevationDownloadPromptBottomSheet` | Accept/decline elevation download UI | `PluginsHelper` |
| `RestStopFinder` | Distance/time intervals along `RouteCalculationResult` | `PoiDiscovery`, settings, `RestStopMapLayer` |
| `RestStopMapLayer` | Map markers for planned rest stops along the active route | `RestStop` |
| `RestStop` | One proposed stop coordinate and attached POI names | `LatLon` |
| `PoiDiscovery` | Route-wide POI index (`searchAmenitiesOnThePath`); per-stop radius lookup; Overpass fallback when index empty | `PoiUIFilter`, Overpass API |
| `PoiLocationValidator` | Building/glacier distance checks for candidates | Map layers |
| `RouteValidator` | Hiking/cycling forbidden fraction, priority paths, MTB scale warnings | `RouteSegmentResult` |
| `ValidationResult` | Fractions and warning strings from validation | — |
| `OBDBackend` | Fuel rate/level via Vehicle Metrics `OBDDataComputer` widgets | `VehicleMetricsPlugin` |
| `J1939Backend` | USB-CAN decode for PGN fuel rate/level at 500000 bps | `usb-serial-for-android` |
| `MegaSquirtBackend` | **Stub** — throws `UnsupportedOperationException` | — |
| `ECUDataSource` / `ECUDataListener` | Fuel data abstraction and callbacks | — |
| `AdaptiveFuelLearner` | EMA over `fuel_samples`; persists `adaptive_learned_lkm` | `DriverBreakDatabase`, `dbExecutor`; on/off via **Adaptive fuel learner** on General settings tab |
| `RangeEstimator` | Range from fuel level and learned or configured consumption | `AdaptiveFuelLearner` |
| `DriverBreakDatabase` | SQLite: config, rest/fuel history, fuel_samples | `SQLiteOpenHelper` |
| `DriverBreakSettings` | Typed config accessors; **DB thread only** for sync methods | `DriverBreakDatabase` |
| `DriverBreakSettingsController` | Applies UI edits to settings on `dbExecutor` | Fragment tabs |
| `DriverBreakSettingsSnapshot` | Read-only settings + tile count for display | — |
| `DriverBreakWidget` | `SimpleWidget` showing time/distance to next break | `BreakTimer` |
| `DriverBreakSettingsFragment` | Tabbed settings: General, Intervals, Overnight, Aero, Elevation | `BaseSettingsFragment` |
| `DownloadProgressListener` | SRTM batch download progress callbacks | — |

## Threading model

| Executor | Owner | Used for |
|---|---|---|
| `DriverBreakSettings.dbExecutor` | Single-thread | **All** SQLite reads/writes and rest stop route analysis |
| `DriverBreakPlugin.downloadExecutor` | Single-thread | Post-route energy analysis async work |
| `ElevationDownloadManager.downloadExecutor` | Single-thread | SRTM HTTP tile downloads |
| `ElevationDownloadCoordinator.backgroundExecutor` | Shared with plugin | Region resolve + missing-tile scans |
| `PoiDiscovery.downloadExecutor` | Single-thread | Overpass API fallback queries |

UI updates after background work must run on the main thread (`MapActivity` / fragment lifecycle). **Do not** call `DriverBreakSettings` synchronous getters or setters from the main thread.

## Database schema

```sql
CREATE TABLE IF NOT EXISTS config (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS rest_stop_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp INTEGER NOT NULL,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  mode TEXT NOT NULL,
  duration_s INTEGER,
  poi_json TEXT
);

CREATE TABLE IF NOT EXISTS fuel_stop_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp INTEGER NOT NULL,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  litres REAL
);

CREATE TABLE IF NOT EXISTS fuel_samples (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp INTEGER NOT NULL,
  distance_m REAL NOT NULL,
  fuel_litres REAL NOT NULL,
  mode TEXT NOT NULL
);
```

Default config keys are seeded in `DriverBreakDatabase.DEFAULT_CONFIG` (aligned with Navit `driver_break_config_default()`).

## Extending the plugin

1. **Add a travel mode** — Extend `TravelMode`, defaults in `DriverBreakDatabase`, interval keys in settings XML/fragment, and branch logic in `BreakTimer` / `RestStopFinder`.
2. **Add an ECU backend** — Implement `ECUDataSource`, register in `DriverBreakPlugin`, add a config flag, respect serial/CAN mutual exclusion with existing backends.
3. **Add a POI category** — Extend `PoiDiscovery` map filters and Overpass query builders; add radius settings if needed.

## Running tests

```bash
# Compile plugin and androidTest sources
GRADLE_OPTS="-Xmx8g" ./gradlew :OsmAnd:assembleGplayFreeLegacyFatDebug \
  :OsmAnd:compileGplayFreeLegacyFatDebugAndroidTestJavaWithJavac

# On-device (emulator or USB debugging)
./gradlew :OsmAnd:connectedGplayFreeLegacyFatDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=net.osmand.plus.plugins.driverbreak
```

**Status (2026-06-27):** 32/32 Driver Break instrumentation tests passed on Pixel 9 Pro AVD (API 17). Emulator route integration (car POI, energy routing, hiking Friisvegen–Rondvassbu–Gautåsæter) is documented in [driver-break-tests.md](../../../../../../web/main/docs/technical/build-osmand/driver-break-tests.md#emulator-route-integration-2026-06-27).

| Test class | Coverage |
|---|---|
| `BreakTimerTest` | Soft/mandatory break timing and hiking distance accumulation |
| `EnergyModelTest` | Segment cost vs hand-calculated Navit constants |
| `EnergyRouteHelperTest` | Segment list building and cheaper-alternative detection |
| `EnergyRouteAlternativeExtractorTest` | Attached route variant extraction |
| `SRTMElevationProviderTest` | Tile naming, void elevation, directory layout |
| `RouteValidatorTest` | Forbidden/priority highway fractions |
| `PoiDiscoveryTest` | Filter wiring, route sampling, route index lookup, Overpass query helpers |
| `AdaptiveFuelLearnerTest` | EMA learning and config persistence |
| `J1939BackendTest` | PGN fuel rate/level scaling |

Full build/test log: [driver-break-tests.md](../../../../../../web/main/docs/technical/build-osmand/driver-break-tests.md) (repo `web/main/docs`).

## Known limitations / planned work

| Feature | Status |
|---|---|
| MegaSquirt ECU backend | Planned — see `MegaSquirtBackend.java` stub |
| Battery electric vehicle (SoC, power) | Planned — Navit `todo-electric.rst` |
| Routing engine deep integration | Planned — post-calculation re-rank only |
| Truck weekly/bi-weekly rest | Planned — daily rules in defaults only |
| iOS port | Not planned for this cycle |
| Motorcycle adventure terrain routing | Partial — legal note in user docs; full Navit highway filter not fully ported |
| Hiking alt stage / daily max in settings UI | Exposed on Intervals tab (Hiking / Cycling categories) |
| Motorcycle break duration / max daily riding | Exposed on Intervals tab (Motorcycle category) |

## Documentation map

| Audience | Location (repo root) |
|---|---|
| End users | [`web/main/docs/user/plugins/driver-break.md`](../../../../../../web/main/docs/user/plugins/driver-break.md) |
| Energy model | [`web/main/docs/technical/build-osmand/driver-break-energy-model.md`](../../../../../../web/main/docs/technical/build-osmand/driver-break-energy-model.md) |
| ECU protocols | [`web/main/docs/technical/build-osmand/driver-break-ecu-protocols.md`](../../../../../../web/main/docs/technical/build-osmand/driver-break-ecu-protocols.md) |
| Tests | [`web/main/docs/technical/build-osmand/driver-break-tests.md`](../../../../../../web/main/docs/technical/build-osmand/driver-break-tests.md) |
| Formula quick reference | `FORMULAS.md` (this package) |
| Navit source of truth | [Supermagnum/navit driver-break docs](https://github.com/Supermagnum/navit/tree/feature/driver-break/docs/user/plugins/driver-break) |
