package net.osmand.plus.srtmplugin;

import android.app.Activity;
import android.widget.ArrayAdapter;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;

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

	public SRTMPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_srtm;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.contour_lines;
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
	public boolean init(final OsmandApplication app, Activity activity) {
		HILLSHADE = app.getSettings().registerBooleanPreference("hillshade_layer", true);
		OsmandSettings settings = app.getSettings();
		CommonPreference<String> pref = settings.getCustomRenderProperty("contourLines");
		if(pref.get().equals("")) {
			for(ApplicationMode m : ApplicationMode.allPossibleValues()) {
				if(pref.getModeValue(m).equals("")) {
					pref.setModeValue(m, "13");
				}
			}
		}
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
			public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
				if (itemId == R.string.layer_hillshade) {
					HILLSHADE.set(!HILLSHADE.get());
					updateLayers(mapView, mapActivity);
				}
				return true;
			}
		};
		adapter.item(R.string.layer_hillshade).selected(HILLSHADE.get()? 1 : 0)
			.iconColor( R.drawable.ic_action_hillshade_dark).listen(listener).position(13).layout(R.layout.drawer_list_item).reg();
	}
	
	@Override
	public void disable(OsmandApplication app) {
	}
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}

}
