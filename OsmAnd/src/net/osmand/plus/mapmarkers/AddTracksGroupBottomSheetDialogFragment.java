package net.osmand.plus.mapmarkers;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.IndexConstants;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.TracksGroupsAdapter;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxFileLoaderTask;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddTracksGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private OsmandApplication app;
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
			if (dbHelper.isRead()) {
				onListPopulated();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		dbHelper = app.getGpxDbHelper();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		progressBar = mainView.findViewById(R.id.progress_bar);
		recyclerView = mainView.findViewById(R.id.groups_recycler_view);
		lookingForTracksText = mainView.findViewById(R.id.looking_for_tracks_text);

		asyncProcessor = new ProcessGpxTask();
		asyncProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
		GPXTrackAnalysis analysis = dataItem.getAnalysis();
		if (analysis != null && !Algorithms.isEmpty(analysis.getWptCategoryNames())) {
			Bundle args = new Bundle();
			args.putString(SelectWptCategoriesBottomSheetDialogFragment.GPX_FILE_PATH_KEY, dataItem.getFile().getAbsolutePath());

			SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.show(getParentFragment().getChildFragmentManager(), SelectWptCategoriesBottomSheetDialogFragment.TAG);
		} else {
			GpxSelectionHelper selectionHelper = app.getSelectedGpxHelper();
			File gpx = dataItem.getFile();
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
			GPXTrackAnalysis analysis = item.getAnalysis();
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
					String sub = gpxSubfolder.length() == 0 ?
							gpxFile.getName() : gpxSubfolder + "/" + gpxFile.getName();
					processGPXFolder(gpxFile, sub);
				} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
					GpxDataItem item = dbHelper.getItem(gpxFile, gpxDataItemCallback);
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
			if (dbHelper.isRead()) {
				onListPopulated();
			}
		}
	}
}
