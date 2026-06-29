/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link SegmentData} lists from calculated routes and compares energy costs.
 */
public class EnergyRouteHelper {

	private static final Log LOG = PlatformUtil.getLog(EnergyRouteHelper.class);

	/** Minimum relative energy saving to suggest an alternative route (5 %). */
	public static final double MIN_ENERGY_SAVING_FRACTION = 0.05;

	/** Maximum relative distance increase for an acceptable alternative (20 %). */
	public static final double MAX_DISTANCE_INCREASE_FRACTION = 0.20;

	/** Fallback speed when a segment reports no speed (30 km/h in m/s). */
	private static final double DEFAULT_SPEED_MS = 30.0 / 3.6;

	/**
	 * Maximum number of segments passed to {@link EnergyModel#routeCost} for long routes.
	 * Additional router segments are merged into equal-distance buckets so SRTM lookups stay
	 * bounded on multi-hour drives.
	 */
	@VisibleForTesting
	static final int MAX_ENERGY_SAMPLE_SEGMENTS = 256;

	private EnergyRouteHelper() {
	}

	@VisibleForTesting
	static int maxEnergySampleSegments() {
		return MAX_ENERGY_SAMPLE_SEGMENTS;
	}

	@VisibleForTesting
	static boolean usesSegmentSampling(int segmentCount) {
		return segmentCount > MAX_ENERGY_SAMPLE_SEGMENTS;
	}

	/**
	 * Sums segment energy cost over explicit route segments.
	 */
	public static double routeCostFromSegments(@NonNull List<RouteSegmentResult> segments,
			@NonNull SRTMElevationProvider elevationProvider, @NonNull EnergyParams params) {
		return new EnergyModel().routeCost(buildSegmentsFromRoute(segments, elevationProvider), params);
	}

	/**
	 * Builds segment data from a raw segment list (e.g. junction alternative), sampling when the
	 * router returns more than {@link #MAX_ENERGY_SAMPLE_SEGMENTS} pieces.
	 */
	@NonNull
	public static List<SegmentData> buildSegmentsFromRoute(@NonNull List<RouteSegmentResult> segments,
			@NonNull SRTMElevationProvider elevationProvider) {
		if (segments.isEmpty()) {
			return new ArrayList<>();
		}
		ElevationReader reader = latLon -> elevationProvider.getElevation(latLon.getLatitude(), latLon.getLongitude());
		if (usesSegmentSampling(segments.size())) {
			LOG.info("DriverBreak: sampling " + segments.size() + " route segments down to "
					+ MAX_ENERGY_SAMPLE_SEGMENTS + " for energy analysis");
			return buildDistanceBucketedSegments(segments, reader, MAX_ENERGY_SAMPLE_SEGMENTS);
		}
		return buildAllSegments(segments, reader);
	}

	/**
	 * Total distance in metres for a raw segment list.
	 */
	public static double segmentListDistanceM(@NonNull List<RouteSegmentResult> segments) {
		double total = 0.0;
		for (RouteSegmentResult segment : segments) {
			total += segment.getDistance();
		}
		return total;
	}

	@NonNull
	public static List<SegmentData> buildSegments(@NonNull RouteCalculationResult route,
			@NonNull SRTMElevationProvider elevationProvider) {
		return buildSegmentsFromRoute(route.getOriginalRoute(), elevationProvider);
	}

	/**
	 * Computes total route distance in metres from segment lengths.
	 */
	public static double routeDistanceM(@NonNull List<SegmentData> segments) {
		double total = 0.0;
		for (SegmentData segment : segments) {
			total += segment.getDistanceM();
		}
		return total;
	}

	/**
	 * Returns true when {@code candidate} uses at least 5 % less energy than {@code baseline}
	 * while being at most 20 % longer by distance.
	 */
	public static boolean isCheaperAlternative(double baselineEnergyJ, double candidateEnergyJ,
			double baselineDistanceM, double candidateDistanceM) {
		if (baselineEnergyJ <= 0.0 || candidateEnergyJ <= 0.0 || baselineDistanceM <= 0.0) {
			return false;
		}
		double energySaving = (baselineEnergyJ - candidateEnergyJ) / baselineEnergyJ;
		double distanceIncrease = (candidateDistanceM - baselineDistanceM) / baselineDistanceM;
		return energySaving >= MIN_ENERGY_SAVING_FRACTION
				&& distanceIncrease <= MAX_DISTANCE_INCREASE_FRACTION;
	}

	@NonNull
	private static List<SegmentData> buildAllSegments(@NonNull List<RouteSegmentResult> segments,
			@NonNull ElevationReader reader) {
		List<SegmentData> result = new ArrayList<>(segments.size());
		for (RouteSegmentResult segment : segments) {
			SegmentData data = segmentFromResult(segment, reader);
			if (data != null) {
				result.add(data);
			}
		}
		return result;
	}

