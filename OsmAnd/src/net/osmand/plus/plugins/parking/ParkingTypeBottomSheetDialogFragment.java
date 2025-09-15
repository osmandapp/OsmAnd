package net.osmand.plus.plugins.parking;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;


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
				.setOnClickListener(v -> addParkingPosition(false))
				.create();
		items.add(byTypeItem);

		BaseBottomSheetItem byDateItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_time_span))
				.setTitle(getString(R.string.osmand_parking_time_limit))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> addParkingPosition(true))
				.create();

		items.add(byDateItem);
	}

	private void addParkingPosition(boolean limited) {
		Bundle args = getArguments();
		MapActivity mapActivity = (MapActivity) getActivity();
		ParkingPositionPlugin plugin = PluginsHelper.getActivePlugin(ParkingPositionPlugin.class);
		if (args != null && mapActivity != null && plugin != null) {
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
			mapActivity.getApp().getFavoritesHelper().setParkingPoint(plugin.getParkingPosition(), null, plugin.getParkingTime(), plugin.isParkingEventAdded());
			if (!limited) {
				plugin.showContextMenuIfNeeded(mapActivity, true);
			}
			dismiss();
		}
	}

	public static void showInstance(@NonNull FragmentActivity activity, double lat, double lon) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putDouble(LAT_KEY, lat);
			args.putDouble(LON_KEY, lon);

			ParkingTypeBottomSheetDialogFragment fragment = new ParkingTypeBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
		}
	}

}
