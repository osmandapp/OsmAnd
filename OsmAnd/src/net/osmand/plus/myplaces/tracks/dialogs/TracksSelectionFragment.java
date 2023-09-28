package net.osmand.plus.myplaces.tracks.dialogs;

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

import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.plugins.osmedit.asynctasks.UploadGPXFilesTask.UploadGpxListener;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.track.data.TracksGroup;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class TracksSelectionFragment extends BaseTrackFolderFragment implements UploadGpxListener {

	public static final String TAG = TracksSelectionFragment.class.getSimpleName();

	private ItemsSelectionHelper<TrackItem> itemsSelectionHelper = new ItemsSelectionHelper<>();
	private ItemsSelectionHelper<TracksGroup> groupsSelectionHelper = new ItemsSelectionHelper<>();

	@Nullable
	private Set<TrackItem> preselectedTrackItems;
	@Nullable
	private Set<TracksGroup> preselectedTracksGroups;
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
	public void setRootFolder(@NonNull TrackFolder rootFolder) {
		super.setRootFolder(rootFolder);
		itemsSelectionHelper.clearSelectedItems();
		groupsSelectionHelper.clearSelectedItems();

		itemsSelectionHelper.setAllItems(rootFolder.getTrackItems());
		groupsSelectionHelper.setAllItems(rootFolder.getSubFolders());

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

		return view;
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
				foldersHelper.showItemsOptionsMenu(view, rootFolder, trackItems, tracksGroups, this, this, this, isNightMode());
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
			int selectedSize = itemsSelectionHelper.getSelectedItemsSize() + groupsSelectionHelper.getSelectedItemsSize();
			actionBar.setTitle(String.valueOf(selectedSize));
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		MyPlacesActivity activity = getMyActivity();
		if (activity != null) {
			activity.updateToolbar();
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
		if (rootFolder.equals(selectedFolder)) {
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
	public void onGpxUploaded(String result) {
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder trackFolder,
	                                @Nullable Fragment target, @Nullable Set<TrackItem> trackItems,
	                                @Nullable Set<TracksGroup> tracksGroups) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TracksSelectionFragment fragment = new TracksSelectionFragment();
			fragment.preselectedTrackItems = trackItems;
			fragment.preselectedTracksGroups = tracksGroups;
			fragment.setRetainInstance(true);
			fragment.setRootFolder(trackFolder);
			fragment.setSelectedFolder(trackFolder);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
