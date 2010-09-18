/**
 * 
 */
package net.osmand.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.GPXUtilities;
import net.osmand.OsmandSettings;
import net.osmand.R;
import net.osmand.ResourceManager;
import net.osmand.GPXUtilities.GPXFileResult;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * 
 */
public class FavouritesActivity extends ListActivity {

	public static final int NAVIGATE_TO = 0;
	public static final int DELETE_ITEM = 1;
	public static final int EDIT_ITEM = 2;
	
	public static final int EXPORT_ID = 0;
	public static final int IMPORT_ID = 1;
	
	public static final String FILE_TO_SAVE = "favourites.gpx"; //$NON-NLS-1$
	

	private List<FavouritePoint> favouritesList;
	private FavouritesDbHelper helper;
	private FavouritesAdapter favouritesAdapter;
	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		setContentView(lv);

		helper = new FavouritesDbHelper(this);
		favouritesList = helper.getFavouritePoints();
		
		favouritesAdapter = new FavouritesAdapter(favouritesList);
		lv.setAdapter(favouritesAdapter);
		/* Add Context-Menu listener to the ListView. */
        lv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener(){

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(R.string.favourites_context_menu_title);
				menu.add(0, NAVIGATE_TO, 0, R.string.favourites_context_menu_navigate);
				menu.add(0, EDIT_ITEM, 1, R.string.favourites_context_menu_edit);
				menu.add(0, DELETE_ITEM, 2, R.string.favourites_context_menu_delete);
			}
        	
        });
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		FavouritePoint point = favouritesList.get(position);
		OsmandSettings.setShowingFavorites(this, true);
		OsmandSettings.setMapLocationToShow(this, point.getLatitude(), point.getLongitude(), getString(R.string.favorite)+" : " + point.getName()); //$NON-NLS-1$
		Intent newIntent = new Intent(FavouritesActivity.this, MapActivity.class);
		startActivity(newIntent);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
		final FavouritePoint point = (FavouritePoint) favouritesList.get(menuInfo.position);
		if (aItem.getItemId() == NAVIGATE_TO) {
			//OsmandSettings.setMapLocationToShow(this, point.getLatitude(), point.getLongitude(), getString(R.string.favorite)+" : " + point.getName()); //$NON-NLS-1$
			OsmandSettings.setPointToNavigate(this, point.getLatitude(), point.getLongitude());
			Intent newIntent = new Intent(FavouritesActivity.this, MapActivity.class);
			startActivity(newIntent);
		} else if (aItem.getItemId() == EDIT_ITEM) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.favourites_edit_dialog_title);
			final EditText editText = new EditText(this);
			builder.setView(editText);
			editText.setText(point.getName());
			builder.setNegativeButton(R.string.default_buttons_cancel, null);
			builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean editied = helper.editFavouriteName(point, editText.getText().toString());
					if (editied) {
						favouritesAdapter.notifyDataSetChanged();
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
						favouritesList.remove(point);
						favouritesAdapter.notifyDataSetChanged();
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
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == EXPORT_ID){
			File appDir = new File(Environment.getExternalStorageDirectory(), ResourceManager.APP_DIR);
			if(favouritesList == null || favouritesList.isEmpty()){
				Toast.makeText(this, R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
			} else if(!appDir.exists()){
				Toast.makeText(this, R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
			} else {
				File f = new File(appDir, FILE_TO_SAVE);
				List<WptPt> wpt = new ArrayList<WptPt>();
				for(FavouritePoint p : favouritesList){
					WptPt pt = new WptPt();
					pt.lat = p.latitude;
					pt.lon = p.longitude;
					pt.name = p.name;
					wpt.add(pt);
				}
				if(GPXUtilities.saveToXMLFiles(f, wpt, this)){
					Toast.makeText(this, MessageFormat.format(getString(R.string.fav_saved_sucessfully), f.getAbsolutePath()), 
							Toast.LENGTH_LONG).show();
				}
			}
		} else if(item.getItemId() == IMPORT_ID){
			File appDir = new File(Environment.getExternalStorageDirectory(), ResourceManager.APP_DIR);
			File f = new File(appDir, FILE_TO_SAVE);
			if(!f.exists()){
				Toast.makeText(this, MessageFormat.format(getString(R.string.fav_file_to_load_not_found), f.getAbsolutePath()), Toast.LENGTH_LONG).show();
			} else {
				Set<String> existedPoints = new LinkedHashSet<String>();
				if(favouritesList != null){
					for(FavouritePoint fp : favouritesList){
						existedPoints.add(fp.name);
					}
				}
				GPXFileResult res = GPXUtilities.loadGPXFile(this, f);
				if (res.error == null) {
					for(WptPt p : res.wayPoints){
						if(!existedPoints.contains(p.name)){
							FavouritePoint fp = new FavouritePoint();
							fp.name = p.name;
							fp.latitude = p.lat;
							fp.longitude = p.lon;
							helper.addFavourite(fp);
							favouritesList.add(fp);
						}
					}
					Toast.makeText(this, R.string.fav_imported_sucessfully, Toast.LENGTH_SHORT).show();
					favouritesAdapter.notifyDataSetChanged();
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
	public static class FavouritesDbHelper extends SQLiteOpenHelper {

	    private static final int DATABASE_VERSION = 1;
	    private static final String FAVOURITE_TABLE_NAME = "favourite"; //$NON-NLS-1$
	    private static final String FAVOURITE_COL_NAME = "name"; //$NON-NLS-1$
	    private static final String FAVOURITE_COL_LAT = "latitude"; //$NON-NLS-1$
	    private static final String FAVOURITE_COL_LON = "longitude"; //$NON-NLS-1$
	    private static final String FAVOURITE_TABLE_CREATE =   "CREATE TABLE " + FAVOURITE_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
	                FAVOURITE_COL_NAME + " TEXT, " + FAVOURITE_COL_LAT + " double, " + //$NON-NLS-1$ //$NON-NLS-2$
	                FAVOURITE_COL_LON + " double);"; //$NON-NLS-1$

	    public FavouritesDbHelper(Context context) {
	        super(context, FAVOURITE_TABLE_NAME, null, DATABASE_VERSION);
	    }
	    
	    public boolean addFavourite(FavouritePoint p){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		// delete with same name before
	    		deleteFavourite(p);
	    		db.execSQL("INSERT INTO " + FAVOURITE_TABLE_NAME + " VALUES (?, ?, ?)",new Object[]{p.getName(), p.getLatitude(), p.getLongitude()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		return true;
	    	}
	    	return false;
	    }
	    
	    public List<FavouritePoint> getFavouritePoints(){
	    	SQLiteDatabase db = getReadableDatabase();
	    	ArrayList<FavouritePoint> list = new ArrayList<FavouritePoint>();
	    	if(db != null){
	    		Cursor query = db.rawQuery("SELECT " + FAVOURITE_COL_NAME +", " + FAVOURITE_COL_LAT +"," + FAVOURITE_COL_LON +" FROM " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
	    				FAVOURITE_TABLE_NAME, null);
	    		if(query.moveToFirst()){
	    			do {
	    				FavouritePoint p = new FavouritePoint();
	    				p.setName(query.getString(0));
	    				p.setLatitude(query.getDouble(1));
	    				p.setLongitude(query.getDouble(2));
	    				list.add(p);
	    			} while(query.moveToNext());
	    		}
	    		query.close();
	    	}
	    	return list;
	    }
	    
	    public boolean editFavouriteName(FavouritePoint p, String newName){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET name = ? WHERE name = ?",new Object[]{newName, p.getName()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		p.setName(newName);
	    		return true;
	    	}
	    	return false;
	    }
	    
	    public boolean editFavourite(FavouritePoint p, double lat, double lon){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("UPDATE " + FAVOURITE_TABLE_NAME + " SET latitude = ?, longitude = ? WHERE name = ?",new Object[]{lat, lon, p.getName()}); //$NON-NLS-1$ //$NON-NLS-2$ 
	    		p.setLatitude(lat);
	    		p.setLongitude(lon);
	    		return true;
	    	}
	    	return false;
	    }
	    
	    public boolean deleteFavourite(FavouritePoint p){
	    	SQLiteDatabase db = getWritableDatabase();
	    	if(db != null){
	    		db.execSQL("DELETE FROM " + FAVOURITE_TABLE_NAME + " WHERE name = ?",new Object[]{p.getName()}); //$NON-NLS-1$ //$NON-NLS-2$
	    		return true;
	    	}
	    	return false;
	    }
	    

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	        db.execSQL(FAVOURITE_TABLE_CREATE);
	    }

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	
	public static class FavouritePoint {
		private String name;
		private double latitude;
		private double longitude;

		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return "Favourite " + getName(); //$NON-NLS-1$
		}
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(helper != null){
			helper.close();
		}
	}

	class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {
		FavouritesAdapter(List<FavouritePoint> list) {
			super(FavouritesActivity.this, R.layout.favourites_list_item, list);
			this.setNotifyOnChange(false);
		}


		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.favourites_list_item, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.favourite_label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.favouritedistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.favourite_icon);
			FavouritePoint model = (FavouritePoint) getItem(position);
			icon.setImageResource(R.drawable.opened_poi);
			LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(FavouritesActivity.this);
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), 
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			distanceLabel.setText(MapUtils.getFormattedDistance(dist));
			label.setText(model.getName());
			return row;
		}
	}

}
