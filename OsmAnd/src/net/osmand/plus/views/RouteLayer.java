package net.osmand.plus.views;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;

public class RouteLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private List<Location> points = new ArrayList<Location>();
	private List<Location> actionPoints = new ArrayList<Location>();

	private Path path;

	// cache
	private Bitmap coloredArrowUp;
	private Bitmap actionArrow;

	private Paint paintIcon;
	private Paint paintIconAction;

	private RenderingLineAttributes attrs;


	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);
		path = new Path();
		
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
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	public void updateLayerStyle() {
		attrs.cachedHash = -1;
	}
	
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		path.reset();
		if (helper.getFinalLocation() != null && helper.getRoute().isCalculated()) {
			boolean updatePaints = attrs.updatePaints(view, settings, tileBox);
			attrs.isPaint3 = false;
			if(updatePaints) {
				paintIconAction.setColorFilter(new PorterDuffColorFilter(attrs.paint3.getColor(), Mode.MULTIPLY));
			}
			
			if(coloredArrowUp == null) {
				Bitmap originalArrowUp = BitmapFactory.decodeResource(view.getResources(), R.drawable.h_arrow, null);
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
			double lat = topLatitude - bottomLatitude + 0.1;
			double lon = rightLongitude - leftLongitude + 0.1;
			drawLocations(tileBox, canvas, topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon);
		}
	
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {}

	private void drawAction(RotatedTileBox tb, Canvas canvas) {
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

	private void drawSegment(RotatedTileBox tb, Canvas canvas) {
		if (points.size() > 0) {
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			try {
				TIntArrayList tx = new TIntArrayList();
				TIntArrayList ty = new TIntArrayList();
				for (int i = 0; i < points.size(); i++) {
					Location o = points.get(i);
					int x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
					int y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
					tx.add(x);
					ty.add(y);
				}
				calculatePath(tb, tx, ty, path);
				attrs.drawPath(canvas, path);
				if (tb.getZoomAnimation() == 0) {
					TIntArrayList lst = new TIntArrayList(50);
					calculateSplitPaths(tb, tx, ty, lst);
					drawArrowsOverPath(canvas, lst, coloredArrowUp);
				}
			} finally {
				canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}


	private void drawArrowsOverPath(Canvas canvas, TIntArrayList lst, Bitmap arrow) {
		double pxStep = arrow.getHeight() * 4f;
		Matrix matrix = new Matrix();
		double dist = 0;
		for (int i = 0; i < lst.size(); i += 4) {
			int px = lst.get(i);
			int py = lst.get(i + 1);
			int x = lst.get(i + 2);
			int y = lst.get(i + 3);
			double angleRad = Math.atan2(y - py, x - px);
			double angle = (angleRad * 180 / Math.PI) + 90f;
			double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			if(distSegment == 0) {
				continue;
			}
			int len = (int) (distSegment / pxStep);
			if (len > 0) {
				double pdx = ((x - px) / len);
				double pdy = ((y - py) / len);
				for (int k = 1; k <= len; k++) {
					matrix.reset();
					matrix.postTranslate(0, -arrow.getHeight() / 2);
					matrix.postRotate((float) angle, arrow.getWidth() / 2, 0);
					matrix.postTranslate((float)(px + k * pdx- arrow.getWidth() / 2) , (float)(py + pdy * k));
					canvas.drawBitmap(arrow, matrix, paintIcon);
					dist = 0;
				}
			} else {
				if(dist > pxStep) {
					matrix.reset();
					matrix.postTranslate(0, -arrow.getHeight() / 2);
					matrix.postRotate((float) angle, arrow.getWidth() / 2, 0);
					matrix.postTranslate(px + (x - px) / 2 - arrow.getWidth() / 2, py + (y - py) / 2);
					canvas.drawBitmap(arrow, matrix, paintIcon);
					dist = 0;
				} else {
					dist += distSegment;
				}
			}
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		points.clear();
		actionPoints.clear();
		boolean previousVisible = false;
		Location lastProjection = helper.getLastProjection();
		if (lastProjection != null) {
			if (leftLongitude <= lastProjection.getLongitude() && lastProjection.getLongitude() <= rightLongitude
					&& bottomLatitude <= lastProjection.getLatitude() && lastProjection.getLatitude() <= topLatitude) {
				points.add(lastProjection);
				previousVisible = true;
			}
		}
		List<Location> routeNodes = helper.getRoute().getRouteLocations();
		int cd = helper.getRoute().getCurrentRoute();
		List<RouteDirectionInfo> rd = helper.getRouteDirections();
		Iterator<RouteDirectionInfo> it = rd.iterator();
		for (int i = 0; i < routeNodes.size(); i++) {
			Location ls = routeNodes.get(i);
			if (leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude && bottomLatitude <= ls.getLatitude()
					&& ls.getLatitude() <= topLatitude) {
				points.add(ls);
				
				if (!previousVisible) {
					if (i > 0) {
						points.add(0, routeNodes.get(i - 1));
					} else if (lastProjection != null) {
						points.add(0, lastProjection);
					}
				}
				previousVisible = true;
			} else if (previousVisible) {
				points.add(ls);
				drawSegment(tb, canvas);
				previousVisible = false;
				points.clear();
			}
		}
		drawSegment(tb, canvas);
		if (tb.getZoom() >= 14) {
			calculateActionPoints(topLatitude, leftLongitude, bottomLatitude, rightLongitude, lastProjection,
					routeNodes, cd, it, tb.getZoom());
			drawAction(tb, canvas);
		}
	}


	private void calculateActionPoints(double topLatitude, double leftLongitude, double bottomLatitude,
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
					addPreviousToActionPoints(lastProjection, routeNodes, DISTANCE_ACTION,
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
	}


	private void addPreviousToActionPoints(Location lastProjection, List<Location> routeNodes, double DISTANCE_ACTION,
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

	
	// to show further direction
	public Path getPath() {
		return path;
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
