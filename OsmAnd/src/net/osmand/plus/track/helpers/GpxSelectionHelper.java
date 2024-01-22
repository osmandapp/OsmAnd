package net.osmand.plus.track.helpers;

import static net.osmand.gpx.GpxParameter.FILE_LAST_MODIFIED_TIME;
import static net.osmand.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.gpx.GpxParameter.SHOW_AS_MARKERS;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.IProgress;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class GpxSelectionHelper {

	private static final Log log = PlatformUtil.getLog(GpxSelectionHelper.class);

	public static final String CURRENT_TRACK = "currentTrack";
	private static final String FILE = "file";
	private static final String BACKUP = "backup";
	private static final String BACKUP_MODIFIED_TIME = "backupTime";
	private static final String COLOR = "color";
	private static final String SELECTED_BY_USER = "selected_by_user";
	private static final String HIDDEN_GROUPS = "hidden_groups";

	private final OsmandApplication app;
	private final SavingTrackHelper savingTrackHelper;
	private final GpxDisplayHelper gpxDisplayHelper;
	@NonNull
	private List<SelectedGpxFile> selectedGPXFiles = new ArrayList<>();
	private final Map<GPXFile, Long> selectedGpxFilesBackUp = new ConcurrentHashMap<>();
	private List<WeakReference<SelectGpxTaskListener>> listeners = new ArrayList<>();
	private SelectGpxTask selectGpxTask;

	public GpxSelectionHelper(@NonNull OsmandApplication app) {
		this.app = app;
		savingTrackHelper = app.getSavingTrackHelper();
		gpxDisplayHelper = app.getGpxDisplayHelper();
	}

	public void clearAllGpxFilesToShow(boolean backupSelection) {
		selectedGpxFilesBackUp.clear();
		if (backupSelection) {
			for (SelectedGpxFile file : selectedGPXFiles) {
				selectedGpxFilesBackUp.put(file.getGpxFile(), file.getModifiedTime());

				if (gpxDisplayHelper.isSplittingTrack(file)) {
					gpxDisplayHelper.cancelTrackSplitting(file);
				}
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
					GpxSelectionParams selectionParams = GpxSelectionParams.getDefaultSelectionParams();
					if (file.lastModified() > gpxEntry.getValue()) {
						new GpxFileLoaderTask(file, null, result -> {
							if (result != null) {
								selectGpxFile(result, selectionParams);
							}
							return true;
						}).execute();
					} else {
						selectGpxFile(gpxEntry.getKey(), selectionParams);
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

	@NonNull
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

	public void addListener(@NonNull SelectGpxTaskListener listener) {
		listeners = Algorithms.updateWeakReferencesList(listeners, listener, true);
	}

	public void removeListener(@NonNull SelectGpxTaskListener listener) {
		listeners = Algorithms.updateWeakReferencesList(listeners, listener, false);
	}

	@Nullable
	public String getGpxDescription() {
		int size = selectedGPXFiles.size();
		if (size == 1) {
			GPXFile currentGPX = app.getSavingTrackHelper().getCurrentGpx();
			if (selectedGPXFiles.get(0).getGpxFile() == currentGPX) {
				return app.getString(R.string.shared_string_currently_recording_track);
			}
			File file = new File(selectedGPXFiles.get(0).getGpxFile().path);
			return Algorithms.getFileNameWithoutExtension(file).replace('_', ' ');
		} else if (size == 0) {
			return null;
		} else {
			return app.getString(R.string.number_of_gpx_files_selected_pattern, String.valueOf(size));
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
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			if (selectedGpxFile.getGpxFile().path.equals(path)) {
				return selectedGpxFile;
			}
		}
		return null;
	}


	/**
	 * @deprecated Use the {@link #getSelectedFileByPath(String filePath)} method.
	 */
	@Nullable
	public SelectedGpxFile getSelectedFileByName(String fileName) {
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			if (selectedGpxFile.getGpxFile().path.endsWith("/" + fileName)) {
				return selectedGpxFile;
			}
		}
		return null;
	}

	@Nullable
	public SelectedGpxFile getSelectedCurrentRecordingTrack() {
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			if (selectedGpxFile.isShowCurrentTrack()) {
				return selectedGpxFile;
			}
		}
		return null;
	}

	@Nullable
	public WptPt getVisibleWayPointByLatLon(@NonNull LatLon latLon) {
		for (SelectedGpxFile selectedGpx : selectedGPXFiles) {
			for (WptPt point : selectedGpx.getGpxFile().getPoints()) {
				if (MapUtils.areLatLonEqual(latLon, point.getLatitude(), point.getLongitude())) {
					return point;
				}
			}
		}
		return null;
	}

	public void setGpxFileToDisplay(GPXFile... gpxs) {
		// special case for gpx current route
		GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
		for (GPXFile gpx : gpxs) {
			selectGpxFile(gpx, params);
		}
		saveCurrentSelections();
	}

	public void loadGPXTracks(@Nullable IProgress progress) {
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
						if (progress != null) {
							progress.startTask(app.getString(R.string.loading_smth, fl.getName()), -1);
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
			SearchHistoryHelper.getInstance(app).addNewItemToHistory(gpxInfo, HistorySource.SEARCH);
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
		for (Map.Entry<GPXFile, Long> entry : selectedGpxFilesBackUp.entrySet()) {
			if (entry != null) {
				try {
					JSONObject obj = new JSONObject();
					if (Algorithms.isEmpty(entry.getKey().path)) {
						obj.put(CURRENT_TRACK, true);
					} else {
						obj.put(FILE, entry.getKey().path);
					}
					obj.put(SELECTED_BY_USER, true);
					obj.put(BACKUP, true);
					obj.put(BACKUP_MODIFIED_TIME, entry.getValue());
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
				selectedFile.setJoinSegments(dataItem.getParameter(JOIN_SEGMENTS));

				GPXTrackAnalysis analysis = dataItem.getAnalysis();
				if (analysis != null) {
					selectedFile.setTrackAnalysis(analysis);
					selectedFile.modifiedTime = dataItem.getParameter(FILE_LAST_MODIFIED_TIME);
				}
			}
			selectedFile.setGpxFile(gpx, app);
		}
		if (selectedFile != null) {
			selectedFile.notShowNavigationDialog = params.isNotShowNavigationDialog();
			if (params.isSelectedByUserChanged()) {
				selectedFile.selectedByUser = params.isSelectedByUser();
			}
			boolean isSelected = selectedGPXFiles.contains(selectedFile);
			if (selectedFile.isLoaded() && (params.shouldUpdateSelected() && showOnMap != isSelected)) {
				updateSelected(showOnMap, selectedFile);
				if (showOnMap) {
					if (dataItem != null && FilteredSelectedGpxFile.isGpsFiltersConfigValid(dataItem)) {
						selectedFile.createFilteredSelectedGpxFile(app, dataItem);
					}
				}
			}
			selectedFile.splitProcessed = false;
		}
		boolean showAsMarkers = dataItem != null ? dataItem.getParameter(SHOW_AS_MARKERS) : false;
		if (params.isAddToMarkers() && showAsMarkers) {
			app.getMapMarkersHelper().addOrEnableGroup(gpx);
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

	void updateSelected(boolean show, @NonNull SelectedGpxFile selectedGpxFile) {
		List<SelectedGpxFile> selectedFiles = new ArrayList<>(selectedGPXFiles);
		if (show) {
			if (!selectedFiles.contains(selectedGpxFile)) {
				selectedFiles.add(selectedGpxFile);
			}
		} else {
			selectedFiles.remove(selectedGpxFile);

			if (gpxDisplayHelper.isSplittingTrack(selectedGpxFile)) {
				gpxDisplayHelper.cancelTrackSplitting(selectedGpxFile);
			}
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

	public void saveTracksVisibility(@NonNull Collection<TrackItem> trackItems) {
		saveTracksVisibility(trackItems, true);
	}

	public void saveTracksVisibility(@NonNull Collection<TrackItem> trackItems, boolean clearPrevious) {
		if (clearPrevious) {
			clearAllGpxFilesToShow(true);
		}
		List<String> selectedPaths = new ArrayList<>();
		for (TrackItem item : trackItems) {
			selectedPaths.add(item.isShowCurrentTrack() ? CURRENT_TRACK : item.getPath());
		}
		runSelection(selectedPaths);
	}

	private void runSelection(@NonNull List<String> selectedPaths) {
		if (selectGpxTask != null && (selectGpxTask.getStatus() == Status.RUNNING)) {
			selectGpxTask.cancel(false);
		}
		selectGpxTask = new SelectGpxTask(app, selectedPaths, getGpxSelectionListener());
		selectGpxTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	private SelectGpxTaskListener getGpxSelectionListener() {
		return new SelectGpxTaskListener() {
			@Override
			public void onGpxSelectionStarted() {
				List<WeakReference<SelectGpxTaskListener>> selectionListeners = listeners;
				for (WeakReference<SelectGpxTaskListener> weakReference : selectionListeners) {
					SelectGpxTaskListener listener = weakReference.get();
					if (listener != null) {
						listener.onGpxSelectionStarted();
					}
				}
			}

			@Override
			public void onGpxSelectionInProgress(@NonNull SelectedGpxFile selectedGpxFile) {
				List<WeakReference<SelectGpxTaskListener>> selectionListeners = listeners;
				for (WeakReference<SelectGpxTaskListener> weakReference : selectionListeners) {
					SelectGpxTaskListener listener = weakReference.get();
					if (listener != null) {
						listener.onGpxSelectionInProgress(selectedGpxFile);
					}
				}
			}

			@Override
			public void onGpxSelectionFinished() {
				List<WeakReference<SelectGpxTaskListener>> selectionListeners = listeners;
				for (WeakReference<SelectGpxTaskListener> weakReference : selectionListeners) {
					SelectGpxTaskListener listener = weakReference.get();
					if (listener != null) {
						listener.onGpxSelectionFinished();
					}
				}
			}
		};
	}
}