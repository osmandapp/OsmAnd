package net.osmand.plus.track.helpers;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class GpxUtils {

	@Nullable
	public static Pair<WptPt, WptPt> findLineNearPoint(@NonNull RotatedTileBox tb,
	                                                   @NonNull List<WptPt> points,
	                                                   int r, int mx, int my) {
		if (Algorithms.isEmpty(points)) {
			return null;
		}
		WptPt prevPoint = points.get(0);
		int ppx = (int) tb.getPixXFromLatLon(prevPoint.lat, prevPoint.lon);
		int ppy = (int) tb.getPixYFromLatLon(prevPoint.lat, prevPoint.lon);
		int pcross = placeInBbox(ppx, ppy, mx, my, r, r);

		for (int i = 1; i < points.size(); i++) {
			WptPt point = points.get(i);
			int px = (int) tb.getPixXFromLatLon(point.lat, point.lon);
			int py = (int) tb.getPixYFromLatLon(point.lat, point.lon);
			int cross = placeInBbox(px, py, mx, my, r, r);
			if (cross == 0) {
				return new Pair<>(prevPoint, point);
			}
			if ((pcross & cross) == 0) {
				int mpx = px;
				int mpy = py;
				int mcross = cross;
				while (Math.abs(mpx - ppx) > r || Math.abs(mpy - ppy) > r) {
					int mpxnew = mpx / 2 + ppx / 2;
					int mpynew = mpy / 2 + ppy / 2;
					int mcrossnew = placeInBbox(mpxnew, mpynew, mx, my, r, r);
					if (mcrossnew == 0) {
						return new Pair<>(prevPoint, point);
					}
					if ((mcrossnew & mcross) != 0) {
						mpx = mpxnew;
						mpy = mpynew;
						mcross = mcrossnew;
					} else if ((mcrossnew & pcross) != 0) {
						ppx = mpxnew;
						ppy = mpynew;
						pcross = mcrossnew;
					} else {
						// this should never happen theoretically
						break;
					}
				}
			}
			pcross = cross;
			ppx = px;
			ppy = py;
			prevPoint = point;
		}
		return null;
	}

	@Nullable
	public static Pair<WptPt, WptPt> findLineInPolygon31(@NonNull List<PointI> polygon31,
	                                                     @NonNull List<WptPt> points) {
		if (points.size() < 2) {
			return null;
		}

		WptPt firstPoint = points.get(0);
		PointI previousPoint31 = NativeUtilities.getPoint31FromLatLon(firstPoint.lat, firstPoint.lon);

		if (NativeUtilities.isPointInsidePolygon(previousPoint31, polygon31)) {
			WptPt secondPoint = points.get(1);
			return Pair.create(firstPoint, secondPoint);
		}

		for (int i = 1; i < points.size(); i++) {
			WptPt currentPoint = points.get(i);
			PointI currentPoint31 = NativeUtilities.getPoint31FromLatLon(currentPoint.lat, currentPoint.lon);

			boolean lineInside = NativeUtilities.isPointInsidePolygon(currentPoint31, polygon31)
					|| NativeUtilities.isSegmentCrossingPolygon(previousPoint31, currentPoint31, polygon31);
			if (lineInside) {
				WptPt previousPoint = points.get(i - 1);
				return new Pair<>(previousPoint, currentPoint);
			}

			previousPoint31 = currentPoint31;
		}

		return null;
	}

	@NonNull
	public static SelectedGpxPoint createSelectedGpxPoint(SelectedGpxFile selectedGpxFile, WptPt prevPoint,
	                                                      WptPt nextPoint, LatLon latLon, boolean showTrackPointMenu) {
		WptPt projectionPoint = createProjectionPoint(prevPoint, nextPoint, latLon);

		Location prevPointLocation = new Location("");
		prevPointLocation.setLatitude(prevPoint.lat);
		prevPointLocation.setLongitude(prevPoint.lon);

		Location nextPointLocation = new Location("");
		nextPointLocation.setLatitude(nextPoint.lat);
		nextPointLocation.setLongitude(nextPoint.lon);

		float bearing = prevPointLocation.bearingTo(nextPointLocation);

		return new SelectedGpxPoint(selectedGpxFile, projectionPoint, prevPoint, nextPoint, bearing,
				showTrackPointMenu);
	}

	public static WptPt createProjectionPoint(WptPt prevPoint, WptPt nextPoint, LatLon latLon) {
		LatLon projection = MapUtils.getProjection(latLon.getLatitude(), latLon.getLongitude(), prevPoint.lat, prevPoint.lon, nextPoint.lat, nextPoint.lon);

		WptPt projectionPoint = new WptPt();
		projectionPoint.lat = projection.getLatitude();
		projectionPoint.lon = projection.getLongitude();
		projectionPoint.heading = prevPoint.heading;
		projectionPoint.distance = prevPoint.distance + MapUtils.getDistance(projection, prevPoint.lat, prevPoint.lon);
		projectionPoint.ele = getValueByDistInterpolation(projectionPoint.distance, prevPoint.distance, prevPoint.ele, nextPoint.distance, nextPoint.ele);
		projectionPoint.speed = getValueByDistInterpolation(projectionPoint.distance, prevPoint.distance, prevPoint.speed, nextPoint.distance, nextPoint.speed);
		if (prevPoint.time != 0 && nextPoint.time != 0) {
			projectionPoint.time = (long) getValueByDistInterpolation(projectionPoint.distance, prevPoint.distance, prevPoint.time, nextPoint.distance, nextPoint.time);
		}

		return projectionPoint;
	}

	private static double getValueByDistInterpolation(double projectionDist, double prevDist, double prevVal, double nextDist, double nextVal) {
		return prevVal + (projectionDist - prevDist) * ((nextVal - prevVal) / (nextDist - prevDist));
	}

	private static int placeInBbox(int x, int y, int mx, int my, int halfw, int halfh) {
		int cross = 0;
		cross |= (x < mx - halfw ? 1 : 0);
		cross |= (x > mx + halfw ? 2 : 0);
		cross |= (y < my - halfh ? 4 : 0);
		cross |= (y > my + halfh ? 8 : 0);
		return cross;
	}

	@Nullable
	public static WptPt getSegmentPointByTime(@NonNull TrkSegment segment, @NonNull GPXFile gpxFile,
	                                          float time, boolean preciseLocation, boolean joinSegments) {
		if (!segment.generalSegment || joinSegments) {
			return getSegmentPointByTime(segment, time, 0, preciseLocation);
		}

		long passedSegmentsTime = 0;
		for (Track track : gpxFile.tracks) {
			if (track.generalTrack) {
				continue;
			}

			for (TrkSegment seg : track.segments) {
				WptPt point = getSegmentPointByTime(seg, time, passedSegmentsTime, preciseLocation);
				if (point != null) {
					return point;
				}

				long segmentStartTime = Algorithms.isEmpty(seg.points) ? 0 : seg.points.get(0).time;
				long segmentEndTime = Algorithms.isEmpty(seg.points) ?
						0 : seg.points.get(seg.points.size() - 1).time;
				passedSegmentsTime += segmentEndTime - segmentStartTime;
			}
		}

		return null;
	}

	@Nullable
	private static WptPt getSegmentPointByTime(@NonNull TrkSegment segment, float timeToPoint,
	                                           long passedSegmentsTime, boolean preciseLocation) {
		WptPt previousPoint = null;
		long segmentStartTime = segment.points.get(0).time;
		for (WptPt currentPoint : segment.points) {
			long totalPassedTime = passedSegmentsTime + currentPoint.time - segmentStartTime;
			if (totalPassedTime >= timeToPoint) {
				return preciseLocation && previousPoint != null
						? getIntermediatePointByTime(totalPassedTime, timeToPoint, previousPoint, currentPoint)
						: currentPoint;
			}
			previousPoint = currentPoint;
		}
		return null;
	}

	@NonNull
	private static WptPt getIntermediatePointByTime(double passedTime, double timeToPoint,
	                                                WptPt prevPoint, WptPt currPoint) {
		double percent = 1 - (passedTime - timeToPoint) / (currPoint.time - prevPoint.time);
		double dLat = (currPoint.lat - prevPoint.lat) * percent;
		double dLon = (currPoint.lon - prevPoint.lon) * percent;
		WptPt intermediatePoint = new WptPt();
		intermediatePoint.lat = prevPoint.lat + dLat;
		intermediatePoint.lon = prevPoint.lon + dLon;
		return intermediatePoint;
	}

	@Nullable
	public static WptPt getSegmentPointByDistance(@NonNull TrkSegment segment, @NonNull GPXFile gpxFile,
	                                              float distanceToPoint, boolean preciseLocation,
	                                              boolean joinSegments) {
		double passedDistance = 0;
		if (!segment.generalSegment || joinSegments) {
			WptPt prevPoint = null;
			for (int i = 0; i < segment.points.size(); i++) {
				WptPt currPoint = segment.points.get(i);
				if (prevPoint != null) {
					passedDistance += MapUtils.getDistance(prevPoint.lat, prevPoint.lon, currPoint.lat, currPoint.lon);
				}
				if (currPoint.distance >= distanceToPoint || Math.abs(passedDistance - distanceToPoint) < 0.1) {
					return preciseLocation && prevPoint != null && currPoint.distance >= distanceToPoint
							? getIntermediatePointByDistance(passedDistance, distanceToPoint, currPoint, prevPoint)
							: currPoint;
				}
				prevPoint = currPoint;
			}
		}

		passedDistance = 0;
		double passedSegmentsPointsDistance = 0;
		WptPt prevPoint = null;
		for (Track track : gpxFile.tracks) {
			if (track.generalTrack) {
				continue;
			}
			for (TrkSegment seg : track.segments) {
				if (Algorithms.isEmpty(seg.points)) {
					continue;
				}
				for (WptPt currPoint : seg.points) {
					if (prevPoint != null) {
						passedDistance += MapUtils.getDistance(prevPoint.lat, prevPoint.lon,
								currPoint.lat, currPoint.lon);
					}
					if (passedSegmentsPointsDistance + currPoint.distance >= distanceToPoint
							|| Math.abs(passedDistance - distanceToPoint) < 0.1) {
						return preciseLocation && prevPoint != null
								&& currPoint.distance + passedSegmentsPointsDistance >= distanceToPoint
								? getIntermediatePointByDistance(passedDistance, distanceToPoint, currPoint, prevPoint)
								: currPoint;
					}
					prevPoint = currPoint;
				}
				prevPoint = null;
				passedSegmentsPointsDistance += seg.points.get(seg.points.size() - 1).distance;
			}
		}
		return null;
	}

	@NonNull
	private static WptPt getIntermediatePointByDistance(double passedDistance, double distanceToPoint,
	                                                    WptPt currPoint, WptPt prevPoint) {
		double percent = 1 - (passedDistance - distanceToPoint) / (currPoint.distance - prevPoint.distance);
		double dLat = (currPoint.lat - prevPoint.lat) * percent;
		double dLon = (currPoint.lon - prevPoint.lon) * percent;
		WptPt intermediatePoint = new WptPt();
		intermediatePoint.lat = prevPoint.lat + dLat;
		intermediatePoint.lon = prevPoint.lon + dLon;
		return intermediatePoint;
	}
}
