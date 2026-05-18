package net.osmand.plus.views.layers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererContext.ProviderType;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.TileSourceProxyProvider;
import net.osmand.core.jni.MapLayerConfiguration;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.ParameterType;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.views.MapTileAdapter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.YandexTrafficAdapter;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class MapTileLayer extends BaseMapLayer {

	public static final int OVERZOOM_IN = 2;
	private static final int ADDITIONAL_TILE_CACHE = 3;
	private static final int SHOWN_MAP_ZOOM_MAX = 20;
	private static final int SHOWN_MAP_ZOOM_MIN = 1;

	protected final boolean mainLayer;
	protected ITileSource map;
	protected MapTileAdapter mapTileAdapter;

	protected Paint paintBitmap;
	protected RectF bitmapToDraw = new RectF();
	protected Rect bitmapToZoom = new Rect();

	protected ResourceManager resourceManager;
	protected OsmandSettings settings;

	private boolean upscaleAllowed = true;

	private boolean useSampling;
	private boolean needUpdateProvider = true;
	private boolean visible = true;
	private boolean cachedVisible = true;
	private int cachedAlpha = -1;
	private StateChangedListener<Float> parameterListener;

	public MapTileLayer(@NonNull Context context, boolean mainLayer) {
		super(context);
		this.mainLayer = mainLayer;
		this.settings = getApplication().getSettings();
		this.resourceManager = getApplication().getResourceManager();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean isMapGestureAllowed(MapGestureType type) {
		MapActivity mapActivity = getMapActivity();
		boolean downloadingTiles = mapActivity != null && mapActivity.getFragmentsHelper().getDownloadTilesFragment() != null;
		boolean rotatingOrTiltingMap = type == MapGestureType.TWO_POINTERS_ROTATION || type == MapGestureType.TWO_POINTERS_TILT;
		return !(downloadingTiles && rotatingOrTiltingMap);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		parameterListener = change -> getApplication().runInUIThread(() -> updateParameter(change));
		useSampling = Build.VERSION.SDK_INT < 28;

		paintBitmap = new Paint();
		paintBitmap.setFilterBitmap(true);
		paintBitmap.setAlpha(getAlpha());

		if (mapTileAdapter != null) {
			mapTileAdapter.initLayerAdapter(this, view);
		}

		needUpdateProvider = true;
	}

	@Override
	public void setAlpha(int alpha) {
		super.setAlpha(alpha);

		if (paintBitmap != null) {
			paintBitmap.setAlpha(alpha);
		}
	}

	public void setUpscaleAllowed(boolean upscaleAllowed) {
		this.upscaleAllowed = upscaleAllowed;
	}

	public void setupParameterListener() {
		CommonPreference<Float> paramValuePref = getParamValuePref();
		if (paramValuePref != null) {
			paramValuePref.addListener(parameterListener);
		}
	}

	public void resetParameterListener() {
		CommonPreference<Float> paramValuePref = getParamValuePref();
		if (paramValuePref != null) {
			paramValuePref.removeListener(parameterListener);
		}
	}

	public void updateParameter() {
		if (map != null) {
			ParameterType paramType = map.getParamType();
			if (paramType != ParameterType.UNDEFINED) {
				CommonPreference<Float> paramValuePref = getParamValuePref();
				if (paramValuePref != null) {
					updateParameter(paramValuePref.get());
				}
			}
		}
	}

	public void updateParameter(float newValue) {
		if (map != null) {
			ParameterType paramType = map.getParamType();
			if (paramType != ParameterType.UNDEFINED) {
				CommonPreference<Float> paramStepPref = getParamStepPref();
				if (paramStepPref != null) {
					float currentValue = Float.NaN;
					String param = map.getUrlParameter(TileSourceManager.PARAMETER_NAME);
					if (!Algorithms.isEmpty(param)) {
						try {
							currentValue = Float.parseFloat(param);
						} catch (NumberFormatException ignore) {
						}
					}
					if (paramType == ParameterType.DATE) {
						newValue += System.currentTimeMillis() / 1000f;
					}
					if (Float.isNaN(currentValue) || Math.abs(newValue - currentValue) > 1) {
						map.setUrlParameter(TileSourceManager.PARAMETER_NAME, "" + (long) newValue);
						ResourceManager mgr = resourceManager;
						mgr.clearCacheAndTiles(map);
						getApplication().getOsmandMap().refreshMap();
					}
				}
			}
		}
	}

	public void setMapTileAdapter(@Nullable MapTileAdapter mapTileAdapter) {
		if (this.mapTileAdapter == mapTileAdapter) {
			return;
		}
		if (this.mapTileAdapter != null) {
			this.mapTileAdapter.onClear();
		}
		this.mapTileAdapter = mapTileAdapter;
		if (mapTileAdapter != null && view != null) {
			mapTileAdapter.initLayerAdapter(this, view);
			mapTileAdapter.onInit();
		}
	}

	public void setMapForMapTileAdapter(ITileSource map, MapTileAdapter mapTileAdapter) {
		if (mapTileAdapter == this.mapTileAdapter) {
			this.map = map;
			needUpdateProvider = true;
		}
	}

	public void setMap(@Nullable ITileSource map) {
		MapTileAdapter target = null;
		if (map instanceof TileSourceTemplate) {
			if (TileSourceManager.RULE_YANDEX_TRAFFIC.equals(map.getRule())) {
				map = null;
				target = new YandexTrafficAdapter();
			} else {
				this.map = map;
				long paramMin = map.getParamMin();
				long paramMax = map.getParamMax();
				long paramStep = map.getParamStep();
				CommonPreference<Float> paramMinPref = getParamMinPref();
				CommonPreference<Float> paramMaxPref = getParamMaxPref();
				CommonPreference<Float> paramStepPref = getParamStepPref();
				if (paramMinPref != null && paramMaxPref != null && paramStepPref != null) {
					paramMinPref.set((float) paramMin);
					paramMaxPref.set((float) paramMax);
					paramStepPref.set((float) paramStep);
				}
				updateParameter();
			}
		}
		this.map = map;
		setMapTileAdapter(target);
		needUpdateProvider = true;
	}

	public MapTileAdapter getMapTileAdapter() {
		return mapTileAdapter;
	}

	@Nullable
	public CommonPreference<Float> getParamMinPref() {
		ITileSource map = this.map;
		if (map != null) {
			return settings.registerFloatPreference(map.getName() + "_param_min", 0f).makeProfile().makeShared();
		}
		return null;
	}

	@Nullable
	public CommonPreference<Float> getParamMaxPref() {
		ITileSource map = this.map;
		if (map != null) {
			return settings.registerFloatPreference(map.getName() + "_param_max", 0f).makeProfile().makeShared();
		}
		return null;
	}

	@Nullable
	public CommonPreference<Float> getParamStepPref() {
		ITileSource map = this.map;
		if (map != null) {
			return settings.registerFloatPreference(map.getName() + "_param_step", 0f).makeProfile().makeShared();
		}
		return null;
	}

	@Nullable
	public CommonPreference<Float> getParamValuePref() {
		ITileSource map = this.map;
		if (map != null) {
			return settings.registerFloatPreference(map.getName() + "_param_value", 0f).makeProfile().makeShared();
		}
		return null;
	}

	protected boolean setLayerProvider(@Nullable ITileSource map) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			MapRendererContext mapRendererContext = mainLayer ? NativeCoreContext.getMapRendererContext() : null;
			int layerIndex = view.getLayerIndex(this);
			if (map != null) {
				TileSourceProxyProvider prov = new TileSourceProxyProvider(getApplication(), map);
				mapRenderer.setMapLayerProvider(layerIndex, prov.instantiateProxy(true));
				prov.swigReleaseOwnership();

				if (mapRendererContext != null) {
					mapRendererContext.recreateRasterAndSymbolsProvider(ProviderType.CONTOUR_LINES);
				}
			} else {
				mapRenderer.resetMapLayerProvider(layerIndex);
				if (mapRendererContext != null) {
					mapRendererContext.resetRasterAndSymbolsProvider(ProviderType.CONTOUR_LINES);
				}
			}
			return true;
		}
		return false;
	}

	private void updateLayerProviderAlpha(int alpha) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			MapLayerConfiguration mapLayerConfiguration = new MapLayerConfiguration();
			mapLayerConfiguration.setOpacityFactor(((float) alpha) / 255.0f);
			mapRenderer.setMapLayerConfiguration(view.getLayerIndex(this), mapLayerConfiguration);
		}
	}

	@SuppressLint("WrongCall")
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

		if (mapTileAdapter != null && visible) {
			mapTileAdapter.onDraw(canvas, tilesRect, drawSettings);
		}

		if (hasMapRenderer()) {
			ITileSource map = visible ? this.map : null;
			boolean providerUpdated = false;
			if (needUpdateProvider || mapRendererChanged) {
				providerUpdated = setLayerProvider(map);
				mapRendererChanged = false;
				needUpdateProvider = false;
			} else if (visibleChanged) {
				providerUpdated = setLayerProvider(map);
			}
			if ((alphaChanged || providerUpdated) && map != null) {
				updateLayerProviderAlpha(alpha);
			}
		} else if (visible) {
			drawTileMap(canvas, tilesRect, drawSettings);
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
	}

	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		ITileSource map = this.map;
		if (map == null) {
			return;
		}
		ResourceManager mgr = resourceManager;
		int nzoom = tileBox.getZoom();
		QuadRect tilesRect = tileBox.getTileBounds();

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection  = 0;
		if (map.isEllipticYTile()) {
			ellipticTileCorrection = (float) (MapUtils.getTileEllipsoidNumberY(nzoom, tileBox.getLatitude()) - tileBox.getCenterTileY());
		}

		int left = (int) Math.floor(tilesRect.left);
		int top = (int) Math.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) Math.ceil(tilesRect.right - left);
		int height = (int) Math.ceil(tilesRect.bottom + ellipticTileCorrection - top);

		int tiles = (width + ADDITIONAL_TILE_CACHE) * (height + ADDITIONAL_TILE_CACHE);
		mgr.setMapTileLayerSizes(this, tiles);

		boolean useInternet = (PluginsHelper.isActive(OsmandRasterMapsPlugin.class) || PluginsHelper.isActive(MapillaryPlugin.class))
				&& settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();
		int maxLevel = map.getMaximumZoomSupported();
		int tileSize = map.getTileSize();
		boolean oneTileShown = false;

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int tileX = left + i;
				int tileY = top + j;

				int x1 = tileBox.getPixXFromTileXNoRot(tileX);
				int x2 = tileBox.getPixXFromTileXNoRot(tileX + 1);

				int y1 = tileBox.getPixYFromTileYNoRot(tileY - ellipticTileCorrection);
				int y2 = tileBox.getPixYFromTileYNoRot(tileY + 1 - ellipticTileCorrection);
				bitmapToDraw.set(x1, y1, x2 , y2);

				Bitmap bmp = null;
				String ordImgTile = mgr.calculateTileId(map, tileX, tileY, nzoom);
				// asking tile image async
				boolean imgExist = mgr.isTileDownloaded(ordImgTile, map, tileX, tileY, nzoom);
				boolean originalWillBeLoaded = useInternet && nzoom <= maxLevel;
				if (imgExist || originalWillBeLoaded) {
					bmp = mgr.getBitmapTilesCache().getTileForMapAsync(ordImgTile, map, tileX, tileY,
							nzoom, useInternet, drawSettings.mapRefreshTimestamp);
				}
				if (bmp == null && upscaleAllowed) {
					int div = 1;
					boolean readFromCache = originalWillBeLoaded || imgExist;
					boolean loadIfExists = !readFromCache;
					// asking if there is small version of the map (in cache)
					int allowedScale = Math.min(OVERZOOM_IN + Math.max(0, nzoom - map.getMaximumZoomSupported()), 8);
					int kzoom = 1;
					for (; kzoom <= allowedScale; kzoom++) {
						div *= 2;
						int x = tileX / div;
						int y = tileY / div;
						int zoom = nzoom - kzoom;
						String imgTileId = mgr.calculateTileId(map, x, y, zoom);
						if (readFromCache) {
							bmp = mgr.getBitmapTilesCache().get(imgTileId, drawSettings.mapRefreshTimestamp);
							if (bmp != null) {
								break;
							}
						} else if (loadIfExists) {
							if (mgr.isTileDownloaded(imgTileId, map, x, y, zoom)
									|| (useInternet && zoom <= maxLevel)) {
								bmp = mgr.getBitmapTilesCache().getTileForMapAsync(imgTileId, map, x, y,
										zoom, useInternet, drawSettings.mapRefreshTimestamp);
								break;
							}
						}

					}
					if (bmp != null) {
						if (bmp.getWidth() != tileSize && bmp.getWidth() > 0) {
							tileSize = bmp.getWidth();
						}
						int xZoom = (tileX % div) * tileSize / div;
						int yZoom = (tileY % div) * tileSize / div;
						// nice scale
						boolean useSampling = this.useSampling && kzoom > 3;
						bitmapToZoom.set(Math.max(xZoom, 0), Math.max(yZoom, 0),
								Math.min(xZoom + tileSize / div, tileSize),
								Math.min(yZoom + tileSize / div, tileSize));
						if (!useSampling) {
							canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
						} else {
							int margin = 1;
							int scaledSize = tileSize / div;
							float innerMargin = 0.5f;
							RectF src = new RectF(0, 0, scaledSize, scaledSize);
							if (bitmapToZoom.left >= margin) {
								bitmapToZoom.left -= margin;
								src.left = innerMargin;
								src.right += margin;
							}
							if (bitmapToZoom.top >= margin) {
								bitmapToZoom.top -= margin;
								src.top = innerMargin;
								src.bottom += margin;
							}
							if (bitmapToZoom.right + margin <= tileSize) {
								bitmapToZoom.right += margin;
								src.right += margin - innerMargin;
							}
							if (bitmapToZoom.bottom + margin <= tileSize) {
								bitmapToZoom.bottom += margin;
								src.bottom += margin - innerMargin;
							}
							Matrix m = new Matrix();
							RectF dest = new RectF(0, 0, tileSize, tileSize);
							m.setRectToRect(src, dest, Matrix.ScaleToFit.FILL);
							Bitmap sampled = Bitmap.createBitmap(bmp,
									bitmapToZoom.left, bitmapToZoom.top,
									bitmapToZoom.width(), bitmapToZoom.height(), m, true);
							bitmapToZoom.set(0, 0, tileSize, tileSize);
							// very expensive that's why put in the cache
							mgr.getBitmapTilesCache().put(ordImgTile, sampled, drawSettings.mapRefreshTimestamp);
							canvas.drawBitmap(sampled, bitmapToZoom, bitmapToDraw, paintBitmap);
						}
					}
				} else if (bmp != null) {
					bitmapToZoom.set(0, 0, tileSize, tileSize);
					canvas.drawBitmap(bmp, bitmapToZoom, bitmapToDraw, paintBitmap);
				}
				if (bmp != null) {
					oneTileShown = true;
				}
			}
		}

		if (mainLayer && !oneTileShown && !useInternet && warningToSwitchMapShown < 3) {
			if (resourceManager.getRenderer().containsLatLonMapData(view.getLatitude(), view.getLongitude(), nzoom)) {
				getApplication().showToastMessage(R.string.switch_to_vector_map_to_see);
				warningToSwitchMapShown++;
			}
		}
	}


	@Override
	public int getMaximumShownMapZoom() {
		return map == null ? SHOWN_MAP_ZOOM_MAX : map.getMaximumZoomSupported() + OVERZOOM_IN;
	}

	@Override
	public int getMinimumShownMapZoom() {
		return map == null ? SHOWN_MAP_ZOOM_MIN : map.getMinimumZoomSupported();
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		setMapTileAdapter(null);
		if (resourceManager != null) {
			resourceManager.removeMapTileLayerSize(this);
		}
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		setLayerProvider(null);
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public ITileSource getMap() {
		return map;
	}

}
