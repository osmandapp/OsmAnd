package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.RouteDataObject;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RoadShield {

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

	@NonNull
	public static List<RoadShield> createDestination(@Nullable RouteDataObject rdo, RouteDirectionInfo info) {
		List<RoadShield> shields = create(rdo);
		String destRef = info.getDestinationRef();
		if (rdo != null && !Algorithms.isEmpty(destRef) && !shields.isEmpty()) {
			String refs = Algorithms.splitAndClearRepeats(destRef, ";");
			List<String> split = Arrays.asList(refs.split(";"));
			Map<String, RoadShield> map = new HashMap<>();
			String tag = null;
			StringBuilder additional = new StringBuilder();
			for (RoadShield s : shields) {
				map.put(s.value, s);
				if (split.contains(s.value)) {
					tag = s.tag;
				}
				additional = s.additional;
			}

			shields.clear();
			if (tag == null) {
				return shields;
			}
			for (String s : split) {
				RoadShield shield = map.get(s);
				if (shield == null) {
					shield = new RoadShield(rdo, tag, s);
					shield.additional = additional;
				}
				shields.add(shield);
				map.remove(s);
			}
			shields.addAll(map.values());
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
