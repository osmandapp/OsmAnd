package net.osmand.plus.parkingpoint;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
	private ParkingPositionPlugin plugin;
	private LatLon latLon;
	private MapActivity mapActivity;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.parking_options)));
		Bundle args = getArguments();
		latLon = new LatLon(args.getDouble(LAT_KEY), args.getDouble(LON_KEY));
		plugin = OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class);
		BaseBottomSheetItem byTypeItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_time_start))
				.setTitle(getString(R.string.osmand_parking_no_lim_text))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						addParkingPositionByType(ParkingType.TYPE_UNLIMITED);
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
						addParkingPositionByType(ParkingType.TYPE_LIMITED);
					}
				})
				.create();
		items.add(byDateItem);
	}

	private void addParkingPositionByType(ParkingType type) {
		if (plugin != null) {
			if (type.isLimited()) {
				if (plugin.isParkingEventAdded()) {
					plugin.showDeleteEventWarning(getActivity());
				}
				plugin.addOrRemoveParkingEvent(false);
				plugin.setParkingPosition(mapActivity, latLon.getLatitude(), latLon.getLongitude(), false);
				plugin.showContextMenuIfNeeded(mapActivity, true);
				mapActivity.refreshMap();
			} else if (type.isUnlimited()) {
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

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		Context context = getActivity();
		if (context instanceof MapActivity) {
			mapActivity = (MapActivity) context;
		}
		return super.onCreateView(inflater, parent, savedInstanceState);
	}


	public enum ParkingType {
		TYPE_UNLIMITED,
		TYPE_LIMITED;

		public boolean isLimited() {
			return this == TYPE_UNLIMITED;
		}

		public boolean isUnlimited() {
			return this == TYPE_LIMITED;
		}
	}
}
