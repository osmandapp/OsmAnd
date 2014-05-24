package net.osmand.plus.gpxedit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.CallbackWithObject;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Koen Rabaey
 */
public class GpxEditorLayer extends OsmandMapLayer {
	private final MapActivity _mapActivity;
	private final OsmandApplication _app;
	private final GpxEditorStates _states;
	private final GpxEditorWidget _widget;

	private OsmandMapTileView _mapView;

	private Bitmap _iconPoi;
	private Bitmap _iconTrack;
	private Bitmap _iconRoute;

	private Paint _bitmapPaint;

	private Paint _paintText;

	private Path _track;
	private Path _route;

	private Paint _trackPaint;
	private Paint _trackPaintSelected;
	private Paint _routePaint;
	private Paint _routePaintSelected;

	private int _boxSize;

	public GpxEditorLayer(final MapActivity activity, final GpxEditorWidget widget, final GpxEditorStates states) {
		_mapActivity = activity;
		_app = _mapActivity.getMyApplication();
		_states = states;
		_widget = widget;
	}

	@Override
	public void initLayer(OsmandMapTileView mapView) {
		_mapView = mapView;

		final boolean nightMode = _mapActivity.getMyApplication().getDaynightHelper().isNightMode();

		_iconPoi = BitmapFactory.decodeResource(mapView.getResources(), R.drawable.gpx_edit_favourite);
		_iconTrack = BitmapFactory.decodeResource(mapView.getResources(), R.drawable.gpx_edit_track);
		_iconRoute = BitmapFactory.decodeResource(mapView.getResources(), R.drawable.gpx_edit_route);

		final int trackColor = _app.getResources().getColor(nightMode ? R.color.nav_track_fluorescent : R.color.nav_track);
		_trackPaint = createPaint(mapView.getDensity() * 3, trackColor);
		_trackPaintSelected = createPaint(mapView.getDensity() * 7, trackColor);

		final int routeColor = _app.getResources().getColor(nightMode ? R.color.gpx_track_fluorescent : R.color.gpx_track);
		_routePaint = createPaint(mapView.getDensity() * 3, routeColor);
		_routePaintSelected = createPaint(mapView.getDensity() * 7, routeColor);

		_paintText = new Paint();
		_paintText.setStyle(Paint.Style.FILL);
		_paintText.setTextAlign(Paint.Align.CENTER);
		_paintText.setColor(Color.BLACK);
		_paintText.setTextSize(_iconPoi.getHeight() / 3);

		_bitmapPaint = new Paint();
		_bitmapPaint.setDither(true);
		_bitmapPaint.setAntiAlias(true);
		_bitmapPaint.setFilterBitmap(true);

		_route = new Path();
		_track = new Path();

		_boxSize = _iconPoi.getHeight() * 2; //twice icon size
	}

	private Paint createPaint(float width, int colour) {
		final Paint paint = new Paint();

		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(width);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setColor(colour);

		return paint;
	}

	private PointF getIconLocation(PointF touchPoint) {
		return new PointF(touchPoint.x - _iconPoi.getWidth() / 2, touchPoint.y - _iconPoi.getHeight());
	}

	private PointF getPointLocation(PointF touchPoint) {
		return new PointF(touchPoint.x, touchPoint.y);
	}

	private PointF getCenterLocation(PointF touchPoint) {
		return new PointF(touchPoint.x, touchPoint.y - _iconPoi.getHeight() + 2);
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		switch (_states.getMode()) {
			case EDIT_POI:
				addPoi(point, tileBox);
				return true;
			case EDIT_ROUTE:
				addRoutePoint(point, tileBox);
				return true;
			case EDIT_TRACK:
				addTrackPoint(point, tileBox);
				return true;
			case DELETE:
				deletePoints(point, tileBox);
				return true;
			default:
				return false;
		}
	}

