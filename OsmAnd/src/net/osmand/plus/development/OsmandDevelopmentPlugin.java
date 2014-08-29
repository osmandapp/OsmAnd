package net.osmand.plus.development;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.content.Intent;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

public class OsmandDevelopmentPlugin extends OsmandPlugin {
	private static final String ID = "osmand.development";
	private OsmandApplication app;
	private TextInfoWidget fps;
	
	public OsmandDevelopmentPlugin(OsmandApplication app) {
		this.app = app;
		//ApplicationMode.regWidget("fps", new ApplicationMode[0]);
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		return true;
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

	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		final OsmandMapTileView mv = activity.getMapView();
		if (mapInfoLayer != null) {
			fps = new TextInfoWidget(activity, 0, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText()) {
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
			mapInfoLayer.getMapInfoControls().registerSideWidget(fps, R.drawable.widget_no_icon,
					R.string.map_widget_fps_info, "fps", false, 30);
			mapInfoLayer.recreateControls();
		}
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference grp = new Preference(activity);
		grp.setTitle(R.string.debugging_and_development);
		grp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, SettingsDevelopmentActivity.class));
				return true;
			}
		});
		screen.addPreference(grp);
	}

}
