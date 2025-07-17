package net.osmand.plus.routing;

import static net.osmand.plus.routing.data.AnnounceTimeDistances.STATE_PREPARE_TURN;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
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

	private boolean useDestination = false;

	public CurrentStreetName() {
	}

	public CurrentStreetName(@NonNull NextDirectionInfo info) {
		setupStreetName(info);
	}

	public CurrentStreetName(@NonNull NextDirectionInfo info, boolean useDestination) {
		this.useDestination = useDestination;
		setupStreetName(info);
	}

	public CurrentStreetName(@NonNull RoutingHelper routingHelper, @NonNull NextDirectionInfo info) {
		setupCurrentName(routingHelper, info);
	}

	public void setupCurrentName(@NonNull RoutingHelper routingHelper, @NonNull NextDirectionInfo info) {
		Location l = routingHelper.getLastFixedLocation();
		AnnounceTimeDistances adt = routingHelper.getVoiceRouter().getAnnounceTimeDistances();
		boolean isSet = false;
		// 1. turn is imminent
		if (info.distanceTo > 0 && adt.isTurnStateActive(adt.getSpeed(l), info.distanceTo * 1.3, STATE_PREPARE_TURN)) {
			useDestination = true;
			isSet = setupStreetName(info);
		}
		// 2. display current road street name
		if (!isSet) {
			useDestination = false;
			RouteSegmentResult rs = routingHelper.getCurrentSegmentResult();
			if (rs != null) {
				text = getRouteSegmentStreetName(routingHelper, rs, false);
				showMarker = true;
				shields = RoadShield.create(rs.getObject());
				if (Algorithms.isEmpty(text) && shields.isEmpty()) {
					text = getRouteSegmentStreetName(routingHelper, rs, true);
				}
				isSet = !Algorithms.isEmpty(text) || !shields.isEmpty();
			}
		}
		// 3. display next road street name if this one empty
		if (!isSet) {
			RouteSegmentResult rs = routingHelper.getNextStreetSegmentResult();
			if (rs != null) {
				text = getRouteSegmentStreetName(routingHelper, rs, false);
				turnType = TurnType.valueOf(TurnType.C, false);
				shields = RoadShield.create(rs.getObject());
			}
		}
		if (turnType == null) {
			showMarker = true;
		}
	}

	public boolean setupStreetName(@NonNull NextDirectionInfo info) {
		boolean isSet = false;
		if (info.directionInfo != null && !info.directionInfo.getTurnType().isSkipToSpeak()) {
			String name = info.directionInfo.getStreetName();
			String ref = info.directionInfo.getRef();
			String destinationName = info.directionInfo.getDestinationName();
			isSet = !(Algorithms.isEmpty(name) && Algorithms.isEmpty(ref) && Algorithms.isEmpty(destinationName));

			RouteDataObject dataObject = info.directionInfo.getRouteDataObject();
			if (useDestination) {
				shields = RoadShield.createDestination(dataObject, info.directionInfo);
			} else {
				shields = RoadShield.create(dataObject);
			}
			if (shields.isEmpty()) {
				destinationName = info.directionInfo.getDestinationRefAndName();
			}
			text = RoutingHelperUtils.formatStreetName(name, ref, destinationName, "", shields);
			turnType = info.directionInfo.getTurnType();
			if (turnType == null) {
				turnType = TurnType.valueOf(TurnType.C, false);
			}
			ExitInfo exitInfo = info.directionInfo.getExitInfo();
			if (exitInfo != null) {
				// don't display name of exit street name
				exitRef = exitInfo.getRef();
				if (!isSet && !Algorithms.isEmpty(info.directionInfo.getDestinationName())) {
					text = info.directionInfo.getDestinationName();
					isSet = true;
				}
			}
		}
		return isSet;
	}

	@NonNull
	private static String getRouteSegmentStreetName(@NonNull RoutingHelper routingHelper, @NonNull RouteSegmentResult rs, boolean includeRef) {
		OsmandSettings settings = routingHelper.getSettings();
		String lang = settings.MAP_PREFERRED_LOCALE.get();
		boolean transliterate = settings.MAP_TRANSLITERATE_NAMES.get();

		RouteDataObject object = rs.getObject();
		String name = object.getName(lang, transliterate);
		String ref = object.getRef(lang, transliterate, rs.isForwardDirection());
		String destinationName = object.getDestinationName(lang, transliterate, rs.isForwardDirection());
		return RoutingHelperUtils.formatStreetName(name, includeRef ? ref : null, destinationName, "»");
	}
}
