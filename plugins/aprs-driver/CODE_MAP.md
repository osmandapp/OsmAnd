# aprs-driver Code Map

Branch: `aprs-driver` (based on `aprs-core`)  
Package: `net.osmand.plus.plugins.aprsdriver`  
Source root: `OsmAnd/plugins/aprs-driver/`

## Dependencies

Requires `aprs-core` (`AprsPlugin`, `AprsMessageListener` / `ingestAx25Frame()`).

## Class diagram

```
AprsSdrPlugin
  ├── AprsSdrDriver (USB Host API)
  └── AprsSdrThread
        └── AprsSdrDsp (FM + Bell202 + AX.25)
              └── AprsPlugin.ingestAx25Frame()
```

## File index

| File | Purpose |
|------|---------|
| `src/.../AprsSdrPlugin.java` | Plugin registration; checks aprs-core presence |
| `src/.../AprsSdrDriver.java` | VID/PID match, USB permission, bulk IN endpoint |
| `src/.../AprsSdrThread.java` | USB loop or WAV replay thread |
| `src/.../AprsSdrDsp.java` | Full DSP pipeline + AX.25 FCS |
| `src/.../AprsSdrReplayReceiver.java` | Debug broadcast for IQ replay |

## DSP pipeline (`AprsSdrDsp.processInterleavedIq`)

1. DC block I/Q
2. Digital mixer (+100 kHz IF offset default)
3. Decimate to 48 kHz
4. FM discriminator
5. DC tracker on discriminator
6. Bell 202 demod (1200/2200 Hz)
7. Bit timing PLL
8. NRZI decode
9. Flag detect, bit destuff, FCS verify

## USB VID/PID table (`AprsSdrDriver`)

| Family | VID | PID |
|--------|-----|-----|
| RTL-SDR Blog V3/V4, Nooelec, Generic | 0x0BDA | 0x2831, 0x2832, 0x2838 |

## Build integration

Sources compile into `:OsmAnd` via extra source sets in `OsmAnd/OsmAnd/build-common.gradle`:

- `plugins/aprs-driver/src`
- `plugins/aprs-driver/res`

Broadcast receiver remains declared in `OsmAnd/OsmAnd/AndroidManifest.xml`.

## Tests

`test/java/net/osmand/plus/plugins/aprsdriver/AprsSdrDspTest.java`

## See also

- `PHASE2_RTLSDR_RESEARCH.md` — external SDR Driver intent option
- `../aprs-core/CODE_MAP.md` — core plugin
