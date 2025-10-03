package net.osmand.plus.plugins.monitoring.widgets;

import static net.osmand.plus.plugins.monitoring.widgets.TripRecordingMaxSpeedWidgetState.MaxSpeedMode.TOTAL;

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

public class TripRecordingMaxSpeedWidgetState extends WidgetState {

	public static final String MAX_SPEED_WIDGET_MODE = "max_speed_widget_mode";

	private final WidgetType widgetType;
	private final OsmandPreference<MaxSpeedMode> maxSpeedModePreference;


	public TripRecordingMaxSpeedWidgetState(@NonNull OsmandApplication app, @Nullable String customId, WidgetType widgetType) {
		super(app);
		this.widgetType = widgetType;
		this.maxSpeedModePreference = registerMaxSpeedModePreference(customId);
	}

	@NonNull
	public WidgetType getWidgetType() {
		return widgetType;
	}

	public OsmandPreference<MaxSpeedMode> getMaxSpeedModePreference() {
		return maxSpeedModePreference;
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
		MaxSpeedMode currentMode = maxSpeedModePreference.get();
		maxSpeedModePreference.set(currentMode.next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerMaxSpeedModePreference(customId).setModeValue(appMode, maxSpeedModePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<MaxSpeedMode> registerMaxSpeedModePreference(@Nullable String customId) {
		String prefId = MAX_SPEED_WIDGET_MODE;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, TOTAL,
				MaxSpeedMode.values(), MaxSpeedMode.class).makeProfile();
	}

	public enum MaxSpeedMode {

		TOTAL(R.string.shared_string_total, R.drawable.widget_track_recording_max_speed_day, R.drawable.widget_track_recording_max_speed_night),
		LAST_DOWNHILL(R.string.shared_string_last_downhill, R.drawable.widget_track_recording_max_speed_last_downhill_day, R.drawable.widget_track_recording_max_speed_last_downhill_night),
		LAST_UPHILL(R.string.shared_string_last_uphill, R.drawable.widget_track_recording_max_speed_last_uphill_day, R.drawable.widget_track_recording_max_speed_last_uphill_night);

		@StringRes
		final int titleId;
		private final int dayIcon;
		private final int nightIcon;

		MaxSpeedMode(int titleId, int dayIcon, int nightIcon) {
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
		public MaxSpeedMode next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}
