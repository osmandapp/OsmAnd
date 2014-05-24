package net.osmand.plus.gpxedit;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.plus.GPXUtilities;
import net.osmand.util.MapUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Koen Rabaey
 */
public class GpxEditorModel {
	private List<GPXUtilities.WptPt> _pois = new LinkedList<GPXUtilities.WptPt>();
	private List<GPXUtilities.WptPt> _routePoints = new LinkedList<GPXUtilities.WptPt>();
	private List<GPXUtilities.WptPt> _trackPoints = new LinkedList<GPXUtilities.WptPt>();

	public GpxEditorModel() {
	}

	public GpxEditorModel(GpxEditorModel state) {
		_pois.addAll(state._pois);
		_routePoints.addAll(state._routePoints);
		_trackPoints.addAll(state._trackPoints);
	}

	public GpxEditorModel copy() {
		GpxEditorModel copy = new GpxEditorModel();
		for (final GPXUtilities.WptPt poi : _pois) {
			copy._pois.add(poi.copy());
		}
		for (final GPXUtilities.WptPt routePoint : _routePoints) {
			copy._routePoints.add(routePoint.copy());
		}
		for (final GPXUtilities.WptPt trackPoint : _trackPoints) {
			copy._trackPoints.add(trackPoint.copy());
		}
		return copy;
	}

	public List<GPXUtilities.WptPt> getTrackPoints() {
		return _trackPoints;
	}

	public List<GPXUtilities.WptPt> getRoutePoints() {
		return _routePoints;
	}

	public List<GPXUtilities.WptPt> getPois() {
		return _pois;
	}

	public List<GPXUtilities.WptPt> all() {
		final List<GPXUtilities.WptPt> all = new LinkedList<GPXUtilities.WptPt>(_pois);
		all.addAll(_routePoints);
		all.addAll(_trackPoints);
		return all;
	}

	public boolean isEmpty() {
		return _pois.isEmpty() && _routePoints.isEmpty() && _trackPoints.isEmpty();
	}

	public GPXUtilities.GPXFile asGpx(GPXUtilities.GPXFile from) {
		GPXUtilities.GPXFile gpx;
		if (from != null) {
			gpx = from;
			gpx.tracks.clear();
			gpx.routes.clear();
			gpx.points.clear();
		} else {
			gpx = new GPXUtilities.GPXFile();
		}
		//waypoints
		gpx.points.addAll(getPois());

		//trackpoints
		final GPXUtilities.Track track = new GPXUtilities.Track();
		final GPXUtilities.TrkSegment segment = new GPXUtilities.TrkSegment();
		segment.points.addAll(getTrackPoints());
		track.segments.add(segment);
		gpx.tracks.add(track);

		//routepoints
		final GPXUtilities.Route route = new GPXUtilities.Route();
		route.points.addAll(getRoutePoints());
		gpx.routes.add(route);

		return gpx;
	}

	public GPXUtilities.WptPt resetWith(GPXUtilities.GPXFile result) {
		GPXUtilities.WptPt show = null;
		for (GPXUtilities.Track t : result.tracks) {
			for (GPXUtilities.TrkSegment s : t.segments) {
				if (!s.points.isEmpty()) {
					show = s.points.get(0);
					_trackPoints.addAll(s.points);
					break;
				}
			}
			if (!_trackPoints.isEmpty()) break;
		}
		for (GPXUtilities.Route r : result.routes) {
			if (!r.points.isEmpty()) {
				show = r.points.get(0);
				_routePoints.addAll(r.points);
				break;
			}
		}
		if (!result.points.isEmpty()) {
			show = result.points.get(0);
			_pois.addAll(result.points);
		}

		return show;
	}

	public float getTrackDistance() {
		return getDistance(_trackPoints);
	}

	public float getRouteDistance() {
		return getDistance(_routePoints);
	}

	private float getDistance(final List<GPXUtilities.WptPt> wptPts) {
		float distance = 0.0F;

		if (wptPts.size() < 2) return distance;

		for (int i = 1; i < wptPts.size(); i++) {
			distance += MapUtils.getDistance(wptPts.get(i - 1).lat, wptPts.get(i - 1).lon, wptPts.get(i).lat, wptPts.get(i).lon);
		}

		return distance;
	}

	public GPXUtilities.WptPt addPoi(LatLon poi, double threshold) {
		final double lat = poi.getLatitude();
		final double lon = poi.getLongitude();

		//don' add if too close to another poi
		for (final GPXUtilities.WptPt wptPt : _pois) {
			double distance = MapUtils.getDistance(lat, lon, wptPt.lat, wptPt.lon);
			if (distance < threshold) {
				return wptPt;
			}
		}

		_pois.add(new GPXUtilities.WptPt(poi.getLatitude(), poi.getLongitude()));
		return null;
	}

