package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.router.TransportRoutePlanner;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRouteResult;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PublicTransportGeometryWay extends GeometryWay<PublicTransportGeometryWayContext, PublicTransportGeometryWayDrawer> {

	private final TransportRoutingHelper transportHelper;
	private TransportRouteResult route;
	private static final Log LOG = PlatformUtil.getLog(PublicTransportGeometryWay.class);

	//OpenGL
	public MapMarkersCollection transportRouteMarkers;

	public PublicTransportGeometryWay(PublicTransportGeometryWayContext context) {
		super(context, new PublicTransportGeometryWayDrawer(context));
		this.transportHelper = context.getApp().getTransportRoutingHelper();
	}

	@NonNull
	@Override
	public GeometryWayStyle<PublicTransportGeometryWayContext> getDefaultWayStyle() {
		return new GeometryWalkWayStyle(getContext());
	}

	public boolean updateRoute(RotatedTileBox tb, TransportRouteResult route) {
		if (tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			List<Location> locations;
			Map<Integer, GeometryWayStyle<?>> styleMap;
			if (route == null) {
				locations = Collections.emptyList();
				styleMap = Collections.emptyMap();
			} else {
				LatLon start = transportHelper.getStartLocation();
				LatLon end = transportHelper.getEndLocation();
				List<Way> list = new ArrayList<>();
				List<GeometryWayStyle<?>> styles = new ArrayList<>();
				calculateTransportResult(start, end, route, list, styles);
				List<Location> locs = new ArrayList<>();
				Map<Integer, GeometryWayStyle<?>> stlMap = new TreeMap<>();
				int i = 0;
				int k = 0;
				if (list.size() > 0) {
					for (Way w : list) {
						stlMap.put(k, styles.get(i++));
						for (Node n : w.getNodes()) {
							Location ln = new Location("");
							ln.setLatitude(n.getLatitude());
							ln.setLongitude(n.getLongitude());
							locs.add(ln);
							k++;
						}
					}
				}
				locations = locs;
				styleMap = stlMap;
			}
			updateWay(locations, styleMap, tb);
			return true;
		}
		return false;
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	private void calculateTransportResult(LatLon start, LatLon end, TransportRouteResult r, List<Way> res, List<GeometryWayStyle<?>> styles) {
		if (r != null) {
			LatLon p = start;
			TransportRouteResultSegment prev = null;
			for (TransportRouteResultSegment s : r.getSegments()) {
				LatLon floc = s.getStart().getLocation();
				addRouteWalk(prev, s, p, floc, res, styles);
				List<Way> geometry = s.getGeometry();
				res.addAll(geometry);
				addStyle(s, geometry, styles);
				p = s.getEnd().getLocation();
				prev = s;
			}
			addRouteWalk(prev, null, p, end, res, styles);
		}
	}

	private void addRouteWalk(TransportRouteResultSegment s1, TransportRouteResultSegment s2,
							  LatLon start, LatLon end, List<Way> res, List<GeometryWayStyle<?>> styles) {
		RouteCalculationResult walkingRouteSegment = transportHelper.getWalkingRouteSegment(s1, s2);
		if (walkingRouteSegment != null && walkingRouteSegment.getRouteLocations().size() > 0) {
			List<Location> routeLocations = walkingRouteSegment.getRouteLocations();
			Way way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
			way.putTag(OSMSettings.OSMTagKey.NAME.getValue(), String.format(Locale.US, "Walk %d m", walkingRouteSegment.getWholeDistance()));
			for (Location l : routeLocations) {
				way.addNode(new Node(l.getLatitude(), l.getLongitude(), -1));
			}
			res.add(way);
			addStyle(null, Collections.singletonList(way), styles);
		} else {
			double dist = MapUtils.getDistance(start, end);
			Way way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
			way.putTag(OSMSettings.OSMTagKey.NAME.getValue(), String.format(Locale.US, "Walk %.1f m", dist));
			way.addNode(new Node(start.getLatitude(), start.getLongitude(), -1));
			way.addNode(new Node(end.getLatitude(), end.getLongitude(), -1));
			res.add(way);
			addStyle(null, Collections.singletonList(way), styles);
		}
	}

	private void addStyle(TransportRouteResultSegment segment, List<Way> geometry, List<GeometryWayStyle<?>> styles) {
		PublicTransportGeometryWayContext context = getContext();
		GeometryWayStyle<?> style;
		Way w = geometry.get(0);
		if (segment == null || segment.route == null) {
			style = new GeometryWalkWayStyle(context);
		} else if (w.getId() == TransportRoutePlanner.GEOMETRY_WAY_ID) {
			style = new GeometryTransportWayStyle(context, segment);
		} else {
			style = new TransportStopsWayStyle(context, segment);
		}
		for (int i = 0; i < geometry.size(); i++) {
			styles.add(style);
		}
	}

	@Override
	public void drawRouteSegment(@NonNull RotatedTileBox tb, @Nullable Canvas canvas, List<GeometryWayPoint> points, double distToFinish) {
		super.drawRouteSegment(tb, canvas, points, distToFinish);

		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			MapMarkersCollection transportRouteMarkers = this.transportRouteMarkers;
			if (transportRouteMarkers == null || !mapRenderer.hasSymbolsProvider(transportRouteMarkers)) {
				transportRouteMarkers = new MapMarkersCollection();
				drawTransportStops(transportRouteMarkers);
				if (!transportRouteMarkers.getMarkers().isEmpty()) {
					mapRenderer.addSymbolsProvider(transportRouteMarkers);
					this.transportRouteMarkers = transportRouteMarkers;
				}
			}
		}
	}

	private void drawTransportStops(@NonNull MapMarkersCollection transportRouteMarkers) {
		GeometryAnchorWayStyle anchorWayStyle = new GeometryAnchorWayStyle(getContext());
		for (Map.Entry<Integer, GeometryWayStyle<?>> entry : styleMap.entrySet()) {
			GeometryWayStyle<?> style = entry.getValue();
			boolean transportStyle = style instanceof GeometryTransportWayStyle;
			if (style != null && transportStyle) {
				GeometryTransportWayStyle wayStyle = (GeometryTransportWayStyle)style;
				List<TransportStop> transportStops = wayStyle.getRoute().getForwardStops();
				TransportRouteResultSegment segment = wayStyle.getSegment();
				int start = segment.start;
				int end = segment.end;
				for (int i = start; i <= end; i++) {
					TransportStop stop = transportStops.get(i);
					int x = MapUtils.get31TileNumberX(stop.getLocation().getLongitude());
					int y = MapUtils.get31TileNumberY(stop.getLocation().getLatitude());
					Bitmap icon = (i == start || i == end) ? anchorWayStyle.getPointBitmap() : wayStyle.getStopBitmap();

					MapMarkerBuilder transportMarkerBuilder = new MapMarkerBuilder();
					transportMarkerBuilder
							.setIsAccuracyCircleSupported(false)
							.setBaseOrder(baseOrder - 1500)
							.setPosition(new PointI(x, y))
							.setIsHidden(false)
							.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
							.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
							.setPinIcon(NativeUtilities.createSkImageFromBitmap(icon));
					transportMarkerBuilder.buildAndAddToCollection(transportRouteMarkers);
				}
			}
		}
	}

	public void resetSymbolProviders() {
		super.resetSymbolProviders();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (transportRouteMarkers != null) {
				mapRenderer.removeSymbolsProvider(transportRouteMarkers);
				transportRouteMarkers = null;
			}
		}
	}

	public static class GeometryWalkWayStyle extends PublicTransportGeometryWayStyle {

		GeometryWalkWayStyle(PublicTransportGeometryWayContext context) {
			super(context);
		}

		@Override
		public boolean hasPathLine() {
			return false;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometryWalkWayStyle;
		}

		public Bitmap getPointBitmap() {
			return getContext().getWalkArrowBitmap();
		}

		@Override
		public boolean hasPaintedPointBitmap() {
			return true;
		}

		@Override
		public double getPointStepPx(double zoomCoef) {
			return getPointBitmap().getHeight() * 1.2f * zoomCoef;
		}

		@Override
		public boolean isVisibleWhileZooming() {
			return true;
		}
	}

	public static class GeometryAnchorWayStyle extends GeometryWayStyle<PublicTransportGeometryWayContext> {

		GeometryAnchorWayStyle(PublicTransportGeometryWayContext context) {
			super(context);
		}

		@Override
		public boolean hasPathLine() {
			return false;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometryAnchorWayStyle;
		}

		public Bitmap getPointBitmap() {
			return getContext().getAnchorBitmap();
		}

		@Override
		public boolean hasPaintedPointBitmap() {
			return true;
		}
	}

	public static class TransportStopsWayStyle extends GeometryTransportWayStyle {
		TransportStopsWayStyle(PublicTransportGeometryWayContext context, TransportRouteResultSegment segment) {
			super(context, segment);
			OsmandApplication app = (OsmandApplication) getCtx().getApplicationContext();
			this.color = ContextCompat.getColor(app, R.color.icon_color_default_light);
			this.pointColor = ColorUtilities.getContrastColor(app, color, true);
		}
	}

	public static class GeometryTransportWayStyle extends GeometryWayStyle<PublicTransportGeometryWayContext> {

		private final TransportRouteResultSegment segment;
		private Drawable stopDrawable;
		protected Integer pointColor;

		GeometryTransportWayStyle(PublicTransportGeometryWayContext context, TransportRouteResultSegment segment) {
			super(context);
			this.segment = segment;

			TransportStopRoute r = new TransportStopRoute();
			TransportRoute route = segment.route;
			r.type = TransportStopType.findType(route.getType());
			r.route = route;
			OsmandApplication app = (OsmandApplication) getCtx().getApplicationContext();
			this.color = r.getRouteColor(app, isNightMode());
			this.pointColor = ColorUtilities.getContrastColor(app, color, true);

			TransportStopType type = TransportStopType.findType(route.getType());
			if (type == null) {
				type = TransportStopType.findType("bus");
			}
			if (type != null) {
				stopDrawable = RenderingIcons.getDrawableIcon(getCtx(), type.getResName(), false);
			}
		}

		public TransportRouteResultSegment getSegment() {
			return segment;
		}

		public TransportRoute getRoute() {
			return segment.route;
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public Integer getPointColor() {
			return pointColor;
		}

		public Bitmap getStopBitmap() {
			return getContext().getStopShieldBitmap(color, stopDrawable);
		}

		public Bitmap getStopSmallBitmap() {
			return getContext().getStopSmallShieldBitmap(color);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			if (!(other instanceof GeometryTransportWayStyle)) {
				return false;
			}
			return getRoute() == ((GeometryTransportWayStyle) other).getRoute();
		}
	}
}
