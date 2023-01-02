package net.osmand.plus.track.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

public class GpxSelectionHelper {

	private static final Log log = PlatformUtil.getLog(GpxSelectionHelper.class);

	public static final String CURRENT_TRACK = "currentTrack";
	private static final String FILE = "file";
	private static final String BACKUP = "backup";
	private static final String BACKUPMODIFIEDTIME = "backupTime";
	private static final String COLOR = "color";
	private static final String SELECTED_BY_USER = "selected_by_user";
	private static final String HIDDEN_GROUPS = "hidden_groups";

	private final OsmandApplication app;
	private final SavingTrackHelper savingTrackHelper;
	@NonNull
	private List<SelectedGpxFile> selectedGPXFiles = new ArrayList<>();
	private final Map<GPXFile, Long> selectedGpxFilesBackUp = new HashMap<>();
	private SelectGpxTask selectGpxTask;

	public GpxSelectionHelper(OsmandApplication app, SavingTrackHelper trackHelper) {
		this.app = app;
		savingTrackHelper = trackHelper;
	}

	public void clearAllGpxFilesToShow(boolean backupSelection) {
		selectedGpxFilesBackUp.clear();
		if (backupSelection) {
			for (SelectedGpxFile s : selectedGPXFiles) {
				selectedGpxFilesBackUp.put(s.gpxFile, s.modifiedTime);
			}
		}
		selectedGPXFiles = new ArrayList<>();
		saveCurrentSelections();
	}

	public void restoreSelectedGpxFiles() {
		for (Entry<GPXFile, Long> gpxEntry : selectedGpxFilesBackUp.entrySet()) {
			if (!Algorithms.isEmpty(gpxEntry.getKey().path)) {
				File file = new File(gpxEntry.getKey().path);
				if (file.exists() && !file.isDirectory()) {
					if (file.lastModified() > gpxEntry.getValue()) {
						new GpxFileLoaderTask(file, null, result -> {
							if (result != null) {
								GpxSelectionParams params = GpxSelectionParams.newInstance()
										.showOnMap().syncGroup().selectedByUser()
										.addToMarkers().addToHistory().saveSelection();
								selectGpxFile(result, params);
							}
							return true;
						}).execute();
					} else {
						GpxSelectionParams params = GpxSelectionParams.newInstance()
								.showOnMap().selectedByUser().syncGroup()
								.addToHistory().addToMarkers().saveSelection();
						selectGpxFile(gpxEntry.getKey(), params);
					}
				}
			}
			saveCurrentSelections();
		}
	}

	@NonNull
	public List<SelectedGpxFile> getSelectedGPXFiles() {
		return selectedGPXFiles;
	}

	public Map<GPXFile, Long> getSelectedGpxFilesBackUp() {
		return selectedGpxFilesBackUp;
	}

	public boolean isAnyGpxFileSelected() {
		return !selectedGPXFiles.isEmpty();
	}

	public static boolean isGpxFileSelected(@NonNull OsmandApplication app, @Nullable GPXFile gpxFile) {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		return gpxFile != null &&
				((gpxFile.showCurrentTrack && helper.getSelectedCurrentRecordingTrack() != null) ||
						(gpxFile.path != null && helper.getSelectedFileByPath(gpxFile.path) != null));
	}

	@SuppressLint({"StringFormatInvalid", "StringFormatMatches"})
	public String getGpxDescription() {
		if (selectedGPXFiles.size() == 1) {
			GPXFile currentGPX = app.getSavingTrackHelper().getCurrentGpx();
			if (selectedGPXFiles.get(0).getGpxFile() == currentGPX) {
				return app.getString(R.string.current_track);
			}

			File file = new File(selectedGPXFiles.get(0).getGpxFile().path);
			return Algorithms.getFileNameWithoutExtension(file).replace('_', ' ');
		} else if (selectedGPXFiles.size() == 0) {
			return null;
		} else {
			return app.getString(R.string.number_of_gpx_files_selected_pattern,
					selectedGPXFiles.size());
		}
	}

