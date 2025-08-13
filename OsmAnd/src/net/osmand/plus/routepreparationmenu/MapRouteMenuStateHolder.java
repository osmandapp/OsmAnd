package net.osmand.plus.routepreparationmenu;

import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.MapRouteMenuType.ROUTE_DETAILS;
import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.MapRouteMenuType.ROUTE_INFO;

import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.MapRouteMenuType;

class MapRouteMenuStateHolder {

	private final MapRouteInfoMenu menu;
	private final MapRouteMenuType type;
	private final int menuState;
	private final Bundle arguments;

	MapRouteMenuStateHolder(@NonNull MapRouteInfoMenu menu, @NonNull MapRouteMenuType type,
			int menuState, @Nullable Bundle arguments) {
		this.menu = menu;
		this.type = type;
		this.menuState = menuState;
		this.arguments = arguments;
	}

	public MapRouteMenuType getType() {
		return type;
	}

	@DrawableRes
	int getButtonImage() {
		OsmandApplication app = menu.getApp();
		return switch (type) {
			case ROUTE_INFO -> 0;
			case ROUTE_DETAILS ->
					app != null ? app.getRoutingHelper().getAppMode().getIconRes() : R.drawable.ic_action_gdirections_dark;
		};
	}

	void showMenu() {
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity != null) {
			switch (type) {
				case ROUTE_INFO -> menu.show(menuState);
				case ROUTE_DETAILS ->
						ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager(), arguments);
			}
		}
	}

	void onDismiss(@Nullable MapRouteMenuStateHolder stateHolder) {
		boolean openingRouteInfo = stateHolder != null && stateHolder.type == ROUTE_INFO;
		MapActivity mapActivity = menu.getMapActivity();
		if (mapActivity != null) {
			if (!openingRouteInfo && type == ROUTE_DETAILS && !menu.isPortraitMode()) {
				mapActivity.getMapPositionManager().setMapPositionShiftedX(false);
			}
		}
	}
}
