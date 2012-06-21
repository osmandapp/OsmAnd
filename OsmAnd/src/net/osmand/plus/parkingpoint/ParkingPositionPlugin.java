package net.osmand.plus.parkingpoint;

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
import android.view.View;
import android.widget.ImageButton;

/**
 * 
 * The plugin facilitates a storage of the location of a parked car 
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
		if((settings.getParkingPosition() == null) && (mapView.getLayers().contains(parkingLayer))){
			mapView.removeLayer(parkingLayer);
		} else {
			if (parkingLayer == null)
				registerLayers(activity);
			mapView.addLayer(parkingLayer, 5);
		}
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter, Object selectedObj) {
		OnContextMenuClick addListener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_parking_point) {
					showAddParkingDialog(mapActivity, latitude, longitude);
//					settings.setParkingPosition(latitude, longitude);
//					if (mapActivity.getMapView().getLayers().contains(parkingLayer))
//						parkingLayer.setParkingPoint(settings.getParkingPosition());
				} else if ((resId == R.string.context_menu_item_delete_parking_point)){
					parkingLayer.showDeleteDialog();
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_parking_point, 0, addListener, -1);
		if (settings.getParkingPosition() != null)
			adapter.registerItem(R.string.context_menu_item_delete_parking_point, 0, addListener, -1);
	}
	
	/**
	 * Method creates confirmation dialog for deletion of a parking location 
	 */
	public void showAddParkingDialog(final MapActivity mapActivity, final double latitude, final double longitude) {
		final View addParking = mapActivity.getLayoutInflater().inflate(R.layout.choose_type_of_parking, null);
		Builder choose = new AlertDialog.Builder(mapActivity);
		choose.setView(addParking);
		choose.setTitle("Choose the type of parking");

		ImageButton limitButton= (ImageButton) addParking.findViewById(R.id.parking_lim_button);
		limitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				settings.setParkingPosition(latitude, longitude);
				settings.setParkingTimeLimit(1);
				if (mapActivity.getMapView().getLayers().contains(parkingLayer))
					parkingLayer.setParkingPoint(settings.getParkingPosition());
			}
		});
		
		ImageButton noLimitButton= (ImageButton)addParking.findViewById(R.id.parking_no_lim_button);		
		noLimitButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				settings.setParkingPosition(latitude, longitude);
				settings.setParkingTimeLimit(-1);
				if (mapActivity.getMapView().getLayers().contains(parkingLayer))
					parkingLayer.setParkingPoint(settings.getParkingPosition());
			}
		});
		choose.create();
		choose.show();
	
	}

}
