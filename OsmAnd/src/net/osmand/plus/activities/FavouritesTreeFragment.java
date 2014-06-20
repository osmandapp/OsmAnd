package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;

public class FavouritesTreeFragment extends OsmandExpandableListFragment {

	public static final int SEARCH_ID = -1;
//	public static final int EXPORT_ID = 0;
	// public static final int IMPORT_ID = 1;
	public static final int DELETE_ID = 2;
	public static final int DELETE_ACTION_ID = 3;
	public static final int SHARE_ID = 4;
	public static final int SELECT_DESTINATIONS_ID = 5;
	public static final int SELECT_DESTINATIONS_ACTION_MODE_ID = 6;

	private FavouritesAdapter favouritesAdapter;
	private FavouritesDbHelper helper;

	private boolean selectionMode = false;
	private Set<FavouritePoint> favoritesSelected = new LinkedHashSet<FavouritePoint>();
	private Set<FavoriteGroup> groupsToDelete = new LinkedHashSet<FavoriteGroup>();
	private ActionMode actionMode;
	private SearchView searchView;
	protected boolean hideActionBar;
	private int defColor;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		helper = getMyApplication().getFavorites();
		favouritesAdapter = new FavouritesAdapter();
		defColor = getResources().getColor(R.color.color_favorite);
		favouritesAdapter.synchronizeGroups();
		setAdapter(favouritesAdapter);
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
	public void onResume() {
		super.onResume();
		// final LatLon mapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
		favouritesAdapter.synchronizeGroups();

		if(favouritesAdapter.getGroupCount() > 0 && 
				"".equals(favouritesAdapter.getGroup(0))) {
			getListView().expandGroup(0);
		}

	}
	
