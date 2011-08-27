package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.location.Location;

public class GPXLayer implements OsmandMapLayer {

	
	private OsmandMapTileView view;
	
	private List<List<Location>> points = new ArrayList<List<Location>>();
	private Paint paint;
	

	private Path path;
	
	public GPXLayer(){
	}
	

	private void initUI() {
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
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, boolean nightMode, boolean moreDetail) {
		if(points.isEmpty()){
			return;
		}
		
		for (List<Location> l : points) {
			path.rewind();
			int startIndex = -1;

			for (int i = 0; i < l.size(); i++) {
				Location ls = l.get(i);
				if (startIndex == -1) {
					if (ls.getLatitude() >= latLonBounds.bottom && ls.getLatitude() <= latLonBounds.top  && ls.getLongitude() >= latLonBounds.left 
							&& ls.getLongitude() <= latLonBounds.right ) {
						startIndex = i > 0 ? i - 1 : i;
					}
				} else if (!(latLonBounds.left <= ls.getLongitude() + 0.03 && ls.getLongitude() - 0.03 <= latLonBounds.right
						&& latLonBounds.bottom <= ls.getLatitude() + 0.03 && ls.getLatitude() - 0.03 <= latLonBounds.top)) {
					drawSegment(canvas, l, startIndex, i);
					// do not continue make method more efficient (because it calls in UI thread)
					// this break also has logical sense !
					// break;
					startIndex = -1;
				}
			}
			if (startIndex != -1) {
				drawSegment(canvas, l, startIndex, l.size() - 1);
				continue;
			}
		}
		
	}


	private void drawSegment(Canvas canvas, List<Location> l, int startIndex, int endIndex) {
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
