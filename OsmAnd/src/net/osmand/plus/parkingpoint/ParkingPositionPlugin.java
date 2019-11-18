package net.osmand.plus.parkingpoint;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.ItemClickListener;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;

import java.util.Calendar;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC;

/**
 * 
 * The plugin facilitates a storage of the location of a parked car.
 * 
 * @author Alena Fedasenka
 */
public class ParkingPositionPlugin extends OsmandPlugin {

	public static final String ID = "osmand.parking.position";
	public static final String PARKING_PLUGIN_COMPONENT = "net.osmand.parkingPlugin"; //$NON-NLS-1$
	public final static String PARKING_POINT_LAT = "parking_point_lat"; //$NON-NLS-1$
	public final static String PARKING_POINT_LON = "parking_point_lon"; //$NON-NLS-1$
	public final static String PARKING_TYPE = "parking_type"; //$NON-NLS-1$
	public final static String PARKING_TIME = "parking_limit_time"; //$//$NON-NLS-1$
	public final static String PARKING_START_TIME = "parking_time"; //$//$NON-NLS-1$
	public final static String PARKING_EVENT_ADDED = "parking_event_added"; //$//$NON-NLS-1$

	// Constants for determining the order of items in the additional actions context menu
	private static final int MARK_AS_PARKING_POS_ITEM_ORDER = 10500;

    private LatLon parkingPosition;
    private OsmandApplication app;

	private ParkingPositionLayer parkingLayer;
	private TextInfoWidget parkingPlaceControl;
	private final CommonPreference<Float> parkingLat;
	private final CommonPreference<Float> parkingLon;
	private CommonPreference<Boolean> parkingType;
	private CommonPreference<Boolean> parkingEvent;
	private CommonPreference<Long> parkingTime;
	private CommonPreference<Long> parkingStartTime;

	public ParkingPositionPlugin(OsmandApplication app) {
		this.app = app;
		OsmandSettings set = app.getSettings();
		ApplicationMode.regWidgetVisibility("parking", (ApplicationMode[]) null);
		parkingLat = set.registerFloatPreference(PARKING_POINT_LAT, 0f).makeGlobal();
		parkingLon = set.registerFloatPreference(PARKING_POINT_LON, 0f).makeGlobal();
		parkingType = set.registerBooleanPreference(PARKING_TYPE, false).makeGlobal();
		parkingEvent = set.registerBooleanPreference(PARKING_EVENT_ADDED, false).makeGlobal();
		parkingTime = set.registerLongPreference(PARKING_TIME, -1).makeGlobal();
		parkingStartTime = set.registerLongPreference(PARKING_START_TIME, -1).makeGlobal();
        parkingPosition = constructParkingPosition();
	}
	

    public LatLon getParkingPosition() {
        return parkingPosition;
    }
	
	public LatLon constructParkingPosition() {
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
        parkingPosition = null;
		return true;
	}

	public boolean setParkingPosition(double latitude, double longitude) {
		parkingLat.set((float) latitude);
		parkingLon.set((float) longitude);
        parkingPosition = constructParkingPosition();
		return true;
	}
	
	public boolean setParkingType(boolean limited) {
		if (!limited)
			parkingTime.set(-1L);
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
	public String getHelpFileName() {
		return "feature_articles/parking-plugin.html";
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}

	@Override
	public String getComponentId1() {
		return PARKING_PLUGIN_COMPONENT;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		// remove old if existing after turn
		if(parkingLayer != null) {
			activity.getMapView().removeLayer(parkingLayer);
		}
		parkingLayer = new ParkingPositionLayer(activity, this);
		activity.getMapView().addLayer(parkingLayer, 5.5f);
		registerWidget(activity);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (isActive()) {
			if (parkingLayer == null) {
				registerLayers(activity);
			}
			if (parkingPlaceControl == null) {
				registerWidget(activity);
			}
		} else {
			if (parkingLayer != null) {
				activity.getMapView().removeLayer(parkingLayer);
				parkingLayer = null;
			}
			MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null && parkingPlaceControl != null) {
				mapInfoLayer.removeSideWidget(parkingPlaceControl);
				mapInfoLayer.recreateControls();
				parkingPlaceControl = null;
			}
		}
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null) {
			parkingPlaceControl = createParkingPlaceInfoControl(activity);
			mapInfoLayer.registerSideWidget(parkingPlaceControl,
					R.drawable.ic_action_parking_dark,  R.string.map_widget_parking, "parking", false, 10);
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
			final double latitude, final double longitude,
			ContextMenuAdapter adapter, Object selectedObj) {

		ItemClickListener addListener = new ItemClickListener() {
			@Override
			public boolean onContextMenuClick(ArrayAdapter<ContextMenuItem> adapter, int resId,
					int pos, boolean isChecked, int[] viewCoordinates) {
				if (resId == R.string.context_menu_item_add_parking_point) {
					showAddParkingDialog(mapActivity, latitude, longitude);
				}
				return true;
			}
		};
		adapter.addItem(new ContextMenuItem.ItemBuilder()
				.setTitleId(R.string.context_menu_item_add_parking_point, mapActivity)
				.setId(MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC)
				.setIcon(R.drawable.ic_action_parking_dark)
				.setOrder(MARK_AS_PARKING_POS_ITEM_ORDER)
				.setListener(addListener)
				.createItem());

	}

