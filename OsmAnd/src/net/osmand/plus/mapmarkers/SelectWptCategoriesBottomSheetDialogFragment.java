package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SelectWptCategoriesBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final String TAG = SelectWptCategoriesBottomSheetDialogFragment.class.getSimpleName();

	private static final String GPX_FILE_PATH_KEY = "gpx_file_path";
	private static final String UPDATE_CATEGORIES_KEY = "update_categories";
	private static final String ACTIVE_CATEGORIES_KEY = "active_categories";

	private GpxFile gpxFile;

	private final Set<String> selectedCategories = new HashSet<>();
	private final List<BottomSheetItemWithCompoundButton> categoryItems = new ArrayList<>();

	private boolean isUpdateMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadGpxFile();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		if (gpxFile == null) {
			return;
		}
		int activeColorResId = ColorUtilities.getActiveColorId(nightMode);
		Bundle args = requireArguments();
		isUpdateMode = args.getBoolean(UPDATE_CATEGORIES_KEY);
		List<String> categories = args.getStringArrayList(ACTIVE_CATEGORIES_KEY);

		items.add(new TitleItem(getGpxName(gpxFile)));

		items.add(new ShortDescriptionItem(getString(R.string.select_waypoints_category_description)));

		Map<String, PointsGroup> pointsGroups = gpxFile.getPointsGroups();

		BottomSheetItemWithCompoundButton[] selectAllItem = new BottomSheetItemWithCompoundButton[1];
		selectAllItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!isUpdateMode || categories != null && categories.size() == pointsGroups.size())
				.setCompoundButtonColorId(activeColorResId)
				.setDescription(getString(R.string.shared_string_total) + ": " + gpxFile.getPointsList().size())
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
			BottomSheetItemWithCompoundButton[] categoryItem = new BottomSheetItemWithCompoundButton[1];
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
					.setDescription(String.valueOf(entry.getValue().getPoints().size()))
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
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();

		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpxFile.getPath());
		if (selectedGpxFile == null) {
			GpxSelectionParams params = GpxSelectionParams.newInstance()
					.showOnMap().selectedAutomatically().saveSelection();
			gpxSelectionHelper.selectGpxFile(gpxFile, params);
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

	private String getGpxName(GpxFile gpxFile) {
		return new File(gpxFile.getPath()).getName()
				.replace(IndexConstants.GPX_FILE_EXT, "")
				.replace("/", " ")
				.replace("_", " ");
	}

	private void loadGpxFile() {
		String filePath = requireArguments().getString(GPX_FILE_PATH_KEY);
		if (filePath != null) {
			SelectedGpxFile selectedGpx = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
			if (selectedGpx != null && selectedGpx.getGpxFile() != null) {
				gpxFile = selectedGpx.getGpxFile();
			} else {
				GpxFileLoaderTask.loadGpxFile(new File(filePath), getActivity(), result -> {
					gpxFile = result;
					if (AndroidUtils.isActivityNotDestroyed(getActivity())) {
						updateMenuItems();
					}
					return true;
				});
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull String gpxFilePath) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(GPX_FILE_PATH_KEY, gpxFilePath);

			SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.show(childFragmentManager, TAG);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull String gpxFilePath,
	                                @Nullable String activeCategories,
	                                @Nullable Boolean updateCategories) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(GPX_FILE_PATH_KEY, gpxFilePath);
			if (activeCategories != null) {
				args.putString(ACTIVE_CATEGORIES_KEY, activeCategories);
			}
			if (updateCategories != null) {
				args.putBoolean(UPDATE_CATEGORIES_KEY, updateCategories);
			}

			SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.show(manager, TAG);
		}
	}
}
