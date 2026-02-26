package net.osmand.plus.render;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRulesStorage;

public class UpdateVectorRendererAsyncTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;

	private final boolean updateMapRenderer;
	private final CallbackWithObject<Boolean> callback;

	public UpdateVectorRendererAsyncTask(@NonNull OsmandApplication app, boolean updateMapRenderer,
	                                     @NonNull CallbackWithObject<Boolean> callback) {
		this.app = app;
		this.callback = callback;
		this.updateMapRenderer = updateMapRenderer;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		OsmandSettings settings = app.getSettings();
		RendererRegistry registry = app.getRendererRegistry();
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();

		RenderingRulesStorage newRenderer = registry.getRenderer(settings.RENDERER.get());
		if (newRenderer == null) {
			newRenderer = registry.defaultRender();
		}
		boolean changed = registry.getCurrentSelectedRenderer() != newRenderer;
		if (changed) {
			registry.setCurrentSelectedRender(newRenderer);
			app.getResourceManager().getRenderer().clearCache();
			mapView.resetDefaultColor();
			mapView.refreshMap(true);
		} else {
			mapView.resetDefaultColor();
		}
		if (mapView.hasMapRenderer()) {
			MapRendererContext rendererContext = NativeCoreContext.getMapRendererContext();
			if (rendererContext != null) {
				rendererContext.updateMapSettings(updateMapRenderer);
			}
		}
		return changed;
	}

	protected void onPostExecute(Boolean changed) {
		if (changed) {
			PluginsHelper.registerRenderingPreferences(app);
		}
		if (callback != null) {
			callback.processResult(changed);
		}
	}
}