package net.osmand.plus.srtmplugin;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.DialogInterface;

public class SRTMPlugin extends OsmandPlugin {

	public static final String ID = "osmand.srtm";
	public static final String FREE_ID = "osmand.srtm.paid";
	private OsmandApplication app;
	private boolean paid;
	private HillshadeLayer hillshadeLayer;
	private CommonPreference<Boolean> HILLSHADE;
	
	@Override
	public String getId() {
		return paid ? ID : FREE_ID;
	}

	public SRTMPlugin(OsmandApplication app, boolean paid) {
		this.app = app;
		this.paid = paid;
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> pref = settings.getCustomRenderProperty("contourLines");
		if(pref.get().equals("")) {
			for(ApplicationMode m : ApplicationMode.allPossibleValues(settings)) {
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
		HILLSHADE = app.getSettings().registerBooleanPreference("hillshade_layer", true);
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		if (hillshadeLayer != null) {
			activity.getMapView().removeLayer(hillshadeLayer);
		}
		if (HILLSHADE.get()) {
			hillshadeLayer = new HillshadeLayer(activity, this);
			activity.getMapView().addLayer(hillshadeLayer, 0.6f);
		}
	}

	public boolean isHillShadeLayerEnabled() {
		return HILLSHADE.get();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (HILLSHADE.get()) {
			if (hillshadeLayer == null) {
				registerLayers(activity);
			}
		} else {
			if (hillshadeLayer != null) {
				mapView.removeLayer(hillshadeLayer);
				hillshadeLayer = null;
				activity.refreshMap();
			}
		}
	}
	
	@Override
	public void registerLayerContextMenuActions(final OsmandMapTileView mapView, ContextMenuAdapter adapter, final MapActivity mapActivity) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (itemId == R.string.layer_hillshade) {
					HILLSHADE.set(!HILLSHADE.get());
					updateLayers(mapView, mapActivity);
				}
			}
		};
		adapter.item(R.string.layer_hillshade).selected(HILLSHADE.get()? 1 : 0)
			.icons( R.drawable.ic_action_hillshade_dark, R.drawable.ic_action_hillshade_light).listen(listener).position(9).reg();
	}
	
	@Override
	public void disable(OsmandApplication app) {
	}

}
