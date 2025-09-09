package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_DIRECTIONS_FROM_ACTION_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavDirectionsFromAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_DIRECTIONS_FROM_ACTION_ID,
			"nav.directions", NavDirectionsFromAction.class)
			.nameRes(R.string.quick_action_directions_from).iconRes(R.drawable.ic_action_route_direction_from_here).nonEditable()
			.category(QuickActionType.NAVIGATION).nameActionRes(R.string.shared_string_set);

	public NavDirectionsFromAction() {
		super(TYPE);
	}

	public NavDirectionsFromAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		mapActivity.getMapActions().enterDirectionsFromPoint(latLon.getLatitude(), latLon.getLongitude());
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		return mapActivity.getMapLayers().getNavigationLayer().getStartPointIcon();
	}

	@NonNull
	@Override
	protected String getDialogTitle(@NonNull Context context) {
		return context.getString(R.string.add_start_point);
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_directions_from_desc);
	}
}
