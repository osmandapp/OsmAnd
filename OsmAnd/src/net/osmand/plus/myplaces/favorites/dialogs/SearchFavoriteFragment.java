package net.osmand.plus.myplaces.favorites.dialogs;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.PERSONAL_CATEGORY;
import static net.osmand.plus.myplaces.favorites.FavoriteGroup.isPersonalCategoryDisplayName;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.TYPE_EMPTY_SEARCH;
import static net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.TYPE_SORT_FAVORITE;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.editors.SelectPointsCategoryBottomSheet.CategorySelectionListener;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteFoldersAdapter.FavoriteAdapterListener;
import net.osmand.plus.myplaces.favorites.dialogs.FavoriteMenu.FavoriteActionListener;
import net.osmand.plus.myplaces.favorites.dialogs.SortFavoriteViewHolder.SortFavoriteListener;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper.SelectionHelperProvider;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchFavoriteFragment extends BaseFullScreenDialogFragment implements
		SortFavoriteListener, FragmentStateHolder, CategorySelectionListener, FavoriteActionListener, FavoritesListener {

	public static final String TAG = SearchFavoriteFragment.class.getSimpleName();

	public static final String FAVORITE_SEARCH_QUERY_KEY = "favorite_search_query_key";
	public static final String FAVORITE_SEARCH_GROUP_KEY = "favorite_search_group_key";

	protected final ItemsSelectionHelper<FavouritePoint> selectionHelper = new ItemsSelectionHelper<>();
	private FavouritesHelper helper;

	private String groupKey;
	private FavouritePoint selectedPoint;
	private List<FavouritePoint> points = new ArrayList<>();

	protected boolean selectionMode;

	private FavoriteFoldersAdapter adapter;
	protected View clearSearchQuery;
	protected EditText searchEditText;
	private Filter myFilter;
	private Set<?> filter;
	private View searchContainer;
	private TextView selectedCountTv;
	private String searchQuery;
	private ImageButton actionButton;

	private ImageButton selectButton;
	private ImageButton backButton;
	private View appbar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!selectionHelper.hasAnyItems()) {
			setupSelectionHelper();
		}
		helper = app.getFavoritesHelper();

		Bundle arguments = getArguments();
		if (savedInstanceState != null) {
			searchQuery = savedInstanceState.getString(FAVORITE_SEARCH_QUERY_KEY);
			groupKey = savedInstanceState.getString(FAVORITE_SEARCH_GROUP_KEY);
			savedInstanceState.remove(FAVORITE_SEARCH_QUERY_KEY);
			savedInstanceState.remove(FAVORITE_SEARCH_GROUP_KEY);
		}
		if (searchQuery == null && arguments != null) {
			searchQuery = arguments.getString(FAVORITE_SEARCH_QUERY_KEY);
			groupKey = arguments.getString(FAVORITE_SEARCH_GROUP_KEY);
			arguments.remove(FAVORITE_SEARCH_QUERY_KEY);
			arguments.remove(FAVORITE_SEARCH_GROUP_KEY);
		}
		if (searchQuery == null) {
			searchQuery = "";
		}
		if (groupKey == null) {
			groupKey = "";
		}

		if (points.isEmpty()) {
			boolean includeAll = groupKey.isEmpty();
			for (FavoriteGroup group : helper.getFavoriteGroups()) {
				if (includeAll) {
					points.addAll(group.getPoints());
				} else if (group.getName().equals(groupKey)) {
					points.addAll(group.getPoints());
					break;
				}
			}
		}
	}

	public void setupSelectionHelper() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof ItemsSelectionHelper.SelectionHelperProvider) {
			SelectionHelperProvider<FavouritePoint> helperProvider = (SelectionHelperProvider<FavouritePoint>) fragment;
			ItemsSelectionHelper<FavouritePoint> originalHelper = helperProvider.getSelectionHelper();
			selectionHelper.syncWith(originalHelper);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.search_myplaces_tracks_fragment, container, false);
		view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.activity_background_color_dark : R.color.list_background_color_light));

		setHasOptionsMenu(true);
		if (!selectionHelper.hasAnyItems()) {
			setupSelectionHelper();
		}
		helper = app.getFavoritesHelper();

		adapter = new FavoriteFoldersAdapter(requireMyPlacesActivity(), nightMode, getFavoriteFolderListener());
		adapter.setSortFavoriteListener(this);

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);

		setupToolbar(view);
		setupSearch(view);

		updateContent();
		updateToolbar();
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (!Algorithms.isEmpty(searchQuery)) {
			searchEditText.setText(searchQuery);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		helper.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		helper.removeListener(this);
	}

	FavoriteAdapterListener getFavoriteFolderListener() {
		return new FavoriteAdapterListener() {

			@Override
			public boolean isItemSelected(@NonNull Object object) {
				return object instanceof FavouritePoint favouritePoint && selectionHelper.isItemSelected(favouritePoint);
			}

			@Override
			public void onItemSingleClick(@NonNull Object object) {
				if (selectionMode) {
					selectItem(object);
				} else if (object instanceof FavouritePoint point) {
					showOnMap(point);
				}
			}

			@Override
			public void onItemLongClick(@NonNull Object object) {
				if (!selectionMode) {
					setSelectionMode(true);
					adapter.setSelectionMode(true);
				}
				if (object instanceof FavouritePoint favouritePoint && !selectionHelper.isItemSelected(favouritePoint)) {
					selectItem(object);
				}
			}

			@Override
			public void onActionButtonClick(@NonNull Object object, @NonNull View anchor) {
				if (object instanceof FavouritePoint point) {
					selectedPoint = point;
					FavoriteMenu menu = new FavoriteMenu(app, app.getUIUtilities(), requireMyPlacesActivity());
					menu.showPointOptionsMenu(anchor, point, nightMode,
							SearchFavoriteFragment.this, SearchFavoriteFragment.this, SearchFavoriteFragment.this);
				}
			}

			@Override
			public void onEmptyStateClick() {
				if (!searchEditText.getText().toString().isEmpty()) {
					searchEditText.setText("");
					searchEditText.setSelection(0);
				}
			}
		};
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof FavoriteFoldersFragment foldersFragment) {
			foldersFragment.updateContent();
		}
	}

	private void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		if (!selectionMode) {
			selectionHelper.clearSelectedItems();
		}
		hideKeyboard();
		adapter.setSelectionMode(selectionMode);
		updateToolbar();
		updateStatusBar();
	}

	private void updateStatusBar() {
		appbar.setBackgroundColor(selectionMode
				? ColorUtilities.getToolbarActiveColor(app, nightMode)
				: ColorUtilities.getAppBarColor(app, nightMode));
		setStatusBarBackgroundColor(selectionMode
				? ColorUtilities.getColor(app, ColorUtilities.getStatusBarActiveColorId(nightMode))
				: ColorUtilities.getStatusBarColor(app, nightMode));
	}

	public void hideKeyboard() {
		if (searchEditText.hasFocus()) {
			AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
		}
	}

	public void showOnMap(FavouritePoint point) {
		settings.FAVORITES_TAB.set(MyPlacesActivity.FAV_TAB);

		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(),
				new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName()),
				true,
				point);

		Bundle bundle = new Bundle();
		bundle.putString(FAVORITE_SEARCH_QUERY_KEY, searchQuery);
		bundle.putString(FAVORITE_SEARCH_GROUP_KEY, groupKey);
		MapActivity.launchMapActivityMoveToTop(requireActivity(), bundle, null, null);
	}

	private void selectItem(Object object) {
		if (object instanceof FavouritePoint favouritePoint && selectionMode) {
			selectionHelper.onItemsSelected(Collections.singleton(favouritePoint), !selectionHelper.isItemSelected(favouritePoint));
			updateToolbar();
			adapter.selectItem(object);
		}
	}

	private void updateToolbar() {
		AndroidUiHelper.setVisibility(selectionMode ? View.VISIBLE : View.GONE, selectButton, actionButton, selectedCountTv);
		AndroidUiHelper.setVisibility(!selectionMode ? View.VISIBLE : View.GONE, searchContainer);
		backButton.setImageDrawable(AppCompatResources.getDrawable(app, selectionMode ? R.drawable.ic_action_close : R.drawable.ic_arrow_back));
		if (selectionMode) {
			boolean allTracksSelected = selectionHelper.isAllItemsSelected();

			int iconId = allTracksSelected ? R.drawable.ic_action_deselect_all : R.drawable.ic_action_select_all;
			selectButton.setImageDrawable(getIcon(iconId));
			selectButton.setContentDescription(getString(allTracksSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all));

			String count = String.valueOf(selectionHelper.getSelectedItems().size());
			selectedCountTv.setText(count);
		}
	}

	public void updateContent() {
		List<Object> items = getAdapterItems();
		FavoriteListSortMode sortMode = getTracksSortMode();
		sortItems(items, sortMode);

		adapter.setSortMode(sortMode);
		adapter.setItems(items);
	}

	private void sortItems(@NonNull List<Object> items, @NonNull FavoriteListSortMode sortMode) {
		LatLon latLon = app.getMapViewTrackingUtilities().getDefaultLocation();
		items.sort(new FavoriteComparator(sortMode, latLon, app));
	}

	private List<Object> getAdapterItems() {
		List<Object> items = new ArrayList<>();
		items.add(TYPE_SORT_FAVORITE);

		List<FavouritePoint> list = new ArrayList<>();

		if (Algorithms.isEmpty(filter)) {
			if (Algorithms.isEmpty(searchQuery)) {
				list.addAll(points);
				selectionHelper.setAllItems(points);
			} else {
				getFilter().filter(searchQuery);
			}
		} else {
			for (Object object : filter) {
				if (object instanceof FavouritePoint point) {
					list.add(point);
				}
			}
		}

		if (list.isEmpty()) {
			items.add(TYPE_EMPTY_SEARCH);
		} else {
			selectionHelper.setAllItems(list);
			items.addAll(list);
		}
		return items;
	}

	@NonNull
	public Filter getFilter() {
		if (myFilter == null) {
			myFilter = new FavoritesFilter();
		}
		return myFilter;
	}

	protected void setupToolbar(@NonNull View view) {
		appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);
		appbar.setBackgroundColor(ColorUtilities.getAppBarColor(app, nightMode));
		setStatusBarBackgroundColor(ColorUtilities.getStatusBarColor(app, nightMode));

		selectedCountTv = view.findViewById(R.id.selected_count);
		selectedCountTv.setTextColor(ContextCompat.getColor(app, R.color.card_and_list_background_light));

		searchContainer = view.findViewById(R.id.search_container);
		selectButton = view.findViewById(R.id.select_all_button);
		actionButton = view.findViewById(R.id.action_button);

		selectButton.setOnClickListener(v -> {
			if (selectionMode) {
				if (selectionHelper.isAllItemsSelected()) {
					selectionHelper.clearSelectedItems();
				} else {
					selectionHelper.selectAllItems();
				}
				adapter.updateSelectionAllItems();
				updateToolbar();
			}
		});

		backButton = view.findViewById(R.id.back_button);
		backButton.setVisibility(View.VISIBLE);
		backButton.setOnClickListener((v) -> dismiss());

		View bottomButtons = view.findViewById(R.id.buttons_container);
		AndroidUiHelper.updateVisibility(bottomButtons, false);
	}

	@Override
	public void dismiss() {
		if (selectionMode) {
			setSelectionMode(false);
		} else {
			super.dismiss();
		}
	}

	protected void setStatusBarBackgroundColor(@ColorInt int color) {
		Window window = requireDialog().getWindow();
		if (window != null) {
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), true);
			AndroidUiHelper.setStatusBarColor(window, color);
		}
	}

	protected void setupSearch(@NonNull View view) {
		View searchContainer = view.findViewById(R.id.search_container);
		clearSearchQuery = searchContainer.findViewById(R.id.clearButton);
		clearSearchQuery.setVisibility(View.GONE);
		searchEditText = searchContainer.findViewById(R.id.searchEditText);
		searchEditText.setHint(R.string.poi_filter_by_name);
		searchEditText.setTextColor(ContextCompat.getColor(app, R.color.card_and_list_background_light));
		searchEditText.setHintTextColor(ContextCompat.getColor(app, R.color.white_50_transparent));
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable query) {
				String newQueryText = query.toString();
				if (!searchQuery.equalsIgnoreCase(newQueryText)) {
					searchQuery = newQueryText;
					getFilter().filter(newQueryText);
				}
				AndroidUiHelper.updateVisibility(clearSearchQuery, query.length() > 0);
			}
		});
		clearSearchQuery.setOnClickListener((v) -> {
			if (!searchEditText.getText().toString().isEmpty()) {
				searchEditText.setText("");
				searchEditText.setSelection(0);
			}
		});
	}


	@NonNull
	protected MyPlacesActivity requireMyPlacesActivity() {
		return (MyPlacesActivity) requireActivity();
	}

	@Override
	public void showSortByDialog() {
		FragmentManager manager = getParentFragmentManager();
		FavoriteSortByBottomSheet.showInstance(manager, getTracksSortMode(), this, isUsedOnMap(), false, true);
	}

	@Override
	public void showFiltersDialog() {
		SortFavoriteListener.super.showFiltersDialog();
	}

	@NonNull
	@Override
	public FavoriteListSortMode getTracksSortMode() {
		return settings.SEARCH_FAVORITE_SORT_MODE.get();
	}

	@Override
	public void setTracksSortMode(@NonNull FavoriteListSortMode sortMode, boolean sortSubFolders) {
		if (sortSubFolders) {
			//sortSubFolder(sortMode);
		} else {
			settings.SEARCH_FAVORITE_SORT_MODE.set(sortMode);
			updateContent();
		}
	}

	@Override
	public Bundle storeState() {
		return null;
	}

	@Override
	public void restoreState(Bundle bundle) {

	}

	@Override
	public void onActionFinish() {
		updateContent();
	}

	private class FavoritesFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (Algorithms.isEmpty(constraint)) {
				results.values = null;
				results.count = 1;
			} else {
				Set<Object> filter = new HashSet<>();
				String query = constraint.toString().toLowerCase().trim();
				NameStringMatcher matcher = new NameStringMatcher(query, CHECK_CONTAINS);

				for (FavouritePoint point : points) {
					if (matcher.matches(point.getName()) || matcher.matches(point.getDisplayName(app))) {
						filter.add(point);
					}
				}

				results.values = filter;
				results.count = filter.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			synchronized (adapter) {
				filter = (Set<?>) results.values;
				updateContent();
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

		updateContent();
	}

	@Override
	public void onSavingFavoritesFinished() {
		updateContent();
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target, List<FavouritePoint> points, String groupKey, String searchQuery) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SearchFavoriteFragment fragment = new SearchFavoriteFragment();
			Bundle bundle = new Bundle();
			if (!Algorithms.isEmpty(searchQuery)) {
				bundle.putString(FAVORITE_SEARCH_QUERY_KEY, searchQuery);
			}
			if (groupKey != null) {
				bundle.putString(FAVORITE_SEARCH_GROUP_KEY, groupKey);
			}
			fragment.setArguments(bundle);
			fragment.points = points;
			fragment.setRetainInstance(true);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}
