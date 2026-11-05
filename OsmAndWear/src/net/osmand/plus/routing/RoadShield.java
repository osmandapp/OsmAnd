package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.RouteDataObject;

import java.util.ArrayList;
import java.util.List;
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
