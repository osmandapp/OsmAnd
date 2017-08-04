package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.plus.activities.MapActivity;

public class RtePtEditor extends WptPtEditor {

	public static final String TAG = "RtePtEditorFragment";

	public RtePtEditor(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	@Override
	public void showEditorFragment() {
		RtePtEditorFragment.showInstance(mapActivity);
	}

	@Override
	public void showEditorFragment(boolean skipDialog) {
		RtePtEditorFragment.showInstance(mapActivity, skipDialog);
	}
}
