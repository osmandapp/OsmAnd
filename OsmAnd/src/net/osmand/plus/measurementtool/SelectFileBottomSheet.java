package net.osmand.plus.measurementtool;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.GpxTrackAdapter;
import net.osmand.plus.helpers.GpxTrackAdapter.OnItemClickListener;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter.HorizontalSelectionAdapterListener;
import net.osmand.util.Algorithms;

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
	private Map<String, List<GPXInfo>> gpxInfoMap;

	public void setListener(SelectFileListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		Context context = requireContext();
		OsmandApplication app = requiredMyApplication();
		mainView = View.inflate(new ContextThemeWrapper(context, themeRes),
				R.layout.bottom_sheet_plan_route_select_file, null);
		final RecyclerView filesRecyclerView = mainView.findViewById(R.id.gpx_track_list);
		filesRecyclerView.setLayoutManager(new LinearLayoutManager(context));

		List<File> dirs = new ArrayList<>();
		final File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		collectDirs(gpxDir, dirs);
		List<String> dirItems = new ArrayList<>();
		for (File dir : dirs) {
			dirItems.add(dir.getName());
		}
		String allFilesFolder = context.getString(R.string.shared_string_all);
		dirItems.add(0, allFilesFolder);

		final List<GPXInfo> allGpxList = getSortedGPXFilesInfo(gpxDir, null, false);
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
		adapter = new GpxTrackAdapter(requireContext(), allGpxList, false);
		adapter.setAdapterListener(new OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				if (position != RecyclerView.NO_POSITION && position < allGpxList.size()) {
					String fileName = allGpxList.get(position).getFileName();
					GPXFile gpxFile = GPXUtilities.loadGPXFile(new File(gpxDir, fileName));
					if (listener != null) {
						listener.selectFileOnCLick(gpxFile);
					}
				}
				dismiss();
			}
		});
		filesRecyclerView.setAdapter(adapter);

		final RecyclerView foldersRecyclerView = mainView.findViewById(R.id.folder_list);
		foldersRecyclerView.setLayoutManager(new LinearLayoutManager(context,
				RecyclerView.HORIZONTAL, false));
		final HorizontalSelectionAdapter folderAdapter = new HorizontalSelectionAdapter(app, nightMode);
		folderAdapter.setItems(dirItems);
		folderAdapter.setSelectedItem(allFilesFolder);
		foldersRecyclerView.setAdapter(folderAdapter);
		folderAdapter.setListener(new HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				List<GPXInfo> gpxInfoList = gpxInfoMap.get(item);
				adapter.setGpxInfoList(gpxInfoList != null ? gpxInfoList : new ArrayList<GPXInfo>());
				adapter.notifyDataSetChanged();
				folderAdapter.notifyDataSetChanged();
			}
		});
		items.add(new BaseBottomSheetItem.Builder().setCustomView(mainView).create());
	}

	private String getFolderName(GPXInfo gpxInfo) {
		int fileNameStartIndex = gpxInfo.getFileName().lastIndexOf(File.separator);
		return fileNameStartIndex != -1
				? gpxInfo.getFileName().substring(0, fileNameStartIndex)
				: IndexConstants.GPX_INDEX_DIR.substring(0, IndexConstants.GPX_INDEX_DIR.length() - 1);
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

		void selectFileOnCLick(GPXFile gpxFile);

		void dismissButtonOnClick();

	}
}
