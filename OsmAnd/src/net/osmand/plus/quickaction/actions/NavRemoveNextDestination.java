package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedMenu;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavRemoveNextDestination extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(34,
			"nav.destination.remove", NavRemoveNextDestination.class)
			.nameRes(R.string.quick_action_remove_next_destination)
			.iconRes(R.drawable.ic_action_navigation_skip_destination)
			.nonEditable()
			.category(QuickActionType.NAVIGATION);

	public NavRemoveNextDestination() {
		super(TYPE);
	}

	public NavRemoveNextDestination(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		TargetPointsHelper targetsHelper = activity.getMyApplication().getTargetPointsHelper();
		if (targetsHelper.getIntermediatePoints().size() > 0) {
			targetsHelper.removeWayPoint(true, 0);
		} else {
			DestinationReachedMenu.show(activity);
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(R.string.quick_action_remove_next_destination_descr);
		parent.addView(view);
	}

	@Override
	public boolean isActionEnable(OsmandApplication app) {
		return app.getRoutingHelper().isRouteCalculated();
	}
}
