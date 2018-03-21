package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarkersGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectWptCategoriesBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SelectWptCategoriesBottomSheetDialogFragment";
	public static final String GPX_FILE_PATH_KEY = "gpx_file_path";

	private GPXFile gpxFile;

	private Set<String> selectedCategories = new HashSet<>();
	private List<BottomSheetItemWithCompoundButton> categoryItems = new ArrayList<>();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		gpxFile = getGpxFile();
		if (gpxFile == null) {
			return;
		}

		items.add(new TitleItem(getGpxName(gpxFile)));

		items.add(new DescriptionItem(getString(R.string.select_waypoints_category_description)));

		final BottomSheetItemWithCompoundButton[] selectAllItem = new BottomSheetItemWithCompoundButton[1];
		selectAllItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setDescription(getString(R.string.shared_string_total) + ": " + gpxFile.getPoints().size())
				.setIcon(getContentIcon(R.drawable.ic_action_group_select_all))
				.setTitle(getString(R.string.shared_string_select_all))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean checked = !selectAllItem[0].isChecked();
						selectAllItem[0].setChecked(checked);
						for (BottomSheetItemWithCompoundButton item : categoryItems) {
							item.setChecked(checked);
						}
					}
				})
				.create();
		items.add(selectAllItem[0]);

		items.add(new DividerItem(getContext()));

		Map<String, List<WptPt>> pointsByCategories = gpxFile.getPointsByCategories();

		for (String category : pointsByCategories.keySet()) {
			final BottomSheetItemWithCompoundButton[] categoryItem = new BottomSheetItemWithCompoundButton[1];
			categoryItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (isChecked) {
								selectedCategories.add((String) categoryItem[0].getTag());
							} else {
								selectedCategories.remove((String) categoryItem[0].getTag());
							}
						}
					})
					.setDescription(String.valueOf(pointsByCategories.get(category).size()))
					.setIcon(getContentIcon(R.drawable.ic_action_folder))
					.setTitle(category.equals("") ? getString(R.string.waypoints) : category)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setTag(category)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							categoryItem[0].setChecked(!categoryItem[0].isChecked());
							selectAllItem[0].setChecked(isAllChecked());
						}
					})
					.create();
			items.add(categoryItem[0]);
			categoryItems.add(categoryItem[0]);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_import;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		GpxSelectionHelper gpxSelectionHelper = app.getSelectedGpxHelper();
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();

		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpxFile.path);
		if (selectedGpxFile == null) {
			gpxSelectionHelper.selectGpxFile(gpxFile, true, false);
		}

		MapMarkersGroup markersGr = mapMarkersHelper.getOrCreateGroup(new File(gpxFile.path));
		markersGr.setWptCategories(selectedCategories);

		mapMarkersHelper.syncWithMarkers(markersGr);

		dismiss();
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
				.replace(".gpx", "")
				.replace("/", " ")
				.replace("_", " ");
	}

	@Nullable
	private GPXFile getGpxFile() {
		String filePath = getArguments().getString(GPX_FILE_PATH_KEY);
		if (filePath != null) {
			OsmandApplication app = getMyApplication();
			SelectedGpxFile selectedGpx = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
			if (selectedGpx != null && selectedGpx.getGpxFile() != null) {
				return selectedGpx.getGpxFile();
			}
			return GPXUtilities.loadGPXFile(app, new File(filePath));
		}
		return null;
	}
}
