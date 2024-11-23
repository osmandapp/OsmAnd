package net.osmand.plus.myplaces.tracks;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import java.util.ArrayList;
import java.util.List;

public abstract class MapBitmapDrawer {

	protected final OsmandApplication app;

	protected final MapDrawParams params;
	protected final List<MapBitmapDrawerListener> listeners = new ArrayList<>();

	protected Bitmap mapBitmap;
	protected RotatedTileBox tileBox;
	protected boolean drawingAllowed = true;


	public MapBitmapDrawer(@NonNull OsmandApplication app, @NonNull MapDrawParams params) {
		this.app = app;
		this.params = params;
	}

	public void addListener(@Nullable MapBitmapDrawerListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(@Nullable MapBitmapDrawerListener listener) {
		listeners.remove(listener);
	}

	public void notifyDrawing() {
		for (MapBitmapDrawerListener listener : listeners) {
			listener.onBitmapDrawing();
		}
	}

	public void notifyDrawn(boolean success) {
		for (MapBitmapDrawerListener listener : listeners) {
			listener.onBitmapDrawn(success);
		}
	}

	public boolean isDrawingAllowed() {
		return drawingAllowed;
	}

	public void setDrawingAllowed(boolean drawingAllowed) {
		this.drawingAllowed = drawingAllowed;
	}

	@NonNull
	public Bitmap getMapBitmap() {
		return mapBitmap;
	}

	public void initAndDraw() {
		notifyDrawing();
		createTileBox();

		DrawSettings drawSettings = new DrawSettings(!app.getSettings().isLightContent(), true);
		ResourceManager resourceManager = app.getResourceManager();
		MapRenderRepositories renderer = resourceManager.getRenderer();
		if (resourceManager.updateRenderedMapNeeded(tileBox, drawSettings)) {
			resourceManager.updateRendererMap(tileBox, interrupted -> app.runInUIThread(() -> {
				if (isDrawingAllowed()) {
					mapBitmap = renderer.getBitmap();
					notifyDrawn(mapBitmap != null);
				}
			}), true);
		}
	}

	protected abstract void createTileBox();
}
