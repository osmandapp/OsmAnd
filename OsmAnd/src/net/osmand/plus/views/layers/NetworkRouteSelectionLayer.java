package net.osmand.plus.views.layers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.PointDescription.POINT_TYPE_ROUTE;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.CallbackWithObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.ResourceManager.ResourceListener;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.NetworkRouteSelectionTask;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class NetworkRouteSelectionLayer extends OsmandMapLayer implements IContextMenuProvider, ResourceListener {

	private OsmandApplication app;
	private ResourceManager resourceManager;
	private Map<RouteKey, GPXFile> routesCache = new HashMap<>();

	private NetworkRouteSelectionTask selectionTask;

	public NetworkRouteSelectionLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = view.getApplication();
		resourceManager = app.getResourceManager();
		resourceManager.addResourceListener(this);
	}

	private boolean isSelectingRoute() {
		return selectionTask != null && selectionTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	private void cancelRouteSelection() {
		if (selectionTask != null && selectionTask.getStatus() == AsyncTask.Status.RUNNING) {
			selectionTask.cancel(false);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) o;
			if (pair.first instanceof RouteKey && pair.second instanceof QuadRect) {
				QuadRect rect = (QuadRect) pair.second;
				return new LatLon(rect.centerY(), rect.centerX());
			}
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) o;
			if (pair.first instanceof RouteKey && pair.second instanceof QuadRect) {
				RouteKey routeKey = (RouteKey) pair.first;
				Locale locale = LocaleHelper.getPreferredNameLocale(app, routeKey.getSupportedNameLocales());
				String localeId = locale != null ? locale.getLanguage() : null;
				return new PointDescription(POINT_TYPE_ROUTE, routeKey.getRouteName(localeId));
			}
		}
		return null;
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		resourceManager.removeResourceListener(this);
	}

	@Override
	public boolean showMenuAction(@Nullable Object object) {
		if (object instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) object;
			if (pair.first instanceof RouteKey && pair.second instanceof QuadRect) {
				Pair<RouteKey, QuadRect> routePair = (Pair<RouteKey, QuadRect>) pair;

				LatLon latLon = getObjectLocation(object);
				GPXFile gpxFile = routesCache.get(pair.first);
				if (gpxFile == null) {
					if (isSelectingRoute()) {
						cancelRouteSelection();
					}
					loadNetworkGpx(routePair, latLon);
				} else {
					saveAndOpenGpx(gpxFile, routePair, latLon);
				}
				return true;
			}
		}
		return false;
	}

	private void loadNetworkGpx(@NonNull Pair<RouteKey, QuadRect> pair, @NonNull LatLon latLon) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			CallbackWithObject<GPXFile> callback = gpxFile -> {
				if (gpxFile != null && gpxFile.error == null) {
					routesCache.put(pair.first, gpxFile);
					saveAndOpenGpx(gpxFile, pair, latLon);
				}
				return true;
			};
			selectionTask = new NetworkRouteSelectionTask(activity, pair.first, pair.second, callback);
			selectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private void saveAndOpenGpx(@NonNull GPXFile gpxFile, @NonNull Pair<RouteKey, QuadRect> pair, @NonNull LatLon latLon) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			WptPt wptPt = new WptPt();
			wptPt.lat = latLon.getLatitude();
			wptPt.lon = latLon.getLongitude();

			String name = getObjectName(pair).getName();
			String fileName = Algorithms.convertToPermittedFileName(name.endsWith(GPX_FILE_EXT) ? name : name + GPX_FILE_EXT);
			File file = new File(FileUtils.getTempDir(app), fileName);
			GpxUiHelper.saveAndOpenGpx(activity, file, gpxFile, wptPt, null, pair.first);
		}
	}

	@Override
	public void onReaderIndexed(BinaryMapIndexReader reader) {
		clearRouteCache(reader);
	}

	@Override
	public void onReaderClosed(BinaryMapIndexReader reader) {
		clearRouteCache(reader);
	}

	private void clearRouteCache(@NonNull BinaryMapIndexReader reader) {
		Map<RouteKey, GPXFile> cache = new HashMap<>(routesCache);
		for (Iterator<Entry<RouteKey, GPXFile>> iterator = cache.entrySet().iterator(); iterator.hasNext(); ) {
			QuadRect rect = iterator.next().getValue().getRect();
			boolean containsRoute = reader.containsRouteData(MapUtils.get31TileNumberX(rect.left),
					MapUtils.get31TileNumberY(rect.top), MapUtils.get31TileNumberX(rect.right),
					MapUtils.get31TileNumberY(rect.bottom), 15);
			if (containsRoute) {
				iterator.remove();
			}
		}
		routesCache = cache;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation, boolean excludeUntouchableObjects) {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}