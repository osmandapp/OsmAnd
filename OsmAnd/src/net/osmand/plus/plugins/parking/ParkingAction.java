package net.osmand.plus.plugins.parking;

import static net.osmand.plus.quickaction.QuickActionIds.PARKING_ACTION_ID;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.quickaction.actions.SelectMapLocationAction;

public class ParkingAction extends SelectMapLocationAction {

	public static final QuickActionType TYPE = new QuickActionType(PARKING_ACTION_ID,
			"parking.add", ParkingAction.class).
			nameRes(R.string.quick_action_parking_place).iconRes(R.drawable.ic_action_parking_dark).nonEditable().
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);

	public ParkingAction() {
		super(TYPE);
	}

	public ParkingAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity, @Nullable Bundle params) {
		if (PluginsHelper.isActive(ParkingPositionPlugin.class)) {
			super.execute(mapActivity, params);
		}
	}

	@Override
	protected void onLocationSelected(@NonNull MapActivity mapActivity, @NonNull LatLon latLon, @Nullable Bundle params) {
		ParkingPositionPlugin plugin = PluginsHelper.getActivePlugin(ParkingPositionPlugin.class);
		if (plugin != null) {
			plugin.showAddParkingDialog(mapActivity, latLon.getLatitude(), latLon.getLongitude());
		}
	}

	@Override
	@Nullable
	protected Object getLocationIcon(@NonNull MapActivity mapActivity) {
		return mapActivity.getMapLayers().getFavouritesLayer().createParkingIcon();
	}

	@NonNull
	@Override
	protected CharSequence getQuickActionDescription(@NonNull Context context) {
		return context.getString(R.string.quick_action_add_parking_descr);
	}
}
