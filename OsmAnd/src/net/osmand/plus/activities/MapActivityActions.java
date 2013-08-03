package net.osmand.plus.activities;



import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.map.ITileSource;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.Version;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.FloatMath;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MapActivityActions implements DialogProvider {
	
	private static final String GPS_STATUS_COMPONENT = "com.eclipsim.gpsstatus2"; //$NON-NLS-1$
	private static final String GPS_STATUS_ACTIVITY = "com.eclipsim.gpsstatus2.GPSStatus"; //$NON-NLS-1$
	private static final String ZXING_BARCODE_SCANNER_COMPONENT = "com.google.zxing.client.android"; //$NON-NLS-1$
	private static final String ZXING_BARCODE_SCANNER_ACTIVITY = "com.google.zxing.client.android.ENCODE"; //$NON-NLS-1$

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
	private static final int DIALOG_SAVE_DIRECTIONS = 106;
	// make static
	private static Bundle dialogBundle = new Bundle();
	
	private final MapActivity mapActivity;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	

	public MapActivityActions(MapActivity mapActivity){
		this.mapActivity = mapActivity;
		settings = mapActivity.getMyApplication().getSettings();
		routingHelper = mapActivity.getMyApplication().getRoutingHelper();
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
		bundle.putDouble(KEY_LATITUDE, latitude);
		bundle.putDouble(KEY_LONGITUDE, longitude);
		bundle.putInt(KEY_ZOOM, zoom);
		return bundle;
	}

	public static void prepareAddFavouriteDialog(Activity activity, Dialog dialog, Bundle args, double lat, double lon, String name) {
		final Resources resources = activity.getResources();
		if(name == null) {
			name = resources.getString(R.string.add_favorite_dialog_default_favourite_name);
		}
		final FavouritePoint point = new FavouritePoint(lat, lon, name,
				resources.getString(R.string.favorite_default_category));
		args.putSerializable(KEY_FAVORITE, point);
		final EditText editText =  (EditText) dialog.findViewById(R.id.Name);
		editText.setText(point.getName());
		editText.selectAll();
		editText.requestFocus();
		final AutoCompleteTextView cat =  (AutoCompleteTextView) dialog.findViewById(R.id.Category);
		cat.setText(point.getCategory());
		AndroidUtils.softKeyboardDelayed(editText);
	}
	
	public  static Dialog createAddFavouriteDialog(final Activity activity, final Bundle args) {
    	Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.favourites_context_menu_edit);
		final View v = activity.getLayoutInflater().inflate(R.layout.favourite_edit_dialog, null, false);
		final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
		builder.setView(v);
		final EditText editText =  (EditText) v.findViewById(R.id.Name);
		final AutoCompleteTextView cat =  (AutoCompleteTextView) v.findViewById(R.id.Category);
		cat.setAdapter(new ArrayAdapter<String>(activity, R.layout.list_textview, helper.getFavoriteGroups().keySet().toArray(new String[] {})));
		
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setNeutralButton(R.string.update_existing, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Don't use showDialog because it is impossible to refresh favorite items list
				Dialog dlg = createReplaceFavouriteDialog(activity, args);
				if(dlg != null) {
					dlg.show();
				}
				// mapActivity.showDialog(DIALOG_REPLACE_FAVORITE);
			}
			
		});
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				FavouritePoint point = (FavouritePoint) args.getSerializable(KEY_FAVORITE);
				final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
				point.setName(editText.getText().toString().trim());
				point.setCategory(cat.getText().toString().trim());
				boolean added = helper.addFavourite(point);
				if (added) {
					AccessibleToast.makeText(activity, MessageFormat.format(
							activity.getString(R.string.add_favorite_dialog_favourite_added_template), point.getName()), Toast.LENGTH_SHORT)
							.show();
				}
				if(activity instanceof MapActivity) {
					((MapActivity) activity).getMapView().refreshMap(true);
				}
			}
		});
		return builder.create();
    }

	protected static Dialog createReplaceFavouriteDialog(final Activity activity, final Bundle args) {
		final FavouritesDbHelper helper = ((OsmandApplication) activity.getApplication()).getFavorites();
		final List<FavouritePoint> points = new ArrayList<FavouritePoint>(helper.getFavouritePoints());
		final Collator ci = java.text.Collator.getInstance();
		Collections.sort(points, new Comparator<FavouritePoint>() {

			@Override
			public int compare(FavouritePoint object1, FavouritePoint object2) {
				return ci.compare(object1.getName(), object2.getName());
			}
		});
		final String[] names = new String[points.size()];
		if(names.length == 0){
			AccessibleToast.makeText(activity, activity.getString(R.string.fav_points_not_exist), Toast.LENGTH_SHORT).show();
			return null;
		}
			
		Builder b = new AlertDialog.Builder(activity);
		final FavouritePoint[] favs = new FavouritePoint[points.size()];
		Iterator<FavouritePoint> it = points.iterator();
		int i=0;
		while (it.hasNext()) {
			FavouritePoint fp = it.next();
			// filter gpx points
			if (fp.isStored()) {
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
					AccessibleToast.makeText(activity, activity.getString(R.string.fav_points_edited), Toast.LENGTH_SHORT).show();
				}
				if(activity instanceof MapActivity) {
					((MapActivity) activity).getMapView().refreshMap();
				}
			}
		});
		AlertDialog al = b.create();
		return al;
	}
	
    public void addWaypoint(final double latitude, final double longitude){
    	String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
    	enhance(dialogBundle,latitude,longitude, name);
    	mapActivity.showDialog(DIALOG_ADD_WAYPOINT);
    }
    
    private Dialog createAddWaypointDialog(final Bundle args) {
    	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(R.string.add_waypoint_dialog_title);
		FrameLayout parent = new FrameLayout(mapActivity);
		final EditText editText = new EditText(mapActivity);
		editText.setId(R.id.TextView);
		parent.setPadding(15, 0, 15, 0);
		parent.addView(editText);
		builder.setView(parent);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_add, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				double latitude = args.getDouble(KEY_LATITUDE);
				double longitude = args.getDouble(KEY_LONGITUDE);
				String name = editText.getText().toString();
				SavingTrackHelper savingTrackHelper = mapActivity.getMyApplication().getSavingTrackHelper();
				savingTrackHelper.insertPointData(latitude, longitude, System.currentTimeMillis(), name);
				if(settings.SHOW_CURRENT_GPX_TRACK.get()) {
					getMyApplication().getFavorites().addFavoritePointToGPXFile(new FavouritePoint(latitude, longitude, name, ""));
				}
				AccessibleToast.makeText(mapActivity, MessageFormat.format(getString(R.string.add_waypoint_dialog_added), name), Toast.LENGTH_SHORT)
							.show();
				dialog.dismiss();
			}
		});
		return builder.create();
    }
    
    public void reloadTile(final int zoom, final double latitude, final double longitude){
    	enhance(dialogBundle,latitude,longitude,zoom);
    	mapActivity.showDialog(DIALOG_RELOAD_TITLE);
    }

    
    
    
    protected String getString(int res){
    	return mapActivity.getString(res);
    }
    
    protected void showToast(final String msg){
    	mapActivity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				AccessibleToast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
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
				"Email", "SMS", "Clipboard", "geo:", "QR-Code"
		}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final double latitude = args.getDouble(KEY_LATITUDE);
				final double longitude = args.getDouble(KEY_LONGITUDE);
				final int zoom = args.getInt(KEY_ZOOM);
				try {
					final String shortOsmUrl = MapUtils.buildShortOsmUrl(latitude, longitude, zoom);
					final String appLink = "http://download.osmand.net/go?lat=" + ((float) latitude) + "&lon=" + ((float) longitude) + "&z=" + zoom;
					String sms = mapActivity.getString(R.string.send_location_sms_pattern, shortOsmUrl, appLink);
					if (which == 0) {
						sendEmail(shortOsmUrl, appLink);
					} else if (which == 1) {
						sendSms(sms);
					} else if (which == 2) {
						sendToClipboard(sms);
					} else if (which == 3) {
						sendGeoActivity(latitude, longitude, zoom);
					} else if (which == 4) {
						sendQRCode(latitude, longitude);
					}
				} catch (RuntimeException e) {
					Toast.makeText(mapActivity, R.string.input_output_error, Toast.LENGTH_SHORT).show();
				}				
			}

			
		});
    	return builder.create();
    }
    

	private void sendEmail(final String shortOsmUrl, final String appLink) {
		String email = mapActivity.getString(R.string.send_location_email_pattern, shortOsmUrl, appLink);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_SUBJECT, "Location"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
		intent.setType("text/html");
		mapActivity.startActivity(Intent.createChooser(intent, getString(R.string.send_location)));
	}

	private void sendSms(String sms) {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("sms_body", sms); 
		sendIntent.setType("vnd.android-dir/mms-sms");
		mapActivity.startActivity(sendIntent);
	}

	private void sendToClipboard(String sms) {
		ClipboardManager clipboard = (ClipboardManager) mapActivity.getSystemService(Activity.CLIPBOARD_SERVICE);
		clipboard.setText(sms);
	}

	private void sendGeoActivity(final double latitude, final double longitude, final int zoom) {
		final String simpleGeo = "geo:"+((float) latitude)+","+((float)longitude) +"?z="+zoom;
		Uri location = Uri.parse(simpleGeo);
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
		mapActivity.startActivity(mapIntent);
	}

	private void sendQRCode(final double latitude, final double longitude) {
		Bundle bundle = new Bundle();
		bundle.putFloat("LAT", (float) latitude);
		bundle.putFloat("LONG", (float) longitude);
		Intent intent = new Intent();
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setAction(ZXING_BARCODE_SCANNER_ACTIVITY);
		ResolveInfo resolved = mapActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

		if (resolved != null) {
			intent.putExtra("ENCODE_TYPE", "LOCATION_TYPE");
			intent.putExtra("ENCODE_DATA", bundle);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			mapActivity.startActivity(intent);
		} else {
			if (Version.isMarketEnabled(mapActivity.getMyApplication())) {
				AlertDialog.Builder builder = new AccessibleAlertBuilder(mapActivity);
				builder.setMessage(getString(R.string.zxing_barcode_scanner_not_found));
				builder.setPositiveButton(getString(R.string.default_buttons_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(mapActivity.getMyApplication()) 
								+ ZXING_BARCODE_SCANNER_COMPONENT));
						try {
							mapActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(getString(R.string.default_buttons_no), null);
				builder.show();
			} else {
				Toast.makeText(mapActivity, R.string.zxing_barcode_scanner_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}
    
    protected void aboutRoute() {
    	Intent intent = new Intent(mapActivity, ShowRouteInfoActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mapActivity.startActivity(intent);
    }
    
    
    public String getRoutePointDescription(double lat, double lon) {
    	return mapActivity.getString(R.string.route_descr_lat_lon, lat, lon);
    }
    
    public String getRoutePointDescription(LatLon l, String d) {
    	if(d != null && d.length() > 0) {
    		return d.replace(':', ' ');
    	}
    	if(l != null) {
    		return mapActivity.getString(R.string.route_descr_lat_lon, l.getLatitude(), l.getLongitude());
    	}
    	return "";
    } 
    
	public String generateViaDescription() {
		TargetPointsHelper targets = getTargets();
		String via = "";
		List<String> names = targets.getIntermediatePointNames();
		List<LatLon> points = targets.getIntermediatePoints();
		if (names.size() == 0) {
			return via;
		}
		for (int i = 0; i < points.size() ; i++) {
			via += "\n - " + getRoutePointDescription(points.get(i), i >= names.size() ? "" :names.get(i));
		}
		return mapActivity.getString(R.string.route_via) + via;
	}
	
	public static class DirectionDialogStyle {
		public boolean gpxRouteEnabled;
		public boolean routeToMapPoint;
		public boolean routeFromMapPoint;

		public static DirectionDialogStyle create() {
			return new DirectionDialogStyle();
		}
		public DirectionDialogStyle gpxRouteEnabled() {
			gpxRouteEnabled = true;
			return this;
		}
		
		public DirectionDialogStyle routeToMapPoint() {
			routeToMapPoint = true;
			return this;
		}
		
		public DirectionDialogStyle routeFromMapPoint() {
			routeFromMapPoint = true;
			return this;
		}
	}
    
    
	public static View prepareAppModeView(Activity a, final Set<ApplicationMode> selected, boolean showDefault,
			ViewGroup parent, final View.OnClickListener onClickListener) {
		View view = a.getLayoutInflater().inflate(R.layout.mode_toggles, parent);
		OsmandSettings settings = ((OsmandApplication) a.getApplication()).getSettings();
		boolean lc = settings.isLightContentMenu();
		final ToggleButton[] buttons = new ToggleButton[ApplicationMode.values().length];
		if(showDefault) {
			buttons[ApplicationMode.DEFAULT.ordinal()] = (ToggleButton) view.findViewById(R.id.DefaultButton);
			buttons[ApplicationMode.DEFAULT.ordinal()].setButtonDrawable(R.drawable.ic_browse_map );
		} else {
			view.findViewById(R.id.DefaultButton).setVisibility(View.GONE);
		}
		buttons[ApplicationMode.CAR.ordinal()] = (ToggleButton) view.findViewById(R.id.CarButton);
		buttons[ApplicationMode.CAR.ordinal()].setButtonDrawable(R.drawable.ic_car );
		buttons[ApplicationMode.BICYCLE.ordinal()] = (ToggleButton) view.findViewById(R.id.BicycleButton);
		buttons[ApplicationMode.BICYCLE.ordinal()].setButtonDrawable(R.drawable.ic_bicycle);
		buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (ToggleButton) view.findViewById(R.id.PedestrianButton);
		buttons[ApplicationMode.PEDESTRIAN.ordinal()].setButtonDrawable(R.drawable.ic_pedestrian);
		
		ApplicationMode appMode = settings.getApplicationMode();
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] != null) {
				final int ind = i;
				ToggleButton b = buttons[i];
				final ApplicationMode buttonAppMode = ApplicationMode.values()[i];
				b.setChecked(appMode == buttonAppMode);
				if(appMode == buttonAppMode) {
					selected.add(appMode);
				}
				b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							selected.clear();
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if(ind == j) {
										selected.add(ApplicationMode.values()[j]);
									}
									if (buttons[j].isChecked() != (ind == j)) {
										buttons[j].setChecked(ind == j);
									}
								}
							}
						} else {
							// revert state
							boolean revert = true;
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if (buttons[j].isChecked()) {
										revert = false;
										break;
									}
								}
							}
							if (revert) {
								buttons[ind].setChecked(true);
							}
						}
						if(onClickListener != null) {
							onClickListener.onClick(null);
						}
					}
				});
			}
		}
		return view;
	}
	public void getDirections(final Location mapView, String name, DirectionDialogStyle style) {
		final Location current = getLastKnownLocation();
		Builder builder = new AlertDialog.Builder(mapActivity);
		final TargetPointsHelper targets = getTargets();

		View view = mapActivity.getLayoutInflater().inflate(R.layout.calculate_route, null);
		boolean lc = mapActivity.getMyApplication().getSettings().isLightContentMenu();
		final CheckBox nonoptimal = (CheckBox) view.findViewById(R.id.OptimalCheckox);
		final ToggleButton[] buttons = new ToggleButton[ApplicationMode.values().length];
		buttons[ApplicationMode.CAR.ordinal()] = (ToggleButton) view.findViewById(R.id.CarButton);
		buttons[ApplicationMode.CAR.ordinal()].setButtonDrawable(R.drawable.ic_car );
		buttons[ApplicationMode.BICYCLE.ordinal()] = (ToggleButton) view.findViewById(R.id.BicycleButton);
		buttons[ApplicationMode.BICYCLE.ordinal()].setButtonDrawable(R.drawable.ic_bicycle);
		buttons[ApplicationMode.PEDESTRIAN.ordinal()] = (ToggleButton) view.findViewById(R.id.PedestrianButton);
		buttons[ApplicationMode.PEDESTRIAN.ordinal()].setButtonDrawable(R.drawable.ic_pedestrian);
		
		final Spinner fromSpinner = setupFromSpinner(mapView, name,view, style);
		final List<LatLon> toList = new ArrayList<LatLon>();
		final Spinner toSpinner = setupToSpinner(mapView, name,view, toList, style);
		
		
		String via = generateViaDescription();
		if(via.length() == 0){
			((TextView) view.findViewById(R.id.ViaView)).setVisibility(View.GONE);
		} else {
			((TextView) view.findViewById(R.id.ViaView)).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.ViaView)).setText(via);
		}
		
		ApplicationMode appMode = settings.getApplicationMode();
		if(appMode == ApplicationMode.DEFAULT) {
			appMode = ApplicationMode.CAR;
		}
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] != null) {
				final int ind = i;
				ToggleButton b = buttons[i];
				final ApplicationMode buttonAppMode = ApplicationMode.values()[i];
				b.setChecked(appMode == buttonAppMode);
				if(b.isChecked()) {
					nonoptimal.setChecked(!settings.OPTIMAL_ROUTE_MODE.getModeValue(buttonAppMode));
				}
				b.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						if (isChecked) {
							nonoptimal.setChecked(!settings.OPTIMAL_ROUTE_MODE.getModeValue(buttonAppMode));
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if (buttons[j].isChecked() != (ind == j)) {
										buttons[j].setChecked(ind == j);
									}
								}
							}
						} else {
							// revert state
							boolean revert = true;
							for (int j = 0; j < buttons.length; j++) {
								if (buttons[j] != null) {
									if (buttons[j].isChecked()) {
										revert = false;
										break;
									}
								}
							}
							if (revert) {
								buttons[ind].setChecked(true);
							}
						}
					}
				});
			}
		}

		DialogInterface.OnClickListener onlyShowCall = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LatLon tos = toList.get(toSpinner.getSelectedItemPosition());
				if ( tos != null && tos != targets.getPointToNavigate()) {
					targets.navigateToPoint(tos, false, -1);
				}
				if (!targets.checkPointToNavigate(getMyApplication())) {
					return;
				}
				Location from = fromSpinner.getSelectedItemPosition() == 0 ? current : mapView;
				if (from == null) {
					from = getLastKnownLocation();
				}
				if (from == null) {
					AccessibleToast.makeText(mapActivity, R.string.unknown_from_location, Toast.LENGTH_LONG).show();
					return;
				}

				ApplicationMode mode = getAppMode(buttons, settings);
				routingHelper.setAppMode(mode);
				settings.OPTIMAL_ROUTE_MODE.setModeValue(mode, !nonoptimal.isChecked());
				settings.FOLLOW_THE_ROUTE.set(false);
				settings.FOLLOW_THE_GPX_ROUTE.set(null);
				routingHelper.setFollowingMode(false);
				routingHelper.setFinalAndCurrentLocation(targets.getPointToNavigate(), targets.getIntermediatePoints(), from, null);
			}
		};

		DialogInterface.OnClickListener followCall = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LatLon tos = toList.get(toSpinner.getSelectedItemPosition());
				if ( tos != null && tos != targets.getPointToNavigate()) {
					targets.navigateToPoint(tos, false, -1);
				}
				if (!targets.checkPointToNavigate(getMyApplication())) {
					return;
				}
				boolean msg = true;
				Location lastKnownLocation = getLastKnownLocation();
				Location from = fromSpinner.getSelectedItemPosition() == 0 ? current : mapView;
				if(from == null) {
					from = lastKnownLocation;
				}
				if (OsmAndLocationProvider.isPointAccurateForRouting(lastKnownLocation)) {
					from = lastKnownLocation;
					msg = false;
				}
				if (msg) {
					AccessibleToast.makeText(mapActivity, R.string.route_updated_loc_found, Toast.LENGTH_LONG).show();
				}
				ApplicationMode mode = getAppMode(buttons, settings);
				settings.OPTIMAL_ROUTE_MODE.setModeValue(mode, !nonoptimal.isChecked());
				dialog.dismiss();
				mapActivity.followRoute(mode, targets.getPointToNavigate(), targets.getIntermediatePoints(), 
						from, null);
			}
		};

		DialogInterface.OnClickListener useGpxNavigation = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LatLon tos = toList.get(toSpinner.getSelectedItemPosition());
				if ( tos != null && tos != targets.getPointToNavigate()) {
					targets.navigateToPoint(tos, false, -1);
				}
				ApplicationMode mode = getAppMode(buttons, settings);
				navigateUsingGPX(mode);
			}
		};

		builder.setView(view);
		builder.setTitle(R.string.get_directions);
		builder.setPositiveButton(R.string.follow, followCall);
		builder.setNeutralButton(R.string.only_show, onlyShowCall);
		if (style.gpxRouteEnabled) {
			builder.setNegativeButton(R.string.gpx_navigation, useGpxNavigation);
		} else {
			builder.setNegativeButton(R.string.no_route, null);
		}
		builder.show();
	}

	private Spinner setupFromSpinner(final Location mapView, String name, View view, DirectionDialogStyle style) {
		String currentLocation = mapActivity.getString(R.string.route_descr_current_location);
		ArrayList<String> fromActions = new ArrayList<String>();
		fromActions.add(currentLocation);
		if(mapView != null) {
			String oname = name != null ? name : getRoutePointDescription(mapView.getLatitude(),mapView.getLongitude());
			String mapLocation = mapActivity.getString(R.string.route_descr_map_location) + " " + oname;
			fromActions.add(mapLocation);
		}
		final Spinner fromSpinner = ((Spinner) view.findViewById(R.id.FromSpinner));
		ArrayAdapter<String> fromAdapter = new ArrayAdapter<String>(view.getContext(), 
				android.R.layout.simple_spinner_item, 
				fromActions
				);
		fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fromSpinner.setAdapter(fromAdapter);
		if(style.routeFromMapPoint && mapView != null) {
			fromSpinner.setSelection(1);
		}
		return fromSpinner;
	}
	
	private Spinner setupToSpinner(final Location mapView, String name, View view, List<LatLon> locs, DirectionDialogStyle style) {
		final TargetPointsHelper targets = getTargets();
		ArrayList<String> toActions = new ArrayList<String>();
		if (targets.getPointToNavigate() != null) {
			toActions.add(mapActivity.getString(R.string.route_descr_destination) + " "
					+ getRoutePointDescription(targets.getPointToNavigate(), targets.getPointNavigateDescription()));
			locs.add(targets.getPointToNavigate());
		}
		if(mapView != null) {
			String oname = name != null ? name : getRoutePointDescription(mapView.getLatitude(),mapView.getLongitude());
			String mapLocation = mapActivity.getString(R.string.route_descr_map_location) + " " + oname;
			toActions.add(mapLocation);
			locs.add(new LatLon(mapView.getLatitude(), mapView.getLongitude()));
		}
		if(style.routeToMapPoint) {
			Collections.reverse(locs);
			Collections.reverse(toActions);
		}
		final Spinner toSpinner = ((Spinner) view.findViewById(R.id.ToSpinner));
		ArrayAdapter<String> toAdapter = new ArrayAdapter<String>(view.getContext(), 
				android.R.layout.simple_spinner_item, 
				toActions
				);
		toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		toSpinner.setAdapter(toAdapter);
		return toSpinner;
	}
    
    protected Location getLastKnownLocation() {
		return getMyApplication().getLocationProvider().getLastKnownLocation();
	}

	protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}

    public void navigateUsingGPX(final ApplicationMode appMode) {
		final LatLon endForRouting = mapActivity.getPointToNavigate();
		final MapActivityLayers mapLayers = mapActivity.getMapLayers();
		mapLayers.selectGPXFileLayer(false, false, false, new CallbackWithObject<GPXFile>() {
			
			@Override
			public boolean processResult(final GPXFile result) {
				Builder builder = new AlertDialog.Builder(mapActivity);
				final boolean[] props = new boolean[]{false, false, false, settings.SPEAK_GPX_WPT.get()};
				builder.setMultiChoiceItems(new String[] { getString(R.string.gpx_option_reverse_route),
						getString(R.string.gpx_option_destination_point), getString(R.string.gpx_option_from_start_point),
						getString(R.string.announce_gpx_waypoints) }, props,
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
						boolean announceGpxWpt = props[3];
						settings.SPEAK_GPX_WPT.set(announceGpxWpt);
						GPXRouteParams gpxRoute = new GPXRouteParams(result, reverse, announceGpxWpt, settings);
						
						Location loc = getLastKnownLocation();
						if(passWholeWay && loc != null){
							gpxRoute.setStartPoint(loc);
						}
						
						Location startForRouting = getLastKnownLocation();
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
								getTargets().navigateToPoint(point, false, -1);
							}
						}
						if(endPoint != null){
							mapActivity.followRoute(appMode, endPoint,
									new ArrayList<LatLon>(), startForRouting, gpxRoute);
							settings.FOLLOW_THE_GPX_ROUTE.set(result.path);
						}
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.show();
				return true;
			}
		});
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
	
	public static  Dialog createSaveDirections(Activity activity) {
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		final File fileDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		final Dialog dlg = new Dialog(activity);
		dlg.setTitle(R.string.save_route_dialog_title);
		dlg.setContentView(R.layout.save_directions_dialog);
		final EditText edit = (EditText) dlg.findViewById(R.id.FileNameEdit);
		
		edit.setText("_" + MessageFormat.format("{0,date,yyyy-MM-dd}", new Date()) + "_");
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
					new SaveDirectionsAsyncTask(app).execute(toSave);
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
	
	
	private static class SaveDirectionsAsyncTask extends AsyncTask<File, Void, String> {
		
		private final OsmandApplication app;

		public SaveDirectionsAsyncTask(OsmandApplication app) {
			this.app = app;
		}

		@Override
		protected String doInBackground(File... params) {
			if (params.length > 0) {
				File file = params[0];
				GPXFile gpx = app.getRoutingHelper().generateGPXFileWithRoute();
				GPXUtilities.writeGpxFile(file, gpx, app);
				return app.getString(R.string.route_successfully_saved_at, file.getName());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(result != null){
				AccessibleToast.makeText(app, result, Toast.LENGTH_LONG).show();
			}
		}
		
	}
	
	public void contextMenuPoint(final double latitude, final double longitude, final ContextMenuAdapter iadapter, Object selectedObj) {
		final ContextMenuAdapter adapter = iadapter == null ? new ContextMenuAdapter(mapActivity) : iadapter;

		adapter.item(R.string.context_menu_item_directions_to).icons(
				R.drawable.ic_action_gdirections_dark, R.drawable.ic_action_gdirections_light).reg();
		final TargetPointsHelper targets = getMyApplication().getTargetPointsHelper();
		final OsmandSettings settings = getMyApplication().getSettings();
		if(targets.getPointToNavigate() != null) {
			adapter.item(R.string.context_menu_item_intermediate_point).icons(R.drawable.ic_action_flage_dark,
					R.drawable.ic_action_flage_light).reg();
		// For button-less search UI
		} else {
			adapter.item(R.string.context_menu_item_destination_point).icons(R.drawable.ic_action_flag_dark,
					R.drawable.ic_action_flag_light).reg();
		}
		adapter.item(R.string.context_menu_item_directions_from).icons(R.drawable.ic_action_gdirections_dark, R.drawable.ic_action_gdirections_light).reg();
		adapter.item(R.string.context_menu_item_search).icons(R.drawable.ic_action_search_dark, 
				R.drawable.ic_action_search_light).reg();
		adapter.item(R.string.context_menu_item_share_location).icons(
				R.drawable.ic_action_gshare_dark, R.drawable.ic_action_gshare_light).reg();
		adapter.item(R.string.context_menu_item_add_favorite).icons(
				R.drawable.ic_action_fav_dark, R.drawable.ic_action_fav_light ).reg();
		
		

		OsmandPlugin.registerMapContextMenu(mapActivity, latitude, longitude, adapter, selectedObj);
		final Builder builder = new AlertDialog.Builder(mapActivity);
		ListAdapter listAdapter ;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			listAdapter =
				adapter.createListAdapter(mapActivity, R.layout.list_menu_item, getMyApplication().getSettings().isLightContentMenu());
		} else {
			listAdapter =
				adapter.createListAdapter(mapActivity, R.layout.list_menu_item_native, getMyApplication().getSettings().isLightContentMenu());
		}
		builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				int standardId = adapter.getItemId(which);
				OnContextMenuClick click = adapter.getClickAdapter(which);
				if (click != null) {
					click.onContextMenuClick(standardId, which, false, dialog);
				} else if (standardId == R.string.context_menu_item_search) {
					Intent intent = new Intent(mapActivity, OsmandIntents.getSearchActivity());
					intent.putExtra(SearchActivity.SEARCH_LAT, latitude);
					intent.putExtra(SearchActivity.SEARCH_LON, longitude);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					mapActivity.startActivity(intent);
				} else if (standardId == R.string.context_menu_item_directions_to) {
					if(!routingHelper.isRouteBeingCalculated() && !routingHelper.isRouteCalculated() ) {
						Location loc = new Location("map");
						loc.setLatitude(latitude);
						loc.setLongitude(longitude);
						String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
						getDirections(loc, name, DirectionDialogStyle.create().gpxRouteEnabled().routeToMapPoint());
					} else {
						String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
						targets.navigateToPoint(new LatLon(latitude, longitude), true, -1, name);
					}
				} else if (standardId == R.string.context_menu_item_directions_from) {
					if (targets.checkPointToNavigate(getMyApplication())) {
						Location loc = new Location("map");
						loc.setLatitude(latitude);
						loc.setLongitude(longitude);
						String name = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
						getDirections(loc, name, DirectionDialogStyle.create().gpxRouteEnabled().routeFromMapPoint());
					}
				} else if (standardId == R.string.context_menu_item_intermediate_point || 
						standardId == R.string.context_menu_item_destination_point) {
					boolean dest = standardId == R.string.context_menu_item_destination_point;
					String selected = mapActivity.getMapLayers().getContextMenuLayer().getSelectedObjectName();
					if(selected == null){
						selected = mapActivity.getString(R.string.route_descr_lat_lon, latitude, longitude);
					}
					targets.navigateToPoint(new LatLon(latitude, longitude), true, 
							dest ? -1 : targets.getIntermediatePoints().size(), selected);
					IntermediatePointsDialog.openIntermediatePointsDialog(mapActivity);
				} else if (standardId == R.string.context_menu_item_share_location) {
					shareLocation(latitude, longitude, mapActivity.getMapView().getZoom());
				} else if (standardId == R.string.context_menu_item_add_favorite) {
					addFavouritePoint(latitude, longitude);
				}
			}
		});
		builder.create().show();
	}
	
	public void contextMenuPoint(final double latitude, final double longitude){
		contextMenuPoint(latitude, longitude, null, null);
	}
	
	private Dialog createReloadTitleDialog(final Bundle args) {
    	Builder builder = new AccessibleAlertBuilder(mapActivity);
    	builder.setMessage(R.string.context_menu_item_update_map_confirm);
    	builder.setNegativeButton(R.string.default_buttons_cancel, null);
    	final OsmandMapTileView mapView = mapActivity.getMapView();
    	builder.setPositiveButton(R.string.context_menu_item_update_map, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int zoom = args.getInt(KEY_ZOOM);
				BaseMapLayer mainLayer = mapView.getMainLayer();
				if(!(mainLayer instanceof MapTileLayer) || !((MapTileLayer) mainLayer).isVisible()){
					AccessibleToast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
					return;
				}
				final ITileSource mapSource = ((MapTileLayer) mainLayer).getMap();
				if(mapSource == null || !mapSource.couldBeDownloadedFromInternet()){
					AccessibleToast.makeText(mapActivity, R.string.maps_could_not_be_downloaded, Toast.LENGTH_SHORT).show();
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
		return builder.create();
    }
	
	

	@Override
	public Dialog onCreateDialog(int id) {
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_ADD_FAVORITE:
				return createAddFavouriteDialog(mapActivity, args);
			case DIALOG_REPLACE_FAVORITE:
				return createReplaceFavouriteDialog(mapActivity, args);
			case DIALOG_ADD_WAYPOINT:
				return createAddWaypointDialog(args);
			case DIALOG_RELOAD_TITLE:
				return createReloadTitleDialog(args);
			case DIALOG_SHARE_LOCATION:
				return createShareLocationDialog(args);
			case DIALOG_SAVE_DIRECTIONS:
				return createSaveDirections(mapActivity);
		}
		return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		Bundle args = dialogBundle;
		switch (id) {
		case DIALOG_ADD_FAVORITE:
			prepareAddFavouriteDialog(mapActivity, dialog, args,
					args.getDouble(KEY_LATITUDE), args.getDouble(KEY_LONGITUDE),args.getString(KEY_NAME));
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
		}
	}
	
	
	public AlertDialog openOptionsMenuAsList() {
		final ContextMenuAdapter cm = createOptionsMenu();
		final Builder bld = new AlertDialog.Builder(mapActivity);
		ListAdapter listAdapter ;
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			listAdapter =
				cm.createListAdapter(mapActivity, R.layout.list_menu_item, getMyApplication().getSettings().isLightContentMenu());
		} else {
			listAdapter =
				cm.createListAdapter(mapActivity, R.layout.list_menu_item_native, getMyApplication().getSettings().isLightContentMenu());
		}
		bld.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OnContextMenuClick click = cm.getClickAdapter(which);
				if (click != null) {
					click.onContextMenuClick(cm.getItemId(which), which, false, dialog);
				}
			}
		});
		return bld.show();

	}

	private ContextMenuAdapter createOptionsMenu() {
		final OsmandMapTileView mapView = mapActivity.getMapView();
		final OsmandApplication app = mapActivity.getMyApplication();
		ContextMenuAdapter optionsMenuHelper = new ContextMenuAdapter(app);
		
		// 1. Where am I
		optionsMenuHelper.item(R.string.where_am_i).
				icons(R.drawable.ic_action_gloc_dark, R.drawable.ic_action_gloc_light)
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						if (getMyApplication().getInternalAPI().accessibilityEnabled()) {
							whereAmIDialog();
						} else {
							mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
						}
					}
				}).reg();

		// 2-4. Navigation related (directions, mute, cancel navigation)
		boolean muteVisible = routingHelper.getFinalLocation() != null && routingHelper.isFollowingMode();
		if (muteVisible) {
			boolean mute = routingHelper.getVoiceRouter().isMute();
			int t = mute ? R.string.menu_mute_on : R.string.menu_mute_off;
			int icon;
			int iconLight;
			if(mute) {
				icon = R.drawable.a_10_device_access_volume_muted_dark;
				iconLight = R.drawable.a_10_device_access_volume_muted_light;
			} else{
				icon = R.drawable.a_10_device_access_volume_on_dark;
				iconLight = R.drawable.a_10_device_access_volume_on_light;
			}
			optionsMenuHelper.item(t).icons(icon, iconLight)
				.listen(new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					routingHelper.getVoiceRouter().setMute(!routingHelper.getVoiceRouter().isMute());
				}
			}).reg();
		}
		optionsMenuHelper.item(routingHelper.isRouteCalculated() ? R.string.show_route: R.string.get_directions)
				.icons(R.drawable.ic_action_gdirections_dark, R.drawable.ic_action_gdirections_light)
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						if (routingHelper.isRouteCalculated()) {
							aboutRoute();
						} else {
							Location loc = new Location("map");
							loc.setLatitude(mapView.getLatitude());
							loc.setLongitude(mapView.getLongitude());
							getDirections(loc, null, DirectionDialogStyle.create().gpxRouteEnabled());
						}						
					}
				}).reg();
		if (getTargets().getPointToNavigate() != null) {
			optionsMenuHelper.item(R.string.target_points).icons(R.drawable.ic_action_flage_dark, R.drawable.ic_action_flage_light)
					.listen(new OnContextMenuClick() {
						@Override
						public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
							openIntermediatePointsDialog();
						}
					}).reg();
		}
		if (mapActivity.getPointToNavigate() != null) {
			int nav;
			if(routingHelper.isFollowingMode()) {
				nav = R.string.cancel_navigation;
			} else if(routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated()) {
				nav = R.string.cancel_route;
			} else {
				nav = R.string.clear_destination;
			}
			optionsMenuHelper.item(nav).icons(R.drawable.ic_action_remove_dark, R.drawable.ic_action_remove_light) 
				.listen(new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					stopNavigationActionConfirm(mapView);
				}
			}).reg();
		}
		
		// 5-9. Default actions (Layers, Configure Map screen, Settings, Search, Favorites) 
		optionsMenuHelper.item(R.string.menu_layers).icons(R.drawable.ic_action_layers_dark, R.drawable.ic_action_layers_light) 
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						mapActivity.getMapLayers().openLayerSelectionDialog(mapView);
					}
				}).reg();

		optionsMenuHelper.item(R.string.layer_map_appearance).icons(R.drawable.ic_action_settings_dark, R.drawable.ic_action_settings_light) 
			.listen(new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					mapActivity.getMapLayers().getMapInfoLayer().openViewConfigureDialog();
				}
			}).reg();

		optionsMenuHelper.item(R.string.settings_Button).icons(R.drawable.ic_action_settings2_dark, R.drawable.ic_action_settings2_light)
				.listen(new OnContextMenuClick() {
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						final Intent intentSettings = new Intent(mapActivity, OsmandIntents.getSettingsActivity());
						mapActivity.startActivity(intentSettings);
					}
				}).reg();

		optionsMenuHelper.item(R.string.search_button).icons(R.drawable.ic_action_search_dark, R.drawable.ic_action_search_light)
				.listen(new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				Intent newIntent = new Intent(mapActivity, OsmandIntents.getSearchActivity());
				// causes wrong position caching:  newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				LatLon loc = mapActivity.getMapLocation();
				newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
				newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapActivity.startActivity(newIntent);
			}
		}).reg();
		
		optionsMenuHelper.item(R.string.favorites_Button).icons( R.drawable.ic_action_fav_dark, R.drawable.ic_action_fav_light)
				.listen(new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				Intent newIntent = new Intent(mapActivity, OsmandIntents.getFavoritesActivity());
				// causes wrong position caching:  newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				mapActivity.startActivity(newIntent);
			}
		}).reg();
		optionsMenuHelper.item(R.string.show_point_options).icons(R.drawable.ic_action_marker_dark, R.drawable.ic_action_marker_light )
				.listen(new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			}
		}).reg();
		//////////// Others
		if (Version.isGpsStatusEnabled(app)) {
			optionsMenuHelper.item(R.string.show_gps_status).icons(R.drawable.ic_action_gabout_dark, R.drawable.ic_action_gabout_light )
				.listen(new OnContextMenuClick() {

				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					startGpsStatusIntent();
				}
			}).reg();
		}
		final OsmAndLocationProvider loc = app.getLocationProvider();
		if (app.getTargetPointsHelper().getPointToNavigate() != null) {
			
			optionsMenuHelper.item(loc.getLocationSimulation().isRouteAnimating() ? R.string.animate_route_off
					: R.string.animate_route).icons(R.drawable.ic_action_play_dark, R.drawable.ic_action_play_light ) 
						.listen(	new OnContextMenuClick() {

				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					// animate moving on route
					loc.getLocationSimulation().startStopRouteAnimation(mapActivity);
				}
			}).reg();
		}
		OsmandPlugin.registerOptionsMenu(mapActivity, optionsMenuHelper);
		optionsMenuHelper.item(R.string.exit_Button).icons(R.drawable.ic_action_quit_dark, R.drawable.ic_action_quit_light )
					.listen(new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				// 1. Work for almost all cases when user open apps from main menu
				Intent newIntent = new Intent(mapActivity, OsmandIntents.getMainMenuActivity());
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				newIntent.putExtra(MainMenuActivity.APP_EXIT_KEY, MainMenuActivity.APP_EXIT_CODE);
				mapActivity.startActivity(newIntent);
				// In future when map will be main screen this should change
				// app.closeApplication(mapActivity);
			}
		}).reg();
		return optionsMenuHelper;
	}
	
	public void openIntermediatePointsDialog(){
		IntermediatePointsDialog.openIntermediatePointsDialog(mapActivity);
	}
	
	public void stopNavigationAction(final OsmandMapTileView mapView) {
		if (routingHelper.isRouteCalculated() || routingHelper.isFollowingMode() || routingHelper.isRouteBeingCalculated()) {
			routingHelper.setFinalAndCurrentLocation(null, new ArrayList<LatLon>(), getLastKnownLocation(),
					routingHelper.getCurrentGPXRoute());
			// restore default mode
			settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
		} else {
			getTargets().clearPointToNavigate(true);
			mapView.refreshMap();
		}
		
	}

	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}
	
	public void stopNavigationActionConfirm(final OsmandMapTileView mapView){
		Builder builder = new AlertDialog.Builder(mapActivity);
		
		if (routingHelper.isRouteCalculated() || routingHelper.isFollowingMode() || routingHelper.isRouteBeingCalculated()) {
			// Stop the navigation
			builder.setTitle(getString(R.string.cancel_route));
			builder.setMessage(getString(R.string.stop_routing_confirm));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(getMyApplication().getLocationProvider().getLocationSimulation().isRouteAnimating()) {
						getMyApplication().getLocationProvider().getLocationSimulation().startStopRouteAnimation(mapActivity);
					}
					routingHelper.setFinalAndCurrentLocation(null, new ArrayList<LatLon>(), getLastKnownLocation(),
							routingHelper.getCurrentGPXRoute());
					settings.APPLICATION_MODE.set(settings.DEFAULT_APPLICATION_MODE.get());
				}
			});
		} else {
			// Clear the destination point
			builder.setTitle(getString(R.string.cancel_navigation));
			builder.setMessage(getString(R.string.clear_dest_confirm));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getTargets().clearPointToNavigate(true);
					mapView.refreshMap();
				}
			});
		}

		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.show();
	}
	

	private void startGpsStatusIntent() {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(GPS_STATUS_COMPONENT,
				GPS_STATUS_ACTIVITY));
		ResolveInfo resolved = mapActivity.getPackageManager().resolveActivity(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (resolved != null) {
			mapActivity.startActivity(intent);
		} else {
			if (Version.isMarketEnabled(getMyApplication())) {
				AlertDialog.Builder builder = new AccessibleAlertBuilder(mapActivity);
				builder.setMessage(getString(R.string.gps_status_app_not_found));
				builder.setPositiveButton(getString(R.string.default_buttons_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(getMyApplication()) + GPS_STATUS_COMPONENT));
						try {
							mapActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(getString(R.string.default_buttons_no), null);
				builder.show();
			} else {
				Toast.makeText(mapActivity, R.string.gps_status_app_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}

	private void whereAmIDialog() {
		final List<String> items = new ArrayList<String>();
		items.add(getString(R.string.show_location));
		items.add(getString(R.string.show_details));
		AlertDialog.Builder menu = new AlertDialog.Builder(mapActivity);
		menu.setItems(items.toArray(new String[items.size()]), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				dialog.dismiss();
				switch (item) {
				case 0:
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
					break;
				case 1:
					OsmAndLocationProvider locationProvider = getMyApplication().getLocationProvider();
					locationProvider.showNavigationInfo(mapActivity.getPointToNavigate(), mapActivity);
					break;
				default:
					break;
				}
			}
		});
		menu.show();
	}
	
	public static void createDirectionsActions(final QuickAction qa , final LatLon location, final Object obj, final String name, 
    		final int z, final Activity activity, final boolean saveHistory, final OnClickListener onShow) {
		createDirectionsActions(qa, location, obj, name, z, activity, saveHistory, onShow, true);
	}
    
    public static void createDirectionsActions(final QuickAction qa , final LatLon location, final Object obj, final String name, 
    		final int z, final Activity activity, final boolean saveHistory, final OnClickListener onShow, boolean favorite) {

		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();

		ActionItem setAsDestination = new ActionItem();
		setAsDestination.setIcon(activity.getResources().getDrawable(R.drawable.ic_action_gdirections_light));
		setAsDestination.setTitle(activity.getString(R.string.get_directions));
		setAsDestination.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onShow != null) {
					onShow.onClick(v);
				}
				qa.dismiss();
				MapActivityActions.directionsToDialogAndLaunchMap(activity, location.getLatitude(), location.getLongitude(), name);
			}
		});
		qa.addActionItem(setAsDestination);

		ActionItem intermediate = new ActionItem();
		if (targetPointsHelper.getPointToNavigate() != null) {
			intermediate.setIcon(activity.getResources().getDrawable(R.drawable.ic_action_flage_light));
			intermediate.setTitle(activity.getString(R.string.context_menu_item_intermediate_point));
		} else {
			intermediate.setIcon(activity.getResources().getDrawable(R.drawable.ic_action_flag_light));
			intermediate.setTitle(activity.getString(R.string.context_menu_item_destination_point));
		}
		intermediate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onShow != null) {
					onShow.onClick(v);
				}
				addWaypointDialogAndLaunchMap(activity, location.getLatitude(), location.getLongitude(), name);
				qa.dismiss();
			}
		});
		qa.addActionItem(intermediate);

		ActionItem showOnMap = new ActionItem();
		showOnMap.setIcon(activity.getResources().getDrawable(R.drawable.ic_action_marker_light));
		showOnMap.setTitle(activity.getString(R.string.show_poi_on_map));
		showOnMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onShow != null) {
					onShow.onClick(v);
				}
				app.getSettings().setMapLocationToShow(location.getLatitude(), location.getLongitude(), z, saveHistory ? name : null, name,
						obj); //$NON-NLS-1$
				MapActivity.launchMapActivityMoveToTop(activity);
				qa.dismiss();
			}
		});
		qa.addActionItem(showOnMap);

		if (favorite) {
			ActionItem addToFavorite = new ActionItem();
			addToFavorite.setIcon(activity.getResources().getDrawable(R.drawable.ic_action_fav_light));
			addToFavorite.setTitle(activity.getString(R.string.add_to_favourite));
			addToFavorite.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onShow != null) {
						onShow.onClick(v);
					}
					qa.dismiss();
					Bundle args = new Bundle();
					Dialog dlg = createAddFavouriteDialog(activity, args);
					dlg.show();
					prepareAddFavouriteDialog(activity, dlg, args, location.getLatitude(), location.getLongitude(), name);

				}
			});
			qa.addActionItem(addToFavorite);
		}
	}
    
    public static void directionsToDialogAndLaunchMap(final Activity act, final double lat, final double lon, final String name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getIntermediatePoints().size() > 0) {
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
							ctx.getSettings().navigateDialog();
							targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			ctx.getSettings().navigateDialog();
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(act);
		}
	}
    
    
	public static void addWaypointDialogAndLaunchMap(final Activity act, final double lat, final double lon, final String name) {
		final OsmandApplication ctx = (OsmandApplication) act.getApplication();
		final TargetPointsHelper targetPointsHelper = ctx.getTargetPointsHelper();
		if (targetPointsHelper.getPointToNavigate() != null) {
			Builder builder = new AlertDialog.Builder(act);
			builder.setTitle(R.string.new_destination_point_dialog);
			builder.setItems(
					new String[] { act.getString(R.string.replace_destination_point),
							act.getString(R.string.add_as_first_destination_point), act.getString(R.string.add_as_last_destination_point) },
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (which == 0) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
							} else if (which == 2) {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, targetPointsHelper.getIntermediatePoints().size(), name);
							} else {
								targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, 0, name);
							}
							MapActivity.launchMapActivityMoveToTop(act);
						}
					});
			builder.show();
		} else {
			targetPointsHelper.navigateToPoint(new LatLon(lat, lon), true, -1, name);
			MapActivity.launchMapActivityMoveToTop(act);
		}
	}
    
}
