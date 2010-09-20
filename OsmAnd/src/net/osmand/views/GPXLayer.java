package net.osmand.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.MapUtils;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.location.Location;

public class GPXLayer implements OsmandMapLayer {

	
	private OsmandMapTileView view;
	
	private Rect boundsRect;
	private RectF tileRect;
	private List<List<Location>> points = new ArrayList<List<Location>>();
	private Paint paint;
	

	private Path path;
	
	public GPXLayer(){
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		tileRect = new RectF();
		paint = new Paint();
		paint.setColor(Color.argb(180, 160, 10, 215));
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		
		
		path = new Path();
	}
	
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	
	
	@Override
	public void onDraw(Canvas canvas) {
		
		if(points.isEmpty()){
			return;
		}
		int w = view.getWidth();
		int h = view.getHeight();
		boundsRect = new Rect(0, 0, w, h);
		view.calculateTileRectangle(boundsRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), view.getYTile(),
				tileRect);
		double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
		double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
		double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
		double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);
		
		for (List<Location> l : points) {
			path.rewind();
			int startIndex = -1;
			int endIndex = -1;

			for (int i = 0; i < l.size(); i++) {
				Location ls = l.get(i);
				if (startIndex == -1) {
					if (leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude && bottomLatitude <= ls.getLatitude()
							&& ls.getLatitude() <= topLatitude) {
						startIndex = i > 0 ? i - 1 : i;
					}
				} else if (!(leftLongitude <= ls.getLongitude() + 0.01 && ls.getLongitude() - 0.01 <= rightLongitude
						&& bottomLatitude <= ls.getLatitude() + 0.01 && ls.getLatitude() - 0.01 <= topLatitude)) {
					endIndex = i;
					// do not continue make method more efficient (because it calls in UI thread)
					// this break also has logical sense !
					break;
				}
			}
			if (startIndex == -1) {
				return;
			}
			if (endIndex == -1) {
				endIndex = l.size() - 1;
			}

			int px = view.getMapXForPoint(l.get(startIndex).getLongitude());
			int py = view.getMapYForPoint(l.get(startIndex).getLatitude());
			path.moveTo(px, py);
			for (int i = startIndex + 1; i <= endIndex; i++) {
				Location p = l.get(i);
				int x = view.getMapXForPoint(p.getLongitude());
				int y = view.getMapYForPoint(p.getLatitude());
				path.lineTo(x, y);
			}
			canvas.drawPath(path, paint);
		}
		
	}
	

	public boolean isVisible(){
		return !points.isEmpty();
	}
	
	
	public void clearCurrentGPX(){
		points.clear();
	}
	
	public void setTracks(List<List<Location>> tracks){
		if(tracks == null){
			clearCurrentGPX();
		} else {
			points = tracks;
		}
	}
	
	
	@Override
	public void destroyLayer() {
		
	}
	@Override
	public boolean drawInScreenPixels() {
		return false;
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
