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
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.LogUtil;
import net.osmand.data.Amenity;
import net.osmand.map.ITileSource;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class MapActivityActions implements DialogProvider {
	
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_NAME = "name";
	private static final String KEY_FAVORITE = "favorite";
	private static final String KEY_ZOOM = "zoom";

	private static final int DIALOG_ADD_FAVORITE = 100;
	private static final int DIALOG_REPLACE_FAVORITE = 101;
	private static final int DIALOG_ADD_WAYPOINT = 102;
	private static final int DIALOG_RELOAD_TITLE = 103;
	private static final int DIALOG_SHARE_LOCATION = 104;
	private static final int DIALOG_ABOUT_ROUTE = 105;
	private static final int DIALOG_SAVE_DIRECTIONS = 106;
	private Bundle dialogBundle = new Bundle();
	
	private final MapActivity mapActivity;

	public MapActivityActions(MapActivity mapActivity){
		this.mapActivity = mapActivity;
	}

	protected void addFavouritePoint(final double latitude, final double longitude){
		String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
		enhance(dialogBundle,latitude,longitude, name);
		mapActivity.showDialog(DIALOG_ADD_FAVORITE);
	}
	
	private Bundle enhance(Bundle aBundle, double latitude, double longitude, String name) {
		aBundle.putDouble(KEY_LATITUDE, latitude);
		aBundle.putDouble(KEY_LONGITUDE, longitude);
		aBundle.putString(KEY_NAME, name);
		return aBundle;
	}
	
	private Bundle enhance(Bundle bundle, double latitude, double longitude, final int zoom) {
		bundle.putInt(KEY_ZOOM, zoom);
		return bundle;
	}

	protected void prepareAddFavouriteDialog(Dialog dialog, Bundle args) {
		final Resources resources = mapActivity.getResources();
		final double latitude = args.getDouble(KEY_LATITUDE);
		final double longitude = args.getDouble(KEY_LONGITUDE);
		String name = resources.getString(R.string.add_favorite_dialog_default_favourite_name);
		if(args.getString(KEY_NAME) != null) {
			name = args.getString(KEY_NAME);
		}
		final FavouritePoint point = new FavouritePoint(latitude, longitude, name,
				resources.getString(R.string.favorite_default_category));
		args.putSerializable(KEY_FAVORITE, point);
		final EditText editText =  (EditText) dialog.findViewById(R.id.Name);
		editText.setText(point.getName());
		final AutoCompleteTextView cat =  (AutoCompleteTextView) dialog.findViewById(R.id.Category);
		cat.setText(point.getCategory());
	}
	
	protected Dialog createAddFavouriteDialog(final Bundle args) {
    	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.favourites_context_menu_edit);
		final View v = mapActivity.getLayoutInflater().inflate(R.layout.favourite_edit_dialog, null, false);
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		builder.setView(v);
		final EditText editText =  (EditText) v.findViewById(R.id.Name);
		final AutoCompleteTextView cat =  (AutoCompleteTextView) v.findViewById(R.id.Category);
		cat.setAdapter(new ArrayAdapter<String>(mapActivity, R.layout.list_textview, helper.getFavoriteGroups().keySet().toArray(new String[] {})));
		
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setNeutralButton(R.string.update_existing, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				mapActivity.showDialog(DIALOG_REPLACE_FAVORITE);
			}
			
		});
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
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
		return builder.create();
    }

	protected Dialog createReplaceFavouriteDialog(final Bundle args) {
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		final Collection<FavouritePoint> points = helper.getFavouritePoints();
		final String[] names = new String[points.size()];
		if(names.length == 0){
			Toast.makeText(mapActivity, getString(R.string.fav_points_not_exist), Toast.LENGTH_SHORT).show();
			helper.close();
			return null;
		}
			
		Builder b = new AlertDialog.Builder(mapActivity);
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
				FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
				if(helper.editFavourite(fv, point.getLatitude(), point.getLongitude())){
					Toast.makeText(mapActivity, getString(R.string.fav_points_edited), Toast.LENGTH_SHORT).show();
				}
				mapActivity.getMapView().refreshMap();
			}
		});
		return b.create();
	}
	
    protected void addWaypoint(final double latitude, final double longitude){
    	String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
    	enhance(dialogBundle,latitude,longitude, name);
    	mapActivity.showDialog(DIALOG_ADD_WAYPOINT);
    }
    
    private Dialog createAddWaypointDialog(final Bundle args) {
    	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.add_waypoint_dialog_title);
		final EditText editText = new EditText(mapActivity);
		editText.setId(R.id.TextView);
		builder.setView(editText);
		editText.setPadding(5, 0, 5, 0);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				double latitude = args.getDouble(KEY_LATITUDE);
				double longitude = args.getDouble(KEY_LONGITUDE);
				String name = editText.getText().toString();
				mapActivity.getSavingTrackHelper().insertPointData(latitude, longitude, System.currentTimeMillis(), name);
				Toast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_waypoint_dialog_added), name), Toast.LENGTH_SHORT)
							.show();
				dialog.dismiss();
			}
		});
		return builder.create();
    }
    
    protected void reloadTile(final int zoom, final double latitude, final double longitude){
    	enhance(dialogBundle,latitude,longitude,zoom);
    	mapActivity.showDialog(DIALOG_RELOAD_TITLE);
    }

    
    private Dialog createReloadTitleDialog(final Bundle args) {
    	Builder builder = new AlertDialog.Builder(mapActivity);
    	builder.setMessage(R.string.context_menu_item_update_map_confirm);
    	builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	final OsmandMapTileView mapView = mapActivity.getMapView();
    	builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int zoom = args.getInt(KEY_ZOOM);
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
				updatePoiDb(args.getInt(KEY_ZOOM), args.getDouble(KEY_LATITUDE), args.getDouble(KEY_LONGITUDE));
			}
    	});
		return builder.create();
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
    	enhance(dialogBundle,latitude,longitude,zoom);
    	mapActivity.showDialog(DIALOG_SHARE_LOCATION);
    }
    
    private Dialog createShareLocationDialog(final Bundle args) {
		AlertDialog.Builder builder = new Builder(mapActivity);
		builder.setTitle(R.string.send_location_way_choose_title);
		builder.setItems(new String[]{
				"Email", "SMS", "Clipboard"
		}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final double latitude = args.getDouble(KEY_LATITUDE);
				final double longitude = args.getDouble(KEY_LONGITUDE);
				final int zoom = args.getInt(KEY_ZOOM);

				final String shortOsmUrl = MapUtils.buildShortOsmUrl(latitude, longitude, zoom);
				// final String simpleGeo = "geo:"+((float) latitude)+","+((float)longitude) +"?z="+zoom;
				final String appLink = "http://download.osmand.net/go?lat="+((float) latitude)+"&lon="+((float)longitude) +"&z="+zoom;
				if(which == 0){
					String email = mapActivity.getString(R.string.send_location_email_pattern, shortOsmUrl, appLink);
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
					intent.putExtra(Intent.EXTRA_SUBJECT, "Location"); //$NON-NLS-1$
					intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
					intent.setType("text/html");
					mapActivity.startActivity(Intent.createChooser(intent, getString(R.string.send_location)));
				} else {
					String sms = mapActivity.getString(R.string.send_location_sms_pattern, shortOsmUrl, appLink);
					if(which == 1){
						Intent sendIntent = new Intent(Intent.ACTION_VIEW);
						sendIntent.putExtra("sms_body", sms); 
						sendIntent.setType("vnd.android-dir/mms-sms");
						mapActivity.startActivity(sendIntent);   
					} else if (which == 2){
						ClipboardManager clipboard = (ClipboardManager) mapActivity.getSystemService(Activity.CLIPBOARD_SERVICE);
						clipboard.setText(sms);
					}
				}
				
			}
		});
    	return builder.create();
    }
    
    protected void aboutRoute() {
    	mapActivity.showDialog(DIALOG_ABOUT_ROUTE);
    }
    
    private void prepareAboutRouteDialog(Dialog dlg, Bundle args) {
    	((AlertDialog)dlg).setMessage(mapActivity.getRoutingHelper().getGeneralRouteInformation());
    }
    
    private Dialog createAboutRouteDialog(Bundle args) {
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
		return builder.create();
    }
    
    private boolean checkPointToNavigate(){
    	MapActivityLayers mapLayers = mapActivity.getMapLayers();
    	if(mapLayers.getNavigationLayer().getPointToNavigate() == null){
			Toast.makeText(mapActivity, R.string.mark_final_location_first, Toast.LENGTH_LONG).show();
			return false;
		}
    	return true;
    }
    
    protected void getDirections(final double lat, final double lon, boolean followEnabled){
    	
    	final OsmandSettings settings = OsmandSettings.getOsmandSettings(mapActivity);
    	final RoutingHelper routingHelper = mapActivity.getRoutingHelper();
    	
    	Builder builder = new AlertDialog.Builder(mapActivity);
    	
    	
    	View view = mapActivity.getLayoutInflater().inflate(R.layout.calculate_route, null);
    	final ToggleButton[] buttons = new ToggleButton[ApplicationMode.values().length];
    	buttons[ApplicationMode.CAR.ordinal()] = (ToggleButton) view.findViewById(R.id.CarButton);
    	buttons[ApplicationMode.BICYCLE.ordinal()] = (ToggleButton) view.findViewById(R.id.BicycleButton);
    	buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (ToggleButton) view.findViewById(R.id.PedestrianButton);
    	ApplicationMode appMode = settings.getApplicationMode();
		for (int i = 0; i < buttons.length; i++) {
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
				if(!checkPointToNavigate()){
					return;
				}
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
				if(!checkPointToNavigate()){
					return;
				}
				ApplicationMode mode = getAppMode(buttons, settings);
				// change global settings
				boolean changed = settings.APPLICATION_MODE.set(mode);
				if (changed) {
					mapActivity.updateApplicationModeSettings();	
					mapActivity.getMapView().refreshMap(true);
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
    		builder.setPositiveButton(R.string.show_gpx_route, onlyShowCall);
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
						mapActivity.getMapView().refreshMap(changed);
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
		mapActivity.showDialog(DIALOG_SAVE_DIRECTIONS);
	}
	
	private Dialog createSaveDirections() {
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
		
		
		return dlg;
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

	@Override
	public Dialog onCreateDialog(int id) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_ADD_FAVORITE:
				return createAddFavouriteDialog(args);
			case DIALOG_REPLACE_FAVORITE:
				return createReplaceFavouriteDialog(args);
			case DIALOG_ADD_WAYPOINT:
				return createAddWaypointDialog(args);
			case DIALOG_RELOAD_TITLE:
				return createReloadTitleDialog(args);
			case DIALOG_SHARE_LOCATION:
				return createShareLocationDialog(args);
			case DIALOG_ABOUT_ROUTE:
				return createAboutRouteDialog(args);
			case DIALOG_SAVE_DIRECTIONS:
				return createSaveDirections();
		}
		return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		Bundle args = dialogBundle;
		switch (id) {
		case DIALOG_ADD_FAVORITE:
			prepareAddFavouriteDialog(dialog, args);
			break;
		case DIALOG_ADD_WAYPOINT:
			EditText v = (EditText) dialog.getWindow().findViewById(R.id.TextView);
			v.setPadding(5, 0, 5, 0);
			if(args.getString(KEY_NAME) != null) {
				v.setText(args.getString(KEY_NAME));
			} else {
				v.setText("");
			}
			break;
		case DIALOG_ABOUT_ROUTE:
			prepareAboutRouteDialog(dialog, args);
			break;
		}
	}

}
