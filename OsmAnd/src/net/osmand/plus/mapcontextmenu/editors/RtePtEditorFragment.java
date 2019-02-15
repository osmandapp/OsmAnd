package net.osmand.plus.mapcontextmenu.editors;

import android.support.v4.app.DialogFragment;

import net.osmand.GPXUtilities;
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

	@Override
	protected DialogFragment createSelectCategoryDialog() {
		return SelectCategoryDialogFragment.createInstance(getEditor().getFragmentTag());
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

	@Override
	protected void addWpt(GPXUtilities.GPXFile gpx, String description, String name, String category, int color) {
		wpt = gpx.addRtePt(wpt.getLatitude(), wpt.getLongitude(),
				System.currentTimeMillis(), description, name, category, color);
	}
}
