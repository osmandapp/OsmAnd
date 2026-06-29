---
sidebar_position: 6
title: Driver Break
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import AndroidStore from '@site/src/components/buttons/AndroidStore.mdx';
import AppleStore from '@site/src/components/buttons/AppleStore.mdx';
import LinksTelegram from '@site/src/components/_linksTelegram.mdx';
import InfoAndroidOnly from '@site/src/components/_infoAndroidOnly.mdx';

<InfoAndroidOnly />

# Driver Break {#driver-break}

## Overview {#overview}

The **Driver Break** plugin helps you plan and take rest stops while travelling by car, truck, motorcycle, on foot, or by bicycle. It tracks driving time or distance according to your travel mode, reminds you when a break is due, and can search OpenStreetMap for nearby points of interest (POIs) such as water sources, cabins, cafes, and fuel stations. Where applicable, it validates hiking and cycling routes and can compare calculated routes for predicted energy use.

When a compatible vehicle ECU is connected, the plugin reads live fuel consumption to refine range estimates and optional eco-weighted route comparison. The plugin is available on **Android** and does not require an OsmAnd Pro subscription.

## Required Setup {#required-setup}

1. Enable the Driver Break plugin via *Menu → Plugins → Driver Break*.
2. Open *Menu → Plugins → Driver Break → Settings* and select a **travel mode** on the General tab.
3. On the **Intervals** tab, configure break thresholds for that mode (hours for car/truck/motorcycle, kilometres for hiking/cycling).
4. Add the **Next break** widget via *Menu → Configure Screen → Other → Driver Break*.
5. (Optional) Enable **Use ECU fuel data** on the General tab and connect an OBD-II adapter through the [Vehicle Metrics](./vehicle-metrics.md) plugin.
6. (Optional) On the **Elevation** tab, download SRTM elevation tiles for your region when prompted, or when planning routes that cross areas without cached tiles.

## Travel Modes {#travel-modes}

### Car {#travel-mode-car}

Car mode tracks **driving time**. You can set a soft limit (default 7 hours), a maximum driving period (default 10 hours), a break interval (default every 4 hours), and break duration (default 30 minutes). Rest stops are placed along the driven route on suitable road types after a minimum distance; nearby POIs include cafes, restaurants, museums, viewpoints, and picnic areas. Enable **Water POIs** on the Overnight tab for remote or hot-climate trips to include drinking water, fountains, and springs at stops.

### Truck {#travel-mode-truck}

Truck mode applies **EU Regulation EC 561/2006**-style limits by default: mandatory break after 4 hours of driving, 45-minute break duration, and a maximum of 9 hours daily driving. Rest-stop search and POI types are similar to car mode. Water POIs for remote or arid conditions use the same Overnight setting as car.

:::tip
For truck drivers, default interval values align with EU Regulation EC 561/2006 break requirements. Adjust them only if your jurisdiction or employer rules differ.
:::

### Hiking {#travel-mode-hiking}

