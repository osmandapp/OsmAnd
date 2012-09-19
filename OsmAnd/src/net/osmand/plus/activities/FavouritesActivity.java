/**
 * 
 */
package net.osmand.plus.activities;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.FavouritePoint;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 */
public class FavouritesActivity extends OsmandExpandableListActivity {

	public static final int SHOW_ON_MAP = 0;
	public static final int NAVIGATE_TO = 1;
	public static final int DELETE_ITEM = 2;
	public static final int EDIT_ITEM = 3;
	
	public static final int EXPORT_ID = 0;
	public static final int IMPORT_ID = 1;
	public static final int DELETE_ID = 2;
	

	private FavouritesAdapter favouritesAdapter;
	private FavouritesDbHelper helper;
	
	private boolean selectionMode = false;
	private Set<FavouritePoint> favoritesToDelete = new LinkedHashSet<FavouritePoint>();
	private Set<String> groupsToDelete = new LinkedHashSet<String>();
	private Comparator<FavouritePoint> favoritesComparator;
	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		final Collator collator = Collator.getInstance();
		collator.setStrength(Collator.SECONDARY);
		favoritesComparator = new Comparator<FavouritePoint>(){

			@Override
			public int compare(FavouritePoint object1, FavouritePoint object2) {
				return collator.compare(object1.getName(), object2.getName());
			}
		};
		CustomTitleBar titleBar = new CustomTitleBar(this, R.string.favourites_activity, R.drawable.tab_favorites_screen_icon);
		setContentView(R.layout.favourites_list);
		titleBar.afterSetContentView();
		
		
		helper = getMyApplication().getFavorites();
		favouritesAdapter = new FavouritesAdapter();
		favouritesAdapter.setFavoriteGroups(helper.getFavoriteGroups());
		getExpandableListView().setAdapter(favouritesAdapter);

