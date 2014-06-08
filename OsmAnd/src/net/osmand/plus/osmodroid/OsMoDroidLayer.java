package net.osmand.plus.osmodroid;

import java.util.ArrayList;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.*;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;
import android.content.DialogInterface;

/**
 * Class represents a OsMoDroidlayer which depicts the position of Esya.ru channels objects
 * 
 * @author Denis Fokin
 * @see OsMoDroidPlugin
 * 
 */
public class OsMoDroidLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	/**
	 * magic number so far
	 */
	private static final int radius = 10;
	OsMoDroidPoint seekOsMoDroidPoint;
	OsMoDroidPlugin myOsMoDroidPlugin;

	private DisplayMetrics dm;

	private final MapActivity map;
	private OsmandMapTileView view;

	private Paint textPaint;

	ArrayList<OsMoDroidPoint> osMoDroidPointArrayList;
	ArrayList<OsMoDroidPoint> osMoDroidFixedPointArrayList;
	ArrayList<ColoredGPX> gpxArrayList = new ArrayList<ColoredGPX>() ;
	int layerId;
	String layerName;
	String layerDescription;
	private Paint paint;

	private Path path;

	private OsmandSettings settings;
	
	private RenderingRulesStorage cachedRrs;
	private boolean cachedNightMode;
	private int cachedColor;
	
	
	private void initUI() {
		paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(14);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);

		path = new Path();
	}
	private Bitmap opIcon;

	public void refresh() {
		map.refreshMap();
	}

	public OsMoDroidLayer(MapActivity map) {
		this.map = map;
	}

	public OsMoDroidLayer(MapActivity map, int layerId, OsMoDroidPlugin osMoDroidPlugin, String layerName, String layerDescription) {
		this.map = map;
		this.layerId = layerId;
		this.myOsMoDroidPlugin = osMoDroidPlugin;
		this.layerName = layerName;
		this.layerDescription = layerDescription;
		

	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		textPaint = new Paint();
		textPaint.setDither(true);
		textPaint.setAntiAlias(true);
		textPaint.setFilterBitmap(true);

		textPaint.setTextSize(22f);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setTextAlign(Paint.Align.CENTER);
		opIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.bicycle_location);
		osMoDroidPointArrayList = myOsMoDroidPlugin.getOsMoDroidPointArrayList(layerId);
		osMoDroidFixedPointArrayList = myOsMoDroidPlugin.getOsMoDroidFixedPointArrayList(layerId);
		initUI();
	}
	
	public void inGPXFilelist(ArrayList<ColoredGPX> in){
		gpxArrayList=in;
		map.refreshMap();
	}
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

		for (ColoredGPX cg : gpxArrayList){
			List<List<WptPt>> points = cg.gpxFile.proccessPoints();
			
			paint.setColor(cg.color);

			final QuadRect latLonBounds = tileBox.getLatLonBounds();
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
						drawSegment(canvas, tileBox, l, startIndex, i);
						startIndex = -1;
					}
				}
				if (startIndex != -1) {
					drawSegment(canvas, tileBox, l, startIndex, l.size() - 1);
					continue;
				}
			}
		}
		
		for (OsMoDroidPoint op : osMoDroidPointArrayList) {
			if(seekOsMoDroidPoint!=null&&seekOsMoDroidPoint.equals(op)){
				map.setMapLocation(op.latlon.getLatitude(), op.latlon.getLongitude());
			}
			LatLon newLatlon;
			try {

				newLatlon = new LatLon(myOsMoDroidPlugin.mIRemoteService.getObjectLat(layerId, op.id),
						myOsMoDroidPlugin.mIRemoteService.getObjectLon(layerId, op.id));

				if (!op.latlon.equals(newLatlon)) {
					op.prevlatlon = op.latlon;
				}
				op.latlon = newLatlon;
				op.speed = myOsMoDroidPlugin.mIRemoteService.getObjectSpeed(layerId, op.id);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}

			double latitude = op.latlon.getLatitude();
			double longitude = op.latlon.getLongitude();
			double prevlatitude = op.latlon.getLatitude();
			double prevlongitude = op.latlon.getLongitude();
			if (op.prevlatlon != null) {
				prevlatitude = op.prevlatlon.getLatitude();
				prevlongitude = op.prevlatlon.getLongitude();
			}

			int locationX = (int) tileBox.getPixXFromLatLon(latitude, longitude);
			int locationY = (int) tileBox.getPixYFromLatLon(latitude, longitude);
			int prevlocationX = (int) tileBox.getPixXFromLatLon(prevlatitude, prevlongitude);
			int prevlocationY = (int) tileBox.getPixYFromLatLon(prevlatitude, prevlongitude);

			// int y = opIcon.getHeight()/2;
			// int x = opIcon.getWidth()/2;
			textPaint.setColor(Color.parseColor("#013220"));
			canvas.drawText(op.name, locationX, locationY - radius, textPaint);
			canvas.drawText(op.speed, locationX, locationY - 2 * radius, textPaint);
			textPaint.setColor(Color.parseColor("#" + op.color));
			textPaint.setShadowLayer(radius, 0, 0, Color.GRAY);
			canvas.drawCircle(locationX, locationY, radius, textPaint);
			// canvas.drawBitmap(opIcon, locationX-x, locationY-y , textPaint);
			textPaint.setStrokeWidth(radius);
			canvas.drawLine(locationX, locationY, prevlocationX, prevlocationY, textPaint);
			// canvas.rotate(-view.getRotate(), locationX, locationY);
			// op.prevlatlon=op.latlon;
			
		}
		
		for (OsMoDroidPoint point : osMoDroidFixedPointArrayList ){
			double latitude = point.latlon.getLatitude();
			double longitude = point.latlon.getLongitude();
			int locationX = (int) tileBox.getPixXFromLatLon(latitude, longitude);
			int locationY = (int) tileBox.getPixYFromLatLon(latitude, longitude);
			textPaint.setColor(Color.parseColor("#013220"));
			canvas.drawText(point.name, locationX, locationY - radius, textPaint);
			textPaint.setColor(Color.parseColor("#" + point.color));
			textPaint.setShadowLayer(radius, 0, 0, Color.GRAY);
			canvas.drawRect(new Rect(locationX-radius, locationY-radius, locationX+radius, locationY+radius), textPaint);
		}
		

	
	}
	private void drawSegment(Canvas canvas, RotatedTileBox tb, List<WptPt> l, int startIndex, int endIndex) {
		int px = (int) tb.getPixXFromLatLon(l.get(startIndex).lat, l.get(startIndex).lon);
		int py = (int) tb.getPixYFromLatLon(l.get(startIndex).lat, l.get(startIndex).lon);
		path.moveTo(px, py);
		for (int i = startIndex + 1; i <= endIndex; i++) {
			WptPt p = l.get(i);
			int x = (int) tb.getPixXFromLatLon(p.lat,p.lon);
			int y = (int) tb.getPixYFromLatLon(p.lat,p.lon);
			path.lineTo(x, y);
		}
		canvas.drawPath(path, paint);
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {}

	public void getOsMoDroidPointFromPoint(RotatedTileBox tb,PointF point, List<? super OsMoDroidPoint> om) {
		if (osMoDroidPointArrayList != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;

			try {
				for (int i = 0; i < osMoDroidPointArrayList.size(); i++) {
					OsMoDroidPoint n = osMoDroidPointArrayList.get(i);
					if (!om.contains(n)) {
						int x = (int) tb.getPixXFromLatLon(n.latlon.getLatitude(), n.latlon.getLongitude());
						int y = (int) tb.getPixYFromLatLon(n.latlon.getLatitude(), n.latlon.getLongitude());
						if (Math.abs(x - ex) <= opIcon.getWidth() && Math.abs(y - ey) <= opIcon.getHeight()) {
							om.add(n);
						}
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce
				// synchronized block
			}
		}
		if (osMoDroidFixedPointArrayList != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;

			try {
				for (int i = 0; i <osMoDroidFixedPointArrayList.size(); i++) {
					OsMoDroidPoint n =osMoDroidFixedPointArrayList.get(i);
					if (!om.contains(n)) {
						int x = (int) tb.getPixXFromLatLon(n.latlon.getLatitude(), n.latlon.getLongitude());
						int y = (int) tb.getPixYFromLatLon(n.latlon.getLatitude(), n.latlon.getLongitude());
						if (Math.abs(x - ex) <= opIcon.getWidth() && Math.abs(y - ey) <= opIcon.getHeight()) {
							om.add(n);
						}
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce
				// synchronized block
			}
		}
	}

	@Override
	public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
		if (o instanceof OsMoDroidPoint && ((OsMoDroidPoint) o).layerId == layerId) {
			final OsMoDroidPoint a = (OsMoDroidPoint) o;
			OnContextMenuClick listener = new ContextMenuAdapter.OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					
					map.getMyApplication().getTargetPointsHelper().navigateToPoint(a.latlon, true, -1);
				}
			};
			OnContextMenuClick seeklistener = new ContextMenuAdapter.OnContextMenuClick() {
				
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if(seekOsMoDroidPoint!=null&&a.equals(seekOsMoDroidPoint))
						{
							seekOsMoDroidPoint=null;
							isChecked=false;
						} else 
							{
								seekOsMoDroidPoint=a;
								isChecked=true;
							}	
				}
			};
			
			adapter.item(map.getString(R.string.get_directions)).listen(listener).reg();
			adapter.item(map.getString(R.string.osmodroid_seek)).listen(seeklistener).reg();

		}
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		List<OsMoDroidPoint> om = new ArrayList<OsMoDroidPoint>();
		getOsMoDroidPointFromPoint(tileBox, point, om);
		if (!om.isEmpty()) {
			StringBuilder res = new StringBuilder();
			for (int i = 0; i < om.size(); i++) {
				OsMoDroidPoint n = om.get(i);
				if (i > 0) {
					res.append("\n\n");
				}
				res = res.append(n.description);
			}
			AccessibleToast.makeText(view.getContext(), res.toString(), Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public String getObjectName(Object o) {
		if (o instanceof OsMoDroidPoint) {
			return ((OsMoDroidPoint) o).name;
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o) {
		getOsMoDroidPointFromPoint(tileBox, point, o);

	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof OsMoDroidPoint) {
			return ((OsMoDroidPoint) o).latlon;
		}
		return null;
	}

	@Override
	public String getObjectDescription(Object o) {
		if (o instanceof OsMoDroidPoint) {
			return ((OsMoDroidPoint) o).description;
		}
		return null;
	}

}