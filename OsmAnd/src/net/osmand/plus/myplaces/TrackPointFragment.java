package net.osmand.plus.myplaces;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.IntermediatePointsDialog;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackPointFragment extends OsmandExpandableListFragment {

	public static final String ARG_TO_FILTER_SHORT_TRACKS = "ARG_TO_FILTER_SHORT_TRACKS";

	public static final int SEARCH_ID = -1;
	public static final int DELETE_ID = 2;
	public static final int DELETE_ACTION_ID = 3;
	public static final int SHARE_ID = 4;
	public static final int SELECT_MAP_MARKERS_ID = 5;
	public static final int SELECT_MAP_MARKERS_ACTION_MODE_ID = 6;
	public static final int SELECT_FAVORITES_ID = 7;
	public static final int SELECT_FAVORITES_ACTION_MODE_ID = 8;

	private OsmandApplication app;
	final private PointGPXAdapter adapter = new PointGPXAdapter();
	private GpxDisplayItemType[] filterTypes = { GpxDisplayItemType.TRACK_POINTS, GpxDisplayItemType.TRACK_ROUTE_POINTS };
	private boolean selectionMode = false;
	private Set<GpxDisplayItem> selectedItems = new LinkedHashSet<>();
	private Set<Integer> selectedGroups = new LinkedHashSet<>();
	private ActionMode actionMode;
	private SearchView searchView;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = getMyApplication();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView.setBackgroundColor(getResources().getColor(
				getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
						: R.color.ctx_menu_info_view_bg_dark));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.favorites_tree, container, false);
		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		setHasOptionsMenu(true);

		TextView tv = new TextView(getActivity());
		tv.setText(R.string.none_selected_gpx);
		tv.setTextSize(24);
		listView.setEmptyView(tv);

		setContent(listView);
		setListView(listView);
		expandAllGroups();
		return view;
	}

	public TrackActivity getTrackActivity() {
		return (TrackActivity) getActivity();
	}

	private GPXFile getGpx() {
		return getTrackActivity().getGpx();
	}

	private GpxDataItem getGpxDataItem() {
		return getTrackActivity().getGpxDataItem();
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			getExpandableListView().expandGroup(i);
		}
	}

	private void setupListView(ListView listView) {
		if (!adapter.isEmpty() && listView.getHeaderViewsCount() == 0) {
			listView.addHeaderView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_header, null, false));
			listView.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));
		}
	}

	private boolean isArgumentTrue(@NonNull String arg) {
		return getArguments() != null && getArguments().getBoolean(arg);
	}

	private boolean hasFilterType(GpxDisplayItemType filterType) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	private List<GpxDisplayGroup> filterGroups() {
		List<GpxDisplayGroup> groups = new ArrayList<>();
		if (getTrackActivity() != null) {
			List<GpxDisplayGroup> result = getTrackActivity().getGpxFile(false);
			for (GpxDisplayGroup group : result) {
				boolean add = hasFilterType(group.getType());
				if (isArgumentTrue(ARG_TO_FILTER_SHORT_TRACKS)) {
					Iterator<GpxDisplayItem> item = group.getModifiableList().iterator();
					while (item.hasNext()) {
						GpxDisplayItem it2 = item.next();
						if (it2.analysis != null && it2.analysis.totalDistance < 100) {
							item.remove();
						}
					}
					if (group.getModifiableList().isEmpty()) {
						add = false;
					}
				}
				if (add) {
					groups.add(group);
				}

			}
		}
		return groups;
	}

	public void setContent() {
		setContent(listView);
		expandAllGroups();
	}

	public void setContent(ExpandableListView listView) {
		adapter.synchronizeGroups(filterGroups());
		setupListView(listView);
		if (listView.getAdapter() == null) {
			listView.setAdapter(adapter);
		}
	}

	protected List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for(GpxDisplayGroup g : groups) {
			list.addAll(g.getModifiableList());
		}
		return list;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == SELECT_MAP_MARKERS_ID) {
			selectMapMarkers();
			return true;
		} else if (item.getItemId() == SELECT_FAVORITES_ID) {
			selectFavorites();
			return true;
		} else if (item.getItemId() == SHARE_ID) {
			shareItems();
			return true;
		} else if (item.getItemId() == DELETE_ID) {
			enterDeleteMode();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void selectMapMarkers() {
		enterMapMarkersMode();
	}

	private void selectFavorites() {
		enterFavoritesMode();
	}

	private void shareItems() {
		final Uri fileUri = Uri.fromFile(new File(getGpx().path));
		final Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		sendIntent.setType("application/gpx+xml");
		startActivity(sendIntent);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark,
				R.drawable.ic_action_search_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		searchView = new SearchView(getActivity());
		FavoritesActivity.updateSearchView(getActivity(), searchView);
		MenuItemCompat.setActionView(mi, searchView);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				adapter.getFilter().filter(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				adapter.getFilter().filter(newText);
				return true;
			}
		});
		MenuItemCompat.setOnActionExpandListener(mi, new MenuItemCompat.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				adapter.setFilterResults(null);
				adapter.synchronizeGroups(filterGroups());
				adapter.notifyDataSetChanged();
				// Needed to hide intermediate progress bar after closing action mode
				new Handler().postDelayed(new Runnable() {
					public void run() {
						hideProgressBar();
					}
				}, 100);
				return true;
			}
		});

		if (!MenuItemCompat.isActionViewExpanded(mi)) {
			createMenuItem(menu, SHARE_ID, R.string.shared_string_share, R.drawable.ic_action_gshare_dark,
					R.drawable.ic_action_gshare_dark, MenuItemCompat.SHOW_AS_ACTION_NEVER);
			if (getSettings().USE_MAP_MARKERS.get()) {
				createMenuItem(menu, SELECT_MAP_MARKERS_ID, R.string.shared_string_add_to_map_markers, R.drawable.ic_action_flag_dark,
						R.drawable.ic_action_flag_dark, MenuItemCompat.SHOW_AS_ACTION_NEVER);
			} else {
				createMenuItem(menu, SELECT_MAP_MARKERS_ID, R.string.select_destination_and_intermediate_points, R.drawable.ic_action_intermediate,
						R.drawable.ic_action_intermediate, MenuItemCompat.SHOW_AS_ACTION_NEVER);
			}
			createMenuItem(menu, SELECT_FAVORITES_ID, R.string.shared_string_add_to_favorites, R.drawable.ic_action_fav_dark,
					R.drawable.ic_action_fav_dark, MenuItemCompat.SHOW_AS_ACTION_NEVER);
			createMenuItem(menu, DELETE_ID, R.string.shared_string_delete, R.drawable.ic_action_delete_dark,
					R.drawable.ic_action_delete_dark, MenuItemCompat.SHOW_AS_ACTION_NEVER);
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

	private void enableSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
	}

	private void enterDeleteMode() {

		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, DELETE_ACTION_ID, R.string.shared_string_delete,
						R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_dark,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				selectedItems.clear();
				selectedGroups.clear();
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
					deleteItemsAction();
				}
				return true;
			}

		});

	}

	private void deleteItemsAction() {
		if (selectedItems.size() > 0) {
			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(getString(R.string.points_delete_multiple, selectedItems.size()));
			b.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (actionMode != null) {
						actionMode.finish();
					}
					deleteItems();
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private void deleteItems() {
		new AsyncTask<Void, Object, String>() {

			@Override
			protected void onPreExecute() {
				showProgressBar();
			}

			@Override
			protected void onPostExecute(String result) {
				hideProgressBar();
				adapter.synchronizeGroups(filterGroups());
			}

			@Override
			protected String doInBackground(Void... params) {
				GPXFile gpx = getGpx();
				SavingTrackHelper savingTrackHelper = app.getSavingTrackHelper();
				if (gpx != null) {
					for (GpxDisplayItem item : selectedItems) {
						if (gpx.showCurrentTrack) {
							savingTrackHelper.deletePointData(item.locationStart);
						} else {
							if (item.group.getType() == GpxDisplayItemType.TRACK_POINTS) {
								gpx.deleteWptPt(item.locationStart);
							} else if (item.group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
								gpx.deleteRtePt(item.locationStart);
							}
						}
					}
					if (!gpx.showCurrentTrack) {
						GPXUtilities.writeGpxFile(new File(gpx.path), gpx, app);
						boolean selected = app.getSelectedGpxHelper().getSelectedFileByPath(gpx.path) != null;
						if (selected) {
							app.getSelectedGpxHelper().setGpxFileToDisplay(gpx);
						}
					}
				}
				selectedItems.clear();
				selectedGroups.clear();
				return getString(R.string.points_delete_multiple_succesful);
			}

		}.execute();
	}

	private void enterMapMarkersMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				if (getSettings().USE_MAP_MARKERS.get()) {
					createMenuItem(menu, SELECT_MAP_MARKERS_ACTION_MODE_ID, R.string.shared_string_add_to_map_markers,
							R.drawable.ic_action_flag_dark, R.drawable.ic_action_flag_dark,
							MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				} else {
					createMenuItem(menu, SELECT_MAP_MARKERS_ACTION_MODE_ID, R.string.select_destination_and_intermediate_points,
							R.drawable.ic_action_intermediate, R.drawable.ic_action_intermediate,
							MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				}
				selectedItems.clear();
				selectedGroups.clear();
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
		if (!selectedItems.isEmpty()) {
			if (getSettings().USE_MAP_MARKERS.get()) {
				MapMarkersHelper markersHelper = app.getMapMarkersHelper();
				List<LatLon> points = new ArrayList<>();
				List<PointDescription> names = new ArrayList<>();
				for(GpxDisplayItem i : selectedItems) {
					if (i.locationStart != null) {
						points.add(new LatLon(i.locationStart.lat, i.locationStart.lon));
						names.add(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, i.name));
					}
				}
				markersHelper.addMapMarkers(points, names);
				MapActivity.launchMapActivityMoveToTop(getActivity());
			} else {
				final TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
				for (GpxDisplayItem i : selectedItems) {
					if (i.locationStart != null) {
						targetPointsHelper.navigateToPoint(new LatLon(i.locationStart.lat, i.locationStart.lon), false,
								targetPointsHelper.getIntermediatePoints().size() + 1,
								new PointDescription(PointDescription.POINT_TYPE_FAVORITE, i.name));
					}
				}
				if (getMyApplication().getRoutingHelper().isRouteCalculated()) {
					targetPointsHelper.updateRouteAndRefresh(true);
				}
				IntermediatePointsDialog.openIntermediatePointsDialog(getActivity(), getMyApplication(), true);
			}
		}
	}

	private void enterFavoritesMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, SELECT_FAVORITES_ACTION_MODE_ID, R.string.shared_string_add_to_favorites,
						R.drawable.ic_action_fav_dark, R.drawable.ic_action_fav_dark,
						MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				selectedItems.clear();
				selectedGroups.clear();
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
				if (item.getItemId() == SELECT_FAVORITES_ACTION_MODE_ID) {
					selectFavoritesImpl();
				}
				return true;
			}
		});

	}

	private void selectFavoritesImpl() {
		if (!selectedItems.isEmpty()) {
			AlertDialog.Builder b = new AlertDialog.Builder(getTrackActivity());
			final EditText editText = new EditText(getTrackActivity());
			String name = selectedItems.iterator().next().group.getName();
			if(name.indexOf('\n') > 0) {
				name = name.substring(0, name.indexOf('\n'));
			}
			editText.setText(name);
			int leftMargin = AndroidUtils.dpToPx(getContext(), 16f);
			int topMargin = AndroidUtils.dpToPx(getContext(), 8f);
			editText.setPadding(leftMargin, topMargin, leftMargin, topMargin);
			b.setTitle(R.string.save_as_favorites_points);
			b.setView(editText);
			b.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (actionMode != null) {
						actionMode.finish();
					}
					FavouritesDbHelper fdb = app.getFavorites();
					for(GpxDisplayItem i : selectedItems) {
						if (i.locationStart != null) {
							FavouritePoint fp = new FavouritePoint(i.locationStart.lat, i.locationStart.lon, i.name, editText.getText().toString());
							if (!Algorithms.isEmpty(i.description)) {
								fp.setDescription(i.description);
							}
							fdb.addFavourite(fp, false);
						}
					}
					fdb.saveCurrentPointsIntoFile();
					MapActivity.launchMapActivityMoveToTop(getActivity());
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private void updateSelectionMode(ActionMode m) {
		if (selectedItems.size() > 0) {
			m.setTitle(selectedItems.size() + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (selectionMode) {
			CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
			GpxDisplayItem item = adapter.getChild(groupPosition, childPosition);
			ch.setChecked(!ch.isChecked());
			if (ch.isChecked()) {
				selectedItems.add(item);
			} else {
				selectedItems.remove(item);
			}
			updateSelectionMode(actionMode);
		} else {
			final GpxDisplayItem item = adapter.getChild(groupPosition, childPosition);
			if (item != null) {
				if (item.group.getGpx() != null) {
					app.getSelectedGpxHelper().setGpxFileToDisplay(item.group.getGpx());
				}
				final OsmandSettings settings = app.getSettings();
				LatLon location = new LatLon(item.locationStart.lat, item.locationStart.lon);
				settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, item.name),
						false,
						item.locationStart);

				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		}
		return true;
	}

	class PointGPXAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups = new LinkedHashMap<>();
		List<GpxDisplayGroup> groups = new ArrayList<>();
		Filter myFilter;
		private Set<?> filter;

		public void synchronizeGroups(List<GpxDisplayGroup> gs) {
			itemGroups.clear();
			groups.clear();
			Set<?> flt = filter;
			Collections.sort(gs, new Comparator<GpxDisplayGroup>() {
				@Override
				public int compare(GpxDisplayGroup g1, GpxDisplayGroup g2) {
					int i1 = g1.getType().ordinal();
					int i2 = g2.getType().ordinal();
					return i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
				}
			});
			for (GpxDisplayGroup g : gs) {
				if (g.getModifiableList().isEmpty()) {
					continue;
				}
				boolean empty = true;
				if (flt == null) {
					empty = false;
					itemGroups.put(g, new ArrayList<>(g.getModifiableList()));
				} else {
					ArrayList<GpxDisplayItem> list = new ArrayList<>();
					for (GpxDisplayItem i : g.getModifiableList()) {
						if (flt.contains(i)) {
							list.add(i);
							empty = false;
						}
					}
					itemGroups.put(g, list);
				}
				if (!empty) {
					groups.add(g);
				}
			}
			notifyDataSetChanged();
		}

		@Override
		public GpxDisplayItem getChild(int groupPosition, int childPosition) {
			return itemGroups.get(groups.get(groupPosition)).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return itemGroups.get(groups.get(groupPosition)).size();
		}

		@Override
		public GpxDisplayGroup getGroup(int groupPosition) {
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
		public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View row = convertView;
			boolean checkBox = row != null && row.findViewById(R.id.toggle_item) instanceof CheckBox;
			boolean same = (selectionMode && checkBox) || (!selectionMode && !checkBox);
			if (row == null || !same) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.wpt_list_item_category, parent, false);
			}
			row.setOnClickListener(null);
			final GpxDisplayGroup group = getGroup(groupPosition);
			row.findViewById(R.id.group_divider).setVisibility(groupPosition == 0 ? View.GONE : View.VISIBLE);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			TextView description = (TextView) row.findViewById(R.id.category_desc);
			if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
				label.setText(getString(R.string.waypoints));
				description.setText(getString(R.string.track_points_category_name));
			} else {
				label.setText(getString(R.string.route_points));
				description.setText(getString(R.string.route_points_category_name));
			}

			if (selectionMode) {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(selectedGroups.contains(groupPosition));

				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<GpxDisplayItem> items = itemGroups.get(group);
						if (ch.isChecked()) {
							selectedGroups.add(groupPosition);
							if (items != null) {
								selectedItems.addAll(items);
							}
						} else {
							selectedGroups.remove(groupPosition);
							if (items != null) {
								selectedItems.removeAll(items);
							}
						}
						adapter.notifyDataSetInvalidated();
						updateSelectionMode(actionMode);
					}
				});
			} else {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
				ch.setVisibility(View.GONE);
			}
			row.findViewById(R.id.category_icon).setVisibility(View.GONE);
			row.findViewById(R.id.options).setVisibility(View.GONE);
			return row;
		}


		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
								 ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.wpt_list_item, parent, false);
			}
			if (childPosition == 0) {
				row.findViewById(R.id.divider).setVisibility(View.VISIBLE);
				row.findViewById(R.id.list_divider).setVisibility(View.GONE);
			} else {
				row.findViewById(R.id.divider).setVisibility(View.GONE);
				row.findViewById(R.id.list_divider).setVisibility(View.VISIBLE);
			}

			ImageView icon = (ImageView) row.findViewById(R.id.icon);
			TextView title = (TextView) row.findViewById(R.id.label);
			TextView description = (TextView) row.findViewById(R.id.description);

			final GpxDisplayItem gpxItem = getChild(groupPosition, childPosition);
			boolean isWpt = gpxItem.group.getType() == GpxDisplayItemType.TRACK_POINTS;
			ImageView options = (ImageView) row.findViewById(R.id.options);
			if (isWpt) {
				options.setFocusable(false);
				options.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(
						R.drawable.ic_overflow_menu_white));
				options.setVisibility(View.VISIBLE);
				options.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						IconsCache iconsCache = getMyApplication().getIconsCache();
						final PopupMenu optionsMenu = new PopupMenu(getActivity(), v);
						DirectionsDialogs.setupPopUpMenuIcon(optionsMenu);

						MenuItem menuItem = optionsMenu.getMenu().add(R.string.shared_string_edit).setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_edit_dark));
						menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem mItem) {
								final OsmandSettings settings = app.getSettings();
								LatLon location = new LatLon(gpxItem.locationStart.lat, gpxItem.locationStart.lon);
								if (gpxItem.group.getGpx() != null) {
									app.getSelectedGpxHelper().setGpxFileToDisplay(gpxItem.group.getGpx());
								}
								settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
										settings.getLastKnownMapZoom(),
										new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
										false,
										gpxItem.locationStart);
								settings.setEditObjectToShow();

								MapActivity.launchMapActivityMoveToTop(getActivity());
								return true;
							}
						});
						optionsMenu.show();
					}
				});
				int groupColor = gpxItem.group.getColor();
				if (gpxItem.locationStart != null) {
					groupColor = gpxItem.locationStart.getColor(groupColor);
				}
				if (groupColor == 0) {
					groupColor = getTrackActivity().getResources().getColor(R.color.gpx_color_point);
				}
				icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), groupColor, false));
			} else {
				icon.setImageDrawable(app.getIconsCache().getThemedIcon(R.drawable.ic_action_marker_dark));
				options.setVisibility(View.GONE);
			}
			title.setText(gpxItem.name);
			if (!Algorithms.isEmpty(gpxItem.description)) {
				description.setText(gpxItem.description);
				description.setVisibility(View.VISIBLE);
			} else {
				description.setVisibility(View.GONE);
			}

			final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(selectedItems.contains(gpxItem));
				row.findViewById(R.id.icon).setVisibility(View.GONE);
				options.setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							selectedItems.add(gpxItem);
						} else {
							selectedItems.remove(gpxItem);
						}
						updateSelectionMode(actionMode);
					}
				});
			} else {
				row.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
			}
			return row;
		}

		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new PointsFilter();
			}
			return myFilter;
		}

		public void setFilterResults(Set<?> values) {
			this.filter = values;
		}
	}

	public class PointsFilter extends Filter {

		public PointsFilter() {
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
				for (GpxDisplayGroup g : filterGroups()) {
					for (GpxDisplayItem i : g.getModifiableList()) {
						if (i.name.toLowerCase().contains(cs)) {
							filter.add(i);
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
				adapter.synchronizeGroups(filterGroups());
			}
			adapter.notifyDataSetChanged();
			expandAllGroups();
		}
	}
}
