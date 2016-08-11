package net.osmand.plus.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.TextUtils;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.ShowRouteInfoActivity.RouteInfoAdapter;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.base.OsmandExpandableListFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.plus.routing.RouteDirectionInfo;
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

import gnu.trove.list.array.TIntArrayList;


public class FavoritesTreeFragment extends OsmandExpandableListFragment {

	public static final int SEARCH_ID = -1;
	//	public static final int EXPORT_ID = 0;
	// public static final int IMPORT_ID = 1;
	public static final int DELETE_ID = 2;
	public static final int DELETE_ACTION_ID = 3;
	public static final int SHARE_ID = 4;
	public static final int SELECT_MAP_MARKERS_ID = 5;
	public static final int SELECT_MAP_MARKERS_ACTION_MODE_ID = 6;
	public static final String GROUP_EXPANDED_POSTFIX = "_group_expanded";

	private FavouritesAdapter favouritesAdapter = new FavouritesAdapter();
	private FavouritesDbHelper helper;

	private boolean selectionMode = false;
	private Set<FavouritePoint> favoritesSelected = new LinkedHashSet<>();
	private Set<FavoriteGroup> groupsToDelete = new LinkedHashSet<>();
	private ActionMode actionMode;
	private SearchView searchView;
	Drawable arrowImage;
	private HashMap<String, OsmandSettings.OsmandPreference<Boolean>> preferenceCache = new HashMap<>();

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		helper = getMyApplication().getFavorites();
		favouritesAdapter.synchronizeGroups();
		setAdapter(favouritesAdapter);

		boolean light = getMyApplication().getSettings().isLightContent();
		arrowImage = ContextCompat.getDrawable(context, R.drawable.ic_direction_arrow);
		arrowImage.mutate();
		if (light) {
			arrowImage.setColorFilter(ContextCompat.getColor(context, R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		} else {
			arrowImage.setColorFilter(ContextCompat.getColor(context, R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		}
	}

	private void deleteFavorites() {
		new AsyncTask<Void, Object, String>() {

			@Override
			protected void onPreExecute() {
				showProgressBar();
			}

			@Override
			protected void onPostExecute(String result) {
				hideProgressBar();
				favouritesAdapter.synchronizeGroups();
			}

			@Override
			protected String doInBackground(Void... params) {
				helper.delete(groupsToDelete, favoritesSelected);
				favoritesSelected.clear();
				groupsToDelete.clear();
				return getString(R.string.favourites_delete_multiple_succesful);
			}

		}.execute();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.favorites_tree, container, false);
		ExpandableListView listView = (ExpandableListView) view.findViewById(android.R.id.list);
		favouritesAdapter.synchronizeGroups();
		listView.setAdapter(favouritesAdapter);
		setListView(listView);
		setHasOptionsMenu(true);
		listView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
			@Override
			public void onGroupCollapse(int groupPosition) {
				String groupName = favouritesAdapter.getGroup(groupPosition).name;
				getGroupExpandedPreference(groupName).set(false);
			}
		});
		listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				String groupName = favouritesAdapter.getGroup(groupPosition).name;
				getGroupExpandedPreference(groupName).set(true);
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		favouritesAdapter.synchronizeGroups();
		initListExpandedState();
	}

