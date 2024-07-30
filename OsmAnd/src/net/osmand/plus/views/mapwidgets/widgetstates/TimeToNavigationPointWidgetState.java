package net.osmand.plus.views.mapwidgets.widgetstates;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.util.Algorithms;

public class TimeToNavigationPointWidgetState extends WidgetState {

	public static final int TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME = R.id.time_control_widget_state_arrival_time;
	public static final int TIME_CONTROL_WIDGET_STATE_TIME_TO_GO = R.id.time_control_widget_state_time_to_go;

	private final boolean intermediate;
	private final OsmandPreference<Boolean> arrivalTimeOrTimeToGo;

	public TimeToNavigationPointWidgetState(@NonNull OsmandApplication app, @Nullable String customId, boolean intermediate) {
		super(app);
		this.intermediate = intermediate;
		this.arrivalTimeOrTimeToGo = registerTimeTypePref(customId);
	}

	public boolean isIntermediate() {
		return intermediate;
	}

	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return arrivalTimeOrTimeToGo;
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
	public void changeToNextState() {
		arrivalTimeOrTimeToGo.set(!arrivalTimeOrTimeToGo.get());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerTimeTypePref(customId).setModeValue(appMode, arrivalTimeOrTimeToGo.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<Boolean> registerTimeTypePref(@Nullable String customId) {
		String prefId = intermediate ? "show_arrival_time" : "show_intermediate_arrival_time";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerBooleanPreference(prefId, true).makeProfile();
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