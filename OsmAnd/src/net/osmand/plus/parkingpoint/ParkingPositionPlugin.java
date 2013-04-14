package net.osmand.plus.parkingpoint;


import java.util.Calendar;
import java.util.EnumSet;

import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OptionsMenuHelper;
import net.osmand.plus.OptionsMenuHelper.OnOptionsMenuClick;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.BaseMapWidget;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.location.Location;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * 
 * The plugin facilitates a storage of the location of a parked car.
 * 
 * @author Alena Fedasenka
 */
public class ParkingPositionPlugin extends OsmandPlugin {

	public static final String ID = "osmand.parking.position";
	public final static String PARKING_POINT_LAT = "parking_point_lat"; //$NON-NLS-1$
	public final static String PARKING_POINT_LON = "parking_point_lon"; //$NON-NLS-1$
	public final static String PARKING_TYPE = "parking_type"; //$NON-NLS-1$
	public final static String PARKING_TIME = "parking_limit_time"; //$//$NON-NLS-1$
	public final static String PARKING_START_TIME = "parking_time"; //$//$NON-NLS-1$
	public final static String PARKING_EVENT_ADDED = "parking_event_added"; //$//$NON-NLS-1$
	private OsmandApplication app;

	private ParkingPositionLayer parkingLayer;
	private BaseMapWidget parkingPlaceControl;
	private final CommonPreference<Float> parkingLat;
	private final CommonPreference<Float> parkingLon;
	private CommonPreference<Boolean> parkingType;
	private CommonPreference<Boolean> parkingEvent;
	private CommonPreference<Long> parkingTime;
	private CommonPreference<Long> parkingStartTime;

	public ParkingPositionPlugin(OsmandApplication app) {
		this.app = app;
		OsmandSettings set = app.getSettings();
		parkingLat = set.registerFloatPreference(PARKING_POINT_LAT, 0f).makeGlobal();
		parkingLon = set.registerFloatPreference(PARKING_POINT_LON, 0f).makeGlobal();
		parkingType = set.registerBooleanPreference(PARKING_TYPE, false).makeGlobal();
		parkingEvent = set.registerBooleanPreference(PARKING_EVENT_ADDED, false).makeGlobal();
		parkingTime = set.registerLongPreference(PARKING_TIME, -1).makeGlobal();
		parkingStartTime = set.registerLongPreference(PARKING_START_TIME, -1).makeGlobal();
	}
	
	
	
	public LatLon getParkingPosition() {
		float lat = parkingLat.get();
		float lon = parkingLon.get();
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}
	
	public boolean getParkingType() {
		return parkingType.get();
	}
	
	public boolean isParkingEventAdded() {
		return parkingEvent.get();
	}
	
	public boolean addOrRemoveParkingEvent(boolean added) {
		return parkingEvent.set(added);
	}
	
	public long getParkingTime() {
		return parkingTime.get();
	}

	public long getStartParkingTime() {
		return parkingStartTime.get();
	}
	
	public boolean clearParkingPosition() {
		parkingLat.resetToDefault();
		parkingLon.resetToDefault();
		parkingType.resetToDefault();
		parkingTime.resetToDefault();
		parkingEvent.resetToDefault();
		parkingStartTime.resetToDefault();
		return true;
	}

	public boolean setParkingPosition(double latitude, double longitude) {
		parkingLat.set((float)latitude);
		parkingLon.set((float)longitude);
		return true;
	}
	
	public boolean setParkingType(boolean limited) {
		if (!limited)
			parkingTime.set(-1l);
		parkingType.set(limited);
		return true;
	}
	
	public boolean setParkingTime(long timeInMillis) {
		parkingTime.set(timeInMillis);
		return true;
	}
	
