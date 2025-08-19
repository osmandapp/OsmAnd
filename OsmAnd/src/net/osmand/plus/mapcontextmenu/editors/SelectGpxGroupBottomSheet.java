package net.osmand.plus.mapcontextmenu.editors;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.utils.AndroidUtils;

public class SelectGpxGroupBottomSheet extends SelectPointsCategoryBottomSheet {

	private GpxFile gpxFile;

	@Override
	protected int getDefaultColorId() {
		return R.color.gpx_color_point;
	}

	@Nullable
	@Override
	protected PointEditor getPointEditor() {
		return ((MapActivity) requireActivity()).getContextMenu().getWptPtPointEditor();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WptPtEditor editor = (WptPtEditor) getPointEditor();
		if (editor != null) {
			gpxFile = editor.getGpxFile();
			pointsGroups.putAll(editor.getPointsGroups());
		}
	}

	@NonNull
	protected BaseBottomSheetItem createCategoriesListItem() {
		View view = inflate(R.layout.favorite_categories_dialog);
		ViewGroup container = view.findViewById(R.id.list_container);

		for (PointsGroup pointsGroup : pointsGroups.values()) {
			boolean hidden = !WptPtEditorFragment.isCategoryVisible(app, gpxFile, pointsGroup.getName());
			container.addView(createCategoryItem(pointsGroup, hidden));
		}

		return new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create();
	}

	@Override
	protected void showAddNewCategoryFragment(CategorySelectionListener listener) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			GpxGroupEditorFragment.showInstance(manager, gpxFile, null, listener);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull String selectedCategory,
	                                @Nullable CategorySelectionListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectGpxGroupBottomSheet fragment = new SelectGpxGroupBottomSheet();
			Bundle args = new Bundle();
			args.putString(KEY_SELECTED_CATEGORY, selectedCategory);

			fragment.setArguments(args);
			fragment.setListener(listener);
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}
}
