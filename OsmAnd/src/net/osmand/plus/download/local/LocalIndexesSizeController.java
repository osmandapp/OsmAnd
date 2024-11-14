package net.osmand.plus.download.local;

import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalIndexesSizeController implements IDialogController {

	private static final String PROCESS_ID = "calculate_and_store_local_indexes_size";

	private static final long BYTES_IN_MB = 1024 * 1024;
	private static final long TILES_SIZE_CALCULATION_LIMIT = 50 * BYTES_IN_MB;

	private final Map<String, Long> cachedSize = new HashMap<>();
	private boolean fullSizeMode = false;

	private LocalIndexesSizeController() { }

	public void askCalculateFullSize(@NonNull List<LocalItem> itemsToCalculate) {
		fullSizeMode = true;
		cachedSize.clear();
		// run in thread
		for (LocalItem item : itemsToCalculate) {
			updateLocalItemSizeIfNeeded(item);
		}
	}

	public void updateLocalItemSizeIfNeeded(@NonNull LocalItem localItem) {
		if (localItem.getType() == LocalItemType.TILES_DATA) {
			File file = localItem.getFile();
			Long size = getCachedSize(file);
			long calculationLimit = getSizeCalculationLimit(localItem);
			if (size == null) {
				size = calculateSize(file, calculationLimit);
				saveCachedSize(file, size);
			}
			localItem.setSize(size);
			localItem.setSizeCalculationLimit(calculationLimit);
		}
	}

	@Nullable
	private Long getCachedSize(@NonNull File file) {
		return cachedSize.get(file.getAbsolutePath());
	}

	private void saveCachedSize(@NonNull File file, long size) {
		cachedSize.put(file.getAbsolutePath(), size);
	}

	private long getSizeCalculationLimit(@NonNull LocalItem localItem) {
		if (localItem.type == TILES_DATA && !fullSizeMode) {
			File file = localItem.getFile();
			String fileName = file.getName();
			if (!fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return TILES_SIZE_CALCULATION_LIMIT;
			}
		}
		return -1;
	}

	private long calculateSize(@NonNull File file, long limit) {
		return file.isDirectory() ? calculateDirSize(file, 0, limit) : file.length();
	}

	private long calculateDirSize(@NonNull File file, long currentSize, long limit) {
		File[] files = file.listFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File f : files) {
				if (f.isDirectory()) {
					currentSize = calculateDirSize(f, currentSize, limit);
				} else {
					currentSize += f.length();
				}
				if (limit > 0 && currentSize >= limit) {
					return currentSize;
				}
			}
		}
		return currentSize;
	}

	public static boolean isFullSizeCalculating(@NonNull OsmandApplication app,
	                                            @NonNull LocalItem localItem) {
		LocalIndexesSizeController controller = requireInstance(app);
		if (localItem.type == TILES_DATA) {
			Long cachedSize = controller.cachedSize.get(localItem.getFile().getAbsolutePath());
			return cachedSize == null && controller.fullSizeMode;
		}
		return false;
	}

	@NonNull
	public static LocalIndexesSizeController requireInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		LocalIndexesSizeController controller = (LocalIndexesSizeController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new LocalIndexesSizeController();
			dialogManager.register(PROCESS_ID, controller);
		}
		return controller;
	}
}
