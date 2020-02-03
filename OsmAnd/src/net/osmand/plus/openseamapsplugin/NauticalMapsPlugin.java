package net.osmand.plus.openseamapsplugin;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;

import java.util.Collections;
import java.util.List;


public class NauticalMapsPlugin extends OsmandPlugin {

	public static final String ID = "nauticalPlugin.plugin";
	public static final String COMPONENT = "net.osmand.nauticalPlugin";

	private OsmandApplication app;

	public NauticalMapsPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_nautical_map;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.nautical_map;
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
	public String getDescription() {
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
	public boolean init(@NonNull final OsmandApplication app, final Activity activity) {
		if (activity != null) {
			// called from UI
			ApplicationMode.changeProfileAvailability(ApplicationMode.BOAT, true, app);
		}
		return true;
	}

	@Override
	public void onInstall(@NonNull OsmandApplication app, @Nullable Activity activity) {
		ApplicationMode.changeProfileAvailability(ApplicationMode.BOAT, true, app);
		super.onInstall(app, activity);
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.BOAT);
	}
	
	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		ApplicationMode.changeProfileAvailability(ApplicationMode.BOAT, false, app);
	}

	@Override
	public String getId() {
		return ID;
	}
}