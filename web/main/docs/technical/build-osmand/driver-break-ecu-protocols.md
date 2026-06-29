---
title: Driver Break — ECU Protocols
---

# Driver Break — ECU Protocols {#driver-break-ecu-protocols}

## Overview {#overview}

The Driver Break plugin reads **instantaneous fuel rate** and, when available, **fuel level** from vehicle ECUs. Three backends exist in the Navit reference design; the OsmAnd port implements **OBD-II** (via Vehicle Metrics / ELM327) and **J1939** (USB-CAN serial). **MegaSquirt** is stubbed and not yet available.

| Backend | Typical vehicle | Transport | OsmAnd status |
|---|---|---|---|
| OBD-II / ELM327 | Cars, light vehicles, some motorcycles | Bluetooth serial (Vehicle Metrics) | Implemented (`OBDBackend`) |
| J1939 | Trucks, heavy vehicles | USB-CAN @ 500000 bps | Implemented (`J1939Backend`) |
| MegaSquirt | Kit cars, performance builds | USB serial 115200 8N1 | Planned (`MegaSquirtBackend` stub) |

All backends feed the same logical fields used for adaptive learning and range estimation. **OBD-II and MegaSquirt share a serial port** in Navit; only one serial backend runs at a time.

## OBD-II (ELM327) {#obd-ii-elm327}

OsmAnd delegates OBD-II to the [Vehicle Metrics](../user/plugins/vehicle-metrics.md) plugin and `OBDDataComputer`. Connect an **ELM327-compatible** Bluetooth adapter (38400 or 115200 baud, 8N1) through Vehicle Metrics before enabling **Use ECU fuel data** in Driver Break.

### Supported PIDs

| PID | Name | Formula | Units | Notes |
|---|---|---|---|---|
| 0x2F | Fuel level | `value / 2.55` | % | Direct tank level read |
| 0x5E | Fuel rate | `(A×256+B) × 0.05` | L/h | Preferred instantaneous rate |
| 0x10 | MAF rate | `(A×256+B) / 100` | g/s | Fallback when 0x5E unsupported |
| 0x52 | Ethanol % | varies | % | Used for flex-fuel AFR selection when available |

### MAF-to-fuel-rate fallback

When PID 0x5E is not supported, fuel rate is estimated from mass air flow (Navit `obd_maf_to_fuel_rate`, documented in [formulas.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/formulas.rst)):

$$
\text{Fuel rate (L/h)} = \frac{\text{MAF (g/s)} \times 3600}{\text{AFR} \times \rho \times 1000}
$$

- **MAF** — PID 0x10 in g/s  
- **AFR** — air–fuel ratio by fuel type and ethanol content (for example ~14.7 for petrol)  
- **ρ** — fuel density in kg/L (for example ~0.745 for petrol)

`OBDBackend` uses `MAF_AFR_PETROL = 14.7` and `MAF_DENSITY_PETROL_KG_L = 0.745` when shared OBD layers do not supply a direct rate.

### Adapter types

Any **ELM327-compatible** USB or Bluetooth adapter supported by Vehicle Metrics. Ensure only one application owns the adapter at a time.

## J1939 (USB-CAN) {#j1939-socketcan}

The J1939 backend listens for fuel **PGNs** on a USB-CAN interface at **500000 bps** (OsmAnd `J1939Backend`; Navit reference uses SocketCAN `can0`, often configured at 250000 bps on some fleets—match your vehicle network).

### PGN table

Navit reference (`ecu_ports.rst`, `formulas.rst`) documents decimal PGNs **65266** and **65276**; SAE names and hex IDs used in OsmAnd:

| PGN (hex) | PGN (dec) | Name | SPN | Scale | Units |
|---|---|---|---|---|---|
| 0xFEEA | 65266 | Engine fuel rate | 183 | 0.05 per bit | L/h |
| 0xFEF4 | 65276 | Fuel level | 96 | 0.4 per bit | % (0–250) |

OsmAnd also decodes **0xF003** (EEC2) and **0xFEF2** (LFE) per SAE J1939-71 with the same SPN scaling (`J1939Backend.FUEL_RATE_SCALE_L_H`, `FUEL_LEVEL_SCALE_PERCENT`).

Fuel rate:

$$
\text{fuel\_rate\_L/h} = \text{raw\_SPN183} \times 0.05
$$

Fuel level percent:

$$
\text{fuel\_level\_\%} = \text{raw\_SPN96} \times 0.4
$$

Litres in tank (when capacity configured):

$$
\text{fuel\_current\_L} = \frac{\text{fuel\_level\_\%}}{100} \times \text{tank\_capacity\_L}
$$

### USB-CAN adapters

USB serial CAN adapters compatible with `usb-serial-for-android` and configured for **500000 bps** (implementation constant in `J1939Backend`).

## MegaSquirt (planned) {#megasquirt-planned}

**Not yet implemented** in OsmAnd. The Navit backend opens serial at **115200 8N1**, sends realtime command `A`, and computes fuel rate from injector pulse width, RPM, cylinder count, and configured injector flow (cc/min):

$$
\text{fuel\_rate\_L/h} = \frac{\text{pw\_ms} \times \text{rpm} \times n_{cyl} \times \text{flow\_cc/min}}{2\,000\,000}
$$

See [aftermarket_ecus.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/aftermarket_ecus.rst) and `MegaSquirtBackend.java` stub.

## Adaptive fuel learning {#adaptive-fuel-learning}

When **Adaptive fuel learning** is enabled, the plugin appends rows to `fuel_samples` (timestamp, distance_m, fuel_litres, mode) and updates a learned **litres per kilometre** rate using an exponential moving average:

$$
\text{rate}_{new} = \alpha \cdot \text{sample} + (1 - \alpha) \cdot \text{rate}_{old}
$$

- **α** — `AdaptiveFuelLearner.EMA_ALPHA` (= **0.1**)
- **sample** — fuel_litres / (distance_m / 1000) for each valid segment
- Persisted key — `adaptive_learned_lkm` in plugin SQLite config

When disabled, no new samples are recorded. Energy-aware routing still works from the kinematic model without ECU or learned fuel data.

## eco_mode_fuel_enabled {#eco-mode-fuel-enabled}

Navit exposes boolean **`eco_mode_fuel_enabled`**: true when (1) an ECU backend is active (OBD-II, J1939, or MegaSquirt), or (2) adaptive fuel learning is enabled—so external tools know live or learned fuel data is in use for eco behaviour.

OsmAnd maps this concept to **Use ECU fuel data** and **Adaptive fuel learning** toggles on the General settings tab; there is no separate DBus attribute in the Android app.

## Test coverage {#test-coverage}

| Component | OsmAnd test class | Navit reference executable |
|---|---|---|
| OBD MAF / J1939 scaling | `J1939BackendTest` | `test_driver_break_obd_j1939` |
| MegaSquirt formula | — (backend stub) | `test_driver_break_megasquirt` |
| Adaptive EMA | `AdaptiveFuelLearnerTest` | (database / config tests) |

See [Driver Break tests](./driver-break-tests.md) for build and CI results.
