package net.osmand.plus.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.osmand.FavouritePoint;
import net.osmand.LogUtil;
import net.osmand.data.Amenity;
import net.osmand.map.ITileSource;
import net.osmand.osm.MapUtils;
import net.osmand.plus.AmenityIndexRepository;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.FloatMath;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class MapActivityActions {
	
	private final MapActivity mapActivity;

	public MapActivityActions(MapActivity mapActivity){
		this.mapActivity = mapActivity;
	}

	protected void addFavouritePoint(final double latitude, final double longitude){
    	final Resources resources = mapActivity.getResources();
    	final FavouritePoint p = new FavouritePoint(latitude, longitude, resources.getString(R.string.add_favorite_dialog_default_favourite_name));
    	
    	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.add_favorite_dialog_top_text);
		final EditText editText = new EditText(mapActivity);
		builder.setView(editText);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setNeutralButton(R.string.update_existing, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Builder b = new AlertDialog.Builder(mapActivity);
				final FavouritesDbHelper helper = ((OsmandApplication)mapActivity.getApplication()).getFavorites();
				final Collection<FavouritePoint> points = helper.getFavouritePoints();
				final String[] ar = new String[points.size()];
				Iterator<FavouritePoint> it = points.iterator();
				int i=0;
				while(it.hasNext()){
					ar[i++] = it.next().getName();
				}
				b.setItems(ar, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FavouritePoint fv = helper.getFavoritePointByName(ar[which]);
						if(helper.editFavourite(fv, latitude, longitude)){
							Toast.makeText(mapActivity, getString(R.string.fav_points_edited), Toast.LENGTH_SHORT).show();
						}
						mapActivity.getMapView().refreshMap();
					}
				});
				if(ar.length == 0){
					Toast.makeText(mapActivity, getString(R.string.fav_points_not_exist), Toast.LENGTH_SHORT).show();
					helper.close();
				}  else {
					b.show();
				}
			}
			
		});
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final FavouritesDbHelper helper = ((OsmandApplication)mapActivity.getApplication()).getFavorites();
				p.setName(editText.getText().toString());
				boolean added = helper.addFavourite(p);
				if (added) {
					Toast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_favorite_dialog_favourite_added_template), p.getName()), Toast.LENGTH_SHORT)
							.show();
				}
				mapActivity.getMapView().refreshMap();
			}
		});
		builder.create().show();
    }

    protected void addWaypoint(final double latitude, final double longitude, final SavingTrackHelper savingTrackHelper){
    	
    	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.add_waypoint_dialog_title);
		final EditText editText = new EditText(mapActivity);
		builder.setView(editText);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String name = editText.getText().toString();
				savingTrackHelper.insertPointData(latitude, longitude, System.currentTimeMillis(), name);
				Toast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_waypoint_dialog_added), name), Toast.LENGTH_SHORT)
							.show();
			}
		});
		builder.create().show();
    }
    
    protected void reloadTile(final int zoom, final double latitude, final double longitude){
    	Builder builder = new AlertDialog.Builder(mapActivity);
    	builder.setMessage(R.string.context_menu_item_update_map_confirm);
    	builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	final OsmandMapTileView mapView = mapActivity.getMapView();
    	builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				BaseMapLayer mainLayer = mapView.getMainLayer();
				if(!(mainLayer instanceof MapTileLayer) || ((MapTileLayer) mainLayer).isVisible()){
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
				}
				final ITileSource mapSource = ((MapTileLayer) mainLayer).getMap();
				if(mapSource == null || !mapSource.couldBeDownloadedFromInternet()){
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				Rect pixRect = new Rect(0, 0, mapView.getWidth(), mapView.getHeight());
		    	RectF tilesRect = new RectF();
		    	mapView.calculateTileRectangle(pixRect, mapView.getCenterPointX(), mapView.getCenterPointY(), 
		    			mapView.getXTile(), mapView.getYTile(), tilesRect);
		    	int left = (int) FloatMath.floor(tilesRect.left);
				int top = (int) FloatMath.floor(tilesRect.top);
				int width = (int) (FloatMath.ceil(tilesRect.right) - left);
				int height = (int) (FloatMath.ceil(tilesRect.bottom) - top);
				for (int i = 0; i <width; i++) {
					for (int j = 0; j< height; j++) {
						((OsmandApplication)mapActivity.getApplication()).getResourceManager().
								clearTileImageForMap(null, mapSource, i + left, j + top, zoom);	
					}
				}
				
				
				mapView.refreshMap();
			}
    	});
    	builder.setNeutralButton(R.string.context_menu_item_update_poi, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				updatePoiDb(zoom, latitude, longitude);
				
			}
    	});
		builder.create().show();
    }
    
    protected String getString(int res){
    	return mapActivity.getString(res);
    }
    
    protected void updatePoiDb(int zoom, double latitude, double longitude){
    	if(zoom < 15){
    		Toast.makeText(mapActivity, getString(R.string.update_poi_is_not_available_for_zoom), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	final List<AmenityIndexRepository> repos = ((OsmandApplication) mapActivity.getApplication()).
    								getResourceManager().searchAmenityRepositories(latitude, longitude);
    	if(repos.isEmpty()){
    		Toast.makeText(mapActivity, getString(R.string.update_poi_no_offline_poi_index), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	final OsmandMapTileView mapView = mapActivity.getMapView();
    	Rect pixRect = new Rect(-mapView.getWidth()/2, -mapView.getHeight()/2, 3*mapView.getWidth()/2, 3*mapView.getHeight()/2);
    	RectF tileRect = new RectF();
    	mapView.calculateTileRectangle(pixRect, mapView.getCenterPointX(), mapView.getCenterPointY(), 
    			mapView.getXTile(), mapView.getYTile(), tileRect);
    	final double leftLon = MapUtils.getLongitudeFromTile(zoom, tileRect.left); 
    	final double topLat = MapUtils.getLatitudeFromTile(zoom, tileRect.top);
		final double rightLon = MapUtils.getLongitudeFromTile(zoom, tileRect.right);
		final double bottomLat = MapUtils.getLatitudeFromTile(zoom, tileRect.bottom);
    	
		ProgressDialog progressDlg = ProgressDialog.show(mapActivity, getString(R.string.loading), getString(R.string.loading_data));
		mapActivity.setProgressDlg(progressDlg);
    	new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					List<Amenity> amenities = new ArrayList<Amenity>();
					boolean loadingPOIs = AmenityIndexRepositoryOdb.loadingPOIs(amenities, leftLon, topLat, rightLon, bottomLat);
					if(!loadingPOIs){
						showToast(getString(R.string.update_poi_error_loading));
					} else {
						for(AmenityIndexRepository r  : repos){
							if(r instanceof AmenityIndexRepositoryOdb){
								((AmenityIndexRepositoryOdb) r).updateAmenities(amenities, leftLon, topLat, rightLon, bottomLat);
							}
						}
						showToast(MessageFormat.format(getString(R.string.update_poi_success), amenities.size()));
						mapView.refreshMap();
					}
				} catch(Exception e) {
					Log.e(LogUtil.TAG, "Error updating local data", e); //$NON-NLS-1$
					showToast(getString(R.string.update_poi_error_local));
				} finally {
					Dialog prog = mapActivity.getProgressDlg();
					if(prog !=null){
						prog.dismiss();
						mapActivity.setProgressDlg(prog);
					}
				}
			}
    	}, "LoadingPOI").start(); //$NON-NLS-1$
    	
    }
    
    protected void showToast(final String msg){
    	mapActivity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
			}
    	});
    }
    
    protected void shareLocation(final double latitude, final double longitude, int zoom){
    	final String shortOsmUrl = MapUtils.buildShortOsmUrl(latitude, longitude, zoom);
		// final String simpleGeo = "geo:"+((float) latitude)+","+((float)longitude) +"?z="+zoom;
		final String appLink = "http://download.osmand.net/go?lat="+((float) latitude)+"&lon="+((float)longitude) +"&z="+zoom;
		
		AlertDialog.Builder builder = new Builder(mapActivity);
		builder.setTitle(R.string.send_location_way_choose_title);
		builder.setItems(new String[]{
				"Email", "SMS", "Clipboard"
		}, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String sms = MessageFormat.format(getString(R.string.send_location_sms_pattern), shortOsmUrl, appLink);
				String email = MessageFormat.format(getString(R.string.send_location_email_pattern), shortOsmUrl, appLink );
				if(which == 0){
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
					intent.putExtra(Intent.EXTRA_SUBJECT, "Mine location"); //$NON-NLS-1$
					intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
					intent.setType("text/html");
					mapActivity.startActivity(Intent.createChooser(intent, getString(R.string.send_location)));
				} else if(which == 1){
					Intent sendIntent = new Intent(Intent.ACTION_VIEW);
					sendIntent.putExtra("sms_body", sms); 
					sendIntent.setType("vnd.android-dir/mms-sms");
					mapActivity.startActivity(sendIntent);   
				} else if (which == 2){
					ClipboardManager clipboard = (ClipboardManager) mapActivity.getSystemService(Activity.CLIPBOARD_SERVICE);
					clipboard.setText(sms);
				}
				
			}
		});
    	
    	builder.show();
    }

}
