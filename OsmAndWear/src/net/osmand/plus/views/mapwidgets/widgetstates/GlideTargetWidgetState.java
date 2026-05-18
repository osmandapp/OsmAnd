package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class GlideTargetWidgetState extends WidgetState {

	private static final String PREF_BASE_ID = "glide_widget_show_target_altitude";

	private final WidgetType widgetType;
	private final OsmandPreference<Boolean> preference;

	public GlideTargetWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app);
		this.widgetType = WidgetType.GLIDE_TARGET;
		this.preference = registerPreference(customId);
	}

	public OsmandPreference<Boolean> getPreference() {
		return preference;
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
		preference.set(!preference.get());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerPreference(customId).setModeValue(appMode, preference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<Boolean> registerPreference(@Nullable String customId) {
		String prefId = PREF_BASE_ID;
		if (!Algorithms.isEmpty(customId)) {
			prefId += "_" + customId;
		}
		return settings.registerBooleanPreference(prefId, false).makeProfile();
	}
}
