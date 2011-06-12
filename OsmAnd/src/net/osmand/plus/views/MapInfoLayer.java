package net.osmand.plus.views;

import net.osmand.Algoritms;
import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.activities.RoutingHelper.TurnType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.WindowManager;

public class MapInfoLayer implements OsmandMapLayer {


	private OsmandMapTileView view;
	private boolean showMiniMap = false;
	private final MapActivity map;
	private final RouteLayer routeLayer;
	
	private boolean showArrivalTime = true; 
	
	
	private Path pathForTurn;
	
	private Paint paintBlack;
	private Paint paintMiniRoute;
	private Paint fillBlack;
	private Paint fillRed;
	private Paint paintAlphaGray;
	private Paint paintRouteDirection;
	
	private RectF boundsForCompass;
	private RectF boundsForZoom;
	private RectF boundsForDist;
	private RectF boundsForMiniRoute;
	private RectF boundsForLeftTime;
	private RectF boundsForSpeed;
	
	
	
	private String cachedLeftTimeString = null;
	private long cachedLeftTime = 0;
	private float[] calculations = new float[1];
	private String cachedDistString = null;
	private int cachedMeters = 0;
	private String cachedSpeedString = null;
	private float cachedSpeed = 0;
	private int cachedZoom = 0;
	private String cachedZoomString = ""; //$NON-NLS-1$
	private int centerMiniRouteY;
	private int centerMiniRouteX;
	private float scaleMiniRoute;
	private Matrix pathTransform;
	private TurnType cachedTurnType = null;
	private String cachedExitOut = null;
	private DisplayMetrics dm;
	
	private float scaleCoefficient;
	private float roundCorner;
	private Bitmap compass;
	private Paint paintImg;
	
	
	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		paintBlack = new Paint();
		WindowManager mgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		dm = new DisplayMetrics();
		mgr.getDefaultDisplay().getMetrics(dm);
		scaleCoefficient = dm.density;
		if(Math.min(dm.widthPixels/(dm.density*160), dm.heightPixels/(dm.density*160)) > 2.5f){
			// large screen
			scaleCoefficient *= 1.5f;
		}
		
		roundCorner = 3 * scaleCoefficient;
		
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setTextSize(23 * scaleCoefficient);
		paintBlack.setAntiAlias(true);
		
		paintImg = new Paint();
		paintImg.setDither(true);
		paintImg.setAntiAlias(true);
		
