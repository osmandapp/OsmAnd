package net.osmand.plus.settings.fragments;

import net.osmand.plus.settings.backend.ExportSettingsType;

import java.util.List;

public class ExportDataObject {

	private ExportSettingsType type;
	private List<?> items;

	public ExportDataObject(ExportSettingsType type, List<?> items) {
		this.type = type;
		this.items = items;
	}

	public ExportSettingsType getType() {
		return type;
	}

	public List<?> getItems() {
		return items;
	}
}