package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CurrentStreetName {
	public String text;
	public TurnType turnType;
	public boolean showMarker; // turn type has priority over showMarker
	public List<RoadShield> shields = new ArrayList<>();
	public String exitRef;

	@NonNull
	private static String getRouteSegmentStreetName(@NonNull RoutingHelper routingHelper, @NonNull RouteSegmentResult rs, boolean includeRef) {
		OsmandSettings settings = routingHelper.getSettings();
		String nm = rs.getObject().getName(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get());
		String rf = rs.getObject().getRef(settings.MAP_PREFERRED_LOCALE.get(), settings.MAP_TRANSLITERATE_NAMES.get(), rs.isForwardDirection());
		String dn = rs.getObject().getDestinationName(settings.MAP_PREFERRED_LOCALE.get(),
				settings.MAP_TRANSLITERATE_NAMES.get(), rs.isForwardDirection());
		return RoutingHelperUtils.formatStreetName(nm, includeRef ? rf : null, dn, "»");
	}

	@NonNull
	public static CurrentStreetName getCurrentName(@NonNull RoutingHelper routingHelper, @NonNull NextDirectionInfo n) {
		CurrentStreetName streetName = new CurrentStreetName();
		Location l = routingHelper.getLastFixedLocation();
		AnnounceTimeDistances adt = routingHelper.getVoiceRouter().getAnnounceTimeDistances();
		boolean isSet = false;
		// 1. turn is imminent
		if (n.distanceTo > 0 && n.directionInfo != null && !n.directionInfo.getTurnType().isSkipToSpeak() &&
				adt.isTurnStateActive(adt.getSpeed(l), n.distanceTo * 1.3, AnnounceTimeDistances.STATE_PREPARE_TURN)) {
			String nm = n.directionInfo.getStreetName();
			String rf = n.directionInfo.getRef();
			String dn = n.directionInfo.getDestinationName();
			isSet = !(Algorithms.isEmpty(nm) && Algorithms.isEmpty(rf) && Algorithms.isEmpty(dn));
			RouteDataObject routeDataObject = n.directionInfo.getRouteDataObject();
			streetName.shields = RoadShield.create(routeDataObject);
			streetName.text = RoutingHelperUtils.formatStreetName(nm, rf, dn, "»", streetName.shields);
			streetName.turnType = n.directionInfo.getTurnType();
			if (streetName.turnType == null) {
				streetName.turnType = TurnType.valueOf(TurnType.C, false);
			}
			if (n.directionInfo.getExitInfo() != null) {
				// don't display name of exit street name
				streetName.exitRef = n.directionInfo.getExitInfo().getRef();
				if (!isSet && !Algorithms.isEmpty(n.directionInfo.getDestinationName())) {
					streetName.text = n.directionInfo.getDestinationName();
					isSet = true;
				}
			}
		}
		// 2. display current road street name
		if (!isSet) {
			RouteSegmentResult rs = routingHelper.getCurrentSegmentResult();
			if (rs != null) {
				streetName.text = getRouteSegmentStreetName(routingHelper, rs, false);
				if (Algorithms.isEmpty(streetName.text)) {
					streetName.text = getRouteSegmentStreetName(routingHelper, rs, true);
					isSet = !Algorithms.isEmpty(streetName.text);
				} else {
					isSet = true;
				}
				streetName.showMarker = true;
				streetName.shields = RoadShield.create(rs.getObject());
			}
		}
		// 3. display next road street name if this one empty
		if (!isSet) {
			RouteSegmentResult rs = routingHelper.getNextStreetSegmentResult();
			if (rs != null) {
				streetName.text = getRouteSegmentStreetName(routingHelper, rs, false);
				streetName.turnType = TurnType.valueOf(TurnType.C, false);
				streetName.shields = RoadShield.create(rs.getObject());
			}
		}
		if (streetName.turnType == null) {
			streetName.showMarker = true;
		}
		return streetName;
	}

	public static class RoadShield {
		private final RouteDataObject rdo;
		private final String tag;
		private final String value;
		private StringBuilder additional;

		public RoadShield(@NonNull RouteDataObject rdo, @NonNull String tag, @NonNull String value) {
			this.rdo = rdo;
			this.tag = tag;
			this.value = value;
		}

		@NonNull
		public static List<RoadShield> create(@Nullable RouteDataObject rdo) {
			List<RoadShield> shields = new ArrayList<>();
			if (rdo != null && rdo.nameIds != null) {
				StringBuilder additional = new StringBuilder();
				for (int i = 0; i < rdo.nameIds.length; i++) {
					String tag = rdo.region.routeEncodingRules.get(rdo.nameIds[i]).getTag();
					String val = rdo.names.get(rdo.nameIds[i]);
					if (!tag.endsWith("_ref") && !tag.startsWith("route_road")) {
						additional.append(tag).append("=").append(val).append(";");
					} else if (tag.startsWith("route_road") && tag.endsWith("_ref")) {
						shields.add(new RoadShield(rdo, tag, val));
					}
				}
				if (!shields.isEmpty()) {
					for (RoadShield shield : shields) {
						shield.additional = additional;
					}
				}
			}
			return shields;
		}

		public RouteDataObject getRdo() {
			return rdo;
		}

		public StringBuilder getAdditional() {
			return additional;
		}

		public String getTag() {
			return tag;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RoadShield shield = (RoadShield) o;
			return Objects.equals(tag, shield.tag)
					&& Objects.equals(value, shield.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(tag, value);
		}
	}
}
