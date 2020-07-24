package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.GpxTrackAdapter;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.helpers.GpxUiHelper.getSortedGPXFilesInfo;

public class SelectFileBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SelectFileBottomSheet.class.getSimpleName();
	public static final int BOTTOM_SHEET_HEIGHT_DP = 427;

	protected View mainView;
	protected GpxTrackAdapter adapter;
	private SelectFileListener listener;
	private Map<String, List<GpxUiHelper.GPXInfo>> gpxInfoMap;

	public void setListener(SelectFileListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes),
				R.layout.bottom_sheet_plan_route_select_file, null);

		final RecyclerView filesRecyclerView = mainView.findViewById(R.id.gpx_track_list);
		filesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}

		List<File> dirs = new ArrayList<>();
		collectDirs(app.getAppPath(IndexConstants.GPX_INDEX_DIR), dirs);
		List<String> dirItems = new ArrayList<>();
		for (File dir : dirs) {
			dirItems.add(dir.getName());
		}
		String allFilesFolder = app.getResources().getString(R.string.shared_string_all);
		dirItems.add(0, allFilesFolder);

		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GpxUiHelper.GPXInfo> list = getSortedGPXFilesInfo(dir, null, false);
		final List<GpxUiHelper.GPXInfo> gpxInfoList = new ArrayList<>();
		gpxInfoMap = new HashMap<>();
		gpxInfoMap.put(allFilesFolder, new ArrayList<GpxUiHelper.GPXInfo>());
		String folderName;
		for (GpxUiHelper.GPXInfo gpxInfo : list) {
			folderName = getFolderName(gpxInfo);
			gpxInfoMap.get(allFilesFolder).add(gpxInfo);
			if (!gpxInfoMap.containsKey(folderName)) {
				gpxInfoMap.put(folderName, new ArrayList<GpxUiHelper.GPXInfo>());
			}
			gpxInfoMap.get(folderName).add(gpxInfo);
		}
		gpxInfoList.clear();
		List<GpxUiHelper.GPXInfo> gpxList = gpxInfoMap.get(allFilesFolder);
		if (gpxList != null) {
			gpxInfoList.addAll(gpxList);
		}
		adapter = new GpxTrackAdapter(requireContext(), gpxInfoList, false);
		adapter.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				if (position != RecyclerView.NO_POSITION && position < gpxInfoList.size()) {
					OsmandApplication app = (OsmandApplication) requireActivity().getApplication();
					String fileName = gpxInfoList.get(position).getFileName();
					GPXUtilities.GPXFile gpxFile = GPXUtilities.loadGPXFile(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), fileName));
					if (listener != null) {
						listener.selectFileOnCLick(gpxFile);
					}
				}
				dismiss();
			}
		});
		filesRecyclerView.setAdapter(adapter);

		final RecyclerView foldersRecyclerView = mainView.findViewById(R.id.folder_list);
		foldersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
				RecyclerView.HORIZONTAL, false));
		final HorizontalSelectionAdapter folderAdapter = new HorizontalSelectionAdapter(app, nightMode);
		folderAdapter.setItems(dirItems);
		folderAdapter.setSelectedItem(allFilesFolder);
		foldersRecyclerView.setAdapter(folderAdapter);
		folderAdapter.setListener(new HorizontalSelectionAdapter.HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				List<GpxUiHelper.GPXInfo> gpxInfoList = gpxInfoMap.get(item);
				if (gpxInfoList != null) {
					adapter.setGpxInfoList(gpxInfoList);
				} else {
					adapter.setGpxInfoList(new ArrayList<GpxUiHelper.GPXInfo>());
				}
				adapter.notifyDataSetChanged();
				folderAdapter.notifyDataSetChanged();
			}
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	private String getFolderName(GpxUiHelper.GPXInfo gpxInfo) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return "";
		}
		int fileNameStartIndex = gpxInfo.getFileName().lastIndexOf(File.separator);
		if (fileNameStartIndex != -1) {
			return gpxInfo.getFileName().substring(0, gpxInfo.getFileName().lastIndexOf(File.separator));
		} else {
			return IndexConstants.GPX_INDEX_DIR.substring(0, IndexConstants.GPX_INDEX_DIR.length() - 1);
		}
	}

	private void collectDirs(File dir, List<File> dirs) {
		File[] listFiles = dir.listFiles();
		if (listFiles != null) {
			Arrays.sort(listFiles);
			for (File f : listFiles) {
				if (f.isDirectory()) {
					dirs.add(f);
					collectDirs(f, dirs);
				}
			}
		}
	}

	@Override
	protected int getCustomHeight() {
		return AndroidUtils.dpToPx(mainView.getContext(), BOTTOM_SHEET_HEIGHT_DP);
	}

	public static void showInstance(FragmentManager fragmentManager, SelectFileListener listener) {
		if (!fragmentManager.isStateSaved()) {
			SelectFileBottomSheet fragment = new SelectFileBottomSheet();
			fragment.setUsedOnMap(true);
			fragment.setRetainInstance(true);
			fragment.setListener(listener);
			fragment.show(fragmentManager, SelectFileBottomSheet.TAG);
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

		void selectFileOnCLick(GPXUtilities.GPXFile gpxFile);

		void dismissButtonOnClick();

	}
}
