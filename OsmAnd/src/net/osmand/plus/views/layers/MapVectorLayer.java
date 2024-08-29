package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererContext.ProviderType;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SymbolSubsectionConfiguration;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.base.BaseMapLayer;

import androidx.annotation.NonNull;

public class MapVectorLayer extends BaseMapLayer {

	private final ResourceManager resourceManager;
	private Paint paintImg;

	private final RectF destImage = new RectF();
	private boolean visible;
	private boolean cachedVisible = true;
	private int cachedAlpha = -1;
	private boolean cachedLabelsVisible;

	public MapVectorLayer(@NonNull Context context) {
		super(context);
		resourceManager = getApplication().getResourceManager();
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		resetLayerProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		paintImg = new Paint();
		paintImg.setFilterBitmap(true);
		paintImg.setAlpha(getAlpha());
		cachedLabelsVisible = view.getSettings().KEEP_MAP_LABELS_VISIBLE.get();
	}

	public boolean isVectorDataVisible() {
		return visible && view != null && view.getZoom() >= view.getSettings().LEVEL_TO_SWITCH_VECTOR_RASTER.get();
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;

		if (!visible) {
			resourceManager.getRenderer().clearCache();
		}
	}

	@Override
	public int getMaximumShownMapZoom() {
		return 22;
	}

	@Override
	public int getMinimumShownMapZoom() {
		return 1;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
	}

	private void recreateLayerProvider() {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		if (mapContext != null) {
			mapContext.recreateRasterAndSymbolsProvider(ProviderType.MAIN);
		}
	}

	private void resetLayerProvider() {
		MapRendererContext mapContext = NativeCoreContext.getMapRendererContext();
		if (mapContext != null) {
			mapContext.resetRasterAndSymbolsProvider(ProviderType.MAIN);
		}
	}

	private void updateLayerProviderAlpha(int alpha) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
			mapLayerConfiguration.setOpacityFactor(((float) alpha) / 255.0f);
			mapRenderer.setMapLayerConfiguration(MapRendererContext.OBF_RASTER_LAYER, mapLayerConfiguration);

			boolean keepLabels = getApplication().getSettings().KEEP_MAP_LABELS_VISIBLE.get();
			SymbolSubsectionConfiguration symbolSubsectionConfiguration = new SymbolSubsectionConfiguration();
			symbolSubsectionConfiguration.setOpacityFactor(keepLabels ? 1.0f : ((float) alpha) / 255.0f);
			mapRenderer.setSymbolSubsectionConfiguration(MapRendererContext.OBF_SYMBOL_SECTION,
				symbolSubsectionConfiguration);
		}
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tilesRect, drawSettings);
		if (view == null) {
			return;
		}
		boolean visible = isVisible();
		boolean visibleChanged = cachedVisible != visible;
		cachedVisible = visible;

		int alpha = getAlpha();
		boolean alphaChanged = cachedAlpha != alpha;
		cachedAlpha = alpha;

		boolean labelsVisible = view.getSettings().KEEP_MAP_LABELS_VISIBLE.get();
		boolean labelsVisibleChanged = cachedLabelsVisible != labelsVisible;
		this.cachedLabelsVisible = labelsVisible;

		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			// opengl renderer
			if (visibleChanged || mapRendererChanged) {
				if (visible) {
					recreateLayerProvider();
				} else {
					resetLayerProvider();
				}
			}
			if (visible) {
				MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
				if (mapRendererContext != null) {
					mapRendererContext.setNightMode(drawSettings.isNightMode());
					mapRendererContext.updateLocalization();
				}
			}
			if ((mapRendererChanged || alphaChanged || visibleChanged || labelsVisibleChanged) && visible) {
				updateLayerProviderAlpha(alpha);
			}

			if (mapActivityInvalidated || mapRendererChanged) {
				mapRenderer.setTarget(new PointI(tilesRect.getCenter31X(), tilesRect.getCenter31Y()));
				mapRenderer.setAzimuth(-tilesRect.getRotate());
				mapRenderer.setZoom((float) (tilesRect.getZoom() + tilesRect.getZoomAnimation() + tilesRect
						.getZoomFloatPart()));
				float zoomMagnifier = getMapDensity();
				mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);
			}
			mapRendererChanged = false;
			mapActivityInvalidated = false;
		} else if (visible) {
			if (!view.isZooming()) {
				if (resourceManager.updateRenderedMapNeeded(tilesRect, drawSettings)) {
					// pixRect.set(-view.getWidth(), -view.getHeight() / 2, 2 * view.getWidth(), 3 *
					// view.getHeight() / 2);
					RotatedTileBox copy = tilesRect.copy();
					copy.increasePixelDimensions(copy.getPixWidth() / 3, copy.getPixHeight() / 4);
					resourceManager.updateRendererMap(copy);
				}
			}
			MapRenderRepositories renderer = resourceManager.getRenderer();
			drawRenderedMap(canvas, renderer.getBitmap(), renderer.getBitmapLocation(), tilesRect);
			drawRenderedMap(canvas, renderer.getPrevBitmap(), renderer.getPrevBmpLocation(), tilesRect);
		}
	}

	private boolean drawRenderedMap(Canvas canvas, Bitmap bmp, RotatedTileBox bmpLoc, RotatedTileBox currentViewport) {
		boolean shown = false;
		if (bmp != null && bmpLoc != null) {
			float rot = -bmpLoc.getRotate();
			canvas.rotate(rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
			RotatedTileBox calc = currentViewport.copy();
			calc.setRotate(bmpLoc.getRotate());
			QuadPointDouble lt = bmpLoc.getLeftTopTile(bmpLoc.getZoom());
			QuadPointDouble rb = bmpLoc.getRightBottomTile(bmpLoc.getZoom());
			float x1 = calc.getPixXFromTile(lt.x, lt.y, bmpLoc.getZoom());
			float x2 = calc.getPixXFromTile(rb.x, rb.y, bmpLoc.getZoom());
			float y1 = calc.getPixYFromTile(lt.x, lt.y, bmpLoc.getZoom());
			float y2 = calc.getPixYFromTile(rb.x, rb.y, bmpLoc.getZoom());

//			LatLon lt = bmpLoc.getLeftTopLatLon();
//			LatLon rb = bmpLoc.getRightBottomLatLon();
//			final float x1 = calc.getPixXFromLatLon(lt.getLatitude(), lt.getLongitude());
//			final float x2 = calc.getPixXFromLatLon(rb.getLatitude(), rb.getLongitude());
//			final float y1 = calc.getPixYFromLatLon(lt.getLatitude(), lt.getLongitude());
//			final float y2 = calc.getPixYFromLatLon(rb.getLatitude(), rb.getLongitude());
			destImage.set(x1, y1, x2, y2);
			if (!bmp.isRecycled()) {
				canvas.drawBitmap(bmp, null, destImage, paintImg);
				shown = true;
			}
			canvas.rotate(-rot, currentViewport.getCenterPixelX(), currentViewport.getCenterPixelY());
		}
		return shown;
	}

	@Override
	public void setAlpha(int alpha) {
		super.setAlpha(alpha);

		if (paintImg != null) {
			paintImg.setAlpha(alpha);
		}
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}
}
