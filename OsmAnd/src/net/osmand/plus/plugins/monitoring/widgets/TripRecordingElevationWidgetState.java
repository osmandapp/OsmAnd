package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidgetState.TripRecordingElevationMode.LAST;
import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidgetState.TripRecordingElevationMode.TOTAL;

import androidx.annotation.DrawableRes;
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

public class TripRecordingElevationWidgetState extends WidgetState {

	public static final String UPHILL_WIDGET_MODE = "uphill_widget_mode";

	private final WidgetType widgetType;
	private final OsmandPreference<TripRecordingElevationMode> elevationModePreference;

	boolean isUphillType;

	public TripRecordingElevationWidgetState(@NonNull OsmandApplication app, boolean isUphillType, @Nullable String customId, WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.isUphillType = isUphillType;
		this.elevationModePreference = registerElevationModePreference(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	public OsmandPreference<TripRecordingElevationMode> getElevationModePreference() {
		return elevationModePreference;
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
		if(elevationModePreference.get() == TOTAL){
			elevationModePreference.set(LAST);
		} else{
			elevationModePreference.set(TOTAL);
		}
	}

	@StringRes
	public int getModeTitleId(){
		return elevationModePreference.get().getTitleId(isUphillType);
	}

	@DrawableRes
	public int getModeIconId(boolean nightMode){
		return elevationModePreference.get().getIcon(isUphillType, nightMode);
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerElevationModePreference(customId).setModeValue(appMode, elevationModePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private String getPrefId() {
		return UPHILL_WIDGET_MODE;
	}

	@NonNull
	private OsmandPreference<TripRecordingElevationMode> registerElevationModePreference(@Nullable String customId) {
		String prefId = UPHILL_WIDGET_MODE;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, TOTAL,
				TripRecordingElevationMode.values(), TripRecordingElevationMode.class).makeProfile();
	}

	public enum TripRecordingElevationMode {

		TOTAL(R.string.shared_string_total, R.string.shared_string_total,
				R.drawable.widget_track_recording_uphill_day, R.drawable.widget_track_recording_uphill_night,
				R.drawable.widget_track_recording_downhill_day, R.drawable.widget_track_recording_downhill_night),
		LAST(R.string.shared_string_last_uphill, R.string.shared_string_last_downhill,
				R.drawable.widget_track_recording_last_uphill_day, R.drawable.widget_track_recording_last_uphill_night,
				R.drawable.widget_track_recording_last_downhill_day, R.drawable.widget_track_recording_last_downhill_night);

		@StringRes
		final int titleIdUphill;

		@StringRes
		final int titleIdDownhill;
		private final int uphillIconDay;
		private final int uphillIconNight;
		private final int downhillIconDay;
		private final int downhillIconNight;

		TripRecordingElevationMode(int titleIdUphill, int titleIdDownhill, int uphillIconDay, int uphillIconNight, int downhillIconDay, int downhillIconNight) {
			this.titleIdUphill = titleIdUphill;
			this.titleIdDownhill = titleIdDownhill;
			this.uphillIconDay = uphillIconDay;
			this.uphillIconNight = uphillIconNight;
			this.downhillIconDay = downhillIconDay;
			this.downhillIconNight = downhillIconNight;
		}

		@StringRes
		public int getTitleId(boolean uphill) {
			return uphill ? titleIdUphill : titleIdDownhill;
		}

		public int getIcon(boolean uphill, boolean nightMode) {
			if (uphill) {
				return nightMode ? uphillIconNight : uphillIconDay;
			} else {
				return nightMode ? downhillIconNight : downhillIconDay;
			}
		}

	}
}
