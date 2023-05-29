package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;
import static net.osmand.plus.settings.fragments.ExportSettingsFragment.SELECTED_TYPES;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.tracks.SearchTrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.TracksAppearanceFragment;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.ImportHelper.GpxImportListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.RecordingTrackViewHolder.RecordingTrackListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TrackFolderAnalysis;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.helpers.folder.TrackFolderOptionsListener;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AvailableTracksFragment extends BaseTrackFolderFragment implements SelectionHelperProvider<TrackItem>,
		OnTrackFileMoveListener, RenameCallback, TrackFolderOptionsListener {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	public static final int RECORDING_TRACK_UPDATE_INTERVAL_MILLIS = 2000;

	private final ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();

	private TrackItem recordingTrackItem;
	private VisibleTracksGroup visibleTracksGroup;
	private TrackFolderLoaderTask asyncLoader;

	private boolean importing;
	private boolean updateEnable;


	@Override
	protected int getLayoutId() {
		return R.layout.recycler_view_fragment;
	}


	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return selectionHelper;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		visibleTracksGroup = new VisibleTracksGroup(app);
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		recordingTrackItem = new TrackItem(app, savingTrackHelper.getCurrentGpx());

		setHasOptionsMenu(true);
	}

	@Override
	protected void setupAdapter(@NonNull View view) {
		super.setupAdapter(view);
		adapter.setRecordingTrackListener(getRecordingTrackListener());
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!importing) {
			if (rootFolder == null && (asyncLoader == null || asyncLoader.getStatus() != Status.RUNNING)) {
				reloadTracks();
			} else {
				updateContent();
			}
		}
		updateRecordingTrack();

		updateEnable = true;
		startHandler();
		restoreState(getArguments());
	}

	private void startHandler() {
		Handler updateCurrentRecordingTrack = new Handler();
		updateCurrentRecordingTrack.postDelayed(() -> {
			if (getView() != null && updateEnable) {
				updateRecordingTrack();
				startHandler();
			}
		}, RECORDING_TRACK_UPDATE_INTERVAL_MILLIS);
	}

	@Override
	public void onPause() {
		super.onPause();
		updateEnable = false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.myplaces_tracks_menu, menu);
		requireMyActivity().setToolbarVisibility(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_search) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				SearchTrackItemsFragment.showInstance(activity.getSupportFragmentManager(), this);
			}
		}
		if (itemId == R.id.action_menu) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				showFolderOptionsMenu(activity.findViewById(R.id.action_menu), rootFolder);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		if (PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			items.add(recordingTrackItem);
		}
		items.add(visibleTracksGroup);
		items.addAll(rootFolder.getSubFolders());
		items.addAll(rootFolder.getTrackItems());

		if (rootFolder.getFlattenedTrackItems().size() != 0) {
			items.add(TrackFolderAnalysis.getFolderAnalysis(rootFolder));
		}
		return items;
	}

	private void updateRecordingTrack() {
		adapter.updateItem(recordingTrackItem);
	}

	private void updateVisibleTracks() {
		adapter.updateItem(visibleTracksGroup);
	}

	private void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		asyncLoader = new TrackFolderLoaderTask(app, gpxDir, getLoadTracksListener());
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void setRootFolder(@NonNull TrackFolder rootFolder) {
		super.setRootFolder(rootFolder);

		List<TrackItem> trackItems = rootFolder.getFlattenedTrackItems();
		List<TrackItem> selectedItems = new ArrayList<>();
		if (gpxSelectionHelper.isAnyGpxFileSelected()) {
			for (TrackItem info : trackItems) {
				if (gpxSelectionHelper.getSelectedFileByPath(info.getPath()) != null) {
					selectedItems.add(info);
				}
			}
		}
		ItemsSelectionHelper<TrackItem> selectionHelper = getSelectionHelper();
		selectionHelper.setAllItems(trackItems);
		selectionHelper.setSelectedItems(selectedItems);
		selectionHelper.setOriginalSelectedItems(selectedItems);
	}

	private void startImport() {
		importing = true;
		updateProgressVisibility(true);
	}

	private void finishImport() {
		importing = false;
		updateProgressVisibility(false);
		reloadTracks();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null) {
				List<Uri> filesUri = IntentHelper.getIntentUris(data);
				if (!Algorithms.isEmpty(filesUri)) {
					startImport();
					importHelper.setGpxImportListener(getGpxImportListener(filesUri.size()));
					importHelper.handleGpxFilesImport(filesUri, true);
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void updateProgressVisibility(boolean visible) {
		MyPlacesActivity activity = getMyActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	private void openTrackFolder(@NonNull TrackFolder trackFolder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TrackFolderFragment.showInstance(activity.getSupportFragmentManager(), trackFolder, this);
		}
	}

	public void saveTracksVisibility() {
		Set<TrackItem> selectedTracks = getSelectionHelper().getSelectedItems();
		app.getSelectedGpxHelper().saveTracksVisibility(selectedTracks, null);
		updateVisibleTracks();
	}

	@Override
	public void onTracksGroupClicked(@NonNull TracksGroup group) {
		if (group instanceof TrackFolder) {
			openTrackFolder((TrackFolder) group);
		} else if (group instanceof VisibleTracksGroup) {
			showTracksVisibilityDialog(TrackTabType.ON_MAP.name());
		}
	}

	private void showTracksVisibilityDialog(@NonNull String tabName) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Bundle bundle = new Bundle();
			bundle.putString(OPEN_TRACKS_TAB, tabName);
			MapActivity.launchMapActivityMoveToTop(activity, storeState(), null, bundle);
		}
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		showItemOptionsMenu(view, trackItem);
	}

	@Override
	public void renamedTo(File file) {
		reloadTracks();
	}

	@Override
	public void onFolderRenamed(@NonNull File oldDir, @NonNull File newDir) {
		reloadTracks();
	}

	@Override
	public void onFolderDeleted() {
		reloadTracks();
	}

	@Override
	public void showFolderTracksOnMap(@NonNull TrackFolder folder) {
		showTracksVisibilityDialog(folder.getDirName());
	}

	@Override
	public void showExportDialog(@NonNull TrackFolder folder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			List<File> selectedFiles = new ArrayList<>();
			for (TrackItem trackItem : folder.getFlattenedTrackItems()) {
				selectedFiles.add(trackItem.getFile());
			}
			HashMap<ExportSettingsType, List<?>> selectedTypes = new HashMap<>();
			selectedTypes.put(ExportSettingsType.TRACKS, selectedFiles);

			Bundle bundle = new Bundle();
			bundle.putSerializable(SELECTED_TYPES, selectedTypes);
			MapActivity.launchMapActivityMoveToTop(activity, storeState(), null, bundle);
		}
	}

	@Override
	public void showChangeAppearanceDialog(@NonNull TrackFolder folder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TracksAppearanceFragment.showInstance(activity.getSupportFragmentManager(), this);
		}
	}

	@Override
	public void onGpxFilesDeletionFinished() {
		reloadTracks();
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		super.onTrackFolderAdd(folderName);
		reloadTracks();
	}

	@Override
	public void onFileMove(@NonNull File src, @NonNull File dest) {
		if (dest.exists()) {
			app.showToastMessage(R.string.file_with_name_already_exists);
		} else if (FileUtils.renameGpxFile(app, src, dest) != null) {
			reloadTracks();
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
	}

	@Override
	public void gpxSelectionStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void gpxSelectionFinished() {
		updateProgressVisibility(false);
		updateVisibleTracks();
	}

	@NonNull
	private RecordingTrackListener getRecordingTrackListener() {
		OsmandMonitoringPlugin plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin.class);
		return new RecordingTrackListener() {
			@Override
			public void saveTrackRecording() {
				if (plugin != null) {
					plugin.saveCurrentTrack(() -> {
						if (isResumed()) {
							reloadTracks();
						}
					});
				}
				updateRecordingTrack();
			}

			@Override
			public void toggleTrackRecording() {
				if (plugin != null) {
					if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
						plugin.stopRecording();
					} else if (app.getLocationProvider().checkGPSEnabled(getActivity())) {
						plugin.startGPXMonitoring(getActivity());
					}
				}
				updateRecordingTrack();
			}
		};
	}

	@Override
	public void restoreState(Bundle bundle) {
		super.restoreState(bundle);

		if (rootFolder != null) {
			if (!Algorithms.isEmpty(selectedItemPath)) {
				TrackItem trackItem = geTrackItem(rootFolder, selectedItemPath);
				if (trackItem != null) {
					showTrackItem(rootFolder, trackItem);
				}
				selectedItemPath = null;
			} else if (!Algorithms.isEmpty(preSelectedFolder)) {
				openSubfolder(rootFolder, new File(preSelectedFolder));
				preSelectedFolder = null;
			}
		}
	}

	private void showTrackItem(@NonNull TrackFolder folder, @NonNull TrackItem trackItem) {
		File file = trackItem.getFile();
		File dirFile = file != null ? file.getParentFile() : null;
		if (dirFile != null) {
			if (Algorithms.objectEquals(selectedFolder.getDirFile(), dirFile)) {
				int index = adapter.getItemPosition(trackItem);
				if (index != -1) {
					recyclerView.scrollToPosition(index);
				}
			} else {
				openSubfolder(folder, dirFile);
			}
		}
	}

	private void openSubfolder(@NonNull TrackFolder folder, @NonNull File file) {
		TrackFolder subfolder = getSubfolder(folder, file);
		if (subfolder != null) {
			openTrackFolder(subfolder);
		}
	}

	@Nullable
	private TrackFolder getSubfolder(@NonNull TrackFolder folder, @NonNull File file) {
		for (TrackFolder subfolder : folder.getFlattenedSubFolders()) {
			if (Algorithms.objectEquals(subfolder.getDirFile(), file)) {
				return subfolder;
			}
		}
		return null;
	}

	@NonNull
	private LoadTracksListener getLoadTracksListener() {
		return new LoadTracksListener() {

			@Override
			public void loadTracksStarted() {
				updateProgressVisibility(true);
			}

			@Override
			public void loadTracksFinished(@NonNull TrackFolder folder) {
				setRootFolder(folder);
				setSelectedFolder(folder);

				updateContent();
				updateFragmentsFolders();
				updateProgressVisibility(false);

				restoreState(getArguments());
			}

			public void updateFragmentsFolders() {
				List<TrackFolder> folders = rootFolder.getFlattenedSubFolders();
				folders.add(rootFolder);

				MyPlacesActivity activity = getMyActivity();
				if (activity != null) {
					TrackFolderFragment folderFragment = activity.getFragment(TrackFolderFragment.TAG);
					if (folderFragment != null) {
						updateFragmentFolders(folderFragment, folders);
					}
					TracksSelectionFragment selectionFragment = activity.getFragment(TracksSelectionFragment.TAG);
					if (selectionFragment != null) {
						updateFragmentFolders(selectionFragment, folders);
					}
				}
			}

			public void updateFragmentFolders(@NonNull BaseTrackFolderFragment fragment, @NonNull List<TrackFolder> folders) {
				TrackFolder rootFolder = fragment.getRootFolder();
				TrackFolder selectedFolder = fragment.getSelectedFolder();

				boolean rootFolderUpdated = false;
				boolean selectedFolderUpdated = false;
				for (TrackFolder folder : folders) {
					if (rootFolder.equals(folder)) {
						fragment.setRootFolder(folder);
						rootFolderUpdated = true;
					}
					if (selectedFolder.equals(folder)) {
						fragment.setSelectedFolder(folder);
						selectedFolderUpdated = true;
					}
					if (rootFolderUpdated && selectedFolderUpdated) {
						break;
					}
				}
				fragment.updateContent();
			}
		};
	}

	@NonNull
	private GpxImportListener getGpxImportListener(int filesSize) {
		return new GpxImportListener() {
			private int counter;

			@Override
			public void onImportComplete(boolean success) {
				if (!success) {
					counter++;
				}
				checkImportFinished();
			}

			@Override
			public void onSaveComplete(boolean success, GPXFile gpxFile) {
				counter++;
				checkImportFinished();
			}

			private void checkImportFinished() {
				if (counter == filesSize) {
					finishImport();
				}
			}
		};
	}
}