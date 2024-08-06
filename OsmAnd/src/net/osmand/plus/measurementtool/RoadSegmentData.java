package net.osmand.plus.measurementtool;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.MapUtils;

import java.util.Collections;
import java.util.List;

public class RoadSegmentData {
	private final ApplicationMode appMode;
	private final WptPt start;
	private final WptPt end;
	private final List<WptPt> points;
	private final List<RouteSegmentResult> segments;
	private final double distance;

	public RoadSegmentData(@NonNull ApplicationMode appMode, @NonNull WptPt start, @NonNull WptPt end,
						   @Nullable List<WptPt> points, @Nullable List<RouteSegmentResult> segments) {
		this.appMode = appMode;
		this.start = start;
		this.end = end;
		this.points = points;
		this.segments = segments;
		double distance = 0;
		if (points != null && points.size() > 1) {
			for (int i = 1; i < points.size(); i++) {
				distance += MapUtils.getDistance(points.get(i - 1).getLat(), points.get(i - 1).getLon(),
						points.get(i).getLat(), points.get(i).getLon());
			}
		} else if (segments != null) {
			for (RouteSegmentResult segment : segments) {
				distance += segment.getDistance();
			}
		}
		this.distance = distance;
	}

	public ApplicationMode getAppMode() {
		return appMode;
	}

	public WptPt getStart() {
		return start;
	}

	public WptPt getEnd() {
		return end;
	}

	@Nullable
	public List<WptPt> getPoints() {
		return points != null ? Collections.unmodifiableList(points) : null;
	}

	@Nullable
	public List<RouteSegmentResult> getSegments() {
		return segments != null ? Collections.unmodifiableList(segments) : null;
	}

	public double getDistance() {
		return distance;
	}
}
