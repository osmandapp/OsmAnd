package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_ADD_DESTINATION_ACTION_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavAddDestinationAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_ADD_DESTINATION_ACTION_ID,
			"nav.destination.add", NavAddDestinationAction.class).
			nameRes(R.string.quick_action_destination).iconRes(R.drawable.ic_action_point_add_destination).nonEditable().
			category(QuickActionType.NAVIGATION).nameActionRes(R.string.shared_string_set);

	public NavAddDestinationAction() {
		super(TYPE);
	}

	public NavAddDestinationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon) {
		mapActivity.getMapActions().addDestination(latLon);
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		return mapActivity.getMapLayers().getNavigationLayer().getPointToNavigateIcon();
	}

	@NonNull
	@Override
	protected String getDialogTitle(@NonNull Context context) {
		return context.getString(R.string.add_destination_point);
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_add_destination_desc);
	}
}
