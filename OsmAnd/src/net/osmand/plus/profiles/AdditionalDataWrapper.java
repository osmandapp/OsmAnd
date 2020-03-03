package net.osmand.plus.profiles;

import java.util.List;

public class AdditionalDataWrapper {

	private Type type;

	private List<?> items;

	public AdditionalDataWrapper(Type type, List<?> items) {
		this.type = type;
		this.items = items;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public List<?> getItems() {
		return items;
	}

	public enum Type {
		PROFILE,
		QUICK_ACTIONS,
		POI_TYPES,
		MAP_SOURCES,
		CUSTOM_RENDER_STYLE,
		CUSTOM_ROUTING,
		AVOID_ROADS
	}
}
