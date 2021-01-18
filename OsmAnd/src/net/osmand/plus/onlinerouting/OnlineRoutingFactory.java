package net.osmand.plus.onlinerouting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.GraphhopperEngine;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.engine.OrsEngine;
import net.osmand.plus.onlinerouting.engine.OsrmEngine;

import java.util.Map;

public class OnlineRoutingFactory {

	public static OnlineRoutingEngine createEngine(@NonNull EngineType type) {
		return createEngine(type, null);
	}

	@NonNull
	public static OnlineRoutingEngine createEngine(@NonNull EngineType type,
	                                               @Nullable Map<String, String> params) {
		switch (type) {
			case GRAPHHOPPER:
				return new GraphhopperEngine(params);
			case OSRM:
				return new OsrmEngine(params);
			case ORS:
				return new OrsEngine(params);
			default:
				throw new IllegalArgumentException(
						"Online routing type {" + type.name() + "} not supported");
		}
	}

}
