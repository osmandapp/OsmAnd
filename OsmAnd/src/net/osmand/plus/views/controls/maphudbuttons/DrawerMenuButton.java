package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MENU_HUD_ID;
import static net.osmand.plus.dashboard.DashboardOnMap.DashboardType.DASHBOARD;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;

public class DrawerMenuButton extends MapButton {

	public DrawerMenuButton(@NonNull MapActivity mapActivity) {
		super(mapActivity, mapActivity.findViewById(R.id.map_menu_button), MENU_HUD_ID, false);
		boolean dashboard = settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get();
		setIconId(dashboard ? R.drawable.ic_dashboard : R.drawable.ic_navigation_drawer);
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setBackground(R.drawable.btn_round, R.drawable.btn_round_night);
		setOnClickListener(v -> {
			MapActivity.clearPrevActivityIntent();
			if (dashboard) {
				mapActivity.getDashboard().setDashboardVisibility(true, DASHBOARD, AndroidUtils.getCenterViewCoordinates(v));
			} else {
				mapActivity.openDrawer();
			}
		});
	}

	@Override
	protected boolean shouldShow() {
		return isShowBottomButtons() && mapActivity.isDrawerAvailable();
	}
}