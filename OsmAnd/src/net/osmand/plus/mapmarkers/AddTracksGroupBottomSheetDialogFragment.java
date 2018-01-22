package net.osmand.plus.mapmarkers;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.GPXTrackAnalysis;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper.MarkersSyncGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.adapters.TracksGroupsAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTracksGroupBottomSheetDialogFragment extends AddGroupBottomSheetDialogFragment {

	private ProcessGpxTask asyncProcessor;
	private List<GpxDataItem> gpxList;
	private GpxSelectionHelper gpxSelectionHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxSelectionHelper = getMyApplication().getSelectedGpxHelper();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		asyncProcessor = new ProcessGpxTask();
		asyncProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void createAdapter() {
		gpxList = new ArrayList<>();
		adapter = new TracksGroupsAdapter(getContext(), gpxList);
	}

	@Override
	public MarkersSyncGroup createMapMarkersSyncGroup(int position) {
		GpxDataItem gpxDataItem = gpxList.get(position - 1);
		File gpx = gpxDataItem.getFile();
		SelectedGpxFile selectedGpxFile = gpxSelectionHelper.getSelectedFileByPath(gpx.getAbsolutePath());
		if (selectedGpxFile == null) {
			GPXFile res = GPXUtilities.loadGPXFile(getContext(), gpx);
			gpxSelectionHelper.selectGpxFile(res, true, false, false);
		}
		return new MarkersSyncGroup(gpx.getAbsolutePath(), AndroidUtils.trimExtension(gpx.getName()), MarkersSyncGroup.GPX_TYPE);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (asyncProcessor != null) {
			asyncProcessor.cancel(false);
			asyncProcessor = null;
		}
	}

	public class ProcessGpxTask extends AsyncTask<Void, GpxDataItem, Void> {

		private OsmandApplication app = getMyApplication();
		private Map<File, GpxDataItem> processedDataFiles = new HashMap<>();
		private GPXDatabase db = app.getGpxDatabase();
		private ProgressBar progressBar = (ProgressBar) mainView.findViewById(R.id.progress_bar);;
		private RecyclerView recyclerView = (RecyclerView) mainView.findViewById(R.id.groups_recycler_view);
		private TextView lookingForTracksText = (TextView) mainView.findViewById(R.id.looking_for_tracks_text);

		ProcessGpxTask() {
			List<GpxDataItem> dataItems = db.getItems();
			for (GpxDataItem item : dataItems) {
				processedDataFiles.put(item.getFile(), item);
			}
		}

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
					GpxDataItem item = processedDataFiles.get(gpxFile);
					if (item == null || item.getFileLastModifiedTime() != gpxFile.lastModified()) {
						GPXFile f = GPXUtilities.loadGPXFile(app, gpxFile);
						GPXTrackAnalysis analysis = f.getAnalysis(gpxFile.lastModified());
						if (item == null) {
							item = new GpxDataItem(gpxFile, analysis);
							db.add(item);
						} else {
							db.updateAnalysis(item, analysis);
						}
					}
					processedDataFiles.put(gpxFile, item);
					if (item.getAnalysis().wptPoints > 0) {
						gpxList.add(item);
					}
				}
				if (isCancelled()) {
					break;
				}
			}
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			asyncProcessor = null;
			adapter.notifyDataSetChanged();
			progressBar.setVisibility(View.GONE);
			lookingForTracksText.setVisibility(View.GONE);
			recyclerView.setVisibility(View.VISIBLE);
		}
	}
}
