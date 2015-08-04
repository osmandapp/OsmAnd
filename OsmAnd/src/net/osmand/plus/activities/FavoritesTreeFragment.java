package net.osmand.plus.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.dialogs.DirectionsDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.myplaces.FavoritesActivity;
import net.osmand.util.MapUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
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
	public static final int SELECT_DESTINATIONS_ID = 5;
	public static final int SELECT_DESTINATIONS_ACTION_MODE_ID = 6;

	private FavouritesAdapter favouritesAdapter = new FavouritesAdapter();;
	private FavouritesDbHelper helper;

	private boolean selectionMode = false;
	private Set<FavouritePoint> favoritesSelected = new LinkedHashSet<FavouritePoint>();
	private Set<FavoriteGroup> groupsToDelete = new LinkedHashSet<FavoriteGroup>();
	private ActionMode actionMode;
	private SearchView searchView;
	Drawable arrowImage;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		helper = getMyApplication().getFavorites();
		favouritesAdapter.synchronizeGroups();
		setAdapter(favouritesAdapter);

		boolean light = getMyApplication().getSettings().isLightContent();
		arrowImage = getResources().getDrawable(R.drawable.ic_destination_arrow_white);
		arrowImage.mutate();
		if (light) {
			arrowImage.setColorFilter(getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		} else {
			arrowImage.setColorFilter(getResources().getColor(R.color.color_distance), PorterDuff.Mode.MULTIPLY);
		}
	}

	private void deleteFavorites() {
		new AsyncTask<Void, Object, String>() {

			@Override
			protected void onPreExecute() {
				showProgressBar();
			};

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
		ExpandableListView listView = (ExpandableListView)view.findViewById(android.R.id.list);
		favouritesAdapter.synchronizeGroups();
		listView.setAdapter(favouritesAdapter);
		setListView(listView);
		setHasOptionsMenu(true);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		// final LatLon mapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
		favouritesAdapter.synchronizeGroups();

		collapseTrees(5);
//		if(favouritesAdapter.getGroupCount() > 0 &&
//				"".equals(favouritesAdapter.getGroup(0).name)) {
//			getExpandableListView().expandGroup(0);
//		}
	}
	
	private void updateSelectionMode(ActionMode m) {
		if(favoritesSelected.size() > 0) {
			m.setTitle(favoritesSelected.size() + " " + getMyApplication().getString(R.string.shared_string_selected_lowercase));
		} else{
			m.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		IconsCache iconsCache = getMyApplication().getIconsCache();
		if (selectionMode) {
			CheckBox ch = (CheckBox) v.findViewById(R.id.check_item);
			FavouritePoint model = favouritesAdapter.getChild(groupPosition, childPosition);
			ch.setChecked(!ch.isChecked());
			if (ch.isChecked()) {
				favoritesSelected.add(model);
			} else {
				favoritesSelected.remove(model);
			}
			updateSelectionMode(actionMode);
		} else {
			final FavouritePoint point = (FavouritePoint) favouritesAdapter.getChild(groupPosition, childPosition);
			showItemPopupOptionsMenu(point, v);
		}
		return true;
	}

	public static boolean editPoint(Context ctx, final FavouritePoint point, final Runnable callback) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.favourites_context_menu_edit);
		final View v = LayoutInflater.from(ctx).inflate(R.layout.favorite_edit_dialog,
				null, false);
		final AutoCompleteTextView cat = (AutoCompleteTextView) v.findViewById(R.id.Category);
		final EditText editText = (EditText) v.findViewById(R.id.Name);
		final EditText editDescr = (EditText) v.findViewById(R.id.descr);
		builder.setView(v);
		editText.setText(point.getName());
		editDescr.setText(point.getDescription());
		cat.setText(point.getCategory());
		cat.setThreshold(1);
		final FavouritesDbHelper helper = app.getFavorites();
		List<FavoriteGroup> gs = helper.getFavoriteGroups();
		String[] list = new String[gs.size()];
		for(int i = 0; i < list.length; i++) {
			list[i] =gs.get(i).name;
		}
		cat.setAdapter(new ArrayAdapter<String>(ctx, R.layout.list_textview, list));
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean edited = helper.editFavouriteName(point, editText.getText().toString().trim(), cat.getText()
						.toString(), editDescr.getText().toString());
				if (edited && callback != null) {
					callback.run();
					
				}

			}
		});
		builder.create().show();
		editText.requestFocus();
		return true;
	}

	private boolean deletePoint(final FavouritePoint point) {
		final Resources resources = this.getResources();
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getString(R.string.favourites_remove_dialog_msg, point.getName()));
		builder.setNegativeButton(R.string.shared_string_no, null);
		builder.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean deleted = helper.deleteFavourite(point);
				if (deleted) {
					AccessibleToast.makeText(
							getActivity(),
							MessageFormat.format(resources.getString(R.string.favourites_remove_dialog_success),
									point.getName()), Toast.LENGTH_SHORT).show();
					favouritesAdapter.synchronizeGroups();
				}

			}
		});
		builder.create().show();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		if (item.getElementId() == EXPORT_ID) {
//			export();
//			return true;
//		} else 
		if (item.getItemId() == SELECT_DESTINATIONS_ID) {
			selectDestinations();
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

	private void selectDestinations() {
		final TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
		if (targetPointsHelper.getIntermediatePoints().size() > 0) {
			final FragmentActivity act = getActivity();
			Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(R.string.new_directions_point_dialog);
			builder.setItems(
					new String[] { act.getString(R.string.keep_intermediate_points),
							act.getString(R.string.clear_intermediate_points)},
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 1) {
								targetPointsHelper.clearPointToNavigate(false);
							}
							enterIntermediatesMode();
						}
					});
			builder.show();
		} else {
			enterIntermediatesMode();
		}
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
			createMenuItem(menu, SELECT_DESTINATIONS_ID, R.string.select_destination_and_intermediate_points, R.drawable.ic_action_flage_dark,
					R.drawable.ic_action_flage_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			createMenuItem(menu, DELETE_ID, R.string.shared_string_delete, R.drawable.ic_action_delete_dark,
					R.drawable.ic_action_delete_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
//			createMenuItem(menu, EXPORT_ID, R.string.shared_string_export, R.drawable.ic_action_gsave_light,
//					R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
		}
	}



	public void showProgressBar() {
		getActionBarActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		getActionBarActivity().setSupportProgressBarIndeterminateVisibility(false);
	}
	
	private void enterIntermediatesMode() {
		actionMode = getActionBarActivity().startSupportActionMode(new ActionMode.Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				enableSelectionMode(true);
				createMenuItem(menu, SELECT_DESTINATIONS_ACTION_MODE_ID, R.string.select_destination_and_intermediate_points,
						R.drawable.ic_action_flage_dark, R.drawable.ic_action_flage_dark,
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
				if (item.getItemId() == SELECT_DESTINATIONS_ACTION_MODE_ID) {
					mode.finish();
					selectDestinationImpl();
				}
				return true;
			}
		});

	}
	
	private void selectDestinationImpl() {
		if(!favoritesSelected.isEmpty()) {
			final TargetPointsHelper targetPointsHelper = getMyApplication().getTargetPointsHelper();
			for(FavouritePoint fp : favoritesSelected) {
				targetPointsHelper.navigateToPoint(new LatLon(fp.getLatitude(), fp.getLongitude()), false, 
						targetPointsHelper.getIntermediatePoints().size() + 1, 
						new PointDescription(PointDescription.POINT_TYPE_FAVORITE, fp.getName()));		
			}
			if(getMyApplication().getRoutingHelper().isRouteCalculated()) {
				targetPointsHelper.updateRouteAndReferesh(true);
			}
			IntermediatePointsDialog.openIntermediatePointsDialog(getActivity(), getMyApplication(), true);
			//MapActivity.launchMapActivityMoveToTop(getActivity());
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
		((FavoritesActivity)getActivity()).setToolbarVisibility(!selectionMode);
	}

	protected void openChangeGroupDialog(final FavoriteGroup group) {
		Builder bld = new AlertDialog.Builder(getActivity());
		View favEdit = getActivity().getLayoutInflater().inflate(R.layout.fav_group_edit, null);
        final TIntArrayList list = new TIntArrayList();
        final Spinner colorSpinner = (Spinner) favEdit.findViewById(R.id.ColorSpinner);
        final int intColor = group.color == 0? getResources().getColor(R.color.color_favorite) : group.color;
        ColorDialogs.setupColorSpinner(getActivity(), intColor, colorSpinner, list);
		
		final CheckBox checkBox = (CheckBox) favEdit.findViewById(R.id.Visibility);
		checkBox.setChecked(group.visible);
		bld.setTitle(R.string.edit_group);
		bld.setView(favEdit);
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int clr = list.get(colorSpinner.getSelectedItemPosition());
				if(clr != intColor || group.visible != checkBox.isChecked()) {
					getMyApplication().getFavorites().editFavouriteGroup(group, clr, checkBox.isChecked());
					favouritesAdapter.notifyDataSetInvalidated();
				}
				
			}
		});
		bld.show();
		
	}

	private void deleteFavoritesAction() {
		if (groupsToDelete.size() + favoritesSelected.size() > 0) {

			Builder b = new AlertDialog.Builder(getActivity());
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


	private void shareFavourites() {
		if (favouritesAdapter.isEmpty()) {
			AccessibleToast.makeText(getActivity(), R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
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
					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, "Favourites.gpx:\n\n\n"+GPXUtilities.asString(gpxFile, getMyApplication()));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_fav_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(helper.getExternalFile()));
//					sendIntent.setType("application/gpx+xml");
					sendIntent.setType("text/plain");
					startActivity(sendIntent);
				}
			};

			exportTask.execute();
		}
	}

	protected void export() {
		final File tosave = getMyApplication().getAppPath(FavouritesDbHelper.FILE_TO_SAVE);
		if (favouritesAdapter.isEmpty()) {
			AccessibleToast.makeText(getActivity(), R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
		} else if (!tosave.getParentFile().exists()) {
			AccessibleToast.makeText(getActivity(), R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
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
						AccessibleToast.makeText(
								getActivity(),
								MessageFormat.format(getString(R.string.fav_saved_sucessfully),
										tosave.getAbsolutePath()), Toast.LENGTH_LONG).show();
					} else {
						AccessibleToast.makeText(getActivity(), warning, Toast.LENGTH_LONG).show();
					}
				};
			};

			if (tosave.exists()) {
				Builder bld = new AlertDialog.Builder(getActivity());
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
	

	class FavouritesAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		Map<FavoriteGroup, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<FavoriteGroup, List<FavouritePoint>>();
		List<FavoriteGroup> groups = new ArrayList<FavoriteGroup>();
		Filter myFilter;
		private Set<?> filter;
		
		public void deleteFavoritePoint(FavouritePoint p) {
			if (favoriteGroups.containsKey(p.getCategory())) {
				favoriteGroups.get(p.getCategory()).remove(p);
			}
			notifyDataSetChanged();
		}

		public void deleteCategory(String p) {
			favoriteGroups.remove(p);
			groups.remove(p);
			notifyDataSetChanged();
		}

		public void synchronizeGroups() {
			favoriteGroups.clear();
			groups.clear();
			List<FavoriteGroup> gs = helper.getFavoriteGroups();
			Set<?> flt = filter;
			for (FavoriteGroup key : gs) {
				boolean empty = true;
				if (flt == null || flt.contains(key)) {
					empty = false;
					favoriteGroups.put(key, new ArrayList<FavouritePoint>(key.points));
				} else {
					ArrayList<FavouritePoint> list = new ArrayList<FavouritePoint>();
					for (FavouritePoint p : key.points) {
						if (flt.contains(p)) {
							list.add(p);
							empty = false;
						}
					}
					favoriteGroups.put(key, list);
				}
				if(!empty) {
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
			boolean checkBox = row != null && row.findViewById(R.id.check_item) instanceof CheckBox;
			boolean same = (selectionMode && checkBox) || (!selectionMode && !checkBox);
			if (row == null || !same) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.expandable_list_item_category, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row, getMyApplication().getSettings().isLightContent());
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final FavoriteGroup model = getGroup(groupPosition);
			label.setText(model.name.length() == 0? getString(R.string.shared_string_favorites) : model.name);

			if (selectionMode) {
				final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
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
				final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
				ch.setVisibility(View.GONE);
			}
			final View ch = row.findViewById(R.id.options);
			if(!selectionMode) {
				((ImageView) ch).setImageDrawable(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_overflow_menu_white));
				ch.setVisibility(View.VISIBLE);
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
			ImageView options = (ImageView) row.findViewById(R.id.options);
			options.setFocusable(false);
			options.setImageDrawable(getMyApplication().getIconsCache()
					.getContentIcon(R.drawable.ic_overflow_menu_white));
			options.setVisibility(View.VISIBLE);
			final FavouritePoint model = (FavouritePoint) getChild(groupPosition, childPosition);
			row.setTag(model);
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showItemPopupOptionsMenu(model, v);
				}
			});
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), model.getColor(), 0));
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

			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
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
							if (groupsToDelete.contains(model.getCategory())) {
								groupsToDelete.remove(model.getCategory());
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
				Set<Object> filter = new HashSet<Object>(); 
				String cs = constraint.toString().toLowerCase();
				for(FavoriteGroup g : helper.getFavoriteGroups()) {
					if(g.name.toLowerCase().indexOf(cs) != -1) {
						filter.add(g);
					} else {
						for(FavouritePoint fp : g.points) {
							if(fp.getName().toLowerCase().indexOf(cs) != -1) {
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
			if(constraint != null && constraint.length() > 1) {
				collapseTrees(5);
			}
		}
	}

	public void showItemPopupOptionsMenu(final FavouritePoint point, final View view) {
		final OsmandSettings settings = getMyApplication().getSettings();
		LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
		final PopupMenu optionsMenu = new PopupMenu(getActivity(), view);
		DirectionsDialogs.createDirectionActionsPopUpMenu(optionsMenu, location, point,
				new PointDescription(PointDescription.POINT_TYPE_FAVORITE, point.getName()),
				settings.getLastKnownMapZoom(),
				getActivity(), true, false);

		MenuItem item = optionsMenu.getMenu().add(R.string.favourites_context_menu_edit)
				.setIcon(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_edit_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				editPoint(getActivity(), point, new Runnable() {
					public void run() {
						favouritesAdapter.synchronizeGroups();
					}
				});
				return true;
			}
		});

		item = optionsMenu.getMenu().add(R.string.favourites_context_menu_delete)
				.setIcon(getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_delete_dark));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				deletePoint(point);

				return true;
			}
		});

		optionsMenu.show();
	}
}
