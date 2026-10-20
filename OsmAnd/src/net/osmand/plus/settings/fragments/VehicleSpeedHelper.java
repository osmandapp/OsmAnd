package net.osmand.plus.settings.fragments;

import static net.osmand.plus.routing.RouteService.DIRECT_TO;
import static net.osmand.plus.routing.RouteService.STRAIGHT;
import static net.osmand.plus.settings.backend.ApplicationMode.FAST_SPEED_THRESHOLD;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.settings.enums.SpeedConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.router.GeneralRouter;

public class VehicleSpeedHelper {

	private static final float MAX_DEFAULT_SPEED = 300;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final ApplicationMode mode;

	public VehicleSpeedHelper(@NonNull OsmandApplication app, @NonNull ApplicationMode mode) {
		this.app = app;
		this.mode = mode;
		this.settings = app.getSettings();
	}

	/** Slider bounds and current values of the profile, in the units the user sees. */
	public static class SpeedConfig {
		public float ratio;
		public int min;
		public int max;
		public float defaultSpeed;
		public float minSpeed;
		public float maxSpeed;
		public boolean defaultSpeedOnly;
		public boolean decimalPrecision;
		public String units;
	}

	@NonNull
	public SpeedConfig createSpeedConfig() {
		GeneralRouter router = app.getRouter(mode);
		RouteService routeService = mode.getRouteService();
		float maxSpeedLimit = VehicleSpeedConfigLimits.getMaxSpeedConfigLimit(app, mode);
		SpeedConfig config = new SpeedConfig();
		config.defaultSpeedOnly = routeService == STRAIGHT || routeService == DIRECT_TO || router == null;
		config.decimalPrecision = !config.defaultSpeedOnly && maxSpeedLimit / 1.5f <= FAST_SPEED_THRESHOLD;

		float[] ratio = getSpeedRatio();
		float[] minValue = new float[1];
		float[] maxValue = new float[1];
		Pair<Integer, Integer> pair = getMinMax(router, ratio, minValue, maxValue, config.defaultSpeedOnly, config.decimalPrecision);
		config.ratio = ratio[0];
		config.min = pair.first;
		config.max = pair.second;
		config.minSpeed = minValue[0];
		config.maxSpeed = maxValue[0];
		config.defaultSpeed = roundSpeed(mode.getDefaultSpeed() * ratio[0], config.decimalPrecision);
		config.units = getSpeedUnits();
		return config;
	}

	@NonNull
	public String formatSpeed(@NonNull SpeedConfig config, float speed) {
		return formatSpeed(speed, config.decimalPrecision);
	}

	@NonNull
	private Pair<Integer, Integer> getMinMax(@Nullable GeneralRouter router, float[] ratio,
	                                         float[] minValue, float[] maxValue,
	                                         boolean defaultSpeedOnly, boolean decimalPrecision) {
		int min;
		int max;

		float settingsDefaultSpeed = mode.getDefaultSpeed();
		if (defaultSpeedOnly || router == null) {
			minValue[0] = Math.round(Math.min(1, settingsDefaultSpeed) * ratio[0]);
			maxValue[0] = Math.round(Math.max(MAX_DEFAULT_SPEED, settingsDefaultSpeed) * ratio[0]);
			min = Math.round(minValue[0]);
		} else {
			float settingsMinSpeed = mode.getMinSpeed();
			float settingsMaxSpeed = mode.getMaxSpeed();

			float minSpeedValue = settingsMinSpeed > 0 ? settingsMinSpeed : router.getMinSpeed();
			float maxSpeedValue = settingsMaxSpeed > 0 ? settingsMaxSpeed : router.getMaxSpeed();

			minValue[0] = roundSpeed(Math.min(minSpeedValue, settingsDefaultSpeed) * ratio[0], decimalPrecision);
			maxValue[0] = roundSpeed(Math.max(maxSpeedValue, settingsDefaultSpeed) * ratio[0], decimalPrecision);

			float minSpeed = router.getMinSpeed() / 2f;
			min = Math.round(Math.min(minValue[0], minSpeed * ratio[0]));
		}
		float maxSpeedConfigLimit = VehicleSpeedConfigLimits.getMaxSpeedConfigLimit(app, mode);
		max = Math.round(Math.max(maxValue[0], maxSpeedConfigLimit * ratio[0]));
		return new Pair<>(min, max);
	}

	@NonNull
	private float[] getSpeedRatio() {
		float[] ratio = new float[1];
		SpeedConstants constants = settings.SPEED_SYSTEM.getModeValue(mode);
		switch (constants) {
			case MILES_PER_HOUR -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
			case KILOMETERS_PER_HOUR -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
			case MINUTES_PER_KILOMETER -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_KILOMETER;
			case NAUTICALMILES_PER_HOUR ->
					ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_NAUTICALMILE;
			case MINUTES_PER_MILE -> ratio[0] = 3600 / OsmAndFormatter.METERS_IN_ONE_MILE;
			case METERS_PER_SECOND -> ratio[0] = 1;
		}
		return ratio;
	}

	@NonNull
	private String getSpeedUnits() {
		SpeedConstants constants = settings.SPEED_SYSTEM.getModeValue(mode);
		switch (constants) {
			case MINUTES_PER_KILOMETER:
				return app.getString(R.string.km_h);
			case MINUTES_PER_MILE:
				return app.getString(R.string.mile_per_hour);
		}
		return constants.toShortString();
	}

	@NonNull
	private String formatSpeed(float speed, boolean decimalPrecision) {
		return decimalPrecision ? OsmAndFormatter.formatValue(speed, "", true, 1, app).value : String.valueOf((int) speed);
	}

	private float roundSpeed(float speed, boolean decimalPrecision) {
		return decimalPrecision ? Math.round(speed * 10) / 10f : Math.round(speed);
	}
}
