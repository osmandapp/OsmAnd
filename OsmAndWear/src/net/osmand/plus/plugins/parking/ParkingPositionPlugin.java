package net.osmand.plus.plugins.parking;


import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_PARKING_POSITION;
import static net.osmand.plus.views.mapwidgets.WidgetType.PARKING;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The plugin facilitates a storage of the location of a parked car.
 *
 * @author Alena Fedasenka
 */
public class ParkingPositionPlugin extends OsmandPlugin {

	public static final String PARKING_PLUGIN_COMPONENT = "net.osmand.parkingPlugin";
	public static final String PARKING_POINT_LAT = "parking_point_lat";
	public static final String PARKING_POINT_LON = "parking_point_lon";
	public static final String PARKING_TYPE = "parking_type";
	public static final String PARKING_TIME = "parking_limit_time";
	public static final String PARKING_PICKUP_DATE = "parking_time";
	public static final String PARKING_EVENT_ADDED = "parking_event_added";

	// Constants for determining the order of items in the additional actions context menu
	private static final int MARK_AS_PARKING_POS_ITEM_ORDER = 10500;

	private LatLon parkingPosition;

	private final CommonPreference<Float> parkingLat;
	private final CommonPreference<Float> parkingLon;
	private final CommonPreference<Boolean> parkingType;
	private final CommonPreference<Boolean> parkingEvent;
	private final CommonPreference<Long> parkingTime;
	private final CommonPreference<Long> parkingPickupDate;

	public ParkingPositionPlugin(OsmandApplication app) {
		super(app);
		OsmandSettings set = app.getSettings();
		WidgetsAvailabilityHelper.regWidgetVisibility(PARKING, (ApplicationMode[]) null);
		parkingLat = set.registerFloatPreference(PARKING_POINT_LAT, 0f).makeGlobal().makeShared();
		parkingLon = set.registerFloatPreference(PARKING_POINT_LON, 0f).makeGlobal().makeShared();
		parkingType = set.registerBooleanPreference(PARKING_TYPE, false).makeGlobal().makeShared();
		parkingEvent = set.registerBooleanPreference(PARKING_EVENT_ADDED, false).makeGlobal().makeShared();
		parkingTime = set.registerLongPreference(PARKING_TIME, -1).makeGlobal().makeShared();
		parkingPickupDate = set.registerLongPreference(PARKING_PICKUP_DATE, -1).makeGlobal().makeShared();
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
		return parkingPickupDate.get();
	}

	public void updateParkingPoint(@NonNull FavouritePoint point) {
		if (point.getSpecialPointType() == SpecialPointType.PARKING) {
			long timestamp = point.getTimestamp();
			boolean timeRestricted = timestamp > 0;
			setParkingType(timeRestricted);
			setParkingTime(timeRestricted ? timestamp : 0);
			setParkingPickupDate(point.getPickupDate());
			setParkingPosition(point.getLatitude(), point.getLongitude());
			addOrRemoveParkingEvent(point.getCalendarEvent());

			if (point.getCalendarEvent()) {
				addCalendarEvent(app);
			}
		}
	}

	public boolean clearParkingPosition() {
		parkingLat.resetToDefault();
		parkingLon.resetToDefault();
		parkingType.resetToDefault();
		parkingTime.resetToDefault();
		parkingEvent.resetToDefault();
		parkingPickupDate.resetToDefault();
		parkingPosition = null;
		FavouritePoint pnt = app.getFavoritesHelper().getSpecialPoint(SpecialPointType.PARKING);
		if (pnt != null) {
			app.getFavoritesHelper().deleteFavourite(pnt);
		}
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

	public boolean setParkingPickupDate(long timeInMillis) {
		parkingPickupDate.set(timeInMillis);
		return true;
	}

	@Override
	public String getId() {
		return PLUGIN_PARKING_POSITION;
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.osmand_parking_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_parking_plugin_name);
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
	public void registerMapContextMenuActions(@NonNull MapActivity mapActivity,
	                                          double latitude, double longitude,
	                                          ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {

		ItemClickListener addListener = (uiAdapter, view, item, isChecked) -> {
			showAddParkingDialog(mapActivity, latitude, longitude);
			return true;
		};
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MARK_AS_PARKING_LOC)
				.setTitleId(R.string.context_menu_item_add_parking_point, mapActivity)
				.setIcon(R.drawable.ic_action_parking_dark)
				.setOrder(MARK_AS_PARKING_POS_ITEM_ORDER)
				.setListener(addListener));

	}

