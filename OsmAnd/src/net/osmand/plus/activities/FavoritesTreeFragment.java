package net.osmand.plus.activities;

import static net.osmand.plus.myplaces.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.FavouritesDbHelper.FavoritesListener;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.myplaces.FavoritesFragmentStateHolder;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FavoritesTreeFragment extends OsmandExpandableListFragment implements
	FavoritesFragmentStateHolder {
	public static final int SEARCH_ID = -1;
	//	public static final int EXPORT_ID = 0;
	// public static final int IMPORT_ID = 1;
	public static final int DELETE_ID = 2;
	public static final int DELETE_ACTION_ID = 3;
	public static final int SHARE_ID = 4;
	public static final int SELECT_MAP_MARKERS_ID = 5;
	public static final int SELECT_MAP_MARKERS_ACTION_MODE_ID = 6;
	public static final int IMPORT_FAVOURITES_ID = 7;
	public static final String GROUP_EXPANDED_POSTFIX = "_group_expanded";

	private FavouritesAdapter favouritesAdapter = new FavouritesAdapter();
	private FavouritesDbHelper helper;

	private OsmandApplication app;
	private boolean selectionMode = false;
	private LinkedHashMap<String, Set<FavouritePoint>> favoritesSelected = new LinkedHashMap<>();
	private Set<FavoriteGroup> groupsToDelete = new LinkedHashSet<>();
	private ActionMode actionMode;
	Drawable arrowImage;
	Drawable arrowImageDisabled;
	private HashMap<String, OsmandSettings.OsmandPreference<Boolean>> preferenceCache = new HashMap<>();
	private View footerView;

	private int selectedGroupPos = -1;
	private int selectedChildPos = -1;

	private FavoritesListener favoritesListener;
	
	String groupNameToShow = null;
	
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.app = (OsmandApplication) getActivity().getApplication();

		helper = getMyApplication().getFavorites();
		if (helper.isFavoritesLoaded()) {
			favouritesAdapter.synchronizeGroups();
		} else {
			helper.addListener(favoritesListener = new FavoritesListener() {
				@Override
				public void onFavoritesLoaded() {
					favouritesAdapter.synchronizeGroups();
				}

				@Override
				public void onFavoriteDataUpdated(@NonNull FavouritePoint favouritePoint) {
				}
			});
		}
		setAdapter(favouritesAdapter);

		boolean light = getMyApplication().getSettings().isLightContent();
		arrowImage = ContextCompat.getDrawable(context, R.drawable.ic_direction_arrow);
		arrowImage.mutate();
		if (light) {
			arrowImage.setColorFilter(ContextCompat.getColor(context, R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		} else {
			arrowImage.setColorFilter(ContextCompat.getColor(context, R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		}
		arrowImageDisabled = ContextCompat.getDrawable(context, R.drawable.ic_direction_arrow);
		arrowImageDisabled.mutate();
		arrowImageDisabled.setColorFilter(ContextCompat.getColor(
				context, light ? R.color.icon_color_default_light : R.color.icon_color_default_dark), PorterDuff.Mode.MULTIPLY);
	}

	private void deleteFavorites() {
		new AsyncTask<Void, Object, Void>() {

			@Override
			protected void onPreExecute() {
				showProgressBar();
			}

			@Override
			protected void onPostExecute(Void result) {
				hideProgressBar();
				favouritesAdapter.synchronizeGroups();
			}

			@Override
			protected Void doInBackground(Void... params) {
				helper.delete(groupsToDelete, getSelectedFavorites());
				favoritesSelected.clear();
				groupsToDelete.clear();
				return null;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.favorites_tree, container, false);
		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		favouritesAdapter.synchronizeGroups();
		if (!favouritesAdapter.isEmpty()) {
			boolean light = getMyApplication().getSettings().isLightContent();
			View searchView = inflater.inflate(R.layout.search_fav_list_item, null);
			searchView.setBackgroundResource(light ? R.color.list_background_color_light : R.color.list_background_color_dark);
			TextView title = (TextView) searchView.findViewById(R.id.title);
			title.setCompoundDrawablesWithIntrinsicBounds(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_search_dark), null, null, null);
			title.setHint(R.string.shared_string_search);
			searchView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FavoritesSearchFragment.showInstance(getActivity(), "");
				}
			});
			listView.addHeaderView(searchView);
			listView.addHeaderView(inflater.inflate(R.layout.list_item_divider, null, false));
			footerView = inflater.inflate(R.layout.list_shadow_footer, null, false);
			listView.addFooterView(footerView);
		}
		View emptyView = view.findViewById(android.R.id.empty);
		ImageView emptyImageView = (ImageView) emptyView.findViewById(R.id.empty_state_image_view);
		if (Build.VERSION.SDK_INT >= 18) {
			emptyImageView.setImageResource(app.getSettings().isLightContent() ? R.drawable.ic_empty_state_favorites_day : R.drawable.ic_empty_state_favorites_night);
		} else {
			emptyImageView.setVisibility(View.INVISIBLE);
		}
		Button importButton = (Button) emptyView.findViewById(R.id.import_button);
		importButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				importFavourites();
			}
		});
		listView.setEmptyView(emptyView);
		listView.setAdapter(favouritesAdapter);
		setListView(listView);
		setHasOptionsMenu(true);
		listView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
			@Override
			public void onGroupCollapse(int groupPosition) {
				String groupName = favouritesAdapter.getGroup(groupPosition).getName();
				getGroupExpandedPreference(groupName).set(false);
			}
		});
		listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				String groupName = favouritesAdapter.getGroup(groupPosition).getName();
				getGroupExpandedPreference(groupName).set(true);
			}
		});
		
		if (getArguments() != null) {
			groupNameToShow = getArguments().getString(GROUP_NAME_TO_SHOW);
		}
		
		if (groupNameToShow != null) {
			int groupPos = favouritesAdapter.getGroupPosition(groupNameToShow);
			if (groupPos != -1) {
				listView.expandGroup(groupPos);
				int selection = listView.getHeaderViewsCount();
				for (int i = 0; i < groupPos; i++) {
					selection++; // because of group header
					if (getGroupExpandedPreference(favouritesAdapter.getGroup(i).getName()).get()) {
						selection += favouritesAdapter.getChildrenCount(i);
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
		listView.setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.activity_background_color_light
						: R.color.activity_background_color_dark));
	}

	@Override
	public void onResume() {
		super.onResume();
		favouritesAdapter.synchronizeGroups();
		initListExpandedState();
		if (groupNameToShow == null) {
			restoreState(getArguments());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
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
		favouritesAdapter.synchronizeGroups();
		favouritesAdapter.notifyDataSetInvalidated();
	}

	private void updateSelectionMode(ActionMode m) {
		int size = getSelectedFavoritesCount();
		if (size > 0) {
			m.setTitle(size + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (selectionMode) {
			CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
			FavouritePoint model = favouritesAdapter.getChild(groupPosition, childPosition);
			FavoriteGroup group = favouritesAdapter.getGroup(groupPosition);
			ch.setChecked(!ch.isChecked());
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
					set.remove(model);
				}
			}
			updateSelectionMode(actionMode);
		} else {
			final FavouritePoint point = favouritesAdapter.getChild(groupPosition, childPosition);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				FavoritesSearchFragment.showInstance(getActivity(), "");
				return true;
			}
		});

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((FavoritesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((FavoritesActivity) getActivity()).getClearToolbar(false);
		}
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);


		if (!MenuItemCompat.isActionViewExpanded(mi)) {
			createMenuItem(menu, IMPORT_FAVOURITES_ID, R.string.shared_string_add_to_favorites, R.drawable.ic_action_plus, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, SHARE_ID, R.string.shared_string_share, R.drawable.ic_action_gshare_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, SELECT_MAP_MARKERS_ID, R.string.select_map_markers, R.drawable.ic_action_flag_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, DELETE_ID, R.string.shared_string_delete, R.drawable.ic_action_delete_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}


	public void showProgressBar() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	public void hideProgressBar() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	private void enterMapMarkersMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, SELECT_MAP_MARKERS_ACTION_MODE_ID, R.string.select_map_markers,
						R.drawable.ic_action_flag_dark,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				favoritesSelected.clear();
				groupsToDelete.clear();
				favouritesAdapter.notifyDataSetInvalidated();
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
				favouritesAdapter.notifyDataSetInvalidated();
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
			MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();
			List<LatLon> points = new ArrayList<>();
			List<PointDescription> names = new ArrayList<>();
			for (Map.Entry<String, Set<FavouritePoint>> entry : favoritesSelected.entrySet()) {
				FavoriteGroup favGr = helper.getGroup(entry.getKey());
				if (entry.getValue().size() == favGr.getPoints().size()) {
					markersHelper.addOrEnableGroup(favGr);
				} else {
					for (FavouritePoint fp : entry.getValue()) {
						points.add(new LatLon(fp.getLatitude(), fp.getLongitude()));
						names.add(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, fp.getName()));
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
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				favoritesSelected.clear();
				groupsToDelete.clear();
				favouritesAdapter.notifyDataSetInvalidated();
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
				favouritesAdapter.notifyDataSetInvalidated();
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
		((FavoritesActivity) getActivity()).setToolbarVisibility(!selectionMode &&
				AndroidUiHelper.isOrientationPortrait(getActivity()));
		((FavoritesActivity) getActivity()).updateListViewFooter(footerView);
	}

	private void deleteFavoritesAction() {
		int size = getSelectedFavoritesCount();
		if (groupsToDelete.size() + size > 0) {

			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(getString(R.string.favorite_delete_multiple, size, groupsToDelete.size()));
			b.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (actionMode != null) {
						actionMode.finish();
					}
					deleteFavorites();
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private StringBuilder generateHtmlPrint(List<FavoriteGroup> groups) {
		StringBuilder html = new StringBuilder();
		html.append("<h1>My Favorites</h1>");
		for (FavoriteGroup group : groups) {
			html.append("<h3>" + group.getName() + "</h3>");
			for (FavouritePoint fp : group.getPoints()) {
				String url = "geo:" + ((float) fp.getLatitude()) + "," + ((float) fp.getLongitude()) + "?m=" + fp.getName();
				html.append("<p>" + fp.getName() + " - " + "<a href=\"" + url + "\">geo:"
						+ ((float) fp.getLatitude()) + "," + ((float) fp.getLongitude()) + "</a><br>");

				if (!Algorithms.isEmpty(fp.getDescription())) {
					html.append(": " + fp.getDescription());
				}
				html.append("</p>");
			}
		}
		return html;
	}


	private void shareFavourites() {
		if (favouritesAdapter.isEmpty()) {
			Toast.makeText(getActivity(), R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
		} else {
			shareFavorites(null);
		}
	}

	private void importFavourites() {
		((FavoritesActivity) getActivity()).importFavourites();
	}

	public void shareFavorites(final FavoriteGroup group) {
		final AsyncTask<Void, Void, Void> exportTask = new AsyncTask<Void, Void, Void>() {

			File src = null;
			File dst = null;

			@Override
			protected void onPreExecute() {
				showProgressBar();
				File dir = new File(getActivity().getCacheDir(), "share");
				if (!dir.exists()) {
					dir.mkdir();
				}
				if (group == null) {
					src = helper.getExternalFile();
				}
				dst = new File(dir, src != null ? src.getName() : FavouritesDbHelper.FILE_TO_SAVE);
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (group != null) {
					helper.saveFile(group.getPoints(), dst);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void res) {
				hideProgressBar();
				if (getActivity() == null) {
					// user quit application
					return;
				}
				try {
					if (src != null && dst != null) {
						Algorithms.fileCopy(src, dst);
					}
					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					List<FavoriteGroup> groups;
					if (group != null) {
						groups = new ArrayList<>();
						groups.add(group);
					} else {
						groups = getMyApplication().getFavorites().getFavoriteGroups();
					}
					sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(generateHtmlPrint(groups).toString()));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_fav_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMyApplication(), dst));
					sendIntent.setType("text/plain");
					sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					startActivity(sendIntent);
				} catch (IOException e) {
					Toast.makeText(getActivity(), "Error sharing favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		};

		exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void export() {
		final File tosave = getMyApplication().getAppPath(FavouritesDbHelper.FILE_TO_SAVE);
		if (favouritesAdapter.isEmpty()) {
			Toast.makeText(getActivity(), R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
		} else if (!tosave.getParentFile().exists()) {
			Toast.makeText(getActivity(), R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
		} else {
			final AsyncTask<Void, Void, Exception > exportTask = new AsyncTask<Void, Void, Exception >() {
				@Override
				protected Exception doInBackground(Void... params) {
					return helper.exportFavorites();
				}

				@Override
				protected void onPreExecute() {
					showProgressBar();
				}

				@Override
				protected void onPostExecute(Exception  warning) {
					hideProgressBar();
					if (warning == null) {
						Toast.makeText(
								getActivity(),
								MessageFormat.format(getString(R.string.fav_saved_sucessfully),
										tosave.getAbsolutePath()), Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getActivity(), warning.getMessage(), Toast.LENGTH_LONG).show();
					}
				}
			};

			if (tosave.exists()) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.setMessage(R.string.fav_export_confirmation);
				bld.show();
			} else {
				exportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		}
	}

	private void initListExpandedState() {
		for (int i = 0; i < favouritesAdapter.getGroupCount(); i++) {
			String groupName = favouritesAdapter.getGroup(i).getName();
			if (getGroupExpandedPreference(groupName).get()) {
				listView.expandGroup(i);
			} else {
				listView.collapseGroup(i);
			}
		}
	}

	private OsmandSettings.OsmandPreference<Boolean> getGroupExpandedPreference(String groupName) {
		OsmandSettings.OsmandPreference<Boolean> preference = preferenceCache.get(groupName);
		if (preference == null) {
			String groupKey = groupName + GROUP_EXPANDED_POSTFIX;
			preference = getSettings().registerBooleanPreference(groupKey, false);
			preferenceCache.put(groupKey, preference);
		}
		return preference;
	}

	public void showOnMap(final FavouritePoint point, int groupPos, int childPos) {
		OsmandSettings settings = requireMyApplication().getSettings();
		settings.FAVORITES_TAB.set(FAV_TAB);
		selectedGroupPos = groupPos;
		selectedChildPos = childPos;
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		String pointType = PointDescription.POINT_TYPE_FAVORITE;
		FavoritesActivity.showOnMap(requireActivity(), this, location.getLatitude(), location.getLongitude(),
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
				if (selectedGroupPos != -1 && selectedChildPos != -1) {
					listView.setSelectedChild(selectedGroupPos, selectedChildPos, true);
				}
			}
		}
	}

	class FavouritesAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		private static final boolean showOptionsButton = false;
		Map<FavoriteGroup, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<>();
		List<FavoriteGroup> groups = new ArrayList<FavoriteGroup>();
		Filter myFilter;
		private Set<?> filter;

		void synchronizeGroups() {
			favoriteGroups.clear();
			groups.clear();
			List<FavoriteGroup> disablesGroups = new ArrayList<>();
			List<FavoriteGroup> gs = helper.getFavoriteGroups();
			Set<?> flt = filter;
			for (FavoriteGroup key : gs) {
				boolean empty = true;
				if (flt == null || flt.contains(key)) {
					empty = false;
					favoriteGroups.put(key, new ArrayList<>(key.getPoints()));
				} else {
					ArrayList<FavouritePoint> list = new ArrayList<>();
					for (FavouritePoint p : key.getPoints()) {
						if (flt.contains(p)) {
							list.add(p);
							empty = false;
						}
					}
					favoriteGroups.put(key, list);
				}
				if (!empty) {
					if (key.isVisible()) {
						groups.add(key);
					} else {
						disablesGroups.add(key);
					}
				}
			}
			groups.addAll(disablesGroups);
			notifyDataSetChanged();
		}

		@Override
		public FavouritePoint getChild(int groupPosition, int childPosition) {
			return favoriteGroups.get(groups.get(groupPosition)).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return favoriteGroups.get(groups.get(groupPosition)).size();
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
			OsmandApplication app = getMyApplication();
			boolean light = app.getSettings().isLightContent();
			final FavoriteGroup model = getGroup(groupPosition);
			boolean visible = model.isVisible();
			int enabledColor = light ? R.color.text_color_primary_light : R.color.text_color_primary_dark;
			int disabledColor = light ? R.color.text_color_secondary_light : R.color.text_color_secondary_dark;
			row.findViewById(R.id.group_divider).setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
			int color = model.getColor() == 0 || model.getColor() == Color.BLACK ? getResources().getColor(R.color.color_favorite) : model.getColor();
			if (!model.isPersonal()) {
				setCategoryIcon(app, app.getUIUtilities().getPaintedIcon(
						R.drawable.ic_action_fav_dark, visible ? (color | 0xff000000) : getResources().getColor(disabledColor)),
						groupPosition, isExpanded, row, light);
			}
			adjustIndicator(app, groupPosition, isExpanded, row, light);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			label.setTextColor(getResources().getColor(visible ? enabledColor : disabledColor));
			if (visible) {
				Typeface typeface = FontCache.getFont(getContext(), "fonts/Roboto-Medium.ttf");
				label.setTypeface(typeface, Typeface.NORMAL);
			} else {
				label.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
			}
			label.setText(model.getName().length() == 0 ? getString(R.string.shared_string_favorites) : model.getDisplayName(app));

			if (selectionMode) {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(groupsToDelete.contains(model));

				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<FavouritePoint> fvs = model.getPoints();
						if (ch.isChecked()) {
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
						favouritesAdapter.notifyDataSetInvalidated();
						updateSelectionMode(actionMode);
					}
				});
				row.findViewById(R.id.category_icon).setVisibility(View.GONE);
			} else {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
				ch.setVisibility(View.GONE);
				row.findViewById(R.id.category_icon).setVisibility(View.VISIBLE);
			}
			final View ch = row.findViewById(R.id.options);
			if (!selectionMode) {
				if (!model.isPersonal()) {
					((ImageView) ch).setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_overflow_menu_white));
					ch.setVisibility(View.VISIBLE);
					ch.setContentDescription(getString(R.string.shared_string_settings));
					ch.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							EditFavoriteGroupDialogFragment.showInstance(getChildFragmentManager(), model.getName());
						}

					});
				}
			} else {
				ch.setVisibility(View.GONE);
			}
			return row;
		}


		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView,
								 ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.favorites_list_item, parent, false);
				row.findViewById(R.id.list_divider).setVisibility(View.VISIBLE);
			}
			OsmandApplication app = getMyApplication();
			boolean light = app.getSettings().isLightContent();
			int enabledColor = light ? R.color.text_color_primary_light : R.color.text_color_primary_dark;
			int disabledColor = light ? R.color.text_color_secondary_light : R.color.text_color_secondary_dark;
			int disabledIconColor = light ? R.color.icon_color_default_light : R.color.icon_color_default_dark;

			TextView name = (TextView) row.findViewById(R.id.favourite_label);
			TextView distanceText = (TextView) row.findViewById(R.id.distance);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);

			final FavouritePoint model = getChild(groupPosition, childPosition);
			final FavoriteGroup group = getGroup(groupPosition);
			boolean visible = model.isVisible();
			row.setTag(model);

			if (showOptionsButton) {
				ImageView options = (ImageView) row.findViewById(R.id.options);
				options.setFocusable(false);
				options.setImageDrawable(getMyApplication().getUIUtilities().getThemedIcon(
						R.drawable.ic_overflow_menu_white));
				options.setVisibility(View.VISIBLE);
				options.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOnMap(model, groupPosition, childPosition);
					}
				});
			}
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			name.setText(model.getDisplayName(app), TextView.BufferType.SPANNABLE);
			name.setTypeface(Typeface.DEFAULT, visible ? Typeface.NORMAL : Typeface.ITALIC);
			name.setTextColor(getResources().getColor(visible ? enabledColor : disabledColor));
			distanceText.setText(distance);
			if (model.isAddressSpecified()) {
				distanceText.setText(String.format(getString(R.string.distance_and_address), distance.trim(), model.getAddress()));
			}
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(),
					visible ? model.getColor() : getResources().getColor(disabledIconColor), false, model));
			if (visible) {
				distanceText.setTextColor(getResources().getColor(R.color.color_distance));
			} else {
				distanceText.setTextColor(getResources().getColor(disabledColor));
			}
			row.findViewById(R.id.group_image).setVisibility(View.GONE);

			ImageView direction = (ImageView) row.findViewById(R.id.direction);
			direction.setVisibility(View.VISIBLE);
			if (visible) {
				direction.setImageDrawable(arrowImage);
			} else {
				direction.setImageDrawable(arrowImageDisabled);
			}

			final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(favoritesSelected.get(group.getName()) != null && favoritesSelected.get(group.getName()).contains(model));
				row.findViewById(R.id.favourite_icon).setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
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
								favouritesAdapter.notifyDataSetInvalidated();
							}
						}
						updateSelectionMode(actionMode);
					}
				});
			} else {
				row.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
			}
			return row;
		}

		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new FavoritesFilter();
			}
			return myFilter;
		}

		public void setFilterResults(Set<?> values) {
			this.filter = values;
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
	}

	public class FavoritesFilter extends Filter {


		public FavoritesFilter() {
		}

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
			synchronized (favouritesAdapter) {
				favouritesAdapter.setFilterResults((Set<?>) results.values);
				favouritesAdapter.synchronizeGroups();
			}
			favouritesAdapter.notifyDataSetChanged();
			if (constraint != null && constraint.length() > 1) {
				initListExpandedState();
			}
		}
	}
}
