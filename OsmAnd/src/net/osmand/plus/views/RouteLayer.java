package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.ColorInt;
import android.util.Pair;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.osm.edit.Way;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.TrackChartPoints;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;

public class RouteLayer extends OsmandMapLayer {
	
	private static final float EPSILON_IN_DPI = 2;

	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private final TransportRoutingHelper transportHelper;
	// keep array lists created
	private List<Location> actionPoints = new ArrayList<Location>();
	
	// cache
	private Bitmap coloredArrowUp;
	private Bitmap actionArrow;

	private Paint paintIcon;
	private Paint paintIconAction;
	private Paint paintGridOuterCircle;
	private Paint paintGridCircle;

	private Paint paintIconSelected;
	private Bitmap selectedPoint;
	private TrackChartPoints trackChartPoints;

	private RenderingLineAttributes attrs;
	private boolean nightMode;


	public RouteLayer(RoutingHelper helper) {
		this.helper = helper;
		this.transportHelper = helper.getTransportRoutingHelper();
	}

	public void setTrackChartPoints(TrackDetailsMenu.TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
	}

	private void initUI() {
		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);

		paintIcon = new Paint();
		paintIcon.setFilterBitmap(true);
		paintIcon.setAntiAlias(true);
		paintIcon.setColor(Color.BLACK);
		paintIcon.setStrokeWidth(3);

		paintIconAction = new Paint();
		paintIconAction.setFilterBitmap(true);
		paintIconAction.setAntiAlias(true);

		attrs = new RenderingLineAttributes("route");
		attrs.defaultWidth = (int) (12 * view.getDensity());
		attrs.defaultWidth3 = (int) (7 * view.getDensity());
		attrs.defaultColor = view.getResources().getColor(R.color.nav_track);
		attrs.paint3.setStrokeCap(Cap.BUTT);
		attrs.paint3.setColor(Color.WHITE);
		
		attrs.paint2.setStrokeCap(Cap.BUTT);
		attrs.paint2.setColor(Color.BLACK);