		findViewById(R.id.CancelButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelSelectingMode();
			}

			
		});
		
		findViewById(R.id.ActionButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(groupsToDelete.size() + favoritesToDelete.size() > 0){
					
					Builder b = new AlertDialog.Builder(FavouritesActivity.this);
					b.setMessage(getString(R.string.favorite_delete_multiple, favoritesToDelete.size(), 
							groupsToDelete.size()));
					b.setPositiveButton(R.string.default_buttons_delete, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							cancelSelectingMode();
							deleteFavorites();
						}
					});
					b.setNegativeButton(R.string.default_buttons_cancel, null);
					b.show();
				}
				
			}
		});
	}

	private void cancelSelectingMode() {
		selectionMode = false;
		findViewById(R.id.ActionButton).setVisibility(View.GONE);
		findViewById(R.id.CancelButton).setVisibility(View.GONE);
		findViewById(R.id.LoadingPanel).setVisibility(View.GONE);
		favouritesAdapter.notifyDataSetInvalidated();
	}
	
	private void deleteFavorites() {
		new AsyncTask<Void, Object, String>(){

			@Override
			protected void onPreExecute() {
				showProgressBar();
			};
			@Override
			protected void onPostExecute(String result) {
				hideProgressBar();
				favouritesAdapter.synchronizeGroups();
				favouritesAdapter.sort(favoritesComparator);
			};
			
			@Override
			protected void onProgressUpdate(Object... values) {
				for(Object o : values){
					if(o instanceof FavouritePoint){
						favouritesAdapter.deleteFavoritePoint((FavouritePoint) o);
					} else if(o instanceof String){
						favouritesAdapter.deleteCategory((String) o);
					}
				}
			};
			
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
	protected void onResume() {
		super.onResume();
//		final LatLon mapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
		favouritesAdapter.synchronizeGroups();
		
//		Sort Favs by distance on Search tab, but sort alphabetically here
		favouritesAdapter.sort(favoritesComparator);
		
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		if(selectionMode){
			CheckBox ch = (CheckBox) v.findViewById(R.id.check_item);
			FavouritePoint model = favouritesAdapter.getChild(groupPosition, childPosition);
			ch.setChecked(!ch.isChecked());
			if(ch.isChecked()){
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
			MapActivityActions.createDirectionsActions(qa, location, point, name, settings.getLastKnownMapZoom(), this, 
					true, onshow);
			if (point.isStored()) {
				ActionItem edit = new ActionItem();
				edit.setIcon(getResources().getDrawable(R.drawable.list_activities_fav_edit));
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
				delete.setIcon(getResources().getDrawable(R.drawable.list_activities_fav_delete));
				delete.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						deletePoint(point);
						qa.dismiss();
					}
				});
				qa.addActionItem(delete);
			}
			
			qa.show();
		}
		return true;
	}
	

	private boolean editPoint(final FavouritePoint point) {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.favourites_context_menu_edit);
		final View v = getLayoutInflater().inflate(R.layout.favourite_edit_dialog, getExpandableListView(), false);
		final AutoCompleteTextView cat =  (AutoCompleteTextView) v.findViewById(R.id.Category);
		final EditText editText =  (EditText) v.findViewById(R.id.Name);
		builder.setView(v);
		editText.setText(point.getName());
		cat.setText(point.getCategory());
		cat.setThreshold(1);
		cat.setAdapter(new ArrayAdapter<String>(this, R.layout.list_textview, helper.getFavoriteGroups().keySet().toArray(new String[] {})));
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean editied = helper.editFavouriteName(point, editText.getText().toString(), cat.getText().toString());
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
		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.favourites_remove_dialog_msg, point.getName()));
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				boolean deleted = helper.deleteFavourite(point);
				if (deleted) {
					AccessibleToast.makeText(FavouritesActivity.this,
							MessageFormat.format(resources.getString(R.string.favourites_remove_dialog_success), point.getName()),
							Toast.LENGTH_SHORT).show();
					favouritesAdapter.synchronizeGroups();
					favouritesAdapter.sort(favoritesComparator);
				}

			}
		});
		builder.create().show();
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, EXPORT_ID, 0, R.string.export_fav);
		item.setIcon(android.R.drawable.ic_menu_save);
		item = menu.add(0, IMPORT_ID, 0, R.string.import_fav);
		item.setIcon(android.R.drawable.ic_menu_upload);
		item = menu.add(0, DELETE_ID, 0, getString(R.string.default_buttons_delete)+"...");
		item.setIcon(android.R.drawable.ic_menu_delete);
		return true;
	}
	
	public void showProgressBar(){
		findViewById(R.id.LoadingPanel).setVisibility(View.VISIBLE);
		findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
	}
	
	public void hideProgressBar(){
		findViewById(R.id.LoadingPanel).setVisibility(View.GONE);
		findViewById(R.id.ProgressBar).setVisibility(View.GONE);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final File appDir = getMyApplication().getSettings().extendOsmandPath(ResourceManager.APP_DIR);
		final File tosave = new File(appDir, FavouritesDbHelper.FILE_TO_SAVE);
		if(item.getItemId() == EXPORT_ID){
			if(favouritesAdapter.isEmpty()){
				AccessibleToast.makeText(this, R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
			} else if(!appDir.exists()){
				AccessibleToast.makeText(this, R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
			} else {
				final AsyncTask<Void, Void, String> exportTask = new AsyncTask<Void, Void, String>(){
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
						if(warning == null){
							AccessibleToast.makeText(
									FavouritesActivity.this,
									MessageFormat.format(getString(R.string.fav_saved_sucessfully), tosave.getAbsolutePath()), Toast.LENGTH_LONG).show();
						} else {
							AccessibleToast.makeText(FavouritesActivity.this, warning, Toast.LENGTH_LONG).show();
						}
					};
				};
				
				if(tosave.exists()) {
					Builder bld = new AlertDialog.Builder(this);
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
		} else if(item.getItemId() == IMPORT_ID){
			if(!tosave.exists()){
				AccessibleToast.makeText(this, MessageFormat.format(getString(R.string.fav_file_to_load_not_found), tosave.getAbsolutePath()), Toast.LENGTH_LONG).show();
			} else {
				new AsyncTask<Void, FavouritePoint, String>(){
					@Override
					protected String doInBackground(Void... params) {
						Set<String> existedPoints = new LinkedHashSet<String>();
						if (!favouritesAdapter.isEmpty()) {
							for (FavouritePoint fp : helper.getFavouritePoints()) {
								existedPoints.add(fp.getName() + "_" + fp.getCategory());
							}
						}
						GPXFile res = GPXUtilities.loadGPXFile(FavouritesActivity.this, tosave, false);
						if(res.warning != null){
							return res.warning;
						}
						for(WptPt p : res.points){
							if(existedPoints.contains(p.name) || existedPoints.contains(p.name + "_" + p.category)){
								continue;
							}
							int c;
							String name = p.name;
							String categoryName = p.category != null ? p.category : "";
							if(name == null){
								name = "";
							}
							//old way to store the category, in name.
							if("".equals(categoryName.trim()) && (c = p.name.lastIndexOf('_')) != -1) {
								categoryName = p.name.substring(c + 1);
								name = p.name.substring(0, c);
							}
							FavouritePoint fp = new FavouritePoint(p.lat, p.lon, name, categoryName);
							if (helper.addFavourite(fp)) {
								publishProgress(fp);
							}
						}
						return null;
					}
					
					@Override
					protected void onProgressUpdate(FavouritePoint... values) {
						for(FavouritePoint p : values){
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
						if(warning == null){
							AccessibleToast.makeText(FavouritesActivity.this, R.string.fav_imported_sucessfully, Toast.LENGTH_SHORT).show();
						} else {
							AccessibleToast.makeText(FavouritesActivity.this, warning, Toast.LENGTH_LONG).show();
						}
						favouritesAdapter.synchronizeGroups();
						favouritesAdapter.sort(favoritesComparator);
					};
					
				}.execute();
			}
		} else if(item.getItemId() == DELETE_ID){
			selectionMode = true;
			findViewById(R.id.ActionButton).setVisibility(View.VISIBLE);
			findViewById(R.id.CancelButton).setVisibility(View.VISIBLE);
			findViewById(R.id.LoadingPanel).setVisibility(View.VISIBLE);
			favoritesToDelete.clear();
			groupsToDelete.clear();
			favouritesAdapter.notifyDataSetInvalidated();
		} else {
			return false;
		}
		return true;
	}
	

	class FavouritesAdapter extends OsmandBaseExpandableListAdapter {

		Map<String, List<FavouritePoint>> sourceFavoriteGroups;
		Map<String, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<String, List<FavouritePoint>>();
		List<String> groups = new ArrayList<String>();
		
		public void setFavoriteGroups(Map<String, List<FavouritePoint>> favoriteGroups) {
			this.sourceFavoriteGroups = favoriteGroups;
			synchronizeGroups();
		}
		
		public void sort(Comparator<FavouritePoint> comparator) {
			for(List<FavouritePoint> ps : favoriteGroups.values()) {
				Collections.sort(ps, comparator);
			}
		}

		public void addFavoritePoint(FavouritePoint p) {
			if(!favoriteGroups.containsKey(p.getCategory())){
				favoriteGroups.put(p.getCategory(), new ArrayList<FavouritePoint>());
				groups.add(p.getCategory());
			}
			favoriteGroups.get(p.getCategory()).add(p);
			notifyDataSetChanged();
		}
		
		public void deleteFavoritePoint(FavouritePoint p) {
			if(favoriteGroups.containsKey(p.getCategory())){
				favoriteGroups.get(p.getCategory()).remove(p);
			}
			notifyDataSetChanged();
		}
		
		public void deleteCategory(String p) {
			favoriteGroups.remove(p);
			groups.remove(p);
			notifyDataSetChanged();
		}

		public void synchronizeGroups(){
			favoriteGroups.clear();
			groups.clear();
			for(String key : sourceFavoriteGroups.keySet()){
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
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_category, parent, false);
			}
			adjustIndicator(groupPosition, isExpanded, row);
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final String model = getGroup(groupPosition);
			label.setText(model);
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			
			if(selectionMode){
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
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}
			
			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = (FavouritePoint) getChild(groupPosition, childPosition);
			row.setTag(model);
			if(model.isStored()){
				icon.setImageResource(R.drawable.favorites);
			} else {
				icon.setImageResource(R.drawable.opened_poi);
			}
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), 
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, FavouritesActivity.this) + "  ";
			label.setText(distance + model.getName(), TextView.BufferType.SPANNABLE);
			((Spannable) label.getText()).setSpan(new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length() - 1, 0);
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			if(selectionMode && model.isStored()){
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(favoritesToDelete.contains(model));
				row.findViewById(R.id.favourite_icon).setVisibility(View.GONE);
				ch.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if(ch.isChecked()){
							favoritesToDelete.add(model);
						} else {
							favoritesToDelete.remove(model);
							if(groupsToDelete.contains(model.getCategory())){
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
	}

}
