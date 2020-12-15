package net.osmand.plus.measurementtool;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.GpxTrackAdapter;
import net.osmand.plus.helpers.GpxTrackAdapter.OnItemClickListener;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.helpers.enums.TracksSortByMode;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionItem;
import net.osmand.plus.widgets.popup.PopUpMenuHelper;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.helpers.GpxUiHelper.getSortedGPXFilesInfo;
import static net.osmand.util.Algorithms.collectDirs;

public class SelectFileBottomSheet extends MenuBottomSheetDialogFragment {

	private List<File> folders;
	private HorizontalSelectionAdapter folderAdapter;
	private GPXInfo currentlyRecording;

	enum Mode {
		OPEN_TRACK(R.string.shared_string_gpx_tracks, R.string.sort_by),
		ADD_TO_TRACK(R.string.add_to_a_track, R.string.route_between_points_add_track_desc);

		int title;
		int description;

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
	TracksSortByMode sortByMode = TracksSortByMode.BY_DATE;

	public void setFragmentMode(Mode fragmentMode) {
		this.fragmentMode = fragmentMode;
	}

	public void setListener(SelectFileListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(final Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		Context context = requireContext();
		final OsmandApplication app = requiredMyApplication();
		mainView = View.inflate(new ContextThemeWrapper(context, themeRes),
				R.layout.bottom_sheet_plan_route_select_file, null);
		TextView titleView = mainView.findViewById(R.id.title);
		titleView.setText(fragmentMode.title);
		final TextView descriptionView = mainView.findViewById(R.id.description);
		descriptionView.setText(fragmentMode.description);
		final RecyclerView filesRecyclerView = mainView.findViewById(R.id.gpx_track_list);
		filesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
		if (fragmentMode == Mode.OPEN_TRACK) {
			titleView.setText(AndroidUtils.addColon(app, fragmentMode.title));
			updateDescription(descriptionView);
		}
		final ImageButton sortButton = mainView.findViewById(R.id.sort_button);
		Drawable background = app.getUIUtilities().getIcon(R.drawable.bg_dash_line_dark,
				nightMode
						? R.color.inactive_buttons_and_links_bg_dark
						: R.color.inactive_buttons_and_links_bg_light);
		AndroidUtils.setBackground(sortButton, background);
		sortButton.setImageResource(sortByMode.getIconId());
		sortButton.setVisibility(View.VISIBLE);
		sortButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final List<PopUpMenuItem> items = new ArrayList<>();
				for (final TracksSortByMode mode : TracksSortByMode.values()) {
					items.add(new PopUpMenuItem.Builder(app)
							.setTitleId(mode.getNameId())
							.setIcon(app.getUIUtilities().getThemedIcon(mode.getIconId()))
							.setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									sortByMode = mode;
									sortButton.setImageResource(mode.getIconId());
									updateDescription(descriptionView);
									sortFolderList();
									folderAdapter.setTitledItems(getFolderNames());
									folderAdapter.notifyDataSetChanged();
									sortFileList();
									adapter.notifyDataSetChanged();
								}
							})
							.setSelected(sortByMode == mode)
							.create());
				}
				new PopUpMenuHelper.Builder(v, items, nightMode)
						.show();
			}
		});

		final File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);

		allFilesFolder = context.getString(R.string.shared_string_all);
		if (savedInstanceState == null) {
			selectedFolder = allFilesFolder;
		}
		final List<GPXInfo> allGpxList = getSortedGPXFilesInfo(gpxDir, null, false);
		currentlyRecording = new GPXInfo(getString(R.string.shared_string_currently_recording_track), 0, 0);
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

		adapter = new GpxTrackAdapter(requireContext(), allGpxList, isShowCurrentGpx(), showFoldersName());
		adapter.setAdapterListener(new OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				List<GPXInfo> gpxList = adapter.getGpxInfoList();
				if (position != RecyclerView.NO_POSITION && position < gpxList.size()) {
					String fileName;
					if (isShowCurrentGpx() && position == 0) {
						fileName = null;
					} else {
						fileName = gpxList.get(position).getFileName();
					}
					if (listener != null) {
						listener.selectFileOnCLick(fileName);
					}
				}
				dismiss();
			}
		});
		filesRecyclerView.setAdapter(adapter);

		final RecyclerView foldersRecyclerView = mainView.findViewById(R.id.folder_list);
		foldersRecyclerView.setLayoutManager(new LinearLayoutManager(context,
				RecyclerView.HORIZONTAL, false));
		folderAdapter = new HorizontalSelectionAdapter(app, nightMode);
		folders = new ArrayList<>();
		collectDirs(gpxDir, folders);
		sortFolderList();
		folderAdapter.setTitledItems(getFolderNames());
		folderAdapter.setSelectedItemByTitle(selectedFolder);
		foldersRecyclerView.setAdapter(folderAdapter);
		folderAdapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(HorizontalSelectionItem item) {
				selectedFolder = item.getTitle();
				updateFileList(folderAdapter);
			}
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
		updateFileList(folderAdapter);
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
			String string = getString(sortByMode.getNameId());
			descriptionView.setText(String.format(getString(R.string.ltr_or_rtl_combine_via_space),
					getString(fragmentMode.description),
					Character.toLowerCase(string.charAt(0)) + string.substring(1)));
		}
	}

	private void updateFileList(HorizontalSelectionAdapter folderAdapter) {
		sortFileList();
		adapter.setShowFolderName(showFoldersName());
		adapter.notifyDataSetChanged();
		folderAdapter.notifyDataSetChanged();
	}

	private void sortFolderList() {
		final Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(folders, new Comparator<File>() {
			@Override
			public int compare(File i1, File i2) {
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
					return -((time1 < time2) ? -1 : ((time1 == time2) ? 0 : 1));
				}
			}
		});
	}

	private void sortFileList() {
		List<GPXInfo> gpxInfoList = gpxInfoMap.get(selectedFolder);
		if (gpxInfoList != null) {
			sortSelected(gpxInfoList);
		}
		adapter.setGpxInfoList(gpxInfoList != null ? gpxInfoList : new ArrayList<GPXInfo>());
	}

	public void sortSelected(List<GPXInfo> gpxInfoList) {
		boolean hasRecording = gpxInfoList.remove(currentlyRecording);
		final Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(gpxInfoList, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
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
					return -((time1 < time2) ? -1 : ((time1 == time2) ? 0 : 1));
				}
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
		return fileNameStartIndex != -1
				? gpxInfo.getFileName().substring(0, fileNameStartIndex)
				: IndexConstants.GPX_INDEX_DIR.substring(0, IndexConstants.GPX_INDEX_DIR.length() - 1);
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

		void selectFileOnCLick(String fileName);

		void dismissButtonOnClick();

	}
}
