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

import net.osmand.router.RouteSegmentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates hiking and cycling routes against forbidden highways and priority infrastructure.
 * Port of driver_break_route_validator.c.
 */
public class RouteValidator {

	private static final String[] FORBIDDEN_HIGHWAYS = {
			"motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link"
	};

	private static final String[] HIKING_PRIORITY_PATHS = {
			"footway", "path", "track", "steps", "bridleway"
	};

	private static final String[] CYCLING_PATH_LIKE = {
			"path", "footway", "track", "living_street", "pedestrian", "service"
	};

	private static final String[] CYCLE_NETWORK_TAGS = {"ncn", "rcn", "lcn", "icn"};

	private static final int MTB_WARNING_THRESHOLD = 4;

	/**
	 * Validate a hiking route.
	 *
	 * @param route           route segments
	 * @param pilgrimageMode  when true, pilgrimage=yes segments count as priority
	 * @return validation summary; never null
	 */
	@NonNull
	public ValidationResult validateHiking(@NonNull List<RouteSegmentResult> route, boolean pilgrimageMode) {
		double total = 0.0;
		double priority = 0.0;
		double forbidden = 0.0;
		List<String> warnings = new ArrayList<>();

		for (RouteSegmentResult segment : route) {
			double length = segmentDistanceM(segment);
			if (length <= 0.0) {
				continue;
			}
			total += length;
			String highway = highwayType(segment);
			if (isForbiddenHighway(highway)) {
				forbidden += length;
			} else if (isHikingPriorityPath(highway)
					|| (pilgrimageMode && isPilgrimage(segment))) {
				priority += length;
			}
		}

		double forbiddenFraction = fraction(forbidden, total);
		double priorityFraction = fraction(priority, total);
		if (forbiddenFraction > 0.0) {
			warnings.add(String.format(Locale.US,
					"Route uses %.1f%% forbidden highways (motorways, trunk, primary roads)",
					forbiddenFraction * 100.0));
		}
		if (priorityFraction < 0.5 && forbiddenFraction == 0.0) {
			warnings.add(String.format(Locale.US,
					"Only %.1f%% of route uses priority paths (footways, paths, tracks)",
					priorityFraction * 100.0));
		}
		return new ValidationResult(forbiddenFraction, priorityFraction, -1, -1, warnings);
	}

	/**
	 * Validate a cycling route including MTB scale warnings.
	 *
	 * @param route           route segments
	 * @param pilgrimageMode  when true, pilgrimage=yes on secondary segments counts as priority
	 * @return validation summary; never null
	 */
	@NonNull
	public ValidationResult validateCycling(@NonNull List<RouteSegmentResult> route, boolean pilgrimageMode) {
		double total = 0.0;
		double priority = 0.0;
		double forbidden = 0.0;
		int maxMtb = -1;
		int maxMtbUphill = -1;
		List<String> warnings = new ArrayList<>();

		for (RouteSegmentResult segment : route) {
			double length = segmentDistanceM(segment);
			if (length <= 0.0) {
				continue;
			}
			total += length;
			if (isCyclingPrioritySegment(segment, pilgrimageMode)) {
				priority += length;
			} else {
				String highway = highwayType(segment);
				if (isForbiddenHighway(highway)) {
					forbidden += length;
				} else if (pilgrimageMode && isPilgrimage(segment)) {
					priority += length;
				}
			}
			maxMtb = Math.max(maxMtb, parseMtbScaleDigit(tagValue(segment, "mtb:scale")));
			maxMtbUphill = Math.max(maxMtbUphill, parseMtbScaleDigit(tagValue(segment, "mtb:scale:uphill")));
		}

		double forbiddenFraction = fraction(forbidden, total);
		double priorityFraction = fraction(priority, total);
		if (forbiddenFraction > 0.0) {
			warnings.add(String.format(Locale.US,
					"Route uses %.1f%% of distance on highways unsuitable for cycling "
							+ "(motorway, trunk, primary and links)",
					forbiddenFraction * 100.0));
		}
		if (priorityFraction < 0.5 && forbiddenFraction == 0.0) {
			warnings.add(String.format(Locale.US,
					"Only %.1f%% on cycle-priority infrastructure (cycleway, bicycle=yes/"
							+ "designated on paths, route=bicycle|mtb, network=ncn|rcn|lcn|icn, "
							+ "pilgrimage=yes on otherwise secondary segments where tagged)",
					priorityFraction * 100.0));
		}
		if (maxMtb >= MTB_WARNING_THRESHOLD) {
			warnings.add(String.format(Locale.US,
					"Difficult MTB terrain: mtb:scale up to %d (0=easiest, 6=extreme)", maxMtb));
		}
		if (maxMtbUphill >= MTB_WARNING_THRESHOLD) {
			warnings.add(String.format(Locale.US,
					"Steep MTB climbs: mtb:scale:uphill up to %d (0=easiest, 5=extreme)", maxMtbUphill));
		}
		return new ValidationResult(forbiddenFraction, priorityFraction, maxMtb, maxMtbUphill, warnings);
	}

	static boolean isForbiddenHighway(@Nullable String highwayType) {
		if (highwayType == null) {
			return false;
		}
		for (String forbidden : FORBIDDEN_HIGHWAYS) {
			if (forbidden.equals(highwayType)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHikingPriorityPath(@Nullable String highwayType) {
		if (highwayType == null) {
			return false;
		}
		for (String path : HIKING_PRIORITY_PATHS) {
			if (path.equals(highwayType)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isCyclingPrioritySegment(@NonNull RouteSegmentResult segment, boolean pilgrimageMode) {
		String highway = highwayType(segment);
		if ("cycleway".equals(highway)) {
			return true;
		}
		if (isCycleNetworkWay(segment)) {
			return true;
		}
		String bicycle = tagValue(segment, "bicycle");
		if (bicycle != null && ("yes".equals(bicycle) || "designated".equals(bicycle)) && highway != null) {
			for (String pathLike : CYCLING_PATH_LIKE) {
				if (pathLike.equals(highway)) {
					return true;
				}
			}
		}
		if (pilgrimageMode && isPilgrimage(segment) && !isForbiddenHighway(highway)) {
			return true;
		}
		return false;
	}

	private static boolean isCycleNetworkWay(@NonNull RouteSegmentResult segment) {
		String routeTag = tagValue(segment, "route");
		if ("bicycle".equals(routeTag) || "mtb".equals(routeTag)) {
			return true;
		}
		for (String networkTag : CYCLE_NETWORK_TAGS) {
			if (networkTag.equals(tagValue(segment, "network"))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPilgrimage(@NonNull RouteSegmentResult segment) {
		String value = tagValue(segment, "pilgrimage");
		return value != null && "yes".equalsIgnoreCase(value);
	}

	static int parseMtbScaleDigit(@Nullable String tag) {
		if (tag == null || tag.isEmpty()) {
			return -1;
		}
		char c = tag.charAt(0);
		if (c >= '0' && c <= '6') {
			return c - '0';
		}
		return -1;
	}

	@Nullable
	static String highwayType(@NonNull RouteSegmentResult segment) {
		if (segment.getObject() == null) {
			return null;
		}
		return segment.getObject().getHighway();
	}

	@Nullable
	static String tagValue(@NonNull RouteSegmentResult segment, @NonNull String key) {
		if (segment.getObject() == null) {
			return null;
		}
		return segment.getObject().getValue(key);
	}

	static double segmentDistanceM(@NonNull RouteSegmentResult segment) {
		return segment.getDistance();
	}

	private static double fraction(double part, double total) {
		if (total <= 0.0) {
			return 0.0;
		}
		return part / total;
	}
}
