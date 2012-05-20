package net.osmand.plus.osmedit;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;

public class OsmEditingPlugin extends OsmandPlugin {
	private static final String ID = "osm.editing.plugin";
	private OsmandSettings settings;
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		return true;
	}
	
	private OsmBugsLayer osmBugsLayer;
	private EditingPOIActivity poiActions;
	
	@Override
	public void updateLayers(OsmandMapTileView mapView){
		if(mapView.getLayers().contains(osmBugsLayer) != settings.SHOW_OSM_BUGS.get()){
			if(settings.SHOW_OSM_BUGS.get()){
				mapView.addLayer(osmBugsLayer, 2);
			} else {
				mapView.removeLayer(osmBugsLayer);
			}
		}
	}
	
	@Override
	public void registerLayers(MapActivity activity){
		osmBugsLayer = new OsmBugsLayer(activity);
	}

	@Override
	public void mapActivityCreate(MapActivity activity) {
		poiActions = new EditingPOIActivity(activity);
		activity.addDialogProvider(poiActions);
		activity.addDialogProvider(osmBugsLayer);
		
	}
	
	public EditingPOIActivity getPoiActions() {
		return poiActions;
	}

	@Override
	public boolean handleContextMenuAction(int resId, final double latitude, final double longitude) {
		if (resId == R.string.context_menu_item_create_poi) {
			poiActions.showCreateDialog(latitude, longitude);
			return true;
		} else if (resId == R.string.context_menu_item_open_bug) {
			osmBugsLayer.openBug(latitude, longitude);
			return true;
		}
		return false;
	}

	@Override
	public void registerContextMenuActions(double latitude, double longitude, TIntArrayList list) {
		list.add(R.string.context_menu_item_create_poi);
		list.add(R.string.context_menu_item_open_bug);
	}

}
