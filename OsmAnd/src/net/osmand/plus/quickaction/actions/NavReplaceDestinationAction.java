package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class NavReplaceDestinationAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(21,
			"nav.destination.replace", NavReplaceDestinationAction.class).
			nameRes(R.string.quick_action_replace_destination).iconRes(R.drawable.ic_action_point_add_destination).nonEditable().
			category(QuickActionType.NAVIGATION);

	public NavReplaceDestinationAction() {
		super(TYPE);
	}

	public NavReplaceDestinationAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		LatLon latLon = activity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		activity.getMapLayers().getMapControlsLayer().replaceDestination(latLon);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_replace_destination_desc);

		parent.addView(view);
	}
}
