package net.osmand.plus.mapcontextmenu.editors;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class RtePtEditorFragment extends WptPtEditorFragment {

	@Override
	public void assignEditor() {
		editor = getMapActivity().getContextMenu().getRtePtPointEditor();
	}

	@Override
	public String getToolbarTitle() {
		return getMapActivity().getResources().getString(R.string.save_route_point);
	}

	public static void showInstance(final MapActivity mapActivity) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		RtePtEditorFragment fragment = new RtePtEditorFragment();
		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
				.addToBackStack(null).commit();
	}

	public static void showInstance(final MapActivity mapActivity, boolean skipDialog) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		//int slideInAnim = editor.getSlideInAnimation();
		//int slideOutAnim = editor.getSlideOutAnimation();

		RtePtEditorFragment fragment = new RtePtEditorFragment();
		fragment.skipDialog = skipDialog;

		mapActivity.getSupportFragmentManager().beginTransaction()
				//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
				.addToBackStack(null).commit();
	}
}
