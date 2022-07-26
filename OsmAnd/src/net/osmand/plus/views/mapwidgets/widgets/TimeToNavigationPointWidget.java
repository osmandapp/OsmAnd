package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.TIME_TO_INTERMEDIATE;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.TimeToNavigationPointWidgetState.TimeToNavigationPointState;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;

public class TimeToNavigationPointWidget extends TextInfoWidget {

	private static final long UPDATE_INTERVAL_SECONDS = 30;

	private final RoutingHelper routingHelper;
	private final TimeToNavigationPointWidgetState widgetState;
	private final OsmandPreference<Boolean> arrivalTimeOtherwiseTimeToGoPref;

	private boolean cachedArrivalTimeOtherwiseTimeToGo;
	private int cachedLeftSeconds;

	public TimeToNavigationPointWidget(@NonNull MapActivity mapActivity, @NonNull TimeToNavigationPointWidgetState widgetState) {
		super(mapActivity, widgetState.isIntermediate() ? TIME_TO_INTERMEDIATE : TIME_TO_DESTINATION);
		this.widgetState = widgetState;
		this.routingHelper = app.getRoutingHelper();
		this.arrivalTimeOtherwiseTimeToGoPref = widgetState.getPreference();
		this.cachedArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.get();

		setText(null, null);
		updateIcons();
		updateContentTitle();
		setOnClickListener(v -> {
			widgetState.changeToNextState();
			updateInfo(null);
			mapActivity.refreshMap();
		});
	}

	public boolean isIntermediate() {
		return widgetState.isIntermediate();
	}

	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return arrivalTimeOtherwiseTimeToGoPref;
	}

	@Nullable
	@Override
	public WidgetState getWidgetState() {
		return widgetState;
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int leftSeconds = 0;

		boolean timeModeUpdated = arrivalTimeOtherwiseTimeToGoPref.get() != cachedArrivalTimeOtherwiseTimeToGo;
		if (timeModeUpdated) {
			cachedArrivalTimeOtherwiseTimeToGo = arrivalTimeOtherwiseTimeToGoPref.get();
			updateIcons();
			updateContentTitle();
		}

		if (routingHelper.isRouteCalculated()) {
			leftSeconds = widgetState.isIntermediate() ? routingHelper.getLeftTimeNextIntermediate() : routingHelper.getLeftTime();
			boolean updateIntervalPassed = Math.abs(leftSeconds - cachedLeftSeconds) > UPDATE_INTERVAL_SECONDS;
			if (leftSeconds != 0 && (updateIntervalPassed || timeModeUpdated)) {
				cachedLeftSeconds = leftSeconds;
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

	private void updateIcons() {
		TimeToNavigationPointState state = getCurrentState();
		setIcons(state.dayIconId, state.nightIconId);
	}

	private void updateContentTitle() {
		String title = getCurrentState().getTitle(app);
		setContentTitle(title);
	}

	private void updateArrivalTime(int leftSeconds) {
		long arrivalTime = System.currentTimeMillis() + leftSeconds * 1000L;
		setTimeText(arrivalTime);
	}

	private void updateTimeToGo(int leftSeconds) {
		String formattedLeftTime = OsmAndFormatter.getFormattedDurationShortMinutes(leftSeconds);
		setText(formattedLeftTime, null);
	}

	@NonNull
	private TimeToNavigationPointState getCurrentState() {
		return TimeToNavigationPointState.getState(widgetState.isIntermediate(), arrivalTimeOtherwiseTimeToGoPref.get());
	}
}