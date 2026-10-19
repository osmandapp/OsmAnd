package net.osmand.plus.plugins.parking;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import net.osmand.plus.plugins.PluginsHelper;
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

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.parking_options)));

		BaseBottomSheetItem byTypeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_time_start))
				.setTitle(getString(R.string.osmand_parking_no_lim_text))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addParkingPosition(false);
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
						addParkingPosition(true);
					}
				})
				.create();

		items.add(byDateItem);
	}

	private void addParkingPosition(boolean limited) {
		ParkingPositionPlugin plugin = PluginsHelper.getActivePlugin(ParkingPositionPlugin.class);
		if (plugin != null) {
			MapActivity mapActivity = (MapActivity) getActivity();
			Bundle args = getArguments();
			double latitude = args.getDouble(LAT_KEY);
			double longitude = args.getDouble(LON_KEY);

			if (plugin.isParkingEventAdded()) {
				plugin.showDeleteEventWarning(mapActivity);
			}

			if (limited) {
				plugin.setParkingPosition(latitude, longitude, true);
				plugin.showSetTimeLimitDialog(mapActivity, new Dialog(mapActivity));
			} else {
				plugin.addOrRemoveParkingEvent(false);
				plugin.setParkingPosition(latitude, longitude, false);
			}
			mapActivity.refreshMap();
			mapActivity.getMyApplication().getFavoritesHelper().setParkingPoint(plugin.getParkingPosition(), null, plugin.getParkingTime(), plugin.isParkingEventAdded());
			if (!limited) {
				plugin.showContextMenuIfNeeded(mapActivity, true);
			}
			dismiss();
		}
	}
}
