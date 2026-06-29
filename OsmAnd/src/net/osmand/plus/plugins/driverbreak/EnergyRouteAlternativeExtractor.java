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

import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds alternative car-route segment lists from junction {@link RouteSegmentResult#getAttachedRoutes()}
 * data exposed by the OsmAnd vector router.
 */
public class EnergyRouteAlternativeExtractor {

	/** Maximum number of distinct alternatives to evaluate (including primary). */
	private static final int MAX_ALTERNATIVES = 8;

	private EnergyRouteAlternativeExtractor() {
	}

	/**
	 * Returns the primary route plus locally diverging variants at junction attachments.
	 *
	 * @param route calculated route
	 * @return at least one entry (primary); never null
	 */
	@NonNull
	public static List<List<RouteSegmentResult>> collectRouteVariants(@NonNull RouteCalculationResult route) {
		List<RouteSegmentResult> primary = route.getOriginalRoute();
		List<List<RouteSegmentResult>> variants = new ArrayList<>();
		if (Algorithms.isEmpty(primary)) {
			return variants;
		}
		variants.add(new ArrayList<>(primary));
		Set<String> seen = new HashSet<>();
		seen.add(routeSignature(primary));

		for (int index = 0; index < primary.size() && variants.size() < MAX_ALTERNATIVES; index++) {
			RouteSegmentResult segment = primary.get(index);
			List<RouteSegmentResult> attached = segment.getAttachedRoutes(segment.getStartPointIndex());
			if (Algorithms.isEmpty(attached)) {
				continue;
			}
			long mainRoadId = segment.getObject().getId();
			for (RouteSegmentResult branch : attached) {
				if (branch.getObject().getId() == mainRoadId) {
					continue;
				}
				List<RouteSegmentResult> variant = new ArrayList<>(primary);
				variant.set(index, branch);
				String signature = routeSignature(variant);
				if (seen.add(signature)) {
					variants.add(variant);
					if (variants.size() >= MAX_ALTERNATIVES) {
						break;
					}
				}
			}
		}
		return variants;
	}

	@NonNull
	private static String routeSignature(@NonNull List<RouteSegmentResult> segments) {
		StringBuilder builder = new StringBuilder();
		for (RouteSegmentResult segment : segments) {
			builder.append(segment.getObject().getId()).append(':')
					.append(segment.getStartPointIndex()).append('-')
					.append(segment.getEndPointIndex()).append(';');
		}
		return builder.toString();
	}
}
