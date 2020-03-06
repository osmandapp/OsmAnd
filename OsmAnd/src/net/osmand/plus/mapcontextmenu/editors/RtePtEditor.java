package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;

public class RtePtEditor extends WptPtEditor {

	public static final String TAG = "RtePtEditorFragment";

	public RtePtEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	@Override
	public void showEditorFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RtePtEditorFragment.showInstance(mapActivity);
		}
	}

	@Override
	public void showEditorFragment(boolean skipDialog) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RtePtEditorFragment.showInstance(mapActivity, skipDialog);
		}
	}
}
