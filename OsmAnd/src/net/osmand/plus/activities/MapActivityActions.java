package net.osmand.plus.activities;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.FavouritePoint;
import net.osmand.GPXUtilities;
import net.osmand.LogUtil;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.Amenity;
import net.osmand.map.ITileSource;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
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
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.AsyncTask;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MapActivityActions {
	
	private final MapActivity mapActivity;

	public MapActivityActions(MapActivity mapActivity){
		this.mapActivity = mapActivity;
	}

	protected void addFavouritePoint(final double latitude, final double longitude){
    	final Resources resources = mapActivity.getResources();
    	final FavouritePoint point = new FavouritePoint(latitude, longitude, resources.getString(R.string.add_favorite_dialog_default_favourite_name),
    			resources.getString(R.string.favorite_default_category));
    	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.favourites_edit_dialog_title);
		final View v = mapActivity.getLayoutInflater().inflate(R.layout.favourite_edit_dialog, null, false);
		final FavouritesDbHelper helper = ((OsmandApplication)mapActivity.getApplication()).getFavorites();
		builder.setView(v);
		final EditText editText =  (EditText) v.findViewById(R.id.Name);
		editText.setText(point.getName());
		final AutoCompleteTextView cat =  (AutoCompleteTextView) v.findViewById(R.id.Category);
		cat.setText(point.getCategory());
		cat.setAdapter(new ArrayAdapter<String>(mapActivity, R.layout.list_textview, helper.getFavoriteGroups().keySet().toArray(new String[] {})));
		
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setNeutralButton(R.string.update_existing, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Builder b = new AlertDialog.Builder(mapActivity);
				
				final Collection<FavouritePoint> points = helper.getFavouritePoints();
				final String[] names = new String[points.size()];
				final FavouritePoint[] favs = new FavouritePoint[points.size()];
				Iterator<FavouritePoint> it = points.iterator();
				int i=0;
				while(it.hasNext()){
					FavouritePoint fp = it.next();
					// filter gpx points
					if(fp.isStored()){
						favs[i] = fp;
						names[i] = fp.getName();
						i++;
					}
				}
				b.setItems(names, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FavouritePoint fv = favs[which];
						if(helper.editFavourite(fv, latitude, longitude)){
							Toast.makeText(mapActivity, getString(R.string.fav_points_edited), Toast.LENGTH_SHORT).show();
						}
						mapActivity.getMapView().refreshMap();
					}
				});
				if(names.length == 0){
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
				point.setName(editText.getText().toString());
				point.setCategory(cat.getText().toString());
				boolean added = helper.addFavourite(point);
				if (added) {
					Toast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_favorite_dialog_favourite_added_template), point.getName()), Toast.LENGTH_SHORT)
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
				if(!(mainLayer instanceof MapTileLayer) || !((MapTileLayer) mainLayer).isVisible()){
					Toast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
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
    	final AmenityIndexRepositoryOdb repo = ((OsmandApplication) mapActivity.getApplication()).
    								getResourceManager().getUpdatablePoiDb();
    	if(repo == null){
    		Toast.makeText(mapActivity, getString(R.string.update_poi_no_offline_poi_index), Toast.LENGTH_LONG).show();
    		return;
    	} else {
    		Toast.makeText(mapActivity, getString(R.string.update_poi_does_not_change_indexes), Toast.LENGTH_LONG).show();
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
						repo.updateAmenities(amenities, leftLon, topLat, rightLon, bottomLat);
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
				String sms = mapActivity.getString(R.string.send_location_sms_pattern, shortOsmUrl, appLink);
				String email = mapActivity.getString(R.string.send_location_email_pattern, shortOsmUrl, appLink);
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
    
    
    protected void aboutRoute() {
    	DialogInterface.OnClickListener showRoute = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(mapActivity, ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapActivity.startActivity(intent);
			}
		};
		DialogInterface.OnClickListener saveDirections = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				saveDirections();
			}
		};
		Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.show_route);
		builder.setMessage(mapActivity.getRoutingHelper().getGeneralRouteInformation());
		builder.setPositiveButton(R.string.default_buttons_save, saveDirections);
		builder.setNeutralButton(R.string.route_about, showRoute);
		builder.setNegativeButton(R.string.close, null);
		builder.show();
    }
    
    protected void getDirections(final double lat, final double lon, boolean followEnabled){
    	MapActivityLayers mapLayers = mapActivity.getMapLayers();
    	final OsmandSettings settings = OsmandSettings.getOsmandSettings(mapActivity);
    	final RoutingHelper routingHelper = mapActivity.getRoutingHelper();
    	if(mapLayers.getNavigationLayer().getPointToNavigate() == null){
			Toast.makeText(mapActivity, R.string.mark_final_location_first, Toast.LENGTH_LONG).show();
			return;
		}
    	
    	Builder builder = new AlertDialog.Builder(mapActivity);
    	
    	View view = mapActivity.getLayoutInflater().inflate(R.layout.calculate_route, null);
    	final ToggleButton[] buttons = new ToggleButton[ApplicationMode.values().length];
    	buttons[ApplicationMode.CAR.ordinal()] = (ToggleButton) view.findViewById(R.id.CarButton);
    	buttons[ApplicationMode.BICYCLE.ordinal()] = (ToggleButton) view.findViewById(R.id.BicycleButton);
    	buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (ToggleButton) view.findViewById(R.id.PedestrianButton);
    	ApplicationMode appMode = settings.getApplicationMode();
    	for(int i=0; i< buttons.length; i++){
    		if(buttons[i] != null){
    			final int ind = i;
    			ToggleButton b = buttons[i];
    			b.setChecked(appMode == ApplicationMode.values()[i]);
    			b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if(isChecked){
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if(buttons[j].isChecked() != (ind == j)){
										buttons[j].setChecked(ind == j);
									}
								}
							}
						} else {
							// revert state
							boolean revert = true;
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if(buttons[j].isChecked()){
										revert = false;
										break;
									}
								}
							}
							if (revert){ 
								buttons[ind].setChecked(true);
							}
						}
					}
    			});
    		}
    	}
    	
    	DialogInterface.OnClickListener onlyShowCall = new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				ApplicationMode mode = getAppMode(buttons, settings);
				Location location = new Location("map"); //$NON-NLS-1$
				location.setLatitude(lat);
				location.setLongitude(lon);
				routingHelper.setAppMode(mode);
				settings.FOLLOW_THE_ROUTE.set(false);
				settings.FOLLOW_THE_GPX_ROUTE.set(null);
				routingHelper.setFollowingMode(false);
				routingHelper.setFinalAndCurrentLocation(mapActivity.getPointToNavigate(), location);
			}
    	};
    	
    	DialogInterface.OnClickListener followCall = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ApplicationMode mode = getAppMode(buttons, settings);
				// change global settings
				boolean changed = settings.APPLICATION_MODE.set(mode);
				if (changed) {
					mapActivity.updateApplicationModeSettings();	
					mapActivity.getMapView().refreshMap();
				}
				
				Location location = getLocationToStartFrom(lat, lon); 
				routingHelper.setAppMode(mode);
				settings.FOLLOW_THE_ROUTE.set(true);
				settings.FOLLOW_THE_GPX_ROUTE.set(null);
				routingHelper.setFollowingMode(true);
				routingHelper.setFinalAndCurrentLocation(mapActivity.getPointToNavigate(), location);
				dialog.dismiss();
				getMyApplication().showDialogInitializingCommandPlayer(mapActivity);
			}
    	};
    	
		
		DialogInterface.OnClickListener useGpxNavigation = new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ApplicationMode mode = getAppMode(buttons, settings);
				navigateUsingGPX(mode);
			}
		};
    	
    	builder.setView(view);
    	if (followEnabled) {
    		builder.setTitle(R.string.follow_route);
			builder.setPositiveButton(R.string.follow, followCall);
			builder.setNeutralButton(R.string.gpx_navigation, useGpxNavigation);
			builder.setNegativeButton(R.string.only_show, onlyShowCall);
		} else {
			builder.setTitle(R.string.show_route);
			view.findViewById(R.id.TextView).setVisibility(View.GONE);
    		builder.setPositiveButton(R.string.show_route, onlyShowCall);
    		builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	}
    	builder.show();
    }
    
    protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}

	private Location getLocationToStartFrom(final double lat, final double lon) {
		Location location = mapActivity.getLastKnownLocation();
		if(location == null){
			location = new Location("map"); //$NON-NLS-1$
			location.setLatitude(lat);
			location.setLongitude(lon);
		}
		return location;
	}
    
    public void navigateUsingGPX(final ApplicationMode appMode) {
		final LatLon endForRouting = mapActivity.getPointToNavigate();
		final MapActivityLayers mapLayers = mapActivity.getMapLayers();
		final OsmandSettings settings = OsmandSettings.getOsmandSettings(mapActivity);
    	final RoutingHelper routingHelper = mapActivity.getRoutingHelper();
		mapLayers.selectGPXFileLayer(new CallbackWithObject<GPXFile>() {
			
			@Override
			public boolean processResult(final GPXFile result) {
				Builder builder = new AlertDialog.Builder(mapActivity);
				final boolean[] props = new boolean[]{false, false, false};
				builder.setMultiChoiceItems(new String[] { getString(R.string.gpx_option_reverse_route),
						getString(R.string.gpx_option_destination_point), getString(R.string.gpx_option_from_start_point) }, props,
						new OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked) {
								props[which] = isChecked;
							}
						});
				builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean reverse = props[0];
						boolean passWholeWay = props[2];
						boolean useDestination = props[1];
						GPXRouteParams gpxRoute = new GPXRouteParams(result, reverse);
						
						Location loc = mapActivity.getLastKnownLocation();
						if(passWholeWay && loc != null){
							gpxRoute.setStartPoint(loc);
						}
						
						Location startForRouting = mapActivity.getLastKnownLocation();
						if(startForRouting == null){
							startForRouting = gpxRoute.getStartPointForRoute();
						}
						
						LatLon endPoint = endForRouting;
						if(endPoint == null || !useDestination){
							LatLon point = gpxRoute.getLastPoint();
							if(point != null){
								endPoint = point;
							}
							if(endPoint != null) {
								settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), null);
								mapLayers.getNavigationLayer().setPointToNavigate(point);
							}
						}
						// change global settings
						boolean changed = settings.APPLICATION_MODE.set(appMode);
						if (changed) {
							mapActivity.updateApplicationModeSettings();	
						}
						mapActivity.getMapView().refreshMap();
						if(endPoint != null){
							settings.FOLLOW_THE_ROUTE.set(true);
							settings.FOLLOW_THE_GPX_ROUTE.set(result.path);
							routingHelper.setFollowingMode(true);
							routingHelper.setFinalAndCurrentLocation(endPoint, startForRouting, gpxRoute);
							getMyApplication().showDialogInitializingCommandPlayer(mapActivity);
						}
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.show();
				return true;
			}
		}, false);
	}
    
    private ApplicationMode getAppMode(ToggleButton[] buttons, OsmandSettings settings){
    	for(int i=0; i<buttons.length; i++){
    		if(buttons[i] != null && buttons[i].isChecked() && i < ApplicationMode.values().length){
    			return ApplicationMode.values()[i];
    		}
    	}
    	return settings.getApplicationMode();
    }

	public void saveDirections() {
		OsmandSettings settings = getMyApplication().getSettings();
		final File fileDir = settings.extendOsmandPath(ResourceManager.GPX_PATH);
		final Dialog dlg = new Dialog(mapActivity);
		dlg.setTitle(R.string.save_route_dialog_title);
		dlg.setContentView(R.layout.save_directions_dialog);
		final EditText edit = (EditText) dlg.findViewById(R.id.FileNameEdit);
		
		edit.setText(MessageFormat.format("{0,date,dd-MM-yyyy}", new Date()));
		((Button) dlg.findViewById(R.id.Save)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = edit.getText().toString();
				fileDir.mkdirs();
				File toSave = fileDir;
				if(name.length() > 0) {
					if(!name.endsWith(".gpx")){
						name += ".gpx";
					}
					toSave = new File(fileDir, name);
				}
				if(toSave.exists()){
					dlg.findViewById(R.id.DuplicateFileName).setVisibility(View.VISIBLE);					
				} else {
					dlg.dismiss();
					new SaveDirectionsAsyncTask().execute(toSave);
				}
			}
		});
		
		((Button) dlg.findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});
		
		
		dlg.show();
	}
	
	
	private class SaveDirectionsAsyncTask extends AsyncTask<File, Void, String> {

		@Override
		protected String doInBackground(File... params) {
			if (params.length > 0) {
				File file = params[0];
				GPXFile gpx = mapActivity.getRoutingHelper().generateGPXFileWithRoute();
				GPXUtilities.writeGpxFile(file, gpx, mapActivity);
				return mapActivity.getString(R.string.route_successfully_saved_at, file.getName());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(result != null){
				Toast.makeText(mapActivity, result, Toast.LENGTH_LONG).show();
			}
		}
		
	}

}
