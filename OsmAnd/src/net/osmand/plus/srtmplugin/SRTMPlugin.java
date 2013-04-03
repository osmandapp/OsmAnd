package net.osmand.plus.srtmplugin;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

public class SRTMPlugin extends OsmandPlugin {

	public static final String ID = "osmand.srtm";
	private static final Log log = PlatformUtil.getLog(SRTMPlugin.class);
	private OsmandApplication app;
	private boolean paid;
	private HillshadeLayer hillshadeLayer;
	
	@Override
	public String getId() {
		return ID;
	}

	public SRTMPlugin(OsmandApplication app, boolean paid) {
		this.app = app;
		this.paid = paid;
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> pref = settings.getCustomRenderProperty("contourLines");
		if(pref.get().equals("")) {
			for(ApplicationMode m : ApplicationMode.values()) {
				if(pref.getModeValue(m).equals("")) {
					pref.setModeValue(m, "13");
				}
			}
		}

	}
	
	public boolean isPaid() {
		return paid;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.srtm_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.srtm_plugin_name);
	}

	@Override
	public boolean init(final OsmandApplication app) {
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		if (hillshadeLayer != null) {
			activity.getMapView().removeLayer(hillshadeLayer);
		}
		hillshadeLayer = new HillshadeLayer(activity, this);
		activity.getMapView().addLayer(hillshadeLayer, 0.3f);
		app.getSettings().MAP_TRANSPARENCY.set(170);
		// TODO make action to enable/disable
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (hillshadeLayer == null) {
			registerLayers(activity);
		}
	}
	@Override
	public void disable(OsmandApplication app) {
	}

}
