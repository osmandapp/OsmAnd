package net.osmand.map;

import net.osmand.util.Algorithms;

public enum ParameterType {
	UNDEFINED(null),
	DATE("date"),
	NUMERIC("numeric");

	private final String paramName;

	ParameterType(String paramName) {
		this.paramName = paramName;
	}

	public String getParamName() {
		return paramName;
	}

	public static ParameterType fromName(String paramName) {
		for (ParameterType value : values()) {
			if (Algorithms.stringsEqual(value.paramName, paramName)) {
				return value;
			}
		}
		return UNDEFINED;
	}
}
