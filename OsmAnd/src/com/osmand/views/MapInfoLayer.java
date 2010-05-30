package com.osmand.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;

import com.osmand.activities.MapActivity;
import com.osmand.osm.MapUtils;

public class MapInfoLayer implements OsmandMapLayer {


	private OsmandMapTileView view;
	private final MapActivity map;
	
	private Paint paintBlack;
	private Path pathForCompass;
	private Path pathForCompass2;
	private Paint fillBlack;
	private Paint fillRed;
	private RectF boundsForCompass;
	private RectF boundsForDist;
	private RectF boundsForSpeed;
	private Paint paintAlphaGray;
	
	
	
	private float[] calculations = new float[1];
	private String cachedDistString = null;
	private int cachedMeters = 0;
	private String cachedSpeedString = null;
	private int cachedSpeed = 0;
	
	
	public MapInfoLayer(MapActivity map){
		this.map = map;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		paintBlack = new Paint();
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setColor(Color.BLACK);
		paintBlack.setTextSize(18);
		paintBlack.setAntiAlias(true);
		
		paintAlphaGray = new Paint();
		paintAlphaGray.setStyle(Style.FILL_AND_STROKE);
		paintAlphaGray.setColor(Color.LTGRAY);
		paintAlphaGray.setAlpha(150);
		
		fillBlack = new Paint();
		fillBlack.setStyle(Style.FILL_AND_STROKE);
		fillBlack.setColor(Color.BLACK);
		fillBlack.setAntiAlias(true);
		
		fillRed = new Paint();
		fillRed.setStyle(Style.FILL_AND_STROKE);
		fillRed.setColor(Color.RED);
		fillRed.setAntiAlias(true);
		
		boundsForCompass = new RectF(0, 0, 32, 32);
		boundsForDist = new RectF(32, 0, 110, 32);
		boundsForSpeed = new RectF(0, 32, 110, 64);
		
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
		if(oldDist != 0 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if(map.getPointToNavigate() != null){
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), map.getPointToNavigate().getLatitude(),
					map.getPointToNavigate().getLongitude(), calculations);
			if(distChanged(cachedMeters, (int)calculations[0])){
				cachedMeters = (int)calculations[0];
				if(cachedMeters <= 20){
					cachedMeters = 0;
					cachedDistString = null;
				} else {
					cachedDistString = MapUtils.getFormattedDistance(cachedMeters);
					boundsForDist.right = paintBlack.measureText(cachedDistString) + 25 + boundsForDist.left;
					boundsForSpeed.right = boundsForDist.right;
				}
			}
		}
		if(map.getLastKnownLocation() != null && map.getLastKnownLocation().hasSpeed()){
			if(cachedSpeed != (int) map.getLastKnownLocation().getSpeed()){
				cachedSpeed = (int) map.getLastKnownLocation().getSpeed();
				cachedSpeedString = ((int) (cachedSpeed * 3.6d)) + " km/h";
			}
			if(cachedSpeed > 0){
				canvas.drawRoundRect(boundsForSpeed, 3, 3, paintAlphaGray);
				canvas.drawText(cachedSpeedString, boundsForSpeed.left + 5, boundsForSpeed.bottom - 9, paintBlack);
			}
		}
		if(cachedDistString != null){
			canvas.drawRoundRect(boundsForDist, 3, 3, paintAlphaGray);
			canvas.drawCircle(boundsForDist.left + 8, boundsForDist.bottom - 15, 4, fillRed);
			canvas.drawText(cachedDistString, boundsForDist.left + 15, boundsForDist.bottom - 9, paintBlack);
		}
		
		// draw the last because it use rotating
		canvas.drawRoundRect(boundsForCompass, 3, 3, paintAlphaGray);
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
