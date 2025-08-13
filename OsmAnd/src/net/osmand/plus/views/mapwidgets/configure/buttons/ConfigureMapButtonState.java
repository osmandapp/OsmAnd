package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.LAYERS_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.ROUND_RADIUS_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.SMALL_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.TRANSPARENT_ALPHA;
import static net.osmand.shared.grid.ButtonPositionSize.POS_LEFT;
import static net.osmand.shared.grid.ButtonPositionSize.POS_TOP;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;

public class ConfigureMapButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public ConfigureMapButtonState(@NonNull OsmandApplication app) {
		super(app, LAYERS_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.configure_map);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.configure_map_description);
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.configure_map_button;
	}

	@Override
	public int getDefaultSize() {
		return SMALL_SIZE_DP;
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	@Override
	public String getDefaultIconName() {
		ApplicationMode appMode = settings.getApplicationMode();
		return appMode.getIconName();
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_LEFT, POS_TOP, false, true);
	}
}