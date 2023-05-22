package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.importfiles.ImportHelper.IMPORT_FILE_REQUEST;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_EMPTY_FOLDER;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TrackMenuTab.OVERVIEW;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.tracks.SortByBottomSheet;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.viewholders.EmptyTracksViewHolder.EmptyTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.SortTracksViewHolder.SortTracksListener;
import net.osmand.plus.configmap.tracks.viewholders.TrackViewHolder.TrackSelectionListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.VisibleTracksGroup;
import net.osmand.plus.myplaces.tracks.dialogs.viewholders.TracksGroupViewHolder.TrackGroupsListener;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask;
import net.osmand.plus.myplaces.tracks.tasks.DeleteGpxFilesTask.GpxFilesDeletionListener;
import net.osmand.plus.myplaces.tracks.tasks.OpenGpxDetailsTask;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper.OsmAuthorizationListener;
import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectGpxTask.SelectGpxTaskListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class BaseTrackFolderFragment extends BaseOsmAndFragment implements FragmentStateHolder,
		SortTracksListener, TrackSelectionListener, TrackGroupsListener, EmptyTracksListener,
		OsmAuthorizationListener, GpxFilesDeletionListener, SelectGpxTaskListener {

	public static final String SELECTED_FOLDER_KEY = "selected_folder_key";

	protected ImportHelper importHelper;
	protected GpxSelectionHelper gpxSelectionHelper;

	protected TrackFolder rootFolder;
	protected TrackFolder selectedFolder;
	protected TrackFoldersAdapter adapter;

	protected String preSelectedFolder;

	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	protected abstract int getLayoutId();

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
		importHelper = new ImportHelper(requireActivity());
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

	protected void setupAdapter(@NonNull View view) {
		adapter = new TrackFoldersAdapter(app, nightMode);
		adapter.setSortTracksListener(this);
		adapter.setTrackGroupsListener(this);
		adapter.setTrackSelectionListener(this);
		adapter.setEmptyTracksListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
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
		}
		items.addAll(folders);
		items.addAll(trackItems);
		return items;
	}

	protected void updateContent() {
		adapter.setItems(getAdapterItems());
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

	public void showFolderOptionsMenu(@NonNull View view, @NonNull TrackFolder trackFolder) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_select)
				.setIcon(getContentIcon(R.drawable.ic_action_deselect_all))
				.setOnClickListener(v -> {
					FragmentManager manager = getFragmentManager();
					if (manager != null) {
						TracksSelectionFragment.showInstance(manager, trackFolder, getTargetFragment());
					}
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.add_new_folder)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_folder_add_outlined))
				.setOnClickListener(v -> {
					FragmentManager manager = getFragmentManager();
					if (manager != null) {
						AddNewTrackFolderBottomSheet.showInstance(manager, null, this, isUsedOnMap());
					}
				})
				.create());

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_import)
				.setIcon(uiUtilities.getThemedIcon(R.drawable.ic_action_import))
				.setOnClickListener(v -> importTracks())
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	public void showItemOptionsMenu(@NonNull View view, @NonNull TrackItem trackItem) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show_on_map)
				.setIcon(getContentIcon(R.drawable.ic_show_on_map))
				.setOnClickListener(v -> showTrackOnMap(trackItem))
				.create()
		);

		File file = trackItem.getFile();
		if (file != null) {
			GPXTrackAnalysis analysis = GpxUiHelper.getGpxTrackAnalysis(trackItem, app, null);
			if (analysis != null && analysis.totalDistance != 0 && !trackItem.isShowCurrentTrack()) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.analyze_on_map)
						.setIcon(getContentIcon(R.drawable.ic_action_info_dark))
						.setOnClickListener(v -> GpxSelectionHelper.getGpxFile(requireActivity(), file, true, result -> {
							FragmentActivity activity = getActivity();
							if (activity != null) {
								OpenGpxDetailsTask detailsTask = new OpenGpxDetailsTask(activity, result);
								detailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							}
							return true;
						}))
						.create()
				);
			}
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_move)
					.setIcon(getContentIcon(R.drawable.ic_action_folder_stroke))
					.setOnClickListener(v -> {
						FragmentManager manager = getFragmentManager();
						if (manager != null) {
							MoveGpxFileBottomSheet.showInstance(manager, this, file.getAbsolutePath(), isUsedOnMap(), false);
						}
					})
					.create()
			);
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_rename)
					.setIcon(getContentIcon(R.drawable.ic_action_edit_dark))
					.setOnClickListener(v -> {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							FileUtils.renameFile(activity, file, this, isUsedOnMap());
						}
					})
					.create()
			);
			OsmEditingPlugin osmEditingPlugin = PluginsHelper.getActivePlugin(OsmEditingPlugin.class);
			if (osmEditingPlugin != null) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.shared_string_export)
						.setIcon(getContentIcon(R.drawable.ic_action_export))
						.setOnClickListener(v -> {
							FragmentActivity activity = getActivity();
							if (activity != null) {
								osmEditingPlugin.sendGPXFiles(activity, this, file);
							}
						})
						.create()
				);
			}
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_delete)
					.setIcon(getContentIcon(R.drawable.ic_action_delete_dark))
					.setOnClickListener(v -> showDeleteConfirmationDialog(trackItem))
					.create()
			);
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void showDeleteConfirmationDialog(@NonNull TrackItem trackItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
		builder.setMessage(getString(R.string.delete_confirmation_msg, trackItem.getName()));
		builder.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> deleteGpxFiles(trackItem.getFile()));
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.show();
	}

	public void deleteGpxFiles(@NonNull File... files) {
		DeleteGpxFilesTask deleteFilesTask = new DeleteGpxFilesTask(app, this);
		deleteFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, files);
	}

	private void showTrackOnMap(@NonNull TrackItem trackItem) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			Bundle bundle = storeState();
			String screenName = app.getString(R.string.shared_string_tracks);
			boolean temporary = gpxSelectionHelper.getSelectedFileByPath(trackItem.getPath()) == null;
			TrackMenuFragment.openTrack(activity, trackItem.getFile(), bundle, screenName, OVERVIEW, temporary);
		}
	}

	@Override
	public void onTracksGroupClicked(@NonNull TracksGroup group) {
		if (group instanceof TrackFolder) {
			selectedFolder = (TrackFolder) group;
		} else if (group instanceof VisibleTracksGroup) {
			boolean selected = !isTracksGroupSelected(group);
			onTracksGroupSelected(group, selected);
		}
		updateContent();
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
			if (Algorithms.stringsEqual(entry.getKey(), selectedFolder.getDirFile().getName())) {
				return TracksSortMode.getByValue(entry.getValue());
			}
		}
		return TracksSortMode.getDefaultSortMode();
	}

	@Override
	public void setTracksSortMode(@NonNull TracksSortMode sortMode) {
		adapter.setSortMode(sortMode);

		Map<String, String> tabsSortModes = settings.getTrackTabsSortModes();
		tabsSortModes.put(selectedFolder.getDirFile().getName(), sortMode.name());

		settings.saveTabsSortModes(tabsSortModes);
	}

	@Override
	public void importTracks() {
		Intent intent = ImportHelper.getImportTrackIntent();
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		AndroidUtils.startActivityForResultIfSafe(this, intent, IMPORT_FILE_REQUEST);
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, GPX_TAB);
		bundle.putString(SELECTED_FOLDER_KEY, selectedFolder.getDirFile().getName());
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.getInt(TAB_ID) == GPX_TAB) {
			preSelectedFolder = bundle.getString(SELECTED_FOLDER_KEY);
		}
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		if (!trackItems.isEmpty()) {
			showTrackOnMap(trackItems.iterator().next());
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