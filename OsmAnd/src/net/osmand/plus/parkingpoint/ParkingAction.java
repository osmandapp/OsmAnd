package net.osmand.plus.parkingpoint;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class ParkingAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(7,
			"parking.add", ParkingAction.class).
			nameRes(R.string.quick_action_add_parking).iconRes(R.drawable.ic_action_parking_dark).nonEditable().
			category(QuickActionType.CREATE_CATEGORY);

	public ParkingAction() {
		super(TYPE);
	}

	public ParkingAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		ParkingPositionPlugin plugin = OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class);

		if (plugin != null) {

			LatLon latLon = activity.getMapView()
					.getCurrentRotatedTileBox()
					.getCenterLatLon();

			plugin.showAddParkingDialog(activity, latLon.getLatitude(), latLon.getLongitude());
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_parking_descr);

		parent.addView(view);
	}
}
