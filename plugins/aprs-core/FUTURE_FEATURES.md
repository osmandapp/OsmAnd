# APRS Plugin — Future Features & Extension Points

This document describes features that are **not yet implemented** but are
architecturally anticipated. The plugin is designed so these can be added
without modifying the core APRS data pipeline.

---

## 1. Message Display Framework

### Design intent

`AprsPacketParser` already extracts and stores the following fields on every
station record:

| Field | Source | Status |
|---|---|---|
| `comment` | All position packets | Parsed, stored |
| `messageText` | `:` data type packets | Parsed, stored |
| `qrvFrequencyMhz` | Comment/message regex | Parsed, stored |
| `weather` | Weather packets | Parsed, stored |

The station record therefore already carries all raw material needed for
message display. What is not yet implemented is a **display layer** that
renders this information on or alongside the map marker.

### How to add a new message type display

Each message type should be implemented as a self-contained handler that:

1. Receives the decoded `AprsStation` record
2. Checks whether the relevant field is populated (non-null / non-zero)
3. Renders or forwards the information independently of the map marker itself

The recommended pattern is an `AprsMessageHandler` interface:

```java
public interface AprsMessageHandler {
    boolean canHandle(AprsStation station);
    void handle(AprsStation station, Context context);
}
```

Handlers are registered with `AprsPlugin` at startup and called for every
station update. This keeps message handling decoupled from icon rendering.

---

## 2. QRV Message Display

### What QRV means

A station transmitting `QRV 144.800` in its comment or message field is
announcing: *"I am listening on 144.800 MHz."* This is standard amateur
radio Q-code usage. The frequency is always in MHz.

### Detection

Already implemented in `AprsPacketParser`:
```
Regex: (?i)\bQRV\s+([\d.]+)
Field: AprsStation.qrvFrequencyMhz (double, 0.0 if not present)
```

### Hamlib integration

Already implemented in `AprsHamlibBridge`: when a station with a valid
`qrvFrequencyMhz` is tapped, a `F <freq_hz>\n` command is sent to `rigctld`
on the configured host/port. This allows a connected radio to be tuned
to the announced frequency automatically.

### What remains to be implemented

**Map display:** A small frequency badge or annotation rendered near the
station icon when `qrvFrequencyMhz > 0`, for example:

```
[LB1XX-9]         ← callsign label (already implemented)
 144.800 MHz      ← QRV badge (not yet implemented)
```

**Implementation notes for a future contributor:**
- The badge should only appear at zoom levels where it will not cause
  clutter (suggest zoom ≥ 14)
- It should disappear after a configurable time (default: same as station
  expiry, 30 minutes) since QRV status is time-sensitive
- The text colour should distinguish it from the callsign label —
  suggest a different colour (e.g. cyan or amber) so it is immediately
  recognisable as a frequency annotation rather than an identifier
- Tapping the badge (not the icon) should trigger the hamlib tune command
  directly, without requiring the info panel to open

**Info panel:** The station info panel already has a placeholder for QRV
frequency. No additional work needed there.

---

## 3. Weather Along a Route

### Concept

APRS weather stations transmit real-time local conditions: wind direction,
wind speed, temperature, barometric pressure, humidity, and rainfall.
Because APRS has dense coverage in many regions, a route planned in OsmAnd
could be enriched with live weather conditions from stations situated along
or near the route corridor.

### What is already in place

- `AprsWeather` data class: all standard weather fields parsed and stored
- `AprsDataManager`: all active weather stations within 300 km are retained
  in memory with their coordinates
- Weather stations are flagged: `AprsStation.isWeatherStation == true`

### What remains to be implemented

**Route weather overlay:** A separate OsmAnd layer (not part of `AprsLayer`)
that:

1. Receives the current active route from OsmAnd's routing engine
2. Queries `AprsDataManager` for all weather stations within N km of the
   route polyline (suggested default: 10 km corridor)
3. Renders weather condition markers along the route at each station position
4. Updates as new weather packets are received

**Implementation notes for a future contributor:**
- This should be implemented as its own OsmAnd plugin layer, not bolted onto
  `AprsLayer` — separation of concerns keeps both layers maintainable
- Weather icons along the route should use a distinct visual style from APRS
  station icons to avoid confusion — consider OsmAnd's existing weather
  layer conventions if a weather plugin already exists
- Wind direction and speed are the most navigation-relevant fields for
  driving; temperature and pressure are secondary
- Data freshness must be shown — a weather reading more than 30 minutes old
  should be visually marked as stale
- This feature is most useful for longer routes (> 50 km) where weather
  may vary meaningfully along the path

