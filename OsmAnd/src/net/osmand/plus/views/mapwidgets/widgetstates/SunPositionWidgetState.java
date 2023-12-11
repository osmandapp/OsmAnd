package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.util.Algorithms;

public class SunPositionWidgetState extends WidgetState {

	private final OsmandPreference<SunPositionMode> preference;

	public SunPositionWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app);
		this.preference = registerPreference(customId);
	}

	@NonNull
	public OsmandPreference<SunPositionMode> getPreference() {
		return preference;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(R.string.shared_string_mode);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return -1;
	}

	@Override
	public void changeToNextState() {
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerPreference(customId).setModeValue(appMode, preference.getModeValue(appMode));
	}

	@NonNull
	private OsmandPreference<SunPositionMode> registerPreference(@Nullable String customId) {
		String prefId = "sun_position_widget_mode";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, SunPositionMode.SUN_POSITION_MODE, SunPositionMode.values(), SunPositionMode.class)
				.makeProfile();
	}

	public enum SunPositionMode {
		SUN_POSITION_MODE(R.string.shared_string_next_event),
		SUNSET_MODE(R.string.shared_string_sunset),
		SUNRISE_MODE(R.string.shared_string_sunrise);

		@StringRes
		final int prefId;

		SunPositionMode(int prefId) {
			this.prefId = prefId;
		}

		@StringRes
		public int getPrefId() {
			return prefId;
		}
	}
}
