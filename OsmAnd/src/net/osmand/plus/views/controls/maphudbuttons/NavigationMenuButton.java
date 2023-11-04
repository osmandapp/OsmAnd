package net.osmand.plus.views.controls.maphudbuttons;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.ROUTE_PLANNING_HUD_ID;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapControlsLayer;

public class NavigationMenuButton extends MapButton {

	private final RoutingHelper routingHelper;

	public NavigationMenuButton(@NonNull MapActivity mapActivity) {
		super(mapActivity, mapActivity.findViewById(R.id.map_route_info_button), ROUTE_PLANNING_HUD_ID);
		routingHelper = app.getRoutingHelper();
		setIconColorId(R.color.map_button_icon_color_light, R.color.map_button_icon_color_dark);
		setBackground(R.drawable.btn_round, R.drawable.btn_round_night);
		setOnClickListener(v -> {
			mapActivity.getFragmentsHelper().dismissCardDialog();
			MapControlsLayer mapControlsLayer = app.getOsmandMap().getMapLayers().getMapControlsLayer();
			if (mapControlsLayer != null) {
				mapControlsLayer.doRoute();
			}
		});
	}

	@Override
	protected void updateState(boolean nightMode) {
		int routePlanningBtnImage = mapActivity.getMapRouteInfoMenu().getRoutePlanningBtnImage();
		boolean planningRoute = routingHelper.isRoutePlanningMode();
		boolean calculatingRoute = routingHelper.isRouteBeingCalculated();
		boolean routeCalculated = routingHelper.isRouteCalculated();
		if (routePlanningBtnImage != 0) {
			setIconId(routePlanningBtnImage);
			setIconColorId(R.color.color_myloc_distance);
		} else if (routingHelper.isFollowingMode()) {
			setIconId(R.drawable.ic_action_start_navigation);
			setIconColorId(R.color.color_myloc_distance);
		} else if (planningRoute || calculatingRoute || routeCalculated) {
			setIconId(R.drawable.ic_action_gdirections_dark);
			setIconColorId(R.color.color_myloc_distance);
		} else {
			setIconId(R.drawable.ic_action_gdirections_dark);
			resetIconColors();
		}
	}

	@Override
	protected boolean shouldShow() {
		return isShowBottomButtons();
	}

	@Override
	protected void setDrawable(@NonNull Drawable drawable) {
		super.setDrawable(AndroidUtils.getDrawableForDirection(app, drawable));
	}
}