	public boolean setParkingStartTime(long timeInMillis) {		
		parkingStartTime.set(timeInMillis);
		return true;
	}


	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_parking_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_parking_plugin_name);
	}

	@Override
	public void registerLayers(MapActivity activity) {
		// remove old if existing after turn
		if(parkingLayer != null) {
			activity.getMapView().removeLayer(parkingLayer);
		}
		parkingLayer = new ParkingPositionLayer(activity, this);
		activity.getMapView().addLayer(parkingLayer, 4.5f);
		registerWidget(activity);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (parkingLayer == null) {
			registerLayers(activity);
		}
		if (parkingPlaceControl == null) {
			registerWidget(activity);
		}
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			parkingPlaceControl = createParkingPlaceInfoControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(parkingPlaceControl,
					R.drawable.widget_parking, R.string.map_widget_parking, "parking", false,
					EnumSet.allOf(ApplicationMode.class), EnumSet.noneOf(ApplicationMode.class), 8);
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
			final double latitude, final double longitude,
			ContextMenuAdapter adapter, Object selectedObj) {
		boolean isParkingSelected = false;
		LatLon parkingPosition = getParkingPosition();
		if (selectedObj instanceof LatLon && parkingLayer != null && parkingPosition != null) {
			LatLon point = (LatLon)selectedObj;	
			if ((point.getLatitude() == parkingPosition.getLatitude()) && (point.getLongitude() == parkingPosition.getLongitude()))
				isParkingSelected = true;
		}
		if (isParkingSelected) {
			OnContextMenuClick removeListener = new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int resId, int pos,
						boolean isChecked, DialogInterface dialog) {
					if ((resId == R.string.context_menu_item_delete_parking_point)) {
						showDeleteDialog(mapActivity);
					}
				}
			};
			if (parkingPosition != null)
				adapter.registerItem(R.string.context_menu_item_delete_parking_point, R.drawable.list_activities_parking_poi_remove, removeListener, 0);
		}
		
		OnContextMenuClick addListener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos,
					boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_parking_point) {
					showAddParkingDialog(mapActivity, latitude, longitude);
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_parking_point, R.drawable.list_activities_parking_poi_add, addListener, -1);
		
	}

	/**
	 * Method dialog for adding of a parking location.
	 * It allows user to choose a type of parking (time-limited or time-unlimited).
	 */
	private void showAddParkingDialog(final MapActivity mapActivity, final double latitude, final double longitude) {
		final boolean wasEventPreviouslyAdded = isParkingEventAdded();
		final View addParking = mapActivity.getLayoutInflater().inflate(R.layout.parking_set_type, null);
		final Dialog choose = new Dialog(mapActivity);
		choose.setContentView(addParking);
		choose.setCancelable(true);
		choose.setTitle(mapActivity.getString(R.string.osmand_parking_choose_type));		
		
		ImageButton limitButton = (ImageButton) addParking.findViewById(R.id.parking_lim_button);
		limitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (wasEventPreviouslyAdded) {
					showDeleteEventWarning(mapActivity);
				}
				setParkingPosition(mapActivity, latitude, longitude, true);
				showSetTimeLimitDialog(mapActivity, choose);
				mapActivity.getMapView().refreshMap();
			}
		});

		ImageButton noLimitButton = (ImageButton) addParking.findViewById(R.id.parking_no_lim_button);
		noLimitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				choose.dismiss();
				if (wasEventPreviouslyAdded) {
					showDeleteEventWarning(mapActivity);
				}
				addOrRemoveParkingEvent(false);
				setParkingPosition(mapActivity, latitude, longitude, false);
				mapActivity.getMapView().refreshMap();
			}
		});

		choose.show();

	}
	
	/**
	 * Method creates confirmation dialog for deletion of a parking location.
	 */
	private void showDeleteDialog(final MapActivity mapActivity) {
		Builder confirm = new AlertDialog.Builder(mapActivity);
		confirm.setTitle(mapActivity.getString(R.string.osmand_parking_delete));
		confirm.setMessage(mapActivity.getString(R.string.osmand_parking_delete_confirm));
		confirm.setCancelable(true);
		confirm.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showDeleteEventWarning(mapActivity);
				if(parkingLayer != null) {
					parkingLayer.removeParkingPoint();
				}
				clearParkingPosition();
				mapActivity.getMapView().refreshMap();
			}
		});
		confirm.setNegativeButton(R.string.default_buttons_cancel, null);
		confirm.show();
	}
	
	/**
	 * Opens the dialog to set a time limit for time-limited parking.
	 * The dialog has option to add a notification to Calendar app. 
	 * Anyway the time-limit can be seen from parking point description.
	 * @param mapActivity
	 * @param choose 
	 */
	private void showSetTimeLimitDialog(final MapActivity mapActivity, final Dialog choose) {
		final View setTimeParking = mapActivity.getLayoutInflater().inflate(R.layout.parking_set_time_limit, null);
		Builder setTime = new AlertDialog.Builder(mapActivity);
		setTime.setView(setTimeParking);
		setTime.setTitle(mapActivity.getString(R.string.osmand_parking_time_limit_title));
		setTime.setNegativeButton(R.string.default_buttons_cancel, null);
		final TextView  textView = (TextView) setTimeParking.findViewById(R.id.parkTime);
		final TimePicker timePicker = (TimePicker) setTimeParking.findViewById(R.id.parking_time_picker);
		
		timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
			private static final int TIME_PICKER_INTERVAL = 5;
			private boolean mIgnoreEvent = false;
			private Calendar cal = Calendar.getInstance();
			
			@Override
			public void onTimeChanged(TimePicker timePicker, int hourOfDay, int minute) {
		        if (mIgnoreEvent) {
		            return;
		        }
		        if (minute%TIME_PICKER_INTERVAL != 0) {
		            int minuteFloor=minute-(minute%TIME_PICKER_INTERVAL);
		            minute=minuteFloor + (minute == minuteFloor + 1 ? TIME_PICKER_INTERVAL : 0);
		            if (minute == 60) {
		                minute = 0;
		            }
		            mIgnoreEvent = true;
		            timePicker.setCurrentMinute(minute);
		            mIgnoreEvent = false;
		            long timeInMillis = cal.getTimeInMillis() + hourOfDay*60*60*1000+ minute*60*1000;
		            textView.setText(mapActivity.getString(R.string.osmand_parking_position_description_add) 
		            		+ " "+  parkingLayer.getFormattedTime(timeInMillis));
		        }

		    }
		});
		
		
		//to set the same 24-hour or 12-hour mode as it is set in the device
		timePicker.setIs24HourView(true);
		timePicker.setCurrentHour(0);
		timePicker.setCurrentMinute(0);
		
		setTime.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				choose.dismiss();
				Calendar cal = Calendar.getInstance();
				boolean is24HourFormat = DateFormat.is24HourFormat(app);
				int hour = cal.get(is24HourFormat ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
				int minute = cal.get(Calendar.MINUTE);
				cal.set(is24HourFormat ? Calendar.HOUR_OF_DAY : Calendar.HOUR, hour + timePicker.getCurrentHour());
				cal.set(Calendar.MINUTE, minute + timePicker.getCurrentMinute());
				setParkingTime(cal.getTimeInMillis());
				CheckBox addCalendarEvent = (CheckBox) setTimeParking.findViewById(R.id.check_event_in_calendar);
				if (addCalendarEvent.isChecked()) {
					addCalendarEvent(setTimeParking);
					addOrRemoveParkingEvent(true);
				} else {
					addOrRemoveParkingEvent(false);
				}
			}
		});
		setTime.create();
		setTime.show();
	}
	
	/**
	 * Opens a Calendar app with added notification to pick up the car from time-limited parking.
	 * @param view
	 */
	private void addCalendarEvent(View view) { 
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event"); //$NON-NLS-1$
		intent.putExtra("calendar_id", 1); //$NON-NLS-1$
		intent.putExtra("title", view.getContext().getString(R.string.osmand_parking_event)); //$NON-NLS-1$
		intent.putExtra("beginTime", getParkingTime()); //$NON-NLS-1$
		intent.putExtra("endTime", getParkingTime()+60*60*1000); //$NON-NLS-1$
		view.getContext().startActivity(intent);
	}

	/**
	 * Method shows warning, if previously the event for time-limited parking was added to Calendar app.
	 * @param mapActivity
	 */
	private void showDeleteEventWarning(final MapActivity mapActivity) {
		if (isParkingEventAdded()) {
			Builder deleteEventWarning = new AlertDialog.Builder(mapActivity);
			deleteEventWarning.setTitle(mapActivity.getString(R.string.osmand_parking_warning));
			deleteEventWarning.setMessage(mapActivity.getString(R.string.osmand_parking_warning_text));
			deleteEventWarning.setNeutralButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {						
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			deleteEventWarning.create();
			deleteEventWarning.show();
		}
	}

	/**
	 * Method sets a parking point on a ParkingLayer.
	 * @param mapActivity
	 * @param latitude
	 * @param longitude
	 * @param isLimited
	 */
	private void setParkingPosition(final MapActivity mapActivity, final double latitude, final double longitude, boolean isLimited) {
		setParkingPosition(latitude, longitude);
		setParkingType(isLimited);
		setParkingStartTime(Calendar.getInstance().getTimeInMillis());
		if (parkingLayer != null) {
			parkingLayer.setParkingPointOnLayer(new LatLon(latitude, longitude), isLimited);
		}
	}
	
	@Override
	public void registerOptionsMenuItems(final MapActivity mapActivity, OptionsMenuHelper helper) {
		if (parkingLayer != null) {
			//NOTE: R.id.parking_lim_text - is used just as a stub
			helper.registerOptionsMenuItem(R.string.osmand_parking_delete, R.string.osmand_parking_delete, android.R.drawable.ic_menu_mylocation, 
					new OnOptionsMenuClick() {
						@Override
						public void prepareOptionsMenu(Menu menu, MenuItem deleteParkingItem) {
							if (getParkingPosition() != null) {
								deleteParkingItem.setVisible(true);
							} else {
								deleteParkingItem.setVisible(false);
							}
						}
						
						@Override
						public boolean onClick(MenuItem item) {
							showDeleteDialog(mapActivity);
							return true;
						}
					});
		}
	}
	
	/**
	 * @return the control to be added on a MapInfoLayer 
	 * that shows a distance between 
	 * the current position on the map 
	 * and the location of the parked car
	 */
	private TextInfoWidget createParkingPlaceInfoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		TextInfoWidget parkingPlaceControl = new TextInfoWidget(map, 0, paintText, paintSubText) {
			private float[] calculations = new float[1];
			private int cachedMeters = 0;			
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				LatLon parkingPoint = parkingLayer.getParkingPoint();
				if( parkingPoint != null && !map.getRoutingHelper().isFollowingMode()) {
					OsmandMapTileView view = map.getMapView();
					int d = 0;
					if (d == 0) {
						Location.distanceBetween(view.getLatitude(), view.getLongitude(), parkingPoint.getLatitude(), parkingPoint.getLongitude(), calculations);
						d = (int) calculations[0];
					}
					if (distChanged(cachedMeters, d)) {
						cachedMeters = d;
						if (cachedMeters <= 20) {
							cachedMeters = 0;
							setText(null, null);
						} else {
							String ds = OsmAndFormatter.getFormattedDistance(cachedMeters, map.getMyApplication());
							int ls = ds.lastIndexOf(' ');
							if (ls == -1) {
								setText(ds, null);
							} else {
								setText(ds.substring(0, ls), ds.substring(ls + 1));
							}
						}
						return true;
					}
				} else if (cachedMeters != 0) {
					cachedMeters = 0;
					setText(null, null);
					return true;
				}
				return false;
			}		
			
			/**
			 * Utility method.
			 * @param oldDist
			 * @param dist
			 * @return
			 */
			private boolean distChanged(int oldDist, int dist){
				if(oldDist != 0 && Math.abs(oldDist - dist) < 30){
					return false;
				}
				return true;
			}
		};
		parkingPlaceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandMapTileView view = map.getMapView();
				AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
				LatLon parkingPoint = getParkingPosition();
				if (parkingPoint != null) {
					float fZoom = view.getFloatZoom() < 15 ? 15 : view.getFloatZoom();
					thread.startMoving(parkingPoint.getLatitude(), parkingPoint.getLongitude(), fZoom, true);
				}
			}
		});
		parkingPlaceControl.setText(null, null);
		parkingPlaceControl.setImageDrawable(map.getResources().getDrawable(R.drawable.widget_parking));
		return parkingPlaceControl;
	}
}
