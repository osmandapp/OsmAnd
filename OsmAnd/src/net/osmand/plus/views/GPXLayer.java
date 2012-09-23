package net.osmand.plus.views;

import java.util.List;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;

public class GPXLayer extends OsmandMapLayer {
	
	private OsmandMapTileView view;
	
	private Paint paint;

	private Path path;

	private OsmandSettings settings;
	
	
	private void initUI() {
		paint = new Paint();
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
		settings = view.getSettings();
		initUI();
	}

	
	
	@Override
	public void onDraw(Canvas canvas, RectF latLonBounds, RectF tilesRect, DrawSettings nightMode) {
		GPXFile gpxFile = view.getApplication().getGpxFileToDisplay();
		if(gpxFile == null){
			return;
		}
		List<List<WptPt>> points = gpxFile.processedPointsToDisplay;
		if (false && view.getSettings().FLUORESCENT_OVERLAYS.get()) {
			paint.setColor(view.getResources().getColor(R.color.gpx_track_fluorescent));
		} else {
			paint.setColor(view.getResources().getColor(R.color.gpx_track));
		}
		
		for (List<WptPt> l : points) {
			path.rewind();
			int startIndex = -1;

			for (int i = 0; i < l.size(); i++) {
				WptPt ls = l.get(i);
				if (startIndex == -1) {
					if (ls.lat >= latLonBounds.bottom - 0.1 && ls.lat <= latLonBounds.top + 0.1  && ls.lon >= latLonBounds.left - 0.1 
							&& ls.lon <= latLonBounds.right + 0.1) {
						startIndex = i > 0 ? i - 1 : i;
					}
				} else if (!(latLonBounds.left <= ls.lon + 0.1 && ls.lon - 0.1 <= latLonBounds.right
						&& latLonBounds.bottom <= ls.lat + 0.1 && ls.lat - 0.1 <= latLonBounds.top)) {
					drawSegment(canvas, l, startIndex, i);
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
	

	public boolean isShowingCurrentTrack(){
		return settings.SHOW_CURRENT_GPX_TRACK.get();
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
