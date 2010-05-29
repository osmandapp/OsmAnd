package com.osmand.views;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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
	
	

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN && objects != null) {
			int ex = (int) event.getX();
			int ey = (int) event.getY();
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						Toast.makeText(view.getContext(), n.getSimpleFormat(), Toast.LENGTH_SHORT).show();
						return true;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
		// return super.onTouchEvent(event);
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
		pixRect.set(0, 0, view.getWidth(), view.getHeight());
	}
	
	public int getRadiusPoi(int zoom){
		if(zoom < 15){
			return 0;
		} else {
			return radiusClick << (zoom - 15);
		} 
	}

	Rect pixRect = new Rect();
	RectF tileRect = new RectF();
	
	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= 15) {
			pixRect.set(0, 0, view.getWidth(), view.getHeight());
			view.calculateTileRectangle(pixRect, view.getCenterPointX(), 
					view.getCenterPointY(), view.getXTile(), view.getYTile(), tileRect);
			double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
			double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
			double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
			double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);

			objects.clear();
			resourceManager.searchAmenitiesAsync(topLatitude, leftLongitude, bottomLatitude, rightLongitude, objects);
			for (Amenity o : objects) {
				int x = view.getMapXForPoint(o.getLocation().getLongitude());
				int y = view.getMapYForPoint(o.getLocation().getLatitude());
				canvas.drawCircle(x, y, getRadiusPoi(view.getZoom()), pointAltUI);
			}

		}
	}

	@Override
	public void destroyLayer() {
		
	}

}
