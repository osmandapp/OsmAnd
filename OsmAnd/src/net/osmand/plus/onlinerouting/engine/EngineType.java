package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.onlinerouting.parser.GpxParser;
import net.osmand.plus.onlinerouting.parser.GraphhopperParser;
import net.osmand.plus.onlinerouting.parser.OrsParser;
import net.osmand.plus.onlinerouting.parser.OsrmParser;
import net.osmand.plus.onlinerouting.parser.ResponseParser;
import net.osmand.util.Algorithms;

public enum EngineType {
	GRAPHHOPPER("Graphhopper", GraphhopperParser.class),
	OSRM("OSRM", OsrmParser.class),
	ORS("Openroute Service", OrsParser.class),
	GPX("GPX", GpxParser.class);

	private final String title;
	private final Class<? extends ResponseParser> parserClass;

	EngineType(String title, Class<? extends ResponseParser> parserClass) {
		this.title = title;
		this.parserClass = parserClass;
	}

	public String getTitle() {
		return title;
	}

	public Class<? extends ResponseParser> getParserClass() {
		return parserClass;
	}

	@NonNull
	public static EngineType getTypeByName(@Nullable String name) {
		if (!Algorithms.isEmpty(name)) {
			for (EngineType type : values()) {
				if (type.name().equals(name)) {
					return type;
				}
			}
		}
		return values()[0];
	}
}
