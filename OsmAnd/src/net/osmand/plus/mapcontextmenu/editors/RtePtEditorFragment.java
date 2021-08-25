package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
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

	public static void showInstance(@NonNull MapActivity mapActivity) {
		showInstance(mapActivity, false);
	}

	public static void showInstance(@NonNull MapActivity mapActivity, boolean skipDialog) {
		RtePtEditor editor = mapActivity.getContextMenu().getRtePtPointEditor();
		if (editor != null) {
			FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
			String tag = editor.getFragmentTag();
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, tag)) {
				RtePtEditorFragment fragment = new RtePtEditorFragment();
				fragment.skipDialog = skipDialog;
				fragmentManager.beginTransaction()
						.add(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(null)
						.commitAllowingStateLoss();
			}
		}
	}

	@Override
	protected void addWpt(GPXUtilities.GPXFile gpx, String description, String name, String category, int color) {
		WptPt wpt = getWpt();
		this.wpt = wpt != null ? gpx.addRtePt(wpt.getLatitude(), wpt.getLongitude(),
				System.currentTimeMillis(), description, name, category, color) : null;
	}
}
