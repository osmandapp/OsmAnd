package net.osmand.plus.mapmarkers;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapmarkers.adapters.GroupsAdapter;
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

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
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

	@SuppressLint("StaticFieldLeak")
	public class ProcessGpxTask extends AsyncTask<Void, GpxDataItem, Void> {

		private OsmandApplication app = getMyApplication();
		private Map<File, GpxDataItem> processedDataFiles = new HashMap<>();
		private GPXDatabase db = app.getGpxDatabase();
		private ProgressBar progressBar = (ProgressBar) mainView.findViewById(R.id.progress_bar);
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
					GPXTrackAnalysis itemAnalysis = item != null ? item.getAnalysis() : null;
					if (item == null
							|| item.getFileLastModifiedTime() != gpxFile.lastModified()
							|| itemAnalysis == null
							|| itemAnalysis.wptCategoryNames == null) {
						GPXFile f = GPXUtilities.loadGPXFile(gpxFile);
						GPXTrackAnalysis analysis = f.getAnalysis(gpxFile.lastModified());
						if (item == null) {
							item = new GpxDataItem(gpxFile, analysis);
							db.add(item);
						} else {
							db.updateAnalysis(item, analysis);
						}
					}
					processedDataFiles.put(gpxFile, item);
					if (itemAnalysis != null && itemAnalysis.wptPoints > 0) {
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
			setupHeightAndBackground(getView());
		}
	}
}