	private void deletePoints(PointF point, RotatedTileBox tileBox) {
		final GpxEditorModel state = _states.createNew();

		state.deletePois(collectWayPoints(state.getPois(), point, tileBox, _boxSize));
		state.deleteTrackPoints(collectWayPoints(state.getTrackPoints(), point, tileBox, _boxSize));
		state.deleteRoutePoints(collectWayPoints(state.getRoutePoints(), point, tileBox, _boxSize));
		_states.push(state);

		if (state.isEmpty()) {
			_widget.refreshDisplay(GpxEditorStates.Mode.EDIT_POI);
			_mapView.refreshMap();
		}
	}

	private void addPoi(final PointF point, RotatedTileBox tileBox) {
		final LatLon l = tileBox.getLatLonFromPixel(point.x, point.y);
		final GpxEditorModel state = _states.createNew();
		final GPXUtilities.WptPt wptPt = state.addPoi(l, getThresholdDistance(point, tileBox, l));
		if (wptPt == null) {
			_states.push(state);
			refreshView();
		}
		else
		{
			buildWaypointView(point, wptPt);
		}
	}

	private void addTrackPoint(PointF point, RotatedTileBox tileBox) {
		final LatLon l = tileBox.getLatLonFromPixel(point.x, point.y);
		final GpxEditorModel state = _states.createNew();

		final GPXUtilities.WptPt wptPt = state.addTrackPoint(l, getThresholdDistance(point, tileBox, l));
		if (wptPt == null) {
			_states.push(state);
			refreshView();
		}
		else
		{
			buildWaypointView(point, wptPt);
		}
	}

	private void addRoutePoint(PointF point, RotatedTileBox tileBox) {
		final LatLon l = tileBox.getLatLonFromPixel(point.x, point.y);
		final GpxEditorModel state = _states.createNew();

		final GPXUtilities.WptPt wptPt = state.addRoutePoint(l, getThresholdDistance(point, tileBox, l));
		if (wptPt == null) {
			_states.push(state);
			refreshView();
		}
		else
		{
			buildWaypointView(point, wptPt);
		}
	}

	private double getThresholdDistance(PointF point, RotatedTileBox tileBox, LatLon l) {
		return MapUtils.getDistance(l, tileBox.getLatLonFromPixel(point.x + _boxSize, point.y));
	}

	public void selectFavourite() {
		GpxEditorUtil.selectFavourite(_mapActivity, false, new CallbackWithObject<FavouritePoint>() {

			@Override
			public boolean processResult(final FavouritePoint favourite) {
				if (favourite != null) {
					final LatLon poi = new LatLon(favourite.getLatitude(), favourite.getLongitude());

					GpxEditorModel state =_states.createNew();
					switch (_states.getMode()) {
						case EDIT_POI:
							state.addPoi(poi, 0);
							break;
						case EDIT_TRACK:
							state.addTrackPoint(poi, 0);
							break;
						case EDIT_ROUTE:
							state.addRoutePoint(poi, 0);
							break;
						default:
							return false;
					}
					_states.push(state);
					_widget.showMap(state);
					refreshView();

					return true;
				}
				return false;
			}
		});
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		//edit properties
		final GpxEditorModel current = _states.current();
		if (current == null) return false;
		final List<GPXUtilities.WptPt> wptPts = collectWayPoints(current.all(), point, tileBox, _boxSize);
		if (wptPts.isEmpty()) {
			return false;
		} else {
			GPXUtilities.WptPt wayPoint = wptPts.get(0);
			buildWaypointEditor(wayPoint);
			return true;
		}
	}