		paintAlphaGray = new Paint();
		paintAlphaGray.setStyle(Style.FILL_AND_STROKE);
		paintAlphaGray.setColor(Color.LTGRAY);
		paintAlphaGray.setAlpha(180); // do not make very transparent (to hide route)
		
		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Style.FILL_AND_STROKE);
		paintRouteDirection.setColor(Color.rgb(100, 0, 255));
		paintRouteDirection.setAntiAlias(true);
		
		fillBlack = new Paint();
		fillBlack.setStyle(Style.FILL_AND_STROKE);
		fillBlack.setColor(Color.BLACK);
		fillBlack.setAntiAlias(true);
		
		paintMiniRoute = new Paint();
		paintMiniRoute.setStyle(Style.STROKE);
		paintMiniRoute.setStrokeWidth(35 * scaleCoefficient);
		paintMiniRoute.setColor(Color.BLUE);
		paintMiniRoute.setStrokeJoin(Join.ROUND);
		paintMiniRoute.setStrokeCap(Cap.ROUND);
		paintMiniRoute.setAntiAlias(true);
		
		fillRed = new Paint();
		fillRed.setStyle(Style.FILL_AND_STROKE);
		fillRed.setColor(Color.RED);
		fillRed.setAntiAlias(true);
		
		boundsForCompass = new RectF(0, 0, 35, 32);
		boundsForDist = new RectF(35, 0, 110, 32);
		boundsForZoom = new RectF(0, 32, 35, 64);
		boundsForSpeed = new RectF(35, 32, 110, 64);
		boundsForMiniRoute = new RectF(0, 64, 96, 196);
		
		boundsForLeftTime = new RectF(0, 0, 75, 32);

		// Scale to have proper view
		scaleRect(boundsForCompass);
		scaleRect(boundsForDist);
		scaleRect(boundsForZoom);
		scaleRect(boundsForSpeed);
		scaleRect(boundsForMiniRoute);
		scaleRect(boundsForLeftTime);
		
		centerMiniRouteX = (int) (boundsForMiniRoute.width()/2);
		centerMiniRouteY= (int) (boundsForMiniRoute.top + 3 * boundsForMiniRoute.height() /4);
		scaleMiniRoute = 0.15f;
		
		pathForTurn = new Path();
		pathTransform = new Matrix();
		pathTransform.postScale(scaleCoefficient, scaleCoefficient);
		pathTransform.postTranslate(boundsForMiniRoute.left, boundsForMiniRoute.top);
		
		compass = BitmapFactory.decodeResource(view.getResources(), R.drawable.compass);
		
		showArrivalTime = view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.get();
	}
	
	private void scaleRect(RectF r){
		r.bottom *= scaleCoefficient;
		r.left *= scaleCoefficient;
		r.right *= scaleCoefficient;
		r.top *= scaleCoefficient;
	}

	public boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas canvas, RectF latlonBounds, RectF tilesRect, boolean nightMode) {
		// prepare data (left distance, speed)
		if(map.getPointToNavigate() != null){
			int d = 0;
			if(map.getRoutingHelper().isRouterEnabled()){
				d = map.getRoutingHelper().getLeftDistance();
			} 
			if (d == 0) {
				Location.distanceBetween(view.getLatitude(), view.getLongitude(), map.getPointToNavigate().getLatitude(), map
						.getPointToNavigate().getLongitude(), calculations);
				d = (int) calculations[0];
			}
			if(distChanged(cachedMeters, d)){
				cachedMeters = d;
				if(cachedMeters <= 20){
					cachedMeters = 0;
					cachedDistString = null;
				} else {
					cachedDistString = OsmAndFormatter.getFormattedDistance(cachedMeters, map);
					float right = paintBlack.measureText(cachedDistString) + 25 * scaleCoefficient + boundsForDist.left;
					if(cachedSpeedString != null){
						boundsForSpeed.right = boundsForDist.right = Math.max(right, boundsForDist.right);
					} else {
						boundsForDist.right = right;
					}
				}
			}
		} else {
			cachedMeters = 0;
			cachedDistString = null;
		}
		if(view.getZoom() != cachedZoom){
			cachedZoom = view.getZoom();
			cachedZoomString = view.getZoom()+""; //$NON-NLS-1$
		}
		// draw zoom
		canvas.drawRoundRect(boundsForZoom, roundCorner, roundCorner, paintAlphaGray);
		canvas.drawRoundRect(boundsForZoom, roundCorner, roundCorner, paintBlack);
		canvas.drawText(cachedZoomString, boundsForZoom.left + 5 * scaleCoefficient, boundsForZoom.bottom - 8 * scaleCoefficient,
				paintBlack);
		
		// draw speed 	
		if(map.getLastKnownLocation() != null && map.getLastKnownLocation().hasSpeed()){
			if(Math.abs(map.getLastKnownLocation().getSpeed() - cachedSpeed) > .3f){
				cachedSpeed = map.getLastKnownLocation().getSpeed();
				cachedSpeedString = OsmAndFormatter.getFormattedSpeed(cachedSpeed, map); 
				float right = paintBlack.measureText(cachedSpeedString) + 8 * scaleCoefficient + boundsForSpeed.left;
				boundsForSpeed.right = boundsForDist.right = Math.max(right, boundsForDist.right);
			}
			if(cachedSpeed > 0){
				canvas.drawRoundRect(boundsForSpeed, roundCorner, roundCorner, paintAlphaGray);
				canvas.drawRoundRect(boundsForSpeed, roundCorner, roundCorner, paintBlack);
				canvas.drawText(cachedSpeedString, boundsForSpeed.left + 8 * scaleCoefficient, boundsForSpeed.bottom - 9f * scaleCoefficient, paintBlack);
			}
		}
		// draw distance to point
		if(cachedDistString != null){
			canvas.drawRoundRect(boundsForDist, roundCorner, roundCorner, paintAlphaGray);
			canvas.drawRoundRect(boundsForDist, roundCorner, roundCorner, paintBlack);
			canvas.drawCircle(boundsForDist.left + 8 * scaleCoefficient, boundsForDist.bottom - 15 * scaleCoefficient,
					4 * scaleCoefficient, fillRed);
			canvas.drawText(cachedDistString, boundsForDist.left + 15 * scaleCoefficient, boundsForDist.bottom - 9f * scaleCoefficient,
					paintBlack);
		}
		
		// draw ruler
		drawRuler(canvas);
	
		// draw route information
		drawRouteInfo(canvas);
		
		// draw compass the last (!) because it use rotating
		canvas.drawRoundRect(boundsForCompass, roundCorner, roundCorner, paintAlphaGray);
		canvas.drawRoundRect(boundsForCompass, roundCorner, roundCorner, paintBlack);
		canvas.rotate(view.getRotate(), 17 * scaleCoefficient, 15 * scaleCoefficient);
		canvas.drawBitmap(compass, 0, 0, paintImg);
	}
	
	
	// cache values for ruler
	int rulerDistPix = 0;
	String rulerDistName = null;
	int rulerBaseLine = 50;
	float rulerTextLen = 0;
	// cache properties
	int rulerCZoom = 0;
	double rulerCTileX = 0;
	double rulerCTileY = 0;

	
	private void drawRuler(Canvas canvas) {
		// occupy length over screen
		double screenPercent = 0.2;
		
				
		// update cache
		if (view.isZooming()) {
			rulerDistName = null;
		} else if(view.getZoom() != rulerCZoom || 
				Math.abs(view.getXTile() - rulerCTileX) +  Math.abs(view.getYTile() - rulerCTileY) > 1){
			rulerCZoom = view.getZoom();
			rulerCTileX = view.getXTile();
			rulerCTileY = view.getYTile();
			double latitude = view.getLatitude();
			double tileNumberLeft = rulerCTileX - ((double) view.getWidth()) / (2d * view.getTileSize());
			double tileNumberRight = rulerCTileX + ((double) view.getWidth()) / (2d * view.getTileSize());
			double dist = MapUtils.getDistance(latitude, MapUtils.getLongitudeFromTile(view.getZoom(), tileNumberLeft), latitude,
					MapUtils.getLongitudeFromTile(view.getZoom(), tileNumberRight));

			dist *= screenPercent;
			int baseDist = 5;
			byte pointer = 0;
			while (dist > baseDist) {
				if (pointer++ % 3 == 2) {
					baseDist = baseDist * 5 / 2;
				} else {
					baseDist *= 2;
				}
			}

			rulerDistPix = (int) (view.getWidth() * screenPercent / dist * baseDist);
			rulerDistName = OsmAndFormatter.getFormattedDistance(baseDist, map);
			rulerBaseLine = (int) (view.getHeight() - 50 * dm.density);
			rulerTextLen = paintBlack.measureText(rulerDistName);
		} 
		if (rulerDistName != null) {
			int w2 = (int) (view.getWidth() - 5 * dm.density);
			canvas.drawLine(w2 - rulerDistPix, rulerBaseLine, w2, rulerBaseLine, paintBlack);
			canvas.drawLine(w2 - rulerDistPix, rulerBaseLine, w2 - rulerDistPix, rulerBaseLine - 10 * dm.density, paintBlack);
			canvas.drawLine(w2, rulerBaseLine, w2, rulerBaseLine - 10 * dm.density, paintBlack);
			canvas.drawText(rulerDistName, w2 - (rulerDistPix + rulerTextLen)/2 + 1, rulerBaseLine - 5 * dm.density, paintBlack);
		}
	}

	private void drawRouteInfo(Canvas canvas) {
		if(routeLayer != null && routeLayer.getHelper().isRouterEnabled()){
			if (routeLayer.getHelper().isFollowingMode()) {
				int d = routeLayer.getHelper().getDistanceToNextRouteDirection();
				if (showMiniMap || d == 0) {
					if (!routeLayer.getPath().isEmpty()) {
						canvas.save();
						canvas.clipRect(boundsForMiniRoute);
						canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintAlphaGray);
						canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintBlack);
						canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
						canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
						canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
						canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3 / scaleMiniRoute, fillBlack);
						canvas.drawPath(routeLayer.getPath(), paintMiniRoute);
						canvas.restore();
					}
				} else {
					canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintAlphaGray);
					canvas.drawRoundRect(boundsForMiniRoute, roundCorner, roundCorner, paintBlack);
					RouteDirectionInfo next = routeLayer.getHelper().getNextRouteDirectionInfo();
					if (next != null) {
						if (!Algoritms.objectEquals(cachedTurnType, next.turnType)) {
							cachedTurnType = next.turnType;
							calcTurnPath(pathForTurn, cachedTurnType, pathTransform);
							if (cachedTurnType.getExitOut() > 0) {
								cachedExitOut = cachedTurnType.getExitOut() + ""; //$NON-NLS-1$
							} else {
								cachedExitOut = null;
							}
						}
						canvas.drawPath(pathForTurn, paintRouteDirection);
						canvas.drawPath(pathForTurn, paintBlack);
						if (cachedExitOut != null) {
							canvas.drawText(cachedExitOut, boundsForMiniRoute.centerX() - 6 * scaleCoefficient, 
									boundsForMiniRoute.centerY() - 9 * scaleCoefficient, paintBlack);
						}
						canvas.drawText(OsmAndFormatter.getFormattedDistance(d, map), boundsForMiniRoute.left + 10 * scaleCoefficient, 
								boundsForMiniRoute.bottom - 9 * scaleCoefficient, paintBlack);
					}
				}
			}
			
			boolean followingMode = routeLayer.getHelper().isFollowingMode();
			int time = routeLayer.getHelper().getLeftTime();
			if(time == 0){
				cachedLeftTime = 0;
				cachedLeftTimeString = null;
			} else {
				if(followingMode && showArrivalTime){
					long toFindTime = time * 1000 + System.currentTimeMillis();
					if(Math.abs(toFindTime - cachedLeftTime) > 30000){
						cachedLeftTime = toFindTime;
						if(DateFormat.is24HourFormat(map)){
							cachedLeftTimeString = DateFormat.format("kk:mm",toFindTime).toString(); //$NON-NLS-1$
						} else {
							cachedLeftTimeString = DateFormat.format("k:mm aa",toFindTime).toString(); //$NON-NLS-1$
						}
						boundsForLeftTime.left = - paintBlack.measureText(cachedLeftTimeString) - 10 * scaleCoefficient + boundsForLeftTime.right;
					}
				} else {
					if(Math.abs(time - cachedLeftTime) > 30){
						cachedLeftTime = time;
						int hours = time / (60 * 60);
						int minutes = (time / 60) % 60;
						cachedLeftTimeString = String.format("%d:%02d", hours, minutes); //$NON-NLS-1$
						boundsForLeftTime.left = - paintBlack.measureText(cachedLeftTimeString) - 10 * scaleCoefficient + boundsForLeftTime.right;
					}
				}
			}
			if(cachedLeftTimeString != null) {
				int w = (int) (boundsForLeftTime.right - boundsForLeftTime.left); 
				boundsForLeftTime.right = view.getWidth();
				boundsForLeftTime.left = view.getWidth() - w;
				canvas.drawRoundRect(boundsForLeftTime, roundCorner, roundCorner, paintAlphaGray);
				canvas.drawRoundRect(boundsForLeftTime, roundCorner, roundCorner, paintBlack);
				canvas.drawText(cachedLeftTimeString, boundsForLeftTime.left + 5 * scaleCoefficient, boundsForLeftTime.bottom - 9 * scaleCoefficient, paintBlack);
				
			}
		}
	}
	

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		if (routeLayer != null && routeLayer.getHelper().isRouterEnabled()) {
			if (boundsForMiniRoute.contains(point.x, point.y) && routeLayer.getHelper().isFollowingMode()) {
				showMiniMap = !showMiniMap;
				view.refreshMap();
				return true;
			}
			if (boundsForLeftTime.contains(point.x, point.y) && routeLayer.getHelper().isFollowingMode()) {
				showArrivalTime = !showArrivalTime;
				view.getSettings().SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME.set(showArrivalTime);
				view.refreshMap();
				return true;
			}
		}
		if(cachedDistString != null && boundsForDist.contains(point.x, point.y)){
			AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
			LatLon pointToNavigate = view.getSettings().getPointToNavigate();
			if(pointToNavigate != null){
				int fZoom = view.getZoom() < 15 ? 15 : view.getZoom(); 
				thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), 
						fZoom, true);
			}
		}
		if(boundsForCompass.contains(point.x, point.y)){
			map.switchRotateMapMode();
			return true;
		}
		return false;
	}
	

	// draw path 96x96
	public static void calcTurnPath(Path pathForTurn, TurnType turnType, Matrix transform) {
		if(turnType == null){
			return;
		}
		pathForTurn.reset();

		int c = 48;
		int w = 16;
		pathForTurn.moveTo(c, 94);
		float sarrowL = 30; // side of arrow
		float harrowL = (float) Math.sqrt(2) * sarrowL; // hypotenuse of arrow
		float spartArrowL = (float) ((sarrowL - w / Math.sqrt(2)) / 2);
		float hpartArrowL = (float) (harrowL - w) / 2;

		if (TurnType.C.equals(turnType.getValue())) {
			int h = 65;

			pathForTurn.rMoveTo(w / 2, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rLineTo(hpartArrowL, 0);
			pathForTurn.rLineTo(-harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-harrowL / 2, harrowL / 2);
			pathForTurn.rLineTo(hpartArrowL, 0);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TR.equals(turnType.getValue())|| TurnType.TL.equals(turnType.getValue())) {
			int b = TurnType.TR.equals(turnType.getValue())? 1 : -1;
			int h = 36;
			float quadShiftX = 22;
			float quadShiftY = 22;

			pathForTurn.rMoveTo(-b * 8, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -quadShiftY, b * quadShiftX, -quadShiftY);
			pathForTurn.rLineTo(0, hpartArrowL);
			pathForTurn.rLineTo(b * harrowL / 2, -harrowL / 2); // center
			pathForTurn.rLineTo(-b * harrowL / 2, -harrowL / 2);
			pathForTurn.rLineTo(0, hpartArrowL);
			pathForTurn.rQuadTo(-b * (quadShiftX + w), 0, -b * (quadShiftX + w), quadShiftY + w);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TSLR.equals(turnType.getValue()) || TurnType.TSLL.equals(turnType.getValue())) {
			int b = TurnType.TSLR.equals(turnType.getValue()) ? 1 : -1;
			int h = 40;
			int quadShiftY = 22;
			float quadShiftX = (float) (quadShiftY / (1 + Math.sqrt(2)));
			float nQuadShiftX = (sarrowL - 2 * spartArrowL) - quadShiftX - w;
			float nQuadShifty = quadShiftY + (sarrowL - 2 * spartArrowL);

			pathForTurn.rMoveTo(-b * 4, 0);
			pathForTurn.rLineTo(0, -h /* + partArrowL */);
			pathForTurn.rQuadTo(0, -quadShiftY + quadShiftX /*- partArrowL*/, b * quadShiftX, -quadShiftY /*- partArrowL*/);
			pathForTurn.rLineTo(b * spartArrowL, spartArrowL);
			pathForTurn.rLineTo(0, -sarrowL); // center
			pathForTurn.rLineTo(-b * sarrowL, 0);
			pathForTurn.rLineTo(b * spartArrowL, spartArrowL);
			pathForTurn.rQuadTo(b * nQuadShiftX, -nQuadShiftX, b * nQuadShiftX, nQuadShifty);
			pathForTurn.rLineTo(0, h);
		} else if (TurnType.TSHR.equals(turnType.getValue()) || TurnType.TSHL.equals(turnType.getValue())) {
			int b = TurnType.TSHR.equals(turnType.getValue()) ? 1 : -1;
			int h = 45;
			float quadShiftX = 22;
			float quadShiftY = -(float) (quadShiftX / (1 + Math.sqrt(2)));
			float nQuadShiftX = -(sarrowL - 2 * spartArrowL) - quadShiftX - w;
			float nQuadShiftY = -quadShiftY + (sarrowL - 2 * spartArrowL);

			pathForTurn.rMoveTo(-b * 8, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -(quadShiftX - quadShiftY), b * quadShiftX, quadShiftY);
			pathForTurn.rLineTo(-b * spartArrowL, spartArrowL);
			pathForTurn.rLineTo(b * sarrowL, 0); // center
			pathForTurn.rLineTo(0, -sarrowL);
			pathForTurn.rLineTo(-b * spartArrowL, spartArrowL);
			pathForTurn.rCubicTo(b * nQuadShiftX / 2, nQuadShiftX / 2, b * nQuadShiftX, nQuadShiftX / 2, b * nQuadShiftX, nQuadShiftY);
			pathForTurn.rLineTo(0, h);
		} else if(TurnType.TU.equals(turnType.getValue())) {
			int h = 54;
			float quadShiftX = 13;
			float quadShiftY = 13;

			pathForTurn.rMoveTo(28, 0);
			pathForTurn.rLineTo(0, -h);
			pathForTurn.rQuadTo(0, -(quadShiftY+w), -(quadShiftX+w), -(quadShiftY+w));
			pathForTurn.rQuadTo(-(quadShiftX+w), 0, -(quadShiftX+w), (quadShiftY+w));
			pathForTurn.rLineTo(-hpartArrowL, 0);
			pathForTurn.rLineTo(harrowL/2, harrowL/2); // center
			pathForTurn.rLineTo(harrowL/2, -harrowL/2);
			pathForTurn.rLineTo(-hpartArrowL, 0);
			pathForTurn.rQuadTo(0, -quadShiftX, quadShiftX, -quadShiftY);
			pathForTurn.rQuadTo(quadShiftX, 0, quadShiftX, quadShiftY);
			pathForTurn.rLineTo(0, h);
		} else if (turnType != null && turnType.isRoundAbout()) {
			float t = turnType.getTurnAngle();
			if (t >= 170 && t < 220) {
				t = 220;
			} else if (t > 160 && t < 170) {
				t = 160;
			}
			float sweepAngle = (t - 360) - 180;
			if (sweepAngle < -360) {
				sweepAngle += 360;
			}
			float r1 = 32f;
			float r2 = 24f;
			float angleToRot = 0.3f;
			
			pathForTurn.moveTo(48, 48 + r1 + 8);
			pathForTurn.lineTo(48, 48 + r1);
			RectF r = new RectF(48 - r1, 48 - r1, 48 + r1, 48 + r1);
			pathForTurn.arcTo(r, 90, sweepAngle);
			float angleRad = (float) ((180 + sweepAngle)*Math.PI/180f);
			
			pathForTurn.lineTo(48 + (r1 + 4) * FloatMath.sin(angleRad), 48 - (r1 + 4) * FloatMath.cos(angleRad));
			pathForTurn.lineTo(48 + (r1 + 6) * FloatMath.sin(angleRad + angleToRot/2), 48 - (r1 + 6) * FloatMath.cos(angleRad + angleToRot/2));
			pathForTurn.lineTo(48 + (r1 + 12) * FloatMath.sin(angleRad - angleToRot/2), 48 - (r1 + 12) * FloatMath.cos(angleRad - angleToRot/2));
			pathForTurn.lineTo(48 + (r1 + 6) * FloatMath.sin(angleRad - 3*angleToRot/2), 48 - (r1 + 6) * FloatMath.cos(angleRad - 3*angleToRot/2));
			pathForTurn.lineTo(48 + (r1 + 4) * FloatMath.sin(angleRad - angleToRot), 48 - (r1 + 4) * FloatMath.cos(angleRad - angleToRot));
			pathForTurn.lineTo(48 + r2 * FloatMath.sin(angleRad - angleToRot), 48 - r2 * FloatMath.cos(angleRad - angleToRot));
			
			r.set(48 - r2, 48 - r2, 48 + r2, 48 + r2);
			pathForTurn.arcTo(r, 360 + sweepAngle + 90, -sweepAngle);
			pathForTurn.lineTo(40, 48 + r2);
			pathForTurn.lineTo(40, 48 + r1 + 8);
			pathForTurn.close();
		}
		pathForTurn.close();
		if(transform != null){
			pathForTurn.transform(transform);
		}
	}



}
