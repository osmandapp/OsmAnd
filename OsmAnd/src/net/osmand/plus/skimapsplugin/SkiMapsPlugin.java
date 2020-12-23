package net.osmand.plus.skimapsplugin;

import android.graphics.drawable.Drawable;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.render.RendererRegistry;

import java.util.Collections;
import java.util.List;

public class SkiMapsPlugin extends OsmandPlugin {

	public static final String ID = "skimaps.plugin";
	public static final String COMPONENT = "net.osmand.skimapsPlugin";

	public SkiMapsPlugin(OsmandApplication app) {
		super(app);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(net.osmand.plus.R.string.plugin_ski_descr);
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_ski_name);
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_skiing;
	}
	
	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.ski_map);
	}

	@Override
	public boolean isMarketPlugin() {
		return true;
	}

	@Override
	public String getComponentId1() {
		return COMPONENT;
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/ski-plugin.html";
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.SKI);
	}

	@Override
	public List<String> getRendererNames() {
		return Collections.singletonList(RendererRegistry.WINTER_SKI_RENDER);
	}

	@Override
	public List<String> getRouterNames() {
		return Collections.singletonList("ski");
	}

	@Override
	public String getId() {
		return ID;
	}
}