package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;

import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
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
		for (ExportType exportType : getTypes()) {
			if (!Algorithms.isEmpty(itemsMap.get(exportType))) {
				notEmptyTypes.add(exportType);
			}
		}
		return notEmptyTypes;
	}

	public long calculateSize() {
		return calculateSize(getTypes());
	}

	public long calculateSize(@NonNull Collection<ExportType> exportTypes) {
		long size = 0;
		for (ExportType exportType : exportTypes) {
			size += BackupUtils.calculateItemsSize(getItemsForType(exportType));
		}
		return size;
	}

	public List<?> getItemsForType(@NonNull ExportType type) {
		return itemsMap.get(type);
	}
}