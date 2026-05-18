package net.osmand.plus.settings.bottomsheets;

import static net.osmand.IndexConstants.AVOID_ROADS_FILE_EXT;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.QuadRect;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.avoidroads.DirectionPointsHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AvoidRoadsPreferencesBottomSheet extends MultiSelectPreferencesBottomSheet {

	private static final String ENABLED_FILES_IDS = "enabled_files_ids";

	private OsmandApplication app;
	private DirectionPointsHelper pointsHelper;

	private final List<String> enabledFiles = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		pointsHelper = app.getAvoidSpecificRoads().getPointsHelper();

		List<String> selectedFileNames;
		if (savedInstanceState != null) {
			selectedFileNames = savedInstanceState.getStringArrayList(ENABLED_FILES_IDS);
		} else {
			selectedFileNames = pointsHelper.getSelectedFilesForMode(getAppMode());
		}
		if (!Algorithms.isEmpty(selectedFileNames)) {
			enabledFiles.addAll(selectedFileNames);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		super.createMenuItems(savedInstanceState);
		createAvoidRoadsFilesItems();
	}

	private void createAvoidRoadsFilesItems() {
		List<File> avoidRoadsFiles = pointsHelper.collectAvoidRoadsFiles();
		if (!Algorithms.isEmpty(avoidRoadsFiles)) {
			items.add(new SubtitleDividerItem(app));
			items.add(new TitleItem(getString(R.string.files_with_route_restrictions)));

			LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);

			for (File file : avoidRoadsFiles) {
				String fileName = file.getName();
				String name = capitalizeFirstLetter(fileName.replace(AVOID_ROADS_FILE_EXT, ""));
				boolean enabled = enabledFiles.contains(fileName);

				View itemView = inflater.inflate(R.layout.bottom_sheet_item_with_switch_and_dialog, null, false);
				AndroidUiHelper.updateVisibility(itemView.findViewById(R.id.divider), false);

				BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
				item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
						.setChecked(enabled)
						.setTitle(name)
						.setIcon(getActiveIcon(R.drawable.ic_action_file_report))
						.setCustomView(itemView)
						.setOnClickListener(v -> {
							boolean checked = !item[0].isChecked();
							if (checked) {
								enabledFiles.add(fileName);
							} else {
								enabledFiles.remove(fileName);
							}
							item[0].setChecked(checked);
						})
						.create();

				pointsHelper.getDirectionPointsForFileAsync(file, result -> {
					int size = result.queryInBox(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE), new ArrayList<>()).size();

					String roads = getString(R.string.roads);
					String used = getString(enabled ? R.string.shared_string_used : R.string.shared_string_not_used);
					String roadsCount = getString(R.string.ltr_or_rtl_combine_via_colon, roads.toLowerCase(), String.valueOf(size));
					String description = getString(R.string.ltr_or_rtl_combine_via_bold_point, used, roadsCount);
					item[0].setDescription(description);
					return true;
				});
				items.add(item[0]);
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(ENABLED_FILES_IDS, (ArrayList<String>) enabledFiles);
	}

	@Override
	protected void onRightBottomButtonClick() {
		super.onRightBottomButtonClick();
		pointsHelper.setSelectedFilesForMode(getAppMode(), enabledFiles);
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @NonNull String prefId,
	                                   @Nullable Fragment target,
	                                   @Nullable ApplicationMode appMode,
	                                   boolean usedOnMap,
	                                   boolean profileDependent) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			AvoidRoadsPreferencesBottomSheet fragment = new AvoidRoadsPreferencesBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setProfileDependent(profileDependent);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
			return true;
		}
		return false;
	}
}
