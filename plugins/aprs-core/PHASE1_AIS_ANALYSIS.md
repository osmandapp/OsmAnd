# Phase 1 — AIS Plugin Architecture Analysis

Blueprint for the APRS plugin. All paths relative to `OsmAnd/OsmAnd/` unless noted.

## Package structure

```
net.osmand.plus.plugins.aistracker/          (Android UI + map layer)
net.osmand.shared.aistracker/                (Kotlin shared: parsing, data model)
```

| Class | Path | Role |
|-------|------|------|
| `AisTrackerPlugin` | `src/.../plugins/aistracker/AisTrackerPlugin.java` | Plugin entry point, settings, lifecycle |
| `AisDataManager` | inner class in `AisTrackerPlugin.java` | Station storage, expiry, max count |
| `AisTrackerLayer` | `src/.../plugins/aistracker/AisTrackerLayer.java` | Map layer, render throttle, touch selection |
| `AisObjectDrawable` | `src/.../plugins/aistracker/AisObjectDrawable.java` | Per-vessel markers, `setPosition()` |
| `AisMessageListener` | `OsmAnd-shared/.../aistracker/AisMessageListener.kt` | UDP/TCP NMEA network I/O |
| `AisMessageSimulationListener` | `src/.../plugins/aistracker/AisMessageSimulationListener.java` | File replay for testing |
| `AisObject` | `OsmAnd-shared/.../aistracker/AisObject.kt` | Vessel data model |
| `AisImagesCache` | `src/.../plugins/aistracker/AisImagesCache.java` | Bitmap cache keyed by drawable + scale |
| `AisTrackerSettingsFragment` | `src/.../plugins/aistracker/AisTrackerSettingsFragment.java` | Settings UI |
| `AisObjectMenuController` | `src/.../plugins/aistracker/AisObjectMenuController.java` | Context menu on tap |
| `AisObjectMenuBuilder` | `src/.../plugins/aistracker/AisObjectMenuBuilder.java` | Menu layout |
| `AisLoadTask` | `src/.../plugins/aistracker/AisLoadTask.java` | Async load helper |
| `AisSimulationProvider` | `src/.../plugins/aistracker/AisSimulationProvider.java` | Dev simulation |

## Plugin registration

1. **Instantiation:** `PluginsHelper.initPlugins()` adds `new AisTrackerPlugin(app)` — `PluginsHelper.java:116`
2. **Plugin ID:** `AisTrackerPlugin.AISTRACKER_ID = "osmand.aistracker"` — `AisTrackerPlugin.java:58`
3. **Component ID:** `getComponentId1()` returns `"net.osmand.aistrackerPlugin"` — `AisTrackerPlugin.java:248`
4. **Market plugin:** `isMarketPlugin()` returns `true` — `AisTrackerPlugin.java:237`
5. **Settings screen:** `getSettingsScreenType()` returns `AIS_SETTINGS` — `AisTrackerPlugin.java:334`
6. **Settings enum:** `SettingsScreenType.AIS_SETTINGS` maps to `AisTrackerSettingsFragment` + `R.xml.ais_settings` — `SettingsScreenType.java:56`

## Lifecycle

| Event | Method | Behavior |
|-------|--------|----------|
| Enable | `AisTrackerPlugin.setEnabled(true)` | `startAisNetworkListener()` |
| Disable | `AisTrackerPlugin.disable()` | `stopAisListener()` |
| Map resume | `mapActivityResume()` | Restart stalled TCP, start listener, background service |
| Map pause | `mapActivityPause()` | Stop listener unless background receive enabled |
| Layer update | `updateLayers()` / `registerLayers()` | Create `AisTrackerLayer`, add at z-order 3.5 |

## AisTrackerLayer — create / update / destroy

**Create:** `onAisObjectReceived()` — if MMSI not in `objectDrawables`, new `AisObjectDrawable`; call `createAisRenderData()` once when OpenGL collections exist — `AisTrackerLayer.java:100-125`

**Update:** `drawable.set(ais)` then `drawable.updateAisRenderData()` — throttled via `shouldRefreshNativeRenderData()` at 1000 ms — `AisTrackerLayer.java:227-244`

**Destroy:** `onAisObjectRemoved()` — `clearAisRenderData()` removes markers/lines from collections — `AisTrackerLayer.java:128-136`, `AisObjectDrawable.java:515-527`

