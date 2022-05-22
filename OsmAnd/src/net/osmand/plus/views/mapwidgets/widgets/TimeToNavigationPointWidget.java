package net.osmand.plus.views.mapwidgets.widgets;

import android.text.format.DateFormat;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState.TimeToNavigationPointState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TimeToNavigationPointWidget extends TextInfoWidget {

	private static final long UPDATE_INTERVAL_SECONDS = 30;

	private final RoutingHelper routingHelper;
	private final boolean intermediate;
	private final OsmandPreference<Boolean> arrivalTimeOtherwiseTimeToGoPref;

	private boolean cachedArrivalTimeOtherwiseTimeToGo;
	private int cachedLeftSeconds = 0;

	public TimeToNavigationPointWidget(@NonNull MapActivity mapActivity, boolean intermediate) {
		super(mapActivity);
		this.routingHelper = app.getRoutingHelper();
		this.intermediate = intermediate;
		this.arrivalTimeOtherwiseTimeToGoPref = intermediate
				? settings.INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO
				: settings.DESTINATION_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO;
		this.cachedArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.get();

		setText(null, null);
		updateIcons();
		updateContentTitle();
		setOnClickListener(v -> {
			arrivalTimeOtherwiseTimeToGoPref.set(!arrivalTimeOtherwiseTimeToGoPref.get());
			updateInfo(null);
			updateIcons();
			updateContentTitle();
			mapActivity.refreshMap();
		});
	}

	private void updateIcons() {
		TimeToNavigationPointState state = getCurrentState();
		setIcons(state.dayIconId, state.nightIconId);
	}

	private void updateContentTitle() {
		String title = getCurrentState().getTitle(app);
		setContentTitle(title);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int leftSeconds = 0;

		if (routingHelper.isRouteCalculated()) {
			leftSeconds = intermediate ? routingHelper.getLeftTimeNextIntermediate() : routingHelper.getLeftTime();
			boolean updateIntervalPassed = Math.abs(leftSeconds - cachedLeftSeconds) > UPDATE_INTERVAL_SECONDS;
			boolean timeModeUpdated = arrivalTimeOtherwiseTimeToGoPref.get() != cachedArrivalTimeOtherwiseTimeToGo;
			if (leftSeconds != 0 && (updateIntervalPassed || timeModeUpdated)) {
				cachedLeftSeconds = leftSeconds;
				cachedArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.get();
				if (arrivalTimeOtherwiseTimeToGoPref.get()) {
					updateArrivalTime(leftSeconds);
				} else {
					updateTimeToGo(leftSeconds);
				}
			}
		}

		if (leftSeconds == 0 && cachedLeftSeconds != 0) {
			cachedLeftSeconds = 0;
			setText(null, null);
		}
	}

	private void updateArrivalTime(int leftSeconds) {
		long arrivalTime = System.currentTimeMillis() + leftSeconds * 1000L;
		if (DateFormat.is24HourFormat(app)) {
			setText(DateFormat.format("k:mm", arrivalTime).toString(), null);
		} else {
			setText(DateFormat.format("h:mm", arrivalTime).toString(),
					DateFormat.format("aa", arrivalTime).toString());
		}
	}

	private void updateTimeToGo(int leftSeconds) {
		String formattedLeftTime = OsmAndFormatter.getFormattedDurationShortMinutes(leftSeconds);
		setText(formattedLeftTime, null);
	}

	@NonNull
	private TimeToNavigationPointState getCurrentState() {
		return TimeToNavigationPointState.getState(intermediate, arrivalTimeOtherwiseTimeToGoPref.get());
	}
}