/**
 * 
 */
package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.FavouritePoint;
import net.osmand.GPXUtilities;
import net.osmand.OsmAndFormatter;
import net.osmand.GPXUtilities.GPXFileResult;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
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
	

	private FavouritesAdapter favouritesAdapter;
	private FavouritesDbHelper helper;
	

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		setContentView(lv);

		
		
		/* Add Context-Menu listener to the ListView. */
        lv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener(){

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				menu.setHeaderTitle(R.string.favourites_context_menu_title);
				menu.add(0, NAVIGATE_TO, 0, R.string.favourites_context_menu_navigate);
				final FavouritePoint point = (FavouritePoint) favouritesAdapter.getItem(((AdapterContextMenuInfo)menuInfo).position);
				if(point.isStored()){
					menu.add(0, EDIT_ITEM, 1, R.string.favourites_context_menu_edit);
					menu.add(0, DELETE_ITEM, 2, R.string.favourites_context_menu_delete);
				}
			}
        	
        });
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		helper = ((OsmandApplication)getApplication()).getFavorites();
		ArrayList<FavouritePoint> list = new ArrayList<FavouritePoint>(helper.getFavouritePoints());
		if(helper.getFavoritePointsFromGPXFile() != null){
			list.addAll(helper.getFavoritePointsFromGPXFile());
		}
		favouritesAdapter = new FavouritesAdapter(list);
		final LatLon mapLocation = OsmandSettings.getLastKnownMapLocation(OsmandSettings.getPrefs(this));
		if(mapLocation != null){
			favouritesAdapter.sort(new Comparator<FavouritePoint>(){

				@Override
				public int compare(FavouritePoint object1, FavouritePoint object2) {
					double d1 = MapUtils.getDistance(mapLocation, object1.getLatitude(), object1.getLongitude());
					double d2 = MapUtils.getDistance(mapLocation, object2.getLatitude(), object2.getLongitude());
					if(d1 == d2){
						return 0;
					} else if(d1 > d2){
						return 1;
					}
					return -1;
				}
				
			});
		}
		getListView().setAdapter(favouritesAdapter);
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		FavouritePoint point = favouritesAdapter.getItem(position);
		OsmandSettings.setShowingFavorites(this, true);
		OsmandSettings.setMapLocationToShow(this, point.getLatitude(), point.getLongitude(), getString(R.string.favorite)+" : " + point.getName()); //$NON-NLS-1$
		Intent newIntent = new Intent(FavouritesActivity.this, MapActivity.class);
		startActivity(newIntent);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem.getMenuInfo();
		final FavouritePoint point = (FavouritePoint) favouritesAdapter.getItem(menuInfo.position);
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
						favouritesAdapter.remove(point);
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
			File appDir = OsmandSettings.extendOsmandPath(getApplicationContext(), ResourceManager.APP_DIR);
			if(favouritesAdapter.isEmpty()){
				Toast.makeText(this, R.string.no_fav_to_save, Toast.LENGTH_LONG).show();
			} else if(!appDir.exists()){
				Toast.makeText(this, R.string.sd_dir_not_accessible, Toast.LENGTH_LONG).show();
			} else {
				File f = new File(appDir, FILE_TO_SAVE);
				List<WptPt> wpt = new ArrayList<WptPt>();
				for (int i = 0; i < favouritesAdapter.getCount(); i++) {
					FavouritePoint p = favouritesAdapter.getItem(i);
					WptPt pt = new WptPt();
					pt.lat = p.getLatitude();
					pt.lon = p.getLongitude();
					pt.name = p.getName();
					wpt.add(pt);
				}
				if(GPXUtilities.saveToXMLFiles(f, wpt, this)){
					Toast.makeText(this, MessageFormat.format(getString(R.string.fav_saved_sucessfully), f.getAbsolutePath()), 
							Toast.LENGTH_LONG).show();
				}
			}
		} else if(item.getItemId() == IMPORT_ID){
			File appDir = OsmandSettings.extendOsmandPath(getApplicationContext(), ResourceManager.APP_DIR);
			File f = new File(appDir, FILE_TO_SAVE);
			if(!f.exists()){
				Toast.makeText(this, MessageFormat.format(getString(R.string.fav_file_to_load_not_found), f.getAbsolutePath()), Toast.LENGTH_LONG).show();
			} else {
				Set<String> existedPoints = new LinkedHashSet<String>();
				if(!favouritesAdapter.isEmpty()){
					for (int i = 0; i < favouritesAdapter.getCount(); i++) {
						FavouritePoint fp = favouritesAdapter.getItem(i);
						existedPoints.add(fp.getName());
					}
				}
				GPXFileResult res = GPXUtilities.loadGPXFile(this, f);
				if (res.error == null) {
					for(WptPt p : res.wayPoints){
						if(!existedPoints.contains(p.name)){
							FavouritePoint fp = new FavouritePoint(p.lat, p.lon, p.name);
							if(helper.addFavourite(fp)){
								favouritesAdapter.add(fp);
							}
						}
					}
					Toast.makeText(this, R.string.fav_imported_sucessfully, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, res.error, Toast.LENGTH_LONG).show();
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
	

	

	class FavouritesAdapter extends ArrayAdapter<FavouritePoint> {
		FavouritesAdapter(List<FavouritePoint> list) {
			super(FavouritesActivity.this, R.layout.favourites_list_item, list);
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
			row.setTag(model);
			if(model.isStored()){
				icon.setImageResource(R.drawable.favorites);
			} else {
				icon.setImageResource(R.drawable.opened_poi);
			}
			LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(OsmandSettings.getPrefs(FavouritesActivity.this));
			int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(), 
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(dist, FavouritesActivity.this));
			label.setText(model.getName());
			return row;
		}
	}

}
