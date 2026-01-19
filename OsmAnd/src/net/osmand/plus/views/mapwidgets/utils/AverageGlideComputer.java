package net.osmand.plus.views.mapwidgets.utils;


import static java.lang.String.format;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.widgets.GlideAverageWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
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

	public AverageGlideComputer(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	protected boolean isEnabled() {
		ApplicationMode appMode = settings.getApplicationMode();
		MapWidgetRegistry registry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();

		for (MapWidgetInfo widgetInfo : registry.getAllWidgets()) {
			MapWidget widget = widgetInfo.widget;
			if (widget instanceof GlideAverageWidget
					&& widgetInfo.isEnabledForAppMode(appMode, ScreenLayoutMode.getDefault(widget.getMapActivity()))
					&& WidgetsAvailabilityHelper.isWidgetAvailable(app, widgetInfo.key, appMode)) {
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
		List<Location> locationsToUse = new ArrayList<>();
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
			return format(pattern, "1", "0");
		} else if (absRatio > MAX_VALUE_TO_FORMAT) {
			return format(pattern, "" + (int) absRatio * sign, "" + divider);
		} else {
			return format(pattern, GLIDE_RATIO_FORMATTER.format(absRatio * sign), "" + divider);
		}
	}

	public static boolean areAltitudesEqual(@Nullable Double a1, @Nullable Double a2) {
		return a1 == null && a2 == null
				|| a1 != null && a2 != null && Math.abs(a1 - a2) > 0.01;
	}
}
