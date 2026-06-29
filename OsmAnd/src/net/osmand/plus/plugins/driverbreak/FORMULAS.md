# Driver Break — Energy Model Formula Reference

For full derivation and background see [Driver Break — Energy Model](../../../../../../web/main/docs/technical/build-osmand/driver-break-energy-model.md) (also on [osmand.net](https://osmand.net/docs/technical/build-osmand/driver-break-energy-model)).

Navit reference: [formulas.rst](https://github.com/Supermagnum/navit/blob/feature/driver-break/docs/user/plugins/driver-break/formulas.rst).

## Symbols and Java constants

| Symbol | Meaning | Java constant / field | Value / unit |
|---|---|---|---|
| $C_{rr}$ | Rolling resistance coefficient | `EnergyModel.CRR` | 0.015 (dimensionless) |
| $m$ | Total mass | `EnergyParams.getTotalMassKg()` / DB `total_weight` | kg |
| $g$ | Gravity | `EnergyModel.G_MS2` | 9.81 m/s² |
| $\rho_0$ | Sea-level air density | `EnergyModel.AIR_DENSITY_SEA_LEVEL_KG_M3` | 1.225 kg/m³ |
| $C_d$ | Drag coefficient | `EnergyParams.getDragCd()` / DB `energy_drag_cd` | dimensionless |
| $A$ | Frontal area | `EnergyParams.getFrontalAreaM2()` / DB `energy_frontal_area_sqm` | m² |
| $v$ | Effective speed | capped speed limit arg | m/s |
| $h$ | Mean segment elevation | `elevationM` parameter | m |
| $d$ | Segment length | `distanceM` parameter | m |
| $\Delta h$ | Signed elevation change | `deltaH` parameter | m |
| $\eta$ | Recuperation efficiency | `EnergyParams.getRecuperationEfficiency()` | 0–1 (default 0.7) |
| $P_s$ | Standby power | `EnergyParams.getStandbyPowerW()` | W (default 0) |
| $T_{ref}$ | Reference outside temp | `EnergyModel.REFERENCE_OUTSIDE_TEMP_C` | 20 °C (private) |
| $k_T$ | Temp correction | `EnergyModel.TEMP_CORRECTION_PER_DEG_C` | 0.0035 (private) |
| $k_H$ | Elevation pressure factor | `EnergyModel.ELEVATION_PRESSURE_FACTOR` | 0.0001375 (private) |
| $h_{ref}$ | Elevation reference | `EnergyModel.ELEVATION_PRESSURE_REFERENCE_M` | 100 m (private) |

## Formulas

### Air coefficient (sea level)

$$k_{air} = \tfrac{1}{2}\,\rho_0\,C_d\,A$$

Java: `EnergyModel.airCoefficientFromDrag(dragCd, frontalAreaM2)`.

### Adjusted air coefficient (per segment)

$$k'_{air} = k_{air}\,\bigl(1 + (T_{ref}-T)\,k_T - k_H\,(h-h_{ref})\bigr)$$

Java: `EnergyModel.adjustedAirCoefficient()` (private).

### Rolling resistance force

$$F_{roll} = C_{rr}\,m\,g$$

Java: `EnergyModel.rollingForce(params)`.

### Aerodynamic force

$$F_{air} = k'_{air}\,v^2$$

### Grade force

$$F_{grade} = \frac{m\,g\,\Delta h}{d}$$

### Total force (uphill or flat, $F_{grade} \ge 0$)

$$F_{total} = F_{roll} + F_{air} + F_{grade}$$

### Total force (downhill, $F_{grade} < 0$)

$$F_{recup} = -F_{grade}\,\eta$$

$$F_{total} = F_{roll} + F_{air} + F_{grade} + F_{recup}$$

### Segment energy

$$E = F_{total}\,d + P_s\,\frac{d}{v}$$

Java: `EnergyModel.segmentCost(...)`.

**Guard:** if $d \le 0$, return **0** immediately. If $v \le 0$ after capping, return $F_{roll}\,d$ only.

### Route total

$$E_{route} = \sum_i E_i$$

Java: `EnergyModel.routeCost(segments, params)`.

## SRTM void

Navit DEM void sentinel: **-32768**. OsmAnd: `SRTMElevationProvider.VOID_ELEVATION` (`Integer.MIN_VALUE`) when tile missing or unreadable.

## ECU fuel rate (OBD MAF fallback)

$$\text{L/h} = \frac{\text{MAF (g/s)} \times 3600}{\text{AFR} \times \rho \times 1000}$$

See `OBDBackend` and Navit `obd_maf_to_fuel_rate`.

## J1939 scaling

$$\text{L/h} = \text{raw}_{SPN183} \times 0.05,\quad \text{\%} = \text{raw}_{SPN96} \times 0.4$$

Java: `J1939Backend.FUEL_RATE_SCALE_L_H`, `FUEL_LEVEL_SCALE_PERCENT`.

## Adaptive EMA

$$\text{rate} \leftarrow \alpha \cdot \text{sample} + (1-\alpha)\cdot \text{rate},\quad \alpha = 0.1$$

Java: `AdaptiveFuelLearner.EMA_ALPHA`.
