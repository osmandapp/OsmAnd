package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.List;

import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;

public class GPXLayer extends OsmandMapLayer {

	
	private OsmandMapTileView view;
	
	private List<List<WptPt>> points = new ArrayList<List<WptPt>>();
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
	
	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, boolean nightMode) {
		List<List<WptPt>> points = this.points;
		if(points.isEmpty()){
			return;
		}
		
		for (List<WptPt> l : points) {
			path.rewind();
			int startIndex = -1;

			for (int i = 0; i < l.size(); i++) {
				WptPt ls = l.get(i);
				if (startIndex == -1) {
					if (ls.lat >= latLonBounds.bottom && ls.lat <= latLonBounds.top  && ls.lon >= latLonBounds.left 
							&& ls.lon <= latLonBounds.right ) {
						startIndex = i > 0 ? i - 1 : i;
					}
				} else if (!(latLonBounds.left <= ls.lon + 0.03 && ls.lon - 0.03 <= latLonBounds.right
						&& latLonBounds.bottom <= ls.lat + 0.03 && ls.lat - 0.03 <= latLonBounds.top)) {
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


	private void drawSegment(Canvas canvas, List<WptPt> l, int startIndex, int endIndex) {
		int px = view.getMapXForPoint(l.get(startIndex).lon);
		int py = view.getMapYForPoint(l.get(startIndex).lat);
		path.moveTo(px, py);
		for (int i = startIndex + 1; i <= endIndex; i++) {
			WptPt p = l.get(i);
			int x = view.getMapXForPoint(p.lon);
			int y = view.getMapYForPoint(p.lat);
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
	
	public void setTracks(List<Track> tracks){
		if(tracks == null){
			clearCurrentGPX();
		} else {
			List<List<WptPt>> tpoints = new ArrayList<List<WptPt>>();
			for (Track t : tracks) {
				for (TrkSegment ts : t.segments) {
					tpoints.add(ts.points);
				}
			}
			points = tpoints;
			
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
	public boolean onSingleTap(PointF point) {
		return false;
	}




}
