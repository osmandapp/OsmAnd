package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.configmap.tracks.TracksFragment.OPEN_TRACKS_TAB;
import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_EMPTY_FOLDER;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TracksAppearanceFragment;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ImportHelper.GpxImportListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.myplaces.tracks.controller.TrackFolderOptionsController;
import net.osmand.plus.myplaces.tracks.controller.TrackFolderOptionsListener;
import net.osmand.plus.myplaces.tracks.dialogs.AddNewTrackFolderBottomSheet.OnTrackFolderAddListener;
import net.osmand.plus.myplaces.tracks.dialogs.MoveGpxFileBottomSheet.OnTrackFileMoveListener;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TracksGroupViewHolder.TrackGroupsListener;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils.RenameCallback;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class BaseTrackFolderFragment extends BaseOsmAndFragment implements FragmentStateHolder,
		SortTracksListener, TrackSelectionListener, TrackGroupsListener, EmptyTracksListener, OsmAuthorizationListener,
		SelectGpxTaskListener, OnTrackFolderAddListener, GpxImportListener, TrackFolderOptionsListener, OnTrackFileMoveListener, RenameCallback {

	public static final String SELECTED_FOLDER_KEY = "selected_folder_key";
	public static final String SELECTED_ITEM_PATH_KEY = "selected_item_path_key";

	protected GpxSelectionHelper gpxSelectionHelper;

	protected TrackFolder rootFolder;
	protected TrackFolder selectedFolder;

	protected TrackFoldersAdapter adapter;
	protected RecyclerView recyclerView;
	protected String selectedItemPath;
	protected String preSelectedFolder;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	protected abstract int getLayoutId();

	@NonNull
	protected abstract String getFragmentTag();

	@NonNull
	public TrackFolder getRootFolder() {
		return rootFolder;
	}

	@NonNull
	public TrackFolder getSelectedFolder() {
		return selectedFolder;
	}

	public void setRootFolder(@NonNull TrackFolder rootFolder) {
		this.rootFolder = rootFolder;
	}

	public void setSelectedFolder(@NonNull TrackFolder selectedFolder) {
		this.selectedFolder = selectedFolder;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		gpxSelectionHelper = app.getSelectedGpxHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		FragmentActivity activity = requireActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View view = themedInflater.inflate(getLayoutId(), container, false);

		setupAdapter(view);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			foldersHelper.setGpxImportListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			foldersHelper.setGpxImportListener(null);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMPORT_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
			TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
			if (foldersHelper != null) {
				foldersHelper.handleImport(data, selectedFolder.getDirFile());
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Nullable
	public TrackFoldersHelper getTrackFoldersHelper() {
		Fragment target = getTargetFragment();
		if (target instanceof BaseTrackFolderFragment) {
			return ((BaseTrackFolderFragment) target).getTrackFoldersHelper();
		}
		return null;
	}

	protected void setupAdapter(@NonNull View view) {
		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setSortTracksListener(this);
		adapter.setTrackGroupsListener(this);
		adapter.setTrackSelectionListener(this);
		adapter.setEmptyTracksListener(this);

		recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setItemAnimator(null);
		recyclerView.setAdapter(adapter);
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_TRACKS);

		List<TrackFolder> folders = selectedFolder.getSubFolders();
		List<TrackItem> trackItems = selectedFolder.getTrackItems();
		if (folders.isEmpty() && trackItems.isEmpty()) {
			items.add(TYPE_EMPTY_FOLDER);
		} else {
			items.addAll(folders);
			items.addAll(trackItems);

			if (shouldShowFolderStats()) {
				items.add(selectedFolder.getFolderAnalysis());
			}
		}
		return items;
	}

	protected boolean shouldShowFolderStats() {
		return true;
	}

	public void updateContent() {
		adapter.setItems(getAdapterItems());
		adapter.setSortMode(getTracksSortMode());
	}

	protected void updateActionBar(boolean visible) {
		MyPlacesActivity activity = requireMyActivity();
		ActionBar actionBar = activity.getSupportActionBar();
		if (actionBar != null) {
			actionBar.setShowHideAnimationEnabled(false);
			if (visible) {
				actionBar.show();
			} else {
				actionBar.hide();
			}
		}
	}

	@NonNull
	private BaseTrackFolderFragment getMainFragment() {
		BaseTrackFolderFragment target = (BaseTrackFolderFragment) getTargetFragment();
		return target != null ? target : this;
	}

	public void showTrackOnMap(@NonNull TrackItem trackItem) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			selectedItemPath = trackItem.getPath();

			Bundle bundle = storeState();
			String screenName = app.getString(R.string.shared_string_tracks);
			boolean temporary = gpxSelectionHelper.getSelectedFileByPath(trackItem.getPath()) == null;
			TrackMenuFragment.openTrack(activity, trackItem.getFile(), bundle, screenName, OVERVIEW, temporary);
		}
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack(getFragmentTag(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	@Override
	public void onTracksGroupOptionsSelected(@NonNull View view, @NonNull TracksGroup group) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null && group instanceof TrackFolder) {
			TrackFolder folder = (TrackFolder) group;
			TrackFolderOptionsController.showDialog(foldersHelper, folder, getMainFragment());
		}
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			SortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap());
		}
	}

	@NonNull
	@Override
	public TracksSortMode getTracksSortMode() {
		Map<String, String> tabsSortModes = settings.getTrackFoldersSortModes();
		for (Entry<String, String> entry : tabsSortModes.entrySet()) {
			if (Algorithms.stringsEqual(entry.getKey(), selectedFolder.getDirName())) {
				return TracksSortMode.getByValue(entry.getValue());
			}
		}
		return TracksSortMode.getDefaultSortMode();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {
		adapter.setSortMode(sortMode);

		Map<String, String> tabsSortModes = settings.getTrackFoldersSortModes();
		tabsSortModes.put(selectedFolder.getDirName(), sortMode.name());

		settings.saveFoldersSortModes(tabsSortModes);
	}

	@Override
	public void importTracks() {
		Intent intent = ImportHelper.getImportTrackIntent();
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		AndroidUtils.startActivityForResultIfSafe(this, intent, IMPORT_FILE_REQUEST);
	}

	@Override
	public void fileRenamed(@NonNull File src, @NonNull File dest) {
		reloadTracks();
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			foldersHelper.onFileMove(src, dest);
		}
	}

	@Override
	public void onTrackFolderAdd(String folderName) {
		File dir = new File(selectedFolder.getDirFile(), folderName);
		if (!dir.exists()) {
			dir.mkdirs();
			dir.setLastModified(System.currentTimeMillis());
		}
		reloadTracks();
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);
		bundle.putString(SELECTED_ITEM_PATH_KEY, selectedItemPath);
		bundle.putString(SELECTED_FOLDER_KEY, selectedFolder.getDirFile().getAbsolutePath());
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.getInt(TAB_ID) == GPX_TAB) {
			preSelectedFolder = bundle.getString(SELECTED_FOLDER_KEY);
			selectedItemPath = bundle.getString(SELECTED_ITEM_PATH_KEY);
		}
	}

	@Nullable
	protected TrackItem geTrackItem(@NonNull TrackFolder folder, @NonNull String path) {
		for (TrackItem trackItem : folder.getFlattenedTrackItems()) {
			if (Algorithms.stringsEqual(trackItem.getPath(), path)) {
				return trackItem;
			}
		}
		return null;
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		if (!trackItems.isEmpty()) {
			showTrackOnMap(trackItems.iterator().next());
		}
	}

	protected void reloadTracks() {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			foldersHelper.reloadTracks();
		}
	}

	@Override
	public void onFolderRenamed(@NonNull File newDir) {
		updateContent();
	}

	@Override
	public void onFolderDeleted() {
		reloadTracks();
	}

	@Override
	public void showFolderTracksOnMap(@NonNull TrackFolder folder) {
		showTracksVisibilityDialog(folder.getDirName());
	}

	protected void showTracksVisibilityDialog(@NonNull String tabName) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Bundle bundle = new Bundle();
			bundle.putString(OPEN_TRACKS_TAB, tabName);
			MapActivity.launchMapActivityMoveToTop(activity, storeState(), null, bundle);
		}
	}

	@Override
	public void showExportDialog(@NonNull TrackFolder folder) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
			if (foldersHelper != null) {
				foldersHelper.showExportDialog(folder.getFlattenedTrackItems(), this);
			}
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
	public void authorizationCompleted() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);

		Intent intent = new Intent(app, app.getAppCustomization().getMyPlacesActivity());
		intent.putExtra(MapActivity.INTENT_PARAMS, bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);

		app.startActivity(intent);
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