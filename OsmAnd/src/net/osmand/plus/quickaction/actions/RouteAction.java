package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.ROUTE_ACTION_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class RouteAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(ROUTE_ACTION_ID, "route.add", RouteAction.class)
			.nameRes(R.string.quick_action_new_route)
			.iconRes(R.drawable.ic_action_plan_route)
			.nonEditable()
			.category(QuickActionType.MY_PLACES)
			.nameActionRes(R.string.shared_string_create);

	public RouteAction() {
		super(TYPE);
	}

	public RouteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), latLon);
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		return null;
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_add_route_descr);
	}
}