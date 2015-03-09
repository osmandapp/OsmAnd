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
		return app.getString(net.osmand.plus.R.string.plugin_touringview_descr);
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.touring_map_view;
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_touringview_name);
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
