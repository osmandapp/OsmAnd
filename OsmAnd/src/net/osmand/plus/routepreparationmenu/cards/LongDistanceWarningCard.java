package net.osmand.plus.routepreparationmenu.cards;

import android.support.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.AddPointBottomSheetDialog;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;

public class LongDistanceWarningCard extends WarningCard {

	public LongDistanceWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		imageId = R.drawable.ic_action_waypoint;
		title = mapActivity.getString(R.string.route_is_too_long_v2);
		linkText = mapActivity.getString(R.string.add_intermediate);
	}

	@Override
	protected void onLinkClicked() {
		AddPointBottomSheetDialog.showInstance(mapActivity, MapRouteInfoMenu.PointType.INTERMEDIATE);
	}
}
