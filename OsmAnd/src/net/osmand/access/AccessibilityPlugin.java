package net.osmand.access;

import android.app.Activity;
import android.graphics.PointF;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.access.tasker.AutoAppsThirdParty;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityLayers;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityPlugin extends OsmandPlugin {
	private static final Log LOG = PlatformUtil.getLog(AccessibilityPlugin.class);
	private static final String ID = "osmand.accessibility";
	private OsmandApplication app;
	private MapActivity mMapActivity;

	static final AutoAppsThirdParty.RegisteredCommand UPDATE_LOCATION_COMMAND =
			new AutoAppsThirdParty.RegisteredCommand("Update location", "updatelocation", false, "speed");


	public AccessibilityPlugin(OsmandApplication app) {
		this.app = app;
	}


	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_accessibility_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.shared_string_accessibility);
	}

	@Override
	public void registerLayers(MapActivity activity) {
	}


	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsAccessibilityActivity.class;
	}


	@Override
	public int getAssetResourceName() {
		return 0;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_accessibility;
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		mMapActivity = activity;
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		mMapActivity = null;
	}

	@Override
	public void updateLocation(Location location) {
		if (app.getSettings().TASKER_PLUGIN.get() && location != null && mMapActivity != null) {
			RotatedTileBox tb = mMapActivity.getMapView().getCurrentRotatedTileBox();
			float x = tb.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			float y = tb.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			PointF currentLocationInPixels = new PointF(x, y);
			List<Object> mapObjectsList = new ArrayList<>();
			MapActivityLayers mapLayers = mMapActivity.getMapLayers();
			List<OsmandMapLayer> mapLayersList = new ArrayList<>();
			mapLayersList.add(mapLayers.getPoiMapLayer());
			mapLayersList.add(mapLayers.getFavoritesLayer());
			for (OsmandMapLayer layer : mapLayersList) {
				ContextMenuLayer.IContextMenuProvider layerWithObjects =
						(ContextMenuLayer.IContextMenuProvider) layer;
				layerWithObjects.collectObjectsFromPoint(currentLocationInPixels, tb, mapObjectsList);
			}
			LOG.debug("MapObjects=" + mapObjectsList);
			AutoAppsThirdParty.sendCommand(app, "updatelocation", String.valueOf(location.getSpeed()));
		}
	}
}