	/**
	 * Method dialog for adding of a parking location.
	 * It allows user to choose a type of parking (time-limited or time-unlimited).
	 */
	public void showAddParkingDialog(MapActivity mapActivity, double latitude, double longitude) {
		Bundle args = new Bundle();
		args.putDouble(ParkingTypeBottomSheetDialogFragment.LAT_KEY, latitude);
		args.putDouble(ParkingTypeBottomSheetDialogFragment.LON_KEY, longitude);
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		ParkingTypeBottomSheetDialogFragment fragment = new ParkingTypeBottomSheetDialogFragment();
		fragment.setArguments(args);
		fragment.show(fragmentManager, ParkingTypeBottomSheetDialogFragment.TAG);
	}

	void showContextMenuIfNeeded(MapActivity mapActivity, boolean animated) {
		MapContextMenu menu = mapActivity.getContextMenu();
		FavouritePoint pnt = app.getFavoritesHelper().getSpecialPoint(SpecialPointType.PARKING);
		if (menu.isVisible()) {
			menu.hide(animated);
			menu.show(new LatLon(parkingPosition.getLatitude(), parkingPosition.getLongitude()),
					getObjectName(parkingPosition), pnt);
		}
	}

	public PointDescription getObjectName(Object o) {
		return new PointDescription(PointDescription.POINT_TYPE_PARKING_MARKER,
				app.getString(R.string.osmand_parking_position_name));
	}

