package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.ROUTE_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class RouteAction extends QuickAction {

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
	public void execute(@NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);
		MeasurementToolFragment.showInstance(mapActivity.getSupportFragmentManager(), latLon);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_route_descr);
		parent.addView(view);
	}
}