### Relationship to other weather sources

APRS weather is a complement to, not a replacement for, internet-based weather
services. It is particularly valuable in areas with poor mobile data coverage
where internet weather is unavailable but local APRS digipeaters are active.
A future implementation could merge APRS weather data with other sources,
preferring the most recent reading at each geographic point.

---

## 4. General Extension Notes

Any future message type handler should follow the same pattern:

- **Parse at the packet level** in `AprsPacketParser` — extract the relevant
  field and store it on `AprsStation`. Do not embed display logic in the parser.
- **Register a handler** via the `AprsMessageHandler` interface — keep display
  and business logic out of the data layer.
- **Document the message format** — APRS comment fields are free-text and
  QRV-style conventions are informal. Any new pattern matched by regex should
  be documented here with the exact regex used and example packets.
- **Respect the 300 km / 30 minute bounds** — all handlers operate on the
  same bounded station dataset. Do not introduce separate caching.

---

## 5. Correcting Symbol Orientation

Some APRS symbol PNG assets from `hessu/aprs-symbols` may render at the wrong
angle for a given context — for example a vehicle icon pointing south instead
of north, or an aircraft icon that appears sideways. This section documents
how to correct orientation for any symbol without modifying the source asset.

### Where orientation is applied

`AprsObjectDrawable` applies rotation in two stages:

1. **Base rotation offset** — a per-symbol constant applied before any
   course/heading rotation. This corrects for the asset's own orientation
   relative to north. Defined in `SYMBOL_BASE_ROTATION` table (see below).
2. **Course rotation** — the station's reported course in degrees (0–359),
   applied on top of the base offset. Only applied when `course != -1`.

### The base rotation table

A static lookup table in `AprsObjectDrawable` maps symbol table + code pairs
to a base rotation offset in degrees:

```java
private static final Map<String, Float> SYMBOL_BASE_ROTATION = new HashMap<>();

static {
    // Primary table '/' symbols — hessu/aprs-symbols assets point north (0°) by default
    SYMBOL_BASE_ROTATION.put("/>" , 0f);  // Car — north-up in PNG; use course directly
    SYMBOL_BASE_ROTATION.put("/^" , 0f);   // Aircraft
    SYMBOL_BASE_ROTATION.put("/v" , 0f);   // Van
    // Alternate table '\' symbols
    SYMBOL_BASE_ROTATION.put("\\A", 0f);   // Example: symbol A, alternate table
    // Add further corrections here as needed
}

private float getBaseRotation(char symbolTable, char symbolCode) {
    String key = "" + symbolTable + symbolCode;
    return SYMBOL_BASE_ROTATION.getOrDefault(key, 0f);
}
```

### How to fix a specific symbol

If symbol `A` in the alternate table (or any other symbol) displays at the
wrong angle:

1. Identify the symbol table character (`/` or `\`) and symbol code (`A`)
2. Observe the rendered angle vs the expected angle
3. Calculate the correction: `correction = expected_angle - observed_angle`
4. Add or update the entry in `SYMBOL_BASE_ROTATION`:

```java
// Example: alternate table symbol A appears rotated 90° clockwise,
// needs 270° (or -90°) base offset to correct it
SYMBOL_BASE_ROTATION.put("\\A", 270f);
```

5. Rebuild and verify on the emulator

### Symbols that should never rotate

Stationary symbols (buildings, repeaters, digipeaters, weather stations, etc.)
should not have course rotation applied regardless of any course value in the
packet. A second lookup set identifies non-rotating symbols:

```java
private static final Set<String> NON_ROTATING_SYMBOLS = new HashSet<>(Arrays.asList(
    "/-",  // House
    "/r",  // Repeater
    "/#",  // Digipeater
    "/_",  // Weather station
    "/Y",  // Yacht (at anchor — stationary variant)
    "\\S"  // SKYWARN
    // Add further stationary symbols here
));

private boolean shouldRotate(char symbolTable, char symbolCode) {
    return !NON_ROTATING_SYMBOLS.contains("" + symbolTable + symbolCode)
           && course != -1;
}
```

### Adding a new correction

When a new symbol is found to display at the wrong angle:

1. Add the entry to `SYMBOL_BASE_ROTATION` in `AprsObjectDrawable`
2. Document it with a comment stating what the asset orientation is and
   what the correction achieves
3. Update this document with the symbol code and the offset applied

No other files need to change — the correction is entirely self-contained
within the base rotation table.

---

*This document should be updated whenever a new message type is added to the
parser, even if no display handler is implemented yet.*
