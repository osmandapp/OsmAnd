package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.SearchTrackItemsFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.GpxImportListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.RecordingTrackViewHolder.RecordingTrackListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TracksGroupViewHolder.TrackGroupsListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AvailableTracksFragment extends BaseOsmAndFragment implements FragmentStateHolder,
		SelectionHelperProvider<TrackItem>, SortTracksListener, OsmAuthorizationListener,
		OnTrackFileMoveListener, RenameCallback, UploadGpxListener, OnTrackFolderAddListener {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	public static final String SELECTED_FOLDER_KEY = "selected_folder_key";
	public static final int RECORDING_TRACK_UPDATE_INTERVAL_MILLIS = 2000;

	private OsmandApplication app;
	private OsmandSettings settings;
	private GpxSelectionHelper gpxSelectionHelper;
	private PopupActionsHelper popupActionsHelper;
	private ImportHelper importHelper;

	private TrackFolder trackFolder;
	private TrackItem recordingTrackItem;
	private VisibleTracksGroup visibleTracksGroup;
	private TrackFolderLoaderTask asyncLoader;
	private TrackFoldersAdapter adapter;

	private String preSelectedFolder;
	private boolean importing;
	private boolean updateEnable;

	@NonNull
	public PopupActionsHelper getGpxActionsHelper() {
		return popupActionsHelper;
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return popupActionsHelper.getSelectionHelper();
	}

	private void setTrackFolder(@NonNull TrackFolder trackFolder) {
		this.trackFolder = trackFolder;

		List<TrackItem> trackItems = trackFolder.getFlattenedTrackItems();
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

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		gpxSelectionHelper = app.getSelectedGpxHelper();
		importHelper = new ImportHelper(requireActivity());

		popupActionsHelper = new PopupActionsHelper(this, nightMode);
		popupActionsHelper.setDeletionListener(getTracksDeletionListener());

		visibleTracksGroup = new VisibleTracksGroup(app);
		SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
		recordingTrackItem = new TrackItem(app, savingTrackHelper.getCurrentGpx());

		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View view = themedInflater.inflate(R.layout.recycler_view_fragment, container, false);

		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setSortTracksListener(this);
		adapter.setTrackGroupsListener(getTrackGroupsListener());
		adapter.setTrackSelectionListener(getTrackSelectionListener());
		adapter.setRecordingTrackListener(getRecordingTrackListener());

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!importing) {
			if (trackFolder == null && (asyncLoader == null || asyncLoader.getStatus() != Status.RUNNING)) {
				reloadTracks();
			} else {
				updateAdapter();
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
			SearchTrackItemsFragment.showInstance(getChildFragmentManager());
		}
		if (itemId == R.id.action_menu) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				popupActionsHelper.showMainPopUpMenu(activity.findViewById(R.id.action_menu), trackFolder);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@NonNull
	private List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		if (PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			items.add(recordingTrackItem);
		}
		items.add(visibleTracksGroup);
		items.addAll(trackFolder.getSubFolders());
		items.addAll(trackFolder.getTrackItems());
		return items;
	}

	public void updateAdapter() {
		adapter.setItems(getAdapterItems());
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

	private void openTrackFolder(@NonNull TrackFolder folder) {
		TrackFolderFragment.showInstance(getChildFragmentManager(), folder);
	}

	public void saveTracksVisibility() {
		Set<TrackItem> selectedTracks = getSelectionHelper().getSelectedItems();
		app.getSelectedGpxHelper().saveTracksVisibility(selectedTracks, null);
		updateVisibleTracks();
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			SortByBottomSheet.showInstance(manager, this);
		}
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.getInt(TAB_ID) == GPX_TAB) {
			preSelectedFolder = bundle.getString(SELECTED_FOLDER_KEY);
		}
	}

	@Override
	public void authorizationCompleted() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);

		Intent intent = new Intent(app, app.getAppCustomization().getMyPlacesActivity());
		intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);

		app.startActivity(intent);
	}

	@Override
	public void onGpxUploaded(String result) {
		FragmentManager manager = getChildFragmentManager();
		TracksSelectionFragment fragment = (TracksSelectionFragment) manager.findFragmentByTag(TracksSelectionFragment.TAG);
		if (fragment != null && fragment.isAdded()) {
			fragment.dismissAllowingStateLoss();
		}
	}

	@NonNull
	@Override
	public TracksSortMode getTracksSortMode() {
		Map<String, String> tabsSortModes = settings.getTrackTabsSortModes();
		for (Entry<String, String> entry : tabsSortModes.entrySet()) {
			if (Algorithms.stringsEqual(entry.getKey(), trackFolder.getDirFile().getName())) {
				return TracksSortMode.getByValue(entry.getValue());
			}
		}
		return TracksSortMode.getDefaultSortMode();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {
		adapter.setSortMode(sortMode);

		Map<String, String> tabsSortModes = settings.getTrackTabsSortModes();
		tabsSortModes.put(trackFolder.getDirFile().getName(), sortMode.name());

		settings.saveTabsSortModes(tabsSortModes);
	}

	public void renamedTo(File file) {
		reloadTracks();
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		File dir = new File(trackFolder.getDirFile(), folderName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		reloadTracks();
	}

	@Override
	public void onFileMove(@NonNull File src, @NonNull File dest) {
		File destFolder = dest.getParentFile();
		if (destFolder != null && !destFolder.exists() && !destFolder.mkdirs()) {
			app.showToastMessage(R.string.file_can_not_be_moved);
		} else if (dest.exists()) {
			app.showToastMessage(R.string.file_with_name_already_exists);
		} else if (src.renameTo(dest)) {
			app.getGpxDbHelper().rename(src, dest);
			reloadTracks();
		} else {
			app.showToastMessage(R.string.file_can_not_be_moved);
		}
	}

	@NonNull
	private TrackGroupsListener getTrackGroupsListener() {
		return new TrackGroupsListener() {
			@Override
			public void onTracksGroupClicked(@NonNull TracksGroup group) {
				if (group instanceof TrackFolder) {
					openTrackFolder((TrackFolder) group);
				} else if (group instanceof VisibleTracksGroup) {
					showTracksVisibilityDialog();
				}
			}

			private void showTracksVisibilityDialog() {
				Bundle bundle = new Bundle();
				bundle.putString(OPEN_TRACKS_TAB, TrackTabType.ON_MAP.name());
				MapActivity.launchMapActivityMoveToTop(requireActivity(), storeState(), null, bundle);
			}
		};
	}

	@NonNull
	private TrackSelectionListener getTrackSelectionListener() {
		return new TrackSelectionListener() {

			@Override
			public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
				if (!trackItems.isEmpty()) {
					popupActionsHelper.openTrackOnMap(trackItems.iterator().next());
				}
			}

			@Override
			public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
				popupActionsHelper.showItemPopupMenu(view, trackItem);
			}
		};
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

	@NonNull
	private LoadTracksListener getLoadTracksListener() {
		return new LoadTracksListener() {

			@Override
			public void loadTracksStarted() {
				updateProgressVisibility(true);
			}

			@Override
			public void loadTracksFinished(@NonNull TrackFolder folder) {
				setTrackFolder(folder);
				updateAdapter();
				updateFragmentsFolders();
				updateProgressVisibility(false);
				checkSubfolder(folder);
			}

			private void checkSubfolder(@NonNull TrackFolder folder) {
				if (preSelectedFolder != null) {
					for (TrackFolder subfolder : folder.getFlattenedSubFolders()) {
						if (Algorithms.stringsEqual(subfolder.getDirFile().getName(), preSelectedFolder)) {
							openTrackFolder(subfolder);
							break;
						}
					}
					preSelectedFolder = null;
				}
			}

			public void updateFragmentsFolders() {
				List<TrackFolder> folders = trackFolder.getFlattenedSubFolders();
				folders.add(trackFolder);

				for (Fragment fragment : getChildFragmentManager().getFragments()) {
					if (fragment instanceof BaseTrackFolderFragment) {
						BaseTrackFolderFragment folderFragment = (BaseTrackFolderFragment) fragment;
						TrackFolder rootFolder = folderFragment.getRootFolder();
						TrackFolder selectedFolder = folderFragment.getSelectedFolder();

						boolean rootFolderUpdated = false;
						boolean selectedFolderUpdated = false;
						for (TrackFolder folder : folders) {
							if (rootFolder.equals(folder)) {
								folderFragment.setRootFolder(folder);
								rootFolderUpdated = true;
							}
							if (selectedFolder.equals(folder)) {
								folderFragment.setSelectedFolder(folder);
								selectedFolderUpdated = true;
							}
							if (rootFolderUpdated && selectedFolderUpdated) {
								break;
							}
						}
						folderFragment.updateContent();
					}
				}
			}
		};
	}

	@NonNull
	private GpxFilesDeletionListener getTracksDeletionListener() {
		return new GpxFilesDeletionListener() {
			@Override
			public void onGpxFilesDeletionFinished() {
				reloadTracks();
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

	@Nullable
	protected MyPlacesActivity getMyActivity() {
		return (MyPlacesActivity) getActivity();
	}

	@NonNull
	protected MyPlacesActivity requireMyActivity() {
		return (MyPlacesActivity) requireActivity();
	}
}