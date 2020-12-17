package net.osmand.plus.openseamapsplugin;

import android.graphics.drawable.Drawable;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.render.RendererRegistry;

import java.util.Collections;
import java.util.List;


public class NauticalMapsPlugin extends OsmandPlugin {

	public static final String ID = "nauticalPlugin.plugin";
	public static final String COMPONENT = "net.osmand.nauticalPlugin";

	public NauticalMapsPlugin(OsmandApplication app) {
		super(app);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_nautical_map;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.nautical_map);
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
	public CharSequence getDescription() {
		return app.getString(net.osmand.plus.R.string.plugin_nautical_descr);
	}

	@Override
	public String getName() {
		return app.getString(net.osmand.plus.R.string.plugin_nautical_name);
	}

	@Override
	public String getHelpFileName() {
		return "feature_articles/nautical-charts.html";
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.BOAT);
	}

	@Override
	public List<String> getRendererNames() {
		return Collections.singletonList(RendererRegistry.NAUTICAL_RENDER);
	}

	@Override
	public List<String> getRouterNames() {
		return Collections.singletonList("boat");
	}

	@Override
	public String getId() {
		return ID;
	}
}