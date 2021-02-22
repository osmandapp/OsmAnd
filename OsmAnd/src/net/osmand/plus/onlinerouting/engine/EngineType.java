package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;

import net.osmand.util.Algorithms;

public class EngineType {

	public final static OnlineRoutingEngine GRAPHHOPPER_TYPE = new GraphhopperEngine(null);
	public final static OnlineRoutingEngine OSRM_TYPE = new OsrmEngine(null);
	public final static OnlineRoutingEngine ORS_TYPE = new OrsEngine(null);
	public final static OnlineRoutingEngine GPX_TYPE = new GpxEngine(null);

	private static OnlineRoutingEngine[] enginesTypes;

	public static OnlineRoutingEngine[] values() {
		if (enginesTypes == null) {
			enginesTypes = new OnlineRoutingEngine[]{
					GRAPHHOPPER_TYPE,
					OSRM_TYPE,
					ORS_TYPE,
					GPX_TYPE
			};
		}
		return enginesTypes;
	}

	@NonNull
	public static OnlineRoutingEngine getTypeByName(@NonNull String typeName) {
		for (OnlineRoutingEngine type : values()) {
			if (Algorithms.objectEquals(type.getTypeName(), typeName)) {
				return type;
			}
		}
		return values()[0];
	}

}
