package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MENU_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.OPAQUE_ALPHA;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.RECTANGULAR_RADIUS_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_BOTTOM;
import static net.osmand.shared.grid.ButtonPositionSize.POS_LEFT;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;

public class DrawerMenuButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public DrawerMenuButtonState(@NonNull OsmandApplication app) {
		super(app, MENU_HUD_ID);
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.backToMenu);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.drawer_button_description);
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

	@Override
	public int getDefaultLayoutId() {
		return R.layout.drawer_menu_button;
	}

	@NonNull
	@Override
	public String getDefaultIconName() {
		boolean dashboard = settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get();
		return dashboard ? "ic_dashboard" : "ic_navigation_drawer";
	}

	@Override
	public float getDefaultOpacity() {
		return OPAQUE_ALPHA;
	}

	@Override
	public int getDefaultCornerRadius() {
		return RECTANGULAR_RADIUS_DP;
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_LEFT, POS_BOTTOM, true, false);
	}
}