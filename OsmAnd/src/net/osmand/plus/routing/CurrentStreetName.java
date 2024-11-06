package net.osmand.plus.routing;

import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_PREPARE_TURN;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.ExitInfo;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

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

	public static CurrentStreetName getCurrentName(@NonNull RoutingHelper routingHelper, @NonNull NextDirectionInfo info) {
		CurrentStreetName streetName = new CurrentStreetName();
		Location l = routingHelper.getLastFixedLocation();
		AnnounceTimeDistances adt = routingHelper.getVoiceRouter().getAnnounceTimeDistances();
		boolean isSet = false;
		// 1. turn is imminent
		if (info.distanceTo > 0 && adt.isTurnStateActive(adt.getSpeed(l), info.distanceTo * 1.3, STATE_PREPARE_TURN)) {
			isSet = setupStreetName(streetName, info);
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

	@NonNull
	public static CurrentStreetName createStreetName(@NonNull NextDirectionInfo info) {
		CurrentStreetName streetName = new CurrentStreetName();
		CurrentStreetName.setupStreetName(streetName, info);
		return streetName;
	}

	public static boolean setupStreetName(@NonNull CurrentStreetName streetName, @NonNull NextDirectionInfo info) {
		boolean isSet = false;
		if (info.directionInfo != null && !info.directionInfo.getTurnType().isSkipToSpeak()) {
			String name = info.directionInfo.getStreetName();
			String ref = info.directionInfo.getRef();
			String destinationName = info.directionInfo.getDestinationName();
			isSet = !(Algorithms.isEmpty(name) && Algorithms.isEmpty(ref) && Algorithms.isEmpty(destinationName));

			RouteDataObject dataObject = info.directionInfo.getRouteDataObject();
			streetName.shields = RoadShield.create(dataObject);
			streetName.text = RoutingHelperUtils.formatStreetName(name, ref, destinationName, "»", streetName.shields);
			streetName.turnType = info.directionInfo.getTurnType();
			if (streetName.turnType == null) {
				streetName.turnType = TurnType.valueOf(TurnType.C, false);
			}
			ExitInfo exitInfo = info.directionInfo.getExitInfo();
			if (exitInfo != null) {
				// don't display name of exit street name
				streetName.exitRef = exitInfo.getRef();
				if (!isSet && !Algorithms.isEmpty(info.directionInfo.getDestinationName())) {
					streetName.text = info.directionInfo.getDestinationName();
					isSet = true;
				}
			}
		}
		return isSet;
	}
}
