package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.FloatMath;

import com.osmand.Algoritms;
import com.osmand.Messages;
import com.osmand.activities.MapActivity;
import com.osmand.activities.RoutingHelper.RouteDirectionInfo;
import com.osmand.activities.RoutingHelper.TurnType;
import com.osmand.osm.MapUtils;

public class MapInfoLayer implements OsmandMapLayer {


	private OsmandMapTileView view;
	private boolean showMiniMap = false;
	private final MapActivity map;
	private final RouteLayer routeLayer;
	
	
	
	private Path pathForCompass;
	private Path pathForCompass2;
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
	
	
	public MapInfoLayer(MapActivity map, RouteLayer layer){
		this.map = map;
		this.routeLayer = layer;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setTextSize(23);
		paintBlack.setAntiAlias(true);
		
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
		paintMiniRoute.setStrokeWidth(35);
		paintMiniRoute.setColor(Color.BLUE);
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
		
		
		centerMiniRouteX = 48;
		centerMiniRouteY= 160;
		scaleMiniRoute = 0.15f;
		
		pathForCompass = new Path();
		pathForCompass.moveTo(9, 15.5f);
		pathForCompass.lineTo(22f, 15.5f);
		pathForCompass.lineTo(15.5f, 30f);
		pathForCompass.lineTo(9, 15.5f);
		
		pathForCompass2 = new Path();
		pathForCompass2.moveTo(9, 15.5f);
		pathForCompass2.lineTo(22f, 15.5f);
		pathForCompass2.lineTo(15.5f, 2f);
		pathForCompass2.lineTo(9, 15);
		
		pathForTurn = new Path();
		pathTransform = new Matrix();
		pathTransform.setTranslate(boundsForMiniRoute.left, boundsForMiniRoute.top);
	}

	public boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
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
					cachedDistString = MapUtils.getFormattedDistance(cachedMeters);
					float right = paintBlack.measureText(cachedDistString) + 25 + boundsForDist.left;
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
		canvas.drawRoundRect(boundsForZoom, 3, 3, paintAlphaGray);
		canvas.drawRoundRect(boundsForZoom, 3, 3, paintBlack);
		canvas.drawText(cachedZoomString, boundsForZoom.left + 5, boundsForZoom.bottom - 7, paintBlack);
		
		// draw speed 	
		if(map.getLastKnownLocation() != null && map.getLastKnownLocation().hasSpeed()){
			if(Math.abs(map.getLastKnownLocation().getSpeed() - cachedSpeed) > .3f){
				cachedSpeed = map.getLastKnownLocation().getSpeed();
				cachedSpeedString = ((int) (cachedSpeed * 3.6f)) + Messages.getMessage(Messages.KEY_KM_H);
				float right = paintBlack.measureText(cachedSpeedString) + 8 + boundsForSpeed.left;
				boundsForSpeed.right = boundsForDist.right = Math.max(right, boundsForDist.right);
			}
			if(cachedSpeed > 0){
				canvas.drawRoundRect(boundsForSpeed, 3, 3, paintAlphaGray);
				canvas.drawRoundRect(boundsForSpeed, 3, 3, paintBlack);
				canvas.drawText(cachedSpeedString, boundsForSpeed.left + 8, boundsForSpeed.bottom - 9, paintBlack);
			}
		}
		// draw distance to point
		if(cachedDistString != null){
			canvas.drawRoundRect(boundsForDist, 3, 3, paintAlphaGray);
			canvas.drawRoundRect(boundsForDist, 3, 3, paintBlack);
			canvas.drawCircle(boundsForDist.left + 8, boundsForDist.bottom - 15, 4, fillRed);
			canvas.drawText(cachedDistString, boundsForDist.left + 15, boundsForDist.bottom - 9, paintBlack);
		}
		
	
		// draw route information
		drawRouteInfo(canvas);
		
