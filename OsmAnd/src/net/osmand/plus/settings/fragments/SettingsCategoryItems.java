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

	@NonNull
	public List<ExportType> getTypes() {
		return new ArrayList<>(itemsMap.keySet());
	}

	@NonNull
	public List<ExportType> getVisibleTypes() {
		List<ExportType> types = new ArrayList<>();
		for (ExportType type : getTypes()) {
			if (!type.isHidden()) {
				types.add(type);
			}
		}
		return types;
	}

	@NonNull
	public List<ExportType> getNotEmptyTypes() {
		List<ExportType> notEmptyTypes = new ArrayList<>();
		for (ExportType exportType : getVisibleTypes()) {
			if (!Algorithms.isEmpty(itemsMap.get(exportType))) {
				notEmptyTypes.add(exportType);
			}
		}
		return notEmptyTypes;
	}

	public long calculateSize() {
		return calculateSize(getVisibleTypes());
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