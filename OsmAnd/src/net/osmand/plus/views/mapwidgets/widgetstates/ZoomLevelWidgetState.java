package net.osmand.plus.views.mapwidgets.widgetstates;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class ZoomLevelWidgetState extends WidgetState {

	private final OsmandPreference<ZoomLevelType> typePreference;

	public ZoomLevelWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app);
		typePreference = registerTypePreference(customId);
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(getZoomLevelType().titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return WidgetType.DEV_ZOOM_LEVEL.getIconId(nightMode);
	}

	@Override
	public void changeToNextState() {
		typePreference.set(getZoomLevelType().next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerTypePreference(customId).setModeValue(appMode, getZoomLevelType());
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerTypePreference(customId).setModeValue(appMode, typePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	public OsmandPreference<ZoomLevelType> getZoomLevelTypePref() {
		return typePreference;
	}

	@NonNull
	public ZoomLevelType getZoomLevelType() {
		return typePreference.get();
	}

	@NonNull
	private OsmandPreference<ZoomLevelType> registerTypePreference(@Nullable String customId) {
		String prefId = "zoom_level_type";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, ZoomLevelType.ZOOM, ZoomLevelType.values(), ZoomLevelType.class).makeProfile();
	}

	public enum ZoomLevelType {

		ZOOM(R.string.map_widget_zoom_level),
		MAP_SCALE(R.string.map_widget_map_scale);

		@StringRes
		public final int titleId;

		ZoomLevelType(@StringRes int titleId) {
			this.titleId = titleId;
		}

		@NonNull
		public ZoomLevelType next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}
