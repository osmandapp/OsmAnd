package com.osmand;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class PointOfView extends View {
	private Paint location;
	private Paint area;
	
	
	private int areaRadius = 0;
	private int locationX = -1;
	private int locationY = -1;

	public PointOfView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initUI();
	}
	
	public PointOfView(Context context) {
		super(context);
		initUI();
	}
	
	private void initUI() {
		location = new Paint();
		location.setColor(Color.BLUE);
		location.setAlpha(150);
		location.setAntiAlias(true);
		
		area = new Paint();
		area.setColor(Color.BLUE);
		area.setAlpha(40);		
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(locationX >= 0 && locationY >=0){
			canvas.drawCircle(locationX, locationY, 4, location);
		}
		if(areaRadius > 4){
			canvas.drawCircle(locationX, locationY, areaRadius, area);
		}
	}
	
	public void setLocationX(int locationX) {
		this.locationX = locationX;
	}
	
	public void setLocationY(int locationY) {
		this.locationY = locationY;
	}
	
	public int getLocationX() {
		return locationX;
	}
	
	public int getLocationY() {
		return locationY;
	}
	
	public int getAreaRadius() {
		return areaRadius;
	}
	
	public boolean isVisible() {
		return locationX >= 0 && locationY >= 0;
	}
	
	public void setAreaRadius(int areaRadius) {
		this.areaRadius = areaRadius;
	}

}
