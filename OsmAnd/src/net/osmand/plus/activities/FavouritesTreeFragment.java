package net.osmand.plus.activities;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
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
	public static final int EXPORT_ID = 0;
	public static final int IMPORT_ID = 1;
	public static final int DELETE_ID = 2;
	public static final int DELETE_ACTION_ID = 3;
	public static final int SHARE_ID = 4;

	private FavouritesAdapter favouritesAdapter;
	private FavouritesDbHelper helper;

	private boolean selectionMode = false;
	private Set<FavouritePoint> favoritesToDelete = new LinkedHashSet<FavouritePoint>();
	private Set<String> groupsToDelete = new LinkedHashSet<String>();
	private Comparator<FavouritePoint> favoritesComparator;
	private ActionMode actionMode;
	private SearchView searchView;
	protected boolean hideActionBar;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		favoritesComparator = new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint object1, FavouritePoint object2) {
				return collator.compare(object1.getName(), object2.getName());
			}
		};

		helper = getMyApplication().getFavorites();
		favouritesAdapter = new FavouritesAdapter();
		favouritesAdapter.setFavoriteGroups(helper.getFavoriteGroups());
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
				favouritesAdapter.sort(favoritesComparator);
			}

			@Override
			protected void onProgressUpdate(Object... values) {
				for (Object o : values) {
					if (o instanceof FavouritePoint) {
						favouritesAdapter.deleteFavoritePoint((FavouritePoint) o);
					} else if (o instanceof String) {
						favouritesAdapter.deleteCategory((String) o);
					}
				}
			}

			@Override
			protected String doInBackground(Void... params) {
				for (FavouritePoint fp : favoritesToDelete) {
					helper.deleteFavourite(fp);
					publishProgress(fp);
				}
				favoritesToDelete.clear();
				for (String group : groupsToDelete) {
					helper.deleteGroup(group);
					publishProgress(group);
				}
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

		// Sort Favs by distance on Search tab, but sort alphabetically here
		favouritesAdapter.sort(favoritesComparator);

	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if (selectionMode) {
			CheckBox ch = (CheckBox) v.findViewById(R.id.check_item);
			FavouritePoint model = favouritesAdapter.getChild(groupPosition, childPosition);
			ch.setChecked(!ch.isChecked());
			if (ch.isChecked()) {
				favoritesToDelete.add(model);
			} else {
				favoritesToDelete.remove(model);
			}
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
		cat.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.list_textview, helper.getFavoriteGroups()
				.keySet().toArray(new String[] {})));
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean editied = helper.editFavouriteName(point, editText.getText().toString().trim(), cat.getText()
						.toString());
				if (editied) {
					favouritesAdapter.synchronizeGroups();
					favouritesAdapter.sort(favoritesComparator);
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
					favouritesAdapter.sort(favoritesComparator);
				}

			}
		});
		builder.create().show();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		if (item.getItemId() == EXPORT_ID) {
			export();
			return true;
		} else if (item.getItemId() == IMPORT_ID) {
			importFile();
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
//		mi.setOnActionExpandListener(new OnActionExpandListener() {
//			
//			@Override
//			public boolean onMenuItemActionExpand(MenuItem item) {
//				return true;
//			}
//			
//			@Override
//			public boolean onMenuItemActionCollapse(MenuItem item) {
//				return true;
//			}
//		});
		if (!mi.isActionViewExpanded()) {
			createMenuItem(menu, EXPORT_ID, R.string.export_fav, R.drawable.ic_action_gsave_light,
					R.drawable.ic_action_gsave_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			createMenuItem(menu, SHARE_ID, R.string.share_fav, R.drawable.ic_action_gshare_light,
					R.drawable.ic_action_gshare_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			createMenuItem(menu, IMPORT_ID, R.string.import_fav, R.drawable.ic_action_grefresh_light,
					R.drawable.ic_action_grefresh_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM
							| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			createMenuItem(menu, DELETE_ID, R.string.default_buttons_delete, R.drawable.ic_action_delete_light,
					R.drawable.ic_action_delete_dark, MenuItem.SHOW_AS_ACTION_IF_ROOM);
		}
	}

	public void showProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		getSherlockActivity().setSupportProgressBarIndeterminateVisibility(false);
	}

	private void enterDeleteMode() {
		actionMode = getSherlockActivity().startActionMode(new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				selectionMode = true;
				createMenuItem(menu, DELETE_ACTION_ID, R.string.default_buttons_delete,
						R.drawable.ic_action_delete_light, R.drawable.ic_action_delete_dark,
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
				favoritesToDelete.clear();
				groupsToDelete.clear();
				favouritesAdapter.notifyDataSetInvalidated();
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
					deleteFavoritesAction();
				}
				return true;
			}

		});

	}

	private void deleteFavoritesAction() {
		if (groupsToDelete.size() + favoritesToDelete.size() > 0) {

			Builder b = new AlertDialog.Builder(getActivity());
			b.setMessage(getString(R.string.favorite_delete_multiple, favoritesToDelete.size(), groupsToDelete.size()));
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
					Set<String> existedPoints = new LinkedHashSet<String>();
					if (!favouritesAdapter.isEmpty()) {
						for (FavouritePoint fp : helper.getFavouritePoints()) {
							existedPoints.add(fp.getName() + "_" + fp.getCategory());
						}
					}
					GPXFile res = GPXUtilities.loadGPXFile(getMyApplication(), tosave);
					if (res.warning != null) {
						return res.warning;
					}
					for (WptPt p : res.points) {
						if (existedPoints.contains(p.name) || existedPoints.contains(p.name + "_" + p.category)) {
							continue;
						}
						int c;
						String name = p.name;
						String categoryName = p.category != null ? p.category : "";
						if (name == null) {
							name = "";
						}
						// old way to store the category, in name.
						if ("".equals(categoryName.trim()) && (c = p.name.lastIndexOf('_')) != -1) {
							categoryName = p.name.substring(c + 1);
							name = p.name.substring(0, c);
						}
						FavouritePoint fp = new FavouritePoint(p.lat, p.lon, name, categoryName);
						fp.setColor(p.getColor(0));
						if (helper.addFavourite(fp)) {
							publishProgress(fp);
						}
					}
					return null;
				}

				@Override
				protected void onProgressUpdate(FavouritePoint... values) {
					for (FavouritePoint p : values) {
						favouritesAdapter.addFavoritePoint(p);
					}
				};

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
					favouritesAdapter.sort(favoritesComparator);
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

		Map<String, List<FavouritePoint>> sourceFavoriteGroups;
		Map<String, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<String, List<FavouritePoint>>();
		List<String> groups = new ArrayList<String>();
		Filter myFilter;

		public void setFavoriteGroups(Map<String, List<FavouritePoint>> favoriteGroups) {
			this.sourceFavoriteGroups = favoriteGroups;
			synchronizeGroups();
		}

		public void sort(Comparator<FavouritePoint> comparator) {
			for (List<FavouritePoint> ps : favoriteGroups.values()) {
				Collections.sort(ps, comparator);
			}
		}
		
		public void addFavoritePoint(FavouritePoint p) {

			if (!favoriteGroups.containsKey(p.getCategory())) {
				favoriteGroups.put(p.getCategory(), new ArrayList<FavouritePoint>());
				groups.add(p.getCategory());
			}
			favoriteGroups.get(p.getCategory()).add(p);
			notifyDataSetChanged();
		}

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
			for (String key : sourceFavoriteGroups.keySet()) {
				groups.add(key);
				favoriteGroups.put(key, new ArrayList<FavouritePoint>(sourceFavoriteGroups.get(key)));
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
		public String getGroup(int groupPosition) {
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
			if (row == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				row = inflater.inflate(R.layout.expandable_list_item_category, parent, false);
				fixBackgroundRepeat(row);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final String model = getGroup(groupPosition);
			label.setText(model);
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);

			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(groupsToDelete.contains(model));

				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							groupsToDelete.add(model);
							List<FavouritePoint> fvs = helper.getFavoriteGroups().get(model);
							if (fvs != null) {
								favoritesToDelete.addAll(fvs);
							}
							favouritesAdapter.notifyDataSetInvalidated();
						} else {
							groupsToDelete.remove(model);
						}
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
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = (FavouritePoint) getChild(groupPosition, childPosition);
			row.setTag(model);
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(getActivity(), 0));
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
				ch.setChecked(favoritesToDelete.contains(model));
				row.findViewById(R.id.favourite_icon).setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (ch.isChecked()) {
							favoritesToDelete.add(model);
						} else {
							favoritesToDelete.remove(model);
							if (groupsToDelete.contains(model.getCategory())) {
								groupsToDelete.remove(model.getCategory());
								favouritesAdapter.notifyDataSetInvalidated();
							}
						}
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
	}

	public class FavoritesFilter extends Filter {


		public FavoritesFilter() {
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = helper.getFavoriteGroups();
				results.count = 1;
			} else {
				TreeMap<String, List<FavouritePoint>> filter = new TreeMap<String, List<FavouritePoint>>(helper.getFavoriteGroups());
				TreeMap<String, List<FavouritePoint>> filterLists = new TreeMap<String, List<FavouritePoint>>();
				String cs = constraint.toString().toLowerCase();
				Iterator<Entry<String, List<FavouritePoint>>> ti = filter.entrySet().iterator();
				while(ti.hasNext()) {
					Entry<String, List<FavouritePoint>> next = ti.next();
					if(next.getKey().toLowerCase().indexOf(cs) == -1) {
						ti.remove();
						filterLists.put(next.getKey(), next.getValue());
					}
				}
				ti = filterLists.entrySet().iterator();
				while(ti.hasNext()) {
					Entry<String, List<FavouritePoint>> next = ti.next();
					final List<FavouritePoint> list = next.getValue();
					LinkedList<FavouritePoint> ll = new LinkedList<FavouritePoint>();
					for(FavouritePoint l : list) {
						if(l.getName().toLowerCase().indexOf(cs) != -1) {
							ll.add(l);
						}
							
					}
					if(ll.size() > 0) {
						filter.put(next.getKey(), ll);
					}
				}
				results.values = filter;
				results.count = filter.size();
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			synchronized (favouritesAdapter) {
				favouritesAdapter.setFavoriteGroups((Map<String, List<FavouritePoint>>) results.values);
			}
			favouritesAdapter.notifyDataSetChanged();
			if(constraint != null && constraint.length() > 1) {
				collapseTrees(5);
			}
		}
	}


}
