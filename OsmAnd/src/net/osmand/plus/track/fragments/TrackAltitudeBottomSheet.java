package net.osmand.plus.track.fragments;

import static net.osmand.plus.measurementtool.MeasurementToolFragment.ATTACH_ROADS_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.CALCULATE_ONLINE_MODE;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class TrackAltitudeBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = TrackAltitudeBottomSheet.class.getSimpleName();

	private static final String SEGMENT_INDEX_KEY = "segment_index_key";

	private int segmentIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			segmentIndex = savedInstanceState.getInt(SEGMENT_INDEX_KEY, -1);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();

		items.add(new TitleItem(getString(R.string.get_altitude_data)));

		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);
		View view = inflater.inflate(R.layout.get_track_altitude_bottom_sheet, null);

		View attachRoads = view.findViewById(R.id.attach_roads);
		attachRoads.setOnClickListener(v -> {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof TrackMenuFragment) {
				((TrackMenuFragment) fragment).openPlanRoute(segmentIndex, ATTACH_ROADS_MODE);
			}
			dismiss();
		});

		View calculateOnline = view.findViewById(R.id.calculate_online);
		calculateOnline.setOnClickListener(v -> {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof TrackMenuFragment) {
				((TrackMenuFragment) fragment).openPlanRoute(segmentIndex, CALCULATE_ONLINE_MODE);
			}
			dismiss();
		});

		BaseBottomSheetItem getAltitudeButtons = new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
		items.add(getAltitudeButtons);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SEGMENT_INDEX_KEY, segmentIndex);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target, int segmentIndex) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackAltitudeBottomSheet fragment = new TrackAltitudeBottomSheet();
			fragment.segmentIndex = segmentIndex;
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}