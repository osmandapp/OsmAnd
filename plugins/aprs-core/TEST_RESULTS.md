# aprs-core — Test Results

Full report: [`../../../TEST_RESULTS.md`](../../../TEST_RESULTS.md)

## Build status

Gradle heap fix in `OsmAnd/gradle.properties` (`-Xmx8g`, parallel, caching).  
`assembleNightlyFreeOpenglX86Debug` succeeds in ~18 s. Previous OOM blocker resolved.

## Synthetic IQ (2026-07-02)

| Property | Value |
|----------|-------|
| WAV | `../../../iq-file/synthetic_gjoevik_5stations_120s.wav` |
| JSON | 32 scheduled transmissions, 5 stations |
| Radius | max 1.91 km from 60.676217 N, 10.710316 E |
| Collisions | 0 (staggered beacon phases) |

Regenerate and validate:
```bash
python3 tools/generate_synthetic_iq.py
python3 tools/validate_synthetic_iq.py
```

Python validator: **28 unique / 32 expected frames**, all telemetry fields extracted.

## Full Telemetry Validation

See [`../../../TEST_RESULTS.md#full-telemetry-validation`](../../../TEST_RESULTS.md) for:

1. Complete per-frame decode log (position, course, speed, altitude, comment, weather, messages, QRV)
2. Per-station summary table
3. Message exchange log (4 APRS messages at T+15/40/71/95 s)
4. LG3ZZ-1 weather readings (5 beacons, varying wind/temp/hum/baro)
5. QRV detections (LA2YY-7 comment and message)
6. Movement traces for all moving stations
7. Collision report (0 generator overlaps)
8. Emulator replay on `emulator-5554` — 5 stations, 17 Java frames, incremental updates
9. Category pass/fail table

### Emulator verdict summary

| Category | Verdict |
|----------|---------|
| Position decoding (Python) | PASS |
| Course and speed extraction | PASS |
| Altitude extraction | PASS |
| Weather field extraction | PASS |
| APRS message decoding | PASS |
| QRV detection | PASS |
| Real-time movement on emulator | PARTIAL |
| Collision handling (no crash) | PASS |

## Unit tests

```bash
cd OsmAnd
./gradlew :OsmAnd:testNightlyFreeOpenglX86DebugUnitTest --tests "net.osmand.plus.plugins.aprs.*"
```

Tests: `AprsPacketParserTest`, `AprsHamlibBridgeTest`, `AprsSyntheticGroundTruthTest`
