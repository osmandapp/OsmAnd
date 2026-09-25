package net.osmand.plus.myplaces.tracks.dialogs;

import static net.osmand.plus.utils.AndroidUtils.getViewOnScreenY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.tracks.DialogClosedListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.SearchMyPlacesTracksFragment;
import net.osmand.plus.myplaces.tracks.TrackFoldersHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.Set;

public class TrackFolderFragment extends BaseTrackFolderFragment {

	public static final String TAG = TrackFolderFragment.class.getSimpleName();

	private ProgressBar progressBar;

	@Override
	protected int getLayoutId() {
		return R.layout.track_folder_fragment;
	}

	@NonNull
	@Override
	public String getFragmentTag() {
		return TAG;
	}

	@Nullable
	protected TracksGroup getCurrentTrackGroup() {
		return selectedFolder;
	}

	private boolean isLoadingItems;

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

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			setupProgressBar(view);
			setupSwipeRefresh(view);
		}
		updateContent();
		return view;
	}

	protected void setupProgressBar(@NonNull View view) {
		progressBar = view.findViewById(R.id.progress_bar);
		updateProgress();
	}

	protected void updateProgress() {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		boolean importing = foldersHelper != null && foldersHelper.isImporting();
		AndroidUiHelper.updateVisibility(progressBar, importing);
	}

	private void setupSwipeRefresh(@NonNull View view) {
		SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipe_refresh);
		swipeRefresh.setColorSchemeColors(ContextCompat.getColor(app, nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange));
		swipeRefresh.setOnRefreshListener(() -> {
			reloadTracks(true);
			swipeRefresh.setRefreshing(false);
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_folder_search) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				TracksGroup group = getCurrentTrackGroup();
				TrackFolder currentFolder = group instanceof TrackFolder ? (TrackFolder) group : null;
				FragmentManager manager = activity.getSupportFragmentManager();
				SearchMyPlacesTracksFragment.showInstance(manager,
						getTargetFragment(),
						false,
						isUsedOnMap(),
						null,
						null,
						new DialogClosedListener() {
							@Override
							public void onDialogClosed() {
								updateContent();
							}
						},
						currentFolder);
				return true;
			}
		} else if (itemId == R.id.action_folder_menu) {
			return showFolderOptionMenu();
		}
		return false;
	}

	protected boolean showFolderOptionMenu() {
		FragmentActivity activity = getActivity();
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null && activity != null) {
			View view = activity.findViewById(R.id.action_folder_menu);
			foldersHelper.showFolderOptionsMenu(selectedFolder, view, this, isRootFolder());
			return true;
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.myplaces_tracks_folder_menu, menu);
		requireMyActivity().setToolbarVisibility(false);
	}

	protected void onBackPressed() {
		if (isRootFolder()) {
			dismiss();
			updateTitle();
		} else {
			selectedFolder = selectedFolder.getParentFolder();
			updateContent();
		}
	}

	private boolean isRootFolder() {
		return Algorithms.objectEquals(rootFolder, selectedFolder);
	}

	@Override
	public void updateContent() {
		super.updateContent();
		updateTitle();
	}

	private void updateTitle() {
		TracksGroup group = getCurrentTrackGroup();
		MyPlacesActivity activity = getMyActivity();
		ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
		if (actionBar != null && group != null) {
			actionBar.setTitle(group.getName());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateProgress();
		restoreState(getArguments());
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
	public void onImportStarted() {
		AndroidUiHelper.updateVisibility(progressBar, true);
	}

	@Override
	public void onImportFinished() {
		AndroidUiHelper.updateVisibility(progressBar, false);
	}

	@Override
	public void onTrackItemOptionsSelected(@NonNull View view, @NonNull TrackItem trackItem) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			foldersHelper.showItemOptionsMenu(trackItem, view, this);
		}
	}

	@Override
	public void onTrackItemLongClick(@NonNull View view, @NonNull TrackItem trackItem) {
		ScreenPositionData positionData = new ScreenPositionData(trackItem, getViewOnScreenY(view));
		showTracksSelection(trackItem, null, positionData);
	}

	@Override
	public void onTracksGroupLongClick(@NonNull View view, @NonNull TracksGroup group) {
		ScreenPositionData positionData = new ScreenPositionData(group, getViewOnScreenY(view));
		showTracksSelection(null, group, positionData);
	}

	private void showTracksSelection(@Nullable TrackItem trackItem, @Nullable TracksGroup tracksGroup,
	                                 @Nullable ScreenPositionData screenPositionData) {
		TrackFoldersHelper foldersHelper = getTrackFoldersHelper();
		if (foldersHelper != null) {
			if (selectedFolder != null) {
				Set<TrackItem> trackItems = trackItem != null ? Collections.singleton(trackItem) : null;
				Set<TracksGroup> tracksGroups = tracksGroup != null ? Collections.singleton(tracksGroup) : null;
				foldersHelper.showTracksSelection(selectedFolder, this, trackItems, tracksGroups, screenPositionData);
			} else if (smartFolder != null) {
				Set<TrackItem> trackItems = trackItem != null ? Collections.singleton(trackItem) : null;
				foldersHelper.showTracksSelection(smartFolder, this, trackItems, null, screenPositionData);
			}
		}
	}

	@Override
	public void onTracksGroupSelected(@NonNull TracksGroup group, boolean selected) {
		if (group instanceof TrackFolder) {
			setSelectedFolder((TrackFolder) group);
		}
		updateContent();
	}

	@Override
	public void restoreState(Bundle bundle) {
		super.restoreState(bundle);

		if (rootFolder != null && !Algorithms.isEmpty(selectedItemPath)) {
			TrackItem trackItem = geTrackItem(rootFolder, selectedItemPath);
			if (trackItem != null) {
				int index = adapter.getItemPosition(trackItem);
				if (index != -1) {
					recyclerView.scrollToPosition(index);
				}
			}
			selectedItemPath = null;
		}
	}

	@NonNull
	@Override
	public ItemsSelectionHelper<TrackItem> getSelectionHelper() {
		ItemsSelectionHelper<TrackItem> selectionHelper = new ItemsSelectionHelper<>();
		selectionHelper.setAllItems(selectedFolder.getFlattenedTrackItems());
		selectionHelper.setSelectedItems(selectedFolder.getFlattenedTrackItems());
		selectionHelper.setOriginalSelectedItems(selectedFolder.getFlattenedTrackItems());
		return selectionHelper;
	}

	@Override
	protected Object getEmptyItem() {
		Object emptyItem;
		if (isLoadingItems) {
			emptyItem = smartFolder == null ? TrackFoldersAdapter.TYPE_EMPTY_FOLDER_LOADING : TrackFoldersAdapter.TYPE_EMPTY_SMART_FOLDER_LOADING;
		} else {
			emptyItem = smartFolder == null ? TrackFoldersAdapter.TYPE_EMPTY_FOLDER : TrackFoldersAdapter.TYPE_EMPTY_SMART_FOLDER;
		}
		return emptyItem;
	}

	public void setLoadingItems(boolean isLoadingItems) {
		this.isLoadingItems = isLoadingItems;
		updateContent();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackFolder folder, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TrackFolderFragment fragment = new TrackFolderFragment();
			fragment.setRootFolder(folder);
			fragment.setSelectedFolder(folder);
			fragment.setTargetFragment(target, 0);
			fragment.setRetainInstance(true);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}