	/**
	 * Method dialog for adding of a parking location.
	 * It allows user to choose a type of parking (time-limited or time-unlimited).
	 */
	public void showAddParkingDialog(final MapActivity mapActivity, final double latitude, final double longitude) {
		Bundle args = new Bundle();
		args.putDouble(ParkingTypeBottomSheetDialogFragment.LAT_KEY, latitude);
		args.putDouble(ParkingTypeBottomSheetDialogFragment.LON_KEY, longitude);
		FragmentManager fragmentManager=mapActivity.getSupportFragmentManager();
		ParkingTypeBottomSheetDialogFragment fragment = new ParkingTypeBottomSheetDialogFragment();
		fragment.setArguments(args);
		fragment.show(fragmentManager, ParkingTypeBottomSheetDialogFragment.TAG);
	}

	void showContextMenuIfNeeded(final MapActivity mapActivity, boolean animated) {
		if (parkingLayer != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			if (menu.isVisible()) {
				menu.hide(animated);
				menu.show(new LatLon(parkingPosition.getLatitude(), parkingPosition.getLongitude()),
						parkingLayer.getObjectName(parkingPosition), parkingPosition);
			}
		}
	}

	/**
	 * Method creates confirmation dialog for deletion of a parking location.
	 */
	public AlertDialog showDeleteDialog(final Activity activity) {
		AlertDialog.Builder confirm = new AlertDialog.Builder(activity);
		confirm.setTitle(activity.getString(R.string.osmand_parking_delete));
		confirm.setMessage(activity.getString(R.string.osmand_parking_delete_confirm));
		confirm.setCancelable(true);
		confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showDeleteEventWarning(activity);
				cancelParking();
				if (activity instanceof MapActivity) {
					((MapActivity) activity).getContextMenu().close();
				}
			}
		});
		confirm.setNegativeButton(R.string.shared_string_cancel, null);
		return confirm.show();
	}
	
	/**
	 * Opens the dialog to set a time limit for time-limited parking.
	 * The dialog has option to add a notification to Calendar app. 
	 * Anyway the time-limit can be seen from parking point description.
	 * @param mapActivity
	 * @param choose
	 */
	void showSetTimeLimitDialog(final MapActivity mapActivity, final DialogInterface choose) {
		boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final View setTimeParking = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.parking_set_time_limit, null);
		AlertDialog.Builder setTime = new AlertDialog.Builder(mapActivity);
		setTime.setView(setTimeParking);
		setTime.setTitle(mapActivity.getString(R.string.osmand_parking_time_limit_title));
		setTime.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				cancelParking();
			}
		});
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
                if (minute % TIME_PICKER_INTERVAL != 0) {
                    int minuteFloor = minute - (minute % TIME_PICKER_INTERVAL);
                    minute = minuteFloor + (minute == minuteFloor + 1 ? TIME_PICKER_INTERVAL : 0);
                    if (minute == 60) {
                        minute = 0;
                    }
                    mIgnoreEvent = true;
                    timePicker.setCurrentMinute(minute);
                    mIgnoreEvent = false;
                }
                long timeInMillis = cal.getTimeInMillis() + hourOfDay * 60 * 60 * 1000 + minute * 60 * 1000;
                textView.setText(mapActivity.getString(R.string.osmand_parking_position_description_add)
                        + " " + parkingLayer.getFormattedTime(timeInMillis));

            }
        });
		
		
		//to set the same 24-hour or 12-hour mode as it is set in the device
		timePicker.setIs24HourView(true);
		timePicker.setCurrentHour(0);
		timePicker.setCurrentMinute(0);
		
		setTime.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				choose.dismiss();
				Calendar cal = Calendar.getInstance();
				//int hour = cal.get(Calendar.HOUR_OF_DAY );
				//int minute = cal.get(Calendar.MINUTE);
				cal.add(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
				cal.add(Calendar.MINUTE, timePicker.getCurrentMinute());
				setParkingTime(cal.getTimeInMillis());
				CheckBox addCalendarEvent = (CheckBox) setTimeParking.findViewById(R.id.check_event_in_calendar);
				if (addCalendarEvent.isChecked()) {
					addCalendarEvent(setTimeParking);
					addOrRemoveParkingEvent(true);
				} else {
					addOrRemoveParkingEvent(false);
				}
				showContextMenuIfNeeded(mapActivity,false);
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
		intent.putExtra("endTime", getParkingTime() + 60 * 60 * 1000); //$NON-NLS-1$
		view.getContext().startActivity(intent);
	}

	/**
	 * Method shows warning, if previously the event for time-limited parking was added to Calendar app.
	 * @param activity
	 */
	void showDeleteEventWarning(final Activity activity) {
		if (isParkingEventAdded()) {
			AlertDialog.Builder deleteEventWarning = new AlertDialog.Builder(activity);
			deleteEventWarning.setTitle(activity.getString(R.string.osmand_parking_warning));
			deleteEventWarning.setMessage(activity.getString(R.string.osmand_parking_warning_text));
			deleteEventWarning.setNeutralButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {						
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
	void setParkingPosition(final MapActivity mapActivity, final double latitude, final double longitude, boolean isLimited) {
		setParkingPosition(latitude, longitude);
		setParkingType(isLimited);
		setParkingStartTime(Calendar.getInstance().getTimeInMillis());
		if (parkingLayer != null) {
			parkingLayer.refresh();
		}
	}

	private void cancelParking() {
		if (parkingLayer != null) {
			parkingLayer.refresh();
		}
		clearParkingPosition();
	}
	
	/**
	 * @return the control to be added on a MapInfoLayer 
	 * that shows a distance between 
	 * the current position on the map 
	 * and the location of the parked car
	 */
	private TextInfoWidget createParkingPlaceInfoControl(final MapActivity map) {
		TextInfoWidget parkingPlaceControl = new TextInfoWidget(map) {
			private float[] calculations = new float[1];
			private int cachedMeters = 0;			
			
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				if (parkingLayer != null) {
					LatLon parkingPoint = parkingLayer.getParkingPoint();
					if (parkingPoint != null && !map.getRoutingHelper().isFollowingMode()) {
						OsmandMapTileView view = map.getMapView();
						int d = 0;
						if (d == 0) {
							net.osmand.Location.distanceBetween(view.getLatitude(), view.getLongitude(), parkingPoint.getLatitude(), parkingPoint.getLongitude(), calculations);
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
				LatLon parkingPoint = parkingPosition;
				if (parkingPoint != null) {
					int fZoom = view.getZoom() < 15 ? 15 : view.getZoom();
					thread.startMoving(parkingPoint.getLatitude(), parkingPoint.getLongitude(), fZoom, true);
				}
			}
		});
		parkingPlaceControl.setText(null, null);
		parkingPlaceControl.setIcons(R.drawable.widget_parking_day, R.drawable.widget_parking_night);
		return parkingPlaceControl;
	}
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.parking_position;
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_parking_dark;
	}

	String getFormattedTime(long timeInMillis, Activity ctx) {
		StringBuilder timeStringBuilder = new StringBuilder();
		Time time = new Time();
		time.set(timeInMillis);
		timeStringBuilder.append(time.hour);
		timeStringBuilder.append(":");
		int minute = time.minute;
		timeStringBuilder.append(minute < 10 ? "0" + minute : minute);
		if (!DateFormat.is24HourFormat(ctx)) {
			timeStringBuilder.append(time.hour >= 12 ? ctx.getString(R.string.osmand_parking_pm) : ctx
					.getString(R.string.osmand_parking_am));
		}
		return timeStringBuilder.toString();
	}

	String getFormattedTimeInterval(long timeInMillis, Activity ctx) {
		if (timeInMillis < 0) {
			timeInMillis *= -1;
		}
		StringBuilder timeStringBuilder = new StringBuilder();
		int hours = (int) timeInMillis / (1000 * 60 * 60);
		int minMills = (int) timeInMillis % (1000 * 60 * 60);
		int minutes = minMills / (1000 * 60);
		if (hours > 0) {
			timeStringBuilder.append(hours);
			timeStringBuilder.append(" ");
			timeStringBuilder.append(ctx.getString(R.string.osmand_parking_hour));
		}

		if (timeStringBuilder.length() > 0) {
			timeStringBuilder.append(" ");
		}
		timeStringBuilder.append(minutes);
		timeStringBuilder.append(" ");
		timeStringBuilder.append(ctx.getString(R.string.osmand_parking_minute));


		return timeStringBuilder.toString();
	}

	public String getParkingTitle(Activity ctx) {
		StringBuilder title = new StringBuilder();
		if (getParkingType()) {
			title.append(ctx.getString(R.string.pick_up_till)).append(" ");
			long endTime = getParkingTime();
			title.append(getFormattedTime(endTime, ctx));
		} else {
			title.append(ctx.getString(R.string.osmand_parking_position_name));
		}
		return title.toString();
	}

	public String getParkingStartDesc(Activity ctx) {
		StringBuilder parkingStartDesc = new StringBuilder();
		String startTime = getFormattedTime(getStartParkingTime(), ctx);
		if (getParkingType()) {
			parkingStartDesc.append(ctx.getString(R.string.osmand_parking_position_name));
			parkingStartDesc.append(", ");
			parkingStartDesc.append(ctx.getString(R.string.parked_at));
			parkingStartDesc.append(" ").append(startTime);
		} else {
			parkingStartDesc.append(ctx.getString(R.string.osmand_parking_position_description_add_time));
			parkingStartDesc.append(" ");
			parkingStartDesc.append(startTime);
		}
		return parkingStartDesc.toString();
	}

	public String getParkingLeftDesc(Activity ctx) {
		StringBuilder descr = new StringBuilder();
		if (getParkingType()) {
			long endtime = getParkingTime();
			long currTime = Calendar.getInstance().getTimeInMillis();
			long timeDiff = endtime - currTime;
			descr.append(getFormattedTimeInterval(timeDiff, ctx)).append(" ");
			if (timeDiff < 0) {
				descr.append(ctx.getString(R.string.osmand_parking_overdue));
			} else {
				descr.append(ctx.getString(R.string.osmand_parking_time_left));
			}
		} else {
			descr.append(ctx.getString(R.string.without_time_limit));
		}
		return descr.toString();
	}

	public String getParkingDescription(Activity ctx) {
		StringBuilder timeLimitDesc = new StringBuilder();
		timeLimitDesc.append(ctx.getString(R.string.osmand_parking_position_description_add_time) + " ");
		timeLimitDesc.append(getFormattedTime(getStartParkingTime(), ctx) + ".");
		if (getParkingType()) {
			// long parkingTime = settings.getParkingTime();
			// long parkingStartTime = settings.getStartParkingTime();
			// Time time = new Time();
			// time.set(parkingTime);
			// timeLimitDesc.append(map.getString(R.string.osmand_parking_position_description_add) + " ");
			// timeLimitDesc.append(time.hour);
			// timeLimitDesc.append(":");
			// int minute = time.minute;
			// timeLimitDesc.append(minute<10 ? "0" + minute : minute);
			// if (!DateFormat.is24HourFormat(map.getApplicationContext())) {
			// timeLimitDesc.append(time.hour >= 12 ? map.getString(R.string.osmand_parking_pm) :
			// map.getString(R.string.osmand_parking_am));
			// }
			timeLimitDesc.append(ctx.getString(R.string.osmand_parking_position_description_add) + " ");
			timeLimitDesc.append(getFormattedTime(getParkingTime(),ctx));
		}
		return ctx.getString(R.string.osmand_parking_position_description, timeLimitDesc.toString());
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashParkingFragment.FRAGMENT_DATA;
	}
}
