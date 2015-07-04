package net.osmand.plus.development;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.app.Activity;

public class OsmandDevelopmentPlugin extends OsmandPlugin {
	private static final String ID = "osmand.development";
	private OsmandApplication app;
	private TextInfoWidget fps;
	
	public OsmandDevelopmentPlugin(OsmandApplication app) {
		this.app = app;
		//ApplicationMode.regWidget("fps", new ApplicationMode[0]);
	}
	
	
	@Override
	public String getId() {
		return ID;
	}
	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_development_plugin_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.debugging_and_development);
	}
	
	@Override
	public void registerLayers(MapActivity activity) {
		registerWidget(activity);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if(isActive()) {
			registerWidget(activity);
		} else {
			MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null && fps != null) {
				mapInfoLayer.removeSideWidget(fps);
				mapInfoLayer.recreateControls();
				fps = null;
			}
		}
	}

	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		final OsmandMapTileView mv = activity.getMapView();
		if (mapInfoLayer != null && fps == null) {
			fps = new TextInfoWidget(activity) {
				@Override
				public boolean updateInfo(DrawSettings drawSettings) {
					if(!mv.isMeasureFPS()) {
						mv.setMeasureFPS(true);
					}
					setText("", Integer.toString((int) mv.getFPS()) + "/"
							+ Integer.toString((int) mv.getSecondaryFPS())
							+ " FPS");
					return true;
				}
			};
			mapInfoLayer.registerSideWidget(fps, R.drawable.widget_no_icon,
					R.string.map_widget_fps_info, "fps", false, 30);
			mapInfoLayer.recreateControls();
		}
	}
	
	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return SettingsDevelopmentActivity.class;
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_developer;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.osmand_development;
	}

}
