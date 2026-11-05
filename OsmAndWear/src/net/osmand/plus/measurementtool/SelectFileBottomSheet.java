package net.osmand.plus.measurementtool;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.track.helpers.GpxUiHelper.getSortedGPXFilesInfo;
import static net.osmand.util.Algorithms.collectDirs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.GpxTrackAdapter;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectFileBottomSheet extends MenuBottomSheetDialogFragment {

	private List<File> folders;
	private GPXInfo currentlyRecording;
	private HorizontalChipsView folderSelector;

	enum Mode {
		OPEN_TRACK(R.string.shared_string_gpx_tracks, R.string.sort_by),
		ADD_TO_TRACK(R.string.add_to_a_track, R.string.route_between_points_add_track_desc);

		final int title;
		final int description;

		Mode(@StringRes int title, @StringRes int description) {
			this.title = title;
			this.description = description;
		}

		public int getTitle() {
			return title;
		}

		public int getDescription() {
			return description;
		}
	}

	public static final String TAG = SelectFileBottomSheet.class.getSimpleName();
	public static final int BOTTOM_SHEET_HEIGHT_DP = 427;

	protected View mainView;
	protected GpxTrackAdapter adapter;
	private SelectFileListener listener;
	private Map<String, List<GPXInfo>> gpxInfoMap;
	private Mode fragmentMode;
	private String selectedFolder;
	private String allFilesFolder;
	private TracksSortByMode sortByMode = TracksSortByMode.BY_DATE;

	public void setFragmentMode(Mode fragmentMode) {
		this.fragmentMode = fragmentMode;
	}

	public void setListener(SelectFileListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		Context context = requireContext();
		OsmandApplication app = requiredMyApplication();
		mainView = View.inflate(new ContextThemeWrapper(context, themeRes),
				R.layout.bottom_sheet_plan_route_select_file, null);
		TextView titleView = mainView.findViewById(R.id.title);
		titleView.setText(fragmentMode.title);
		TextView descriptionView = mainView.findViewById(R.id.description);
		descriptionView.setText(fragmentMode.description);
		RecyclerView filesRecyclerView = mainView.findViewById(R.id.gpx_track_list);
		filesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
		if (fragmentMode == Mode.OPEN_TRACK) {
			titleView.setText(AndroidUtils.addColon(app, fragmentMode.title));
			updateDescription(descriptionView);
		}
		ImageButton sortButton = mainView.findViewById(R.id.sort_button);
		int backgroundColorId = ColorUtilities.getInactiveButtonsAndLinksColorId(nightMode);
		Drawable background = app.getUIUtilities().getIcon(R.drawable.bg_dash_line_dark, backgroundColorId);
		AndroidUtils.setBackground(sortButton, background);
		sortButton.setImageResource(sortByMode.getIconId());
		sortButton.setVisibility(View.VISIBLE);
		sortButton.setOnClickListener(v -> showTracksSortPopUpMenu(app, v, sortButton, descriptionView));

		File gpxDir = app.getAppPath(GPX_INDEX_DIR);

		allFilesFolder = context.getString(R.string.shared_string_all);
		if (savedInstanceState == null) {
			selectedFolder = allFilesFolder;
		}
		List<GPXInfo> allGpxList = getSortedGPXFilesInfo(gpxDir, null, false);
		currentlyRecording = new GPXInfo(getString(R.string.shared_string_currently_recording_track), null);
		if (isShowCurrentGpx()) {
			allGpxList.add(0, currentlyRecording);
		}
		gpxInfoMap = new HashMap<>();
		gpxInfoMap.put(allFilesFolder, allGpxList);
		for (GPXInfo gpxInfo : allGpxList) {
			String folderName = getFolderName(gpxInfo);
			List<GPXInfo> gpxList = gpxInfoMap.get(folderName);
			if (gpxList == null) {
				gpxList = new ArrayList<>();
				gpxInfoMap.put(folderName, gpxList);
			}
			gpxList.add(gpxInfo);
		}

		adapter = new GpxTrackAdapter(requireContext(), allGpxList);
		adapter.setShowCurrentGpx(isShowCurrentGpx());
		adapter.setShowFolderName(showFoldersName());
		adapter.setAdapterListener(position -> {
			List<GPXInfo> gpxList = adapter.getGpxInfoList();
			if (position != RecyclerView.NO_POSITION && position < gpxList.size()) {
				String filePath;
				if (isShowCurrentGpx() && position == 0) {
					filePath = null;
				} else {
					filePath = gpxList.get(position).getFilePath();
				}
				if (listener != null) {
					listener.selectFileOnCLick(filePath);
				}
			}
			dismiss();
		});
		filesRecyclerView.setAdapter(adapter);

		folderSelector = mainView.findViewById(R.id.folder_list);
		folders = new ArrayList<>();
		collectDirs(gpxDir, folders);
		sortFolderList();
		folderSelector.setItems(getFolderChips());
		ChipItem selected = folderSelector.getChipById(selectedFolder);
		folderSelector.setSelected(selected);
		folderSelector.setOnSelectChipListener(chip -> {
			selectedFolder = chip.id;
			folderSelector.smoothScrollTo(chip);
			updateFileList();
			return true;
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
		updateFileList();
	}

	private void showTracksSortPopUpMenu(@NonNull OsmandApplication app, @NonNull View anchorView,
	                                     @NonNull ImageButton sortButton, @NonNull TextView tvDesc) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();
		for (TracksSortByMode mode : TracksSortByMode.values()) {
			menuItems.add(new PopUpMenuItem.Builder(app)
					.setTitleId(mode.getNameId())
					.setIcon(getContentIcon(mode.getIconId()))
					.setOnClickListener(v -> {
						sortByMode = mode;
						sortButton.setImageResource(mode.getIconId());
						updateDescription(tvDesc);
						sortFolderList();
						folderSelector.setItems(getFolderChips());
						folderSelector.notifyDataSetChanged();
						sortFileList();
						adapter.notifyDataSetChanged();
					})
					.setSelected(sortByMode == mode)
					.create());
		}
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = anchorView;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	@NonNull
	private List<ChipItem> getFolderChips() {
		List<ChipItem> chipItems = new ArrayList<>();
		for (String name : getFolderNames()) {
			ChipItem item = new ChipItem(name);
			item.title = name;
			item.contentDescription = name;
			chipItems.add(item);
		}
		return chipItems;
	}

	private List<String> getFolderNames() {
		List<String> folderNames = new ArrayList<>();
		folderNames.add(allFilesFolder);
		for (File folder : folders) {
			folderNames.add(folder.getName());
		}
		return folderNames;
	}

	private void updateDescription(TextView descriptionView) {
		if (fragmentMode == Mode.OPEN_TRACK) {
			String sortName = getString(sortByMode.getNameId());
			descriptionView.setText(String.format(getString(R.string.ltr_or_rtl_combine_via_space),
					getString(fragmentMode.description),
					Character.toLowerCase(sortName.charAt(0)) + sortName.substring(1)));
		}
	}

	private void updateFileList() {
		sortFileList();
		adapter.setShowFolderName(showFoldersName());
		adapter.notifyDataSetChanged();
		folderSelector.notifyDataSetChanged();
	}

	private void sortFolderList() {
		Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(folders, (i1, i2) -> {
			if (sortByMode == TracksSortByMode.BY_NAME_ASCENDING) {
				return collator.compare(i1.getName(), i2.getName());
			} else if (sortByMode == TracksSortByMode.BY_NAME_DESCENDING) {
				return -collator.compare(i1.getName(), i2.getName());
			} else {
				long time1 = i1.lastModified();
				long time2 = i2.lastModified();
				if (time1 == time2) {
					return collator.compare(i1.getName(), i2.getName());
				}
				return -(Long.compare(time1, time2));
			}
		});
	}

	private void sortFileList() {
		List<GPXInfo> gpxInfoList = gpxInfoMap.get(selectedFolder);
		if (gpxInfoList != null) {
			sortSelected(gpxInfoList);
		}
		adapter.setGpxInfoList(gpxInfoList != null ? gpxInfoList : new ArrayList<>());
	}

	public void sortSelected(List<GPXInfo> gpxInfoList) {
		boolean hasRecording = gpxInfoList.remove(currentlyRecording);
		Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(gpxInfoList, (i1, i2) -> {
			if (sortByMode == TracksSortByMode.BY_NAME_ASCENDING) {
				return collator.compare(i1.getFileName(), i2.getFileName());
			} else if (sortByMode == TracksSortByMode.BY_NAME_DESCENDING) {
				return -collator.compare(i1.getFileName(), i2.getFileName());
			} else {
				long time1 = i1.getLastModified();
				long time2 = i2.getLastModified();
				if (time1 == time2) {
					return collator.compare(i1.getFileName(), i2.getFileName());
				}
				return -(Long.compare(time1, time2));
			}
		});
		if (hasRecording) {
			gpxInfoList.add(0, currentlyRecording);
		}
	}

	private boolean showFoldersName() {
		return allFilesFolder.equals(selectedFolder);
	}

	private boolean isShowCurrentGpx() {
		return fragmentMode == Mode.ADD_TO_TRACK;
	}

	private String getFolderName(GPXInfo gpxInfo) {
		int fileNameStartIndex = gpxInfo.getFileName().lastIndexOf(File.separator);
		return fileNameStartIndex == -1 ? GPX_INDEX_DIR.substring(0, GPX_INDEX_DIR.length() - 1)
				: gpxInfo.getFileName().substring(0, fileNameStartIndex);
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	@Override
	protected boolean useExpandableList() {
		return true;
	}

	@Override
	protected int getCustomHeight() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			int screenHeight = AndroidUtils.getScreenHeight(activity);
			int statusBarHeight = AndroidUtils.getStatusBarHeight(activity);
			int navBarHeight = AndroidUtils.getNavBarHeight(activity);
			int buttonsHeight = getResources().getDimensionPixelSize(R.dimen.dialog_button_ex_height);

			return screenHeight - statusBarHeight - buttonsHeight - navBarHeight - getResources().getDimensionPixelSize(R.dimen.toolbar_height);
		}
		return super.getCustomHeight();
	}

	public static void showInstance(FragmentManager fragmentManager, SelectFileListener listener, Mode mode) {
		if (!fragmentManager.isStateSaved()) {
			SelectFileBottomSheet fragment = new SelectFileBottomSheet();
			fragment.setRetainInstance(true);
			fragment.setListener(listener);
			fragment.setFragmentMode(mode);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected void onDismissButtonClickAction() {
		if (listener != null) {
			listener.dismissButtonOnClick();
		}
	}

	interface SelectFileListener {

		void selectFileOnCLick(String filePath);

		void dismissButtonOnClick();

	}
}