		// draw compass the last because it use rotating
		canvas.drawRoundRect(boundsForCompass, 3, 3, paintAlphaGray);
		canvas.drawRoundRect(boundsForCompass, 3, 3, paintBlack);
		canvas.rotate(view.getRotate(), 15, 15);
		canvas.drawPath(pathForCompass2, fillRed);
		canvas.drawPath(pathForCompass, fillBlack);
	}
	
	
	private void drawRouteInfo(Canvas canvas) {
		if(routeLayer != null && routeLayer.getHelper().isRouterEnabled() && routeLayer.getHelper().isFollowingMode() ){
			int d = routeLayer.getHelper().getDistanceToNextRouteDirection();
			if (showMiniMap || d == 0) {
				if (!routeLayer.getPath().isEmpty()) {
					canvas.save();
					canvas.clipRect(boundsForMiniRoute);
					canvas.drawRoundRect(boundsForMiniRoute, 3, 3, paintAlphaGray);
					canvas.drawRoundRect(boundsForMiniRoute, 3, 3, paintBlack);
					canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
					canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
					canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
					canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3 / scaleMiniRoute, fillBlack);
					canvas.drawPath(routeLayer.getPath(), paintMiniRoute);
					canvas.restore();
				}
			} else {
				canvas.drawRoundRect(boundsForMiniRoute, 3, 3, paintAlphaGray);
				canvas.drawRoundRect(boundsForMiniRoute, 3, 3, paintBlack);
				RouteDirectionInfo next = routeLayer.getHelper().getNextRouteDirectionInfo();
				if (next != null) {
					if (!Algoritms.objectEquals(cachedTurnType, next.turnType)) {
						cachedTurnType = next.turnType;
						calcTurnPath(pathForTurn, cachedTurnType, pathTransform);
						if(cachedTurnType.getExitOut() > 0){
							cachedExitOut = cachedTurnType.getExitOut()+""; //$NON-NLS-1$
						} else {
							cachedExitOut = null;
						}
					}
					canvas.drawPath(pathForTurn, paintRouteDirection);
					canvas.drawPath(pathForTurn, paintBlack);
					if(cachedExitOut != null){
						canvas.drawText(cachedExitOut, boundsForMiniRoute.centerX() - 6, boundsForMiniRoute.centerY() - 9, paintBlack);
					}
					canvas.drawText(MapUtils.getFormattedDistance(d), boundsForMiniRoute.left + 10, boundsForMiniRoute.bottom - 9,
							paintBlack);
				}
			}
			
			boolean followingMode = routeLayer.getHelper().isFollowingMode();
			int time = routeLayer.getHelper().getLeftTime() * 1000;
			if(time == 0){
				cachedLeftTime = 0;
				cachedLeftTimeString = null;
			} else {
				long toFindTime = time;
				if(followingMode){
					toFindTime += System.currentTimeMillis();
				}
				if(Math.abs(toFindTime - cachedLeftTime) > 30000){
					cachedLeftTime = toFindTime;
					cachedLeftTimeString = DateFormat.format("kk:mm", cachedLeftTime).toString(); //$NON-NLS-1$
				}
			}
			if(cachedLeftTimeString != null) {
				int w = (int) (boundsForLeftTime.right - boundsForLeftTime.left); 
				boundsForLeftTime.right = view.getWidth();
				boundsForLeftTime.left = view.getWidth() - w;
				canvas.drawRoundRect(boundsForLeftTime, 3, 3, paintAlphaGray);
				canvas.drawRoundRect(boundsForLeftTime, 3, 3, paintBlack);
				canvas.drawText(cachedLeftTimeString, boundsForLeftTime.left + 5, boundsForLeftTime.bottom - 9, paintBlack);
				
			}
		}
	}

	public static void calcTurnPath(Path pathForTurn, TurnType turnType, Matrix transform) {
		if(turnType == null){
			return;
		}
		pathForTurn.reset();
		// draw path 96x96
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
		} else if (turnType != null && turnType.isExit()) {
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
		pathForTurn.transform(transform);
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
		if(boundsForMiniRoute.contains(point.x, point.y) && routeLayer != null && routeLayer.getHelper().isRouterEnabled()){
			showMiniMap = !showMiniMap;
			view.refreshMap();
			return true;
		}
		return false;
	}


}
