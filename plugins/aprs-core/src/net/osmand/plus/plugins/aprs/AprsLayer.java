package net.osmand.plus.plugins.aprs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.RotatedTileBox;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapSelectionRules;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AprsLayer extends OsmandMapLayer implements IContextMenuProvider, AprsDataManager.StationListener {

	public static final int START_ZOOM = 6;
	private static final long APRS_RENDER_REFRESH_INTERVAL_MS = 1000L;

	private final AprsPlugin plugin = PluginsHelper.requirePlugin(AprsPlugin.class);
	private final Map<String, AprsObjectDrawable> drawables = new ConcurrentHashMap<>();

	private MapMarkersCollection markersCollection;
	private long lastRenderRefreshTimeMs;
	private int lastRenderZoom = -1;
	private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

	public AprsLayer(@NonNull Context context) {
		super(context);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
	}

	@Override
	public void cleanupResources() {
		MapRendererView renderer = getMapRenderer();
		if (renderer != null && markersCollection != null) {
			markersCollection.removeAllMarkers();
			renderer.removeSymbolsProvider(markersCollection);
		}
		drawables.clear();
		lastRenderZoom = -1;
		lastRenderRefreshTimeMs = 0;
	}

	@Override
	public void onStationReceived(@NonNull AprsStation station) {
		getApplication().runInUIThread(() -> applyStationUpdate(station));
	}

	@Override
	public void onStationRemoved(@NonNull AprsStation station) {
		getApplication().runInUIThread(() -> {
			AprsObjectDrawable d = drawables.remove(station.getCallsign());
			if (d != null && markersCollection != null) {
				d.clearRenderData(markersCollection);
			}
		});
	}

	private void ensureNativeMarkersCollection() {
		MapRendererView renderer = getMapRenderer();
		if (renderer != null && markersCollection == null) {
			markersCollection = new MapMarkersCollection();
			renderer.addSymbolsProvider(markersCollection);
		}
	}

	private void applyStationUpdate(@NonNull AprsStation station) {
		ensureNativeMarkersCollection();
		AprsObjectDrawable d = drawables.get(station.getCallsign());
		if (d == null) {
			d = new AprsObjectDrawable(plugin, station);
			drawables.put(station.getCallsign(), d);
		} else {
			d.set(station);
		}
		if (getMapRenderer() != null && markersCollection != null) {
			if (!d.hasRenderData()) {
				d.createRenderData(getBaseOrder(), markersCollection);
			}
			d.updateRenderData(getTileView());
		}
		OsmandMapTileView tileView = getTileView();
		if (tileView != null) {
			tileView.refreshMap();
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		MapRendererView renderer = getMapRenderer();
		if (renderer != null) {
			if (mapActivityInvalidated || mapRendererChanged) {
				cleanupResources();
				markersCollection = new MapMarkersCollection();
				renderer.addSymbolsProvider(markersCollection);
				for (AprsStation s : plugin.getDataManager().getStations()) {
					AprsObjectDrawable d = new AprsObjectDrawable(plugin, s);
					drawables.put(s.getCallsign(), d);
					d.createRenderData(getBaseOrder(), markersCollection);
					d.updateRenderData(getTileView());
				}
				updateRenderRefreshState();
			} else if (shouldRefreshNativeRenderData()) {
				for (AprsObjectDrawable d : drawables.values()) {
					d.updateRenderData(getTileView());
				}
				updateRenderRefreshState();
			}
			mapActivityInvalidated = false;
			mapRendererChanged = false;
		} else if (tileBox.getZoom() >= START_ZOOM) {
			for (AprsStation s : plugin.getDataManager().getStations()) {
				if (!s.hasPosition()) {
					continue;
				}
				AprsObjectDrawable d = drawables.get(s.getCallsign());
				if (d == null) {
					d = new AprsObjectDrawable(plugin, s);
					drawables.put(s.getCallsign(), d);
				} else {
					d.set(s);
				}
				if (isLocationVisible(tileBox, s.getLatitude(), s.getLongitude())) {
					d.draw(bitmapPaint, canvas, tileBox);
				}
			}
		}
	}

	private boolean shouldRefreshNativeRenderData() {
		OsmandMapTileView tileView = getTileView();
		if (tileView == null || drawables.isEmpty()) {
			return false;
		}
		long now = System.currentTimeMillis();
		return tileView.getZoom() != lastRenderZoom
				|| now - lastRenderRefreshTimeMs >= APRS_RENDER_REFRESH_INTERVAL_MS;
	}

	private void updateRenderRefreshState() {
		OsmandMapTileView tileView = getTileView();
		lastRenderZoom = tileView != null ? tileView.getZoom() : -1;
		lastRenderRefreshTimeMs = System.currentTimeMillis();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		if (tileBox.getZoom() < START_ZOOM) {
			return;
		}
		MapRendererView renderer = getMapRenderer();
		float radius = getScaledTouchRadius(getApplication(), tileBox.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> polygon = renderer != null
				? NativeUtilities.getPolygon31FromPixelAndRadius(renderer, point, radius) : null;
		for (AprsStation s : plugin.getDataManager().getStations()) {
			if (!s.hasPosition()) {
				continue;
			}
			boolean hit = polygon != null
					? NativeUtilities.isPointInsidePolygon(s.getLatitude(), s.getLongitude(), polygon)
					: tileBox.isLatLonNearPixel(s.getLatitude(), s.getLongitude(), point.x, point.y, radius);
			if (hit) {
				result.collect(s, this);
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof AprsStation s && s.hasPosition()) {
			return new LatLon(s.getLatitude(), s.getLongitude());
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof AprsStation s) {
			return new PointDescription("APRS", s.getCallsign());
		}
		return null;
	}

	public void onStationTapped(@NonNull AprsStation station) {
		if (station.getQrvFrequencyMhz() > 0) {
			plugin.getHamlibBridge().tuneToFrequencyMhz(station.getQrvFrequencyMhz());
		}
	}

	public boolean isLocationVisible(@Nullable RotatedTileBox tileBox, double lat, double lon) {
		return tileBox != null && tileBox.containsLatLon(lat, lon);
	}
}