		paintIconSelected = new Paint();
		selectedPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_default_location);

		paintGridCircle = new Paint();
		paintGridCircle.setStyle(Paint.Style.FILL_AND_STROKE);
		paintGridCircle.setAntiAlias(true);
		paintGridCircle.setColor(attrs.defaultColor);
		paintGridCircle.setAlpha(255);
		paintGridOuterCircle = new Paint();
		paintGridOuterCircle.setStyle(Paint.Style.FILL_AND_STROKE);
		paintGridOuterCircle.setAntiAlias(true);
		paintGridOuterCircle.setColor(Color.WHITE);
		paintGridOuterCircle.setAlpha(204);
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if ((helper.isPublicTransportMode() && transportHelper.getRoutes() != null) ||
				(helper.getFinalLocation() != null && helper.getRoute().isCalculated())) {

			updateAttrs(settings, tileBox);
			
			if(coloredArrowUp == null) {
				Bitmap originalArrowUp = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_route_direction_arrow, null);
				coloredArrowUp = originalArrowUp;
//				coloredArrowUp = Bitmap.createScaledBitmap(originalArrowUp, originalArrowUp.getWidth() * 3 / 4,	
//						originalArrowUp.getHeight() * 3 / 4, true);
			}
			int w = tileBox.getPixWidth();
			int h = tileBox.getPixHeight();
			Location lastProjection = helper.getLastProjection();
			final RotatedTileBox cp ;
			if(lastProjection != null &&
					tileBox.containsLatLon(lastProjection.getLatitude(), lastProjection.getLongitude())){
				cp = tileBox.copy();
				cp.increasePixelDimensions(w /2, h);
			} else {
				cp = tileBox;
			}

			final QuadRect latlonRect = cp.getLatLonBounds();
			double topLatitude = latlonRect.top;
			double leftLongitude = latlonRect.left;
			double bottomLatitude = latlonRect.bottom;
			double rightLongitude = latlonRect.right;
			// double lat = 0; 
			// double lon = 0; 
			// this is buggy lat/lon should be 0 but in that case 
			// it needs to be fixed in case there is no route points in the view bbox
			double lat = topLatitude - bottomLatitude + 0.1;  
			double lon = rightLongitude - leftLongitude + 0.1;
			drawLocations(tileBox, canvas, topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon);

			if (trackChartPoints != null) {
				drawXAxisPoints(canvas, tileBox);
				LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
				if (highlightedPoint != null
						&& highlightedPoint.getLatitude() >= latlonRect.bottom
						&& highlightedPoint.getLatitude() <= latlonRect.top
						&& highlightedPoint.getLongitude() >= latlonRect.left
						&& highlightedPoint.getLongitude() <= latlonRect.right) {
					float x = tileBox.getPixXFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
					float y = tileBox.getPixYFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
					canvas.drawBitmap(selectedPoint, x - selectedPoint.getWidth() / 2, y - selectedPoint.getHeight() / 2, paintIconSelected);
				}
			}
		}
	
	}

	private void updateAttrs(DrawSettings settings, RotatedTileBox tileBox) {
		boolean updatePaints = attrs.updatePaints(view, settings, tileBox);
		attrs.isPaint3 = false;
		attrs.isPaint2 = false;
		if(updatePaints) {
			paintIconAction.setColorFilter(new PorterDuffColorFilter(attrs.paint3.getColor(), Mode.MULTIPLY));
			paintIcon.setColorFilter(new PorterDuffColorFilter(attrs.paint2.getColor(), Mode.MULTIPLY));
		}
		nightMode = settings != null && settings.isNightMode();
	}

	private void drawXAxisPoints(Canvas canvas, RotatedTileBox tileBox) {
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		List<WptPt> xAxisPoints = trackChartPoints.getXAxisPoints();
		float r = 3 * tileBox.getDensity();
		for (int i = 0; i < xAxisPoints.size(); i++) {
			WptPt axisPoint = xAxisPoints.get(i);
			if (axisPoint.getLatitude() >= latLonBounds.bottom
					&& axisPoint.getLatitude() <= latLonBounds.top
					&& axisPoint.getLongitude() >= latLonBounds.left
					&& axisPoint.getLongitude() <= latLonBounds.right) {
				float x = tileBox.getPixXFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				float y = tileBox.getPixYFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				canvas.drawCircle(x, y, r + 2 * (float) Math.ceil(tileBox.getDensity()), paintGridOuterCircle);
				canvas.drawCircle(x, y, r + (float) Math.ceil(tileBox.getDensity()), paintGridCircle);
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {}

	private void drawAction(RotatedTileBox tb, Canvas canvas, List<Location> actionPoints) {
		if (actionPoints.size() > 0) {
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			try {
				Path pth = new Path();
				Matrix matrix = new Matrix();
				boolean first = true;
				int x = 0, px = 0, py = 0, y = 0;
				for (int i = 0; i < actionPoints.size(); i++) {
					Location o = actionPoints.get(i);
					if (o == null) {
						first = true;
						canvas.drawPath(pth, attrs.paint3);
						double angleRad = Math.atan2(y - py, x - px);
						double angle = (angleRad * 180 / Math.PI) + 90f;
						double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
						if (distSegment == 0) {
							continue;
						}
						// int len = (int) (distSegment / pxStep);
						float pdx = x - px;
						float pdy = y - py;
						matrix.reset();
						matrix.postTranslate(0, -actionArrow.getHeight() / 2);
						matrix.postRotate((float) angle, actionArrow.getWidth() / 2, 0);
						matrix.postTranslate(px + pdx - actionArrow.getWidth() / 2, py + pdy);
						canvas.drawBitmap(actionArrow, matrix, paintIconAction);
					} else {
						px = x;
						py = y;
						x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
						y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
						if (first) {
							pth.reset();
							pth.moveTo(x, y);
							first = false;
						} else {
							pth.lineTo(x, y);
						}
					}
				}

			} finally {
				canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}

	@ColorInt
	public int getRouteLineColor(boolean night) {
		updateAttrs(new DrawSettings(night), view.getCurrentRotatedTileBox());
		return attrs.paint.getColor();
	}
	
	private void cullRamerDouglasPeucker(TByteArrayList survivor, List<Location> points,
			int start, int end, double epsillon) {
        double dmax = Double.NEGATIVE_INFINITY;
        int index = -1;
        Location startPt = points.get(start);
        Location endPt = points.get(end);

        for (int i = start + 1; i < end; i++) {
            Location pt = points.get(i);
            double d = MapUtils.getOrthogonalDistance(pt.getLatitude(), pt.getLongitude(), 
            		startPt.getLatitude(), startPt.getLongitude(), endPt.getLatitude(), endPt.getLongitude());
            if (d > dmax) {
                dmax = d;
                index = i;
            }
        }
        if (dmax > epsillon) {
        	cullRamerDouglasPeucker(survivor, points, start, index, epsillon);
        	cullRamerDouglasPeucker(survivor, points, index, end, epsillon);
        } else {
            survivor.set(end, (byte) 1);
        }
    }
	

	private void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, TIntArrayList tx, TIntArrayList ty,
			List<Double> angles, List<Double> distances, Bitmap arrow, double distPixToFinish) {
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int left =  -w / 4;
		int right = w + w / 4;
		int top = - h/4;
		int bottom = h + h/4;
		
		double pxStep = arrow.getHeight() * 4f;
		Matrix matrix = new Matrix();
		double dist = 0;
		if(distPixToFinish != 0) {
			dist = distPixToFinish - pxStep * ((int) (distPixToFinish / pxStep)); // dist < 1
		}
		
		for (int i = tx.size() - 2; i >= 0; i --) {
			int px = tx.get(i);
			int py = ty.get(i);
			int x = tx.get(i + 1);
			int y = ty.get(i + 1);
			double distSegment = distances.get(i + 1);
			double angle = angles.get(i + 1);
//			double angleRad = Math.atan2(y - py, x - px);
//			angle = (angleRad * 180 / Math.PI) + 90f;
//			distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			if(distSegment == 0) {
				continue;
			}
			if(dist >= pxStep) {
				dist = 0; // unnecessary but double check to avoid errors
			}
			double percent = 1 - (pxStep - dist) / distSegment;
			dist += distSegment;
			while(dist >= pxStep) {
				double pdx = (x - px) * percent;
				double pdy = (y - py) * percent;
				if (isIn((int)(px + pdx), (int) (py + pdy), left, top, right, bottom)) {
					float icony = (float) (py + pdy);
					float iconx = (float) (px + pdx - arrow.getWidth() / 2);
					matrix.reset();
					matrix.postTranslate(0, -arrow.getHeight() / 2);
					matrix.postRotate((float) angle, arrow.getWidth() / 2, 0);
					matrix.postTranslate(iconx, icony);
					canvas.drawBitmap(arrow, matrix, paintIcon);
				}
				dist -= pxStep;
				percent -= pxStep / distSegment;
			}
		}
	}
	
	private static class RouteGeometryZoom {
		final TByteArrayList simplifyPoints;
		List<Double> distances;
		List<Double> angles;
		
		public RouteGeometryZoom(List<Location> locations, RotatedTileBox tb) {
			//  this.locations = locations;
			tb = new RotatedTileBox(tb);
			tb.setZoomAndAnimation(tb.getZoom(), 0, tb.getZoomFloatPart());
			simplifyPoints = new TByteArrayList(locations.size());
			distances = new ArrayList<Double>(locations.size());
			angles = new ArrayList<Double>(locations.size());
			simplifyPoints.fill(0, locations.size(), (byte)0);
			if(locations.size() > 0) {
				simplifyPoints.set(0, (byte) 1);
			}
			double distInPix = (tb.getDistance(0, 0, tb.getPixWidth(), 0) / tb.getPixWidth());
			double cullDistance = (distInPix * (EPSILON_IN_DPI * Math.max(1, tb.getDensity())));
			cullRamerDouglasPeucker(simplifyPoints, locations, 0, locations.size() - 1, cullDistance);
			
			int previousIndex = -1;
			for(int i = 0; i < locations.size(); i++) {
				double d = 0;
				double angle = 0;
				if(simplifyPoints.get(i) > 0) {
					if(previousIndex > -1) {
						Location loc = locations.get(i);
						Location pr = locations.get(previousIndex);
						float x = tb.getPixXFromLatLon(loc.getLatitude(), loc.getLongitude());
						float y = tb.getPixYFromLatLon(loc.getLatitude(), loc.getLongitude());
						float px = tb.getPixXFromLatLon(pr.getLatitude(), pr.getLongitude());
						float py = tb.getPixYFromLatLon(pr.getLatitude(), pr.getLongitude());
						d = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
						if(px != x || py != y) {
							double angleRad = Math.atan2(y - py, x - px);
							angle = (angleRad * 180 / Math.PI) + 90f;
						}
					}
					previousIndex = i;
				}
				distances.add(d);
				angles.add(angle);
			}
		}
		
		
		public List<Double> getDistances() {
			return distances;
		}
		
		private void cullRamerDouglasPeucker(TByteArrayList survivor, List<Location> points,
				int start, int end, double epsillon) {
	        double dmax = Double.NEGATIVE_INFINITY;
	        int index = -1;
	        Location startPt = points.get(start);
	        Location endPt = points.get(end);

	        for (int i = start + 1; i < end; i++) {
	            Location pt = points.get(i);
	            double d = MapUtils.getOrthogonalDistance(pt.getLatitude(), pt.getLongitude(), 
	            		startPt.getLatitude(), startPt.getLongitude(), endPt.getLatitude(), endPt.getLongitude());
	            if (d > dmax) {
	                dmax = d;
	                index = i;
	            }
	        }
	        if (dmax > epsillon) {
	        	cullRamerDouglasPeucker(survivor, points, start, index, epsillon);
	        	cullRamerDouglasPeucker(survivor, points, index, end, epsillon);
	        } else {
	            survivor.set(end, (byte) 1);
	        }
	    }
		
		public TByteArrayList getSimplifyPoints() {
			return simplifyPoints;
		}
	}
	
	private class RouteSimplificationGeometry {
		RouteCalculationResult route;
		TransportRouteResult transportRoute;
		double mapDensity;
		TreeMap<Integer, RouteGeometryZoom> zooms = new TreeMap<>();
		List<Location> locations = Collections.emptyList();
		Map<Integer, GeometryWayStyle> styleMap = Collections.emptyMap();

		// cache arrays
		TIntArrayList tx = new TIntArrayList();
		TIntArrayList ty = new TIntArrayList();
		List<Double> angles = new ArrayList<>();
		List<Double> distances = new ArrayList<>();
		List<GeometryWayStyle> styles = new ArrayList<>();

		private GeometryWayStyle getStyle(int index, GeometryWayStyle defaultWayStyle) {
			List<Integer> list = new ArrayList<>(styleMap.keySet());
			for (int i = list.size() - 1; i >= 0; i--) {
				int c = list.get(i);
				if (c <= index) {
					return styleMap.get(c);
				}
			}
			return defaultWayStyle;
		}

		private boolean isTransportRoute() {
			return transportRoute != null;
		}

		public void updateRoute(RotatedTileBox tb, RouteCalculationResult route) {
			if(tb.getMapDensity() != mapDensity || this.route != route) {
				this.route = route;
				if (route == null) {
					locations = Collections.emptyList();
				} else {
					locations = route.getImmutableAllLocations();
				}
				styleMap = Collections.emptyMap();
				this.mapDensity = tb.getMapDensity();
				zooms.clear();
			}
		}

		public void updateTransportRoute(RotatedTileBox tb, TransportRouteResult route) {
			if (tb.getMapDensity() != mapDensity || this.transportRoute != route) {
				this.transportRoute = route;
				if (route == null) {
					locations = Collections.emptyList();
					styleMap = Collections.emptyMap();
				} else {
					LatLon start = transportHelper.getStartLocation();
					LatLon end = transportHelper.getEndLocation();
					List<Way> list = new ArrayList<>();
					List<GeometryWayStyle> styles = new ArrayList<>();
					calculateTransportResult(start, end, route, list, styles);
					List<Location> locs = new ArrayList<>();
					Map<Integer, GeometryWayStyle> stlMap = new TreeMap<>();
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
				this.mapDensity = tb.getMapDensity();
				zooms.clear();
			}
		}

		private RouteGeometryZoom getGeometryZoom(RotatedTileBox tb) {
			RouteGeometryZoom zm = zooms.get(tb.getZoom());
			if(zm == null) {
				zm = new RouteGeometryZoom(locations, tb);
				zooms.put(tb.getZoom(), zm);
			}
			return zm;
		}
		
		
		private void drawSegments(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude,
				double bottomLatitude, double rightLongitude, Location lastProjection, int currentRoute) {
			RouteGeometryZoom geometryZoom = getGeometryZoom(tb);
			TByteArrayList simplification = geometryZoom.getSimplifyPoints();
			List<Double> odistances = geometryZoom.getDistances();
			
			clearArrays();
			GeometryWayStyle defaultWayStyle = new GeometryWayStyle(attrs.paint.getColor(), GeometryWayStyle.WAY_TYPE_SOLID_LINE);
			GeometryWayStyle style = defaultWayStyle;
			boolean previousVisible = false;
			if (lastProjection != null) {
				if (leftLongitude <= lastProjection.getLongitude() && lastProjection.getLongitude() <= rightLongitude
						&& bottomLatitude <= lastProjection.getLatitude() && lastProjection.getLatitude() <= topLatitude) {
					addLocation(tb, lastProjection, style, tx, ty, angles, distances, 0, styles);
					previousVisible = true;
				}
			}
			List<Location> routeNodes = locations;
			int previous = -1;
			for (int i = currentRoute; i < routeNodes.size(); i++) {
				Location ls = routeNodes.get(i);
				style = getStyle(i, defaultWayStyle);
				if (simplification.getQuick(i) == 0 && !styleMap.containsKey(i)) {
					continue;
				}
				if (leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude && bottomLatitude <= ls.getLatitude()
						&& ls.getLatitude() <= topLatitude) {
					double dist = 0;
					if (!previousVisible) {
						Location lt = null;
						if (previous != -1) {
							lt = routeNodes.get(previous);
							dist = odistances.get(i);
						} else if (lastProjection != null) {
							lt = lastProjection;
						}
						addLocation(tb, lt, style, tx, ty, angles, distances, 0, styles); // first point
					}
					addLocation(tb, ls, style, tx, ty, angles, distances, dist, styles);
					previousVisible = true;
				} else if (previousVisible) {
					addLocation(tb, ls, style, tx, ty, angles, distances, previous == -1 ? 0 : odistances.get(i), styles);
					double distToFinish = 0;
					for(int ki = i + 1; ki < odistances.size(); ki++) {
						distToFinish += odistances.get(ki);
					}
					drawRouteSegment(tb, canvas, tx, ty, angles, distances, distToFinish, styles);
					previousVisible = false;
					clearArrays();
				}
				previous = i;
			}
			drawRouteSegment(tb, canvas, tx, ty, angles, distances, 0, styles);
		}

		private void clearArrays() {
			tx.clear();
			ty.clear();
			distances.clear();
			angles.clear();
			styles.clear();
		}

		private void addLocation(RotatedTileBox tb, Location ls, GeometryWayStyle style, TIntArrayList tx, TIntArrayList ty,
				List<Double> angles, List<Double> distances, double dist, List<GeometryWayStyle> styles) {
			float x = tb.getPixXFromLatLon(ls.getLatitude(), ls.getLongitude());
			float y = tb.getPixYFromLatLon(ls.getLatitude(), ls.getLongitude());
			float px = x;
			float py = y;
			int previous = tx.size() - 1;
			if (previous >= 0 && previous < tx.size()) {
				px = tx.get(previous);
				py = ty.get(previous);
			}
			double angle = 0;
			if (px != x || py != y) {
				double angleRad = Math.atan2(y - py, x - px);
				angle = (angleRad * 180 / Math.PI) + 90f;
			}
			double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			if(dist != 0) {
				distSegment = dist;
			}
			tx.add((int) x);
			ty.add((int) y);
			angles.add(angle);
			distances.add(distSegment);
			styles.add(style);
		}
	}
	
	RouteSimplificationGeometry routeGeometry  = new RouteSimplificationGeometry();
	
	private void drawRouteSegment(RotatedTileBox tb, Canvas canvas, TIntArrayList tx, TIntArrayList ty,
			List<Double> angles, List<Double> distances, double distToFinish, List<GeometryWayStyle> styles) {
		if (tx.size() < 2) {
			return;
		}
		try {
			List<Pair<Path, Integer>> paths = new ArrayList<>();
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			calculatePath(tb, tx, ty, styles, paths);
			for (Pair<Path, Integer> pc : paths) {
				attrs.customColor = pc.second;
				attrs.drawPath(canvas, pc.first);
			}
			attrs.customColor = 0;
			if (tb.getZoomAnimation() == 0) {
				drawArrowsOverPath(canvas, tb, tx, ty, angles, distances, coloredArrowUp, distToFinish);
			}
		} finally {
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		if (helper.isPublicTransportMode()) {
			int currentRoute = transportHelper.getCurrentRoute();
			List<TransportRouteResult> routes = transportHelper.getRoutes();
			TransportRouteResult route = routes != null && routes.size() > currentRoute ? routes.get(currentRoute) : null;
			routeGeometry.updateRoute(tb, null);
			routeGeometry.updateTransportRoute(tb, route);
			if (route != null) {
				LatLon start = transportHelper.getStartLocation();
				Location startLocation = new Location("transport");
				startLocation.setLatitude(start.getLatitude());
				startLocation.setLongitude(start.getLongitude());
				routeGeometry.drawSegments(tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude, startLocation, 0);
			}
		} else {
			RouteCalculationResult route = helper.getRoute();
			routeGeometry.updateTransportRoute(tb, null);
			routeGeometry.updateRoute(tb, route);
			routeGeometry.drawSegments(tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
					helper.getLastProjection(), route == null ? 0 : route.getCurrentRoute());
			List<RouteDirectionInfo> rd = helper.getRouteDirections();
			Iterator<RouteDirectionInfo> it = rd.iterator();
			if (tb.getZoom() >= 14) {
				List<Location> actionPoints = calculateActionPoints(topLatitude, leftLongitude, bottomLatitude, rightLongitude, helper.getLastProjection(),
						helper.getRoute().getRouteLocations(), helper.getRoute().getCurrentRoute(), it, tb.getZoom());
				drawAction(tb, canvas, actionPoints);
			}
		}
	}
	
	

	private List<Location> calculateActionPoints(double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude, Location lastProjection, List<Location> routeNodes, int cd,
			Iterator<RouteDirectionInfo> it, int zoom) {
		RouteDirectionInfo nf = null;
		
		double DISTANCE_ACTION = 35;
		if(zoom >= 17) {
			DISTANCE_ACTION = 15;
		} else if (zoom == 15) {
			DISTANCE_ACTION = 70;
		} else if (zoom < 15) {
			DISTANCE_ACTION = 110;
		}
		double actionDist = 0;
		Location previousAction = null; 
		List<Location> actionPoints = this.actionPoints;
		actionPoints.clear();
		int prevFinishPoint = -1;
		for (int routePoint = 0; routePoint < routeNodes.size(); routePoint++) {
			Location loc = routeNodes.get(routePoint);
			if(nf != null) {
				int pnt = nf.routeEndPointOffset == 0 ? nf.routePointOffset : nf.routeEndPointOffset;
				if(pnt < routePoint + cd ) {
					nf = null;
				}
			}
			while (nf == null && it.hasNext()) {
				nf = it.next();
				int pnt = nf.routeEndPointOffset == 0 ? nf.routePointOffset : nf.routeEndPointOffset;
				if (pnt < routePoint + cd) {
					nf = null;
				}
			}
			boolean action = nf != null && (nf.routePointOffset == routePoint + cd ||
					(nf.routePointOffset <= routePoint + cd && routePoint + cd  <= nf.routeEndPointOffset));
			if(!action && previousAction == null) {
				// no need to check
				continue;
			}
			boolean visible = leftLongitude <= loc.getLongitude() && loc.getLongitude() <= rightLongitude && bottomLatitude <= loc.getLatitude()
					&& loc.getLatitude() <= topLatitude;
			if(action && !visible && previousAction == null) {
				continue;
			}
			if (!action) {
				// previousAction != null
				float dist = loc.distanceTo(previousAction);
				actionDist += dist;
				if (actionDist >= DISTANCE_ACTION) {
					actionPoints.add(calculateProjection(1 - (actionDist - DISTANCE_ACTION) / dist, previousAction, loc));
					actionPoints.add(null);
					prevFinishPoint = routePoint;
					previousAction = null;
					actionDist = 0;
				} else {
					actionPoints.add(loc);
					previousAction = loc;
				}
			} else {
				// action point
				if (previousAction == null) {
					addPreviousToActionPoints(actionPoints, lastProjection, routeNodes, DISTANCE_ACTION,
							prevFinishPoint, routePoint, loc);
				}
				actionPoints.add(loc);
				previousAction = loc;
				prevFinishPoint = -1;
				actionDist = 0;
			}
		}
		if(previousAction != null) {
			actionPoints.add(null);
		}
		return actionPoints;
	}


	private void addPreviousToActionPoints(List<Location> actionPoints, Location lastProjection, List<Location> routeNodes, double DISTANCE_ACTION,
			int prevFinishPoint, int routePoint, Location loc) {
		// put some points in front
		int ind = actionPoints.size();
		Location lprevious = loc;
		double dist = 0;
		for (int k = routePoint - 1; k >= -1; k--) {
			Location l = k == -1 ? lastProjection : routeNodes.get(k);
			float locDist = lprevious.distanceTo(l);
			dist += locDist;
			if (dist >= DISTANCE_ACTION) {
				if (locDist > 1) {
					actionPoints.add(ind,
							calculateProjection(1 - (dist - DISTANCE_ACTION) / locDist, lprevious, l));
				}
				break;
			} else {
				actionPoints.add(ind, l);
				lprevious = l;
			}
			if (prevFinishPoint == k) {
				if (ind >= 2) {
					actionPoints.remove(ind - 2);
					actionPoints.remove(ind - 2);
				}
				break;
			}
		}
	}
	
	private Location calculateProjection(double part, Location lp, Location l) {
		Location p = new Location(l);
		p.setLatitude(lp.getLatitude() + part * (l.getLatitude() - lp.getLatitude()));
		p.setLongitude(lp.getLongitude() + part * (l.getLongitude() - lp.getLongitude()));
		return p;
	}


	public RoutingHelper getHelper() {
		return helper;
	}

	
	
	@Override
	public void destroyLayer() {
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	private void calculateTransportResult(LatLon start, LatLon end, TransportRouteResult r, List<Way> res, List<GeometryWayStyle> styles) {
		if (r != null) {
			LatLon p = start;
			TransportRouteResultSegment prev = null;
			for (TransportRouteResultSegment s : r.getSegments()) {
				LatLon floc = s.getStart().getLocation();
				addRouteWalk(prev, s, p, floc, res, styles);
				List<Way> geometry = s.getGeometry();
				res.addAll(geometry);
				addStyle(s.route, geometry.size(), styles);
				p = s.getEnd().getLocation();
				prev = s;
			}
			addRouteWalk(prev, null, p, end, res, styles);
		}
	}

	private void addRouteWalk(TransportRouteResultSegment s1, TransportRouteResultSegment s2,
							  LatLon start, LatLon end, List<Way> res, List<GeometryWayStyle> styles) {
		final RouteCalculationResult walkingRouteSegment = transportHelper.getWalkingRouteSegment(s1, s2);
		if (walkingRouteSegment != null && walkingRouteSegment.getRouteLocations().size() > 0) {
			final List<Location> routeLocations = walkingRouteSegment.getRouteLocations();
			Way way = new Way(-1);
			way.putTag(OSMSettings.OSMTagKey.NAME.getValue(), String.format(Locale.US, "Walk %d m", walkingRouteSegment.getWholeDistance()));
			for (Location l : routeLocations) {
				way.addNode(new Node(l.getLatitude(), l.getLongitude(), -1));
			}
			res.add(way);
			addStyle(null, 1, styles);
		} else {
			double dist = MapUtils.getDistance(start, end);
			Way way = new Way(-1);
			way.putTag(OSMSettings.OSMTagKey.NAME.getValue(), String.format(Locale.US, "Walk %.1f m", dist));
			way.addNode(new Node(start.getLatitude(), start.getLongitude(), -1));
			way.addNode(new Node(end.getLatitude(), end.getLongitude(), -1));
			res.add(way);
			addStyle(null, 1, styles);
		}
	}

	private void addStyle(TransportRoute route, int count, List<GeometryWayStyle> styles) {
		int color;
		int type;
		if (route == null) {
			color = attrs.paint.getColor();
			type = GeometryWayStyle.WAY_TYPE_WALK_LINE;
		} else {
			TransportStopRoute r = new TransportStopRoute();
			r.type = TransportStopType.findType(route.getType());
			r.route = route;
			color = r.getColor(helper.getApplication(), nightMode);
			type = GeometryWayStyle.WAY_TYPE_TRANSPORT_LINE;
		}
		GeometryWayStyle style = new GeometryWayStyle(color, type);
		for (int i = 0; i < count; i++) {
			styles.add(style);
		}
	}
}
