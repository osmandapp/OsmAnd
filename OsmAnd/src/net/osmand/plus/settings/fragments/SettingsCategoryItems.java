package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ExportType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsCategoryItems {

	private final Map<ExportType, List<?>> itemsMap;

	public SettingsCategoryItems(@NonNull Map<ExportType, List<?>> itemsMap) {
		this.itemsMap = itemsMap;
	}

	public List<ExportType> getTypes() {
		return new ArrayList<>(itemsMap.keySet());
	}

	public List<ExportType> getNotEmptyTypes() {
		List<ExportType> notEmptyTypes = new ArrayList<>();
		for (ExportType type : getTypes()) {
			if (!Algorithms.isEmpty(itemsMap.get(type))) {
				notEmptyTypes.add(type);
			}
		}
		return notEmptyTypes;
	}

	public List<?> getItemsForType(ExportType type) {
		return itemsMap.get(type);
	}

	public Map<ExportType, List<?>> getItemsMap() {
		return itemsMap;
	}
}