**Full rebuild:** `cleanupResources()` on zoom/text-scale change — `AisTrackerLayer.java:85-97`

## AisObjectDrawable — setPosition()

Native OpenGL path in `updateAisRenderData()`:

```java
activeMarker.setPosition(markerLocation);
restMarker.setPosition(markerLocation);
lostMarker.setPosition(markerLocation);
```

`AisObjectDrawable.java:476-485` — markers created once in `createAisRenderData()`, position updated in place.

Canvas fallback in `draw()` uses lat/lon to pixel conversion each frame — `AisObjectDrawable.java:331-381`

## AisMessageListener — network and parsing

| Constructor | Transport |
|-------------|-----------|
| `(dataListener, udpPort)` | UDP bind on port |
| `(dataListener, serverIp, serverPort)` | TCP connect with 10 s reconnect |
| protected `(dataListener)` | Base for simulation |

Flow: `processLine()` -> `SentenceFactory` -> `AISSentence` -> type-specific listeners -> `handleAisMessage()` -> `dataListener.onAisObjectReceived()` — `AisMessageListener.kt:117-137`, `172+`

Stop: `stopListener()` cancels coroutine scope — `AisMessageListener.kt:157-166`

## AisDataManager — storage and ageing

- **Storage:** `Map<Integer, AisObject>` keyed by MMSI — `AisTrackerPlugin.java:118`
- **Update:** `onAisObjectReceived()` merge or insert — `AisTrackerPlugin.java:164-176`
- **Max count:** 200 objects; oldest removed — `AisTrackerPlugin.java:172-173`, `194-210`
- **Expiry timer:** every 30 s (initial 20 s delay), removes objects where `obj.isLost(maxAgeMinutes)` — `AisTrackerPlugin.java:127-136`, `183-192`
- **Lost timeout prefs:** ship lost 4 min default, object lost 7 min default — `AisTrackerPlugin.java:81-84`

## Custom icons per vessel type

`AisObjectDrawable.selectBitmap(AisObjType)` maps type to `R.drawable.mm_ais_*` — `AisObjectDrawable.java:104-115`

`selectColor(AisObjType)` tints bitmap via `LightingColorFilter` — `AisObjectDrawable.java:117-131`

Bitmaps loaded via `AisImagesCache.getBitmap(drawableId)` with text-scale cache — `AisImagesCache.java:26-38`

No per-vessel custom images; fixed drawable set per AIS object class.

## Render refresh throttle

```java
private static final long AIS_RENDER_REFRESH_INTERVAL_MS = 1000L;
```

`AisTrackerLayer.shouldRefreshNativeRenderData()` — refresh when zoom changes OR elapsed >= 1000 ms — `AisTrackerLayer.java:51`, `227-234`

Global symbols interval (all layers): `MapViewWithLayers.SYMBOLS_UPDATE_INTERVAL = 2000` ms.

## Settings, menu, UI

| Item | Location |
|------|----------|
| Preferences XML | `res/xml/ais_settings.xml` |
| Settings fragment | `AisTrackerSettingsFragment.java` |
| Strings | `res/values/strings.xml` (`plugin_ais_tracker_*`, `ais_*`) |
| Plugin logo | `getLogoResourceId()` -> `R.drawable.mm_sport_sailing` |
| Plugin asset image | `R.drawable.ais_map` |
| Default app mode | `ApplicationMode.BOAT` |
| Renderer | `RendererRegistry.NAUTICAL_RENDER` |
| Background service | `NavigationService.USED_BY_AIS` + notification type `AIS` |
| Context menu | `AisObjectMenuController` via `IContextMenuProvider` on layer |
| Dev simulation | `DevelopmentSettingsFragment` + `AisLoadTask` |

## APRS mapping

| AIS component | APRS equivalent |
|---------------|-----------------|
| `AisTrackerPlugin` | `AprsPlugin` |
| `AisDataManager` | `AprsDataManager` |
| `AisTrackerLayer` | `AprsLayer` |
| `AisObjectDrawable` | `AprsObjectDrawable` |
| `AisMessageListener` | `AprsMessageListener` (interface, AX.25 frames) |
| `AisObject` | `AprsStation` |
| `AisImagesCache` + type icons | `AprsSymbolResolver` + hessu/aprs-symbols |
| `AisTrackerSettingsFragment` | `AprsSettingsFragment` |
