package net.osmand.plus.track.helpers;

import static net.osmand.shared.gpx.GpxParameter.FILE_LAST_MODIFIED_TIME;
import static net.osmand.shared.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_AS_MARKERS;

import android.app.Activity;
import android.os.AsyncTask.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.Collator;
import net.osmand.IProgress;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxDbUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxUtilities;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.io.KFile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
	private final Map<String, BackupSelection> selectedGpxFilesBackUp = new ConcurrentHashMap<>();
	private final Map<String, PendingSelection> pendingSelections =
			Collections.synchronizedMap(new LinkedHashMap<>());
	private final AtomicBoolean pendingRestoreRunning = new AtomicBoolean();
	private final AtomicInteger restoreGeneration = new AtomicInteger();
	private long pendingRestoreStartTime;
	private final Collator collator = OsmAndCollator.primaryCollator();
	private List<WeakReference<SelectGpxTaskListener>> listeners = new ArrayList<>();
	private SelectGpxTask selectGpxTask;

	public GpxSelectionHelper(@NonNull OsmandApplication app) {
		this.app = app;
		savingTrackHelper = app.getSavingTrackHelper();
		gpxDisplayHelper = app.getGpxDisplayHelper();
	}

	public void clearAllGpxFilesToShow(boolean backupSelection) {
		List<PendingSelection> pendingSnapshot;
		synchronized (pendingSelections) {
			pendingSnapshot = new ArrayList<>(pendingSelections.values());
		}
		cancelPendingRestore();
		selectedGpxFilesBackUp.clear();
		if (backupSelection) {
			for (SelectedGpxFile file : selectedGPXFiles) {
				BackupSelection backup = BackupSelection.fromGpxFile(
						file.getGpxFile(), file.getModifiedTime(), file.selectedByUser);
				selectedGpxFilesBackUp.put(backup.key, backup);

				if (gpxDisplayHelper.isSplittingTrack(file)) {
					gpxDisplayHelper.cancelTrackSplitting(file);
				}
			}
			for (PendingSelection selection : pendingSnapshot) {
				File file = new File(selection.path);
				BackupSelection backup = BackupSelection.fromPath(
						selection.path, file.lastModified(), selection.selectedByUser);
				selectedGpxFilesBackUp.put(backup.key, backup);
			}
		}
		for (SelectedGpxFile file : selectedGPXFiles) {
			file.cancelPendingFullAnalysis();
			FilteredSelectedGpxFile filtered = file.getFilteredSelectedGpxFile();
			if (filtered != null) {
				app.getGpsFilterHelper().cancelFiltering(filtered);
			}
		}
		selectedGPXFiles = new ArrayList<>();
		saveCurrentSelections();
	}

	public void restoreSelectedGpxFiles() {
		int generation = restoreGeneration.incrementAndGet();
		synchronized (pendingSelections) {
			pendingSelections.clear();
		}
		List<BackupSelection> backups = new ArrayList<>(selectedGpxFilesBackUp.values());
		selectedGpxFilesBackUp.clear();
		for (BackupSelection backup : backups) {
			if (!backup.currentTrack) {
				File file = new File(backup.path);
				if (file.isFile()) {
					if (backup.gpxFile == null || file.lastModified() != backup.modifiedTime) {
						PendingSelection selection = new PendingSelection(
								backup.path, backup.selectedByUser, null, null, generation);
						synchronized (pendingSelections) {
							pendingSelections.put(selection.key, selection);
						}
					} else {
						selectGpxFile(backup.gpxFile, GpxSelectionParams.getDefaultSelectionParams()
								.setSelectedByUser(backup.selectedByUser));
					}
				}
			} else {
				selectGpxFile(savingTrackHelper.getCurrentTrack().gpxFile,
						GpxSelectionParams.getDefaultSelectionParams()
								.setSelectedByUser(backup.selectedByUser));
			}
		}
		saveCurrentSelections();
		startPendingGpxRestore();
	}

	@NonNull
	public List<SelectedGpxFile> getSelectedGPXFiles() {
		return selectedGPXFiles;
	}

	@NonNull
	public List<GpxFile> getBackupSelectedGpxFiles() {
		List<GpxFile> files = new ArrayList<>();
		for (BackupSelection backup : selectedGpxFilesBackUp.values()) {
			files.add(backup.getDisplayGpxFile(savingTrackHelper));
		}
		return files;
	}

	@NonNull
	@Deprecated
	public Map<GpxFile, Long> getSelectedGpxFilesBackUp() {
		Map<GpxFile, Long> files = new LinkedHashMap<>();
		for (BackupSelection backup : selectedGpxFilesBackUp.values()) {
			files.put(backup.getDisplayGpxFile(savingTrackHelper), backup.modifiedTime);
		}
		return files;
	}

	public boolean isAnyGpxFileSelected() {
		if (!selectedGPXFiles.isEmpty()) {
			return true;
		}
		synchronized (pendingSelections) {
			return !pendingSelections.isEmpty();
		}
	}

	public static boolean isGpxFileSelected(@NonNull OsmandApplication app, @Nullable GpxFile gpxFile) {
		GpxSelectionHelper helper = app.getSelectedGpxHelper();
		return gpxFile != null &&
				((gpxFile.isShowCurrentTrack() && helper.getSelectedCurrentRecordingTrack() != null) ||
						(gpxFile.getPath() != null && (helper.getSelectedFileByPath(gpxFile.getPath()) != null
								|| helper.isPendingSelection(gpxFile.getPath()))));
	}

	public boolean isPendingSelection(@NonNull String path) {
		synchronized (pendingSelections) {
			return pendingSelections.containsKey(path);
		}
	}

	public void cancelPendingSelection(@NonNull String path) {
		boolean removed;
		synchronized (pendingSelections) {
			removed = pendingSelections.remove(path) != null;
		}
		removed |= selectedGpxFilesBackUp.remove(path) != null;
		if (removed) {
			saveCurrentSelections();
		}
	}

	public void renamePendingSelection(@NonNull String oldPath, @NonNull String newPath) {
		PendingSelection renamed = null;
		synchronized (pendingSelections) {
			PendingSelection previous = pendingSelections.remove(oldPath);
			if (previous != null) {
				renamed = new PendingSelection(newPath, previous.selectedByUser,
						previous.color, previous.hiddenGroups, restoreGeneration.get());
				pendingSelections.put(renamed.key, renamed);
			}
		}
		BackupSelection renamedBackup = null;
		BackupSelection previousBackup = selectedGpxFilesBackUp.remove(oldPath);
		if (previousBackup != null) {
			GpxFile backupFile = previousBackup.gpxFile;
			if (backupFile != null) {
				backupFile.setPath(newPath);
			}
			renamedBackup = new BackupSelection(newPath, false, previousBackup.modifiedTime,
					previousBackup.selectedByUser, backupFile);
			selectedGpxFilesBackUp.put(renamedBackup.key, renamedBackup);
		}
		if (renamed != null || renamedBackup != null) {
			saveCurrentSelections();
			if (renamed != null && !app.isApplicationInitializing()) {
				startPendingGpxRestore();
			}
		}
	}

	public void addListener(@NonNull SelectGpxTaskListener listener) {
		listeners = Algorithms.updateWeakReferencesList(listeners, listener, true);
	}

	public void removeListener(@NonNull SelectGpxTaskListener listener) {
		listeners = Algorithms.updateWeakReferencesList(listeners, listener, false);
	}

	@Nullable
	public String getGpxDescription() {
		List<PendingSelection> pendingSnapshot;
		synchronized (pendingSelections) {
			pendingSnapshot = new ArrayList<>(pendingSelections.values());
		}
		int size = selectedGPXFiles.size() + pendingSnapshot.size();
		if (size == 1) {
			if (!selectedGPXFiles.isEmpty()) {
				GpxFile currentGPX = app.getSavingTrackHelper().getCurrentGpx();
				if (selectedGPXFiles.get(0).getGpxFile() == currentGPX) {
					return app.getString(R.string.shared_string_currently_recording_track);
				}
				File file = new File(selectedGPXFiles.get(0).getGpxFile().getPath());
				return Algorithms.getFileNameWithoutExtension(file).replace('_', ' ');
			}
			File file = new File(pendingSnapshot.get(0).path);
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
			GpxFile gpxFile = selectedGpxFile.getGpxFile();
			if (gpxFile.containsPoint(point) || gpxFile.containsRoutePoint(point)) {
				return selectedGpxFile;
			}
		}
		return null;
	}

	@Nullable
	public SelectedGpxFile getSelectedFileByPath(String path) {
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			if (selectedGpxFile.getGpxFile().getPath().equals(path)) {
				return selectedGpxFile;
			}
		}
		return null;
	}

	@Nullable
	public GpxFile getBackupedFileByPath(@NonNull String path) {
		BackupSelection backup = selectedGpxFilesBackUp.get(path);
		if (backup == null || backup.gpxFile == null) {
			return null;
		}
		File file = new File(path);
		boolean modified = file.lastModified() != backup.modifiedTime;
		return file.isFile() && !modified ? backup.gpxFile : null;
	}

	@NonNull
	public List<SelectedGpxFile> getSelectedFilesByDir(@NonNull String dirPath) {
		List<SelectedGpxFile> list = new ArrayList<>();
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			if (selectedGpxFile.getGpxFile().getPath().startsWith(dirPath)) {
				list.add(selectedGpxFile);
			}
		}
		return list;
	}

	/**
	 * @deprecated Use the {@link #getSelectedFileByPath(String filePath)} method.
	 */
	@Nullable
	@Deprecated
	public SelectedGpxFile getSelectedFileByName(String fileName) {
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			if (selectedGpxFile.getGpxFile().getPath().endsWith("/" + fileName)) {
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
			for (WptPt point : selectedGpx.getGpxFile().getPointsList()) {
				if (MapUtils.areLatLonEqual(latLon, point.getLatitude(), point.getLongitude())) {
					return point;
				}
			}
		}
		return null;
	}

	public void setGpxFileToDisplay(GpxFile... gpxs) {
		// special case for gpx current route
		GpxSelectionParams params = GpxSelectionParams.getDefaultSelectionParams();
		for (GpxFile gpx : gpxs) {
			selectGpxFile(gpx, params);
		}
		saveCurrentSelections();
	}

	public void loadGPXTracks(@Nullable IProgress progress) {
		String load = app.getSettings().SELECTED_GPX.get();
		int generation = restoreGeneration.incrementAndGet();
		synchronized (pendingSelections) {
			pendingSelections.clear();
		}
		selectedGpxFilesBackUp.clear();
		if (!Algorithms.isEmpty(load)) {
			try {
				JSONArray ar = new JSONArray(load);
				for (int i = 0; i < ar.length(); i++) {
					JSONObject obj = ar.getJSONObject(i);
					boolean backup = obj.has(BACKUP);
					boolean selectedByUser = obj.optBoolean(SELECTED_BY_USER, true);
					if (obj.has(FILE)) {
						String path = obj.getString(FILE);
						if (backup) {
							long backupTime = obj.optLong(BACKUP_MODIFIED_TIME, 0);
							BackupSelection selection = BackupSelection.fromPath(
									path, backupTime, selectedByUser);
							selectedGpxFilesBackUp.put(selection.key, selection);
						} else {
							Integer color = obj.has(COLOR)
									? GpxUtilities.INSTANCE.parseColor(obj.getString(COLOR), 0) : null;
							String hiddenGroups = obj.has(HIDDEN_GROUPS)
									? obj.getString(HIDDEN_GROUPS) : null;
							PendingSelection selection = new PendingSelection(
									path, selectedByUser, color, hiddenGroups, generation);
							synchronized (pendingSelections) {
								pendingSelections.put(selection.key, selection);
							}
						}
					} else if (obj.has(CURRENT_TRACK)) {
						SelectedGpxFile file = savingTrackHelper.getCurrentTrack();
						file.selectedByUser = selectedByUser;

						if (backup) {
							long backupTime = obj.optLong(BACKUP_MODIFIED_TIME, file.getModifiedTime());
							BackupSelection selection = BackupSelection.fromGpxFile(
									file.getGpxFile(), backupTime, selectedByUser);
							selectedGpxFilesBackUp.put(selection.key, selection);
						} else {
							updateSelected(true, file);
						}
					}
				}
			} catch (Exception e) {
				app.getSettings().SELECTED_GPX.set("");
				synchronized (pendingSelections) {
					pendingSelections.clear();
				}
				selectedGpxFilesBackUp.clear();
				log.error(e);
			}
		}
	}

	public void startPendingGpxRestore() {
		if (!pendingRestoreRunning.compareAndSet(false, true)) {
			return;
		}
		pendingRestoreStartTime = System.currentTimeMillis();
		loadNextPendingSelection(restoreGeneration.get());
	}

	private void loadNextPendingSelection(int generation) {
		if (generation != restoreGeneration.get()) {
			pendingRestoreRunning.set(false);
			startPendingGpxRestore();
			return;
		}
		PendingSelection selection = getNextPendingSelection(generation);
		if (selection == null) {
			pendingRestoreRunning.set(false);
			log.info("Restored selected GPX geometry in "
					+ (System.currentTimeMillis() - pendingRestoreStartTime) + " ms");
			app.getGpxDbHelper().startFilesystemReconciliation();
			return;
		}

		File file = new File(selection.path);
		if (!file.isFile()) {
			removePendingSelection(selection);
			saveCurrentSelections();
			app.runInUIThread(() -> loadNextPendingSelection(generation));
			return;
		}
		long scheduledModifiedTime = file.lastModified();
		long fileRestoreStart = System.currentTimeMillis();
		OsmAndTaskManager.executeTask(new GpxFileLoaderTask(file, null, false, gpx -> {
			try {
				if (isPendingSelectionActive(selection, generation)) {
					boolean fileUnchanged = file.isFile() && file.lastModified() == scheduledModifiedTime
							&& gpx != null && gpx.getModifiedTime() == scheduledModifiedTime;
					if (gpx != null && gpx.getError() == null && fileUnchanged) {
						if (selection.color != null) {
							gpx.setColor(selection.color);
						}
						if (selection.hiddenGroups != null) {
							readHiddenGroups(gpx, selection.hiddenGroups);
						}
						GpxSelectionParams params = GpxSelectionParams.newInstance()
								.showOnMap().syncGroup().setSelectedByUser(selection.selectedByUser);
						SelectedGpxFile selectedFile = selectGpxFile(gpx, params);
						log.info("Restored selected GPX name=" + file.getName()
								+ ", size=" + file.length()
								+ ", points=" + (selectedFile != null ? selectedFile.getPointsToDisplayCount() : 0)
								+ " in " + (System.currentTimeMillis() - fileRestoreStart) + " ms");
						app.getOsmandMap().refreshMap();
						removePendingSelection(selection);
					} else if (!file.isFile() || gpx == null || gpx.getError() != null) {
						removePendingSelection(selection);
					} else {
						markPendingSelectionForRetry(selection);
					}
					saveCurrentSelections();
				}
			} catch (RuntimeException error) {
				log.error("Failed to restore selected GPX " + selection.path, error);
				// Keep the pending preference for a retry on the next process start.
				saveCurrentSelections();
			} finally {
				loadNextPendingSelection(generation);
			}
			return true;
		}));
	}

	@Nullable
	private PendingSelection getNextPendingSelection(int generation) {
		synchronized (pendingSelections) {
			for (PendingSelection selection : pendingSelections.values()) {
				if (selection.generation == generation && !selection.restoreAttempted) {
					selection.restoreAttempted = true;
					selection.restoreAttempts++;
					return selection;
				}
			}
		}
		return null;
	}

	private boolean isPendingSelectionActive(@NonNull PendingSelection selection, int generation) {
		if (generation != restoreGeneration.get()) {
			return false;
		}
		synchronized (pendingSelections) {
			return pendingSelections.get(selection.key) == selection;
		}
	}

	private void removePendingSelection(@NonNull PendingSelection selection) {
		synchronized (pendingSelections) {
			if (pendingSelections.get(selection.key) == selection) {
				pendingSelections.remove(selection.key);
			}
		}
	}

	private void markPendingSelectionForRetry(@NonNull PendingSelection selection) {
		synchronized (pendingSelections) {
			if (pendingSelections.get(selection.key) == selection) {
				if (selection.restoreAttempts < 2) {
					pendingSelections.remove(selection.key);
					selection.restoreAttempted = false;
					pendingSelections.put(selection.key, selection);
				}
			}
		}
	}

	private void cancelPendingRestore() {
		restoreGeneration.incrementAndGet();
		synchronized (pendingSelections) {
			pendingSelections.clear();
		}
	}

	@NonNull
	private String saveHiddenGroups(@NonNull GpxFile gpxFile) {
		StringBuilder builder = new StringBuilder();
		for (PointsGroup group : gpxFile.getPointsGroups().values()) {
			if (group.isHidden()) {
				if (builder.length() > 0) {
					builder.append(",");
				}
				builder.append(Algorithms.isEmpty(group.getName()) ? " " : group.getName());
			}
		}
		return builder.toString();
	}

	public void readHiddenGroups(@NonNull GpxFile gpxFile, @NonNull String text) {
		List<String> names = Arrays.asList(text.split(","));

		for (PointsGroup group : gpxFile.getPointsGroups().values()) {
			String key = Algorithms.isEmpty(group.getName()) ? " " : group.getName();
			group.setHidden(names.contains(key));
		}
	}

	private void saveGpxToHistory(@NonNull GpxFile gpx) {
		String relativePath = GpxUiHelper.getGpxFileRelativePath(app, gpx.getPath());
		GPXInfo gpxInfo = GpxUiHelper.getGpxInfoByFileName(app, relativePath);
		if (gpxInfo != null) {
			app.getSearchHistoryHelper().addNewItemToHistory(gpxInfo, HistorySource.SEARCH);
		}
	}

	private void saveCurrentSelections() {
		JSONArray array = new JSONArray();
		Set<String> savedPaths = new HashSet<>();
		for (SelectedGpxFile selectedGpxFile : selectedGPXFiles) {
			GpxFile gpxFile = selectedGpxFile.getGpxFile();
			if (!selectedGpxFile.notShowNavigationDialog) {
				JSONObject obj = new JSONObject();
				try {
					if (selectedGpxFile.isShowCurrentTrack()) {
						obj.put(CURRENT_TRACK, true);
					} else if (!Algorithms.isEmpty(gpxFile.getPath())) {
						obj.put(FILE, gpxFile.getPath());
						savedPaths.add(gpxFile.getPath());
						if (gpxFile.getColor(0) != 0) {
							obj.put(COLOR, Algorithms.colorToString(gpxFile.getColor(0)));
						}
						obj.put(HIDDEN_GROUPS, saveHiddenGroups(gpxFile));
					}
					obj.put(SELECTED_BY_USER, selectedGpxFile.selectedByUser);
				} catch (JSONException e) {
					log.error(e);
				}
				array.put(obj);
			}
		}

		List<PendingSelection> pendingSnapshot;
		synchronized (pendingSelections) {
			pendingSnapshot = new ArrayList<>(pendingSelections.values());
		}
		for (PendingSelection selection : pendingSnapshot) {
			if (savedPaths.contains(selection.path)) {
				continue;
			}
			try {
				JSONObject obj = new JSONObject();
				obj.put(FILE, selection.path);
				if (selection.color != null && selection.color != 0) {
					obj.put(COLOR, Algorithms.colorToString(selection.color));
				}
				if (selection.hiddenGroups != null) {
					obj.put(HIDDEN_GROUPS, selection.hiddenGroups);
				}
				obj.put(SELECTED_BY_USER, selection.selectedByUser);
				array.put(obj);
				savedPaths.add(selection.path);
			} catch (JSONException e) {
				log.error(e);
			}
		}

		List<BackupSelection> backups = new ArrayList<>(selectedGpxFilesBackUp.values());
		backups.sort((o1, o2) -> collator.compare(o1.path, o2.path));

		for (BackupSelection backup : backups) {
			try {
				JSONObject obj = new JSONObject();
				if (backup.currentTrack) {
					obj.put(CURRENT_TRACK, true);
				} else {
					obj.put(FILE, backup.path);
				}
				obj.put(SELECTED_BY_USER, backup.selectedByUser);
				obj.put(BACKUP, true);
				obj.put(BACKUP_MODIFIED_TIME, backup.modifiedTime);
				array.put(obj);
			} catch (JSONException e) {
				log.error(e);
			}
		}
		app.getSettings().SELECTED_GPX.set(array.toString());
	}

	public SelectedGpxFile selectGpxFile(@NonNull GpxFile gpx, @NonNull GpxSelectionParams params) {
		boolean showOnMap = params.isShowOnMap();
		boolean currentTrack = gpx.isShowCurrentTrack();
		boolean pendingSelection = !currentTrack && !Algorithms.isEmpty(gpx.getPath())
				&& isPendingSelection(gpx.getPath());
		if (!currentTrack && !Algorithms.isEmpty(gpx.getPath())) {
			synchronized (pendingSelections) {
				pendingSelections.remove(gpx.getPath());
			}
		}
		KFile file = new KFile(gpx.getPath());
		GpxDataItem dataItem = file.exists()
				? app.getGpxDbHelper().getItem(file, !pendingSelection) : null;

		SelectedGpxFile selectedFile = currentTrack ? savingTrackHelper.getCurrentTrack() : getSelectedFileByPath(gpx.getPath());
		if (!currentTrack && (showOnMap || !params.shouldUpdateSelected())) {
			if (selectedFile == null) {
				selectedFile = new SelectedGpxFile();
			}
			if (dataItem != null) {
				selectedFile.setJoinSegments(dataItem.getParameter(JOIN_SEGMENTS));
			}
			selectedFile.setGpxFile(gpx, app);
			if (dataItem != null) {
				GpxTrackAnalysis analysis = dataItem.getAnalysis();
				Long itemModifiedTime = dataItem.getParameter(FILE_LAST_MODIFIED_TIME);
				if (analysis != null && itemModifiedTime != null
						&& itemModifiedTime == gpx.getModifiedTime()
						&& !GpxDbUtils.INSTANCE.isAnalyseNeeded(dataItem)) {
					selectedFile.setTrackSummaryAnalysis(
							analysis, itemModifiedTime, dataItem.getAnalysisParametersVersion());
				}
			}
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
			selectedGpxFile.cancelPendingFullAnalysis();
			FilteredSelectedGpxFile filtered = selectedGpxFile.getFilteredSelectedGpxFile();
			if (filtered != null) {
				app.getGpsFilterHelper().cancelFiltering(filtered);
			}

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

	public void addPoint(WptPt point, GpxFile gpxFile) {
		gpxFile.addPoint(point);
		syncGpxWithMarkers(gpxFile);
	}

	public void addPoints(List<WptPt> collection, GpxFile gpxFile) {
		gpxFile.addPoints(collection);
		syncGpxWithMarkers(gpxFile);
	}

	public boolean removePoint(WptPt point, GpxFile gpxFile) {
		boolean res = gpxFile.deleteWptPt(point);
		syncGpxWithMarkers(gpxFile);
		return res;
	}

	public void syncGpxWithMarkers(GpxFile gpxFile) {
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
	                              @NonNull CallbackWithObject<GpxFile> callback) {
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

	private static class PendingSelection {
		@NonNull
		private final String key;
		@NonNull
		private final String path;
		private final boolean selectedByUser;
		@Nullable
		private final Integer color;
		@Nullable
		private final String hiddenGroups;
		private final int generation;
		private boolean restoreAttempted;
		private int restoreAttempts;

		private PendingSelection(@NonNull String path,
		                         boolean selectedByUser,
		                         @Nullable Integer color,
		                         @Nullable String hiddenGroups,
		                         int generation) {
			this.key = path;
			this.path = path;
			this.selectedByUser = selectedByUser;
			this.color = color;
			this.hiddenGroups = hiddenGroups;
			this.generation = generation;
		}
	}

	private static class BackupSelection {
		@NonNull
		private final String key;
		@NonNull
		private final String path;
		private final boolean currentTrack;
		private final long modifiedTime;
		private final boolean selectedByUser;
		@Nullable
		private final GpxFile gpxFile;

		private BackupSelection(@NonNull String path,
		                        boolean currentTrack,
		                        long modifiedTime,
		                        boolean selectedByUser,
		                        @Nullable GpxFile gpxFile) {
			this.key = currentTrack ? CURRENT_TRACK : path;
			this.path = path;
			this.currentTrack = currentTrack;
			this.modifiedTime = modifiedTime;
			this.selectedByUser = selectedByUser;
			this.gpxFile = gpxFile;
		}

		@NonNull
		private static BackupSelection fromPath(@NonNull String path,
		                                        long modifiedTime,
		                                        boolean selectedByUser) {
			return new BackupSelection(path, false, modifiedTime, selectedByUser, null);
		}

		@NonNull
		private static BackupSelection fromGpxFile(@NonNull GpxFile gpxFile,
		                                           long modifiedTime,
		                                           boolean selectedByUser) {
			boolean currentTrack = gpxFile.isShowCurrentTrack() || Algorithms.isEmpty(gpxFile.getPath());
			return new BackupSelection(gpxFile.getPath(), currentTrack, modifiedTime, selectedByUser, gpxFile);
		}

		@NonNull
		private GpxFile getDisplayGpxFile(@NonNull SavingTrackHelper savingTrackHelper) {
			if (currentTrack) {
				return savingTrackHelper.getCurrentTrack().getGpxFile();
			}
			if (gpxFile != null) {
				return gpxFile;
			}
			GpxFile placeholder = new GpxFile((String) null);
			placeholder.setPath(path);
			placeholder.setModifiedTime(modifiedTime);
			return placeholder;
		}
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
		OsmAndTaskManager.executeTask(selectGpxTask);
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
