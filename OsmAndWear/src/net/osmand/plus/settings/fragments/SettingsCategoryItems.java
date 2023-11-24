package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsCategoryItems {

	private final Map<ExportSettingsType, List<?>> itemsMap;

	public SettingsCategoryItems(@NonNull Map<ExportSettingsType, List<?>> itemsMap) {
		this.itemsMap = itemsMap;
	}

	public List<ExportSettingsType> getTypes() {
		return new ArrayList<>(itemsMap.keySet());
	}

	public List<ExportSettingsType> getNotEmptyTypes() {
		List<ExportSettingsType> notEmptyTypes = new ArrayList<>();
		for (ExportSettingsType type : getTypes()) {
			if (!Algorithms.isEmpty(itemsMap.get(type))) {
				notEmptyTypes.add(type);
			}
		}
		return notEmptyTypes;
	}

	public List<?> getItemsForType(ExportSettingsType type) {
		return itemsMap.get(type);
	}

	public Map<ExportSettingsType, List<?>> getItemsMap() {
		return itemsMap;
	}
}