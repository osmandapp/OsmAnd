package net.osmand.plus.quickaction.actions;

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

	public static final QuickActionType TYPE = new QuickActionType(37, "route.add", RouteAction.class)
			.nameRes(R.string.plan_route_create_new_route)
			.iconRes(R.drawable.ic_action_plan_route)
			.nonEditable()
			.category(QuickActionType.CREATE_CATEGORY);

	public RouteAction() {
		super(TYPE);
	}

	public RouteAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {

		LatLon latLon = mapActivity.getMapView()
				.getCurrentRotatedTileBox()
				.getCenterLatLon();

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