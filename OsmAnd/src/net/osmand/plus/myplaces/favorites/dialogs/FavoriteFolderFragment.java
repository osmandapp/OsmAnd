package net.osmand.plus.myplaces.favorites.dialogs;

import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.TYPE_EMPTY_FOLDER;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.TYPE_SORT_FAVORITE;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteFolderFormatter;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteMenu.FavoriteActionListener;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.settings.enums.FavoriteListSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteFolderFragment extends BaseFavoriteListFragment
		implements SortFavoriteListener, FragmentStateHolder, CategorySelectionListener,
		FavoriteActionListener, FavoritesListener, OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = FavoriteFolderFragment.class.getSimpleName();
	protected static final String SELECTED_POINTS_KEY = "selected_points_key";
	protected static final String SELECTED_FOLDERS_KEY = "selected_folders_key";

	protected final ItemsSelectionHelper<Object> selectionHelper = new ItemsSelectionHelper<>(true);

	private FavouritePoint selectedPoint;
	private Location lastLocation;
	private float lastHeading;
	private boolean compassUpdateAllowed = true;
	private boolean locationUpdateStarted;

	@Override
	protected int getLayoutId() {
		return R.layout.favorite_folder_fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (selectedFolderPath == null && selectedGroup != null) {
			selectedFolderPath = selectedGroup.getName();
		}
		if (selectedFolderPath == null) {
			getParentFragmentManager().popBackStack();
			return;
		}
		FavoriteFolder selectedFolder = getSelectedFolder();
		if (selectedFolder == null) {
			getParentFragmentManager().popBackStack();
			return;
		}
		selectedGroup = selectedFolder.getGroup();
		selectionHelper.setAllItems(getSelectableItems());

		if (savedInstanceState != null && selectionMode) {
			ArrayList<String> selectedPointsNames = savedInstanceState.getStringArrayList(SELECTED_POINTS_KEY);
			ArrayList<String> selectedFolders = savedInstanceState.getStringArrayList(SELECTED_FOLDERS_KEY);
			for (Object item : getSelectableItems()) {
				if (item instanceof FavouritePoint point
						&& selectedPointsNames != null
						&& selectedPointsNames.contains(point.getName())) {
					selectionHelper.onItemsSelected(Collections.singletonList(item), true);
				} else if (item instanceof FavoriteFolder folder
						&& selectedFolders != null
						&& selectedFolders.contains(folder.getFullPath())) {
					selectionHelper.onItemsSelected(Collections.singletonList(item), true);
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<String> selectedPoints = new ArrayList<>();
		ArrayList<String> selectedFolders = new ArrayList<>();
		for (Object item : selectionHelper.getSelectedItems()) {
			if (item instanceof FavouritePoint point) {
				selectedPoints.add(point.getName());
			} else if (item instanceof FavoriteFolder folder && !Algorithms.isEmpty(folder.getFullPath())) {
				selectedFolders.add(folder.getFullPath());
			}
		}
		if (selectionMode) {
			outState.putStringArrayList(SELECTED_POINTS_KEY, selectedPoints);
			outState.putStringArrayList(SELECTED_FOLDERS_KEY, selectedFolders);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContent();
		if (selectionMode) {
			changeTitle(String.valueOf(selectionHelper.getSelectedItems().size()));
		} else {
			changeTitle(getSelectedFolderTitle());
		}
		helper.addListener(this);
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		helper.removeListener(this);
		stopLocationUpdate();
	}

	private void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			app.getLocationProvider().resumeAllUpdates();
			app.getLocationProvider().removeCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().addCompassListener(this);
			app.getLocationProvider().addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			app.getLocationProvider().removeLocationListener(this);
			app.getLocationProvider().removeCompassListener(this);
			app.getLocationProvider().addCompassListener(app.getLocationProvider().getNavigationInfo());
			app.getLocationProvider().pauseAllUpdates();
		}
	}


	@Override
	protected void setupViews(@NonNull View view) {
		super.setupViews(view);

		recyclerView.addOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				compassUpdateAllowed = newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
			}
		});
	}

	private void setupSelectionHelper() {
		selectionHelper.setAllItems(getSelectableItems());
	}

	protected FavoriteAdapterListener getFavoriteFolderListener() {
		return new FavoriteAdapterListener() {

			@Override
			public boolean isItemSelected(@NonNull Object object) {
				return isSelectableItem(object) && selectionHelper.isItemSelected(object);
			}

			@Override
			public void onItemSingleClick(@NonNull Object object) {
				if (isInSelectionMode()) {
					selectItem(object);
				} else if (object instanceof FavouritePoint point) {
					LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
					String pointType = PointDescription.POINT_TYPE_FAVORITE;
					requireMyActivity().showOnMap(FavoriteFolderFragment.this, location.getLatitude(), location.getLongitude(),
							settings.getLastKnownMapZoom(), new PointDescription(pointType, point.getDisplayName(app)), true, point);
				} else if (object instanceof FavoriteFolder folder) {
					openFolder(folder.getFullPath());
				}
			}

			@Override
			public void onItemLongClick(@NonNull Object object) {
				if (!isSelectableItem(object)) {
					return;
				}
				if (!isInSelectionMode()) {
					setSelectionMode(true);
					adapter.setSelectionMode(true);
				}
				if (!selectionHelper.isItemSelected(object)) {
					selectItem(object);
				}
			}

			@Override
			public void onActionButtonClick(@NonNull Object object, @NonNull View anchor) {
				if (object instanceof FavouritePoint favouritePoint) {
					selectedPoint = favouritePoint;
					FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
					menu.showPointOptionsMenu(anchor, favouritePoint, nightMode, FavoriteFolderFragment.this, FavoriteFolderFragment.this, FavoriteFolderFragment.this);
				} else if (object instanceof FavoriteFolder folder) {
					selectedPoint = null;
					FavoriteOptionsDialogFragment.showInstance(getChildFragmentManager(), folder.getFullPath());
				}
			}

			@Override
			public void onEmptyStateClick() {
				importFavourites();
			}
		};
	}

	private void selectItem(Object object) {
		if (isSelectableItem(object) && isInSelectionMode()) {
			selectionHelper.onItemsSelected(Collections.singleton(object), !selectionHelper.isItemSelected(object));
			changeTitle(String.valueOf(selectionHelper.getSelectedItems().size()));
			adapter.selectItem(object);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		requireMyActivity().updateToolbar();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteFoldersFragment foldersFragment) {
			foldersFragment.updateContent();
		}
	}

	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	@Override
	public void updateContent() {
		List<Object> items = getAdapterItems();
		setupSelectionHelper();
		FavoriteListSortMode sortMode = getTracksSortMode();
		sortItems(items, sortMode);

		adapter.setSortMode(sortMode);
		adapter.setItems(items);
	}

	@Override
	protected ItemsSelectionHelper<?> getSelectionHelper() {
		return selectionHelper;
	}

	@Override
	protected void updateSelectionToolbar() {
		MyPlacesActivity activity = requireMyActivity();
		ActionBar ab = activity.getSupportActionBar();
		if (ab == null) return;

		if (selectionMode) {
			ab.setHomeAsUpIndicator(R.drawable.ic_action_close);
		} else {
			setupHomeButton();
		}
		invalidateOptionsMenu(activity);

		if (selectionMode) {
			ab.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getToolbarActiveColor(app, isNightMode())));
			ab.setTitle(String.valueOf(selectionHelper.getSelectedItems().size()));
			AndroidUiHelper.setStatusBarColor(activity, ColorUtilities.getColor(app, ColorUtilities.getStatusBarActiveColorId(isNightMode())));
		} else {
			ab.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getAppBarColor(app, isNightMode())));
			ab.setTitle(getSelectedFolderTitle());
			activity.updateStatusBarColor();
		}
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			FavoriteSortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap(), false, !getExactPoints().isEmpty());
		}
	}

	@NonNull
	public FavoriteListSortMode getTracksSortMode() {
		FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
		return sortModesHelper.requireSortMode(getSelectedFolderSortId());
	}

	@Override
	public void setTracksSortMode(@NonNull FavoriteListSortMode sortMode, boolean sortSubFolders) {
		if (sortSubFolders) {
			//sortSubFolder(sortMode);
		} else {
			FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
			sortModesHelper.setSortMode(getSelectedFolderSortId(), sortMode);
			sortModesHelper.syncSettings();
			updateContent();
		}
	}

	private void sortItems(@NonNull List<Object> items, @NonNull FavoriteListSortMode sortMode) {
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		items.sort(new FavoriteComparator(sortMode, latLon, app));
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_FAVORITE);

		FavoriteFolder selectedFolder = getSelectedFolder();
		List<FavoriteFolder> childFolders = getChildFolders(selectedFolder);
		List<FavouritePoint> exactPoints = getExactPoints();
		if (Algorithms.isEmpty(childFolders) && Algorithms.isEmpty(exactPoints)) {
			items.add(TYPE_EMPTY_FOLDER);
		} else {
			items.addAll(childFolders);
			items.addAll(exactPoints);
			if (selectedFolder != null) {
				items.add(isRootExactFolder(selectedFolder) && selectedGroup != null
						? new FavoriteFolderAnalysis(selectedGroup)
						: new FavoriteFolderAnalysis(selectedFolder));
			}
		}

		return items;
	}

	public void setSelectedGroup(FavoriteGroup selectedGroup) {
		this.selectedGroup = selectedGroup;
		this.selectedFolderPath = selectedGroup.getName();
	}

	public void setSelectedFolderPath(@NonNull String selectedFolderPath) {
		this.selectedFolderPath = selectedFolderPath;
		selectedGroup = helper != null ? helper.getGroup(selectedFolderPath) : null;
	}

	@NonNull
	protected MyPlacesActivity requireMyActivity() {
		return (MyPlacesActivity) requireActivity();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home && isInSelectionMode()) {
			exitSelectionMode();
			return true;
		} else if (item.getItemId() == R.id.select_all && isInSelectionMode()) {
			if (selectionHelper.isAllItemsSelected()) {
				selectionHelper.clearSelectedItems();
			} else {
				selectionHelper.selectAllItems();
			}
			changeTitle(String.valueOf(selectionHelper.getSelectedItems().size()));
			adapter.updateSelectionAllItems();
			return true;
		} else if (item.getItemId() == R.id.action_search) {
			FragmentManager manager = getFragmentManager();
			if (manager != null) {
				SearchFavoriteFragment.showInstance(manager, this, getSearchPoints(), getSelectedFolderSortId(), "", false);
			}
		} else if (item.getItemId() == R.id.action_menu) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.action_menu);
			FavoriteFolder selectedFolder = getSelectedFolder();
			if (selectedFolder != null) {
				selectedPoint = null;
				menu.showFolderOptionsMenu(requireMyActivity(), view, selectedFolder, nightMode,
						FavoriteFolderFragment.this, FavoriteFolderFragment.this, FavoriteFolderFragment.this);
			}
		} else if (item.getItemId() == R.id.more_button) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.more_button);
			if (selectionMode) {
				FavoriteSelection selection = new FavoriteSelection(selectionHelper.getSelectedItems());
				if (selection.isOnlyPoints()) {
					menu.showPointsSelectOptionsMenu(view, selection.getPoints(), selectedGroup, nightMode,
							FavoriteFolderFragment.this, FavoriteFolderFragment.this, FavoriteFolderFragment.this);
				} else if (selection.hasFolders()) {
					menu.showDeleteSelectionOptionsMenu(view, selection, nightMode, FavoriteFolderFragment.this);
				}
			}
		}
		return false;
	}

	@Override
	protected void setSelectionMode(boolean mode) {
		super.setSelectionMode(mode);
		changeTitle(selectionMode
				? String.valueOf(selectionHelper.getSelectedItems().size())
				: getSelectedFolderTitle());
	}

	@Override
	public void onCategorySelected(PointsGroup pointsGroup) {
		String category = FavoriteGroup.getCategoryFromPointGroup(app, pointsGroup);
		if (selectionMode) {
			FavoriteSelection selection = new FavoriteSelection(selectionHelper.getSelectedItems());
			helper.editFavouritesGroup(new ArrayList<>(selection.getPoints()), category);
			selectionHelper.clearSelectedItems();
			changeTitle(String.valueOf(selectionHelper.getSelectedItems().size()));
			updateContent();
		} else if (selectedPoint != null) {
			helper.editFavouriteName(selectedPoint, selectedPoint.getName(), category, selectedPoint.getDescription(), selectedPoint.getAddress());
		} else {
			FavoriteGroup group = helper.getGroup(pointsGroup.getName());
			if (group != null) {
				helper.saveSelectedGroupsIntoFile(Collections.singletonList(group), true);
			}
		}

		updateContent();
	}

	@Override
	public void onActionFinish() {
		updateContent();
		if (selectionMode) {
			selectionHelper.clearSelectedItems();
			exitSelectionMode();
			Fragment fragment = getTargetFragment();
			if (fragment instanceof FavoriteFoldersFragment foldersFragment) {
				foldersFragment.updateContent();
			}
		} else {
			changeTitle(getSelectedFolderTitle());
		}
	}

	@Override
	public void updateCompassValue(float heading) {
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			lastHeading = heading;
			updateLocationUi();
		}
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(lastLocation, location)) {
			lastLocation = location;
			updateLocationUi();
		}
	}

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		if (adapter != null) {
			adapter.updateLocationAllItems();
		}
	}

	@Override
	public void onSavingFavoritesFinished() {
		updateContent();
	}

	@Nullable
	private FavoriteFolder getSelectedFolder() {
		if (selectedFolderPath == null) {
			return null;
		}
		FavoriteFolder folder = helper.getFavoriteFolder(selectedFolderPath);
		selectedGroup = folder != null ? folder.getGroup() : null;
		return folder;
	}

	@NonNull
	private List<FavoriteFolder> getChildFolders(@Nullable FavoriteFolder selectedFolder) {
		if (selectedFolder == null || isRootExactFolder(selectedFolder)) {
			return Collections.emptyList();
		}
		return selectedFolder.getSubFolders();
	}

	private boolean isRootExactFolder(@NonNull FavoriteFolder selectedFolder) {
		return selectedFolder.isRoot() && selectedFolder.getGroup() != null;
	}

	@NonNull
	private List<FavouritePoint> getExactPoints() {
		return selectedGroup != null ? selectedGroup.getPoints() : Collections.emptyList();
	}

	@NonNull
	private List<Object> getSelectableItems() {
		List<Object> items = new ArrayList<>();
		FavoriteFolder selectedFolder = getSelectedFolder();
		items.addAll(getChildFolders(selectedFolder));
		items.addAll(getExactPoints());
		return items;
	}

	private boolean isSelectableItem(@NonNull Object object) {
		return object instanceof FavouritePoint
				|| (object instanceof FavoriteFolder folder && !Algorithms.isEmpty(folder.getFullPath()));
	}

	@NonNull
	private List<FavouritePoint> getSearchPoints() {
		FavoriteFolder selectedFolder = getSelectedFolder();
		if (selectedFolder == null) {
			return Collections.emptyList();
		}
		if (isRootExactFolder(selectedFolder)) {
			return new ArrayList<>(getExactPoints());
		}
		List<FavouritePoint> points = new ArrayList<>();
		for (FavoriteGroup group : helper.getFavoriteGroupsInSubtree(selectedFolder.getFullPath())) {
			points.addAll(group.getPoints());
		}
		return points;
	}

	@NonNull
	private String getSelectedFolderTitle() {
		return FavoriteFolderFormatter.getDisplayName(app, selectedFolderPath);
	}

	@NonNull
	private String getSelectedFolderSortId() {
		return selectedFolderPath != null ? selectedFolderPath : "";
	}

	private void openFolder(@NonNull String folderPath) {
		FragmentManager manager = getParentFragmentManager();
		Fragment target = getTargetFragment();
		FavoriteFolderFragment.showInstance(manager, folderPath, target != null ? target : this);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull FavoriteGroup group,
	                                @Nullable Fragment target) {
		showInstance(manager, group.getName(), target);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String folderPath,
	                                @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FavoriteFolderFragment fragment = new FavoriteFolderFragment();
			fragment.setSelectedFolderPath(folderPath);
			fragment.setTargetFragment(target, 0);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