	@NonNull
	private static List<SegmentData> buildDistanceBucketedSegments(@NonNull List<RouteSegmentResult> segments,
			@NonNull ElevationReader reader, int maxBuckets) {
		double totalDistanceM = segmentListDistanceM(segments);
		if (totalDistanceM <= 0.0 || maxBuckets <= 0) {
			return new ArrayList<>();
		}
		double bucketTargetM = totalDistanceM / maxBuckets;
		List<SegmentData> result = new ArrayList<>(maxBuckets);
		DistanceBucket bucket = new DistanceBucket();

		for (RouteSegmentResult segment : segments) {
			double distanceM = segment.getDistance();
			if (distanceM <= 0.0) {
				continue;
			}
			double speedMs = segmentSpeedMs(segment);
			LatLon segStart = segmentStart(segment);
			LatLon segEnd = segmentEnd(segment);
			if (bucket.isEmpty()) {
				bucket.start = segStart;
			}
			if (segEnd != null) {
				bucket.end = segEnd;
			}
			bucket.distanceM += distanceM;
			bucket.speedDistanceSum += speedMs * distanceM;

			if (bucket.distanceM >= bucketTargetM && result.size() < maxBuckets - 1) {
				flushBucket(result, bucket, reader);
				bucket.reset();
			}
		}
		if (!bucket.isEmpty()) {
			flushBucket(result, bucket, reader);
		}
		return result;
	}

	private static void flushBucket(@NonNull List<SegmentData> result, @NonNull DistanceBucket bucket,
			@NonNull ElevationReader reader) {
		if (bucket.distanceM <= 0.0) {
			return;
		}
		double speedMs = bucket.speedDistanceSum / bucket.distanceM;
		double deltaH = 0.0;
		double elevationM = 0.0;
		if (bucket.start != null && bucket.end != null) {
			deltaH = elevationDelta(bucket.start, bucket.end, reader);
			elevationM = meanElevation(bucket.start, bucket.end, reader);
		}
		result.add(SegmentData.of(bucket.distanceM, deltaH, elevationM, speedMs));
	}

	@Nullable
	private static SegmentData segmentFromResult(@NonNull RouteSegmentResult segment,
			@NonNull ElevationReader reader) {
		double distanceM = segment.getDistance();
		if (distanceM <= 0.0) {
			return null;
		}
		LatLon start = segmentStart(segment);
		LatLon end = segmentEnd(segment);
		double deltaH = 0.0;
		double elevationM = 0.0;
		if (start != null && end != null) {
			deltaH = elevationDelta(start, end, reader);
			elevationM = meanElevation(start, end, reader);
		}
		return SegmentData.of(distanceM, deltaH, elevationM, segmentSpeedMs(segment));
	}

	private static double segmentSpeedMs(@NonNull RouteSegmentResult segment) {
		double speedMs = segment.getSegmentSpeed();
		return speedMs > 0.0 ? speedMs : DEFAULT_SPEED_MS;
	}

	@Nullable
	private static LatLon segmentStart(@NonNull RouteSegmentResult segment) {
		LatLon start = segment.getStartPoint();
		if (start != null) {
			return start;
		}
		try {
			return segment.getPoint(segment.getStartPointIndex());
		} catch (RuntimeException e) {
			return null;
		}
	}

	@Nullable
	private static LatLon segmentEnd(@NonNull RouteSegmentResult segment) {
		LatLon end = segment.getEndPoint();
		if (end != null) {
			return end;
		}
		try {
			return segment.getPoint(segment.getEndPointIndex());
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static double elevationDelta(@NonNull LatLon start, @NonNull LatLon end,
			@NonNull ElevationReader reader) {
		int startElev = reader.getElevation(start);
		int endElev = reader.getElevation(end);
		if (startElev == SRTMElevationProvider.VOID_ELEVATION
				|| endElev == SRTMElevationProvider.VOID_ELEVATION) {
			return 0.0;
		}
		return endElev - startElev;
	}

	private static double meanElevation(@NonNull LatLon start, @NonNull LatLon end,
			@NonNull ElevationReader reader) {
		int startElev = reader.getElevation(start);
		int endElev = reader.getElevation(end);
		if (startElev == SRTMElevationProvider.VOID_ELEVATION && endElev == SRTMElevationProvider.VOID_ELEVATION) {
			return 0.0;
		}
		if (startElev == SRTMElevationProvider.VOID_ELEVATION) {
			return endElev;
		}
		if (endElev == SRTMElevationProvider.VOID_ELEVATION) {
			return startElev;
		}
		return (startElev + endElev) / 2.0;
	}

	private interface ElevationReader {
		int getElevation(@NonNull LatLon latLon);
	}

	private static final class DistanceBucket {
		double distanceM;
		double speedDistanceSum;
		@Nullable
		LatLon start;
		@Nullable
		LatLon end;

		boolean isEmpty() {
			return distanceM <= 0.0;
		}

		void reset() {
			distanceM = 0.0;
			speedDistanceSum = 0.0;
			start = null;
			end = null;
		}
	}
}
