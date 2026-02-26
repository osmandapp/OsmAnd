package net.osmand.plus.importfiles.ui;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

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
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectTrackDirectoryBottomSheet extends MenuBottomSheetDialogFragment implements OnTrackFolderAddListener {

	public static final String TAG = SelectTrackDirectoryBottomSheet.class.getSimpleName();

	private static final String SELECTED_DIRECTORY_KEY = "selected_directory_key";
	private static final String SUGGESTED_DIRECTORY_KEY = "suggested_directory_key";

	private OsmandApplication app;

	private String selectedFolder;
	private String suggestedDirName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();

		if (savedInstanceState != null) {
			selectedFolder = savedInstanceState.getString(SELECTED_DIRECTORY_KEY);
			suggestedDirName = savedInstanceState.getString(SUGGESTED_DIRECTORY_KEY);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);

		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.select_category_descr))
				.setTitle(getString(R.string.all_groups))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);

		items.add(new DividerSpaceItem(app, AndroidUtils.dpToPx(app, 12)));
		createAddFolderItem(inflater);
		items.add(new SimpleDividerItem(app));
		createFoldersItem(inflater);
	}

	private void createAddFolderItem(@NonNull LayoutInflater inflater) {
		View view = inflater.inflate(R.layout.bottom_sheet_item_simple_pad_32dp, null);
		TextView title = view.findViewById(R.id.title);
		title.setTypeface(FontCache.getMediumFont());

		BaseBottomSheetItem item = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.favorite_category_add_new))
				.setTitleColorId(ColorUtilities.getActiveColorId(nightMode))
				.setIcon(getActiveIcon(R.drawable.ic_action_folder_add))
				.setOnClickListener(v -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						AddNewTrackFolderBottomSheet.showInstance(manager, null, suggestedDirName, this, usedOnMap);
					}
				})
				.setCustomView(view)
				.create();
		items.add(item);
	}

	private void createFoldersItem(@NonNull LayoutInflater inflater) {
		List<File> folders = new ArrayList<>();
		File gpxDir = app.getAppPath(GPX_INDEX_DIR);
		folders.add(gpxDir);
		Algorithms.collectDirs(gpxDir, folders);

		View view = inflater.inflate(R.layout.favorite_categories_dialog, null);
		LinearLayout container = view.findViewById(R.id.list_container);

		for (File dir : folders) {
			container.addView(createFolderView(inflater, dir));
		}
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create());
	}

	private View createFolderView(@NonNull LayoutInflater inflater, @NonNull File folder) {
		View view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		AndroidUtils.setPadding(view, 0, 0, 0, 0);

		TextView text = view.findViewById(R.id.title);
		text.setText(GpxHelper.INSTANCE.getGpxDirTitle(folder.getName()));

		int count = getGpxFilesCount(folder);
		TextView description = view.findViewById(R.id.description);
		description.setText(count > 0 ? String.valueOf(count) : getString(R.string.shared_string_empty));

		RadioButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(Algorithms.stringsEqual(selectedFolder, folder.getName()));
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), compoundButton);

		view.setOnClickListener(v -> {
			Fragment target = getTargetFragment();
			if (target instanceof FolderSelectionListener) {
				((FolderSelectionListener) target).onFolderSelected(folder);
			}
			dismiss();
		});

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_folder));

		int padding = AndroidUtils.dpToPx(app, 8f);
		AndroidUtils.setPadding(view.findViewById(R.id.icon), 0, 0, padding, 0);

		return view;
	}

	public static int getGpxFilesCount(@NonNull File folder) {
		int count = 0;
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().toLowerCase().endsWith(GPX_FILE_EXT)) {
					count++;
				}
			}
		}
		return count;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_DIRECTORY_KEY, selectedFolder);
		outState.putString(SUGGESTED_DIRECTORY_KEY, suggestedDirName);
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		Fragment target = getTargetFragment();
		if (target instanceof OnTrackFolderAddListener) {
			((OnTrackFolderAddListener) target).onTrackFolderAdd(folderName);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String selectedDir,
	                                @NonNull String suggestedDirName, @Nullable Fragment target,
	                                boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectTrackDirectoryBottomSheet fragment = new SelectTrackDirectoryBottomSheet();
			fragment.selectedFolder = selectedDir;
			fragment.suggestedDirName = suggestedDirName;
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface FolderSelectionListener {

		void onFolderSelected(@NonNull File folder);
	}
}