	private void updateSelectionMode(ActionMode m) {
		if (favoritesSelected.size() > 0) {
			m.setTitle(favoritesSelected.size() + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else {
			m.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (selectionMode) {
			CheckBox ch = (CheckBox) v.findViewById(R.id.toggle_item);
			FavouritePoint model = favouritesAdapter.getChild(groupPosition, childPosition);
			ch.setChecked(!ch.isChecked());
			if (ch.isChecked()) {
				favoritesSelected.add(model);
			} else {
				favoritesSelected.remove(model);
			}
			updateSelectionMode(actionMode);
		} else {
			final FavouritePoint point = favouritesAdapter.getChild(groupPosition, childPosition);
			showOnMap(point);
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
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.search_poi_filter, R.drawable.ic_action_search_dark,
				R.drawable.ic_action_search_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		searchView = new SearchView(getActivity());
		FavoritesActivity.updateSearchView(getActivity(), searchView);
		MenuItemCompat.setActionView(mi, searchView);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				favouritesAdapter.getFilter().filter(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				favouritesAdapter.getFilter().filter(newText);
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
				favouritesAdapter.setFilterResults(null);
				favouritesAdapter.synchronizeGroups();
				favouritesAdapter.notifyDataSetChanged();
				// Needed to hide intermediate progress bar after closing action mode
				new Handler().postDelayed(new Runnable() {
					public void run() {
						hideProgressBar();
					}
				}, 100);
				return true;
			}
		});

		if (AndroidUiHelper.isOrientationPortrait(getActivity())) {
			menu = ((FavoritesActivity) getActivity()).getClearToolbar(true).getMenu();
		} else {
			((FavoritesActivity) getActivity()).getClearToolbar(false);
		}


		if (!MenuItemCompat.isActionViewExpanded(mi)) {
			createMenuItem(menu, SHARE_ID, R.string.shared_string_share, R.drawable.ic_action_gshare_dark,
					R.drawable.ic_action_gshare_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			if (getSettings().USE_MAP_MARKERS.get()) {
				createMenuItem(menu, SELECT_MAP_MARKERS_ID, R.string.select_map_markers, R.drawable.ic_action_flag_dark,
						R.drawable.ic_action_flag_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			} else {
				createMenuItem(menu, SELECT_MAP_MARKERS_ID, R.string.select_destination_and_intermediate_points, R.drawable.ic_action_intermediate,
						R.drawable.ic_action_intermediate, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			}
			createMenuItem(menu, DELETE_ID, R.string.shared_string_delete, R.drawable.ic_action_delete_dark,
					R.drawable.ic_action_delete_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//			createMenuItem(menu, EXPORT_ID, R.string.shared_string_export, R.drawable.ic_action_gsave_light,
//					R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);

		}
	}


	public void showProgressBar() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if(activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	public void hideProgressBar() {
		OsmandActionBarActivity activity = getActionBarActivity();
		if(activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
	}

	private void enterMapMarkersMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				if (getSettings().USE_MAP_MARKERS.get()) {
					createMenuItem(menu, SELECT_MAP_MARKERS_ACTION_MODE_ID, R.string.select_map_markers,
							R.drawable.ic_action_flag_dark, R.drawable.ic_action_flag_dark,
							MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				} else {
					createMenuItem(menu, SELECT_MAP_MARKERS_ACTION_MODE_ID, R.string.select_destination_and_intermediate_points,
							R.drawable.ic_action_intermediate, R.drawable.ic_action_intermediate,
							MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
				}
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
		if(!favoritesSelected.isEmpty()) {
			if (getSettings().USE_MAP_MARKERS.get()) {
				MapMarkersHelper markersHelper = getMyApplication().getMapMarkersHelper();
				List<LatLon> points = new ArrayList<>(favoritesSelected.size());
				List<PointDescription> names = new ArrayList<>(favoritesSelected.size());
				for (FavouritePoint fp : favoritesSelected) {
					points.add(new LatLon(fp.getLatitude(), fp.getLongitude()));
					names.add(new PointDescription(PointDescription.POINT_TYPE_MAP_MARKER, fp.getName()));
				}
				markersHelper.addMapMarkers(points, names);
				MapActivity.launchMapActivityMoveToTop(getActivity());
			} else {
				final TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
				for (FavouritePoint fp : favoritesSelected) {
					targetPointsHelper.navigateToPoint(new LatLon(fp.getLatitude(), fp.getLongitude()), false,
							targetPointsHelper.getIntermediatePoints().size() + 1,
							new PointDescription(PointDescription.POINT_TYPE_FAVORITE, fp.getName()));
				}
				if (getMyApplication().getRoutingHelper().isRouteCalculated()) {
					targetPointsHelper.updateRouteAndRefresh(true);
				}
				IntermediatePointsDialog.openIntermediatePointsDialog(getActivity(), getMyApplication(), true);
			}
		}
	}

	private void enterDeleteMode() {

		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, DELETE_ACTION_ID, R.string.shared_string_delete,
						R.drawable.ic_action_delete_dark, R.drawable.ic_action_delete_dark,
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
	}

	protected void openChangeGroupDialog(final FavoriteGroup group) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(R.layout.fav_group_edit, null);

		final EditText nameEditText = (EditText) view.findViewById(R.id.nameEditText);
		nameEditText.setText(group.name);

		final CheckBox checkBox = (CheckBox) view.findViewById(R.id.Visibility);
		checkBox.setChecked(group.visible);

		final Spinner colorSpinner = (Spinner) view.findViewById(R.id.ColorSpinner);
		final TIntArrayList list = new TIntArrayList();
		final int intColor = group.color == 0 ? getResources().getColor(R.color.color_favorite) : group.color;
		ColorDialogs.setupColorSpinner(getActivity(), intColor, colorSpinner, list);

		builder.setTitle(R.string.edit_group);
		builder.setView(view);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				int clr = list.get(colorSpinner.getSelectedItemPosition());
				String name = nameEditText.getText().toString();
				boolean nameChanged = !Algorithms.objectEquals(group.name, name);
				if (clr != intColor || group.visible != checkBox.isChecked() || nameChanged) {
					getMyApplication().getFavorites().editFavouriteGroup(group, name, clr,
							checkBox.isChecked());
					if (nameChanged) {
						favouritesAdapter.synchronizeGroups();
					}
					favouritesAdapter.notifyDataSetInvalidated();
				}

			}
		});
		builder.show();
	}

	private void deleteFavoritesAction() {
		if (groupsToDelete.size() + favoritesSelected.size() > 0) {

			AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(getString(R.string.favorite_delete_multiple, favoritesSelected.size(), groupsToDelete.size()));
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
	
	private StringBuilder generateHtmlPrint() {
		StringBuilder html = new StringBuilder();
		html.append("<h1>My Favorites</h1>");
		List<FavoriteGroup> groups = getMyApplication().getFavorites().getFavoriteGroups();
		for(FavoriteGroup group : groups) {
			html.append("<h3>"+group.name+"</h3>");
			for(FavouritePoint fp : group.points) {
				String url = "geo:"+((float)fp.getLatitude())+","+((float)fp.getLongitude())+"?m="+fp.getName();
				html.append("<p>" + fp.getName() + " - " + "<a href=\"" + url + "\">geo:"
						+ ((float) fp.getLatitude()) + "," + ((float) fp.getLongitude()) + "</a><br>");
				
				if(!Algorithms.isEmpty(fp.getDescription())) {
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
			final AsyncTask<Void, Void, GPXFile> exportTask = new AsyncTask<Void, Void, GPXFile>() {
				@Override
				protected GPXFile doInBackground(Void... params) {
					return helper.asGpxFile();
				}

				@Override
				protected void onPreExecute() {
					showProgressBar();
				}

				@Override
				protected void onPostExecute(GPXFile gpxFile) {
					hideProgressBar();
					File dir = new File(getActivity().getCacheDir(), "share");
					if (!dir.exists()) {
						dir.mkdir();
					}
					File src = helper.getExternalFile();
					File dst = new File(dir, src.getName());
					try {
						Algorithms.fileCopy(src, dst);
						final Intent sendIntent = new Intent();
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(generateHtmlPrint().toString()));
						sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_fav_subject));
						sendIntent.putExtra(Intent.EXTRA_STREAM,
								FileProvider.getUriForFile(getActivity(),
										getActivity().getPackageName() + ".fileprovider", dst));
						sendIntent.setType("text/plain");
						startActivity(sendIntent);
					} catch (IOException e) {
						//Toast.makeText(getActivity(), "Error sharing favorites: " + e.getMessage(), Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}
				}
			};

			exportTask.execute();
		}
	}

	protected void export() {
		final File tosave = getMyApplication().getAppPath(FavouritesDbHelper.FILE_TO_SAVE);
		if (favouritesAdapter.isEmpty()) {
			Toast.makeText(getActivity(), R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
		} else if (!tosave.getParentFile().exists()) {
			Toast.makeText(getActivity(), R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
		} else {
			final AsyncTask<Void, Void, String> exportTask = new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(Void... params) {
					return helper.exportFavorites();
				}

				@Override
				protected void onPreExecute() {
					showProgressBar();
				}

				@Override
				protected void onPostExecute(String warning) {
					hideProgressBar();
					if (warning == null) {
						Toast.makeText(
								getActivity(),
								MessageFormat.format(getString(R.string.fav_saved_sucessfully),
										tosave.getAbsolutePath()), Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getActivity(), warning, Toast.LENGTH_LONG).show();
					}
				}
			};

			if (tosave.exists()) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						exportTask.execute();
					}
				});
				bld.setNegativeButton(R.string.shared_string_no, null);
				bld.setMessage(R.string.fav_export_confirmation);
				bld.show();
			} else {
				exportTask.execute();
			}
		}
	}

	private void initListExpandedState() {
		for (int i = 0; i < favouritesAdapter.getGroupCount(); i++) {
			String groupName = favouritesAdapter.getGroup(i).name;
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

	public void showOnMap(final FavouritePoint point) {
		getMyApplication().getSettings().FAVORITES_TAB.set(FavoritesActivity.FAV_TAB);

		final OsmandSettings settings = getMyApplication().getSettings();
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
				settings.getLastKnownMapZoom(),
				new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName()),
				true,
				point); //$NON-NLS-1$
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	class FavouritesAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		private static final boolean showOptionsButton = false;
		Map<FavoriteGroup, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<>();
		List<FavoriteGroup> groups = new ArrayList<FavoriteGroup>();
		Filter myFilter;
		private Set<?> filter;

		public void synchronizeGroups() {
			favoriteGroups.clear();
			groups.clear();
			List<FavoriteGroup> gs = helper.getFavoriteGroups();
			Set<?> flt = filter;
			for (FavoriteGroup key : gs) {
				boolean empty = true;
				if (flt == null || flt.contains(key)) {
					empty = false;
					favoriteGroups.put(key, new ArrayList<>(key.points));
				} else {
					ArrayList<FavouritePoint> list = new ArrayList<>();
					for (FavouritePoint p : key.points) {
						if (flt.contains(p)) {
							list.add(p);
							empty = false;
						}
					}
					favoriteGroups.put(key, list);
				}
				if (!empty) {
					groups.add(key);
				}
			}
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
			adjustIndicator(groupPosition, isExpanded, row, getMyApplication().getSettings().isLightContent());
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final FavoriteGroup model = getGroup(groupPosition);
			label.setText(model.name.length() == 0 ? getString(R.string.shared_string_favorites) : model.name);

			if (selectionMode) {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(groupsToDelete.contains(model));

				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							groupsToDelete.add(model);
							List<FavouritePoint> fvs = model.points;
							if (fvs != null) {
								favoritesSelected.addAll(fvs);
							}
							favouritesAdapter.notifyDataSetInvalidated();
						} else {
							groupsToDelete.remove(model);
						}
						updateSelectionMode(actionMode);
					}
				});
			} else {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
				ch.setVisibility(View.GONE);
			}
			final View ch = row.findViewById(R.id.options);
			if (!selectionMode) {
				((ImageView) ch).setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_overflow_menu_white));
				ch.setVisibility(View.VISIBLE);
				ch.setContentDescription(getString(R.string.shared_string_settings));
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openChangeGroupDialog(model);
					}

				});
			} else {
				ch.setVisibility(View.GONE);
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
			}

			TextView name = (TextView) row.findViewById(R.id.favourite_label);
			TextView distanceText = (TextView) row.findViewById(R.id.distance);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);

			final FavouritePoint model = getChild(groupPosition, childPosition);
			row.setTag(model);

			if (showOptionsButton) {
				ImageView options = (ImageView) row.findViewById(R.id.options);
				options.setFocusable(false);
				options.setImageDrawable(getMyApplication().getIconsCache().getThemedIcon(
						R.drawable.ic_overflow_menu_white));
				options.setVisibility(View.VISIBLE);
				options.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showOnMap(model);
					}
				});
			}
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), model.getColor(), false));
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			name.setText(model.getName(), TextView.BufferType.SPANNABLE);
			name.setTypeface(Typeface.DEFAULT, model.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
			distanceText.setText(distance);
			distanceText.setTextColor(getResources().getColor(R.color.color_distance));
			row.findViewById(R.id.group_image).setVisibility(View.GONE);

			ImageView direction = (ImageView) row.findViewById(R.id.direction);
			direction.setVisibility(View.VISIBLE);
			direction.setImageDrawable(arrowImage);

			final CheckBox ch = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(favoritesSelected.contains(model));
				row.findViewById(R.id.favourite_icon).setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							favoritesSelected.add(model);
						} else {
							favoritesSelected.remove(model);
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
					if (g.name.toLowerCase().contains(cs)) {
						filter.add(g);
					} else {
						for (FavouritePoint fp : g.points) {
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
