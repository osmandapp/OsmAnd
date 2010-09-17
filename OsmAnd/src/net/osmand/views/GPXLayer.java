package net.osmand.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.osmand.LogUtil;
import net.osmand.R;
import net.osmand.osm.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
import android.util.Xml;

public class GPXLayer implements OsmandMapLayer {

	private final static Log log = LogUtil.getLog(GPXLayer.class);
	
	private OsmandMapTileView view;
	
	private Rect boundsRect;
	private RectF tileRect;
	private List<Location> points = new ArrayList<Location>();
	private Paint paint;
	

	private Path path;
	
	public GPXLayer(){
	}
	

	private void initUI() {
		boundsRect = new Rect(0, 0, view.getWidth(), view.getHeight());
		tileRect = new RectF();
		paint = new Paint();
		paint.setColor(Color.argb(190, 160, 10, 215));
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
		path.reset();
		if(points.isEmpty()){
			return;
		}
		int w = view.getWidth();
		int h = view.getHeight();
		boundsRect = new Rect(-w / 2, -h / 2, 3 * w / 2, 3 * h / 2);
		view.calculateTileRectangle(boundsRect, view.getCenterPointX(), view.getCenterPointY(), view.getXTile(), view.getYTile(),
				tileRect);
		double topLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.top);
		double leftLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.left);
		double bottomLatitude = MapUtils.getLatitudeFromTile(view.getZoom(), tileRect.bottom);
		double rightLongitude = MapUtils.getLongitudeFromTile(view.getZoom(), tileRect.right);
		int startIndex = -1;
		int endIndex = -1;
		for (int i = 0; i < points.size(); i++) {
			Location ls = points.get(i);
			if(leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude &&
					bottomLatitude <= ls.getLatitude() && ls.getLatitude() <= topLatitude){
				if(startIndex == -1){
					startIndex = i > 0 ? i - 1 : i;
				}
			} else if(startIndex > 0){
				endIndex = i;
				// do not continue make method more efficient (because it calls in UI thread)
				// this break also has logical sense !
				break;
			}
		}
		if(startIndex == -1){
			return;
		} else if(endIndex == -1){
			endIndex = points.size() - 1;
		}
		
		int px = view.getMapXForPoint(points.get(startIndex).getLongitude());
		int py = view.getMapYForPoint(points.get(startIndex).getLatitude());
		path.moveTo(px, py);
		for (int i = startIndex + 1; i <= endIndex; i++) {
			Location o = points.get(i);
			int x = view.getMapXForPoint(o.getLongitude());
			int y = view.getMapYForPoint(o.getLatitude());
			path.lineTo(x, y);
		}
		canvas.drawPath(path, paint);
	}
	

	public boolean isVisible(){
		return !points.isEmpty();
	}
	
	public String showGPXFile(File f){
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new FileInputStream(f), "UTF-8"); //$NON-NLS-1$
			ArrayList<Location> locations = new ArrayList<Location>();
			int tok;
			Location current = null;
			while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if(tok == XmlPullParser.START_TAG){
					// currently not distinguish different point represents all as a line
					if(parser.getName().equals("wpt") || parser.getName().equals("trkpt") /*|| parser.getName().equals("rtept")*/){ //$NON-NLS-1$ //$NON-NLS-2$
						try {
							current = new Location("gpx_file"); //$NON-NLS-1$
							current.setLatitude(Double.parseDouble(parser.getAttributeValue("", "lat"))); //$NON-NLS-1$ //$NON-NLS-2$
							current.setLongitude(Double.parseDouble(parser.getAttributeValue("", "lon"))); //$NON-NLS-1$ //$NON-NLS-2$
						} catch (NumberFormatException e) {
							current= null;
							
						}
					}
				} else if(tok == XmlPullParser.END_TAG){
					if(parser.getName().equals("wpt") ||  //$NON-NLS-1$
							parser.getName().equals("trkpt") /*|| parser.getName().equals("rtept")*/){ //$NON-NLS-1$ 
						if(current != null){
							locations.add(current);
						}
					}
				}
			}
			this.points = locations;
		} catch (XmlPullParserException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			return view.getContext().getString(R.string.error_reading_gpx);
		} catch (IOException e) {
			log.error("Error reading gpx", e); //$NON-NLS-1$
			return view.getContext().getString(R.string.error_reading_gpx);
		}
		
		return null;
	}
	
	public void clearCurrentGPX(){
		points.clear();
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
