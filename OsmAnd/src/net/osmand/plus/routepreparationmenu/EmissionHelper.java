package net.osmand.plus.routepreparationmenu;

import static net.osmand.map.WorldRegion.FRANCE_REGION_ID;
import static net.osmand.map.WorldRegion.GERMANY_REGION_ID;
import static net.osmand.plus.routing.RoutingHelperUtils.getParameterForDerivedProfile;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.populateListParameters;
import static net.osmand.router.GeneralRouter.MOTOR_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.resources.DetectRegionTask;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.preferences.ListParameters;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.util.Algorithms;

import java.text.DecimalFormat;

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

public class EmissionHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final TargetPointsHelper targetPointsHelper;

	public EmissionHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		targetPointsHelper = app.getTargetPointsHelper();
	}

	public enum MotorType {
		PETROL(7.85f, 2.80f), // L
		DIESEL(6.59f, 3.17f), // L
		LPG(10.60f, 1.86f), // L
		GAS(4.90f, 2.28f), // kg
		ELECTRIC(21.1f, 0.42f), // kWh fuelEmissionFactor "UE except France"
		HYBRID(5.61f, 2.80f); // L, hybrid petrol

		public final float fuelConsumption; // unit (L/kwH/kg)/100km
		public final float fuelEmissionFactor; // kg CO2/unit (L/kwH/kg)

		MotorType(float fuelConsumption, float fuelEmissionFactor) {
			this.fuelConsumption = fuelConsumption;
			this.fuelEmissionFactor = fuelEmissionFactor;
		}

		@Nullable
		public static MotorType getMotorTypeByName(@NonNull String name) {
			for (MotorType type : values()) {
				if (type.name().equalsIgnoreCase(name)) {
					return type;
				}
			}
			return null;
		}

		public boolean shouldCheckRegion() {
			return this == ELECTRIC;
		}
	}

	@Nullable
	public MotorType getMotorTypeForMode(@NonNull ApplicationMode mode) {
		GeneralRouter router = app.getRouter(mode);
		if (router != null && mode.getRouteService() == RouteService.OSMAND) {
			RoutingParameter parameter = getParameterForDerivedProfile(MOTOR_TYPE, mode, router);
			if (parameter != null) {
				OsmandPreference<String> pref = settings.getCustomRoutingProperty(parameter.getId(), parameter.getDefaultString());
				ListParameters parameters = populateListParameters(app, parameter);
				int index = parameters.findIndexOfValue(pref.getModeValue(mode));
				if (index != -1) {
					return MotorType.getMotorTypeByName(parameters.originalNames[index]);
				}
			}
		}
		return null;
	}

	public void getEmission(@NonNull MotorType motorType, float meters, @NonNull CallbackWithObject<String> callback) {
		if (motorType.shouldCheckRegion()) {
			LatLon latLon = getLatLon();
			if (latLon != null) {
				CallbackWithObject<WorldRegion> onRegionDetected = region -> {
					float emissionFactor = getEmissionFactorForRegion(region, motorType.fuelEmissionFactor);
					callback.processResult(getFormattedEmission(motorType, meters, emissionFactor));
					return true;
				};
				DetectRegionTask task = new DetectRegionTask(app, onRegionDetected);
				OsmAndTaskManager.executeTask(task, latLon);
			}
		} else {
			callback.processResult(getFormattedEmission(motorType, meters, motorType.fuelEmissionFactor));
		}
	}

	private String getFormattedEmission(@NonNull MotorType motorType, float meters, float fuelEmissionFactor) {
		float emissionsGramsByKm = motorType.fuelConsumption * fuelEmissionFactor * 10;
		double totalEmissionsKg = meters / 1000 * emissionsGramsByKm / 1000;

		String pattern = totalEmissionsKg / 10 > 1 ? "0" : "0.0";
		String emission = new DecimalFormat(pattern).format(totalEmissionsKg);
		String text = app.getString(R.string.ltr_or_rtl_combine_via_space, emission, app.getString(R.string.kg));
		text = app.getString(R.string.ltr_or_rtl_combine_via_space, "~", text);
		return app.getString(R.string.ltr_or_rtl_combine_via_space, text, app.getString(R.string.co2_mission));
	}

	private float getEmissionFactorForRegion(@Nullable WorldRegion region, float defaultEmissionFactor) {
		if (region != null) {
			String id = region.getRegionId();
			if (Algorithms.stringsEqual(id, FRANCE_REGION_ID)) {
				return 0.055f;
			} else if (Algorithms.stringsEqual(id, GERMANY_REGION_ID)) {
				return 0.4f;
			}
			WorldRegion parent = region.getSuperregion();
			if (!parent.isContinent()) {
				return getEmissionFactorForRegion(parent, defaultEmissionFactor);
			}
		}
		return defaultEmissionFactor;
	}

	@Nullable
	private LatLon getLatLon() {
		TargetPoint targetPoint = targetPointsHelper.getPointToStart();
		if (targetPoint == null) {
			targetPoint = targetPointsHelper.getMyLocationToStart();
		}
		return targetPoint != null ? targetPoint.getLatLon() : null;
	}
}