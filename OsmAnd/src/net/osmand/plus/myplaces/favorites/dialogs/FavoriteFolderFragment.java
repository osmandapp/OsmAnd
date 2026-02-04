package net.osmand.plus.myplaces.favorites.dialogs;

import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.isPersonalCategoryDisplayName;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.TYPE_EMPTY_FOLDER;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.TYPE_SORT_FAVORITE;

import android.graphics.drawable.ColorDrawable;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteMenu.FavoriteActionListener;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteFolderFragment extends BaseFavoriteListFragment
		implements SortFavoriteListener, FragmentStateHolder, CategorySelectionListener, FavoriteActionListener, FavoritesListener {

	public static final String TAG = FavoriteFolderFragment.class.getSimpleName();

	protected final ItemsSelectionHelper<FavouritePoint> selectionHelper = new ItemsSelectionHelper<>();

	private FavoriteGroup selectedGroup;
	private FavouritePoint selectedPoint;

	@Override
	protected int getLayoutId() {
		return R.layout.favorite_folder_fragment;
	}

	@Override
	public void onResume() {
		super.onResume();
		ActionBar ab = requireMyActivity().getSupportActionBar();
		if (ab != null) {
			ab.setTitle(selectedGroup.getDisplayName(app));
		}
		helper.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		helper.removeListener(this);
	}

	private void setupSelectionHelper() {
		selectionHelper.setAllItems(selectedGroup.getPoints());
	}

	protected FavoriteAdapterListener getFavoriteFolderListener() {
		return new FavoriteAdapterListener() {

			@Override
			public boolean isItemSelected(@NonNull Object object) {
				return object instanceof FavouritePoint favouritePoint && selectionHelper.isItemSelected(favouritePoint);
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
				}
			}

			@Override
			public void onItemLongClick(@NonNull Object object) {
				if (!isInSelectionMode()) {
					setSelectionMode(true);
					adapter.setSelectionMode(true);
				}
				if (object instanceof FavouritePoint favouritePoint && !selectionHelper.isItemSelected(favouritePoint)) {
					selectItem(object);
				}
			}

			@Override
			public void onActionButtonClick(@NonNull Object object, @NonNull View anchor) {
				if (object instanceof FavouritePoint favouritePoint) {
					selectedPoint = favouritePoint;
					FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
					menu.showPointOptionsMenu(anchor, favouritePoint, nightMode, FavoriteFolderFragment.this, FavoriteFolderFragment.this, FavoriteFolderFragment.this);
				}
			}

			@Override
			public void onEmptyStateClick() {
				importFavourites();
			}
		};
	}

	private void selectItem(Object object) {
		if (object instanceof FavouritePoint favouritePoint && isInSelectionMode()) {
			selectionHelper.onItemsSelected(Collections.singleton(favouritePoint), !selectionHelper.isItemSelected(favouritePoint));
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
		collection.replace(InsetTarget.createScrollable(android.R.id.list));
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
			AndroidUiHelper.setStatusBarColor(activity, ColorUtilities.getColor(app, ColorUtilities.getStatusBarActiveColorId(isNightMode())));
		} else {
			ab.setBackgroundDrawable(new ColorDrawable(ColorUtilities.getAppBarColor(app, isNightMode())));
			ab.setTitle(selectedGroup.getDisplayName(app));
			activity.updateStatusBarColor();
		}
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			FavoriteSortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap(), false, !selectedGroup.getPoints().isEmpty());
		}
	}

	@NonNull
	public FavoriteListSortMode getTracksSortMode() {
		FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
		return sortModesHelper.requireSortMode(selectedGroup.getDisplayName(app));
	}

	@Override
	public void setTracksSortMode(@NonNull FavoriteListSortMode sortMode, boolean sortSubFolders) {
		if (sortSubFolders) {
			//sortSubFolder(sortMode);
		} else {
			FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
			sortModesHelper.setSortMode(selectedGroup.getDisplayName(app), sortMode);
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

		if (Algorithms.isEmpty(selectedGroup.getPoints())) {
			items.add(TYPE_EMPTY_FOLDER);
		} else {
			items.addAll(selectedGroup.getPoints());
			items.add(new FavoriteFolderAnalysis(selectedGroup));
		}

		return items;
	}

	public void setSelectedGroup(FavoriteGroup selectedGroup) {
		this.selectedGroup = selectedGroup;
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
				SearchFavoriteFragment.showInstance(manager, this, selectedGroup.getPoints(), selectedGroup.getDisplayName(app), "");
			}
		} else if (item.getItemId() == R.id.action_menu) {

		} else if (item.getItemId() == R.id.more_button) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.more_button);
			menu.showFolderSelectOptionsMenu(view, nightMode);
		}
		return false;
	}

	@Override
	protected void setSelectionMode(boolean mode) {
		super.setSelectionMode(mode);
		if (!selectionMode) {
			ActionBar ab = requireMyActivity().getSupportActionBar();
			if (ab != null) {
				ab.setTitle(selectedGroup.getDisplayName(app));
			}
		}
	}

	@Override
	public void onCategorySelected(PointsGroup pointsGroup) {
		String category;
		if (isPersonalCategoryDisplayName(requireContext(), pointsGroup.getName())) {
			category = PERSONAL_CATEGORY;
		} else if (Algorithms.stringsEqual(pointsGroup.getName(), getString(R.string.shared_string_favorites))) {
			category = "";
		} else {
			category = pointsGroup.getName();
		}
		if (selectionMode) {
			for (FavouritePoint point : selectionHelper.getSelectedItems()) {
				helper.editFavouriteName(point, point.getName(), category, point.getDescription(), point.getAddress());
			}
		} else {
			helper.editFavouriteName(selectedPoint, selectedPoint.getName(), category, selectedPoint.getDescription(), selectedPoint.getAddress());
		}

		reloadData();
	}

	@Override
	public void onActionFinish() {
		updateContent();
	}

	@Override
	public void onSavingFavoritesFinished() {
		updateContent();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull FavoriteGroup group,
	                                @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			FavoriteFolderFragment fragment = new FavoriteFolderFragment();
			fragment.setSelectedGroup(group);
			fragment.setTargetFragment(target, 0);
			fragment.setRetainInstance(true);

			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