Hiking mode tracks **distance**. Default main rest intervals follow the historical Scandinavian **rast** unit (11.295 km main stage, 2.275 km alternative stage, 40 km suggested maximum per day); see [Rast and vei](#rast-and-vei) for the historical basis. Rest stops include water (drinking water, fountain, spring) and cabins or huts (wilderness hut, alpine hut, hostel, camping). Optional SRTM elevation supports energy-aware route comparison when enabled.

### Cycling {#travel-mode-cycling}

Cycling mode uses the same **rast/vei** concept scaled for bicycles: default 28.24 km main stage, 5.69 km alternative stage, and 100 km suggested maximum per day (see [Rast and vei](#rast-and-vei)). Water and cabin POIs match hiking. Charging stations, bicycle repair, and related service POIs are searched at rest stops when present in map data or via Overpass fallback.

### Motorcycle {#travel-mode-motorcycle}

Motorcycle mode tracks **riding time** with defaults of a 2-hour soft limit, mandatory break after 3.5 hours, and 15–30 minute break duration (configurable in minutes in OsmAnd). POI search matches car plus `amenity=motorcycle_repair` and `shop=motorcycle` when mapped. Fuel data uses OBD-II on Euro 4+ bikes where supported; otherwise adaptive fuel learning or manual values apply. Energy routing uses configurable rider-plus-bike mass (default total mass in settings should reflect roughly 250 kg rider and gear unless you change it).

:::note
**Adventure/dual-sport terrain** (where implemented in routing profiles) is limited to legally accessible ways: the plugin must not route across uncultivated land, unmapped terrain, or ways tagged `access=private` or `access=no`. Only `highway=track` and similar ways with explicit `access=yes`, `access=permissive`, or `motorcycle=yes` / `designated` / `permissive` are considered. Off-road motor traffic on uncultivated land is prohibited without a permit in many countries—including Norway (Motorferdselloven, 1977), Sweden (Terrängkörningslagen, 1975:1313), Finland (Maastoliikennelaki, 1710/1995), and similar laws elsewhere.
:::

## Rast and vei (historical basis for hiking and cycling defaults) {#rast-and-vei}

The suggested default rest intervals for hiking (11.295 km main, 2.275 km alternative; 40 km daily max) and for cycling (28.24 km main, 5.69 km alternative; 100 km daily max) are inspired by the old Scandinavian units of length **rast** and **vei**. For cycling, the same rast/vei concept is used with distances scaled up. A "rast" was the distance one traveled on foot before needing a rest ("rast," "pause," or the like); it corresponded to a **mil** and was often tied to the length of the ell. The distance varied by region and over time. In the 900s a rast was about 192 stone throws, divided into four quarters ("fjerdingvei"), and corresponded to roughly 9,100.8 meters; in the 12th century it was expressed as 16,000 ells (four quarters of 8,000 feet) but remained in the same order of magnitude.

A "dagsvei" (day's way/journey) was a traditional Scandinavian unit meaning roughly how far you could walk in a day, commonly reckoned at about 40 km.

The plugin's hiking and cycling interval defaults follow from these historical rast-based distances.

## Rest Stop Suggestions {#rest-stop-suggestions}

The plugin suggests stops based on your travel mode:

- **Hiking and cycling** — Rest positions are placed at configured **distance intervals** along the route (main and alternative stage distances, plus a daily maximum).
- **Car, truck, and motorcycle** — The plugin walks the **route geometry**, accumulates segment length, and after a minimum distance (about 5 km) looks for suitable highway types (for example unclassified, service, track, tertiary). Each candidate is checked against building and glacier distance rules before POIs are attached.

**POI search** prefers **map-based** OsmAnd search within configurable radii (defaults: water 2 km, cabins 5 km, general POIs 15 km, network huts up to 25 km). If map data is insufficient, **Overpass API** fallback queries run in the background. **Hiking/pilgrimage priority** adds places of worship within the general POI radius on hiking and cycling stages.

### Networks and priorities {#networks-and-priorities}

- **DNT/network priority** — Optional priority for network huts (e.g. Norwegian Trekking Association, DNT) with configurable hut search radius. Enable this on the **Overnight** tab. Set the network hut search radius according to typical spacing (see below); in remote areas consider raising it toward the upper range to include the next cabin.

**Networked cabin spacing** (nearest-neighbor distances, for setting search radius):

- **Norway (DNT)** — OpenStreetMap relation 1110420 (DNT cabins): 449 huts; average 10.56 km, median 8.83 km, max 100.45 km.
- **Sweden** — Overpass API (`tourism=wilderness_hut`, `tourism=alpine_hut` in Sweden): 439 huts total; average 12.31 km, median 8.24 km, max 83.85 km. STF (Svenska Turistföreningen) network only (42 huts): average 14.47 km, median 11.50 km, max 83.85 km.
- **Finland** — Overpass API (`tourism=wilderness_hut`, `tourism=alpine_hut` in Finland): 324 huts total; average 11.72 km, median 6.68 km, max 64.32 km. Metsähallitus network only (108 huts): average 16.05 km, median 5.31 km, max 247 km (remote areas).
- **Germany** — Overpass API (`tourism=wilderness_hut`, `tourism=alpine_hut` in Germany): 261 huts total; average 12.98 km, median 9.72 km, max 119.76 km. DAV (Deutscher Alpenverein) network: 22 huts in sample.
- **Switzerland** — Overpass API (`tourism=wilderness_hut`, `tourism=alpine_hut` in Switzerland): 328 huts total; average 4.40 km, median 3.82 km, max 23.70 km. SAC/CAS (Schweizer Alpen-Club) and similar: 66 huts in sample; denser spacing in the Alps.
- **Austria** — Overpass API (`tourism=wilderness_hut`, `tourism=alpine_hut` in Austria): 330 huts total; average 5.30 km, median 3.56 km, max 102.51 km. OeAV (Österreichischer Alpenverein) and similar: 22 huts in sample; denser spacing in the Alps.

**Open / non-networked huts** (no `network` tag, not operated by DNT/STF/DAV/SAC/OeAV/Metsähallitus etc.) can also show somewhat regular spacing in the same countries. Use the same Overpass tag set and exclude networked operators; nearest-neighbor distances (for setting cabin search radius) in the samples:

- **Germany** — 235 huts; average 14.29 km, median 10.22 km, max 119.76 km.
- **Switzerland** — 261 huts; average 4.93 km, median 4.03 km, max 23.70 km.
- **Austria** — 287 huts; average 5.71 km, median 3.65 km, max 102.51 km.
- **Sweden** — 395 huts; average 12.45 km, median 7.85 km, max 64.86 km.
- **Finland** — 206 huts; average 17.00 km, median 12.33 km, max 75.75 km.

Set **cabin search radius** in the typical-to-max range for the region (e.g. 5–15 km in the Alps, 10–20 km in Scandinavia/Germany/Finland for open huts). Use at least the typical spacing (e.g. 10–12 km) so nearby huts are found; in remote areas consider raising the radius toward the max values.

**Distance from buildings** — For overnight or camping-style stops (for example under right-to-roam rules such as Norwegian *allemannsretten*), candidates closer than the configured minimum (default 150 m) to buildings or dwellings are rejected.

**Distance from glaciers** — Nightly camping positions closer than the configured minimum (default 300 m) to glaciers are rejected unless a camping building is present at the site.

:::caution Water sources and filtration
When you use water from natural sources (streams, lakes, or springs) suggested as rest-stop POIs, **always treat or filter water before drinking**. Use a portable filter with:

- A hollow-fibre or ceramic membrane rated at **0.1 micron or smaller** to remove bacteria, protozoa, and parasites such as Giardia and Cryptosporidium.
- An **activated carbon** stage (preferably carbon block) to reduce pesticides, herbicides, heavy metals, chlorine, and improve taste and odour. Prefer filters certified to **NSF/ANSI 42 and 53**.

Standard filters do **not** remove viruses. In remote wilderness (Norway, Sweden, Finland) viral risk is generally low, but near settlements, farms, or grazing land consider a **0.02 micron** filter or chemical treatment (iodine tablets, chlorine dioxide) as a second stage.

Water inside Norwegian national parks and nature reserves is often considered safe to drink directly because of protection from agricultural runoff and industry—but collect from **fast-moving or high-altitude** sources rather than slow or stagnant water, and avoid points immediately downstream of trails, campsites, or huts.

Even POIs tagged `amenity=drinking_water` on OpenStreetMap may be outdated; treat remote sources with caution.
:::

## Energy-Efficient Routing {#energy-efficient-routing}

**Energy-aware routing** (kinetic routing) compares calculated route variants using a **kinematic energy model**: rolling resistance, aerodynamic drag, grade (from elevation data), and downhill recuperation contribute to a cost per segment. After a route is calculated, OsmAnd can suggest an alternative that uses less predicted energy if it is not much longer (default thresholds: at least 5% energy saving with at most 20% distance increase).

Enable **Energy-aware routing** on the General tab. Set **total mass**, **drag coefficient Cd**, and **frontal area** on the Aerodynamics tab so they match your current mode (car laden weight, rider and bicycle, and so on). The plugin stores **one** mass/Cd/area triple at a time; changing travel mode does not load separate presets automatically.

:::note
Energy-based routing requires a **terrain profile** (per-segment elevation) along the route. Without downloaded SRTM tiles, grade terms are not meaningful and the model cannot reliably prefer flatter paths. Download elevation data for your regions on the Elevation tab or accept the prompt when enabling the plugin or when a route crosses an uncovered area.
:::

For formulas and implementation details, see the [Driver Break energy model technical doc](../../technical/build-osmand/driver-break-energy-model.md).

## Elevation Data (SRTM) {#elevation-data-srtm}

Elevation tiles are **1° × 1°** cells stored locally under app data. Downloads try sources **in order**:

1. **Copernicus DEM GLO-30** (GeoTIFF from AWS S3)
2. **Viewfinder Panoramas dem3** (HGT)
3. **NASA SRTMGL1** (HGT fallback)

When you enable the plugin or plan a route into a region without tiles, OsmAnd prompts you to download missing tiles for that **county or country** (from OsmAnd region boundaries). Declined regions are remembered; disabling the plugin **does not delete** cached tiles.

**Void / missing samples** — Source DEM uses **-32768** as void; the plugin treats missing tiles or void pixels as unknown elevation and skips grade discrimination for those segments.

<!-- SCREENSHOT: Menu → Plugins → Driver Break → Settings → Elevation tab -->
![Driver Break elevation settings](@site/static/img/plugins/driver-break/settings_elevation.png)

## ECU Fuel Data {#ecu-fuel-data}

### OBD-II {#ecu-obd-ii}

For cars and light vehicles, the plugin reads **fuel level** (PID 0x2F) and **fuel rate** (PID 0x5E) through OsmAnd’s [Vehicle Metrics](./vehicle-metrics.md) OBD-II connection (ELM327-compatible Bluetooth adapters). If PID 0x5E is unavailable, fuel rate may be estimated from **MAF** (PID 0x10) using stoichiometric air–fuel ratio and fuel density for your fuel type.

### J1939 {#ecu-j1939}

For trucks, a **USB-CAN** adapter at **500000 bps** can decode **PGN 0xF003** (engine fuel rate, SPN 183, 0.05 L/h per bit) and **PGN 0xFEF2** (fuel level, SPN 96, 0.4% per bit). Navit reference also documents PGN 65266 / 65276 with the same scaling factors.

### MegaSquirt {#ecu-megasquirt}

Support for **MegaSquirt** (MS1, MS2, MS3, MS3-Pro, MicroSquirt) is **planned** but **not yet available** in this OsmAnd release. When implemented, it will read injector pulse width and RPM over serial (115200 8N1) similar to the Navit reference backend.

### Adaptive fuel learner {#adaptive-fuel-learning}

With **Adaptive fuel learner** enabled on the General settings tab, the plugin records fuel and distance samples in SQLite and maintains a learned **litres per kilometre** rate using an exponential moving average. This refines estimates when no live ECU is connected. Turn the toggle off to stop recording new samples.

For PID tables and protocol detail, see [Driver Break ECU protocols](../../technical/build-osmand/driver-break-ecu-protocols.md).

## Widget — Next Break {#widget--next-break}

Add the widget from *Menu → Configure Screen → Other → Driver Break → Next break*. It shows **remaining time** until the next recommended break for car, truck, and motorcycle, or **remaining distance** for hiking and cycling. The value updates as you move and when break settings or travel mode change.

<!-- SCREENSHOT: Map screen with Driver Break Next break widget visible -->
![Driver Break next break widget](@site/static/img/plugins/driver-break/widget_next_break.png)

## Settings Reference {#settings-reference}

Open *Menu → Plugins → Driver Break → Settings*.

### General {#settings-general}

| Setting | Description |
|---|---|
| **Travel mode** | Selects Car, Truck, Hiking, Cycling, or Motorcycle. |
| **Energy-aware routing** | After route calculation, compare variants and suggest lower-energy alternatives when thresholds are met. |
| **Use ECU fuel data** | Use live OBD-II or J1939 fuel rate and level when connected (requires Vehicle Metrics for OBD-II). |
| **Adaptive fuel learner** | Enable adaptive fuel learning: record fuel/distance samples and maintain a learned consumption rate (litres per km). Off by default. |

### Intervals {#settings-intervals}

| Setting | Description |
|---|---|
| **Car soft limit (hours)** | Hours before a soft break reminder (default 7; configurable). |
| **Car maximum driving (hours)** | Maximum continuous driving period (default 10). |
| **Car break interval (hours)** | Interval between suggested breaks (default 4; often 4–4.5 h). |
| **Car break duration (minutes)** | Suggested break length (default 30; often 15–45 min). |
| **Truck mandatory break (hours)** | Driving time before a mandatory break (default 4; EU 561/2006 uses 4.5 h max continuous driving—adjust to match your rules). |
| **Truck break duration (minutes)** | Mandatory break length (default 45). |
| **Truck max daily driving (hours)** | Daily driving cap (default 9). |
| **Hiking main stage (km)** | Main rest interval distance (default 11.295 km). |
| **Hiking alternative rests** | Toggle shorter alternative-stage rests (default 2.275 km). |
| **Hiking alternative stage (km)** | Alternative rest interval when enabled. |
| **Hiking max daily distance (km)** | Suggested daily maximum (default 40 km). |
| **Cycling main stage (km)** | Main rest interval distance (default 28.24 km). |
| **Cycling alternative rests** | Toggle shorter alternative-stage rests (default 5.69 km). |
| **Cycling alternative stage (km)** | Alternative rest interval when enabled. |
| **Cycling max daily distance (km)** | Suggested daily maximum (default 100 km). |
| **Motorcycle soft limit (minutes)** | Soft riding limit (default 120). |
| **Motorcycle mandatory break (minutes)** | Mandatory break after this riding time (default 210). |
| **Motorcycle break duration (minutes)** | Suggested break length (default 20). |
| **Motorcycle max daily riding (hours)** | Suggested maximum riding time per day (default 8). |

Rest stops along a calculated route appear as Driver Break markers on the map when **Travel mode** is set to hiking, cycling, or another supported mode.

### Overnight {#settings-overnight}

| Setting | Description |
|---|---|
| **Water POIs** | Include drinking water, fountains, and springs at car/truck/motorcycle stops in remote or hot conditions (also affects hiking water search labelling in this build). |
| **DNT/network hut priority** | Prefer network-operated cabins (DNT, STF, DAV, SAC, and similar) when sorting hut results. See [Networks and priorities](#networks-and-priorities) for typical hut spacing when choosing **Cabin search radius**. |
| **General POI search radius (m)** | Radius for cafes, viewpoints, worship, and general amenities (default 15000). |
| **Water POI radius (m)** | Radius for water POIs (default 2000). |
| **Cabin search radius (m)** | Radius for huts and cabins (default 5000). |
| **Minimum distance from buildings (m)** | Camping/overnight clearance from buildings (default 150). |
| **Minimum distance from glaciers (m)** | Overnight clearance from glaciers (default 300). |

### Aerodynamics {#settings-aerodynamics}

| Setting | Description |
|---|---|
| **Total mass (kg)** | Mass for energy model: vehicle, cargo, rider, and gear (**1–50000** kg). |
| **Drag coefficient Cd** | Aerodynamic drag coefficient, dimensionless (**0.01–1.5**). |
| **Frontal area (m²)** | Projected frontal area for drag (**0.05–20** m²). |

### Elevation {#settings-elevation}

| Setting | Description |
|---|---|
| **Cached elevation tiles** | Read-only count of local HGT and GeoTIFF tiles. |
| **Download tile at current location** | Queues download of the 1° tile at your current map position or GPS location. |
