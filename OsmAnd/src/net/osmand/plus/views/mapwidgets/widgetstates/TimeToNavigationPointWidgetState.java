package net.osmand.plus.views.mapwidgets.widgetstates;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class TimeToNavigationPointWidgetState extends WidgetState {

	public static final int TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME = R.id.time_control_widget_state_arrival_time;
	public static final int TIME_CONTROL_WIDGET_STATE_TIME_TO_GO = R.id.time_control_widget_state_time_to_go;

	private final boolean intermediate;
	private final OsmandPreference<Boolean> arrivalTimeOrTimeToGo;

	public TimeToNavigationPointWidgetState(@NonNull OsmandApplication app, boolean intermediate) {
		super(app);
		this.intermediate = intermediate;
		this.arrivalTimeOrTimeToGo = intermediate
				? app.getSettings().INTERMEDIATE_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO
				: app.getSettings().DESTINATION_ARRIVAL_TIME_OTHERWISE_TIME_TO_GO;
	}

	@NonNull
	@Override
	public String getTitle() {
		return TimeToNavigationPointState
				.getState(intermediate, arrivalTimeOrTimeToGo.get())
				.getTitle(app);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return TimeToNavigationPointState
				.getState(intermediate, arrivalTimeOrTimeToGo.get())
				.getIconId(nightMode);
	}

	@Override
	public void changeState(int stateId) {
		arrivalTimeOrTimeToGo.set(stateId == TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME);
	}

	public enum TimeToNavigationPointState {

		INTERMEDIATE_TIME_TO_GO(R.string.map_widget_time, R.drawable.widget_intermediate_time_to_go_day, R.drawable.widget_intermediate_time_to_go_night, true),
		INTERMEDIATE_ARRIVAL_TIME(R.string.access_arrival_time, R.drawable.widget_intermediate_time_day, R.drawable.widget_intermediate_time_night, true),
		DESTINATION_TIME_TO_GO(R.string.map_widget_time, R.drawable.widget_destination_time_to_go_day, R.drawable.widget_destination_time_to_go_night, false),
		DESTINATION_ARRIVAL_TIME(R.string.access_arrival_time, R.drawable.widget_time_to_distance_day, R.drawable.widget_time_to_distance_night, false);

		@StringRes
		public final int titleId;
		@DrawableRes
		public final int dayIconId;
		@DrawableRes
		public final int nightIconId;
		public final boolean intermediate;

		TimeToNavigationPointState(@StringRes int titleId,
		                           @DrawableRes int dayIconId,
		                           @DrawableRes int nightIconId,
		                           boolean intermediate) {
			this.titleId = titleId;
			this.dayIconId = dayIconId;
			this.nightIconId = nightIconId;
			this.intermediate = intermediate;
		}

		@NonNull
		public String getTitle(@NonNull Context context) {
			int timeToId = intermediate
					? R.string.map_widget_time_to_intermediate
					: R.string.map_widget_time_to_destination;
			String timeToString = context.getString(timeToId);
			String stateTitle = context.getString(titleId);
			return context.getString(R.string.ltr_or_rtl_combine_via_colon, timeToString, stateTitle);
		}

		@DrawableRes
		public int getIconId(boolean nightMode) {
			return nightMode ? nightIconId : dayIconId;
		}

		@NonNull
		public static TimeToNavigationPointState getState(boolean intermediate, boolean arrivalOtherwiseTimeToGo) {
			if (intermediate) {
				return arrivalOtherwiseTimeToGo ? INTERMEDIATE_ARRIVAL_TIME : INTERMEDIATE_TIME_TO_GO;
			} else {
				return arrivalOtherwiseTimeToGo ? DESTINATION_ARRIVAL_TIME : DESTINATION_TIME_TO_GO;
			}
		}
	}
}