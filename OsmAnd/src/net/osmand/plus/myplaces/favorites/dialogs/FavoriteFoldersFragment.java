package net.osmand.plus.myplaces.favorites.dialogs;

import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.*;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoriteFoldersFragment extends BaseFavoriteListFragment
		implements SortFavoriteListener, FragmentStateHolder, CategorySelectionListener {

	protected final ItemsSelectionHelper<FavoriteGroup> selectionHelper = new ItemsSelectionHelper<>();

	private final List<FavoriteGroup> groups = new ArrayList<>();

	@Override
	protected int getLayoutId() {
		return R.layout.favorite_folders_fragment;
	}

	private void setupSelectionHelper() {
		selectionHelper.setAllItems(groups);
	}

	protected FavoriteAdapterListener getFavoriteFolderListener(){
		return new FavoriteAdapterListener() {

			@Override
			public boolean isItemSelected(@NonNull Object object) {
				return object instanceof FavoriteGroup favoriteGroup && selectionHelper.isItemSelected(favoriteGroup);
			}

			@Override
			public void onItemSingleClick(@NonNull Object object) {
				if (isInSelectionMode()) {
					selectItem(object);
				} else if (object instanceof FavoriteGroup favoriteGroup) {
					openGroup(favoriteGroup);
				}
			}

			@Override
			public void onItemLongClick(@NonNull Object object) {
				if (!isInSelectionMode()) {
					setSelectionMode(true);
					adapter.setSelectionMode(true);
				}
				if (object instanceof FavoriteGroup favoriteGroup && !selectionHelper.isItemSelected(favoriteGroup)) {
					selectItem(object);
				}
			}

			@Override
			public void onActionButtonClick(@NonNull Object object, @NonNull View anchor) {
				FragmentManager manager = getChildFragmentManager();
				if (object instanceof FavoriteGroup favoriteGroup) {
					FavoriteOptionsDialogFragment.showInstance(manager, favoriteGroup.getName());
				}
			}

			@Override
			public void onEmptyStateClick() {

			}
		};
	}

	private void selectItem(@Nullable Object object){
		if (object instanceof FavoriteGroup favoriteGroup && isInSelectionMode()) {
			selectionHelper.onItemsSelected(Collections.singleton(favoriteGroup), !selectionHelper.isItemSelected(favoriteGroup));
			changeTitle(String.valueOf(selectionHelper.getSelectedItems().size()));
			adapter.selectItem(object);
		}
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

	private void sortItems(@NonNull List<Object> items, @NonNull FavoriteListSortMode sortMode) {
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		items.sort(new FavoriteComparator(sortMode, latLon, app));
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			FavoriteSortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap(), false, false);
		}
	}

	@NonNull
	public FavoriteListSortMode getTracksSortMode() {
		FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
		return sortModesHelper.requireSortMode("");
	}

	@Override
	public void setTracksSortMode(@NonNull FavoriteListSortMode sortMode, boolean sortSubFolders) {
		if (sortSubFolders) {
			//sortSubFolder(sortMode);
		} else {
			FavoriteSortModesHelper sortModesHelper = app.getFavoriteSortModesHelper();
			sortModesHelper.setSortMode("", sortMode);
			sortModesHelper.syncSettings();
			updateContent();
		}
	}

	@NonNull
	protected List<Object> getAdapterItems() {
		groups.clear();

		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_FAVORITE);

		List<FavoriteGroup> favoriteGroups = helper.getFavoriteGroups();

		groups.addAll(favoriteGroups);
		items.addAll(groups);

		items.add(new FavoriteFolderAnalysis(groups));
		return items;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createScrollable(android.R.id.list));
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		FragmentManager manager = getParentFragmentManager();
		Fragment fragment = manager.findFragmentByTag(FavoriteFolderFragment.TAG);
		if (fragment != null) {
			return false;
		}

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
			List<FavouritePoint> list = new ArrayList<>();
			for (FavoriteGroup group : groups) {
				list.addAll(group.getPoints());
			}
			SearchFavoriteFragment.showInstance(manager, this, list, "", "");
			return true;
		} else if (item.getItemId() == R.id.action_menu) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.action_menu);
			menu.showFolderOptionsMenu(requireMyActivity(), view, nightMode, this, this);
			return true;
		} else if (item.getItemId() == R.id.more_button) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.more_button);
			menu.showFolderSelectOptionsMenu(view, nightMode);
			return true;
		}
		return false;
	}

	@NonNull
	protected MyPlacesActivity requireMyActivity() {
		return (MyPlacesActivity) requireActivity();
	}

	@Override
	public void restoreState(Bundle bundle) {
		super.restoreState(bundle);

		if (selectedGroup != null) {
			openGroup(selectedGroup);
		}
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
			ab.setTitle(R.string.shared_string_my_places);
			activity.updateStatusBarColor();
		}
	}

	private void openGroup(@NonNull FavoriteGroup group) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager manager = getParentFragmentManager();
			FavoriteFolderFragment.showInstance(manager, group, FavoriteFoldersFragment.this);
		}
	}

	@Override
	public void onCategorySelected(@NonNull PointsGroup pointsGroup) {
		FavoriteGroup group = helper.getGroup(pointsGroup.getName());
		if (group != null) {
			helper.saveSelectedGroupsIntoFile(Collections.singletonList(group), true);
		}
		reloadData();
	}
}
