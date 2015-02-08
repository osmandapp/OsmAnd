package net.osmand.plus.skimapsplugin;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.render.RendererRegistry;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

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
		return "Nautical Maps plugin provides maps for pistes based on OSM data.\n "
				+ "(Here could be description of provided details)\n"
				+ "It changes default rendering style to 'Winter-Ski'. You can change it back in 'Configure Map' menu (Translation)";
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
			if(!app.getResourceManager().getIndexFileNames().containsKey("World-ski"+
					 IndexConstants.BINARY_MAP_INDEX_EXT)){
				Builder dlg = new AlertDialog.Builder(activity);
				dlg.setMessage(net.osmand.plus.R.string.world_ski_missing);
				dlg.setPositiveButton(net.osmand.plus.R.string.default_buttons_ok, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final Intent intent = new Intent(activity, app.getAppCustomization().getDownloadIndexActivity());
						intent.putExtra(DownloadActivity.FILTER_KEY, app.getString(net.osmand.plus.R.string.index_item_world_ski));
						intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
						activity.startActivity(intent);
					}
				});
				dlg.setNegativeButton(net.osmand.plus.R.string.default_buttons_cancel, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						app.getSettings().RENDERER.set(previousRenderer);						
					}
				});
				dlg.show();
			}
			
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
