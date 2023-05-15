package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.GpxActionsHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TracksGroupViewHolder.TrackGroupsListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AvailableTracksFragment extends BaseOsmAndFragment implements FragmentStateHolder, SortTracksListener,
		TrackGroupsListener, GpxFilesDeletionListener, TrackSelectionListener, OsmAuthorizationListener {

	public static final String TAG = TrackItemsFragment.class.getSimpleName();

	public static final String SELECTED_FOLDER_KEY = "selected_folder_key";

	private OsmandApplication app;
	private OsmandSettings settings;
	private GpxActionsHelper gpxActionsHelper;

	private TrackFolder trackFolder;
	private TrackFolderLoaderTask asyncLoader;
	private TrackFoldersAdapter adapter;

	private String selectedFolder;
	private boolean importing;

	@NonNull
	public GpxActionsHelper getGpxActionsHelper() {
		return gpxActionsHelper;
	}

	@NonNull
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		return gpxActionsHelper.getSelectionHelper();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();

		MyPlacesActivity activity = (MyPlacesActivity) requireActivity();
		gpxActionsHelper = new GpxActionsHelper(activity, nightMode);
		gpxActionsHelper.setTargetFragment(this);
		gpxActionsHelper.setDeletionListener(this);
		setHasOptionsMenu(true);
		activity.setToolbarVisibility(false);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View view = themedInflater.inflate(R.layout.recycler_view_fragment, container, false);

		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setSortTracksListener(this);
		adapter.setTrackGroupsListener(this);
		adapter.setTrackSelectionListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);

		if (trackFolder != null) {
			updateAdapter();
		}

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.myplaces_tracks_menu, menu);
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
				gpxActionsHelper.showPopUpMenu(activity.findViewById(R.id.action_menu), trackFolder);
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (!importing && trackFolder == null && (asyncLoader == null || asyncLoader.getStatus() != Status.RUNNING)) {
			reloadTracks();
		}
	}

	private void reloadTracks() {
		File gpxDir = FileUtils.getExistingDir(app, GPX_INDEX_DIR);
		asyncLoader = new TrackFolderLoaderTask(app, gpxDir, getLoadTracksListener());
		asyncLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void startImport() {
		importing = true;
	}

	public void finishImport(boolean success) {
		if (success) {
			reloadTracks();
		}
		importing = false;
	}

	private void updateAdapter() {
		adapter.setItems(getAdapterItems());
	}

	@NonNull
	private List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);
		items.add(new VisibleTracksGroup(app));
		items.addAll(trackFolder.getSubFolders());
		items.addAll(trackFolder.getTrackItems());
		return items;
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
				trackFolder = folder;
				updateAdapter();
				updateProgressVisibility(false);
				checkSubfolder(folder);
			}

			private void checkSubfolder(@NonNull TrackFolder folder) {
				if (selectedFolder != null) {
					for (TrackFolder subfolder : folder.getFlattenedSubFolders()) {
						if (Algorithms.stringsEqual(subfolder.getDirFile().getName(), selectedFolder)) {
							openTrackFolder(subfolder);
							break;
						}
					}
					selectedFolder = null;
				}
			}
		};
	}

	public void updateProgressVisibility(boolean visible) {
		MyPlacesActivity activity = (MyPlacesActivity) getActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		TrackItem trackItem = trackItems.iterator().next();
		gpxActionsHelper.openTrackOnMap(trackItem);
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		gpxActionsHelper.showItemPopupMenu(view, trackItem);
	}

	@Override
	public void onGpxFilesDeletionFinished(boolean shouldUpdateFolders) {
		if (shouldUpdateFolders) {
			reloadTracks();
		}
	}

	@Override
	public void onTracksGroupClicked(@NonNull TracksGroup group) {
		if (group instanceof TrackFolder) {
			openTrackFolder((TrackFolder) group);
		} else if (group instanceof VisibleTracksGroup) {
			showTracksVisibilityDialog();
		}
	}

	private void openTrackFolder(@NonNull TrackFolder folder) {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			TrackFolderFragment.showInstance(manager, folder, this);
		}
	}

	private void showTracksVisibilityDialog() {
		Bundle bundle = new Bundle();
		bundle.putString(OPEN_TRACKS_TAB, TrackTabType.ON_MAP.name());
		MapActivity.launchMapActivityMoveToTop(requireActivity(), storeState(), null, bundle);
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			SortByBottomSheet.showInstance(manager, this);
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

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.getInt(TAB_ID) == GPX_TAB) {
			selectedFolder = bundle.getString(SELECTED_FOLDER_KEY);
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
}