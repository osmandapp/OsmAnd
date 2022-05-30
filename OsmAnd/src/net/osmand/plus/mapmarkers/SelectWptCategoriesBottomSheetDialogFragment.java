package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.PointsGroup;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SelectWptCategoriesBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectWptCategoriesBottomSheetDialogFragment.class.getSimpleName();

	public static final String GPX_FILE_PATH_KEY = "gpx_file_path";
	public static final String UPDATE_CATEGORIES_KEY = "update_categories";
	public static final String ACTIVE_CATEGORIES_KEY = "active_categories";

	private GPXFile gpxFile;

	private final Set<String> selectedCategories = new HashSet<>();
	private final List<BottomSheetItemWithCompoundButton> categoryItems = new ArrayList<>();

	private boolean isUpdateMode = false;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		gpxFile = getGpxFile();
		if (gpxFile == null) {
			return;
		}
		int activeColorResId = ColorUtilities.getActiveColorId(nightMode);
		isUpdateMode = getArguments().getBoolean(UPDATE_CATEGORIES_KEY);
		List<String> categories = getArguments().getStringArrayList(ACTIVE_CATEGORIES_KEY);

		items.add(new TitleItem(getGpxName(gpxFile)));

		items.add(new ShortDescriptionItem(getString(R.string.select_waypoints_category_description)));

		Map<String, PointsGroup> pointsGroups = gpxFile.getPointsGroups();

		final BottomSheetItemWithCompoundButton[] selectAllItem = new BottomSheetItemWithCompoundButton[1];
		selectAllItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!isUpdateMode || categories != null && categories.size() == pointsGroups.size())
				.setCompoundButtonColorId(activeColorResId)
				.setDescription(getString(R.string.shared_string_total) + ": " + gpxFile.getPoints().size())
				.setIcon(getContentIcon(R.drawable.ic_action_group_select_all))
				.setTitle(getString(R.string.shared_string_select_all))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
				.setOnClickListener(v -> {
					boolean checked = !selectAllItem[0].isChecked();
					selectAllItem[0].setChecked(checked);
					for (BottomSheetItemWithCompoundButton item : categoryItems) {
						item.setChecked(checked);
					}
				})
				.create();
		items.add(selectAllItem[0]);

		items.add(new DividerItem(getContext()));

		for (Entry<String, PointsGroup> entry : pointsGroups.entrySet()) {
			String category = entry.getKey();
			final BottomSheetItemWithCompoundButton[] categoryItem = new BottomSheetItemWithCompoundButton[1];
			categoryItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(!isUpdateMode || (categories != null && categories.contains(category)))
					.setOnCheckedChangeListener((buttonView, isChecked) -> {
						if (isChecked) {
							selectedCategories.add((String) categoryItem[0].getTag());
						} else {
							selectedCategories.remove((String) categoryItem[0].getTag());
						}
					})
					.setCompoundButtonColorId(activeColorResId)
					.setDescription(String.valueOf(entry.getValue().pointsSize))
					.setIcon(getContentIcon(R.drawable.ic_action_folder))
					.setTitle(category.isEmpty() ? getString(R.string.shared_string_waypoints) : category)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setTag(category)
					.setOnClickListener(v -> {
						categoryItem[0].setChecked(!categoryItem[0].isChecked());
						selectAllItem[0].setChecked(isAllChecked());
					})
					.create();
			items.add(categoryItem[0]);
			categoryItems.add(categoryItem[0]);
			if (!isUpdateMode || categories != null && categories.contains(category)) {
				selectedCategories.add(category);
			}
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return isUpdateMode ? R.string.shared_string_update : R.string.shared_string_add;
	}

	@Override
	protected void onRightBottomButtonClick() {
		updateAddOrEnableGroupWptCategories();
		dismiss();
	}

	private void updateAddOrEnableGroupWptCategories() {
		OsmandApplication app = requiredMyApplication();
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();

		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpxFile.path);
		if (selectedGpxFile == null) {
			gpxSelectionHelper.selectGpxFile(gpxFile, true, false, false, false, false);
		}
		MapMarkersGroup group = mapMarkersHelper.getMarkersGroup(gpxFile);
		if (group == null) {
			group = mapMarkersHelper.addOrEnableGroup(gpxFile);
		}
		mapMarkersHelper.updateGroupWptCategories(group, selectedCategories);
		mapMarkersHelper.runSynchronization(group);
	}

	private boolean isAllChecked() {
		for (BottomSheetItemWithCompoundButton item : categoryItems) {
			if (!item.isChecked()) {
				return false;
			}
		}
		return true;
	}

	private String getGpxName(GPXFile gpxFile) {
		return new File(gpxFile.path).getName()
				.replace(IndexConstants.GPX_FILE_EXT, "")
				.replace("/", " ")
				.replace("_", " ");
	}

	@Nullable
	private GPXFile getGpxFile() {
		String filePath = getArguments().getString(GPX_FILE_PATH_KEY);
		if (filePath != null) {
			OsmandApplication app = requiredMyApplication();
			SelectedGpxFile selectedGpx = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
			if (selectedGpx != null && selectedGpx.getGpxFile() != null) {
				return selectedGpx.getGpxFile();
			}
			return GPXUtilities.loadGPXFile(new File(filePath));
		}
		return null;
	}
}