	/**
	 * Method creates confirmation dialog for deletion of a parking location.
	 */
	public AlertDialog showDeleteDialog(Activity activity) {
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
					FavouritePoint pnt = app.getFavoritesHelper().getSpecialPoint(SpecialPointType.PARKING);
					if (pnt != null) {
						app.getFavoritesHelper().deleteFavourite(pnt);
					}
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
	 *
	 * @param mapActivity
	 * @param choose
	 */
	void showSetTimeLimitDialog(MapActivity mapActivity, DialogInterface choose) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		View setTimeParking = UiUtilities.getInflater(mapActivity, nightMode).inflate(R.layout.parking_set_time_limit, null);
		AlertDialog.Builder setTime = new AlertDialog.Builder(mapActivity);
		setTime.setView(setTimeParking);
		setTime.setTitle(mapActivity.getString(R.string.osmand_parking_time_limit_title));
		setTime.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				cancelParking();
			}
		});
		TextView textView = setTimeParking.findViewById(R.id.parkTime);
		TimePicker timePicker = setTimeParking.findViewById(R.id.parking_time_picker);

		timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
			private static final int TIME_PICKER_INTERVAL = 5;
			private boolean mIgnoreEvent;
			private final Calendar cal = Calendar.getInstance();


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
					timePicker.setMinute(minute);
					mIgnoreEvent = false;
				}
				long timeInMillis = cal.getTimeInMillis() + hourOfDay * 60 * 60 * 1000 + minute * 60 * 1000;
				String title = mapActivity.getString(R.string.osmand_parking_position_description_add);
				textView.setText(mapActivity.getString(R.string.ltr_or_rtl_combine_via_space, title, getFormattedTime(timeInMillis)));
			}
		});

		//to set the same 24-hour or 12-hour mode as it is set in the device
		timePicker.setIs24HourView(true);
		timePicker.setHour(0);
		timePicker.setMinute(0);

		setTime.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				choose.dismiss();
				Calendar cal = Calendar.getInstance();
				//int hour = cal.get(Calendar.HOUR_OF_DAY );
				//int minute = cal.get(Calendar.MINUTE);
				cal.add(Calendar.HOUR_OF_DAY, timePicker.getHour());
				cal.add(Calendar.MINUTE, timePicker.getMinute());
				setParkingTime(cal.getTimeInMillis());
				app.getFavoritesHelper().setParkingPoint(getParkingPosition(), null, getParkingTime(), isParkingEventAdded());
				CheckBox addCalendarEvent = setTimeParking.findViewById(R.id.check_event_in_calendar);
				if (addCalendarEvent.isChecked()) {
					addCalendarEvent(setTimeParking.getContext());
					addOrRemoveParkingEvent(true);
				} else {
					addOrRemoveParkingEvent(false);
				}
				showContextMenuIfNeeded(mapActivity, false);
			}
		});
		setTime.create();
		setTime.show();
	}

	/**
	 * Opens a Calendar app with added notification to pick up the car from time-limited parking.
	 *
	 * @param context
	 */
	public void addCalendarEvent(Context context) {
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event"); //$NON-NLS-1$
		intent.putExtra("calendar_id", 1); //$NON-NLS-1$
		intent.putExtra("title", context.getString(R.string.osmand_parking_event)); //$NON-NLS-1$
		intent.putExtra("beginTime", getParkingTime()); //$NON-NLS-1$
		intent.putExtra("endTime", getParkingTime() + 60 * 60 * 1000); //$NON-NLS-1$
		AndroidUtils.startActivityIfSafe(context, intent);
	}

	/**
	 * Method shows warning, if previously the event for time-limited parking was added to Calendar app.
	 *
	 * @param activity
	 */
	void showDeleteEventWarning(Activity activity) {
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
	 *
	 * @param latitude
	 * @param longitude
	 * @param isLimited
	 */
	void setParkingPosition(double latitude, double longitude, boolean isLimited) {
		setParkingPosition(latitude, longitude);
		setParkingType(isLimited);
		setParkingPickupDate(Calendar.getInstance().getTimeInMillis());
	}

	private void cancelParking() {
		clearParkingPosition();
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		if (widgetType == PARKING) {
			return new ParkingMapWidget(this, mapActivity, customId, widgetsPanel);
		}
		return null;
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
		MapWidget widget = createMapWidgetForParams(mapActivity, PARKING);
		widgetsInfos.add(creator.createWidgetInfo(widget));
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.parking_position);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_parking_dark;
	}

	String getFormattedTime(long timeInMillis) {
		java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(app);
		java.text.DateFormat timeFormat = DateFormat.getTimeFormat(app);
		return timeFormat.format(timeInMillis) + " " + dateFormat.format(timeInMillis);
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
		timeStringBuilder.append(ctx.getString(R.string.shared_string_minute_lowercase));


		return timeStringBuilder.toString();
	}

	public String getParkingTitle(Activity ctx) {
		StringBuilder title = new StringBuilder();
		if (getParkingType()) {
			title.append(ctx.getString(R.string.pick_up_till)).append(" ");
			long endTime = getParkingTime();
			title.append(getFormattedTime(endTime));
		} else {
			title.append(ctx.getString(R.string.osmand_parking_position_name));
		}
		return title.toString();
	}

	public String getParkingStartDesc(Activity ctx) {
		StringBuilder parkingStartDesc = new StringBuilder();
		String startTime = getFormattedTime(getStartParkingTime());
		if (getParkingType()) {
			parkingStartDesc.append(ctx.getString(R.string.osmand_parking_position_name));
			if (getStartParkingTime() > 0) {
				parkingStartDesc.append(", ");
				parkingStartDesc.append(ctx.getString(R.string.parked_at));
				parkingStartDesc.append(" ").append(startTime);
			}
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
		timeLimitDesc.append(getFormattedTime(getStartParkingTime()) + ".");
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
			timeLimitDesc.append(getFormattedTime(getParkingTime()));
		}
		return ctx.getString(R.string.osmand_parking_position_description, timeLimitDesc.toString());
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashParkingFragment.FRAGMENT_DATA;
	}


	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(ParkingAction.TYPE);
		return quickActionTypes;
	}
}
