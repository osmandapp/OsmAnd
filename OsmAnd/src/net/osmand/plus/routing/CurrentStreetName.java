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

public class CurrentStreetName {
	public String text;
	public TurnType turnType;
	public boolean showMarker; // turn type has priority over showMarker
	public RoadShield shield;
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
			streetName.shield = RoadShield.create(routeDataObject);
			streetName.text = RoutingHelperUtils.formatStreetName(nm, rf, dn, "»", streetName.shield);
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
				streetName.shield = RoadShield.create(rs.getObject());
			}
		}
		// 3. display next road street name if this one empty
		if (!isSet) {
			RouteSegmentResult rs = routingHelper.getNextStreetSegmentResult();
			if (rs != null) {
				streetName.text = getRouteSegmentStreetName(routingHelper, rs, false);
				streetName.turnType = TurnType.valueOf(TurnType.C, false);
				streetName.shield = RoadShield.create(rs.getObject());
			}
		}
		if (streetName.turnType == null) {
			streetName.showMarker = true;
		}
		return streetName;
	}

	public static class RoadShield {
		private final RouteDataObject rdo;
		private String text;
		private String nameTag;
		private StringBuilder additional;

		public RoadShield(@NonNull RouteDataObject rdo) {
			this.rdo = rdo;
			StringBuilder additional = new StringBuilder();
			for (int i = 0; i < rdo.nameIds.length; i++) {
				String key = rdo.region.routeEncodingRules.get(rdo.nameIds[i]).getTag();
				String val = rdo.names.get(rdo.nameIds[i]);
				if (!key.endsWith("_ref") && !key.startsWith("route_road")) {
					additional.append(key).append("=").append(val).append(";");
				}
			}
			for (int i = 0; i < rdo.nameIds.length; i++) {
				String tag = rdo.region.routeEncodingRules.get(rdo.nameIds[i]).getTag();
				String val = rdo.names.get(rdo.nameIds[i]);
				if (tag.startsWith("route_road") && tag.endsWith("_ref")) {
					this.additional = additional;
					this.nameTag = tag;
					text = val;
					break;
				}
			}
		}

		public static RoadShield create(@Nullable RouteDataObject rdo) {
			if (rdo != null && rdo.nameIds != null) {
				return new RoadShield(rdo);
			}
			return null;
		}

		public RouteDataObject getRdo() {
			return rdo;
		}

		public String getText() {
			return text;
		}

		public String getNameTag() {
			return nameTag;
		}

		public StringBuilder getAdditional() {
			return additional;
		}

		public boolean hasShield() {
			return text != null;
		}
	}
}
