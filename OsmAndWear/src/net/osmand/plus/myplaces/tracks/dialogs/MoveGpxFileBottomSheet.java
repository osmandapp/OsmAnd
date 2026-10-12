package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.util.Algorithms.capitalizeFirstLetter;
import static net.osmand.util.Algorithms.collectDirs;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MoveGpxFileBottomSheet extends MenuBottomSheetDialogFragment implements OnTrackFolderAddListener {

	private static final String TAG = MoveGpxFileBottomSheet.class.getSimpleName();
	private static final String SRC_FILE_KEY = "file_path_key";
	private static final String EXCLUDED_DIR_KEY = "excluded_dir_key";
	private static final String SHOW_ALL_FOLDERS_KEY = "show_all_folders_key";

	private OsmandApplication app;
	@Nullable
	private File srcFile;
	@Nullable
	private File excludedDir;
	private boolean showAllFolders;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = requiredMyApplication();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(SRC_FILE_KEY)) {
				srcFile = AndroidUtils.getSerializable(savedInstanceState, SRC_FILE_KEY, File.class);
			}
			if (savedInstanceState.containsKey(EXCLUDED_DIR_KEY)) {
				excludedDir = AndroidUtils.getSerializable(savedInstanceState, EXCLUDED_DIR_KEY, File.class);
			}
			showAllFolders = savedInstanceState.getBoolean(SHOW_ALL_FOLDERS_KEY);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.select_folder_descr))
				.setTitle(getString(R.string.shared_string_folders))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);

		View addNewFolderView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_with_descr_64dp, null);
		addNewFolderView.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height));
		AndroidUiHelper.updateVisibility(addNewFolderView.findViewById(R.id.description), false);
		BaseBottomSheetItem addNewFolderItem = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.add_new_folder))
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_64dp)
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						AddNewTrackFolderBottomSheet.showInstance(activity.getSupportFragmentManager(),
								null, null, MoveGpxFileBottomSheet.this, usedOnMap);
					}
				})
				.setCustomView(addNewFolderView)
				.create();
		items.add(addNewFolderItem);

		DividerItem dividerItem = new DividerItem(getContext());
		dividerItem.setMargins(0, 0, 0, 0);
		items.add(dividerItem);

		List<File> dirs = new ArrayList<>();
		File rootDir = app.getAppPath(GPX_INDEX_DIR);
		File fileDir = srcFile != null ? srcFile.getParentFile() : null;

		collectDirs(rootDir, dirs, excludedDir);
		if (showAllFolders || fileDir != null && !Algorithms.objectEquals(fileDir, rootDir)) {
			dirs.add(0, rootDir);
		}
		String gpxDir = rootDir.getPath();
		for (File dir : dirs) {
			String dirName = dir.getPath();
			if (dirName.startsWith(gpxDir)) {
				if (dirName.length() == gpxDir.length()) {
					dirName = dir.getName();
				} else {
					dirName = dirName.substring(gpxDir.length() + 1);
				}
			}
			String description;
			List<File> files = collectFiles(dir);
			if (Algorithms.isEmpty(files)) {
				description = getString(R.string.shared_string_empty);
			} else {
				description = String.valueOf(files.size());
			}
			BaseBottomSheetItem[] folderItem = new BaseBottomSheetItem[1];
			folderItem[0] = new BottomSheetItemWithDescription.Builder()
					.setDescription(description)
					.setTitle(capitalizeFirstLetter(dirName))
					.setIcon(getActiveIcon(R.drawable.ic_action_folder))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_64dp)
					.setOnClickListener(v -> {
						folderSelected(dir);
						dismiss();
					})
					.setTag(dir)
					.create();
			items.add(folderItem[0]);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (srcFile != null) {
			outState.putSerializable(SRC_FILE_KEY, srcFile);
		}
		if (excludedDir != null) {
			outState.putSerializable(EXCLUDED_DIR_KEY, excludedDir);
		}
		outState.putBoolean(SHOW_ALL_FOLDERS_KEY, showAllFolders);
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		File rootDir = app.getAppPath(GPX_INDEX_DIR);
		folderSelected(new File(rootDir, folderName));
		dismiss();
	}

	private void folderSelected(@NonNull File destDir) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnTrackFileMoveListener) {
			File dest = srcFile != null ? new File(destDir, srcFile.getName()) : destDir;
			((OnTrackFileMoveListener) fragment).onFileMove(srcFile, dest);
		}
	}

	public List<File> collectFiles(File parentDir) {
		List<File> files = new ArrayList<>();
		File[] listFiles = parentDir.listFiles();
		if (listFiles != null) {
			for (File file : listFiles) {
				if (!file.isDirectory()) {
					files.add(file);
				}
			}
		}
		return files;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable File srcFile,
	                                @Nullable File excludedDir, @Nullable Fragment target,
	                                boolean usedOnMap, boolean showAllFolders) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			MoveGpxFileBottomSheet fragment = new MoveGpxFileBottomSheet();
			fragment.srcFile = srcFile;
			fragment.excludedDir = excludedDir;
			fragment.showAllFolders = showAllFolders;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface OnTrackFileMoveListener {
		void onFileMove(@Nullable File src, @NonNull File dest);
	}
}