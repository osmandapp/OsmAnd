package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_EMPTY_TRACKS;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.SearchTrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TrackItemsFragment;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.SearchMyPlacesTracksFragment;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.RecordingTrackViewHolder.RecordingTrackListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvailableTracksFragment extends BaseTrackFolderFragment {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	public static final int RECORDING_TRACK_UPDATE_INTERVAL_MILLIS = 2000;

	private TrackFoldersHelper trackFoldersHelper;
	private final ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();

	private TrackItem recordingTrackItem;
	private VisibleTracksGroup visibleTracksGroup;

	private boolean updateEnable;


	@Override
	protected int getLayoutId() {
		return R.layout.available_tracks_fragment;
	}


	@NonNull
	@Override
	public String getFragmentTag() {
		return TAG;
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return selectionHelper;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		trackFoldersHelper = new TrackFoldersHelper(requireMyActivity());
		trackFoldersHelper.setLoadTracksListener(getLoadTracksListener());

		visibleTracksGroup = new VisibleTracksGroup(app);
		recordingTrackItem = new TrackItem(app, app.getSavingTrackHelper().getCurrentGpx());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			setupSwipeRefresh(view);
		}
		return view;
	}

	private void setupSwipeRefresh(@NonNull View view) {
		SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
		swipeRefresh.setColorSchemeColors(ContextCompat.getColor(app, nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange));
		swipeRefresh.setOnRefreshListener(() -> {
			reloadTracks();
			swipeRefresh.setRefreshing(false);
		});
	}

	@Nullable
	public TrackFoldersHelper getTrackFoldersHelper() {
		return trackFoldersHelper;
	}

	@Override
	protected void setupAdapter(@NonNull View view) {
		super.setupAdapter(view);
		adapter.setRecordingTrackListener(getRecordingTrackListener());
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!trackFoldersHelper.isImporting()) {
			if (rootFolder == null && !trackFoldersHelper.isLoadingTracks()) {
				reloadTracks();
			} else {
				updateContent();
			}
		}
		updateRecordingTrack();

		updateEnable = true;
		startHandler();
		restoreState(getArguments());
		updateProgressVisibility();
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
				FragmentManager manager = activity.getSupportFragmentManager();
				SearchMyPlacesTracksFragment.showInstance(manager, this, false, isUsedOnMap());
			}
		}
		if (itemId == R.id.action_menu) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				View view = activity.findViewById(R.id.action_menu);
				trackFoldersHelper.showFolderOptionsMenu(rootFolder, view, this);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		boolean osmMonitoringEnabled = PluginsHelper.isActive(OsmandMonitoringPlugin.class);
		boolean isTracksEmpty = rootFolder.getFlattenedTrackItems().size() != 0;
		boolean isSubFoldersEmpty = rootFolder.getFlattenedSubFolders().size() != 0;

		items.add(TYPE_SORT_TRACKS);
		if (osmMonitoringEnabled) {
			items.add(recordingTrackItem);
		}

		if (isTracksEmpty || isSubFoldersEmpty || osmMonitoringEnabled) {
			items.add(visibleTracksGroup);
			items.addAll(rootFolder.getSubFolders());
			items.addAll(rootFolder.getTrackItems());
		} else {
			items.add(TYPE_EMPTY_TRACKS);
		}

		if (isTracksEmpty) {
			items.add(rootFolder.getFolderAnalysis());
		}
		return items;
	}

	private void updateRecordingTrack() {
		adapter.updateItem(recordingTrackItem);
	}

	private void updateVisibleTracks() {
		adapter.updateItem(visibleTracksGroup);
	}

	public void setRootFolder(@NonNull TrackFolder rootFolder) {
		super.setRootFolder(rootFolder);
		setupSelectionHelper(rootFolder);
	}

	private void setupSelectionHelper(@NonNull TrackFolder folder) {
		List<TrackItem> trackItems = folder.getFlattenedTrackItems();
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get() || gpxSelectionHelper.getSelectedCurrentRecordingTrack() != null) {
			SelectedGpxFile selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
			TrackItem trackItem = new TrackItem(app, selectedGpxFile.getGpxFile());
			trackItems.add(trackItem);
		}
		List<TrackItem> selectedItems = new ArrayList<>();
		if (gpxSelectionHelper.isAnyGpxFileSelected()) {
			for (TrackItem info : trackItems) {
				if (gpxSelectionHelper.getSelectedFileByPath(info.getPath()) != null) {
					selectedItems.add(info);
				}
			}
		}
		selectionHelper.setAllItems(trackItems);
		selectionHelper.setSelectedItems(selectedItems);
		selectionHelper.setOriginalSelectedItems(selectedItems);
	}

	@Override
	public void onImportStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void onImportFinished() {
		updateProgressVisibility(false);
	}

	private void updateProgressVisibility() {
		boolean importing = trackFoldersHelper.isImporting();
		boolean loadingTracks = trackFoldersHelper.isLoadingTracks();
		updateProgressVisibility(importing || loadingTracks);
	}

	private void updateProgressVisibility(boolean visible) {
		MyPlacesActivity activity = getMyActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	private void openTrackFolder(@NonNull TrackFolder trackFolder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			TrackFolderFragment.showInstance(manager, trackFolder, this);
		}
	}

	@Override
	public void onTracksGroupSelected(@NonNull TracksGroup group, boolean selected) {
		if (group instanceof TrackFolder) {
			openTrackFolder((TrackFolder) group);
		} else if (group instanceof VisibleTracksGroup) {
			showTracksVisibilityDialog(TrackTabType.ON_MAP.name());
		}
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		trackFoldersHelper.showItemOptionsMenu(trackItem, view, this);
	}

	@Override
	public void onTrackItemLongClick(@NonNull View view, @NonNull TrackItem trackItem) {
		trackFoldersHelper.showTracksSelection(selectedFolder, this, Collections.singleton(trackItem), null);
	}

	@Override
	public void onTracksGroupLongClick(@NonNull View view, @NonNull TracksGroup group) {
		trackFoldersHelper.showTracksSelection(selectedFolder, this, null, Collections.singleton(group));
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
			} else if (!Algorithms.isEmpty(preSelectedFolder)
					&& !preSelectedFolder.equals(rootFolder.getDirFile().getAbsolutePath())) {
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
}