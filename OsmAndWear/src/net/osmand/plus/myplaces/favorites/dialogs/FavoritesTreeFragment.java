package net.osmand.plus.myplaces.favorites.dialogs;

import static android.view.Gravity.CENTER;
import static net.osmand.data.PointDescription.POINT_TYPE_MAP_MARKER;
import static net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import static net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import static net.osmand.plus.myplaces.MyPlacesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.Location;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.MyPlacesActivity;
import net.osmand.plus.myplaces.favorites.DeleteFavoritesTask;
import net.osmand.plus.myplaces.favorites.DeleteFavoritesTask.DeleteFavoritesListener;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask;
import net.osmand.plus.myplaces.favorites.ShareFavoritesAsyncTask.ShareFavoritesListener;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.MapUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FavoritesTreeFragment extends OsmandExpandableListFragment implements FragmentStateHolder,
		OsmAndCompassListener, OsmAndLocationListener, ShareFavoritesListener {

	private static final int IMPORT_FAVOURITES_REQUEST = 1007;
	private static final int SEARCH_ID = -1;
	private static final int DELETE_ID = 2;
	private static final int DELETE_ACTION_ID = 3;
	private static final int SHARE_ID = 4;
	private static final int SELECT_MAP_MARKERS_ID = 5;
	private static final int SELECT_MAP_MARKERS_ACTION_MODE_ID = 6;
	private static final int IMPORT_FAVOURITES_ID = 7;
	private static final String GROUP_EXPANDED_POSTFIX = "_group_expanded";

	private FavouritesHelper helper;
	private ImportHelper importHelper;
	private FavouritesAdapter adapter;
	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private boolean selectionMode;
	private final LinkedHashMap<String, Set<FavouritePoint>> favoritesSelected = new LinkedHashMap<>();
	private final Set<FavoriteGroup> groupsToDelete = new LinkedHashSet<>();
	private ActionMode actionMode;
	private Drawable arrowImageDisabled;
	private final HashMap<String, OsmandPreference<Boolean>> preferenceCache = new HashMap<>();
	private View footerView;
	private Location lastLocation;
	private float lastHeading;

	private int selectedGroupPos = -1;
	private int selectedChildPos = -1;

	private FavoritesListener favoritesListener;

	private String groupNameToShow;
	private boolean compassUpdateAllowed = true;
	private boolean locationUpdateStarted;

	private View freeFavoritesBackupCard;
	private View freeFavoritesBackupCardDivider;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new FavouritesAdapter(requireActivity());
		importHelper = app.getImportHelper();

		helper = app.getFavoritesHelper();
		if (helper.isFavoritesLoaded()) {
			adapter.synchronizeGroups();
		} else {
			helper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					adapter.synchronizeGroups();
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint point) {
					adapter.notifyDataSetChanged();
				}
			});
		}
		setAdapter(adapter);

		arrowImageDisabled = AppCompatResources.getDrawable(app, R.drawable.ic_direction_arrow);
		arrowImageDisabled.mutate();
		arrowImageDisabled.setColorFilter(ColorUtilities.getDefaultIconColor(app, nightMode), PorterDuff.Mode.MULTIPLY);
	}

	private void deleteFavorites() {
		DeleteFavoritesTask task = new DeleteFavoritesTask(helper, groupsToDelete, getSelectedFavorites(), new DeleteFavoritesListener() {
			@Override
			public void onDeletingStarted() {
				updateProgressVisibility(true);
			}

			@Override
			public void onDeletingFinished() {
				groupsToDelete.clear();
				favoritesSelected.clear();

				updateProgressVisibility(false);
				adapter.synchronizeGroups();
			}
		});
		task.executeOnExecutor(singleThreadExecutor);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = inflater.inflate(R.layout.favorites_tree, container, false);
		listView = view.findViewById(android.R.id.list);
		adapter.synchronizeGroups();
		if (!adapter.isEmpty()) {
			boolean nightMode = !app.getSettings().isLightContent();
			View searchView = inflater.inflate(R.layout.search_fav_list_item, listView, false);
			searchView.setBackgroundResource(ColorUtilities.getListBgColorId(nightMode));

			TextView title = searchView.findViewById(R.id.title);
			Drawable searchIcon = getContentIcon(R.drawable.ic_action_search_dark);
			AndroidUtils.setCompoundDrawablesWithIntrinsicBounds(title, searchIcon, null, null, null);
			searchView.setOnClickListener(v -> FavoritesSearchFragment.showInstance(requireActivity(), ""));
			listView.addHeaderView(searchView);
			View dividerView = inflater.inflate(R.layout.list_item_divider, null, false);
			boolean available = InAppPurchaseUtils.isBackupAvailable(app);
			boolean isRegistered = app.getBackupHelper().isRegistered();
			if (!available && !isRegistered && !app.getSettings().FAVORITES_FREE_ACCOUNT_CARD_DISMISSED.get()) {
				freeFavoritesBackupCardDivider = inflater.inflate(R.layout.list_item_divider, listView, false);
				listView.addHeaderView(freeFavoritesBackupCardDivider, null, false);
				freeFavoritesBackupCard = inflater.inflate(R.layout.free_backup_card, listView, false);
				setupGetOsmAndCloudButton(freeFavoritesBackupCard);
				listView.addHeaderView(freeFavoritesBackupCard);
			}
			listView.addHeaderView(dividerView, null, false);
			footerView = inflater.inflate(R.layout.list_shadow_footer, null, false);
			listView.addFooterView(footerView);
		}
		View emptyView = view.findViewById(android.R.id.empty);
		ImageView emptyImageView = emptyView.findViewById(R.id.empty_state_image_view);
		emptyImageView.setImageResource(app.getSettings().isLightContent() ? R.drawable.ic_empty_state_favorites_day : R.drawable.ic_empty_state_favorites_night);
		Button importButton = emptyView.findViewById(R.id.import_button);
		importButton.setOnClickListener(view1 -> importFavourites());
		listView.setEmptyView(emptyView);
		listView.setAdapter(adapter);
		setListView(listView);
		setHasOptionsMenu(true);
		listView.setOnGroupCollapseListener(groupPosition -> {
			String groupName = adapter.getGroup(groupPosition).getName();
			getGroupExpandedPreference(groupName).set(false);
		});
		listView.setOnGroupExpandListener(groupPosition -> {
			String groupName = adapter.getGroup(groupPosition).getName();
			getGroupExpandedPreference(groupName).set(true);
		});
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int newState) {
				compassUpdateAllowed = newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
		if (getArguments() != null) {
			groupNameToShow = getArguments().getString(GROUP_NAME_TO_SHOW);
		}
		if (groupNameToShow != null) {
			int groupPos = adapter.getGroupPosition(groupNameToShow);
			if (groupPos != -1) {
				listView.expandGroup(groupPos);
				int selection = listView.getHeaderViewsCount();
				for (int i = 0; i < groupPos; i++) {
					selection++; // because of group header
					if (getGroupExpandedPreference(adapter.getGroup(i).getName()).get()) {
						selection += adapter.getChildrenCount(i);
					}
				}
				listView.setSelection(selection);
			}
		}
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(lastLocation, location)) {
			lastLocation = location;
			updateLocationUi();
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
	public void onResume() {
		super.onResume();
		adapter.synchronizeGroups();
		initListExpandedState();
		if (groupNameToShow == null) {
			restoreState(getArguments());
		}

		startLocationUpdate();
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

	private void updateLocationUi() {
		if (!compassUpdateAllowed) {
			return;
		}
		if (app != null && adapter != null) {
			app.runInUIThread(adapter::notifyDataSetChanged);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
		if (actionMode != null) {
			actionMode.finish();
		}
		if (favoritesListener != null) {
			helper.removeListener(favoritesListener);
			favoritesListener = null;
		}

	}

	private int getSelectedFavoritesCount() {
		int count = 0;
		for (Set<FavouritePoint> set : favoritesSelected.values()) {
			if (set != null) {
				count += set.size();
			}
		}
		return count;
	}

	@NonNull
	private Set<FavouritePoint> getSelectedFavorites() {
		Set<FavouritePoint> result = new LinkedHashSet<>();
		for (Set<FavouritePoint> set : favoritesSelected.values()) {
			if (set != null) {
				result.addAll(set);
			}
		}
		return result;
	}

	public void reloadData() {
		adapter.synchronizeGroups();
		adapter.notifyDataSetInvalidated();
	}

	private void updateSelectionMode(@NonNull ActionMode mode) {
		int size = getSelectedFavoritesCount();
		if (size > 0) {
			mode.setTitle(size + " " + getString(R.string.shared_string_selected_lowercase));
		} else {
			mode.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		FavouritePoint point = adapter.getChild(groupPosition, childPosition);
		if (point == null) {
			return true;
		}
		if (selectionMode) {
			FavoriteGroup group = adapter.getGroup(groupPosition);

			CheckBox checkBox = v.findViewById(R.id.toggle_item);
			checkBox.setChecked(!checkBox.isChecked());
			if (checkBox.isChecked()) {
				Set<FavouritePoint> set = favoritesSelected.get(group.getName());
				if (set != null) {
					set.add(point);
				} else {
					set = new LinkedHashSet<>();
					set.add(point);
					favoritesSelected.put(group.getName(), set);
				}
			} else {
				Set<FavouritePoint> set = favoritesSelected.get(group.getName());
				if (set != null) {
					set.remove(point);
				}
			}
			updateSelectionMode(actionMode);
		} else {
			showOnMap(point, groupPosition, childPosition);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SELECT_MAP_MARKERS_ID) {
			selectMapMarkers();
			return true;
		} else if (item.getItemId() == SHARE_ID) {
			shareFavourites();
			return true;
		} else if (item.getItemId() == DELETE_ID) {
			enterDeleteMode();
			return true;
		} else if (item.getItemId() == DELETE_ACTION_ID) {
			deleteFavoritesAction();
			return true;
		} else if (item.getItemId() == IMPORT_FAVOURITES_ID) {
			importFavourites();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void selectMapMarkers() {
		enterMapMarkersMode();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark, MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mi.setOnMenuItemClickListener(item -> {
			FavoritesSearchFragment.showInstance(getActivity(), "");
			return true;
		});

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((MyPlacesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((MyPlacesActivity) getActivity()).getClearToolbar(false);
		}
		((MyPlacesActivity) getActivity()).updateListViewFooter(footerView);


		if (!mi.isActionViewExpanded()) {
			createMenuItem(menu, IMPORT_FAVOURITES_ID, R.string.shared_string_add_to_favorites, R.drawable.ic_action_plus, MenuItem.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, SHARE_ID, R.string.shared_string_share, R.drawable.ic_action_gshare_dark, MenuItem.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, SELECT_MAP_MARKERS_ID, R.string.select_map_markers, R.drawable.ic_action_flag, MenuItem.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, DELETE_ID, R.string.shared_string_delete, R.drawable.ic_action_delete_dark, MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}

	private void updateProgressVisibility(boolean visible) {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	private void enterMapMarkersMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, SELECT_MAP_MARKERS_ACTION_MODE_ID, R.string.select_map_markers,
						R.drawable.ic_action_flag,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				favoritesSelected.clear();
				groupsToDelete.clear();
				adapter.notifyDataSetInvalidated();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				enableSelectionMode(false);
				adapter.notifyDataSetInvalidated();
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == SELECT_MAP_MARKERS_ACTION_MODE_ID) {
					mode.finish();
					selectMapMarkersImpl();
				}
				return true;
			}
		});

	}

	private void selectMapMarkersImpl() {
		if (getSelectedFavoritesCount() > 0) {
			MapMarkersHelper markersHelper = app.getMapMarkersHelper();
			List<LatLon> points = new ArrayList<>();
			List<PointDescription> names = new ArrayList<>();
			for (Map.Entry<String, Set<FavouritePoint>> entry : favoritesSelected.entrySet()) {
				FavoriteGroup group = helper.getGroup(entry.getKey());
				if (group != null && entry.getValue().size() == group.getPoints().size()) {
					markersHelper.addOrEnableGroup(group);
				} else {
					for (FavouritePoint point : entry.getValue()) {
						points.add(new LatLon(point.getLatitude(), point.getLongitude()));
						names.add(new PointDescription(POINT_TYPE_MAP_MARKER, point.getName()));
					}
					markersHelper.addMapMarkers(points, names, null);
					points.clear();
					names.clear();
				}
			}
			MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	private void enterDeleteMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, DELETE_ACTION_ID, R.string.shared_string_delete,
						R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				favoritesSelected.clear();
				groupsToDelete.clear();
				adapter.notifyDataSetInvalidated();
				updateSelectionMode(mode);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				enableSelectionMode(false);
				adapter.notifyDataSetInvalidated();
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == DELETE_ACTION_ID) {
					mode.finish();
					deleteFavoritesAction();
				}
				return true;
			}
		});
	}

	private void enableSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		((MyPlacesActivity) getActivity()).setToolbarVisibility(!selectionMode &&
				AndroidUiHelper.isOrientationPortrait(getActivity()));
		((MyPlacesActivity) getActivity()).updateListViewFooter(footerView);
	}

	private void deleteFavoritesAction() {
		int size = getSelectedFavoritesCount();
		if (groupsToDelete.size() + size > 0) {

			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(getString(R.string.favorite_delete_multiple, size, groupsToDelete.size()));
			b.setPositiveButton(R.string.shared_string_delete, (dialog, which) -> {
				if (actionMode != null) {
					actionMode.finish();
				}
				deleteFavorites();
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private void shareFavourites() {
		if (adapter.isEmpty()) {
			Toast.makeText(getActivity(), R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
		} else {
			shareFavorites(null);
		}
	}

	private void importFavourites() {
		Intent intent = ImportHelper.getImportFileIntent();
		AndroidUtils.startActivityForResultIfSafe(this, intent, IMPORT_FAVOURITES_REQUEST);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == IMPORT_FAVOURITES_REQUEST && resultCode == Activity.RESULT_OK) {
			if (data != null && data.getData() != null) {
				importHelper.handleFavouritesImport(data.getData());
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void shareFavorites(@Nullable FavoriteGroup group) {
		ShareFavoritesAsyncTask shareFavoritesTask = new ShareFavoritesAsyncTask(app, group, this);
		shareFavoritesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void initListExpandedState() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			String groupName = adapter.getGroup(i).getName();
			if (getGroupExpandedPreference(groupName).get()) {
				listView.expandGroup(i);
			} else {
				listView.collapseGroup(i);
			}
		}
	}

	private OsmandPreference<Boolean> getGroupExpandedPreference(String groupName) {
		OsmandPreference<Boolean> preference = preferenceCache.get(groupName);
		if (preference == null) {
			String groupKey = groupName + GROUP_EXPANDED_POSTFIX;
			preference = settings.registerBooleanPreference(groupKey, false);
			preferenceCache.put(groupKey, preference);
		}
		return preference;
	}

	public void showOnMap(FavouritePoint point, int groupPos, int childPos) {
		settings.FAVORITES_TAB.set(FAV_TAB);
		selectedGroupPos = groupPos;
		selectedChildPos = childPos;
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		String pointType = PointDescription.POINT_TYPE_FAVORITE;
		((MyPlacesActivity) getActivity()).showOnMap(this, location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(), new PointDescription(pointType, point.getDisplayName(app)), true, point);
	}

	@Override
	public Bundle storeState() {
		Bundle bundle = new Bundle();
		bundle.putInt(TAB_ID, FAV_TAB);
		if (selectedGroupPos != -1) {
			bundle.putInt(GROUP_POSITION, selectedGroupPos);
		}
		if (selectedChildPos != -1) {
			bundle.putInt(ITEM_POSITION, selectedChildPos);
		}
		return bundle;
	}

	@Override
	public void restoreState(Bundle bundle) {
		if (bundle != null && bundle.containsKey(TAB_ID) && bundle.containsKey(ITEM_POSITION)) {
			if (bundle.getInt(TAB_ID) == FAV_TAB) {
				selectedGroupPos = bundle.getInt(GROUP_POSITION, -1);
				selectedChildPos = bundle.getInt(ITEM_POSITION, -1);
				if (selectedGroupPos != -1 && selectedChildPos != -1
						&& selectedGroupPos < adapter.getGroupCount()
						&& selectedChildPos < adapter.getChildrenCount(selectedGroupPos)) {
					listView.setSelectedChild(selectedGroupPos, selectedChildPos, true);
				}
			}
		}
	}

	@Override
	public void shareFavoritesStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void shareFavoritesFinished() {
		updateProgressVisibility(false);
	}

	private void setupGetOsmAndCloudButton(@NonNull View view) {
		UiUtilities iconsCache = app.getUIUtilities();
		ImageView closeBtn = view.findViewById(R.id.btn_close);
		closeBtn.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_cancel, isNightMode()));
		closeBtn.setOnClickListener(v -> {
			if (listView != null) {
				if (freeFavoritesBackupCard != null) {
					listView.removeHeaderView(freeFavoritesBackupCard);
				}
				if (freeFavoritesBackupCardDivider != null) {
					listView.removeHeaderView(freeFavoritesBackupCardDivider);
				}
				app.getSettings().FAVORITES_FREE_ACCOUNT_CARD_DISMISSED.set(true);
			}
		});

		view.findViewById(R.id.dismiss_button_container).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				((MyPlacesActivity) getActivity()).showOsmAndCloud(this);
			}
		});
	}

	private class FavouritesAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		private static final boolean SHOW_OPTIONS_BUTTON = false;

		private final UpdateLocationViewCache cache;
		private final List<FavoriteGroup> groups = new ArrayList<>();
		private final Map<FavoriteGroup, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<>();

		private Filter filter;
		private Set<?> filteredValues;

		FavouritesAdapter(@NonNull FragmentActivity activity) {
			cache = UpdateLocationUtils.getUpdateLocationViewCache(activity);
		}

		void synchronizeGroups() {
			favoriteGroups.clear();
			groups.clear();

			Set<?> flt = filteredValues;
			List<FavoriteGroup> disablesGroups = new ArrayList<>();
			List<FavoriteGroup> favoriteGroups = helper.getFavoriteGroups();

			for (FavoriteGroup group : favoriteGroups) {
				boolean empty = true;
				if (flt == null || flt.contains(group)) {
					empty = false;
					this.favoriteGroups.put(group, new ArrayList<>(group.getPoints()));
				} else {
					List<FavouritePoint> list = new ArrayList<>();
					for (FavouritePoint p : group.getPoints()) {
						if (flt.contains(p)) {
							list.add(p);
							empty = false;
						}
					}
					this.favoriteGroups.put(group, list);
				}
				if (!empty) {
					if (group.isVisible()) {
						groups.add(group);
					} else {
						disablesGroups.add(group);
					}
				}
			}
			groups.addAll(disablesGroups);
			notifyDataSetChanged();
		}

		@Override
		public FavouritePoint getChild(int groupPosition, int childPosition) {
			FavoriteGroup group = groups.get(groupPosition);
			List<FavouritePoint> points = favoriteGroups.get(group);
			return points != null ? points.get(childPosition) : null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000L + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			FavoriteGroup group = groups.get(groupPosition);
			List<FavouritePoint> points = favoriteGroups.get(group);
			return points != null ? points.size() : 0;
		}

		@Override
		public FavoriteGroup getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groups.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			boolean checkBox = row != null && row.findViewById(R.id.toggle_item) instanceof CheckBox;
			boolean same = (selectionMode && checkBox) || (!selectionMode && !checkBox);
			if (row == null || !same) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.expandable_list_item_category, parent, false);
				fixBackgroundRepeat(row);
			}
			FavoriteGroup model = getGroup(groupPosition);
			boolean visible = model.isVisible();
			int enabledColor = ColorUtilities.getPrimaryTextColorId(nightMode);
			int disabledColor = ColorUtilities.getSecondaryTextColorId(nightMode);
			row.findViewById(R.id.group_divider).setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
			int color = model.getColor() == 0 ? getColor(R.color.color_favorite) : model.getColor();
			if (!model.isPersonal()) {
				setCategoryIcon(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_folder,
						visible ? color : getColor(disabledColor)), row);
			}
			adjustIndicator(app, groupPosition, isExpanded, row, !nightMode);
			TextView label = row.findViewById(R.id.category_name);
			label.setTextColor(getColor(visible ? enabledColor : disabledColor));
			if (visible) {
				label.setTypeface(FontCache.getMediumFont());
			} else {
				label.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
			}
			label.setText(model.getName().length() == 0 ? getString(R.string.shared_string_favorites) : model.getDisplayName(app));

			CheckBox toggleItem = row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				toggleItem.setVisibility(View.VISIBLE);
				toggleItem.setChecked(groupsToDelete.contains(model));

				toggleItem.setOnClickListener(v -> {
					List<FavouritePoint> fvs = model.getPoints();
					if (toggleItem.isChecked()) {
						groupsToDelete.add(model);
						if (fvs != null) {
							Set<FavouritePoint> set = favoritesSelected.get(model.getName());
							if (set != null) {
								set.addAll(model.getPoints());
							} else {
								set = new LinkedHashSet<>(model.getPoints());
								favoritesSelected.put(model.getName(), set);
							}
						}
					} else {
						groupsToDelete.remove(model);
						favoritesSelected.remove(model.getName());
					}
					adapter.notifyDataSetInvalidated();
					updateSelectionMode(actionMode);
				});
				row.findViewById(R.id.category_icon).setVisibility(View.GONE);
			} else {
				toggleItem.setVisibility(View.GONE);
				row.findViewById(R.id.category_icon).setVisibility(View.VISIBLE);
			}
			View options = row.findViewById(R.id.options);
			if (!selectionMode) {
				if (!model.isPersonal()) {
					((ImageView) options).setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));
					options.setVisibility(View.VISIBLE);
					options.setContentDescription(getString(R.string.shared_string_settings));
					options.setOnClickListener(v -> EditFavoriteGroupDialogFragment.showInstance(getChildFragmentManager(), model.getName()));
				}
			} else {
				options.setVisibility(View.GONE);
			}
			return row;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
		                         ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.favorites_list_item, parent, false);
				row.findViewById(R.id.list_divider).setVisibility(View.VISIBLE);
			}
			int enabledColor = ColorUtilities.getPrimaryTextColorId(nightMode);
			int disabledColor = ColorUtilities.getSecondaryTextColorId(nightMode);
			int disabledIconColor = ColorUtilities.getDefaultIconColorId(nightMode);

			TextView name = row.findViewById(R.id.favourite_label);
			TextView distanceText = row.findViewById(R.id.distance);
			TextView addressText = row.findViewById(R.id.group_name);
			ImageView icon = row.findViewById(R.id.favourite_icon);

			FavouritePoint model = getChild(groupPosition, childPosition);
			if (model == null) {
				return row;
			}
			FavoriteGroup group = getGroup(groupPosition);
			boolean visible = model.isVisible();
			row.setTag(model);

			if (SHOW_OPTIONS_BUTTON) {
				ImageView options = row.findViewById(R.id.options);
				options.setFocusable(false);
				options.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));
				options.setVisibility(View.VISIBLE);
				options.setOnClickListener(v -> showOnMap(model, groupPosition, childPosition));
			}
			name.setText(model.getDisplayName(app), TextView.BufferType.SPANNABLE);
			name.setTypeface(Typeface.DEFAULT, visible ? Typeface.NORMAL : Typeface.ITALIC);
			name.setTextColor(getColor(visible ? enabledColor : disabledColor));
			addressText.setText(model.isAddressSpecified() ? model.getAddress() : null);
			int color = visible
					? app.getFavoritesHelper().getColorWithCategory(model, getColor(R.color.color_favorite))
					: ContextCompat.getColor(app, disabledIconColor);
			icon.setImageDrawable(PointImageUtils.getFromPoint(getActivity(), color, false, model));
			int iconSize = (int) getResources().getDimension(R.dimen.favorites_my_places_icon_size);
			FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(iconSize, iconSize, CENTER);
			icon.setLayoutParams(lp);
			row.findViewById(R.id.group_image).setVisibility(View.GONE);

			ImageView direction = row.findViewById(R.id.direction);
			UpdateLocationUtils.updateLocationView(app, cache, direction, distanceText, model.getLatitude(), model.getLongitude());
			if (model.isAddressSpecified()) {
				String addComma = app.getString(R.string.ltr_or_rtl_combine_via_comma);
				distanceText.setText(String.format(addComma, distanceText.getText(), ""));
			}
			direction.setVisibility(View.VISIBLE);
			if (!visible) {
				distanceText.setTextColor(getColor(disabledColor));
				direction.setImageDrawable(arrowImageDisabled);
			}

			CheckBox ch = row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(favoritesSelected.get(group.getName()) != null && favoritesSelected.get(group.getName()).contains(model));
				row.findViewById(R.id.favourite_icon).setVisibility(View.GONE);
				ch.setOnClickListener(v -> {
					if (ch.isChecked()) {
						Set<FavouritePoint> set = favoritesSelected.get(group.getName());
						if (set != null) {
							set.add(model);
						} else {
							set = new LinkedHashSet<>();
							set.add(model);
							favoritesSelected.put(group.getName(), set);
						}
					} else {
						Set<FavouritePoint> set = favoritesSelected.get(group.getName());
						if (set != null) {
							groupsToDelete.remove(group);
							getGroupPosition(group.getName());
							set.remove(model);
							adapter.notifyDataSetInvalidated();
						}
					}
					updateSelectionMode(actionMode);
				});
			} else {
				row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
			}
			return row;
		}

		@Override
		public Filter getFilter() {
			if (filter == null) {
				filter = new FavoritesFilter();
			}
			return filter;
		}

		public void setFilterResults(Set<?> values) {
			this.filteredValues = values;
		}

		public int getGroupPosition(String groupName) {
			for (int i = 0; i < getGroupCount(); i++) {
				FavoriteGroup group = getGroup(i);
				if (group.getName().equals(groupName)) {
					return i;
				}
			}
			return -1;
		}

		private void fixBackgroundRepeat(@NonNull View view) {
			Drawable drawable = view.getBackground();
			if (drawable != null) {
				if (drawable instanceof BitmapDrawable) {
					BitmapDrawable bmp = (BitmapDrawable) drawable;
					// bmp.mutate(); // make sure that we aren't sharing state anymore
					bmp.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
				}
			}
		}
	}

	private class FavoritesFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = null;
				results.count = 1;
			} else {
				Set<Object> filter = new HashSet<>();
				String cs = constraint.toString().toLowerCase();
				for (FavoriteGroup g : helper.getFavoriteGroups()) {
					if (g.getName().toLowerCase().contains(cs)) {
						filter.add(g);
					} else {
						for (FavouritePoint fp : g.getPoints()) {
							if (fp.getName().toLowerCase().contains(cs)) {
								filter.add(fp);
							}
						}
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
				adapter.setFilterResults((Set<?>) results.values);
				adapter.synchronizeGroups();
			}
			adapter.notifyDataSetChanged();
			if (constraint != null && constraint.length() > 1) {
				initListExpandedState();
			}
		}
	}
}
