package net.osmand.plus.plugins.parking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
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
	public void execute(@NonNull MapActivity mapActivity) {
		ParkingPositionPlugin plugin = PluginsHelper.getActivePlugin(ParkingPositionPlugin.class);
		if (plugin != null) {
			LatLon latLon = getMapLocation(mapActivity);
			plugin.showAddParkingDialog(mapActivity, latLon.getLatitude(), latLon.getLongitude());
		}
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);
		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_add_parking_descr);
		parent.addView(view);
	}
}
