package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingSlopeWidgetState.AverageSlopeMode.LAST_UPHILL;

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

public class TripRecordingSlopeWidgetState extends WidgetState {

	public static final String AVERAGE_SLOPE_WIDGET_MODE = "average_slope_widget_mode";

	private final WidgetType widgetType;
	private final OsmandPreference<AverageSlopeMode> averageSlopeModePreference;


	public TripRecordingSlopeWidgetState(@NonNull OsmandApplication app, @Nullable String customId, WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.averageSlopeModePreference = registerAverageSlopeModePreference(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	public OsmandPreference<AverageSlopeMode> getAverageSlopeModePreference() {
		return averageSlopeModePreference;
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
		AverageSlopeMode currentMode = averageSlopeModePreference.get();
		averageSlopeModePreference.set(currentMode.next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerAverageSlopeModePreference(customId).setModeValue(appMode, averageSlopeModePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<AverageSlopeMode> registerAverageSlopeModePreference(@Nullable String customId) {
		String prefId = AVERAGE_SLOPE_WIDGET_MODE;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, LAST_UPHILL,
				AverageSlopeMode.values(), AverageSlopeMode.class).makeProfile();
	}

	public enum AverageSlopeMode {

		LAST_DOWNHILL(R.string.shared_string_last_downhill, R.drawable.widget_track_recording_average_slope_downhill_day, R.drawable.widget_track_recording_average_slope_downhill_night),
		LAST_UPHILL(R.string.shared_string_last_uphill, R.drawable.widget_track_recording_average_slope_uphill_day, R.drawable.widget_track_recording_average_slope_uphill_night);

		@StringRes
		final int titleId;
		private final int dayIcon;
		private final int nightIcon;

		AverageSlopeMode(int titleId, int dayIcon, int nightIcon) {
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
		public AverageSlopeMode next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}
