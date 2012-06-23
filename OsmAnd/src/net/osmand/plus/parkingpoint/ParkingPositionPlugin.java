package net.osmand.plus.parkingpoint;

import java.util.Calendar;

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
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.Time;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TimePicker;

/**
 * 
 * The plugin facilitates a storage of the location of a parked car
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
		OnContextMenuClick addListener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos,
					boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_parking_point) {
					showAddParkingDialog(mapActivity, latitude, longitude);
					// settings.setParkingPosition(latitude, longitude);
					// if
					// (mapActivity.getMapView().getLayers().contains(parkingLayer))
					// parkingLayer.setParkingPoint(settings.getParkingPosition());
				} else if ((resId == R.string.context_menu_item_delete_parking_point)) {
					parkingLayer.showDeleteDialog();
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_parking_point, 0,
				addListener, -1);
		if (settings.getParkingPosition() != null)
			adapter.registerItem(
					R.string.context_menu_item_delete_parking_point, 0,
					addListener, -1);
	}

	/**
	 * Method creates confirmation dialog for deletion of a parking location
	 */
	public void showAddParkingDialog(final MapActivity mapActivity,
			final double latitude, final double longitude) {
		final View addParking = mapActivity.getLayoutInflater().inflate(R.layout.choose_type_of_parking, null);
		Builder choose = new AlertDialog.Builder(mapActivity);
		choose.setView(addParking);
		choose.setTitle("Choose the type of parking");
		final AlertDialog create = choose.create();
		
		ImageButton limitButton = (ImageButton) addParking
				.findViewById(R.id.parking_lim_button);
		limitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				create.cancel();
				setParkingPosition(mapActivity, latitude, longitude, true);
				showChooseParkingTypeDialog(mapActivity);
			}
		});

		ImageButton noLimitButton = (ImageButton) addParking
				.findViewById(R.id.parking_no_lim_button);
		noLimitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				create.cancel();
				setParkingPosition(mapActivity, latitude, longitude, false);
			}
		});

		choose.show();

	}

	private void showChooseParkingTypeDialog(final MapActivity mapActivity) {
		final View setTimeParking = mapActivity.getLayoutInflater().inflate(R.layout.set_time_of_parking, null);
		Builder setTime = new AlertDialog.Builder(mapActivity);
		setTime.setView(setTimeParking);
		setTime.setTitle("Set the time limit of parking");
		setTime.setNegativeButton(R.string.default_buttons_cancel, null);
		setTime.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						TimePicker timePicker = (TimePicker) setTimeParking.findViewById(R.id.parkingTimePicker);
						Calendar cal = Calendar.getInstance();
						cal.set(Calendar.HOUR, timePicker.getCurrentHour());
						cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
						settings.setParkingTime(cal.getTimeInMillis());
						addCalendarEvent(setTimeParking);
					}
				});
		setTime.create();
		setTime.show();
	}
	
	private void addCalendarEvent(View view) {
		Calendar cal = Calendar.getInstance();              
		Intent intent = new Intent(Intent.ACTION_EDIT);
		intent.setType("vnd.android.cursor.item/event");
		intent.putExtra("calendar_id", 1);
		intent.putExtra("beginTime", settings.getParkingTime());
		intent.putExtra("allDay", false);
		intent.putExtra("endTime", cal.getTimeInMillis()+60*60*1000);
		intent.putExtra("title", "Pickup the car from parking");
		intent.putExtra("name", "parkingEvent");
//		opens Calendar app. TODO Is that needed?
		view.getContext().startActivity(intent);
	}

	private void setParkingPosition(final MapActivity mapActivity, final double latitude, final double longitude, boolean isLimited) {
		settings.setParkingPosition(latitude, longitude);
		settings.setParkingType(isLimited);
		if (mapActivity.getMapView().getLayers().contains(parkingLayer)) {
			parkingLayer.setParkingPoint(settings.getParkingPosition());
		}
	}
}
