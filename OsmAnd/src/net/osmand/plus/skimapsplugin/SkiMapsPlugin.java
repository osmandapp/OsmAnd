package net.osmand.plus.skimapsplugin;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;

import java.util.Collections;
import java.util.List;

public class SkiMapsPlugin extends OsmandPlugin {

	public static final String ID = "skimaps.plugin";
	public static final String COMPONENT = "net.osmand.skimapsPlugin";

	private OsmandApplication app;

	public SkiMapsPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public String getDescription() {
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
	public int getAssetResourceName() {
		return R.drawable.ski_map;
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
	public boolean init(@NonNull final OsmandApplication app, final Activity activity) {
		if (activity != null) {
			// called from UI
			ApplicationMode.changeProfileAvailability(ApplicationMode.SKI, true, app);
		}
		return true;
	}

	@Override
	public void onInstall(@NonNull OsmandApplication app, @Nullable Activity activity) {
		ApplicationMode.changeProfileAvailability(ApplicationMode.SKI, true, app);
		super.onInstall(app, activity);
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return Collections.singletonList(ApplicationMode.SKI);
	}

	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		ApplicationMode.changeProfileAvailability(ApplicationMode.SKI, false, app);
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
