package net.osmand.plus.views;

import gnu.trove.list.array.TByteArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.TrackChartPoints;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.MapUtils;
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
import android.support.v4.util.Pair;

public class RouteLayer extends OsmandMapLayer {
	
	private static final float EPSILON_IN_DPI = 2;

	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	// keep array lists created
	private List<Location> actionPoints = new ArrayList<Location>();
	
	private Path path;

	// cache
	private Bitmap coloredArrowUp;
	private Bitmap actionArrow;

	private Paint paintIcon;
	private Paint paintIconAction;
	private Paint paintGridTextIcon;
	private Paint paintInnerRect;

	private Paint paintIconSelected;
	private Bitmap selectedPoint;
	private TrackChartPoints trackChartPoints;

	private RenderingLineAttributes attrs;


	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}

	public void setTrackChartPoints(TrackDetailsMenu.TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
	}

	private void initUI() {
		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);
		path = new Path();
		
		paintIcon = new Paint();
		paintIcon.setFilterBitmap(true);
		paintIcon.setAntiAlias(true);
		paintIcon.setColor(Color.BLACK);
		paintIcon.setStrokeWidth(3);

		paintGridTextIcon = new Paint();
		paintGridTextIcon.setTextAlign(Paint.Align.CENTER);
		paintGridTextIcon.setFakeBoldText(true);
		paintGridTextIcon.setColor(Color.WHITE);
		paintGridTextIcon.setAntiAlias(true);

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

		paintInnerRect = new Paint();
		paintInnerRect.setStyle(Paint.Style.FILL_AND_STROKE);
		paintInnerRect.setAntiAlias(true);
		paintInnerRect.setColor(attrs.defaultColor);
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (helper.getFinalLocation() != null && helper.getRoute().isCalculated()) {
			boolean updatePaints = attrs.updatePaints(view, settings, tileBox);
			attrs.isPaint3 = false;
			attrs.isPaint2 = false;
			if(updatePaints) {
				paintIconAction.setColorFilter(new PorterDuffColorFilter(attrs.paint3.getColor(), Mode.MULTIPLY));
				paintIcon.setColorFilter(new PorterDuffColorFilter(attrs.paint2.getColor(), Mode.MULTIPLY));
			}
			
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

	private void drawXAxisPoints(Canvas canvas, RotatedTileBox tileBox) {
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		List<Pair<String, WptPt>> xAxisPoints = trackChartPoints.getXAxisPoints();
		float r = 12 * tileBox.getDensity();
		paintGridTextIcon.setTextSize(r);
		for (int i = 0; i < xAxisPoints.size(); i++) {
			WptPt axisPoint = xAxisPoints.get(i).second;
			if (axisPoint.getLatitude() >= latLonBounds.bottom
					&& axisPoint.getLatitude() <= latLonBounds.top
					&& axisPoint.getLongitude() >= latLonBounds.left
					&& axisPoint.getLongitude() <= latLonBounds.right) {
				String textOnPoint = xAxisPoints.get(i).first;
				float textWidth = paintGridTextIcon.measureText(textOnPoint);
				float x = tileBox.getPixXFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				float y = tileBox.getPixYFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				canvas.drawRect(
						x - textWidth / 2 - 2 * (float) Math.ceil(tileBox.getDensity()),
						y - r / 2 - 2 * (float) Math.ceil(tileBox.getDensity()),
						x + textWidth / 2 + 2 * (float) Math.ceil(tileBox.getDensity()),
						y + r / 2 + 3 * (float) Math.ceil(tileBox.getDensity()),
						paintInnerRect);
				canvas.drawText(textOnPoint, x, y + r / 2, paintGridTextIcon);
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
		double mapDensity;
		TreeMap<Integer, RouteGeometryZoom> zooms = new TreeMap<>();
		List<Location> locations = Collections.emptyList(); 
		
		// cache arrays
		TIntArrayList tx = new TIntArrayList();
		TIntArrayList ty = new TIntArrayList();
		List<Double> angles = new ArrayList<>();
		List<Double> distances = new ArrayList<>();
		
		public void updateRoute(RotatedTileBox tb, RouteCalculationResult route) {
			if(tb.getMapDensity() != mapDensity || this.route != route) {
				this.route = route;
				if(route == null) {
					locations = Collections.emptyList();
				} else {
					locations = route.getImmutableAllLocations();
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
			boolean previousVisible = false;
			if (lastProjection != null) {
				if (leftLongitude <= lastProjection.getLongitude() && lastProjection.getLongitude() <= rightLongitude
						&& bottomLatitude <= lastProjection.getLatitude() && lastProjection.getLatitude() <= topLatitude) {
					addLocation(tb, lastProjection, tx, ty, angles, distances, 0);
					previousVisible = true;
				}
			}
			List<Location> routeNodes = locations;
			int previous = -1;
			for (int i = currentRoute; i < routeNodes.size(); i++) {
				Location ls = routeNodes.get(i);
				if(simplification.getQuick(i) == 0) {
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
						addLocation(tb, lt, tx, ty, angles, distances, 0); // first point
					}
					addLocation(tb, ls, tx, ty, angles, distances, dist);
					previousVisible = true;
				} else if (previousVisible) {
					addLocation(tb, ls, tx, ty, angles, distances, previous == -1 ? 0 : odistances.get(i));
					double distToFinish = 0;
					for(int ki = i + 1; ki < odistances.size(); ki++) {
						distToFinish += odistances.get(ki);
					}
					drawRouteSegment(tb, canvas, tx, ty, angles, distances, distToFinish);
					previousVisible = false;
					clearArrays();
				}
				previous = i;
			}
			drawRouteSegment(tb, canvas, tx, ty, angles, distances, 0);
		}

		private void clearArrays() {
			tx.clear();
			ty.clear();
			distances.clear();
			angles.clear();
		}

		private void addLocation(RotatedTileBox tb, Location ls, TIntArrayList tx, TIntArrayList ty, 
				List<Double> angles, List<Double> distances, double dist) {
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
		}
	}
	
	RouteSimplificationGeometry routeGeometry  = new RouteSimplificationGeometry();
	
	private void drawRouteSegment(RotatedTileBox tb, Canvas canvas, TIntArrayList tx, TIntArrayList ty,
			List<Double> angles, List<Double> distances, double distToFinish) {
		if(tx.size() < 2) {
			return;
		}
		try {
			path.reset();
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			calculatePath(tb, tx, ty, path);
			attrs.drawPath(canvas, path);
			if (tb.getZoomAnimation() == 0) {
				drawArrowsOverPath(canvas, tb, tx, ty, angles, distances, coloredArrowUp, distToFinish);
			}
		} finally {
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		RouteCalculationResult route = helper.getRoute();
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


	

}
