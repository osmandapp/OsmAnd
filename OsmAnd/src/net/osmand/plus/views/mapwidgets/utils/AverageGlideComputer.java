package net.osmand.plus.views.mapwidgets.utils;

import static net.osmand.plus.views.mapwidgets.WidgetType.GLIDE_AVERAGE;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AverageGlideComputer extends AverageValueComputer {

	private static final NumberFormat GLIDE_RATIO_FORMATTER = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
	public static final float MAX_VALUE_TO_DISPLAY = 150.f;
	public static final float MAX_VALUE_TO_FORMAT = 100.f;
	public static final float MIN_ACCEPTABLE_VALUE = 0.1f;

	private final List<Location> locationsToUse = new ArrayList<>();
	private final List<MapWidgetInfo> glideWidgets = new ArrayList<>();

	public AverageGlideComputer(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected boolean isEnabled() {
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		MapInfoLayer mapInfoLayer = mapLayers.getMapInfoLayer();
		if (mapInfoLayer == null) {
			return false;
		}
		MapActivity activity = mapInfoLayer.getMapActivity();
		if (activity == null) {
			return false;
		}
		ApplicationMode appMode = settings.getApplicationMode();
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
		MapWidgetRegistry widgetRegistry = mapLayers.getMapWidgetRegistry();

		glideWidgets.clear();
		widgetRegistry.collectWidgetsInfo(glideWidgets, appMode, layoutMode, null, GLIDE_AVERAGE, true);

		for (int i = 0; i < glideWidgets.size(); i++) {
			MapWidgetInfo info = glideWidgets.get(i);
			if (WidgetsAvailabilityHelper.isWidgetAvailable(app, info.key, appMode)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void saveLocation(@NonNull Location location, long time) {
		if (location.hasAltitude()) {
			Location loc = new Location(location);
			loc.setTime(time);
			locations.add(loc);
			clearExpiredLocations(BIGGEST_MEASURED_INTERVAL);
		}
	}

	@Nullable
	public String getFormattedAverageGlideRatio(long measuredInterval) {
		locationsToUse.clear();

		long now = System.currentTimeMillis();
		for (Location location : locations) {
			long locationTime = location.getTime();
			// Check if the location is within the measured interval and after the start timestamp
			if (now - locationTime <= measuredInterval) {
				locationsToUse.add(location);
			}
		}
		if (!Algorithms.isEmpty(locationsToUse)) {
			double distance = calculateTotalDistance(locationsToUse);
			double difference = calculateAltitudeDifference(locationsToUse);
			return calculateFormattedRatio(app, distance, difference);
		}
		return null;
	}

	private double calculateTotalDistance(@NonNull List<Location> locations) {
		double totalDistance = 0;
		for (int i = 0; i < locations.size() - 1; i++) {
			Location l1 = locations.get(i);
			Location l2 = locations.get(i + 1);
			totalDistance += MapUtils.getDistance(l1, l2);
		}
		return totalDistance;
	}

	private double calculateAltitudeDifference(@NonNull List<Location> locations) {
		int size = locations.size();
		if (size > 1) {
			Location start = locations.get(0);
			Location end = locations.get(size - 1);
			return start.getAltitude() - end.getAltitude();
		}
		return 0;
	}

	@NonNull
	public static String calculateFormattedRatio(@NonNull Context ctx, LatLon l1, LatLon l2, double a1, double a2) {
		double distance = MapUtils.getDistance(l1, l2);
		return calculateFormattedRatio(ctx, distance, a1 - a2);
	}

	@NonNull
	public static String calculateFormattedRatio(@NonNull Context ctx, double distance, double altDif) {
		int sign = altDif < 0 ? -1 : 1;

		// Round arguments to '0' if they are smaller
		// in absolute value than the minimum acceptable value
		if (Math.abs(distance) < MIN_ACCEPTABLE_VALUE) {
			distance = 0;
		}
		if (Math.abs(altDif) < MIN_ACCEPTABLE_VALUE) {
			altDif = 0;
		}

		// Calculate and round glide ratio if needed
		float absRatio = 0;
		if (distance > 0) {
			absRatio = altDif != 0 ? (float) Math.abs(distance / altDif) : 1;
		}
		if (absRatio < MIN_ACCEPTABLE_VALUE) {
			absRatio = 0;
		}
		int divider = altDif != 0 ? 1 : 0;

		String pattern = ctx.getString(R.string.ltr_or_rtl_combine_via_colon_with_space);
		if (absRatio > MAX_VALUE_TO_DISPLAY || (absRatio == 1 && divider == 0)) {
			return String.format(pattern, "1", "0");
		} else if (absRatio > MAX_VALUE_TO_FORMAT) {
			return String.format(pattern, "" + (int) absRatio * sign, "" + divider);
		} else {
			return String.format(pattern, GLIDE_RATIO_FORMATTER.format(absRatio * sign), "" + divider);
		}
	}

	public static boolean areAltitudesEqual(@Nullable Double a1, @Nullable Double a2) {
		return a1 == null && a2 == null || a1 != null && a2 != null && Math.abs(a1 - a2) > 0.01;
	}
}
