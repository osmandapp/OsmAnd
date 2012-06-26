package net.osmand.plus.parkingpoint;

import java.util.Calendar;

import net.osmand.osm.LatLon;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TimePicker;

/**
 * 
 * The plugin facilitates a storage of the location of a parked car.
 * 
 * @author Alena Fedasenka
 */
public class ParkingPositionPlugin extends OsmandPlugin {

	private static final String ID = "osmand.parking.position";
	private OsmandApplication app;

	private ParkingPositionLayer parkingLayer;
	private OsmandSettings settings;

	public ParkingPositionPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
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
		parkingLayer = new ParkingPositionLayer(activity);
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if ((settings.getParkingPosition() == null)
				&& (mapView.getLayers().contains(parkingLayer))) {
			mapView.removeLayer(parkingLayer);
		} else {
			if (parkingLayer == null)
				registerLayers(activity);
			mapView.addLayer(parkingLayer, 5);
		}
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity,
			final double latitude, final double longitude,
			ContextMenuAdapter adapter, Object selectedObj) {
		boolean isParkingSelected = false;
		LatLon parkingPosition = settings.getParkingPosition();
		if (selectedObj instanceof LatLon && parkingLayer != null) {
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
				adapter.registerItem(R.string.context_menu_item_delete_parking_point, 0, removeListener, 0);
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
		adapter.registerItem(R.string.context_menu_item_add_parking_point, 0, addListener, -1);
		
	}

	/**
	 * Method dialog for adding of a parking location.
	 * It allows user to choose a type of parking (time-limited or time-unlimited).
	 */
	private void showAddParkingDialog(final MapActivity mapActivity, final double latitude, final double longitude) {
		final boolean wasEventPreviouslyAdded = settings.isParkingEventAdded();
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
				settings.addParkingEvent(false);
				setParkingPosition(mapActivity, latitude, longitude, false);
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
				settings.clearParkingPosition();
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
		final TimePicker timePicker = (TimePicker) setTimeParking.findViewById(R.id.parking_time_picker);
		
		//to set the same 24-hour or 12-hour mode as it is set in the device
		timePicker.setIs24HourView(DateFormat.is24HourFormat(app));
		timePicker.setCurrentHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
		
		setTime.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				choose.dismiss();
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.HOUR, timePicker.getCurrentHour());
				cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
				settings.setParkingTime(cal.getTimeInMillis());
				CheckBox addCalendarEvent = (CheckBox)setTimeParking.findViewById(R.id.check_event_in_calendar);
				if (addCalendarEvent.isChecked())
					addCalendarEvent(setTimeParking);
					settings.addParkingEvent(true);
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
		intent.putExtra("beginTime", settings.getParkingTime()); //$NON-NLS-1$
		intent.putExtra("endTime", settings.getParkingTime()+60*60*1000); //$NON-NLS-1$
		view.getContext().startActivity(intent);
	}

	/**
	 * Method shows warning, if previously the event for time-limited parking was added to Calendar app.
	 * @param mapActivity
	 */
	private void showDeleteEventWarning(final MapActivity mapActivity) {
		if (settings.isParkingEventAdded()) {
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
		settings.setParkingPosition(latitude, longitude);
		settings.setParkingType(isLimited);
		if (mapActivity.getMapView().getLayers().contains(parkingLayer)) {
			parkingLayer.setParkingPointOnLayer(settings.getParkingPosition());
		}
	}
}
