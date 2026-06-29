---
title: Driver Break — Energy Model
---

# Driver Break — Energy Model {#driver-break-energy-model}

## Overview {#overview}

The Driver Break plugin assigns a **predicted mechanical energy cost** (Joules) to each route segment when **energy-aware routing** is enabled. OsmAnd sums segment costs for each calculated route variant and may suggest an alternative with lower total cost if it saves enough energy without exceeding a distance penalty threshold.

The model is a port of Navit `driver_break_energy.c` (kinematic rolling resistance, speed-dependent drag with temperature and elevation corrections to the drag coefficient, gravitational work on grade, downhill recuperation, and optional standby power). It does **not** replace OsmAnd’s primary router; comparison runs **after** route calculation using segment geometry and SRTM elevation at endpoints.

## Parameters {#parameters}

| Parameter | Symbol | Unit | Valid range | SQLite / config key | Java access |
|---|---|---|---|---|---|
| Total mass | $m$ | kg | 1–50000 | `total_weight` | `EnergyParams.getTotalMassKg()` |
| Drag coefficient | $C_d$ | — | 0.01–1.5 | `energy_drag_cd` | `EnergyParams.getDragCd()` |
| Frontal area | $A$ | m² | 0.05–20 | `energy_frontal_area_sqm` | `EnergyParams.getFrontalAreaM2()` |
| Rolling resistance coeff. | $C_{rr}$ | — | fixed in code | — | `EnergyModel.CRR` (= 0.015) |
| Sea-level air density | $\rho_0$ | kg/m³ | fixed | — | `EnergyModel.AIR_DENSITY_SEA_LEVEL_KG_M3` (= 1.225) |
| Gravity | $g$ | m/s² | fixed | — | `EnergyModel.G_MS2` (= 9.81) |
| Recuperation efficiency | $\eta$ | 0–1 | 0–1 | — (default 0.7 in `EnergyParams.fromDriverBreakDefaults()`) | `EnergyParams.getRecuperationEfficiency()` |
| Standby / auxiliary power | $P_s$ | W | ≥ 0 | — (default 0) | `EnergyParams.getStandbyPowerW()` |
| Outside temperature | $T$ | °C | — | — (default 20) | `EnergyParams.getOutsideTempCelsius()` |
| Max speed cap | $v_{max}$ | m/s | — | — (default 80 km/h) | `EnergyParams.getMaxSpeedMs()` |

Defaults after a fresh install (`driver_break_config_default()` / `DriverBreakDatabase`) use **80 kg** mass, **Cd 0.30**, and **2.2 m²** frontal area—oriented toward a light passenger car order of magnitude until you configure values for your mode.

## Air density and drag coefficient adjustment {#air-density-correction}

Sea-level air resistance coefficient (before speed² term):

$$
k_{air} = \tfrac{1}{2}\,\rho_0\,C_d\,A
$$

Implemented as `EnergyModel.airCoefficientFromDrag()` using `AIR_DENSITY_SEA_LEVEL_KG_M3`, `dragCd`, and `frontalAreaM2`.

Per segment, Navit applies **temperature** and **elevation** multipliers to $k_{air}$ (port of `energy_calculate_segment`):

$$
k'_{air} = k_{air}\,\bigl(1 + t_{corr} - e_{corr}\bigr)
$$

where

$$
t_{corr} = (T_{ref} - T)\,k_T,\quad e_{corr} = k_H\,(h - h_{ref})
$$

with $T_{ref} = 20\,°C$ (`REFERENCE_OUTSIDE_TEMP_C`), $k_T = 0.0035$ (`TEMP_CORRECTION_PER_DEG_C`), $k_H = 0.0001375$ (`ELEVATION_PRESSURE_FACTOR`), and $h_{ref} = 100\,m$ (`ELEVATION_PRESSURE_REFERENCE_M`).

Aerodynamic **force** at speed $v$:

$$
F_{air} = k'_{air}\,v^2
$$

