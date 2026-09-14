# aprs-core Code Map

Branch: `aprs-core`  
Package: `net.osmand.plus.plugins.aprs`  
Source root: `OsmAnd/plugins/aprs-core/`

## Class diagram (logical)

```
AprsPlugin
  ├── AprsDataManager (implements AprsMessageListener)
  │     └── AprsPacketParser
  ├── AprsLayer (implements StationListener, IContextMenuProvider)
  │     └── AprsObjectDrawable (per station)
  │           └── AprsSymbolResolver
  ├── AprsHamlibBridge
  └── AprsSettingsFragment

AprsMessageListener (interface) <-- aprs-driver / KISS / test harness
AprsStation, AprsWeather (data model)
```

## File index

| File | Purpose |
|------|---------|
| `src/.../AprsPlugin.java` | Plugin registration, prefs, layer lifecycle |
| `src/.../AprsDataManager.java` | Station store, radius filter, expiry timer |
| `src/.../AprsPacketParser.java` | AX.25 + APRS info field decode, QRV regex |
| `src/.../AprsMessageListener.java` | External frame source interface |
| `src/.../AprsLayer.java` | Map layer, 1000 ms render throttle, `setPosition()` |
| `src/.../AprsObjectDrawable.java` | Per-station MapMarker |
| `src/.../AprsSymbolResolver.java` | hessu/aprs-symbols sprite sheet lookup |
| `src/.../AprsHamlibBridge.java` | rigctld TCP `F <hz>\n` |
| `src/.../AprsStation.java` | Station data model |
| `src/.../AprsWeather.java` | Weather sub-record |
| `src/.../AprsSettingsFragment.java` | Settings UI |

## Assets

`assets/aprs-symbols/` (bundled into the main APK via `:OsmAnd` sourceSets)
- `aprs-symbols-48-0.png` — primary table `/`
- `aprs-symbols-48-1.png` — alternate table `\`
- `aprs-symbols-48-2.png` — overlay characters

## Resources

- `res/values/aprs_strings.xml` — plugin and settings strings
- `res/xml/aprs_settings.xml` — preference screen
- `res/drawable/ic_plugin_aprs.xml` — plugin icon (shared with aprs-driver)

## Build integration

Sources compile into `:OsmAnd` via extra source sets in `OsmAnd/OsmAnd/build-common.gradle`:

- `plugins/aprs-core/src`
- `plugins/aprs-core/res`
- `plugins/aprs-core/assets`

## Integration points (main app)

| Location | Change |
|----------|--------|
| `OsmAnd/src/.../PluginsHelper.java` | `new AprsPlugin(app)` |
| `OsmAnd/src/.../SettingsScreenType.java` | `APRS_SETTINGS` |

## Tests

`test/java/net/osmand/plus/plugins/aprs/`
- `AprsPacketParserTest.java`
- `AprsHamlibBridgeTest.java`
- `AprsSyntheticGroundTruthTest.java`

## External API

```java
AprsPlugin plugin = PluginsHelper.getPlugin(AprsPlugin.class);
plugin.ingestAx25Frame(ax25FrameBytes);
```

Driver branch calls `ingestAx25Frame()` after DSP decode.