	public QuadRect getBoundingBox() {

		final Iterator<GPXUtilities.WptPt> points = all().iterator();

		if (!points.hasNext()) {
			return null;
		}

		GPXUtilities.WptPt point = points.next();

		double left = point.lon;
		double right = point.lon;
		double top = point.lat;
		double bottom = point.lat;

		while (points.hasNext()) {
			point = points.next();

			left = Math.min(left, point.lon);
			right = Math.max(right, point.lon);
			top = Math.max(top, point.lat);
			bottom = Math.min(bottom, point.lat);
		}

		return new QuadRect(left, top, right, bottom);
	}

	public void deletePois(List<GPXUtilities.WptPt> pois) {
		_pois.removeAll(pois);
	}

	public GPXUtilities.WptPt addTrackPoint(LatLon l, double threshold) {
		return addPoint(l, _trackPoints, threshold);
	}

	public void deleteTrackPoints(List<GPXUtilities.WptPt> wptPts) {
		_trackPoints.removeAll(wptPts);
	}

	public GPXUtilities.WptPt addRoutePoint(LatLon l, double threshold) {
		return addPoint(l, _routePoints, threshold);
	}

	public void deleteRoutePoints(List<GPXUtilities.WptPt> wptPts) {
		_routePoints.removeAll(wptPts);
	}

	/**
	 * Add a point on a given list of points forming a path.
	 * If the distance to any point on the path is above threshold add it to the end
	 * IF the distance to any point is closer than threshold add it to the closest point below threshold, or on the
	 * orthogonal projection of the point on the path
	 *
	 * @param wptPt     The point to add
	 * @param wptPts    The path formed by given waypoints
	 * @param threshold The threshold to snap to the path
	 * @return true if the point was added
	 */
	private GPXUtilities.WptPt addPoint(LatLon wptPt, List<GPXUtilities.WptPt> wptPts, double threshold) {
		final double lat = wptPt.getLatitude();
		final double lon = wptPt.getLongitude();

		//don' add if too close to another one in the list
		for (final GPXUtilities.WptPt poi : wptPts) {
			double distance = MapUtils.getDistance(lat, lon, poi.lat, poi.lon);
			if (distance < threshold) {
				return poi;
			}
		}

		double ptMin = Double.MAX_VALUE;
		int ptIndex = 0;

		//find closest point
		for (int i = 0; i < all().size(); i++) {
			GPXUtilities.WptPt trackPoint = all().get(i);
			double distance = MapUtils.getDistance(lat, lon, trackPoint.lat, trackPoint.lon);
			if (distance < ptMin) {
				ptMin = distance;
				ptIndex = i;
			}
		}

		int lnIndex = 0;
		double lnMin = Double.MAX_VALUE;

		//find closest line
		for (int i = 0; i < wptPts.size() - 1; i++) {
			GPXUtilities.WptPt current = wptPts.get(i);
			GPXUtilities.WptPt next = wptPts.get(i + 1);
			double distance = MapUtils.getOrthogonalDistance(lat, lon, current.lat, current.lon, next.lat, next.lon);
			if (distance < ptMin) {
				lnMin = distance;
				lnIndex = i;
			}
		}

		if (lnMin < ptMin && lnMin < threshold) {
			//add on line (closest is line)
			final GPXUtilities.WptPt from = wptPts.get(lnIndex);
			final GPXUtilities.WptPt to = wptPts.get(lnIndex + 1);
			final LatLon projection = MapUtils.getProjection(lat, lon, from.lat, from.lon, to.lat, to.lon);
			wptPts.add(lnIndex + 1, new GPXUtilities.WptPt(projection.getLatitude(), projection.getLongitude()));
		} else if (ptMin < threshold) {
			//add point at end on top of other point (closest is point)
			wptPts.add(all().get(ptIndex));
		} else {
			//add point at end (no points/lines within threshold
			wptPts.add(new GPXUtilities.WptPt(lat, lon));
		}
		return null;
	}

	/**
	 * Move all points over the indicated delta
	 *
	 * @param wptPts The list of waypoints that will be moved
	 * @param delta  The distance over which to move
	 */
	public void movePoints(final List<GPXUtilities.WptPt> wptPts, final LatLon delta) {
		for (final GPXUtilities.WptPt from : wptPts) {
			from.lat = MapUtils.checkLatitude(from.lat + delta.getLatitude());
			from.lon = MapUtils.checkLongitude(from.lon + delta.getLongitude());
		}
	}
}
