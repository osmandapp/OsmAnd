package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_EMPTY_FOLDER;
import static net.osmand.plus.myplaces.tracks.dialogs.TrackFoldersAdapter.TYPE_SORT_TRACKS;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.track.AndroidOrganizeByResourceMapper;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.shared.gpx.data.OrganizedTracksGroup;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TracksSelectionFragment extends BaseTrackFolderFragment implements UploadGpxListener {

	public static final String TAG = TracksSelectionFragment.class.getSimpleName();

	private final ItemsSelectionHelper<TrackItem> itemsSelectionHelper = new ItemsSelectionHelper<>();
	private final ItemsSelectionHelper<TracksGroup> groupsSelectionHelper = new ItemsSelectionHelper<>();

	@Nullable
	private Set<TrackItem> preselectedTrackItems;
	@Nullable
	private Set<TracksGroup> preselectedTracksGroups;
	@Nullable
	private ScreenPositionData screenPositionData;
	private boolean scrollPositionApplied;
	@Nullable
	private MenuItem selectionItem;

	@Override
	@ColorRes
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarActiveColorId(nightMode);
	}

	@NonNull
	@Override
	public String getFragmentTag() {
		return TAG;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.track_folder_fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		FragmentActivity activity = requireActivity();
		activity.getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				onBackPressed();
			}
		});
	}

	@Override
	public void setRootFolder(@NonNull TracksGroup rootGroup) {
		if (rootGroup instanceof OrganizedTracksGroup organizedTracks) {
			setSmartFolder(organizedTracks.getRelatedSmartFolder());
		} else if (rootGroup instanceof SmartFolder folder) {
			setSmartFolder(folder);
		} else {
			super.setRootFolder(rootGroup);
		}
		itemsSelectionHelper.clearSelectedItems();
		groupsSelectionHelper.clearSelectedItems();

		if (rootGroup instanceof TrackFolder folder) {
			groupsSelectionHelper.setAllItems(folder.getSubFolders());
		}
		boolean addIndividualItems = true;
		if (rootGroup instanceof SmartFolder folder) {
			List<OrganizedTracksGroup> organizedTracks = folder.getOrganizedTrackItems(AndroidOrganizeByResourceMapper.INSTANCE);
			if (!Algorithms.isEmpty(organizedTracks)) {
				groupsSelectionHelper.setAllItems(organizedTracks);
				addIndividualItems = false;
			}
		}
		if (addIndividualItems) {
			itemsSelectionHelper.setAllItems(rootGroup.getTrackItems());
		}
		if (!Algorithms.isEmpty(preselectedTrackItems)) {
			itemsSelectionHelper.setSelectedItems(preselectedTrackItems);
		}
		if (!Algorithms.isEmpty(preselectedTracksGroups)) {
			groupsSelectionHelper.setSelectedItems(preselectedTracksGroups);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		setupToolbar();
		updateContent();
		updateSelection();
		applyScrollPosition();

		return view;
	}

	@NonNull
	@Override
	protected List<Object> getAdapterItems() {
		if (rootFolder == null) {
			List<Object> items = new ArrayList<>();
			items.add(TYPE_SORT_TRACKS);

			Set<TrackItem> trackItems = itemsSelectionHelper.getAllItems();
			Set<TracksGroup> tracksGroups = groupsSelectionHelper.getAllItems();

			if (trackItems.isEmpty() && tracksGroups.isEmpty()) {
				items.add(TYPE_EMPTY_FOLDER);
			} else if (!tracksGroups.isEmpty()){
				items.addAll(tracksGroups);
			} else {
				items.addAll(trackItems);
			}
			return items;
		}
		return super.getAdapterItems();
	}

	@Override
	protected void setupAdapter(@NonNull View view) {
		super.setupAdapter(view);
		adapter.setSelectionMode(true);
		adapter.setShouldShowFolder(true);
	}

	private void setupToolbar() {
		MyPlacesActivity activity = getMyActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			actionBar.setHomeAsUpIndicator(R.drawable.ic_action_close);
			actionBar.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getToolbarActiveColor(app, nightMode)));
		}
		updateSelection();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.myplaces_selection_menu, menu);
		selectionItem = menu.findItem(R.id.action_select);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_select) {
			if (isAllItemsSelected()) {
				itemsSelectionHelper.clearSelectedItems();
				groupsSelectionHelper.clearSelectedItems();
			} else {
				itemsSelectionHelper.selectAllItems();
				groupsSelectionHelper.selectAllItems();
			}
			updateSelection();
			adapter.notifyDataSetChanged();
			return true;
		} else if (itemId == R.id.action_overflow_menu) {
			MyPlacesActivity activity = getMyActivity();
			TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
			if (foldersHelper != null && activity != null) {
				View view = activity.findViewById(R.id.action_overflow_menu);
				Set<TrackItem> trackItems = itemsSelectionHelper.getSelectedItems();
				Set<TracksGroup> tracksGroups = groupsSelectionHelper.getSelectedItems();
				foldersHelper.showItemsOptionsMenu(view, rootFolder, trackItems, tracksGroups, this, this, isNightMode());
				return true;
			}
		}
		return false;
	}

	private boolean isAllItemsSelected() {
		return itemsSelectionHelper.isAllItemsSelected() && groupsSelectionHelper.isAllItemsSelected();
	}

	private void updateSelection() {
		updateToolbar();

		if (selectionItem != null) {
			boolean selected = isAllItemsSelected();
			selectionItem.setTitle(selected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
			selectionItem.setIcon(getIcon(selected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all));

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				selectionItem.setContentDescription(getString(selected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all));
			}
		}
	}

	@Override
	public void updateContent() {
		super.updateContent();
		updateToolbar();
	}

	private void updateToolbar() {
		MyPlacesActivity activity = getMyActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			Set<TrackItem> tracks = itemsSelectionHelper.getSelectedItems();
			Set<TracksGroup> groups = groupsSelectionHelper.getSelectedItems();
			int items = tracks.size() + groups.size();
			int total = tracks.size();
			for (TracksGroup group : groups) {
				if (group instanceof TrackFolder) {
					total += ((TrackFolder) group).getFlattenedTrackItems().size();
				} else {
					total += group.getTrackItems().size();
				}
			}
			String text = getResources().getQuantityString(R.plurals.tracks, total, items, total);
			actionBar.setTitle(text);
		}
	}

	private void applyScrollPosition() {
		if (screenPositionData == null || scrollPositionApplied) {
			return;
		}
		recyclerView.post(() -> {
			int position = adapter.getItemPosition(screenPositionData.getReferenceObject());
			recyclerView.scrollToPosition(position);

			app.runInUIThread(() -> {
				ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
				View view = viewHolder != null ? viewHolder.itemView : null;
				if (view != null) {
					int previousItemY = screenPositionData.getReferenceItemOnScreenY();
					int currentItemY = AndroidUtils.getViewOnScreenY(view);
					int correction = currentItemY - previousItemY;
					recyclerView.scrollBy(0, correction);
					scrollPositionApplied = true;
				}
			});
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		updateActivityTitle();
	}

	private void updateActivityTitle() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof AvailableTracksFragment) {
			MyPlacesActivity activity = getMyActivity();
			if (activity != null) {
				activity.updateToolbar();
			}
		}
	}

	@Override
	public void onDestroyView() {
		MyPlacesActivity activity = getMyActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null) {
			int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
			actionBar.setHomeAsUpIndicator(getIcon(AndroidUtils.getNavigationIconResId(app), colorId));
			actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(app, ColorUtilities.getAppBarColorId(nightMode))));
		}
		super.onDestroyView();
	}

	private void onBackPressed() {
		if (rootFolder == null || rootFolder.equals(selectedFolder)) {
			dismiss();
		} else {
			selectedFolder = selectedFolder.getParentFolder();
			updateContent();
		}
	}

	@Override
	public boolean isTrackItemSelected(@NonNull TrackItem trackItem) {
		return itemsSelectionHelper.isItemSelected(trackItem);
	}

	@Override
	public void onTrackItemsSelected(@NonNull Set<TrackItem> trackItems, boolean selected) {
		itemsSelectionHelper.onItemsSelected(trackItems, selected);
		adapter.onItemsSelected(trackItems);
		updateSelection();
	}

	@Override
	public boolean isTracksGroupSelected(@NonNull TracksGroup group) {
		return groupsSelectionHelper.isItemsSelected(Collections.singleton(group));
	}

	@Override
	public void onTracksGroupSelected(@NonNull TracksGroup group, boolean selected) {
		groupsSelectionHelper.onItemsSelected(Collections.singleton(group), selected);
		adapter.onItemsSelected(Collections.singleton(group));
		updateSelection();
	}

	@Override
	protected boolean shouldShowFolderStats() {
		return false;
	}

	@Override
	public void onGpxUploadFinished(String result) {
		dismiss();
	}

	@Override
	public void onFileMove(@Nullable File src, @NonNull File dest) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			Set<TrackItem> trackItems = itemsSelectionHelper.getSelectedItems();
			Set<TracksGroup> tracksGroups = groupsSelectionHelper.getSelectedItems();

			foldersHelper.moveTracks(trackItems, tracksGroups, dest, result -> {
				reloadTracks();
				dismiss();
				return false;
			});
		}
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			Set<TrackItem> trackItems = itemsSelectionHelper.getSelectedItems();
			Set<TracksGroup> tracksGroups = groupsSelectionHelper.getSelectedItems();

			selectionHelper.setSelectedItems(foldersHelper.getSelectedTrackItems(trackItems, tracksGroups));
		}
		return selectionHelper;
	}

	public static void showInstance(
			@NonNull FragmentManager manager, @NonNull TracksGroup tracksGroup, @Nullable Fragment target,
			@Nullable Set<TrackItem> trackItems, @Nullable Set<TracksGroup> tracksGroups,
			@Nullable ScreenPositionData screenPositionData
	) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksSelectionFragment fragment = new TracksSelectionFragment();
			fragment.preselectedTrackItems = trackItems;
			fragment.preselectedTracksGroups = tracksGroups;
			fragment.screenPositionData = screenPositionData;
			fragment.setRetainInstance(true);
			fragment.setRootFolder(tracksGroup);
			if (tracksGroup instanceof TrackFolder trackFolder) {
				fragment.setSelectedFolder(trackFolder);
			}
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
