package com.osmand.views;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.widget.Toast;

import com.osmand.ResourceManager;
import com.osmand.data.Amenity;
import com.osmand.osm.MapUtils;

public class POIMapLayer implements OsmandMapLayer {
	private static final int radiusClick = 2; // for 15 level zoom
	
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<Amenity> objects = new ArrayList<Amenity>();

	private ResourceManager resourceManager;
	
	

	// TODO optimize all evaluations
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN && objects != null) {
			double tileNumberX = MapUtils.getTileNumberX(view.getZoom(), view.getLongitude());
			double tileNumberY = MapUtils.getTileNumberY(view.getZoom(), view.getLatitude());
			double xTileLeft = tileNumberX - view.getWidth() / (2d * getTileSize());
			double yTileUp = tileNumberY - view.getHeight() / (2d * getTileSize());
			int ex = (int) event.getX();
			int ey = (int) event.getY();
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			for(Amenity n : objects){
				double tileX = MapUtils.getTileNumberX(view.getZoom(), n.getLocation().getLongitude());
				int x = (int) ((tileX - xTileLeft) * getTileSize());
				double tileY = MapUtils.getTileNumberY(view.getZoom(), n.getLocation().getLatitude());
				int y = (int) ((tileY - yTileUp) * getTileSize());
				if(Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius){
					Toast.makeText(view.getContext(), n.getSimpleFormat(), Toast.LENGTH_SHORT).show();
					return true;
				}
			}
		} 
//		return super.onTouchEvent(event);
		return false;
	}
	
	public int getTileSize(){
		return view.getTileSize();
		
	}
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		pointAltUI = new Paint();
		pointAltUI.setColor(Color.GREEN);
		pointAltUI.setAlpha(150);
		pointAltUI.setAntiAlias(true);
		resourceManager = ResourceManager.getResourceManager();
	}
	
	public int getRadiusPoi(int zoom){
		if(zoom < 15){
			return 0;
		} else {
			return radiusClick << (zoom - 15);
		} 
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= 15) {
			double tileNumberX = MapUtils.getTileNumberX(view.getZoom(), view.getLongitude());
			double tileNumberY = MapUtils.getTileNumberY(view.getZoom(), view.getLatitude());
			double xTileLeft = tileNumberX - view.getWidth() / (2d * getTileSize());
			double xTileRight = tileNumberX + view.getWidth() / (2d * getTileSize());
			double yTileUp = tileNumberY - view.getHeight() / (2d * getTileSize());
			double yTileDown = tileNumberY + view.getHeight() / (2d * getTileSize());
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), yTileUp);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), xTileLeft);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), yTileDown);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), xTileRight);

			objects.clear();
			resourceManager.searchAmenitiesAsync(topLatitude, leftLongitude, bottomLatitude, rightLongitude, objects);
			for (Amenity o : objects) {
				double tileX = MapUtils.getTileNumberX(view.getZoom(), o.getLocation().getLongitude());
				int x = (int) ((tileX - xTileLeft) * getTileSize());
				double tileY = MapUtils.getTileNumberY(view.getZoom(), o.getLocation().getLatitude());
				int y = (int) ((tileY - yTileUp) * getTileSize());
				canvas.drawCircle(x, y, getRadiusPoi(view.getZoom()), pointAltUI);
			}

		}
	}

	@Override
	public void destroyLayer() {
		
	}

}
