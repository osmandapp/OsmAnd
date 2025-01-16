package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RouteInfoWidget extends SimpleWidget implements ISupportWidgetResizing {

	private static final String DISPLAY_MODE_PREF_ID = "route_info_widget_display_mode";

	private static final String ARRIVAL_TIME_FORMAT = "HH:mm";

	private final CommonPreference<RouteInfoDisplayMode> displayModePref;

	public RouteInfoWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, WidgetType.ROUTE_INFO, customId, panel);
		displayModePref = registerDisplayModePreference(customId);
	}

	@NonNull
	public RouteInfoDisplayMode getDisplayMode(@NonNull ApplicationMode appMode) {
		return displayModePref.getModeValue(appMode);
	}

	public void setDisplayMode(@NonNull ApplicationMode appMode, @NonNull RouteInfoDisplayMode displayMode) {
		displayModePref.setModeValue(appMode, displayMode);
	}

	@NonNull
	private CommonPreference<RouteInfoDisplayMode> registerDisplayModePreference(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId)
				? DISPLAY_MODE_PREF_ID
				: DISPLAY_MODE_PREF_ID + customId;
		return settings.registerEnumStringPreference(prefId, RouteInfoDisplayMode.ARRIVAL_TIME,
						RouteInfoDisplayMode.values(), RouteInfoDisplayMode.class)
				.makeProfile()
				.cache();
	}

	@NonNull
	public static String formatArrivalTime(long arrivalTime) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(ARRIVAL_TIME_FORMAT, Locale.getDefault());
		return dateFormat.format(arrivalTime);
	}

	@NonNull
	public static String formatDuration(@NonNull Context ctx, long timeLeft) {
		long diffInMinutes = TimeUnit.MINUTES.convert(timeLeft, TimeUnit.MILLISECONDS);
		String hour = ctx.getString(R.string.int_hour);
		String formattedDuration = Algorithms.formatMinutesDuration((int) diffInMinutes, true);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, formattedDuration, hour);
	}

	@NonNull
	public static String formatDistance(@NonNull OsmandApplication ctx, float meters) {
		return OsmAndFormatter.getFormattedDistance(meters, ctx, OsmAndFormatterParams.USE_LOWER_BOUNDS);
	}
}
