package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ExportSettingsType;

import java.util.List;

public class ExportDataObject {

	private ExportSettingsType type;
	private List<?> items;

	public ExportDataObject(@NonNull ExportSettingsType type, @NonNull List<?> items) {
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