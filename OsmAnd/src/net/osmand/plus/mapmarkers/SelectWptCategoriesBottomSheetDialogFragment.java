package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SelectWptCategoriesBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SelectWptCategoriesBottomSheetDialogFragment";
	public static final String GPX_FILE_PATH_KEY = "gpx_file_path";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		GPXFile gpxFile = getGpxFile();
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
						selectAllItem[0].setChecked(!selectAllItem[0].isChecked());
					}
				})
				.create();
		items.add(selectAllItem[0]);

		items.add(new DividerItem(getContext()));

		Map<String, List<WptPt>> pointsByCategories = gpxFile.getPointsByCategories();

		for (String category : pointsByCategories.keySet()) {
			final BottomSheetItemWithCompoundButton[] categoryItem = new BottomSheetItemWithCompoundButton[1];
			categoryItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setDescription(String.valueOf(pointsByCategories.get(category).size()))
					.setIcon(getContentIcon(R.drawable.ic_action_folder)) // todo
					.setTitle(category.equals("") ? getString(R.string.waypoints) : category)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setTag(category)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							categoryItem[0].setChecked(!categoryItem[0].isChecked());
							Toast.makeText(getContext(), (String) v.getTag(), Toast.LENGTH_SHORT).show();
						}
					})
					.create();
			items.add(categoryItem[0]);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_import;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Toast.makeText(getContext(), "import", Toast.LENGTH_SHORT).show();
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
