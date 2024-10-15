package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MENU_HUD_ID;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.BIG_SIZE_DP;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.OPAQUE_ALPHA;
import static net.osmand.plus.quickaction.ButtonAppearanceParams.RECTANGULAR_RADIUS_DP;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.ButtonAppearanceParams;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

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
	public ButtonAppearanceParams createDefaultAppearanceParams() {
		boolean dashboard = settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get();
		String iconName = dashboard ? "ic_dashboard" : "ic_navigation_drawer";
		return new ButtonAppearanceParams(iconName, BIG_SIZE_DP, OPAQUE_ALPHA, RECTANGULAR_RADIUS_DP);
	}
}