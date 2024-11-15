package net.osmand.plus.quickaction.actions;

import static net.osmand.plus.quickaction.QuickActionIds.MARKER_ACTION_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;

public class MarkerAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(MARKER_ACTION_ID,
			"marker.add", MarkerAction.class)
			.nameRes(R.string.map_marker).iconRes(R.drawable.ic_action_flag).nonEditable().
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);


	public MarkerAction() {
		super(TYPE);
	}

	public MarkerAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		LatLon latLon = getMapLocation(mapActivity);

		PointDescription pointDescription = new PointDescription(
				latLon.getLatitude(),
				latLon.getLongitude());

		if (pointDescription.isLocation() && pointDescription.getName().equals(PointDescription.getAddressNotFoundStr(mapActivity)))
			pointDescription = new PointDescription(PointDescription.POINT_TYPE_LOCATION, "");

		mapActivity.getMapActions().addMapMarker(
				latLon.getLatitude(),
				latLon.getLongitude(),
				pointDescription,
				null);
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_marker_descr);

		parent.addView(view);
	}
}
