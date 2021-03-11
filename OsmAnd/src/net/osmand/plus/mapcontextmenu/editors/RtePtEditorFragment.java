package net.osmand.plus.mapcontextmenu.editors;

import androidx.fragment.app.DialogFragment;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class RtePtEditorFragment extends WptPtEditorFragment {

	@Override
	public void assignEditor() {
		MapActivity mapActivity = getMapActivity();
		editor = mapActivity != null ? mapActivity.getContextMenu().getRtePtPointEditor() : null;
	}

	@Override
	public String getToolbarTitle() {
		return getString(R.string.save_route_point);
	}

	@Override
	protected DialogFragment createSelectCategoryDialog() {
		PointEditor editor = getEditor();
		return editor != null ? SelectFavoriteCategoryBottomSheet.createInstance(editor.getFragmentTag(), "") : null;
	}

	public static void showInstance(final MapActivity mapActivity) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		if (editor != null) {
			//int slideInAnim = editor.getSlideInAnimation();
			//int slideOutAnim = editor.getSlideOutAnimation();

			RtePtEditorFragment fragment = new RtePtEditorFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					//.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.fragmentContainer, fragment, editor.getFragmentTag())
					.addToBackStack(null).commit();
		}
	}

	public static void showInstance(final MapActivity mapActivity, boolean skipDialog) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		if (editor != null) {
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

	@Override
	protected void addWpt(GPXUtilities.GPXFile gpx, String description, String name, String category, int color) {
		WptPt wpt = getWpt();
		this.wpt = wpt != null ? gpx.addRtePt(wpt.getLatitude(), wpt.getLongitude(),
				System.currentTimeMillis(), description, name, category, color) : null;
	}
}
