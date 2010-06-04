package com.osmand.views;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Toast;

import com.osmand.OsmandSettings;
import com.osmand.ResourceManager;
import com.osmand.data.Amenity;
import com.osmand.osm.MapUtils;

public class POIMapLayer implements OsmandMapLayer {
	// it is very slow to use with 15 level
	private static final int startZoom = 16;
	
	private Paint pointAltUI;
	private OsmandMapTileView view;
	private List<Amenity> objects = new ArrayList<Amenity>();

	private ResourceManager resourceManager;
	
	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}
	

	@Override
	public boolean onTouchEvent(PointF point) {
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getRadiusPoi(view.getZoom()) * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = view.getRotatedMapXForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = view.getRotatedMapYForPoint(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						String format = n.getSimpleFormat(OsmandSettings.usingEnglishNames(view.getContext()));
						if(n.getOpeningHours() != null){
							format += "\n Opening hours : " + n.getOpeningHours();
						}
						Toast.makeText(view.getContext(), format, Toast.LENGTH_SHORT).show();
						return true;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
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
		if(zoom < startZoom){
			return 0;
		} else if(zoom == 16){
			return 10;
		} else if(zoom == 17){
			return 14;
		} else {
			return 18;
		}
	}

	Rect pixRect = new Rect();
	RectF tileRect = new RectF();
	
	@Override
	public void onDraw(Canvas canvas) {
		if (view.getZoom() >= startZoom) {
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

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

}