	private void buildWaypointEditor(final GPXUtilities.WptPt waypoint) {
		final AlertDialog.Builder b = new AlertDialog.Builder(_mapActivity);
		final LinearLayout ll = (LinearLayout) LayoutInflater.from(_mapActivity).inflate(R.layout.gpx_edit_waypoint, null);

		final EditText waypointName = (EditText) ll.findViewById(R.id.gpx_edit_waypoint_name);
		final EditText waypointDescription = (EditText) ll.findViewById(R.id.gpx_edit_waypoint_description);

		waypointName.setText(waypoint.name);
		waypointDescription.setText(waypoint.desc);

		b.setView(ll);
		b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				waypoint.name = waypointName.getText().toString();
				waypoint.desc = waypointDescription.getText().toString();
			}
		});
		b.setNegativeButton(R.string.default_buttons_cancel, null);
		b.show();
	}

	private void buildWaypointView(final PointF origin, final GPXUtilities.WptPt waypoint) {
		final LinearLayout ll = (LinearLayout) LayoutInflater.from(_mapActivity).inflate(R.layout.gpx_edit_view_waypoint, (ViewGroup) _mapActivity.findViewById(R.id.gpx_edit_view_waypoint));
		final AlertDialog ad = new AlertDialog.Builder(_mapActivity).setView(ll).create();

		ll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				_mapActivity.getMapActions().contextMenuPoint(waypoint.lat, waypoint.lon);
				ad.dismiss();
			}
		});
		ll.setClickable(true);
		((TextView) ll.findViewById(R.id.gpx_edit_waypoint_name)).setText(waypoint.name);
		((TextView) ll.findViewById(R.id.gpx_edit_waypoint_description)).setText(waypoint.desc);

		final Window adWindow = ad.getWindow();
		adWindow.setGravity(Gravity.TOP| Gravity.LEFT);

		final WindowManager.LayoutParams params = adWindow.getAttributes();

		final PointF position = getPointLocation(origin);

		params.x = (int) position.x;
		params.y = (int) position.y;

		adWindow.setAttributes(params);

		ad.show();
	}

	private List<GPXUtilities.WptPt> _startPoints = new ArrayList<GPXUtilities.WptPt>();

	private boolean isScrolling() {
		return !_startPoints.isEmpty();
	}

	private void startScrolling(MotionEvent e1, RotatedTileBox tileBox) {
		final GpxEditorModel current = _states.current();
		if (current == null) return;

		final GpxEditorModel startState = current.copy();
		final List<GPXUtilities.WptPt> wayPoints;
		switch (_states.getMode()) {
			case EDIT_ROUTE:
				wayPoints = new ArrayList<GPXUtilities.WptPt>(startState.getRoutePoints());
				break;
			case EDIT_TRACK:
				wayPoints = new ArrayList<GPXUtilities.WptPt>(startState.getTrackPoints());
				break;
			case EDIT_POI:
				wayPoints = new ArrayList<GPXUtilities.WptPt>(startState.getPois());
				break;
			default:
				return;
		}

		final PointF start = new PointF(e1.getX(), e1.getY());
		final List<GPXUtilities.WptPt> points = collectWayPoints(wayPoints, start, tileBox, _boxSize);

		if (!points.isEmpty()) {
			_states.push(startState);
			_startPoints.addAll(points);
		}
	}

	private void continueScrolling(MotionEvent e2, float distanceX, float distanceY, RotatedTileBox tileBox) {
		PointF start = new PointF(e2.getX(), e2.getY());
		PointF end = new PointF(e2.getX() - distanceX, e2.getY() - distanceY);
		_states.current().movePoints(_startPoints, getDelta(tileBox, start, end));
		refreshView();
	}

	private void stopScrolling() {
		_startPoints.clear();
	}

	private void refreshView() {
		_widget.refreshDisplay(_states.getMode());
		_mapView.refreshMap();
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		final RotatedTileBox tileBox = _mapView.getCurrentRotatedTileBox();
		if (isScrolling()) {
			continueScrolling(e2, distanceX, distanceY, tileBox);
		} else {
			startScrolling(e1, tileBox);
		}
		return isScrolling();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		if (onTouchEvent(event, _mapView.getCurrentRotatedTileBox())) {
			return true;
		}

		if (isScrolling() && event.getAction() == MotionEvent.ACTION_UP) {
			stopScrolling();
			return true;
		}

		return false;
	}

	private LatLon getDelta(RotatedTileBox tileBox, PointF start, PointF end) {
		LatLon a = tileBox.getLatLonFromPixel(start.x, start.y);
		LatLon b = tileBox.getLatLonFromPixel(end.x, end.y);
		return new LatLon(b.getLatitude() - a.getLatitude(), b.getLongitude() - a.getLongitude());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		final GpxEditorModel state = _states.current();
		if (state == null) return;

		final GpxEditorStates.Mode mode = _states.getMode();

		drawPath(_track, mode == GpxEditorStates.Mode.EDIT_TRACK ? _trackPaintSelected : _trackPaint, canvas, tileBox, state.getTrackPoints().iterator());
		drawPath(_route, mode == GpxEditorStates.Mode.EDIT_ROUTE ? _routePaintSelected : _routePaint, canvas, tileBox, state.getRoutePoints().iterator());

		drawPoints(_iconTrack, canvas, tileBox, state.getTrackPoints(), true);
		drawPoints(_iconRoute, canvas, tileBox, state.getRoutePoints(), true);
		drawPoints(_iconPoi, canvas, tileBox, state.getPois(), false);
	}

	private void drawPoints(Bitmap icon, Canvas canvas, RotatedTileBox tileBox, List<GPXUtilities.WptPt> points, boolean withNumbers) {
		for (int i = 0; i < points.size(); i++) {
			GPXUtilities.WptPt wptPt = points.get(i);
			if (tileBox.containsLatLon(wptPt.lat, wptPt.lon)) {
				final PointF pointOrigin = toPoint(tileBox, wptPt);
				final PointF iconOrigin = getIconLocation(pointOrigin);
				final PointF centerOrigin = getCenterLocation(pointOrigin);

				canvas.rotate(-_mapView.getRotate(), pointOrigin.x, pointOrigin.y);
				canvas.drawBitmap(icon, iconOrigin.x, iconOrigin.y, _bitmapPaint);
				if (withNumbers) {
					canvas.drawText("" + (i + 1), centerOrigin.x, centerOrigin.y, _paintText);
				}
				canvas.rotate(_mapView.getRotate(), pointOrigin.x, pointOrigin.y);
			}
		}
	}

	private void drawPath(Path path, Paint paint, Canvas canvas, RotatedTileBox tileBox, Iterator<GPXUtilities.WptPt> wptPtIterator) {
		path.reset();
		if (wptPtIterator.hasNext()) {
			PointF first = getPointLocation(toPoint(tileBox, wptPtIterator.next()));
			path.moveTo(first.x, first.y);
			while (wptPtIterator.hasNext()) {
				PointF next = getPointLocation(toPoint(tileBox, wptPtIterator.next()));
				path.lineTo(next.x, next.y);
			}
		}
		canvas.drawPath(path, paint);
	}

	private PointF toPoint(RotatedTileBox tileBox, double lat, double lon) {
		int locationX = tileBox.getPixXFromLonNoRot(lon);
		int locationY = tileBox.getPixYFromLatNoRot(lat);
		return new PointF(locationX, locationY);
	}

	private PointF toPoint(RotatedTileBox tileBox, GPXUtilities.WptPt wptPt) {
		return toPoint(tileBox, wptPt.lat, wptPt.lon);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {

	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	/**
	 * Collect all waypoints sufficiently close to the given point, within the given tileBox.
	 *
	 * @param wayPoints  All waypoints to check
	 * @param touchPoint The point in which neighbourhood to check
	 * @param tileBox    The bounding box
	 * @param box        The radius centered around point, within with each waypoint will be collected
	 * @return The list of matching waypoints
	 */
	private List<GPXUtilities.WptPt> collectWayPoints(final List<GPXUtilities.WptPt> wayPoints, final PointF touchPoint, final RotatedTileBox tileBox, final int box) {
		final List<GPXUtilities.WptPt> collected = new ArrayList<GPXUtilities.WptPt>();
		final PointF pointOrigin = getPointLocation(touchPoint);
		final int ex = (int) pointOrigin.x;
		final int ey = (int) pointOrigin.y;
		for (final GPXUtilities.WptPt wayPoint : wayPoints) {
			int x = (int) tileBox.getPixXFromLatLon(wayPoint.lat, wayPoint.lon);
			int y = (int) tileBox.getPixYFromLatLon(wayPoint.lat, wayPoint.lon);
			if (within(ex, ey, x, y, box / 2)) {
				collected.add(wayPoint);
			}
		}
		return collected;
	}

	private boolean within(int ex, int ey, int objx, int objy, int radius) {
		final boolean within = Math.abs(objx - ex) < radius && Math.abs(objy - ey) < radius;
		return within;
	}
}
