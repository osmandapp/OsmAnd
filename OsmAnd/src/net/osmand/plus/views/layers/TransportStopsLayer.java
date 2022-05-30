package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.ResultMatcher;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.BackgroundType;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.TransportStopsTileProvider;
import net.osmand.plus.views.layers.core.TransportStopsTileProvider.StopsCollectionPoint;
import net.osmand.plus.views.layers.geometry.GeometryWay;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class TransportStopsLayer extends OsmandMapLayer implements IContextMenuProvider {

	public static final String TRANSPORT_STOPS_OVER_MAP = "transportStops";

	private static final int startZoom = 12;
	private static final int startZoomRoute = 10;

	private RenderingLineAttributes attrs;

	private MapLayerData<List<TransportStop>> data;
	private TransportStopRoute stopRoute = null;

	private final CommonPreference<Boolean> showTransportStops;

	private Path path;

	//OpenGL
	private float textScale = 1.0f;
	private boolean nightMode = false;
	private TransportStopsTileProvider transportStopsTileProvider;
	private VectorLinesCollection vectorLinesCollection;
	private MapMarkersCollection mapMarkersCollection;
	private int stopRouteDist = 0;
	private TransportStopType stopRouteType = null;
	private boolean mapsInitialized = false;

	public TransportStopsLayer(@NonNull Context context, int baseOrder) {
		super(context);
		OsmandSettings settings = getApplication().getSettings();
		showTransportStops = settings.getCustomRenderBooleanProperty(TRANSPORT_STOPS_OVER_MAP).cache();
		this.baseOrder = baseOrder;
	}

	@Override
	public void initLayer(@NonNull final OsmandMapTileView view) {
		super.initLayer(view);

		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		path = new Path();
		attrs = new RenderingLineAttributes("transport_route");
		attrs.defaultWidth = (int) (6 * view.getDensity());
		data = new OsmandMapLayer.MapLayerData<List<TransportStop>>() {
			{
				ZOOM_THRESHOLD = 0;
			}

			@Override
			public boolean isInterrupted() {
				return super.isInterrupted();
			}

			@Override
			public void layerOnPostExecute() {
				view.refreshMap();
			}

			@Override
			protected List<TransportStop> calculateResult(@NonNull QuadRect latLonBounds, int zoom) {
				try {
					List<TransportStop> res = view.getApplication().getResourceManager().searchTransportSync(latLonBounds.top, latLonBounds.left,
							latLonBounds.bottom, latLonBounds.right, new ResultMatcher<TransportStop>() {

								@Override
								public boolean publish(TransportStop object) {
									return true;
								}

								@Override
								public boolean isCancelled() {
									return isInterrupted();
								}
							});
					Collections.sort(res, (lhs, rhs) -> lhs.getId() < rhs.getId()
							? -1 : (lhs.getId().longValue() == rhs.getId().longValue() ? 0 : 1));
					return res;
				} catch (IOException e) {
					return new ArrayList<>();
				}
			}
		};
		addMapsInitializedListener();
	}

	private void getFromPoint(RotatedTileBox tb, PointF point, List<? super TransportStop> res,
							  List<TransportStop> objects) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		final int rp = getScaledTouchRadius(getApplication(), getRadiusPoi(tb));
		int radius = rp * 3 / 2;
		try {
			TreeSet<String> ms = new TreeSet<>();
			for (int i = 0; i < objects.size(); i++) {
				TransportStop n = objects.get(i);
				if (n.getLocation() == null) {
					continue;
				}
				PointF pixel = NativeUtilities.getPixelFromLatLon(getMapRenderer(), tb,
						n.getLocation().getLatitude(), n.getLocation().getLongitude());
				if (Math.abs(pixel.x - ex) <= radius && Math.abs(pixel.y - ey) <= radius) {
					if (!ms.add(n.getName())) {
						// only unique names
						continue;
					}
					radius = rp;
					res.add(n);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// that's really rare case, but is much efficient than introduce synchronized block
		}
	}

	public TransportStopRoute getRoute() {
		return stopRoute;
	}

	public void setRoute(TransportStopRoute route) {
		this.stopRoute = route;
	}

	private int getRadiusPoi(RotatedTileBox tb){
		final double zoom = tb.getZoom();
		int r;
		if(zoom < startZoomRoute){
			r = 0;
		} else if(zoom <= 15){
			r = 8;
		} else if(zoom <= 16){
			r = 10;
		} else if(zoom <= 17){
			r = 14;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (!mapsInitialized) {
			return;
		}
		List<TransportStop> objects = null;
		boolean nightMode = settings.isNightMode();
		OsmandApplication app = getApplication();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			float textScale = getTextScale();
			int stopRouteDist = stopRoute != null ? stopRoute.distance : 0;
			TransportStopType stopRouteType = stopRoute != null ? stopRoute.type : null;
			if (this.nightMode != nightMode || this.textScale != textScale || mapActivityInvalidated
					|| tb.getZoom() < startZoomRoute || this.stopRouteDist != stopRouteDist
					|| this.stopRouteType != stopRouteType || !showTransportStops.get()) {
				clearTransportRouteCollections();
				clearTransportStopsTileProvider();
			}
			if (tb.getZoom() >= startZoomRoute && showTransportStops.get()) {
				if (stopRoute != null) {
					initTransportRouteCollections();
				} else {
					initTransportStopsTileProvider();
				}
			}
			mapActivityInvalidated = false;
			this.nightMode = nightMode;
			this.textScale = textScale;
			this.stopRouteDist = stopRouteDist;
			this.stopRouteType = stopRouteType;
			return;
		}

		if (tb.getZoom() >= startZoomRoute) {
			if (stopRoute != null) {
				objects = stopRoute.route.getForwardStops();
				int color = stopRoute.getColor(app, nightMode);
				attrs.paint.setColor(color);
				attrs.updatePaints(view.getApplication(), settings, tb);
				try {
					path.reset();
					List<Way> ws = stopRoute.route.getForwardWays();
					if (ws != null) {
						for (Way w : ws) {
							List<Float> tx = new ArrayList<>();
							List<Float> ty = new ArrayList<>();
							for (int i = 0; i < w.getNodes().size(); i++) {
								Node o = w.getNodes().get(i);
								float x = tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
								float y = tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
								tx.add(x);
								ty.add(y);
							}
							GeometryWay.calculatePath(tb, tx, ty, path);
						}
					}
					attrs.drawPath(canvas, path);
				} catch (Exception e) {
					// ignore
				}
			}
		}

		if (showTransportStops.get() && tb.getZoom() >= startZoom && objects == null) {
			data.queryNewData(tb);
			objects = data.getResults();
		}

		if (objects != null) {
			Context ctx = getContext();
			float textScale = getTextScale();
			float iconSize = getIconSize(app);
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tb);
			List<TransportStop> fullObjects = new ArrayList<>();
			for (TransportStop o : objects) {
				float x = tb.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tb.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());

				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(ctx,
							ContextCompat.getColor(ctx, R.color.transport_stop_icon_background),
							true, false, 0, BackgroundType.SQUARE);
					pointImageDrawable.setAlpha(0.9f);
					pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
				} else {
					fullObjects.add(o);
				}
			}

			for (TransportStop o : fullObjects) {
				float x = tb.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				float y = tb.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation().getLongitude());
				if (stopRoute != null) {
					TransportStopType type = TransportStopType.findType(stopRoute.route.getType());
					if (type != null) {
						drawPoint(canvas, textScale, x, y, RenderingIcons.getResId(type.getResName()));
					}
				} else {
					drawPoint(canvas, textScale, x, y, R.drawable.mx_highway_bus_stop);
				}
			}
		}
	}

	private void drawPoint(Canvas canvas, float textScale, float x, float y, @DrawableRes int iconId) {
		PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(getContext(),
				ContextCompat.getColor(getContext(), R.color.transport_stop_icon_background),
				true,false ,iconId, BackgroundType.SQUARE);
		pointImageDrawable.setAlpha(0.9f);
		pointImageDrawable.drawPoint(canvas, x, y, textScale, false);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
		clearTransportStopsTileProvider();
		clearTransportRouteCollections();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if(o instanceof TransportStop){
			return new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_STOP, getContext().getString(R.string.transport_Stop),
					((TransportStop)o).getName());
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
		if(tileBox.getZoom() >= startZoomRoute  && stopRoute != null) {
			getFromPoint(tileBox, point, res, stopRoute.route.getForwardStops());
		} else if (tileBox.getZoom() >= startZoom && data.getResults() != null) {
			getFromPoint(tileBox, point, res, data.getResults());
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if(o instanceof TransportStop){
			return ((TransportStop)o).getLocation();
		}
		return null;
	}

	/**OpenGL*/
	private void initTransportRouteCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || stopRoute == null || stopRoute.route == null) {
			return;
		}
		if (vectorLinesCollection != null || mapMarkersCollection != null) {
			return;
		}
		List<Way> ws = stopRoute.route.getForwardWays();
		if (ws != null) {
			int lineId = 1;
			OsmandApplication app = getApplication();
			int color = stopRoute.getColor(app, nightMode);
			for (Way w : ws) {
				QVectorPointI points = new QVectorPointI();
				for (int i = 0; i < w.getNodes().size(); i++) {
					Node o = w.getNodes().get(i);
					int x = MapUtils.get31TileNumberX(o.getLongitude());
					int y = MapUtils.get31TileNumberY(o.getLatitude());
					points.add(new PointI(x, y));
				}
				if (points.size() > 1) {
					if (vectorLinesCollection == null) {
						vectorLinesCollection = new VectorLinesCollection();
					}
					VectorLineBuilder builder = new VectorLineBuilder();
					builder.setPoints(points)
							.setIsHidden(false)
							.setLineId(lineId++)
							.setLineWidth(attrs.defaultWidth * 1.5d)
							.setFillColor(NativeUtilities.createFColorARGB(color))
							.setApproximationEnabled(false)
							.setBaseOrder(baseOrder);
					builder.buildAndAddToCollection(vectorLinesCollection);
				}
			}
			if (vectorLinesCollection != null) {
				mapRenderer.addSymbolsProvider(vectorLinesCollection);
			}
		}
		List<TransportStop> transportStops = stopRoute.route.getForwardStops();
		String transportRouteType = stopRoute.route.getType();
		if (transportStops.size() > 0) {
			int baseOrder = getBaseOrder() - 1;
			mapMarkersCollection = new MapMarkersCollection();
			for (TransportStop ts : transportStops) {
				StopsCollectionPoint collectionPoint =
						new StopsCollectionPoint(getContext(), ts, getTextScale(), transportRouteType);
				MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
				mapMarkerBuilder
						.setPosition(collectionPoint.getPoint31())
						.setIsHidden(false)
						.setBaseOrder(baseOrder)
						.setPinIcon(collectionPoint.getImageBitmap(true))
						.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
						.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);
				mapMarkerBuilder.buildAndAddToCollection(mapMarkersCollection);
			}
			mapRenderer.addSymbolsProvider(mapMarkersCollection);
		}
	}

	/**OpenGL*/
	private void clearTransportRouteCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (mapMarkersCollection != null) {
			mapRenderer.removeSymbolsProvider(mapMarkersCollection);
			mapMarkersCollection = null;
		}
		if (vectorLinesCollection != null) {
			mapRenderer.removeSymbolsProvider(vectorLinesCollection);
			vectorLinesCollection = null;
		}
	}

	/**OpenGL*/
	private void initTransportStopsTileProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (transportStopsTileProvider == null) {
			transportStopsTileProvider = new TransportStopsTileProvider(getContext(), data, baseOrder, getTextScale());
			transportStopsTileProvider.drawSymbols(mapRenderer);
		}
	}

	/**OpenGL*/
	private void clearTransportStopsTileProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && transportStopsTileProvider != null) {
			transportStopsTileProvider.clearSymbols(mapRenderer);
			transportStopsTileProvider = null;
		}
	}

	private void addMapsInitializedListener() {
		OsmandApplication app = getApplication();
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onStart(AppInitializer init) {
				}

				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
					if (event == AppInitializer.InitEvents.MAPS_INITIALIZED) {
						mapsInitialized = true;
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
				}
			});
		} else {
			mapsInitialized = true;
		}
	}

}
