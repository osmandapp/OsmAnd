package net.osmand.plus.routepreparationmenu;

import android.content.Context;
import net.osmand.plus.R;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import static net.osmand.plus.settings.backend.ApplicationMode.CAR;
import java.text.DecimalFormat;

public class Co2Computer {
    public static final String MOTOR_TYPE = "motor_type";

    /*
    Here I considered the CO2 footprint of the use only, not the whole life cycle
    I copied methodology of https://www.youtube.com/watch?v=zjaUqUozwdc&t=3000s
    which is to take the average consumption of all "automobile" of all years that rode > 1500 km on
    https://www.spritmonitor.de/en/overview/0-All_manufactures/0-All_models.html?powerunit=2
    and multiply by the emission factor found at p.16 of
    "Information GES des prestations de transport - Guide méthodologique" version September 2018
    https://www.ecologie.gouv.fr/sites/default/files/Info%20GES_Guide%20m%C3%A9thodo.pdf
    For electricity, the default CO2/kWh number is "Europe (except France)"
    Specific number for France is at 7min25s in the video.
    I took the kWh/100km number of the video because the spritmonitor average was very close
    to the one used in the video for the Renault Clio.
    For natural gas, I took 1 m^3 = 1.266 kg from  https://www.grdf.fr/acteurs-gnv/accompagnement-grdf-gnv/concevoir-projet/reservoir-gnc
    Only fossil fuel are counted for hybrid cars since their consumption is given in liter
     */

    public enum MotorType {
        PETROL(R.string.petrol,     7.85f, 2.80f), // L
        DIESEL(R.string.diesel,     6.59f, 3.17f), // L
        LPG(R.string.LPG,           10.60f,1.86f), // L
        GAS(R.string.gas,           4.90f, 2.28f), // kg
        ELECTRIC(R.string.electric, 21.1f, 0.42f), // kWh fuelEmissionFactor "UE except France"
        HYBRID(R.string.hybrid,     5.61f, 2.80f); // L, hybrid petrol

        public final int key;
        public final float fuelConsumptionForCar; // unit (L/kwH/kg)/100km
        public final float fuelEmissionFactor; // kg CO2/unit (L/kwH/kg)

        MotorType(int key, float fuelConsumptionForCar, float fuelEmissionFactor) {
            this.key = key;
            this.fuelConsumptionForCar = fuelConsumptionForCar;
            this.fuelEmissionFactor = fuelEmissionFactor;
        }
        public String toHumanString(Context ctx) {
            return ctx.getString(key);
        }
    }

    public static boolean modeEmitCo2(ApplicationMode am) {
        return am == CAR;
    }

    public static String getFormattedCO2(float meters, OsmandApplication app) {

        MotorType motor = app.getSettings().MOTOR_TYPE.get();
        double lat = app.getRoutingHelper().getLastFixedLocation().getLatitude();
        double lon = app.getRoutingHelper().getLastFixedLocation().getLongitude();
        LatLon lastFixedLocationLatLon = new LatLon(lat,lon);
        String locationCountry = app.getRegions().getCountryName(lastFixedLocationLatLon);
        ApplicationMode am = app.getSettings().getApplicationMode();

        float emissionsGramsByKm = 0;
        float fuelEmissionFactor = motor.fuelEmissionFactor;
        if (motor.equals(MotorType.ELECTRIC)) {
            if (locationCountry.equals("France")) {
                fuelEmissionFactor = 0.055f;
            } else if (locationCountry.equals("Deutschland")) {
                fuelEmissionFactor = 0.4f;
            }
        }
        if (am == CAR) {
            emissionsGramsByKm = motor.fuelConsumptionForCar * fuelEmissionFactor * 10;
        }
        double totalEmissions_kg = meters/1000. * emissionsGramsByKm/1000.;
        if (totalEmissions_kg/10. > 1) {
            DecimalFormat fixed1 = new DecimalFormat("0");
            return fixed1.format(totalEmissions_kg) + " kg";
        } else {
            DecimalFormat fixed1 = new DecimalFormat("0.0");
            return fixed1.format(totalEmissions_kg) + " kg";
        }
    }
}
