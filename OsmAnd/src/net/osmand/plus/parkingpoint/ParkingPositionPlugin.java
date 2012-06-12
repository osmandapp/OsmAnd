package net.osmand.plus.parkingpoint;

import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.DialogInterface;
import android.preference.PreferenceScreen;

public class ParkingPositionPlugin extends OsmandPlugin {

	private static final String ID = "osmand.parking.position";
	private OsmandApplication app;
//	private static final Log log = LogUtil.getLog(ParkingPositionPlugin.class);	
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
		return app.getString(R.string.osmand_parking_position_description);
	}
	
	@Override
	public String getName() {
		return app.getString(R.string.osmand_parking_position_name);
	}
	
	@Override
	public void registerLayers(MapActivity activity) {
		parkingLayer = new ParkingPositionLayer(activity);
	}
	
	@Override
	public void mapActivityCreate(MapActivity activity) {
//		activity.addDialogProvider(parkingLayer);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if((settings.getParkingPosition() == null) && (mapView.getLayers().contains(parkingLayer))){
			mapView.removeLayer(parkingLayer);
		} else {
			mapView.addLayer(parkingLayer, 8);
		}
	}
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter, Object selectedObj) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_parking_point){
					settings.setParkingPosition(latitude, longitude);
					if (mapActivity.getMapView().getLayers().contains(parkingLayer))
						parkingLayer.setParkingPoint(settings.getParkingPosition());
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_parking_point, 0, listener, -1);
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
	}
}
