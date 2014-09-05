package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
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
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;

public class RouteLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private List<Location> points = new ArrayList<Location>();
	private Paint paint;
	private Paint paint2;
	private boolean isPaint2;
	private Paint shadowPaint;
	private boolean isShadowPaint;
	private Paint paint_1;
	private boolean isPaint_1;
	private int cachedHash;

	private Path path;

	// cache
	private Bitmap coloredArrowUp;

	private Paint paintIcon;

	private OsmandRenderer osmandRenderer;

	public RouteLayer(RoutingHelper helper){
		this.helper = helper;
	}
	

	private void initUI() {
		paint = new Paint();
		
		paint.setStyle(Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		path = new Path();
		
		paintIcon = new Paint();
		paintIcon.setFilterBitmap(true);
		paintIcon.setAntiAlias(true);
		paintIcon.setColor(Color.BLACK);
		paintIcon.setStrokeWidth(3);
		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		osmandRenderer = view.getApplication().getResourceManager().getRenderer().getRenderer();
		initUI();
	}

	
	private void updatePaints(DrawSettings nightMode, RotatedTileBox tileBox){
		RenderingRulesStorage rrs = view.getApplication().getRendererRegistry().getCurrentSelectedRenderer();
		final boolean isNight = nightMode != null && nightMode.isNightMode();
		int hsh = calculateHash(rrs, isNight, tileBox.getZoomScale());
		if (hsh != cachedHash) {
			cachedHash = hsh;
			// cachedColor = view.getResources().getColor(R.color.nav_track);
			if (rrs != null) {
				RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
				req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
				if (req.searchRenderingAttribute("route")) {
					RenderingContext rc = new OsmandRenderer.RenderingContext(view.getContext());
					rc.setDensityValue((float) Math.pow(2, tileBox.getZoomScale()));
//					cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
					osmandRenderer.updatePaint(req, paint, 0, false, rc);
					isPaint2 = osmandRenderer.updatePaint(req, paint2, 1, false, rc);
					isPaint_1 = osmandRenderer.updatePaint(req, paint_1, -1, false, rc);
					isShadowPaint = req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS);
					if(isShadowPaint) {
						ColorFilter cf = new PorterDuffColorFilter(req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR), Mode.SRC_IN);
						shadowPaint.setColorFilter(cf);
						shadowPaint.setStrokeWidth(paint.getStrokeWidth() + 2 * rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS));
					}
				} else {
					System.err.println("Rendering attribute route is not found !");
					paint.setStrokeWidth(7 * view.getDensity());
				}
			}
		}
	}
	
	
	private int calculateHash(Object... o) {
		return Arrays.hashCode(o);
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		path.reset();
		if (helper.getFinalLocation() != null && helper.getRoute().isCalculated()) {
			updatePaints(settings, tileBox);
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


	private void drawSegment(RotatedTileBox tb, Canvas canvas) {
		if (points.size() > 0) {
			paint.setStrokeWidth(12 * tb.getDensity());
			
			int px = tb.getPixXFromLonNoRot(points.get(0).getLongitude());
			int py = tb.getPixYFromLatNoRot(points.get(0).getLatitude());
			path.moveTo(px, py);
			for (int i = 1; i < points.size(); i++) {
				Location o = points.get(i);
				int x = tb.getPixXFromLonNoRot(o.getLongitude());
				int y = tb.getPixYFromLatNoRot(o.getLatitude());
				path.lineTo(x, y);
			}
			if(isPaint_1) {
				canvas.drawPath(path, paint_1);
			}
			if(isShadowPaint) {
				canvas.drawPath(path, shadowPaint);
			}
			canvas.drawPath(path, paint);
			if(isPaint2) {
				canvas.drawPath(path, paint2);
			}
			if(tb.getZoomAnimation() == 0) {
				drawArrowsOverPath(canvas, path, coloredArrowUp);
			}
		}
	}


	private void drawArrowsOverPath(Canvas canvas, Path path, Bitmap arrow) {
		PathMeasure pm = new PathMeasure();
		pm.setPath(path, false);
		float pxStep = arrow.getHeight() * 4f;
		float dist = pxStep;
		double length = pm.getLength();
		Matrix matrix = new Matrix();
		float[] pos = new float[2];
		float[] tan = new float[2];
		while(dist < length) {
			if(pm.getPosTan(dist, pos, tan)) {
				matrix.reset();
				matrix.postTranslate(0, - arrow.getHeight() / 2);
				matrix.postRotate((float) (Math.atan2(tan[1], tan[0]) * 180 / Math.PI) + 90f, 
						arrow.getWidth() / 2, 0);
				matrix.postTranslate(pos[0] - arrow.getWidth() / 2, pos[1]);
				canvas.drawBitmap(arrow, matrix, paintIcon);
			}
			dist += pxStep;
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		points.clear();
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
