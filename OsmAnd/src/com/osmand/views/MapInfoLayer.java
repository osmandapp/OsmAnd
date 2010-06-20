package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;

import com.osmand.Messages;
import com.osmand.activities.MapActivity;
import com.osmand.osm.MapUtils;

public class MapInfoLayer implements OsmandMapLayer {


	private OsmandMapTileView view;
	private final MapActivity map;
	private final RouteLayer routeLayer;
	
	private Paint paintBlack;
	private Paint paintMiniRoute;
	private Path pathForCompass;
	private Path pathForCompass2;
	private Paint fillBlack;
	private Paint fillRed;
	private RectF boundsForCompass;
	private RectF boundsForZoom;
	private RectF boundsForDist;
	private RectF boundsForMiniRoute;
	private RectF boundsForSpeed;
	private Paint paintAlphaGray;
	
	
	
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
				d = map.getRoutingHelper().getDistance(view.getLatitude(), view.getLongitude());
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
		
		

			
		if(routeLayer != null && !routeLayer.getPath().isEmpty()){
			canvas.save();
			canvas.clipRect(boundsForMiniRoute);
			canvas.drawRoundRect(boundsForMiniRoute, 3, 3, paintAlphaGray);
			canvas.drawRoundRect(boundsForMiniRoute, 3, 3, paintBlack);
			
			canvas.translate(centerMiniRouteX - view.getCenterPointX(), centerMiniRouteY - view.getCenterPointY());
			canvas.scale(scaleMiniRoute, scaleMiniRoute, view.getCenterPointX(), view.getCenterPointY());
			canvas.rotate(view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
			
			canvas.drawCircle(view.getCenterPointX(), view.getCenterPointY(), 3/scaleMiniRoute, fillBlack);
			canvas.drawPath(routeLayer.getPath(), paintMiniRoute);
			canvas.restore();
		}
		
		// draw compass the last because it use rotating
		canvas.drawRoundRect(boundsForCompass, 3, 3, paintAlphaGray);
		canvas.drawRoundRect(boundsForCompass, 3, 3, paintBlack);
		canvas.rotate(view.getRotate(), 15, 15);
		canvas.drawPath(pathForCompass2, fillRed);
		canvas.drawPath(pathForCompass, fillBlack);
		
		
		
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
		return false;
	}


}
