package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingDistanceWidgetState.TripRecordingDistanceMode.TOTAL_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState;
import net.osmand.util.Algorithms;

public class TripRecordingDistanceWidgetState extends WidgetState {

	public static final String TRIP_RECORDING_DISTANCE_WIDGET_MODE = "trip_recording_distance_widget_mode";

	private final WidgetType widgetType;
	private final OsmandPreference<TripRecordingDistanceMode> distanceModePreference;

	public TripRecordingDistanceWidgetState(@NonNull OsmandApplication app, @Nullable String customId, WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.distanceModePreference = registerDistanceModePreference(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	public OsmandPreference<TripRecordingDistanceMode> getDistanceModePreference() {
		return distanceModePreference;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(widgetType.titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return widgetType.getIconId(nightMode);
	}

	@Override
	public void changeToNextState() {
		TripRecordingDistanceMode currentMode = distanceModePreference.get();
		distanceModePreference.set(currentMode.next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerDistanceModePreference(customId).setModeValue(appMode, distanceModePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<TripRecordingDistanceMode> registerDistanceModePreference(@Nullable String customId) {
		String prefId = TRIP_RECORDING_DISTANCE_WIDGET_MODE;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, TOTAL_DISTANCE,
				TripRecordingDistanceMode.values(), TripRecordingDistanceMode.class).makeProfile();
	}

	public enum TripRecordingDistanceMode {

		TOTAL_DISTANCE(R.string.total_distance, R.drawable.widget_trip_recording_distance_day, R.drawable.widget_trip_recording_distance_night),
		LAST_DOWNHILL(R.string.shared_string_last_downhill, R.drawable.widget_trip_recording_distance_last_downhill_day, R.drawable.widget_trip_recording_distance_last_downhill_day),
		LAST_UPHILL(R.string.shared_string_last_uphill, R.drawable.widget_trip_recording_distance_last_uphill_day, R.drawable.widget_trip_recording_distance_last_uphill_day);

		@StringRes
		final int titleId;
		private final int dayIcon;
		private final int nightIcon;

		TripRecordingDistanceMode(int titleId, int dayIcon, int nightIcon) {
			this.titleId = titleId;
			this.dayIcon = dayIcon;
			this.nightIcon = nightIcon;
		}

		@StringRes
		public int getTitleId() {
			return titleId;
		}

		public int getIcon(boolean nightMode) {
			return nightMode ? dayIcon : nightIcon;
		}

		@NonNull
		public TripRecordingDistanceMode next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}