## Rolling resistance {#rolling-resistance}

$$
F_{roll} = C_{rr}\,m\,g
$$

- $C_{rr}$ — `EnergyModel.CRR` (0.015)
- $m$ — `EnergyParams.getTotalMassKg()`
- $g$ — `EnergyModel.G_MS2`

## Aerodynamic drag (combined form) {#aerodynamic-drag}

At reference conditions ($t_{corr} = e_{corr} = 0$):

$$
F_{drag} = \tfrac{1}{2}\,\rho_0\,C_d\,A\,v^2
$$

Effective speed $v$ is the segment speed limit capped by `EnergyParams.getMaxSpeedMs()`. If $v \le 0$, only rolling work is applied for that segment.

## Grade (gravitational work) {#grade-gravitational-work}

Signed height change $\Delta h$ over distance $d$ contributes:

$$
F_{grade} = \frac{m\,g\,\Delta h}{d}
$$

$\Delta h$ and mean elevation come from SRTM samples at segment endpoints (`EnergyRouteHelper` / `SRTMElevationProvider`).

## Total segment cost {#total-segment-cost}

Total force along the segment:

$$
F_{total} = F_{roll} + F_{air} + F_{grade}\quad(\text{with recuperation adjustment on downhill grade, see below})
$$

Mechanical work:

$$
E_{mech} = F_{total}\,d
$$

Travel time $t = d/v$. Standby energy:

$$
E_{standby} = P_s\,t
$$

**Segment cost** returned by `EnergyModel.segmentCost()`:

$$
E = E_{mech} + E_{standby}
$$

Route total: `EnergyModel.routeCost()` sums $E$ over all segments.

## Recuperation {#recuperation}

When $F_{grade} < 0$ (downhill), the implementation adds a recuperation term (port of negative-gradient handling in `driver_break_energy.c`):

$$
F_{recup} = -F_{grade}\,\eta
$$

$$
F_{total} = F_{roll} + F_{air} + F_{grade} + F_{recup} = F_{roll} + F_{air} + F_{grade}\,(1 - \eta)
$$

$\eta$ — `EnergyParams.getRecuperationEfficiency()` (default 0.7).

## Elevation dependency {#elevation-dependency}

Grade terms require **per-segment elevation** from cached **1° × 1°** SRTM tiles (Copernicus GeoTIFF, then HGT). If a tile is missing or the sample is void (Navit DEM sentinel **-32768**; OsmAnd uses `SRTMElevationProvider.VOID_ELEVATION` when unreadable), endpoints lack reliable $\Delta h$ and the model cannot distinguish hilly from flat alternatives meaningfully.

Enable energy-aware routing only after downloading tiles for your corridor (see user doc [Elevation Data (SRTM)](../../user/plugins/driver-break.md#elevation-data-srtm)).

## Implementation notes {#implementation-notes}

| Item | Location |
|---|---|
| Core model | `OsmAnd/src/net/osmand/plus/plugins/driverbreak/EnergyModel.java` |
| Parameters | `EnergyParams.java` |
| Segment list from routes | `EnergyRouteHelper.java` |
| Elevation sampling | `SRTMElevationProvider.java` |
| Post-route comparison UI | `EnergyRouteBottomSheet.java`, `DriverBreakPlugin.analyzeRouteEnergy()` |

Key constants in `EnergyModel.java`: `CRR`, `G_MS2`, `AIR_DENSITY_SEA_LEVEL_KG_M3`, `REFERENCE_OUTSIDE_TEMP_C`, `TEMP_CORRECTION_PER_DEG_C`, `ELEVATION_PRESSURE_FACTOR`, `ELEVATION_PRESSURE_REFERENCE_M`.

Unit tests: `OsmAnd/test/java/net/osmand/plus/plugins/driverbreak/EnergyModelTest.java` (hand-calculated checks against Navit constants).

Navit reference: [formulas.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/formulas.rst), [how_it_works.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/how_it_works.rst).
