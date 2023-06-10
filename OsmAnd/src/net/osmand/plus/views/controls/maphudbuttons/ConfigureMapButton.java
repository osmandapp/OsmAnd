package net.osmand.plus.views.controls.maphudbuttons;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;

import androidx.annotation.NonNull;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.LAYERS_HUD_ID;

public class ConfigureMapButton extends MapButton {

	public ConfigureMapButton(@NonNull MapActivity mapActivity) {
		super(mapActivity, mapActivity.findViewById(R.id.map_layers_button), LAYERS_HUD_ID);
		setBackground(R.drawable.btn_inset_circle_trans, R.drawable.btn_inset_circle_night);
		setOnClickListener(v -> {
			MapActivity.clearPrevActivityIntent();
			mapActivity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, AndroidUtils.getCenterViewCoordinates(v));
		});
	}

	@Override
	protected void updateState(boolean nightMode) {
		ApplicationMode appMode = settings.getApplicationMode();
		setIconColor(appMode.getProfileColor(nightMode));
		setIconId(appMode.getIconRes());
	}

	@Override
	protected boolean shouldShow() {
		return !isRouteDialogOpened() && widgetsVisibilityHelper.shouldShowTopButtons();
	}

	@Override
	public void refresh() {
		updateVisibility(shouldShow());
	}
}