package net.osmand.plus.render;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.TileSourceProxyProvider;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

public class MapVectorLayer extends BaseMapLayer {

	private OsmandMapTileView view;
	private ResourceManager resourceManager;
	private Paint paintImg;

	private RectF destImage = new RectF();
	private final MapTileLayer tileLayer;
	private boolean visible = false;
	private boolean oldRender = false;
	private String cachedUnderlay;
	private Integer cachedMapTransparency;
	private String cachedOverlay;
	private Integer cachedOverlayTransparency;

	public MapVectorLayer(MapTileLayer tileLayer, boolean oldRender) {
		this.tileLayer = tileLayer;
		this.oldRender = oldRender;
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		resourceManager = view.getApplication().getResourceManager();
		paintImg = new Paint();
		paintImg.setFilterBitmap(true);
		paintImg.setAlpha(getAlpha());
	}

	public boolean isVectorDataVisible() {
		return visible && view.getZoom() >= view.getSettings().LEVEL_TO_SWITCH_VECTOR_RASTER.get();
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

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tilesRect, DrawSettings drawSettings) {
		if (!visible) {
			return;
		}
		// if (!isVectorDataVisible() && tileLayer != null) {
		// tileLayer.drawTileMap(canvas, tilesRect);
		// resourceManager.getRenderer().interruptLoadingMap();
		// } else {
		final MapRendererView mapRenderer = view.getMapRenderer();
		if (mapRenderer != null && !oldRender) {
			NativeCoreContext.getMapRendererContext().setNightMode(drawSettings.isNightMode());
			OsmandSettings st = view.getApplication().getSettings();
			/* TODO: Commented to avoid crash (looks like IMapTiledDataProvider.Request parameter does not pass correctly or cannot be resolved while calling obtainImage method)
			if (!Algorithms.objectEquals(st.MAP_UNDERLAY.get(), cachedUnderlay)) {
				cachedUnderlay = st.MAP_UNDERLAY.get();
				ITileSource tileSource = st.getTileSourceByName(cachedUnderlay, false);
				if (tileSource != null) {
					TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), tileSource);
					mapRenderer.setMapLayerProvider(-1, prov.instantiateProxy(true));
					prov.swigReleaseOwnership();
					// mapRenderer.setMapLayerProvider(-1,
					// net.osmand.core.jni.OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)"));
				} else {
					mapRenderer.resetMapLayerProvider(-1);
				}
			}
			*/
			if (!Algorithms.objectEquals(st.MAP_TRANSPARENCY.get(), cachedMapTransparency)) {
				cachedMapTransparency = st.MAP_TRANSPARENCY.get();
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(((float) cachedMapTransparency) / 255.0f);
				mapRenderer.setMapLayerConfiguration(0, mapLayerConfiguration);
			}
			/* TODO: Commented to avoid crash (looks like IMapTiledDataProvider.Request parameter does not pass correctly or cannot be resolved while calling obtainImage method)
			if (!Algorithms.objectEquals(st.MAP_OVERLAY.get(), cachedOverlay)) {
				cachedOverlay = st.MAP_OVERLAY.get();
				ITileSource tileSource = st.getTileSourceByName(cachedOverlay, false);
				if (tileSource != null) {
					TileSourceProxyProvider prov = new TileSourceProxyProvider(view.getApplication(), tileSource);
					mapRenderer.setMapLayerProvider(1, prov.instantiateProxy(true));
					prov.swigReleaseOwnership();
					// mapRenderer.setMapLayerProvider(1,
					// net.osmand.core.jni.OnlineTileSources.getBuiltIn().createProviderFor("Mapnik (OsmAnd)"));
				} else {
					mapRenderer.resetMapLayerProvider(1);
				}
			}
			if (!Algorithms.objectEquals(st.MAP_OVERLAY_TRANSPARENCY.get(), cachedOverlayTransparency)) {
				cachedOverlayTransparency = st.MAP_OVERLAY_TRANSPARENCY.get();
				MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
				mapLayerConfiguration.setOpacityFactor(((float) cachedOverlayTransparency) / 255.0f);
				mapRenderer.setMapLayerConfiguration(1, mapLayerConfiguration);
			}
			*/
			// opengl renderer
			LatLon ll = tilesRect.getLatLonFromPixel(tilesRect.getPixWidth() / 2, tilesRect.getPixHeight() / 2);
			mapRenderer.setTarget(new PointI(MapUtils.get31TileNumberX(ll.getLongitude()), MapUtils.get31TileNumberY(ll
					.getLatitude())));
			mapRenderer.setAzimuth(-tilesRect.getRotate());
			mapRenderer.setZoom((float) (tilesRect.getZoom() + tilesRect.getZoomAnimation() + tilesRect
					.getZoomFloatPart()));
			float zoomMagnifier = st.MAP_DENSITY.get();
			mapRenderer.setVisualZoomShift(zoomMagnifier - 1.0f);
		} else {
			if (!view.isZooming()) {
				if (resourceManager.updateRenderedMapNeeded(tilesRect, drawSettings)) {
					// pixRect.set(-view.getWidth(), -view.getHeight() / 2, 2 * view.getWidth(), 3 *
					// view.getHeight() / 2);
					final RotatedTileBox copy = tilesRect.copy();
					copy.increasePixelDimensions(copy.getPixWidth() / 3, copy.getPixHeight() / 4);
					resourceManager.updateRendererMap(copy, null);
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
			final RotatedTileBox calc = currentViewport.copy();
			calc.setRotate(bmpLoc.getRotate());
			QuadPointDouble lt = bmpLoc.getLeftTopTile(bmpLoc.getZoom());
			QuadPointDouble rb = bmpLoc.getRightBottomTile(bmpLoc.getZoom());
			final float x1 = calc.getPixXFromTile(lt.x, lt.y, bmpLoc.getZoom());
			final float x2 = calc.getPixXFromTile(rb.x, rb.y, bmpLoc.getZoom());
			final float y1 = calc.getPixYFromTile(lt.x, lt.y, bmpLoc.getZoom());
			final float y2 = calc.getPixYFromTile(rb.x, rb.y, bmpLoc.getZoom());
			
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
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}
}
