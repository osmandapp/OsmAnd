package net.osmand.plus.myplaces;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.util.Algorithms.capitalizeFirstLetter;
import static net.osmand.util.Algorithms.collectDirs;

public class MoveGpxFileBottomSheet extends MenuBottomSheetDialogFragment implements OnTrackFolderAddListener {

	public static final String TAG = MoveGpxFileBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(MoveGpxFileBottomSheet.class);
	private static final String FILE_PATH_KEY = "file_path_key";
	private static final String SHOW_ALL_FOLDERS_KEY = "show_all_folders_key";

	private OsmandApplication app;
	private String filePath;
	private boolean showAllFolders = false;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		if (savedInstanceState != null) {
			filePath = savedInstanceState.getString(FILE_PATH_KEY);
			showAllFolders = savedInstanceState.getBoolean(SHOW_ALL_FOLDERS_KEY);
		}
		if (filePath == null) {
			return;
		}
		final File file = new File(filePath);
		final File fileDir = file.getParentFile();

		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.select_folder_descr))
				.setTitle(getString(R.string.shared_string_folders))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);

		View addNewFolderView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.bottom_sheet_item_with_descr_64dp, null);
		addNewFolderView.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height));
		AndroidUiHelper.updateVisibility(addNewFolderView.findViewById(R.id.description), false);
		BaseBottomSheetItem addNewFolderItem = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.add_new_folder))
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_64dp)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							AddNewTrackFolderBottomSheet.showInstance(activity.getSupportFragmentManager(),
									MoveGpxFileBottomSheet.this, usedOnMap);
						}
					}
				})
				.setCustomView(addNewFolderView)
				.create();
		items.add(addNewFolderItem);

		DividerItem dividerItem = new DividerItem(app);
		dividerItem.setMargins(0, 0, 0, 0);
		items.add(dividerItem);

		final List<File> dirs = new ArrayList<>();
		collectDirs(app.getAppPath(IndexConstants.GPX_INDEX_DIR), dirs, showAllFolders ? null : fileDir);
		if (showAllFolders || !Algorithms.objectEquals(fileDir, app.getAppPath(IndexConstants.GPX_INDEX_DIR))) {
			dirs.add(0, app.getAppPath(IndexConstants.GPX_INDEX_DIR));
		}
		String gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR).getPath();
		for (final File dir : dirs) {
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
			final BaseBottomSheetItem[] folderItem = new BaseBottomSheetItem[1];
			folderItem[0] = new BottomSheetItemWithDescription.Builder()
					.setDescription(description)
					.setTitle(capitalizeFirstLetter(dirName))
					.setIcon(getActiveIcon(R.drawable.ic_action_folder))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_64dp)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Fragment fragment = getTargetFragment();
							if (fragment instanceof OnTrackFileMoveListener) {
								OnTrackFileMoveListener listener = (OnTrackFileMoveListener) fragment;
								listener.onFileMove(file, new File(dir, file.getName()));
							}
							dismiss();
						}
					})
					.setTag(dir)
					.create();
			items.add(folderItem[0]);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(FILE_PATH_KEY, filePath);
		outState.putBoolean(SHOW_ALL_FOLDERS_KEY, showAllFolders);
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnTrackFileMoveListener) {
			File file = new File(filePath);
			File destFolder = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), folderName);
			OnTrackFileMoveListener listener = (OnTrackFileMoveListener) fragment;
			listener.onFileMove(file, new File(destFolder, file.getName()));
		}
		dismiss();
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

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target,
									@NonNull String filePath, boolean usedOnMap, boolean showAllFolders) {
		try {
			if (!fragmentManager.isStateSaved() && fragmentManager.findFragmentByTag(MoveGpxFileBottomSheet.TAG) == null) {
				MoveGpxFileBottomSheet fragment = new MoveGpxFileBottomSheet();
				fragment.filePath = filePath;
				fragment.setUsedOnMap(usedOnMap);
				fragment.showAllFolders = showAllFolders;
				fragment.setTargetFragment(target, 0);
				fragment.show(fragmentManager, MoveGpxFileBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public interface OnTrackFileMoveListener {
		void onFileMove(@NonNull File src, @NonNull File dest);
	}
}