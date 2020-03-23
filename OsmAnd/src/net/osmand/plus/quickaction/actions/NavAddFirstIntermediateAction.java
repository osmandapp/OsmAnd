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

public class NavAddFirstIntermediateAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(22,
			"nav.intermediate.add", NavAddFirstIntermediateAction.class).
			nameRes(R.string.quick_action_add_first_intermediate).iconRes(R.drawable.ic_action_intermediate).nonEditable().
			category(QuickActionType.NAVIGATION);

	public NavAddFirstIntermediateAction() {
		super(TYPE);
	}

	public NavAddFirstIntermediateAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		LatLon latLon = activity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		activity.getMapLayers().getMapControlsLayer().addFirstIntermediate(latLon);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_first_intermediate_desc);

		parent.addView(view);
	}
}