	private void updateSelectionMode(ActionMode m) {
		if(favoritesSelected.size() > 0) {
			m.setTitle(favoritesSelected.size() + " " + getMyApplication().getString(R.string.selected));
		} else{
			m.setTitle("");
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
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
			final QuickAction qa = new QuickAction(v);
			final OsmandSettings settings = getMyApplication().getSettings();
			final FavouritePoint point = (FavouritePoint) favouritesAdapter.getChild(groupPosition, childPosition);
			String name = getString(R.string.favorite) + ": " + point.getName();
			LatLon location = new LatLon(point.getLatitude(), point.getLongitude());
			OnClickListener onshow = new OnClickListener() {
				@Override
				public void onClick(View v) {
					settings.SHOW_FAVORITES.set(true);
				}
			};
			MapActivityActions.createDirectionsActions(qa, location, point, name, settings.getLastKnownMapZoom(),
					getActivity(), true, onshow, false);
			ActionItem edit = new ActionItem();
			edit.setIcon(getResources().getDrawable(R.drawable.ic_action_edit_light));
			edit.setTitle(getString(R.string.favourites_context_menu_edit));
			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					editPoint(point);
					qa.dismiss();
				}
			});
			qa.addActionItem(edit);

			ActionItem delete = new ActionItem();
			delete.setTitle(getString(R.string.favourites_context_menu_delete));
			delete.setIcon(getResources().getDrawable(R.drawable.ic_action_delete_light));
			delete.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deletePoint(point);
					qa.dismiss();
				}
			});
			qa.addActionItem(delete);

			qa.show();
		}
		return true;
	}

	private boolean editPoint(final FavouritePoint point) {
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.favourites_context_menu_edit);
		final View v = getActivity().getLayoutInflater().inflate(R.layout.favourite_edit_dialog,
				getExpandableListView(), false);
		final AutoCompleteTextView cat = (AutoCompleteTextView) v.findViewById(R.id.Category);
		final EditText editText = (EditText) v.findViewById(R.id.Name);
		builder.setView(v);
		editText.setText(point.getName());
		cat.setText(point.getCategory());
		cat.setThreshold(1);
		List<FavoriteGroup> gs = helper.getFavoriteGroups();
		String[] list = new String[gs.size()];
		for(int i = 0; i < list.length; i++) {
			list[i] =gs.get(i).name;
		}
		cat.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.list_textview, list));
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean edited = helper.editFavouriteName(point, editText.getText().toString().trim(), cat.getText()
						.toString());
				if (edited) {
					favouritesAdapter.synchronizeGroups();
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
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
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
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
//		if (item.getItemId() == EXPORT_ID) {
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
		MenuItem mi = createMenuItem(menu, SEARCH_ID, R.string.export_fav, R.drawable.ic_action_search_light,
				R.drawable.ic_action_search_dark, MenuItem.SHOW_AS_ACTION_ALWAYS
						| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		searchView = new com.actionbarsherlock.widget.SearchView(getActivity());
		mi.setActionView(searchView);
		searchView.setOnQueryTextListener(new OnQueryTextListener() {


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
		mi.setOnActionExpandListener(new OnActionExpandListener() {
			
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}
			
			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				favouritesAdapter.setFilterResults(null);
				favouritesAdapter.synchronizeGroups();
				favouritesAdapter.notifyDataSetChanged();
				return true;
			}
		});
		if (!mi.isActionViewExpanded()) {
			createMenuItem(menu, SHARE_ID, R.string.share_fav, R.drawable.ic_action_gshare_light,
					R.drawable.ic_action_gshare_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			createMenuItem(menu, SELECT_DESTINATIONS_ID, R.string.select_destination_and_intermediate_points, R.drawable.ic_action_flage_light,
					R.drawable.ic_action_flage_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			createMenuItem(menu, DELETE_ID, R.string.default_buttons_delete, R.drawable.ic_action_delete_light,
					R.drawable.ic_action_delete_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
//			createMenuItem(menu, EXPORT_ID, R.string.export_fav, R.drawable.ic_action_gsave_light,
//					R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			
		}
	}

	public void showProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
	}
	
	private void enterIntermediatesMode() {
		actionMode = getSherlockActivity().startActionMode(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				createMenuItem(menu, SELECT_DESTINATIONS_ACTION_MODE_ID, R.string.select_destination_and_intermediate_points,
						R.drawable.ic_action_flage_light, R.drawable.ic_action_flage_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
				selectionMode = false;
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
						getString(R.string.favorite) + ": " + fp.getName());		
			}
			if(getMyApplication().getRoutingHelper().isRouteCalculated()) {
				targetPointsHelper.updateRouteAndReferesh(true);
			}
			IntermediatePointsDialog.openIntermediatePointsDialog(getActivity(), getMyApplication(), false);
			//MapActivity.launchMapActivityMoveToTop(getActivity());
		}
	}

	private void enterDeleteMode() {
		actionMode = getSherlockActivity().startActionMode(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				createMenuItem(menu, DELETE_ACTION_ID, R.string.default_buttons_delete,
						R.drawable.ic_action_delete_light, R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
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
				selectionMode = false;
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
	
	protected void openChangeGroupDialog() {
		// TODO Auto-generated method stub
		
	}

	private void deleteFavoritesAction() {
		if (groupsToDelete.size() + favoritesSelected.size() > 0) {

			Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(getString(R.string.favorite_delete_multiple, favoritesSelected.size(), groupsToDelete.size()));
			b.setPositiveButton(R.string.default_buttons_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (actionMode != null) {
						actionMode.finish();
					}
					deleteFavorites();
				}
			});
			b.setNegativeButton(R.string.default_buttons_cancel, null);
			b.show();
		}
	}

	private void importFile() {
		final File tosave = getMyApplication().getAppPath(FavouritesDbHelper.FILE_TO_SAVE);
		if (!tosave.exists()) {
			AccessibleToast.makeText(getActivity(),
					MessageFormat.format(getString(R.string.fav_file_to_load_not_found), tosave.getAbsolutePath()),
					Toast.LENGTH_LONG).show();
		} else {
			new AsyncTask<Void, FavouritePoint, String>() {
						
				@Override
				protected String doInBackground(Void... params) {
					// helper.loadGPXFile(getMyApplication(), tosave, this);
					return null ;
				}

				@Override
				protected void onPreExecute() {
					showProgressBar();
				};

				@Override
				protected void onPostExecute(String warning) {
					hideProgressBar();
					if (warning == null) {
						AccessibleToast.makeText(getActivity(), R.string.fav_imported_sucessfully, Toast.LENGTH_SHORT)
								.show();
					} else {
						AccessibleToast.makeText(getActivity(), warning, Toast.LENGTH_LONG).show();
					}
					favouritesAdapter.synchronizeGroups();
				};

			}.execute();
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
					sendIntent.putExtra(Intent.EXTRA_TEXT, GPXUtilities.asString(gpxFile, getMyApplication()));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_fav_subject));
					sendIntent.setType("application/gpx+xml");
					startActivity(sendIntent);
				}
			};

			exportTask.execute();
		}
	}

	private void export() {
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
				};

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
				bld.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						exportTask.execute();
					}
				});
				bld.setNegativeButton(R.string.default_buttons_no, null);
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
				row = inflater.inflate(
						selectionMode ? R.layout.expandable_list_item_category :
							R.layout.expandable_list_item_category_btn, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final FavoriteGroup model = getGroup(groupPosition);
			label.setText(model.name.length() == 0? getString(R.string.favourites_activity) : model.name);

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
				final ImageView ch = (ImageView) row.findViewById(R.id.check_item);
				ch.setVisibility(View.VISIBLE);
				ch.setImageDrawable(getActivity().getResources().getDrawable(
						getMyApplication().getSettings().isLightContent() ? R.drawable.ic_action_settings_light
								: R.drawable.ic_action_settings_dark));
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						openChangeGroupDialog();
					}

				});
			}
			return row;
		}


		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = (FavouritePoint) getChild(groupPosition, childPosition);
			row.setTag(model);
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), model.getColor()));
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			label.setText(distance + model.getName(), TextView.BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(
					new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length() - 1,
					0);
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


}
