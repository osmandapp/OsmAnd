package net.osmand.plus.plugins.skimaps;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_SKI_MAPS;

import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.Collections;
import java.util.List;

public class SkiMapsPlugin extends OsmandPlugin {

	public static final String COMPONENT = "net.osmand.skimapsPlugin";

	public SkiMapsPlugin(OsmandApplication app) {
		super(app);
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
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
		return PLUGIN_SKI_MAPS;
	}
}