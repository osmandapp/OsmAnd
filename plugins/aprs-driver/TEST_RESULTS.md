# aprs-driver — Test Results Summary

Full report: [`../../../TEST_RESULTS.md`](../../../TEST_RESULTS.md)

## Phase 9 highlights

- Synthetic IQ pushed to emulator `/sdcard/Download/`
- Full DSP decode from WAV: **PASS** (17/26 frames, phase=3, 5 stations on emulator)
- USB VID/PID + FCS + destuff unit tests: PASS

## Unit tests

```bash
./gradlew :OsmAnd:testNightlyFreeOpenglX86DebugUnitTest --tests "net.osmand.plus.plugins.aprsdriver.*"
```

See `OsmAnd/plugins/aprs-driver/test/java/net/osmand/plus/plugins/aprsdriver/AprsSdrDspTest.java`
