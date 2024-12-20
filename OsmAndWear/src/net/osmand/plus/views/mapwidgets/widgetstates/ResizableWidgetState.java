package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.WidgetSize;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class ResizableWidgetState extends WidgetState {

	private final OsmandPreference<WidgetSize> widgetSizePref;
	private final WidgetType widgetType;

	public ResizableWidgetState(@NonNull OsmandApplication app, @Nullable String customId, @NonNull WidgetType widgetType) {
		super(app);
		this.widgetSizePref = registerWidgetSizePref(customId, widgetType);
		this.widgetType = widgetType;
	}

	@NonNull
	private OsmandPreference<WidgetSize> registerWidgetSizePref(@Nullable String customId, @NonNull WidgetType widgetType) {
		String prefId = "simple_widget_size";
		prefId += widgetType.id;
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, WidgetSize.MEDIUM, WidgetSize.values(), WidgetSize.class)
				.makeProfile();
	}

	@NonNull
	public OsmandPreference<WidgetSize> getWidgetSizePref() {
		return widgetSizePref;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(widgetType.titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return nightMode ? widgetType.nightIconId : widgetType.dayIconId;
	}

	@Override
	public void changeToNextState() {
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerWidgetSizePref(customId, widgetType).setModeValue(appMode, widgetSizePref.getModeValue(sourceAppMode));
	}
}