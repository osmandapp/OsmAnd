package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.NAV_REMOVE_NEXT_DESTINATION_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedFragment;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavRemoveNextDestination extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(NAV_REMOVE_NEXT_DESTINATION_ACTION_ID,
			"nav.destination.remove", NavRemoveNextDestination.class)
			.nameRes(R.string.quick_action_remove_next_destination)
			.iconRes(R.drawable.ic_action_navigation_skip_destination)
			.nonEditable()
			.category(QuickActionType.NAVIGATION)
			.nameActionRes(R.string.shared_string_remove);

	public NavRemoveNextDestination() {
		super(TYPE);
	}

	public NavRemoveNextDestination(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		TargetPointsHelper targetsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
		if (targetsHelper.getIntermediatePoints().size() > 0) {
			targetsHelper.removeWayPoint(true, 0);
		} else {
			DestinationReachedFragment.show(mapActivity);
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
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
