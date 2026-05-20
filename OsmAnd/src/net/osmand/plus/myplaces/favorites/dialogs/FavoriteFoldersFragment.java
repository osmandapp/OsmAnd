package net.osmand.plus.myplaces.favorites.dialogs;

import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.*;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment.IMPORT_FAVOURITES_REQUEST;

import android.content.Intent;
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

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteFolder;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.enums.FavoriteListSortMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class FavoriteFoldersFragment extends BaseFavoriteListFragment
		implements SortFavoriteListener, FragmentStateHolder, CategorySelectionListener, BaseCard.CardListener {

	protected static final String SELECTED_GROUPS_KEY = "selected_groups_key";

	protected final ItemsSelectionHelper<Object> selectionHelper = new ItemsSelectionHelper<>();

	private final List<Object> folderItems = new ArrayList<>();

	@Override
	protected int getLayoutId() {
		return R.layout.favorite_folders_fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		selectionHelper.setAllItems(getRootFolderItems());

		if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_GROUPS_KEY)) {
			ArrayList<String> selectedFolders = savedInstanceState.getStringArrayList(SELECTED_GROUPS_KEY);
			if (selectedFolders != null) {
				List<Object> rootItems = getRootFolderItems();
				for (String folderPath : selectedFolders) {
					for (Object item : rootItems) {
						if (Algorithms.stringsEqual(getFolderPath(item), folderPath)) {
							selectionHelper.onItemsSelected(Collections.singletonList(item), true);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<String> selectedFolders = new ArrayList<>();
		for (Object item : selectionHelper.getSelectedItems()) {
			String folderPath = getFolderPath(item);
			if (folderPath != null) {
				selectedFolders.add(folderPath);
			}
		}
		if (selectionMode) {
			outState.putStringArrayList(SELECTED_GROUPS_KEY, selectedFolders);
		}
	}

	private void setupSelectionHelper() {
		selectionHelper.setAllItems(folderItems);
	}

	@Override
	@NonNull
	protected FavoriteFoldersAdapter createAdapter() {
		return new FavoriteFoldersAdapter(requireMyActivity(), nightMode, false, getFavoriteFolderListener(), this);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateContent();
	}

	protected FavoriteAdapterListener getFavoriteFolderListener(){
		return new FavoriteAdapterListener() {

			@Override
			public boolean isItemSelected(@NonNull Object object) {
				return isFolderItem(object) && selectionHelper.isItemSelected(object);
			}

			@Override
			public void onItemSingleClick(@NonNull Object object) {
				if (isInSelectionMode()) {
					selectItem(object);
				} else if (isFolderItem(object)) {
					openFolder(getFolderPath(object));
				}
			}

			@Override
			public void onItemLongClick(@NonNull Object object) {
				if (!isFolderItem(object)) {
					return;
				}
				if (!isInSelectionMode()) {
					setSelectionMode(true);
					adapter.setSelectionMode(true);
				}
				if (isFolderItem(object) && !selectionHelper.isItemSelected(object)) {
					selectItem(object);
				}
			}

			@Override
			public void onActionButtonClick(@NonNull Object object, @NonNull View anchor) {
				FragmentManager manager = getChildFragmentManager();
				String folderPath = getFolderPath(object);
				if (folderPath != null) {
					FavoriteOptionsDialogFragment.showInstance(manager, folderPath);
				}
			}

			@Override
			public void onEmptyStateClick() {
				Intent intent = ImportHelper.getImportFileIntent();
				AndroidUtils.startActivityForResultIfSafe(FavoriteFoldersFragment.this, intent, IMPORT_FAVOURITES_REQUEST);
			}
		};
	}

	private void selectItem(@Nullable Object object){
		if (isFolderItem(object) && isInSelectionMode()) {
			selectionHelper.onItemsSelected(Collections.singleton(object), !selectionHelper.isItemSelected(object));
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
		folderItems.clear();

		List<Object> items = new ArrayList<>();
		List<FavoriteGroup> favoriteGroups = helper.getFavoriteGroups();
		if (FavoritesFreeBackupCard.shouldShow(app, favoriteGroups)) {
			items.add(TYPE_FREE_BACKUP_CARD);
		}
		items.add(TYPE_SORT_FAVORITE);

		if (Algorithms.isEmpty(favoriteGroups)) {
			items.add(TYPE_EMPTY_FOLDERS);
		} else {
			FavoriteFolder rootFolder = helper.getFavoriteFolderRoot();
			folderItems.addAll(getRootFolderItems(rootFolder));
			items.addAll(folderItems);
			items.add(new FavoriteFolderAnalysis(rootFolder));
		}

		return items;
	}

	@NonNull
	private List<Object> getRootFolderItems() {
		return getRootFolderItems(helper.getFavoriteFolderRoot());
	}

	@NonNull
	private List<Object> getRootFolderItems(@NonNull FavoriteFolder rootFolder) {
		List<Object> items = new ArrayList<>();
		FavoriteGroup rootGroup = rootFolder.getGroup();
		if (rootGroup != null) {
			items.add(rootGroup);
		}
		items.addAll(rootFolder.getSubFolders());
		return items;
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
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
			SearchFavoriteFragment.showInstance(manager, this, helper.getFavouritePoints(), "", "");
			return true;
		} else if (item.getItemId() == R.id.action_menu) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.action_menu);
			menu.showFoldersOptionsMenu(requireMyActivity(), view, nightMode, this, this);
			return true;
		} else if (item.getItemId() == R.id.more_button) {
			FavoriteMenu menu = new FavoriteMenu(app, uiUtilities, requireMyActivity());
			View view = requireMyActivity().findViewById(R.id.more_button);
			FavoriteSelection selection = new FavoriteSelection(selectionHelper.getSelectedItems());
			if (selection.isOnlyExactGroups()) {
				menu.showFolderSelectionOptionsMenu(requireMyActivity(), view, new LinkedHashSet<>(selection.getExactGroups()), nightMode, this);
			} else if (selection.hasFolders()) {
				menu.showDeleteSelectionOptionsMenu(view, selection, nightMode, this);
			}
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

		if (selectedFolderPath != null) {
			String folderPath = selectedFolderPath;
			selectedFolderPath = null;
			selectedGroup = null;
			openFolder(folderPath);
		}
	}

	@Override
	protected void setSelectionMode(boolean mode) {
		if (mode != selectionMode) {
			requireMyActivity().animateShowHideTabs(mode);
		}
		super.setSelectionMode(mode);
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
			ab.setTitle(R.string.shared_string_my_places);
			activity.updateStatusBarColor();
		}
	}

	private void openFolder(@Nullable String folderPath) {
		FragmentActivity activity = getActivity();
		if (activity != null && folderPath != null) {
			FragmentManager manager = getParentFragmentManager();
			FavoriteFolderFragment.showInstance(manager, folderPath, FavoriteFoldersFragment.this);
		}
	}

	private boolean isFolderItem(@Nullable Object object) {
		return object instanceof FavoriteGroup || object instanceof FavoriteFolder;
	}

	@Nullable
	private String getFolderPath(@Nullable Object object) {
		if (object instanceof FavoriteGroup group) {
			return group.getName();
		} else if (object instanceof FavoriteFolder folder) {
			return folder.getFullPath();
		}
		return null;
	}

	@Override
	public void onCategorySelected(@NonNull PointsGroup pointsGroup) {
		FavoriteGroup group = helper.getGroup(pointsGroup.getName());
		if (group != null) {
			helper.saveSelectedGroupsIntoFile(Collections.singletonList(group), true);
		}
		reloadData();
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof FavoritesFreeBackupCard) {
			updateContent();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		if (card instanceof FavoritesFreeBackupCard
				&& buttonIndex == FavoritesFreeBackupCard.GET_OSMAND_CLOUD_BUTTON_INDEX) {
			requireMyActivity().showOsmAndCloud(this);
		}
	}
}
