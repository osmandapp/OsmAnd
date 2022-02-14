package net.osmand.plus.render;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRulesStorage;

import java.util.concurrent.ExecutorService;

public class UpdateVectorRendererAsyncTask extends AsyncTask<Void, Void, Boolean> {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;
	private final CallbackWithObject<Boolean> callback;

	public UpdateVectorRendererAsyncTask(@NonNull OsmandMapTileView mapView,
	                                     @NonNull CallbackWithObject<Boolean> callback) {
		this.mapView = mapView;
		this.app = mapView.getApplication();
		this.settings = app.getSettings();
		this.callback = callback;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		RendererRegistry registry = app.getRendererRegistry();
		RenderingRulesStorage newRenderer = registry.getRenderer(settings.RENDERER.get());
		if (newRenderer == null) {
			newRenderer = registry.defaultRender();
		}
		if (mapView.getMapRenderer() != null) {
			NativeCoreContext.getMapRendererContext().updateMapSettings();
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
		return changed;
	}

	protected void onPostExecute(Boolean changed) {
		if (callback != null) {
			callback.processResult(changed);
		}
	}

	public static void execute(@NonNull OsmandMapTileView mapView,
	                           @NonNull ExecutorService executor,
	                           @NonNull CallbackWithObject<Boolean> callback) {
		UpdateVectorRendererAsyncTask task = new UpdateVectorRendererAsyncTask(mapView, callback);
		task.executeOnExecutor(executor, (Void) null);
	}


}
