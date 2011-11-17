/**
 * 
 */
package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.FavouritePoint;
import net.osmand.GPXUtilities;
import net.osmand.OsmAndFormatter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

/**
 * 
 */
public class FavouritesActivity extends ExpandableListActivity {

	public static final int SHOW_ON_MAP = 0;
	public static final int NAVIGATE_TO = 1;
	public static final int DELETE_ITEM = 2;
	public static final int EDIT_ITEM = 3;
	
	public static final int EXPORT_ID = 0;
	public static final int IMPORT_ID = 1;
	public static final int DELETE_ID = 2;
	
	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	

	private FavouritesAdapter favouritesAdapter;
	private FavouritesDbHelper helper;
	
	private boolean selectionMode = false;
	private Set<FavouritePoint> favoritesToDelete = new LinkedHashSet<FavouritePoint>();
	private Set<String> groupsToDelete = new LinkedHashSet<String>();
	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.favourites_list);

		helper = ((OsmandApplication)getApplication()).getFavorites();
		favouritesAdapter = new FavouritesAdapter();
		favouritesAdapter.setFavoriteGroups(helper.getFavoriteGroups());
		getExpandableListView().setAdapter(favouritesAdapter);
		
		/* Add Context-Menu listener to the ListView. */
		getExpandableListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener(){

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				int child = ExpandableListView.getPackedPositionChild(((ExpandableListContextMenuInfo)menuInfo).packedPosition);
				int group = ExpandableListView.getPackedPositionGroup(((ExpandableListContextMenuInfo)menuInfo).packedPosition);
				if (child == -1) {
					return;
				}
				// menu.setHeaderTitle(R.string.favourites_context_menu_title);
				
				menu.add(0, SHOW_ON_MAP, 0, R.string.show_poi_on_map);
				menu.add(0, NAVIGATE_TO, 1, R.string.favourites_context_menu_navigate);
				
				final FavouritePoint point = (FavouritePoint) favouritesAdapter.getChild(group, child);
				if(point.isStored()){
					menu.add(0, EDIT_ITEM, 2, R.string.favourites_context_menu_edit);
					menu.add(0, DELETE_ITEM, 3, R.string.favourites_context_menu_delete);
				}
			}
        	
        });
		
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
		favouritesAdapter.synchronizeGroups();
		final LatLon mapLocation = OsmandSettings.getOsmandSettings(this).getLastKnownMapLocation();
		
		if(mapLocation != null){
			// TODO sort
//			favouritesAdapter.sort(new Comparator<FavouritePoint>(){
//
//				@Override
//				public int compare(FavouritePoint object1, FavouritePoint object2) {
//					double d1 = MapUtils.getDistance(mapLocation, object1.getLatitude(), object1.getLongitude());
//					double d2 = MapUtils.getDistance(mapLocation, object2.getLatitude(), object2.getLongitude());
//					if(d1 == d2){
//						return 0;
//					} else if(d1 > d2){
//						return 1;
//					}
//					return -1;
//				}
//				
//			});
		}
		
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
			FavouritePoint point = (FavouritePoint) favouritesAdapter.getChild(groupPosition, childPosition);
			OsmandSettings settings = OsmandSettings.getOsmandSettings(this);
			settings.SHOW_FAVORITES.set(true);
			settings.setMapLocationToShow(point.getLatitude(), point.getLongitude(), 
					Math.max(12, settings.getLastKnownMapZoom()), null, getString(R.string.favorite)+" : " + point.getName());
			MapActivity.launchMapActivityMoveToTop(FavouritesActivity.this);
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		ContextMenuInfo menuInfo = aItem.getMenuInfo();
		int child = ExpandableListView.getPackedPositionChild(((ExpandableListContextMenuInfo)menuInfo).packedPosition);
		int group = ExpandableListView.getPackedPositionGroup(((ExpandableListContextMenuInfo)menuInfo).packedPosition);
		final FavouritePoint point = (FavouritePoint) favouritesAdapter.getChild(group, child);
		if (aItem.getItemId() == SHOW_ON_MAP) {
			OsmandSettings settings = OsmandSettings.getOsmandSettings(this);
			settings.SHOW_FAVORITES.set(true);
			settings.setMapLocationToShow(point.getLatitude(), point.getLongitude(), 
					Math.max(12, settings.getLastKnownMapZoom()), null, getString(R.string.favorite)+" : " + point.getName());
			MapActivity.launchMapActivityMoveToTop(this);
		} else if (aItem.getItemId() == NAVIGATE_TO) {
			OsmandSettings.getOsmandSettings(this).setPointToNavigate(point.getLatitude(), point.getLongitude(), getString(R.string.favorite)+" : " + point.getName());
			MapActivity.launchMapActivityMoveToTop(this);
		} else if (aItem.getItemId() == EDIT_ITEM) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.favourites_edit_dialog_title);
			final View v = getLayoutInflater().inflate(R.layout.favourite_edit_dialog, getExpandableListView(), false);
			final AutoCompleteTextView cat =  (AutoCompleteTextView) v.findViewById(R.id.Category);
			final EditText editText =  (EditText) v.findViewById(R.id.Name);
			builder.setView(v);
			editText.setText(point.getName());
			cat.setText(point.getCategory());
			cat.setThreshold(1);
			cat.setAdapter(new ArrayAdapter(this, R.layout.list_textview, helper.getFavoriteGroups().keySet().toArray()));
			builder.setNegativeButton(R.string.default_buttons_cancel, null);
			builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean editied = helper.editFavouriteName(point, editText.getText().toString(), cat.getText().toString());
					if (editied) {
						favouritesAdapter.synchronizeGroups();
					}

				}
			});
			builder.create().show();
			return true;
		}
		if (aItem.getItemId() == DELETE_ITEM) {
			final Resources resources = this.getResources();
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.favourites_remove_dialog_title);
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean deleted = helper.deleteFavourite(point);
					if (deleted) {
						Toast.makeText(FavouritesActivity.this,
								MessageFormat.format(resources.getString(R.string.favourites_remove_dialog_success), point.getName()),
								Toast.LENGTH_SHORT).show();
						favouritesAdapter.synchronizeGroups();
					}

				}
			});
			builder.create().show();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem item = menu.add(0, EXPORT_ID, 0, R.string.export_fav);
		item.setIcon(android.R.drawable.ic_menu_save);
		item = menu.add(0, IMPORT_ID, 0, R.string.import_fav);
		item.setIcon(android.R.drawable.ic_menu_upload);
		item = menu.add(0, DELETE_ID, 0, R.string.local_index_mi_delete);
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
		if(item.getItemId() == EXPORT_ID){
			final File appDir = OsmandSettings.getOsmandSettings(this).extendOsmandPath(ResourceManager.APP_DIR);
			if(favouritesAdapter.isEmpty()){
				Toast.makeText(this, R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
			} else if(!appDir.exists()){
				Toast.makeText(this, R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
			} else {
				final File f = new File(appDir, FILE_TO_SAVE);
				new AsyncTask<Void, Void, String>(){
					@Override
					protected String doInBackground(Void... params) {
						GPXFile gpx = new GPXFile();
						
						for (FavouritePoint p : helper.getFavouritePoints()) {
							if (p.isStored()) {
								WptPt pt = new WptPt();
								pt.lat = p.getLatitude();
								pt.lon = p.getLongitude();
								pt.name = p.getName() + "_" + p.getCategory();
								gpx.points.add(pt);
							}
						}
						return GPXUtilities.writeGpxFile(f, gpx, FavouritesActivity.this);
					}
					
					@Override
					protected void onPreExecute() {
						showProgressBar();
					};
					
					@Override
					protected void onPostExecute(String warning) {
						hideProgressBar();
						if(warning == null){
							Toast.makeText(FavouritesActivity.this, MessageFormat.format(getString(R.string.fav_saved_sucessfully), f.getAbsolutePath()), 
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(FavouritesActivity.this, warning, Toast.LENGTH_LONG).show();
						}
					};
					
				}.execute();
			}
		} else if(item.getItemId() == IMPORT_ID){
			File appDir = OsmandSettings.getOsmandSettings(this).extendOsmandPath(ResourceManager.APP_DIR);
			final File f = new File(appDir, FILE_TO_SAVE);
			if(!f.exists()){
				Toast.makeText(this, MessageFormat.format(getString(R.string.fav_file_to_load_not_found), f.getAbsolutePath()), Toast.LENGTH_LONG).show();
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
						GPXFile res = GPXUtilities.loadGPXFile(FavouritesActivity.this, f, false);
						if(res.warning != null){
							return res.warning;
						}
						for(WptPt p : res.points){
							if(!existedPoints.contains(p.name)){
								String categoryName = FavouritesActivity.this.getString(R.string.favorite_default_category);
								int c;
								String name = p.name;
								if(name == null){
									name = "";
								}
								if((c = p.name.lastIndexOf('_')) != -1){
									categoryName = p.name.substring(c + 1);
									name = p.name.substring(0, c);
								}
								FavouritePoint fp = new FavouritePoint(p.lat, p.lon, name, categoryName);
								if (helper.addFavourite(fp)) {
									publishProgress(fp);
								}
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
							Toast.makeText(FavouritesActivity.this, R.string.fav_imported_sucessfully, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(FavouritesActivity.this, warning, Toast.LENGTH_LONG).show();
						}
						favouritesAdapter.synchronizeGroups();
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
	

	class FavouritesAdapter extends BaseExpandableListAdapter {

		Map<String, List<FavouritePoint>> sourceFavoriteGroups;
		Map<String, List<FavouritePoint>> favoriteGroups = new LinkedHashMap<String, List<FavouritePoint>>();
		List<String> groups = new ArrayList<String>();
		
		public void setFavoriteGroups(Map<String, List<FavouritePoint>> favoriteGroups) {
			this.sourceFavoriteGroups = favoriteGroups;
			synchronizeGroups();
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
			TextView label = (TextView) row.findViewById(R.id.category_name);
			final String model = getGroup(groupPosition);
			List<FavouritePoint> ms = helper.getFavoriteGroups().get(model);
			int sz = ms != null ? ms.size() : 0;
			label.setText(model + " [" + sz +"]");
			final CheckBox ch = (CheckBox) row.findViewById(R.id.check_item);
			
			if(selectionMode){
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(groupsToDelete.contains(model));
				
				ch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(ch.isChecked()){
							groupsToDelete.add(model);
							favoritesToDelete.addAll(helper.getFavoriteGroups().get(model));
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
			TextView distanceLabel = (TextView) row.findViewById(R.id.favouritedistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			final FavouritePoint model = (FavouritePoint) getChild(groupPosition, childPosition);
			row.setTag(model);
			if(model.isStored()){
				icon.setImageResource(R.drawable.favorites);
			} else {
				icon.setImageResource(R.drawable.opened_poi);
			}
			LatLon lastKnownMapLocation = OsmandSettings.getOsmandSettings(FavouritesActivity.this).getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), 
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, FavouritesActivity.this));
			label.setText(model.getName());
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
