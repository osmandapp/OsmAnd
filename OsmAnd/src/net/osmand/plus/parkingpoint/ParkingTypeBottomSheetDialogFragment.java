package net.osmand.plus.parkingpoint;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;


public class ParkingTypeBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ParkingTypeBottomSheetDialogFragment";
	public static final String LAT_KEY = "latitude";
	public static final String LON_KEY = "longitude";
	public static final int TYPE_UNLIMITED = 0;
	public static final int TYPE_LIMITED = 1;

	private LatLon latLon;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		latLon = new LatLon(args.getDouble(LAT_KEY), args.getDouble(LON_KEY));

		items.add(new TitleItem(getString(R.string.parking_options)));
		BaseBottomSheetItem byTypeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_time_start))
				.setTitle(getString(R.string.osmand_parking_no_lim_text))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addParkingPositionByType(TYPE_UNLIMITED);
					}
				})
				.create();
		items.add(byTypeItem);

		BaseBottomSheetItem byDateItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_time_span))
				.setTitle(getString(R.string.osmand_parking_time_limit))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addParkingPositionByType(TYPE_LIMITED);
					}
				})
				.create();
		items.add(byDateItem);
	}

	private void addParkingPositionByType(int type) {
		ParkingPositionPlugin plugin = OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class);
		MapActivity mapActivity = (MapActivity) getActivity();
		if (plugin != null) {
			if (type == 0) {
				if (plugin.isParkingEventAdded()) {
					plugin.showDeleteEventWarning(mapActivity);
				}
				plugin.addOrRemoveParkingEvent(false);
				plugin.setParkingPosition(mapActivity, latLon.getLatitude(), latLon.getLongitude(), false);
				plugin.showContextMenuIfNeeded(mapActivity, true);
				mapActivity.refreshMap();
			} else if (type == 1) {
				if (plugin.isParkingEventAdded()) {
					plugin.showDeleteEventWarning(mapActivity);
				}
				plugin.setParkingPosition(mapActivity, latLon.getLatitude(), latLon.getLongitude(), true);
				plugin.showSetTimeLimitDialog(mapActivity, new Dialog(getContext()));
				mapActivity.getMapView().refreshMap();
			}
		}
		dismiss();
	}
}
