package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.ui.ActivateMapToolbarController;
import net.osmand.plus.download.ui.DownloadMapToolbarController;
import net.osmand.plus.download.ui.SuggestMapToolbarController;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapSuggestionController {

	private static final int MIN_ZOOM_TO_SHOW_BANNER = 9;
	private static final int MAX_ZOOM_TO_SHOW_BANNER = 11;

	private final OsmandApplication app;
	private final OsmandMapTileView view;
	private final OsmandRegions osmandRegions;
	private final ResourceManager resourceManager;

	private int lastMapCenterX;
	private int lastMapCenterY;
	private int lastMapZoom;
	private int lastCheckDataHash;

	public MapSuggestionController(@NonNull OsmandMapTileView view) {
		this.view = view;
		this.app = view.getApplication();
		this.osmandRegions = app.getRegions();
		this.resourceManager = app.getResourceManager();
	}

	public void updateSuggestion(@NonNull RotatedTileBox tileBox,
	                             @Nullable List<BinaryMapDataObject> currentObjects) {
		if (!updateCachedMapPosition(tileBox, currentObjects)) return;

		if (currentObjects != null && shouldShowBanner()) {
			Map<WorldRegion, BinaryMapDataObject> deactivated = new LinkedHashMap<>();
			Map<WorldRegion, BinaryMapDataObject> downloadable = new LinkedHashMap<>();
			collectSuggestions(currentObjects, deactivated, downloadable);

			if (!deactivated.isEmpty() || !downloadable.isEmpty()) {
				boolean activate = !deactivated.isEmpty();
				WorldRegion region = fetchSmallestWorldRegion(activate ? deactivated : downloadable);
				if (region != null) {
					Object item = activate ? findBackupedLocalItem(region) : findIndexItem(region);
					String name = region.getLocaleName();
					if (item != null && !Algorithms.isEmpty(name)) {
						showSuggestMapBanner(name, item);
						return;
					}
				}
			}
		}
		hideSuggestMapToolbar();
	}

	private boolean updateCachedMapPosition(@NonNull RotatedTileBox tileBox,
	                                        @Nullable List<BinaryMapDataObject> currentObjects) {
		int zoom = tileBox.getZoom();
		int cx = tileBox.getCenter31X();
		int cy = tileBox.getCenter31Y();
		if (lastMapCenterX == cx && lastMapCenterY == cy && lastMapZoom == zoom && !checkHashForFirstUsage(currentObjects)) {
			return false;
		}
		lastMapCenterX = cx;
		lastMapCenterY = cy;
		lastMapZoom = zoom;
		if (currentObjects != null) {
			lastCheckDataHash = currentObjects.hashCode();
		}
		return true;
	}

	private boolean checkHashForFirstUsage(@Nullable List<BinaryMapDataObject> currentObjects) {
		boolean firstTimeAppStart = app.getAppInitializer().isFirstTime();
		return firstTimeAppStart && currentObjects != null && lastCheckDataHash != currentObjects.hashCode();
	}

	private boolean shouldShowBanner() {
		MapActivity mapActivity = view.getMapActivity();
		return app.getSettings().SHOW_SUGGEST_MAP_DIALOG.get()
				&& mapActivity != null && mapActivity.getWidgetsVisibilityHelper().shouldShowSuggestMapBanner()
				&& lastMapZoom >= MIN_ZOOM_TO_SHOW_BANNER
				&& lastMapZoom <= MAX_ZOOM_TO_SHOW_BANNER
				&& !view.isAnimatingMapMove()
				&& !hasAnyDownloadedActiveMap();
	}

	private boolean hasAnyDownloadedActiveMap() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		DownloadResources indexes = downloadThread.getIndexes();
		return !indexes.getExternalMapFileNamesAt(lastMapCenterX, lastMapCenterY, false).isEmpty();
	}

	private void collectSuggestions(@NonNull List<BinaryMapDataObject> currentObjects,
	                                @NonNull Map<WorldRegion, BinaryMapDataObject> deactivated,
	                                @NonNull Map<WorldRegion, BinaryMapDataObject> downloadable) {
		for (int i = 0; i < currentObjects.size(); i++) {
			BinaryMapDataObject o = currentObjects.get(i);
			String fullName = osmandRegions.getFullName(o);
			WorldRegion regionData = osmandRegions.getRegionData(fullName);
			if (regionData != null && regionData.isRegionMapDownload()) {
				String downloadName = regionData.getRegionDownloadName();
				if (downloadName != null) {
					if (resourceManager.checkIfObjectBackuped(downloadName)) {
						deactivated.put(regionData, o);
					} else if (!resourceManager.checkIfObjectDownloaded(downloadName)) {
						downloadable.put(regionData, o);
					} else {
						deactivated.clear();
						downloadable.clear();
						return;
					}
				}
			}
		}
	}

	@Nullable
	private WorldRegion fetchSmallestWorldRegion(@NonNull Map<WorldRegion, BinaryMapDataObject> mapObjects) {
		Map.Entry<WorldRegion, BinaryMapDataObject> entry = app.getRegions().getSmallestBinaryMapDataObjectAt(mapObjects);
		return entry != null ? entry.getKey() : null;
	}

	@Nullable
	private LocalItem findBackupedLocalItem(@NonNull WorldRegion regionData) {
		String regionName = regionData.getRegionDownloadName();
		String mapFileName = ResourceManager.getMapFileName(regionName);
		String roadMapFileName = ResourceManager.getRoadMapFileName(regionName);

		LocalItem localItem = findBackupedLocalItem(mapFileName);
		return localItem != null ? localItem : findBackupedLocalItem(roadMapFileName);
	}

	@Nullable
	private LocalItem findBackupedLocalItem(@NonNull String fileName) {
		File backupDir = app.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		File file = new File(backupDir, fileName);
		if (file.exists()) {
			LocalItemType type = LocalItemUtils.getItemType(app, file);
			if (type != null) {
				LocalItem localItem = new LocalItem(file, type);
				LocalItemUtils.updateItem(app, localItem);
				return localItem;
			}
		}
		return null;
	}

	@Nullable
	private IndexItem findIndexItem(@Nullable WorldRegion regionData) {
		IndexItem indexItem = null;
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		DownloadResources indexes = downloadThread.getIndexes();
		if (regionData != null) {
			List<IndexItem> indexItems = indexes.getIndexItems(regionData);
			if (indexItems.isEmpty()) {
				if (!indexes.isDownloadedFromInternet && app.getSettings().isInternetConnectionAvailable()) {
					downloadThread.runReloadIndexFilesSilent();
					lastMapCenterX = 0;
					lastMapCenterY = 0;
					lastMapZoom = 0;
				}
			} else {
				for (IndexItem item : indexItems) {
					if (item.getType() == DownloadActivityType.NORMAL_FILE
							&& !(item.isDownloaded() || downloadThread.isDownloading(item))) {
						indexItem = item;
						break;
					}
				}
			}
		}
		return indexItem;
	}

	private void showSuggestMapBanner(@NonNull String regionName, @NonNull Object object) {
		MapActivity mapActivity = view.getMapActivity();
		if (mapActivity != null && !SuggestMapToolbarController.isLastProcessedRegionName(regionName)) {
			app.runInUIThread(() -> {
				if (!SuggestMapToolbarController.isLastProcessedRegionName(regionName)) {
					TopToolbarController controller = mapActivity.getTopToolbarController(TopToolbarControllerType.SUGGEST_MAP);
					if (controller == null || !((SuggestMapToolbarController) controller).getRegionName().equals(regionName)) {
						if (object instanceof IndexItem indexItem) {
							controller = new DownloadMapToolbarController(mapActivity, indexItem, regionName);
						} else if (object instanceof LocalItem localItem) {
							controller = new ActivateMapToolbarController(mapActivity, localItem, regionName);
						}
						if (controller != null) {
							mapActivity.showTopToolbar(controller);
						}
					}
				}
			});
		} else {
			hideSuggestMapToolbar();
		}
	}

	private void hideSuggestMapToolbar() {
		MapActivity mapActivity = view.getMapActivity();
		if (mapActivity != null) {
			app.runInUIThread(() -> mapActivity.hideTopToolbar(TopToolbarControllerType.SUGGEST_MAP));
		}
	}
}
