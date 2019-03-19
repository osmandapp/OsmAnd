package net.osmand.plus.skimapsplugin;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import net.osmand.IndexConstants;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.render.RendererRegistry;

import java.util.LinkedHashSet;
import java.util.Set;

public class SkiMapsPlugin extends OsmandPlugin {

	public static final String ID = "skimaps.plugin";
	public static final String COMPONENT = "net.osmand.skimapsPlugin";
	private static String previousRenderer = RendererRegistry.DEFAULT_RENDER;
	private OsmandApplication app;
	
	public SkiMapsPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_plugin_skimaps;
	}
	
	@Override
	public int getAssetResourceName() {
		return R.drawable.ski_map;
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
	public String getHelpFileName() {
		return "feature_articles/ski-plugin.html";
	}

	@Override
	public boolean init(@NonNull final OsmandApplication app, final Activity activity) {
		if(activity != null) {
			// called from UI 
			previousRenderer = app.getSettings().RENDERER.get(); 
			app.getSettings().RENDERER.set(RendererRegistry.WINTER_SKI_RENDER);
		}
		return true;
	}

	public void addSkiProfile(boolean flag) {
		Set<ApplicationMode> selectedProfiles = new LinkedHashSet<>(ApplicationMode.values(app));
		boolean isSkiEnabled = selectedProfiles.contains(ApplicationMode.SKI);
		if((!isSkiEnabled && flag) || (isSkiEnabled && !flag)) {
			String s = app.getSettings().AVAILABLE_APP_MODES.get();
			String currModes = flag ? s + ApplicationMode.SKI.getStringKey() + ","
					: s.replace(ApplicationMode.SKI.getStringKey() + ",", "");
			app.getSettings().AVAILABLE_APP_MODES.set(currModes);
		}
	}
	
	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		if(app.getSettings().RENDERER.get().equals(RendererRegistry.WINTER_SKI_RENDER)) {
			app.getSettings().RENDERER.set(previousRenderer);
		}
		addSkiProfile(false);
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
