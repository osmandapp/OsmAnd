package net.osmand.plus.touringview;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.render.RendererRegistry;
import android.app.Activity;

public class TouringViewPlugin extends OsmandPlugin {

	public static final String ID = "touringView.plugin";
	public static final String COMPONENT = "net.osmand.touringviewPlugin";
	private static String previousRenderer = RendererRegistry.DEFAULT_RENDER;
	private OsmandApplication app;

	public TouringViewPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getDescription() {
		return "Activating this view changes the map style to \'Touring view\', this is a special high-detail view for travelers and professional drivers.\n\n"
				+ "This view provides, at any give map zoom, the maximum amount of travel details available in the map data (particularly roads, tracks, paths, and orientation marks), and clearly depicts all types of roads unambiguously by color coding, which is useful when e.g. driving large vehicles.\n\n"
				+ "A special map download is not needed, the view is created from our standard maps.\n\n"
				+ "This view can be reverted by either de-activating it again here, or by changing the \'Map style\' under \'Configure map\' as desired.";
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_touring_view);
	}

	@Override
	public boolean init(final OsmandApplication app, final Activity activity) {
		if(activity != null) {
			// called from UI 
			previousRenderer = app.getSettings().RENDERER.get(); 
			app.getSettings().RENDERER.set(RendererRegistry.TOURING_VIEW);
		}
		return true;
	}
	
	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		if(app.getSettings().RENDERER.get().equals(RendererRegistry.TOURING_VIEW)) {
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
