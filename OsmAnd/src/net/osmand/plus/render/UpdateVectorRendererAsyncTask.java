package net.osmand.plus.render;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.core.android.MapRendererContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRulesStorage;

public class UpdateVectorRendererAsyncTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;

	private final CallbackWithObject<Boolean> callback;

	public UpdateVectorRendererAsyncTask(@NonNull OsmandApplication app, @NonNull CallbackWithObject<Boolean> callback) {
		this.app = app;
		this.callback = callback;
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
				rendererContext.updateMapSettings(true);
			}
		}
		return changed;
	}

	protected void onPostExecute(Boolean changed) {
		if (callback != null) {
			callback.processResult(changed);
		}
	}
}