	@Nullable
	public SelectedGpxFile getSelectedGPXFile(@NonNull WptPt point) {
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			if (gpxFile.containsPoint(point) || gpxFile.containsRoutePoint(point)) {
				return selectedGpxFile;
			}
		}
		return null;
	}

	@Nullable
	public SelectedGpxFile getSelectedFileByPath(String path) {
		List<SelectedGpxFile> newList = new ArrayList<>(selectedGPXFiles);
		for (SelectedGpxFile s : newList) {
			if (s.getGpxFile().path.equals(path)) {
				return s;
			}
		}
		return null;
	}

	@Nullable
	public SelectedGpxFile getSelectedFileByName(String fileName) {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.getGpxFile().path.endsWith("/" + fileName)) {
				return s;
			}
		}
		return null;
	}

	@Nullable
	public SelectedGpxFile getSelectedCurrentRecordingTrack() {
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.isShowCurrentTrack()) {
				return s;
			}
		}
		return null;
	}

	@Nullable
	public WptPt getVisibleWayPointByLatLon(@NonNull LatLon latLon) {
		for (SelectedGpxFile selectedGpx : selectedGPXFiles) {
			GPXFile gpx;
			if (selectedGpx != null && (gpx = selectedGpx.getGpxFile()) != null) {
				for (WptPt pt : gpx.getPoints()) {
					if (latLon.equals(new LatLon(pt.getLatitude(), pt.getLongitude()))) {
						return pt;
					}
				}
			}
		}
		return null;
	}

	public void setGpxFileToDisplay(GPXFile... gpxs) {
		// special case for gpx current route
		for (GPXFile gpx : gpxs) {
			GpxSelectionParams params = GpxSelectionParams.newInstance()
					.showOnMap().selectedByUser().syncGroup().addToMarkers()
					.addToHistory().saveSelection();
			selectGpxFile(gpx, params);
		}
		saveCurrentSelections();
	}

	public void loadGPXTracks(IProgress p) {
		String load = app.getSettings().SELECTED_GPX.get();
		if (!Algorithms.isEmpty(load)) {
			try {
				JSONArray ar = new JSONArray(load);
				boolean save = false;
				for (int i = 0; i < ar.length(); i++) {
					JSONObject obj = ar.getJSONObject(i);
					boolean selectedByUser = obj.optBoolean(SELECTED_BY_USER, true);
					if (obj.has(FILE)) {
						File fl = new File(obj.getString(FILE));
						if (p != null) {
							p.startTask(app.getString(R.string.loading_smth, fl.getName()), -1);
						}
						GPXFile gpx = GPXUtilities.loadGPXFile(fl);
						if (obj.has(COLOR)) {
							int color = GPXUtilities.parseColor(obj.getString(COLOR), 0);
							gpx.setColor(color);
						}
						if (gpx.error != null) {
							save = true;
						} else if (obj.has(BACKUP)) {
							selectedGpxFilesBackUp.put(gpx, gpx.modifiedTime);
						} else {
							save = true;
							GpxSelectionParams params = GpxSelectionParams.newInstance()
									.showOnMap().syncGroup().setSelectedByUser(selectedByUser);
							SelectedGpxFile file = selectGpxFile(gpx, params);
							if (obj.has(HIDDEN_GROUPS)) {
								readHiddenGroups(file, obj.getString(HIDDEN_GROUPS));
							}
						}
						gpx.addGeneralTrack();
					} else if (obj.has(CURRENT_TRACK)) {
						SelectedGpxFile file = savingTrackHelper.getCurrentTrack();
						file.selectedByUser = selectedByUser;
						updateSelected(true, file);
					}
				}
				if (save) {
					saveCurrentSelections();
				}
			} catch (Exception e) {
				app.getSettings().SELECTED_GPX.set("");
				log.error(e);
			}
		}
	}

	private String saveHiddenGroups(SelectedGpxFile selectedGpxFile) {
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<String> it = selectedGpxFile.hiddenGroups.iterator();
		while (it.hasNext()) {
			String name = it.next();
			stringBuilder.append(name != null ? name : " ");
			if (it.hasNext()) {
				stringBuilder.append(",");
			}
		}
		return stringBuilder.toString();
	}

	public void readHiddenGroups(SelectedGpxFile selectedGpxFile, String text) {
		StringTokenizer toks = new StringTokenizer(text, ",");
		Set<String> res = new HashSet<>();
		while (toks.hasMoreTokens()) {
			String token = toks.nextToken();
			if (!Algorithms.isBlank(token)) {
				res.add(token);
			} else {
				res.add(null);
			}
		}
		selectedGpxFile.hiddenGroups = res;
	}

	private void saveGpxToHistory(@NonNull GPXFile gpx) {
		String relativePath = GpxUiHelper.getGpxFileRelativePath(app, gpx.path);
		GPXInfo gpxInfo = GpxUiHelper.getGpxInfoByFileName(app, relativePath);
		if (gpxInfo != null) {
			SearchHistoryHelper.getInstance(app).addNewItemToHistory(gpxInfo);
		}
	}

	private void saveCurrentSelections() {
		JSONArray ar = new JSONArray();
		for (SelectedGpxFile s : selectedGPXFiles) {
			if (s.gpxFile != null && !s.notShowNavigationDialog) {
				JSONObject obj = new JSONObject();
				try {
					if (s.isShowCurrentTrack()) {
						obj.put(CURRENT_TRACK, true);
					} else if (!Algorithms.isEmpty(s.gpxFile.path)) {
						obj.put(FILE, s.gpxFile.path);
						if (s.gpxFile.getColor(0) != 0) {
							obj.put(COLOR, Algorithms.colorToString(s.gpxFile.getColor(0)));
						}
						obj.put(HIDDEN_GROUPS, saveHiddenGroups(s));
					}
					obj.put(SELECTED_BY_USER, s.selectedByUser);
				} catch (JSONException e) {
					log.error(e);
				}
				ar.put(obj);
			}
		}
		for (Map.Entry<GPXFile, Long> s : selectedGpxFilesBackUp.entrySet()) {
			if (s != null) {
				try {
					JSONObject obj = new JSONObject();
					if (Algorithms.isEmpty(s.getKey().path)) {
						obj.put(CURRENT_TRACK, true);
					} else {
						obj.put(FILE, s.getKey().path);
					}
					obj.put(SELECTED_BY_USER, true);
					obj.put(BACKUP, true);
					obj.put(BACKUPMODIFIEDTIME, s.getValue());
					ar.put(obj);
				} catch (JSONException e) {
					log.error(e);
				}
			}
		}
		app.getSettings().SELECTED_GPX.set(ar.toString());
	}

	public SelectedGpxFile selectGpxFile(@NonNull GPXFile gpx, @NonNull GpxSelectionParams params) {
		boolean showOnMap = params.isShowOnMap();
		boolean isCurrentRecordingTrack = gpx.showCurrentTrack;
		GpxDataItem dataItem = app.getGpxDbHelper().getItem(new File(gpx.path));
		SelectedGpxFile selectedFile = isCurrentRecordingTrack ?
				savingTrackHelper.getCurrentTrack() : getSelectedFileByPath(gpx.path);
		if (!isCurrentRecordingTrack && (showOnMap || !params.shouldUpdateSelected())) {
			if (selectedFile == null) {
				selectedFile = new SelectedGpxFile();
			}
			if (dataItem != null) {
				selectedFile.setJoinSegments(dataItem.isJoinSegments());
			}
			selectedFile.setGpxFile(gpx, app);
		}
		if (selectedFile != null) {
			if (dataItem != null && FilteredSelectedGpxFile.isGpsFiltersConfigValid(dataItem)) {
				selectedFile.createFilteredSelectedGpxFile(app, dataItem);
			}
			selectedFile.notShowNavigationDialog = params.isNotShowNavigationDialog();
			if (params.isSelectedByUserChanged()) {
				selectedFile.selectedByUser = params.isSelectedByUser();
			}
			boolean isSelected = selectedGPXFiles.contains(selectedFile);
			if (selectedFile.isLoaded() && (params.shouldUpdateSelected() && showOnMap != isSelected)) {
				updateSelected(showOnMap, selectedFile);
			}
			selectedFile.splitProcessed = false;
		}
		if (params.isAddToMarkers() && dataItem != null && dataItem.isShowAsMarkers()) {
			MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();
			mapMarkersHelper.addOrEnableGroup(gpx);
		}
		if (params.isSyncGroup()) {
			syncGpxWithMarkers(gpx);
		}
		if (params.isAddToHistory()) {
			saveGpxToHistory(gpx);
		}
		if (params.isSaveSelection()) {
			saveCurrentSelections();
		}
		return selectedFile;
	}

	private void updateSelected(boolean show, SelectedGpxFile file) {
		List<SelectedGpxFile> selectedFiles = new ArrayList<>(selectedGPXFiles);
		if (show) {
			if (!selectedFiles.contains(file)) {
				selectedFiles.add(file);
			}
		} else {
			selectedFiles.remove(file);
		}
		selectedGPXFiles = selectedFiles;
	}

	public void updateSelectedGpxFile(SelectedGpxFile selectedGpxFile) {
		if (selectedGPXFiles.contains(selectedGpxFile)) {
			saveCurrentSelections();
		}
	}

	public void clearPoints(GPXFile gpxFile) {
		gpxFile.clearPoints();
		syncGpxWithMarkers(gpxFile);
	}

	public void addPoint(WptPt point, GPXFile gpxFile) {
		gpxFile.addPoint(point);
		syncGpxWithMarkers(gpxFile);
	}

	public void addPoints(Collection<? extends WptPt> collection, GPXFile gpxFile) {
		gpxFile.addPoints(collection);
		syncGpxWithMarkers(gpxFile);
	}

	public boolean removePoint(WptPt point, GPXFile gpxFile) {
		boolean res = gpxFile.deleteWptPt(point);
		syncGpxWithMarkers(gpxFile);
		return res;
	}

	private void syncGpxWithMarkers(GPXFile gpxFile) {
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();
		MapMarkersGroup group = mapMarkersHelper.getMarkersGroup(gpxFile);
		if (group != null) {
			mapMarkersHelper.runSynchronization(group);
		}
	}

	/**
	 * @param file null if current track
	 */
	public static void getGpxFile(@NonNull Activity activity,
	                              @Nullable File file,
	                              boolean showProgress,
	                              @NonNull CallbackWithObject<GPXFile> callback) {
		OsmandApplication app = ((OsmandApplication) activity.getApplication());
		SelectedGpxFile selectedGpxFile = file == null
				? app.getSavingTrackHelper().getCurrentTrack()
				: app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
		if (selectedGpxFile != null) {
			callback.processResult(selectedGpxFile.getGpxFileToDisplay());
		} else {
			GpxFileLoaderTask.loadGpxFile(file, showProgress ? activity : null, gpxFile -> {
				callback.processResult(gpxFile);
				return true;
			});
		}
	}

	public enum GpxDisplayItemType {
		TRACK_SEGMENT,
		TRACK_POINTS,
		TRACK_ROUTE_POINTS
	}

	public void runSelection(Map<String, Boolean> selectedItems, SelectGpxTaskListener gpxTaskListener) {
		if (selectGpxTask != null && (selectGpxTask.getStatus() == AsyncTask.Status.RUNNING)) {
			selectGpxTask.cancel(false);
		}
		selectGpxTask = new SelectGpxTask(selectedItems, gpxTaskListener);
		selectGpxTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public interface SelectGpxTaskListener {

		void gpxSelectionInProgress();

		void gpxSelectionStarted();

		void gpxSelectionFinished();

	}

	@SuppressLint("StaticFieldLeak")
	public class SelectGpxTask extends AsyncTask<Void, Void, String> {

		private final Set<GPXFile> originalSelectedItems = new HashSet<>();
		private final Map<String, Boolean> selectedItems;
		private final SelectGpxTaskListener gpxTaskListener;

		SelectGpxTask(Map<String, Boolean> selectedItems, SelectGpxTaskListener gpxTaskListener) {
			this.selectedItems = selectedItems;
			this.gpxTaskListener = gpxTaskListener;
		}

		@Override
		protected String doInBackground(Void... voids) {
			for (GPXFile gpxFile : originalSelectedItems) {
				if (isCancelled()) {
					break;
				}
				if (!gpxFile.showCurrentTrack) {
					gpxFile = GPXUtilities.loadGPXFile(new File(gpxFile.path));
				}
				GpxSelectionParams params = GpxSelectionParams.newInstance()
						.showOnMap().selectedByUser().syncGroup()
						.addToHistory().addToMarkers().saveSelection();
				selectGpxFile(gpxFile, params);
				publishProgress();
			}
			return "";
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			gpxTaskListener.gpxSelectionInProgress();
		}

		@Override
		protected void onPreExecute() {
			collectSelectedItems();
			gpxTaskListener.gpxSelectionStarted();
		}

		private void collectSelectedItems() {
			for (String filePath : selectedItems.keySet()) {
				SelectedGpxFile sf;
				if (!filePath.equals(CURRENT_TRACK)) {
					sf = getSelectedFileByPath(filePath);
					if (sf == null) {
						sf = new SelectedGpxFile();
						sf.setGpxFile(new GPXFile(null), app);
					}
					sf.getGpxFile().path = filePath;
				} else {
					sf = getSelectedCurrentRecordingTrack();
					if (sf == null) {
						sf = savingTrackHelper.getCurrentTrack();
					}
				}
				boolean visible = false;
				if (selectedItems.get(filePath) != null) {
					visible = selectedItems.get(filePath);
				}
				if (visible) {
					if (!sf.isShowCurrentTrack()) {
						sf.getGpxFile().modifiedTime = -1;
						sf.getGpxFile().pointsModifiedTime = -1;
					}
					originalSelectedItems.add(sf.getGpxFile());
				}
				updateSelected(visible, sf);
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (gpxTaskListener != null) {
				gpxTaskListener.gpxSelectionFinished();
			}
		}
	}
}
