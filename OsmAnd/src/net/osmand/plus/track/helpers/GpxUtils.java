package net.osmand.plus.track.helpers;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.Track;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
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
		int ppx = (int) tb.getPixXFromLatLon(prevPoint.getLat(), prevPoint.getLon());
		int ppy = (int) tb.getPixYFromLatLon(prevPoint.getLat(), prevPoint.getLon());
		int pcross = placeInBbox(ppx, ppy, mx, my, r, r);

		for (int i = 1; i < points.size(); i++) {
			WptPt point = points.get(i);
			int px = (int) tb.getPixXFromLatLon(point.getLat(), point.getLon());
			int py = (int) tb.getPixYFromLatLon(point.getLat(), point.getLon());
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
		PointI previousPoint31 = NativeUtilities.getPoint31FromLatLon(firstPoint.getLat(), firstPoint.getLon());

		if (NativeUtilities.isPointInsidePolygon(previousPoint31, polygon31)) {
			WptPt secondPoint = points.get(1);
			return Pair.create(firstPoint, secondPoint);
		}

		for (int i = 1; i < points.size(); i++) {
			WptPt currentPoint = points.get(i);
			PointI currentPoint31 = NativeUtilities.getPoint31FromLatLon(currentPoint.getLat(), currentPoint.getLon());

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
		prevPointLocation.setLatitude(prevPoint.getLatitude());
		prevPointLocation.setLongitude(prevPoint.getLongitude());

		Location nextPointLocation = new Location("");
		nextPointLocation.setLatitude(nextPoint.getLatitude());
		nextPointLocation.setLongitude(nextPoint.getLongitude());

		float bearing = prevPointLocation.bearingTo(nextPointLocation);

		return new SelectedGpxPoint(selectedGpxFile, projectionPoint, prevPoint, nextPoint, bearing,
				showTrackPointMenu);
	}

	public static WptPt createProjectionPoint(WptPt prevPoint, WptPt nextPoint, LatLon latLon) {
		LatLon projection = MapUtils.getProjection(latLon.getLatitude(), latLon.getLongitude(), prevPoint.getLat(), prevPoint.getLon(), nextPoint.getLat(), nextPoint.getLon());

		WptPt projectionPoint = new WptPt();
		projectionPoint.setLat(projection.getLatitude());
		projectionPoint.setLon(projection.getLongitude());
		projectionPoint.setHeading(prevPoint.getHeading());
		projectionPoint.setDistance(prevPoint.getDistance() + MapUtils.getDistance(projection, prevPoint.getLat(), prevPoint.getLon()));
		projectionPoint.setEle(getValueByDistInterpolation(projectionPoint.getDistance(), prevPoint.getDistance(), prevPoint.getEle(), nextPoint.getDistance(), nextPoint.getEle()));
		projectionPoint.setSpeed(getValueByDistInterpolation(projectionPoint.getDistance(), prevPoint.getDistance(), prevPoint.getSpeed(), nextPoint.getDistance(), nextPoint.getSpeed()));
		if (prevPoint.getTime() != 0 && nextPoint.getTime() != 0) {
			projectionPoint.setTime((long) getValueByDistInterpolation(projectionPoint.getDistance(), prevPoint.getDistance(), prevPoint.getTime(), nextPoint.getDistance(), nextPoint.getTime()));
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
	public static WptPt getSegmentPointByTime(@NonNull TrkSegment segment, @NonNull GpxFile gpxFile,
	                                          float time, boolean preciseLocation, boolean joinSegments) {
		if (!segment.getGeneralSegment() || joinSegments) {
			return getSegmentPointByTime(segment, time, 0, preciseLocation);
		}

		long passedSegmentsTime = 0;
		for (Track track : gpxFile.getTracks()) {
			if (track.isGeneralTrack()) {
				continue;
			}

			for (TrkSegment seg : track.getSegments()) {
				WptPt point = getSegmentPointByTime(seg, time, passedSegmentsTime, preciseLocation);
				if (point != null) {
					return point;
				}

				long segmentStartTime = Algorithms.isEmpty(seg.getPoints()) ? 0 : seg.getPoints().get(0).getTime();
				long segmentEndTime = Algorithms.isEmpty(seg.getPoints()) ?
						0 : seg.getPoints().get(seg.getPoints().size() - 1).getTime();
				passedSegmentsTime += segmentEndTime - segmentStartTime;
			}
		}

		return null;
	}

	@Nullable
	private static WptPt getSegmentPointByTime(@NonNull TrkSegment segment, float timeToPoint,
	                                           long passedSegmentsTime, boolean preciseLocation) {
		WptPt previousPoint = null;
		long segmentStartTime = segment.getPoints().get(0).getTime();
		for (WptPt currentPoint : segment.getPoints()) {
			long totalPassedTime = passedSegmentsTime + currentPoint.getTime() - segmentStartTime;
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
		double percent = 1 - (passedTime - timeToPoint) / (currPoint.getTime() - prevPoint.getTime());
		double dLat = (currPoint.getLat() - prevPoint.getLat()) * percent;
		double dLon = (currPoint.getLon() - prevPoint.getLon()) * percent;
		WptPt intermediatePoint = new WptPt();
		intermediatePoint.setLat(prevPoint.getLat() + dLat);
		intermediatePoint.setLon(prevPoint.getLon() + dLon);
		return intermediatePoint;
	}

	@Nullable
	public static WptPt getSegmentPointByDistance(@NonNull TrkSegment segment, @NonNull GpxFile gpxFile,
	                                              float distanceToPoint, boolean preciseLocation,
	                                              boolean joinSegments) {
		double passedDistance = 0;
		if (!segment.isGeneralSegment() || joinSegments) {
			WptPt prevPoint = null;
			for (int i = 0; i < segment.getPoints().size(); i++) {
				WptPt currPoint = segment.getPoints().get(i);
				if (prevPoint != null) {
					passedDistance += MapUtils.getDistance(prevPoint.getLat(), prevPoint.getLon(), currPoint.getLat(), currPoint.getLon());
				}
				if (currPoint.getDistance() >= distanceToPoint || Math.abs(passedDistance - distanceToPoint) < 0.1) {
					return preciseLocation && prevPoint != null && currPoint.getDistance() >= distanceToPoint
							? getIntermediatePointByDistance(passedDistance, distanceToPoint, currPoint, prevPoint)
							: currPoint;
				}
				prevPoint = currPoint;
			}
		}

		passedDistance = 0;
		double passedSegmentsPointsDistance = 0;
		WptPt prevPoint = null;
		for (Track track : gpxFile.getTracks()) {
			if (track.isGeneralTrack()) {
				continue;
			}
			for (TrkSegment seg : track.getSegments()) {
				if (Algorithms.isEmpty(seg.getPoints())) {
					continue;
				}
				for (WptPt currPoint : seg.getPoints()) {
					if (prevPoint != null) {
						passedDistance += MapUtils.getDistance(prevPoint.getLat(), prevPoint.getLon(),
								currPoint.getLat(), currPoint.getLon());
					}
					if (passedSegmentsPointsDistance + currPoint.getDistance() >= distanceToPoint
							|| Math.abs(passedDistance - distanceToPoint) < 0.1) {
						return preciseLocation && prevPoint != null
								&& currPoint.getDistance() + passedSegmentsPointsDistance >= distanceToPoint
								? getIntermediatePointByDistance(passedDistance, distanceToPoint, currPoint, prevPoint)
								: currPoint;
					}
					prevPoint = currPoint;
				}
				prevPoint = null;
				passedSegmentsPointsDistance += seg.getPoints().get(seg.getPoints().size() - 1).getDistance();
			}
		}
		return null;
	}

	@NonNull
	private static WptPt getIntermediatePointByDistance(double passedDistance, double distanceToPoint,
	                                                    WptPt currPoint, WptPt prevPoint) {
		double percent = 1 - (passedDistance - distanceToPoint) / (currPoint.getDistance() - prevPoint.getDistance());
		double dLat = (currPoint.getLat() - prevPoint.getLat()) * percent;
		double dLon = (currPoint.getLon() - prevPoint.getLon()) * percent;
		WptPt intermediatePoint = new WptPt();
		intermediatePoint.setLat(prevPoint.getLat() + dLat);
		intermediatePoint.setLon(prevPoint.getLon() + dLon);
		return intermediatePoint;
	}
}
