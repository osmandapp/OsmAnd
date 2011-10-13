package net.osmand.plus.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.OsmAndFormatter;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.MapObject;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandApplication;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

// Map layer provides capabilities of exploration mode
public class ExploreInfoLayer extends BaseMapLayer implements OsmandMapTileView.OnPointTouchListener {

	private TextView textView;
	private DisplayMetrics dm;
	private OsmandMapTileView view;
	private static final int BASE_TEXT_SIZE = 170;
	private int textSize = BASE_TEXT_SIZE;

	private Paint paintLightBorder;
	private Paint paintBlack;
	private RectF textBorder;
	private Paint paintBorder;
	private final MapActivity activity;

	// Coordinates of exploring point
	private LatLon latLon;

	protected Handler handler = new Handler();

	// Angle of compass
	private float heading = 0;

	public ExploreInfoLayer(MapActivity activity){
		this.activity = activity;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		dm = new DisplayMetrics();
		WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
		wmgr.getDefaultDisplay().getMetrics(dm);
		textSize = (int) (BASE_TEXT_SIZE * dm.density);

		paintLightBorder = new Paint();
		paintLightBorder.setARGB(130, 220, 220, 220);
		paintLightBorder.setStyle(Style.FILL);
		paintBlack = new Paint();
		paintBlack.setARGB(255, 0, 0, 0);
		paintBlack.setStyle(Style.STROKE);
		paintBlack.setAntiAlias(true);
		paintBorder = new Paint();
		paintBorder.setARGB(220, 160, 160, 160);
		paintBorder.setStyle(Style.FILL);

		textView = new TextView(view.getContext());
		LayoutParams lp = new LayoutParams(textSize, LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setTextSize(16);
		textView.setTextColor(Color.argb(255, 0, 0, 0));
		textView.setMinLines(1);
		textView.setGravity(Gravity.CENTER_HORIZONTAL);
		textBorder = new RectF(-2, -1, textSize + 2, 0);

		view.setOnPointTouchListener(this);
	}

	@Override
	// Output text view with information of exploring point
	public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect,
			boolean nightMode) {
		if(latLon != null) {
			int x = view.getMapXForPoint(latLon.getLongitude());
			int y = view.getMapYForPoint(latLon.getLatitude());
			canvas.drawCircle(x, y, 5 * dm.density, paintBorder);
			canvas.drawCircle(x, y, 5 * dm.density, paintBlack);

			if (textView.getText().length() > 0) {
				x = view.getRotatedMapXForPoint(latLon.getLatitude(), latLon.getLongitude());
				y = view.getRotatedMapYForPoint(latLon.getLatitude(), latLon.getLongitude());
				canvas.rotate(-view.getRotate(), view.getCenterPointX(), view.getCenterPointY());
				canvas.translate(x - textView.getWidth() / 2, y - textView.getHeight() - 12);
				int c = textView.getLineCount();
				textBorder.bottom = textView.getHeight() + 2;
				canvas.drawRect(textBorder, paintLightBorder);
				canvas.drawRect(textBorder, paintBlack);
				textView.draw(canvas);
				if (c == 0) {
					// special case relayout after on draw method
					textView.layout(0, 0, textSize, (int) ((textView.getPaint().getTextSize() + 4) * textView.getLineCount()));
					view.refreshMap();
				}
			}
		}
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean onTouchEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	private LatLon getLocationPoint() {
		return new LatLon(view.getLatitude(), view.getLongitude());
	}

	private float getHeading() {
		return heading;
	}

	private boolean getRotateMapCompass() {
		OsmandApplication app = (OsmandApplication)activity.getApplication();
		return app.getSettings().ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS;
	}

	private float getMapHeading() {
		if (getRotateMapCompass()) return heading; else return 0;
	}

	private void setInfo(LatLon loc, String info) {
		latLon = loc;
		if(latLon != null){
			textView.setText(info);
			view.setExploreInfo(info);
		} else {
			textView.setText(""); //$NON-NLS-1$
			view.setExploreInfo("");
		}
		textView.layout(0, 0, textSize, (int) ((textView.getPaint().getTextSize()+4) * textView.getLineCount()));
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	public PointF getNearestPointToLine(PointF point, PointF lineA, PointF lineB) {
		float dx21 = lineB.x - lineA.x;
		float dy21 = lineB.y - lineA.y;
		float dx1 = point.x - lineA.x;
		float dy1 = point.y - lineB.y;
		float aSide = dx21 * dx1 + dy21 * dy1;
		if (aSide <= 0)
			return lineA;

		float dx2 = point.x - lineB.x;
		float dy2 = point.y - lineB.y;

		aSide = dx21 * dx2 + dy21 * dy2;
		if (aSide >= 0)
			return lineB;

		float d = dy21 * dx1 - dx21 * dy1;
		float l2 = dx21 * dx21 + dy21 * dy21;
		d = d / l2;
		return new PointF(point.x - dy21 * d, point.y + dx21 * d);
	}

	public double getDistancePointToLine(LatLon point, PointF lineA, PointF lineB, PointF nearest) {
		PointF n = getNearestPointToLine(
					new PointF(MapUtils.get31TileNumberX(point.getLongitude()), MapUtils.get31TileNumberY(point.getLatitude())), lineA, lineB);
		nearest.x = n.x;
		nearest.y = n.y;

		LatLon nearLoc = new LatLon(MapUtils.get31LatitudeY((int) nearest.y), MapUtils.get31LongitudeX((int) nearest.x));

		return MapUtils.getDistance(point, nearLoc);
	}

	public double getDistancePointToMapDataObject(LatLon point, BinaryMapDataObject object, PointF nearest) {
		double min = 0;
		int prevX = 0, prevY = 0;
		double d = 0;
		PointF near = new PointF(0, 0);

		for (int i = 0; i < object.getPointsLength(); i++) {	
			int x = object.getPoint31XTile(i);
			int y = object.getPoint31YTile(i);
			if (i == 0) {
				prevX = x;
				prevY = y;
				d = MapUtils.getDistance(point,
						new LatLon(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x)));
			} else {
				d = getDistancePointToLine(point, new PointF(prevX, prevY), new PointF(x, y), near);
				if (i == 1) {
					min = d;
					nearest.x = near.x;
					nearest.y = near.y;
				} else if (d < min) {
					min = d;
					nearest.x = near.x;
					nearest.y = near.y;
				}
			}
		}
		return d;
	}

	@Override
	public boolean onPointTouchEvent(PointF point) {
		handler.removeMessages(1);

		final int z = view.getZoom() > 15 ? view.getZoom() : 8;
		final PointF p = point;
		final LatLon leftTop = view.getLatLonFromScreenPoint(p.x - dm.density * z, p.y - dm.density * z);
		final LatLon rightBottom = view.getLatLonFromScreenPoint(p.x + dm.density * z, p.y + dm.density * z);

		Message msg = Message.obtain(handler, new Runnable() {
			@Override
			public void run() {
				exploreMap(p, leftTop, rightBottom);
			}
		});
		msg.what = 1;
		handler.sendMessageDelayed(msg, 10);

		return true; 
	}

	// Angle distance between 2 points
	// Returns value in radiant
	private static double distanceAngle(LatLon a, LatLon b) {
		if (a.getLongitude() == b.getLongitude() && a.getLatitude() == b.getLatitude()) 
		    return 0; 
		else
		    return Math.acos(Math.sin(a.getLatitude()) * Math.sin(b.getLatitude()) +
		                     Math.cos(a.getLatitude()) * Math.cos(b.getLatitude()) *
		                     Math.cos(b.getLongitude() - a.getLongitude()));
	}

	public static double radEps = 0.00001;

	// Azimuth between 2 given points
	// Returns value in radiant
	public static double azimuth2Points(LatLon a, LatLon b) {
		if (Math.cos(a.getLatitude()) == 0) {
		    if (a.getLatitude() > 0) return Math.PI;
		    return 0;
		} 

		if (a.getLongitude() == b.getLongitude() || Math.abs(a.getLongitude() - b.getLongitude()) < radEps) {
			if (a.getLatitude() >= b.getLatitude()) return Math.PI;
			return 0;
		}

		double dAngle = distanceAngle(a, b);
		double angle = Math.acos((Math.sin(b.getLatitude()) - Math.sin(a.getLatitude()) * Math.cos(dAngle)) /
						(Math.cos(a.getLatitude()) * Math.sin(dAngle)));

		if (Math.sin(a.getLongitude() - b.getLongitude()) >= 0)
			angle = 2 * Math.PI - angle;

		return angle;
	}

	private List<MapObjectInfo> exploredObjects = new ArrayList<MapObjectInfo>();

	private MapObjectInfo findMapObjectByName(String name) {
		for (MapObjectInfo o : exploredObjects) {
			if (o.name.compareToIgnoreCase(name) == 0)
				return o;
		}
		return null;
	}

	private void sortMapObjectByDistance() {
		Collections.sort(exploredObjects, new Comparator<MapObjectInfo>() {
			@Override
			public int compare(MapObjectInfo o1, MapObjectInfo o2) {
				return Double.compare(o1.distance, o2.distance);
			}
		});	
	}

	// Get objects from vector map for specified region.
	// Calculate distance and angle for these objects.
	private void exploreMapObjects(LatLon loc, LatLon leftTop, LatLon rightBottom) {
		exploredObjects.clear();
		List<BinaryMapDataObject> objects = searchObjects(leftTop, rightBottom);
		double maxDistance = 
			Math.max(
				Math.max(MapUtils.getDistance(loc, leftTop), MapUtils.getDistance(loc, rightBottom)),
				Math.max(MapUtils.getDistance(loc, new LatLon(leftTop.getLatitude(), rightBottom.getLongitude())), 
						MapUtils.getDistance(loc, new LatLon(rightBottom.getLatitude(), leftTop.getLongitude()))));

		if (objects != null && !objects.isEmpty()) {
			double d, a;
			PointF nearest = new PointF(0, 0);

			for (BinaryMapDataObject o : objects) {
				if (o.getName() != null) {
					MapObjectInfo info = findMapObjectByName(o.getName());
					if (info != null) {
						d = getDistancePointToMapDataObject(loc, o, nearest);
						if (d < info.distance) {
							exploredObjects.remove(info);
							a = azimuth2Points(loc, new LatLon(MapUtils.get31LatitudeY((int) nearest.y), MapUtils.get31LongitudeX((int) nearest.x)));
							if (d <= maxDistance * 1.5)
								exploredObjects.add(new MapObjectInfo(o.getName(), d, a));
						}
					} else {
						d = getDistancePointToMapDataObject(loc, o, nearest);
						a = azimuth2Points(loc, new LatLon(MapUtils.get31LatitudeY((int) nearest.y), MapUtils.get31LongitudeX((int) nearest.x)));
						if (d <= maxDistance * 1.5)
							exploredObjects.add(new MapObjectInfo(o.getName(), d, a));
					}
				}
			}
		}
	}

	private void exploreMapObjects(LatLon loc, LatLon leftTop, LatLon rightBottom, float direction, double distance) {
		exploredObjects.clear();

		List<BinaryMapDataObject> objects = searchObjects(leftTop, rightBottom);

		if (objects != null && !objects.isEmpty()) {
			double d, a;
			float degree, _degree;
			PointF nearest = new PointF(0, 0);

			for (BinaryMapDataObject o : objects) {
				if (o.getName() != null) {
					MapObjectInfo info = findMapObjectByName(o.getName());
					if (info != null) {
						d = getDistancePointToMapDataObject(loc, o, nearest);
						if (d < info.distance) {
							a = azimuth2Points(loc, new LatLon(MapUtils.get31LatitudeY((int) nearest.y), MapUtils.get31LongitudeX((int) nearest.x)));
							degree = MapUtils.unifyRotationTo360((float)(a * 180 / Math.PI) - getMapHeading());
							_degree = MapUtils.unifyRotationDiff(degree, direction);
							if (Math.abs(_degree) <= 15) {
								exploredObjects.remove(info);
								exploredObjects.add(new MapObjectInfo(o.getName(), d, a));
							}
						}
					} else {
						d = getDistancePointToMapDataObject(loc, o, nearest);
						a = azimuth2Points(loc, new LatLon(MapUtils.get31LatitudeY((int) nearest.y), MapUtils.get31LongitudeX((int) nearest.x)));
						degree = MapUtils.unifyRotationTo360((float)(a * 180 / Math.PI) - getMapHeading());
						_degree = MapUtils.unifyRotationDiff(degree, direction);

						if (Math.abs(_degree) <= 15 && d <= distance * 1.5) 
							exploredObjects.add(new MapObjectInfo(o.getName(), d, a));
					}
				}
			}
		}
	}

	private String getExploredObjectText(boolean sortByDistance, int maxRows) {
		String s = ""; //String.valueOf(zoom);
		boolean useMaxRows = maxRows > 0;

		if (sortByDistance)
			sortMapObjectByDistance();

		for (MapObjectInfo info : exploredObjects) {
			String ds = OsmAndFormatter.getFormattedDistance((float) info.distance, view.getContext());
			float degree = MapUtils.unifyRotationTo360((float)(info.azimuth * 180 / Math.PI) - getMapHeading());
			int hour = degreeToHour(degree);

			if (s.length() != 0)
				s += "\n";
			s += info.name + " - " + ds + ", " + hour + " ч";

			if (useMaxRows) {
				maxRows--;
				if (maxRows == 0)
					break;
			}
		}
		return s;
	}

	// Print data on screen.
	private void exploreMap(PointF point, LatLon leftTop, LatLon rightBottom) {
		LatLon loc = getLocationPoint();
		exploreMapObjects(loc, leftTop, rightBottom);
		String s = getExploredObjectText(true, 7);

		if (s.length() > 0) {
			setInfo(view.getLatLonFromScreenPoint(point.x, point.y), s);
		} else {
			setInfo(null, s);
		}

		view.refreshMap();			
	}

	private double extMod(double y, double x) {
		double result = y - x * Math.rint(y/x);

		if (result < 0)
			return result + x;
		return result;
	}

	// Coordinate of the point for specified distance in meters and azimuth in radians.
	// Return point's coordinates in radians.
	private LatLon getAzimuthDistancePoint(double latRad, double lonRad, double azimuth, double distance) {
		double lat, lon;
		double dAngle = distance / 6371000;
		lat = Math.asin(Math.sin(latRad) * Math.cos(dAngle) + Math.cos(latRad) *
	                            Math.sin(dAngle) * Math.cos(azimuth));
		double dLong = Math.atan2(Math.sin(azimuth) * Math.sin(dAngle) * Math.cos(latRad),
	                   Math.cos(dAngle) - Math.sin(latRad) * Math.sin(lat));
		lon = Math.PI - extMod(-lonRad - dLong + Math.PI, 2 * Math.PI);
		return new LatLon(Math.toDegrees(lat), Math.toDegrees(lon));
	}

	private LatLon getAzimuthDistancePoint(LatLon p, double azimuth, double distance) {
		return getAzimuthDistancePoint(Math.toRadians(p.getLatitude()), Math.toRadians(p.getLongitude()), azimuth, distance);
	}

	public void LookForward() {
		double d = MapUtils.getDistance(view.getLatLonFromScreenPoint(0, 0), view.getLatLonFromScreenPoint(view.getCenterPointX(), view.getCenterPointY()));
		LatLon loc = getLocationPoint();
		exploreMap(loc, getMapHeading(), d);
	}

	public void LookRight() {
		double d = MapUtils.getDistance(view.getLatLonFromScreenPoint(0, 0), view.getLatLonFromScreenPoint(view.getCenterPointX(), view.getCenterPointY()));
		LatLon loc = getLocationPoint();
		exploreMap(loc, MapUtils.unifyRotationTo360(getMapHeading() + 90), d);
	}

	public void LookLeft() {
		double d = MapUtils.getDistance(view.getLatLonFromScreenPoint(0, 0), view.getLatLonFromScreenPoint(view.getCenterPointX(), view.getCenterPointY()));
		LatLon loc = getLocationPoint();
		exploreMap(loc, MapUtils.unifyRotationTo360(getMapHeading() + 270), d);
	}

	public void LookBack() {
		double d = MapUtils.getDistance(view.getLatLonFromScreenPoint(0, 0), view.getLatLonFromScreenPoint(view.getCenterPointX(), view.getCenterPointY()));
		LatLon loc = getLocationPoint();
		exploreMap(loc, MapUtils.unifyRotationTo360(getMapHeading() + 180), d);
	}

	private void exploreMap(LatLon loc, float direction, double distance) {
		double rad1 = Math.toRadians(direction - 15);
		double rad2 = Math.toRadians(direction + 15);
		LatLon p1 = getAzimuthDistancePoint(loc, rad1, distance);
		LatLon p2 = getAzimuthDistancePoint(loc, rad2, distance);

		view.log.debug("Location: " + loc.toString());
		view.log.debug("Point 1: " + p1.toString());
		view.log.debug("Point 2: " + p2.toString());
		view.log.debug("Explore map: " + String.valueOf(direction) + ", " + String.valueOf(distance));

		LatLon leftTop = 
			new LatLon(Math.max(loc.getLatitude(), Math.max(p1.getLatitude(), p2.getLatitude())),
					Math.min(loc.getLongitude(), Math.min(p1.getLongitude(), p2.getLongitude())));
		LatLon rightBottom =
			new LatLon(Math.min(loc.getLatitude(), Math.min(p1.getLatitude(), p2.getLatitude())),
					Math.max(loc.getLongitude(), Math.max(p1.getLongitude(), p2.getLongitude())));

		exploreMapObjects(loc, leftTop, rightBottom, direction, distance);

		String s = getExploredObjectText(true, 7);

		if (s.length() > 0) {
			setInfo(loc, s);
		} else {
			setInfo(null, s);
		}

		view.refreshMap();
	}

	// Get list of objects for specified region.
	private List<BinaryMapDataObject> searchObjects(LatLon leftTop, LatLon rightBottom) {
		return view.getApplication().getResourceManager().getExplorer().loadMapObjects(
				new RectF((float) leftTop.getLongitude(), (float) leftTop.getLatitude(), (float) rightBottom.getLongitude(), (float) rightBottom.getLatitude()), view.getZoom());		
	}

	// Set compass angle.
	public void setHeading(float val) {
		heading = val;
	}

	// Convert radians to hours.
	public int radiantToHour(double rad) {
		return degreeToHour((float)(rad * 180 / Math.PI));
	}

	// Convert angle to hours.
	public int degreeToHour(float degree) {
		double hour = Math.abs((degree) / 30);
		return (hour >= 11.5 || hour < 0.5) ? 12 : (int) Math.round(hour);
	}

	// This class is intended for objects info storage.
	private class MapObjectInfo {
		public String name;
		public double distance;
		public double azimuth;

		public MapObjectInfo(String name, double distance, double azimuth) {
			this.name = name;
			this.distance = distance;
			this.azimuth = azimuth;
		}
	}

	public void whereAmI(double latitude, double longitude, int fZoom) {
		PointF point = new PointF(MapUtils.get31TileNumberX(longitude), MapUtils.get31TileNumberY(latitude));
		float d = view.calcDiffTileX( dm.density * fZoom,  dm.density * fZoom);
		LatLon leftTop = view.getLatLonFromScreenPoint(point.x - d, point.y - d);
		LatLon rightBottom = view.getLatLonFromScreenPoint(point.x + d, point.y + d);

		LatLon loc = new LatLon(latitude, longitude);
		exploreMapObjects(loc, leftTop, rightBottom);

		String s = getExploredObjectText(true, 5);

		if (s.length() > 0) {
			loc = view.getLatLonFromScreenPoint(view.getCenterPointX(), view.getCenterPointY());
			setInfo(loc, s);
		} else {
			setInfo(null, s);
		}

		view.refreshMap();
	}
}
