package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;

public class MarkerAction extends QuickAction {

	public static final int TYPE = 2;

	public MarkerAction() {
		super(TYPE);
	}

	public MarkerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {

		LatLon latLon = activity.getMapView()
				.getCurrentRotatedTileBox()
				.getCenterLatLon();

		PointDescription pointDescription = new PointDescription(
				latLon.getLatitude(),
				latLon.getLongitude());

		if (pointDescription.isLocation() && pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(activity)))
			pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");

		activity.getMapActions().addMapMarker(
				latLon.getLatitude(),
				latLon.getLongitude(),
				pointDescription,
				null);
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_marker_descr);

		parent.addView(view);
	}
}
