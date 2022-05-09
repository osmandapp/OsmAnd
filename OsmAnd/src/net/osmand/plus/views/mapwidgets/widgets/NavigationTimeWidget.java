package net.osmand.plus.views.mapwidgets.widgets;

import android.text.format.DateFormat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.WidgetParams.ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_ARRIVAL_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.INTERMEDIATE_TIME_TO_GO;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TIME_TO_GO;

public abstract class NavigationTimeWidget extends TextInfoWidget {

	private final RoutingHelper routingHelper;
	private final boolean intermediate;

	protected long cachedTime = 0;

	public NavigationTimeWidget(@NonNull MapActivity mapActivity, boolean intermediate) {
		super(mapActivity);
		this.routingHelper = app.getRoutingHelper();
		this.intermediate = intermediate;

		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int leftSeconds = 0;

		if (routingHelper.isRouteCalculated()) {
			leftSeconds = intermediate ? routingHelper.getLeftTimeNextIntermediate() : routingHelper.getLeftTime();
			if (leftSeconds != 0) {
				updateTime(leftSeconds);
			}
		}

		if (leftSeconds == 0 && cachedTime != 0) {
			cachedTime = 0;
			setText(null, null);
		}
	}

	protected abstract void updateTime(int leftSeconds);

	public static class ArrivalTimeWidget extends NavigationTimeWidget {

		private static final long UPDATE_INTERVAL_MILLIS = 30 * 1000;

		public ArrivalTimeWidget(@NonNull MapActivity mapActivity, boolean intermediate) {
			super(mapActivity, intermediate);
			setIcons(intermediate ? INTERMEDIATE_ARRIVAL_TIME : ARRIVAL_TIME);
			setContentTitle(intermediate ? R.string.access_intermediate_arrival_time : R.string.access_arrival_time);
		}

		@Override
		protected void updateTime(int leftSeconds) {
			long arrivalTime = System.currentTimeMillis() + leftSeconds * 1000L;
			boolean shouldUpdate = Math.abs(arrivalTime - cachedTime) > UPDATE_INTERVAL_MILLIS;
			if (shouldUpdate) {
				cachedTime = arrivalTime;
				if (DateFormat.is24HourFormat(app)) {
					setText(DateFormat.format("k:mm", leftSeconds).toString(), null);
				} else {
					setText(DateFormat.format("h:mm", leftSeconds).toString(),
							DateFormat.format("aa", leftSeconds).toString());
				}
			}
		}
	}

	public static class TimeToGoWidget extends NavigationTimeWidget {

		private static final long UPDATE_INTERVAL_SECONDS = 30;

		public TimeToGoWidget(@NonNull MapActivity mapActivity, boolean intermediate) {
			super(mapActivity, intermediate);
			setIcons(intermediate ? INTERMEDIATE_TIME_TO_GO : TIME_TO_GO);
			setContentTitle(intermediate ? R.string.map_widget_intermediate_time : R.string.map_widget_time);
		}

		@Override
		protected void updateTime(int leftSeconds) {
			if (Math.abs(leftSeconds - cachedTime) > UPDATE_INTERVAL_SECONDS) {
				cachedTime = leftSeconds;
				String formattedLeftTime = OsmAndFormatter.getFormattedDurationShortMinutes(leftSeconds);
				setText(formattedLeftTime, null);
			}
		}
	}
}