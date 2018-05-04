package net.osmand.plus.liveupdates;

import android.app.Activity;
import android.os.Bundle;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class OsmLiveRestartBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = OsmLiveRestartBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.osm_live)));
		items.add(new LongDescriptionItem(getString(R.string.osm_live_thanks)));
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_restart;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.later;
	}

	@Override
	protected void onDismissButtonClickAction() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof MapActivity) {
			MapActivity.doRestart(activity);
		} else {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
	}
}

