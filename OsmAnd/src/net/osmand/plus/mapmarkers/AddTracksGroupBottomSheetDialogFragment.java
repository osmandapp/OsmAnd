package net.osmand.plus.mapmarkers;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.TracksGroupsAdapter;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.GpxDbHelper.GpxDataItemCallback;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddTracksGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private GpxDbHelper dbHelper;

	private ProcessGpxTask asyncProcessor;
	private List<GpxDataItem> gpxList;

	private ProgressBar progressBar;
	private RecyclerView recyclerView;
	private TextView lookingForTracksText;

	private final GpxDataItemCallback gpxDataItemCallback = new GpxDataItemCallback() {
		@Override
		public boolean isCancelled() {
			ProcessGpxTask processor = asyncProcessor;
			return processor == null || processor.isCancelled();
		}

		@Override
		public void onGpxDataItemReady(@NonNull GpxDataItem item) {
			populateList(item);
			if (dbHelper.isReading()) {
				onListPopulated();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbHelper = app.getGpxDbHelper();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		progressBar = mainView.findViewById(R.id.progress_bar);
		recyclerView = mainView.findViewById(R.id.groups_recycler_view);
		lookingForTracksText = mainView.findViewById(R.id.looking_for_tracks_text);

		asyncProcessor = new ProcessGpxTask();
		OsmAndTaskManager.executeTask(asyncProcessor);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (asyncProcessor != null) {
			asyncProcessor.cancel(false);
			asyncProcessor = null;
		}
	}

	@Override
	public GroupsAdapter createAdapter() {
		gpxList = new ArrayList<>();
		return new TracksGroupsAdapter(getContext(), gpxList);
	}

	@Override
	protected void onItemClick(int position) {
		GpxDataItem dataItem = gpxList.get(position - 1);
		GpxTrackAnalysis analysis = dataItem.getAnalysis();
		if (analysis != null && !Algorithms.isEmpty(analysis.getWptCategoryNames())) {
			Fragment parent = getParentFragment();
			if (parent != null) {
				FragmentManager fragmentManager = parent.getChildFragmentManager();
				String path = dataItem.getFile().absolutePath();
				SelectWptCategoriesBottomSheetDialogFragment.showInstance(fragmentManager, path);
			}
		} else {
			GpxSelectionHelper selectionHelper = app.getSelectedGpxHelper();
			File gpx = SharedUtil.jFile(dataItem.getFile());
			if (selectionHelper.getSelectedFileByPath(gpx.getAbsolutePath()) == null) {
				GpxFileLoaderTask.loadGpxFile(gpx, getActivity(), gpxFile -> {
					GpxSelectionParams params = GpxSelectionParams.newInstance()
							.showOnMap().selectedAutomatically().saveSelection();
					selectionHelper.selectGpxFile(gpxFile, params);
					app.getMapMarkersHelper().addOrEnableGpxGroup(gpx);
					return true;
				});
			} else {
				app.getMapMarkersHelper().addOrEnableGpxGroup(gpx);
			}
		}
		dismiss();
	}

	private void populateList(GpxDataItem item) {
		if (item != null) {
			GpxTrackAnalysis analysis = item.getAnalysis();
			if (analysis != null && analysis.getWptPoints() > 0) {
				int index = gpxList.indexOf(item);
				if (index != -1) {
					gpxList.set(index, item);
				} else {
					gpxList.add(item);
				}
			}
		}
	}

	private void onListPopulated() {
		asyncProcessor = null;

		adapter.notifyDataSetChanged();
		progressBar.setVisibility(View.GONE);
		lookingForTracksText.setVisibility(View.GONE);
		recyclerView.setVisibility(View.VISIBLE);
		setupHeightAndBackground(getView());
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			AddGroupBottomSheetDialogFragment fragment = new AddTracksGroupBottomSheetDialogFragment();
			fragment.setUsedOnMap(false);
			fragment.setReenterTransition(true);
			fragment.show(childFragmentManager, TAG);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ProcessGpxTask extends AsyncTask<Void, GpxDataItem, Void> {

		@Override
		protected void onPreExecute() {
			recyclerView.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			lookingForTracksText.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			File gpxPath = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			if (gpxPath.canRead()) {
				processGPXFolder(gpxPath, "");
			}
			return null;
		}

		private File[] listFilesSorted(File dir) {
			File[] listFiles = dir.listFiles();
			if (listFiles == null) {
				return new File[0];
			}
			Arrays.sort(listFiles);
			return listFiles;
		}

		private void processGPXFolder(File gpxPath, String gpxSubfolder) {
			for (File gpxFile : listFilesSorted(gpxPath)) {
				if (gpxFile.isDirectory()) {
					String sub = gpxSubfolder.isEmpty() ?
							gpxFile.getName() : gpxSubfolder + "/" + gpxFile.getName();
					processGPXFolder(gpxFile, sub);
				} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
					GpxDataItem item = dbHelper.getItem(SharedUtil.kFile(gpxFile), gpxDataItemCallback);
					publishProgress(item);
				}
				if (isCancelled()) {
					break;
				}
			}
		}

		@Override
		protected void onProgressUpdate(GpxDataItem... items) {
			GpxDataItem item = items[0];
			if (item != null) {
				populateList(item);
			}
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (dbHelper.isReading()) {
				onListPopulated();
			}
		}
	}
}
