package net.osmand.plus.skimapsplugin;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.render.RendererRegistry;
import android.app.Activity;

public class SkiMapsPlugin extends OsmandPlugin {

	public static final String ID = "skimaps.plugin";
	public static final String COMPONENT = "net.osmand.skimapsPlugin";
	private OsmandApplication app;
	private String previousRenderer = RendererRegistry.DEFAULT_RENDER;
	
	public SkiMapsPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getDescription() {
		return "Activating this view changes the map style to \'Winter and ski\', showing all landscape features under wintery conditions."
				+ "A global map displaying all ski slopes and cross country ski routes in this view is available under Downloads as \'World ski map\'.\n\n"
				+ "This view can be reverted by either de-activating it again here, or by changing the \'Map style\' under \'Configure Maps\' as desired.";
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_ski_name);
	}
	@Override
	public boolean init(final OsmandApplication app, final Activity activity) {
		if(activity != null) {
			// called from UI 
			previousRenderer = app.getSettings().RENDERER.get(); 
			app.getSettings().RENDERER.set(RendererRegistry.WINTER_SKI_RENDER);
		}
		return true;
	}
	
	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		if(app.getSettings().RENDERER.get().equals(RendererRegistry.WINTER_SKI_RENDER)) {
			app.getSettings().RENDERER.set(previousRenderer);
		}
	}


	@Override
	public String getId() {
		return ID;
	}


	@Override
	public Class<? extends Activity> getSettingsActivity() {
		return null;
	}
}
