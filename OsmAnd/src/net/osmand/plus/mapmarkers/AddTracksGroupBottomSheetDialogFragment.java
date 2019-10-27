package net.osmand.plus.mapmarkers;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;
import net.osmand.plus.mapmarkers.adapters.TracksGroupsAdapter;

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

	private GpxDataItemCallback gpxDataItemCallback = new GpxDataItemCallback() {
		@Override
		public boolean isCancelled() {
			ProcessGpxTask processor = asyncProcessor;
			return processor == null || processor.isCancelled();
		}

		@Override
		public void onGpxDataItemReady(GpxDataItem item) {
			populateList(item);
			if (dbHelper.isRead()) {
				onListPopulated();
			}
		}
	};

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		app = requiredMyApplication();
		dbHelper = app.getGpxDbHelper();

		progressBar = (ProgressBar) mainView.findViewById(R.id.progress_bar);
		recyclerView = (RecyclerView) mainView.findViewById(R.id.groups_recycler_view);
		lookingForTracksText = (TextView) mainView.findViewById(R.id.looking_for_tracks_text);

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
		if (analysis != null && analysis.wptCategoryNames != null && analysis.wptCategoryNames.size() > 1) {
			Bundle args = new Bundle();
			args.putString(SelectWptCategoriesBottomSheetDialogFragment.GPX_FILE_PATH_KEY, dataItem.getFile().getAbsolutePath());

			SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(false);
			fragment.show(getParentFragment().getChildFragmentManager(), SelectWptCategoriesBottomSheetDialogFragment.TAG);
		} else {
			OsmandApplication app = getMyApplication();
			if (app != null) {
				GpxSelectionHelper selectionHelper = app.getSelectedGpxHelper();
				File gpx = dataItem.getFile();
				if (selectionHelper.getSelectedFileByPath(gpx.getAbsolutePath()) == null) {
					GPXFile res = GPXUtilities.loadGPXFile(gpx);
					selectionHelper.selectGpxFile(res, true, false, false, false, false);
				}
				app.getMapMarkersHelper().addOrEnableGpxGroup(gpx);
			}
		}
		dismiss();
	}

	private void populateList(GpxDataItem item) {
		if (item != null && item.getFile() != null) {
			GPXTrackAnalysis analysis = item.getAnalysis();
			if (analysis != null && analysis.wptPoints > 0) {
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
				} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(".gpx")) {
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
