package net.osmand.plus.download;


import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.FONT_INDEX_DIR;
import static net.osmand.IndexConstants.FONT_INDEX_EXT;
import static net.osmand.IndexConstants.GEOTIFF_DIR;
import static net.osmand.IndexConstants.HEIGHTMAP_INDEX_DIR;
import static net.osmand.IndexConstants.HEIGHTMAP_SQLITE_EXT;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.NAUTICAL_INDEX_DIR;
import static net.osmand.IndexConstants.ROADS_INDEX_DIR;
import static net.osmand.IndexConstants.SRTM_INDEX_DIR;
import static net.osmand.IndexConstants.TIF_EXT;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.VOICE_INDEX_DIR;
import static net.osmand.IndexConstants.WEATHER_EXT;
import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.IndexConstants.WIKIVOYAGE_INDEX_DIR;
import static net.osmand.IndexConstants.WIKI_INDEX_DIR;
import static net.osmand.plus.download.local.LocalItemType.DEPTH_DATA;
import static net.osmand.plus.download.local.LocalItemType.FONT_DATA;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.ROAD_DATA;
import static net.osmand.plus.download.local.ItemType.TERRAIN_DATA;
import static net.osmand.plus.download.local.ItemType.TILES_DATA;
import static net.osmand.plus.download.local.ItemType.TTS_VOICE_DATA;
import static net.osmand.plus.download.local.ItemType.VOICE_DATA;
import static net.osmand.plus.download.local.ItemType.WEATHER_DATA;
import static net.osmand.plus.download.local.ItemType.WIKI_AND_TRAVEL_MAPS;
import static java.text.DateFormat.SHORT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.ItemType;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.voice.JsMediaCommandPlayer;
import net.osmand.plus.voice.JsTtsCommandPlayer;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class LocalIndexHelper {

	private final Log log = PlatformUtil.getLog(LocalIndexHelper.class);

	private final OsmandApplication app;
	private final ResourceManager resourceManager;

	public LocalIndexHelper(@NonNull OsmandApplication app) {
		this.app = app;
		resourceManager = app.getResourceManager();
	}

	@NonNull
	private String getInstalledDate(@NonNull Date date) {
		return DateFormat.getDateInstance(SHORT).format(date);
	}

	public void updateItem(@NonNull LocalItem item) {
		File file = item.getFile();
		String fileName = file.getName();
		LocalItemType type = item.getType();
		if (Algorithms.equalsToAny(type, MAP_DATA, ROAD_DATA)) {
			item.setDescription(getInstalledDate(file));
		} else if (type == TILES_DATA) {
			ITileSource template;
			if (file.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(file)) {
				template = TileSourceManager.createTileSourceTemplate(file);
			} else if (file.isFile() && fileName.endsWith(SQLiteTileSource.EXT)) {
				template = new SQLiteTileSource(app, file, TileSourceManager.getKnownSourceTemplates());
			} else {
				return;
			}
			String descr = "";
			if (template.getExpirationTimeMinutes() >= 0) {
				descr += app.getString(R.string.local_index_tile_data_expire, String.valueOf(template.getExpirationTimeMinutes()));
			}
			item.setAttachedObject(template);
			item.setDescription(descr);
		} else if (type == TERRAIN_DATA && fileName.endsWith(BINARY_SRTM_MAP_INDEX_EXT)) {
			item.setDescription(app.getString(R.string.download_srtm_maps));
		} else {
			item.setDescription(getInstalledDate(new Date(file.lastModified())));
		}
	}

	@NonNull
	private String getInstalledDate(@NonNull File file) {
		String fileModifiedDate = resourceManager.getIndexFileNames().get(file.getName());
		if (fileModifiedDate != null) {
			try {
				Date date = resourceManager.getDateFormat().parse(fileModifiedDate);
				if (date != null) {
					return getInstalledDate(date);
				}
			} catch (Exception e) {
				log.error(e);
			}
		}
		return getInstalledDate(new Date(file.lastModified()));
	}

	private void collectLocalItems(@NonNull List<LocalItem> items, @NonNull LocalItemType type,
	                               @NonNull String downloadName, boolean backuped) {
		String name = Algorithms.capitalizeFirstLetterAndLowercase(downloadName);
		if (type == MAP_DATA) {
			addLocalItem(items, type, name, MAPS_PATH, BINARY_MAP_INDEX_EXT, backuped);
		} else if (type == ROAD_DATA) {
			addLocalItem(items, type, name, ROADS_INDEX_DIR, BINARY_ROAD_MAP_INDEX_EXT, backuped);
		} else if (type == WIKI_AND_TRAVEL_MAPS) {
			addLocalItem(items, type, name, WIKI_INDEX_DIR, BINARY_WIKI_MAP_INDEX_EXT, backuped);
			addLocalItem(items, type, name, WIKIVOYAGE_INDEX_DIR, BINARY_WIKIVOYAGE_MAP_INDEX_EXT, backuped);
		} else if (type == DEPTH_DATA) {
			addLocalItem(items, type, name, NAUTICAL_INDEX_DIR, BINARY_DEPTH_MAP_INDEX_EXT, backuped);
		} else if (type == WEATHER_DATA) {
			addLocalItem(items, type, name, WEATHER_FORECAST_DIR, WEATHER_EXT, backuped);
		} else if (type == TERRAIN_DATA) {
			addLocalItem(items, type, name, GEOTIFF_DIR, TIF_EXT, backuped);
			addLocalItem(items, type, name, SRTM_INDEX_DIR, BINARY_SRTM_MAP_INDEX_EXT, backuped);
		}
	}

	private void addLocalItem(@NonNull List<LocalItem> items, @NonNull LocalItemType type, @NonNull String name,
	                          @NonNull String dirName, @NonNull String extension, boolean backuped) {
		dirName = backuped ? BACKUP_INDEX_DIR : dirName;
		File file = app.getAppPath(dirName + name + extension);
		if (file.exists()) {
			loadLocalData(file, type, items, true, null);
		}
	}

	@NonNull
	public List<LocalItem> getLocalItems(@NonNull String downloadName) {
		List<LocalItem> items = new ArrayList<>();

		collectLocalItems(items, downloadName, false);
		collectLocalItems(items, downloadName, true);

		return items;
	}

	private void collectLocalItems(@NonNull List<LocalItem> items, @NonNull String downloadName, boolean backuped) {
		for (LocalItemType type : getSuggestedItemTypes()) {
			collectLocalItems(items, type, downloadName, backuped);
		}
	}

	@NonNull
	public List<LocalItemType> getSuggestedItemTypes() {
		List<LocalItemType> types = new ArrayList<>();
		types.add(MAP_DATA);
		types.add(ROAD_DATA);
		types.add(TERRAIN_DATA);
		types.add(WIKI_AND_TRAVEL_MAPS);
		types.add(DEPTH_DATA);
		types.add(WEATHER_DATA);
		return types;
	}

	@NonNull
	public List<LocalItem> getLocalIndexItems(boolean readFiles, boolean addDescription,
	                                          @Nullable AbstractLoadLocalIndexTask task,
	                                          @NonNull LocalItemType... types) {
		List<LocalItem> items = new ArrayList<>();
		Map<String, File> indexFiles = resourceManager.getIndexFiles();

		boolean voicesCollected = false;
		for (LocalItemType type : types) {
			switch (type) {
				case WIKI_AND_TRAVEL_MAPS:
					loadDataImpl(app.getAppPath(WIKI_INDEX_DIR), WIKI_AND_TRAVEL_MAPS, BINARY_MAP_INDEX_EXT,
							readFiles, addDescription, items, indexFiles, task);
					loadDataImpl(app.getAppPath(WIKIVOYAGE_INDEX_DIR), WIKI_AND_TRAVEL_MAPS, BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT,
							readFiles, addDescription, items, indexFiles, task);
					break;
				case MAP_DATA:
					loadObfData(app.getAppPath(MAPS_PATH), items, readFiles, addDescription, indexFiles, task);
					break;
				case ROAD_DATA:
					loadObfData(app.getAppPath(ROADS_INDEX_DIR), items, readFiles, addDescription, indexFiles, task);
					break;
				case TILES_DATA:
					loadTilesData(app.getAppPath(TILES_INDEX_DIR), items, addDescription, task);
					loadTilesData(app.getAppPath(HEIGHTMAP_INDEX_DIR), items, addDescription, task);
					break;
				case TTS_VOICE_DATA:
				case VOICE_DATA:
					if (!voicesCollected) {
						loadVoiceData(app.getAppPath(VOICE_INDEX_DIR), items, readFiles, addDescription, indexFiles, task);
						voicesCollected = true;
					}
					break;
				case FONT_DATA:
					loadFontData(app.getAppPath(FONT_INDEX_DIR), items, readFiles, addDescription, indexFiles, task);
					break;
				case DEPTH_DATA:
					loadDataImpl(app.getAppPath(NAUTICAL_INDEX_DIR), DEPTH_DATA, BINARY_MAP_INDEX_EXT,
							readFiles, addDescription, items, indexFiles, task);
					break;
				case WEATHER_DATA:
					loadDataImpl(app.getAppPath(WEATHER_FORECAST_DIR), WEATHER_DATA, WEATHER_EXT,
							readFiles, addDescription, items, indexFiles, task);
					break;
				case TERRAIN_DATA:
					loadDataImpl(app.getAppPath(GEOTIFF_DIR), TERRAIN_DATA, TIF_EXT,
							readFiles, addDescription, items, indexFiles, task);
					loadDataImpl(app.getAppPath(SRTM_INDEX_DIR), TERRAIN_DATA, BINARY_MAP_INDEX_EXT,
							readFiles, addDescription, items, indexFiles, task);
					break;
			}
		}
		return items;
	}

	@NonNull
	public List<LocalItem> getLocalFullMaps(@Nullable AbstractLoadLocalIndexTask task) {
		List<LocalItem> results = new ArrayList<>();
		List<LocalItem> roadOnlyList = new ArrayList<>();

		Map<String, File> indexFiles = resourceManager.getIndexFiles();
		loadObfData(app.getAppPath(MAPS_PATH), results, true, true, indexFiles, task);
		loadObfData(app.getAppPath(ROADS_INDEX_DIR), roadOnlyList, true, true, indexFiles, task);

		addUnique(results, roadOnlyList);

		return results;
	}

	public static boolean addUnique(@NonNull List<LocalItem> results, @NonNull List<LocalItem> items) {
		int size = results.size();
		for (LocalItem item : items) {
			boolean needAdd = true;
			for (LocalItem result : results) {
				if (result.getName().equals(item.getName())) {
					needAdd = false;
					break;
				}
			}
			if (needAdd) {
				results.add(item);
			}
		}
		return size != results.size();
	}

	public void loadVoiceData(@NonNull File dir, @NonNull List<LocalItem> items, boolean readFiles,
	                          boolean addDescription, @NonNull Map<String, File> indexFiles,
	                          @Nullable AbstractLoadLocalIndexTask task) {
		if (readFiles && dir.canRead()) {
			File[] files = listFilesSorted(dir);
			if (files.length > 0) {
				loadVoiceDataImpl(files, items, addDescription, task);
			}
		} else {
			List<File> voiceFiles = new ArrayList<>();
			for (File file : indexFiles.values()) {
				if (dir.getPath().equals(file.getParent())) {
					voiceFiles.add(file);
				}
			}
			if (voiceFiles.size() > 0) {
				Collections.sort(voiceFiles);
				loadVoiceDataImpl(voiceFiles.toArray(new File[0]), items, addDescription, task);
			}
		}
	}

	private void loadVoiceDataImpl(@NonNull File[] files, @NonNull List<LocalItem> items,
	                               boolean addDescription, @Nullable AbstractLoadLocalIndexTask task) {
		List<File> voiceFilesList = new ArrayList<>(Arrays.asList(files));
		//First list TTS files, they are preferred
		Iterator<File> iterator = voiceFilesList.iterator();
		while (iterator.hasNext()) {
			File voiceFile = iterator.next();
			if (voiceFile.isDirectory() && (JsTtsCommandPlayer.isMyData(voiceFile))) {
				loadLocalData(voiceFile, TTS_VOICE_DATA, items, addDescription, task);
				iterator.remove();
			}
		}
		//Now list recorded voices
		for (File voiceFile : voiceFilesList) {
			if (voiceFile.isDirectory() && (JsMediaCommandPlayer.isMyData(voiceFile))) {
				loadLocalData(voiceFile, VOICE_DATA, items, addDescription, task);
			}
		}
	}

	private void loadFontData(@NonNull File dir, @NonNull List<LocalItem> items, boolean readFiles,
	                          boolean addDescription, @NonNull Map<String, File> indexFiles, @Nullable AbstractLoadLocalIndexTask task) {
		loadDataImpl(dir, FONT_DATA, FONT_INDEX_EXT, readFiles, addDescription, items, indexFiles, task);
	}

	private void loadTilesData(@NonNull File dir, @NonNull List<LocalItem> items,
	                           boolean addDescription, @Nullable AbstractLoadLocalIndexTask task) {
		if (dir.canRead()) {
			for (File file : listFilesSorted(dir)) {
				if (file.isFile()) {
					String fileName = file.getName();
					boolean tilesData = Algorithms.endsWithAny(fileName, SQLiteTileSource.EXT, HEIGHTMAP_SQLITE_EXT);
					if (tilesData) {
						loadLocalData(file, TILES_DATA, items, addDescription, task);
					}
				} else if (file.isDirectory()) {
					loadLocalData(file, TILES_DATA, items, addDescription, task);
				}
			}
		}
	}

	@NonNull
	private File[] listFilesSorted(@NonNull File dir) {
		File[] listFiles = dir.listFiles();
		if (listFiles == null) {
			return new File[0];
		}
		Arrays.sort(listFiles);
		return listFiles;
	}

	private void loadObfData(@NonNull File dir, @NonNull List<LocalItem> items, boolean readFiles,
	                         boolean addDescription, @NonNull Map<String, File> indexFiles,
	                         @Nullable AbstractLoadLocalIndexTask task) {
		boolean readDir = readFiles && dir.canRead();
		List<File> files = readDir ? Arrays.asList(listFilesSorted(dir)) : new ArrayList<>(indexFiles.values());
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(BINARY_MAP_INDEX_EXT)
					&& (!readDir || dir.getPath().equals(file.getParent()))) {
				LocalItemType type = LocalItemType.getItemType(app, file);
				if (type != null) {
					loadLocalData(file, type, items, addDescription, task);
				}
			}
		}
	}

	private void loadDataImpl(@NonNull File dir, @NonNull LocalItemType type, @NonNull String extension,
	                          boolean readFiles, boolean addDescription, @NonNull List<LocalItem> items,
	                          @NonNull Map<String, File> indexFiles, @Nullable AbstractLoadLocalIndexTask task) {
		boolean readDir = readFiles && dir.canRead();
		List<File> files = readDir ? Arrays.asList(listFilesSorted(dir)) : new ArrayList<>(indexFiles.values());
		for (File file : files) {
			if (file.isFile() && file.getName().endsWith(extension)
					&& (!readDir || file.getPath().startsWith(dir.getPath()))) {
				loadLocalData(file, type, items, addDescription, task);
			}
		}
	}

	private void loadLocalData(@NonNull File file, @NonNull LocalItemType type, @NonNull List<LocalItem> items,
	                           boolean addDescription, @Nullable AbstractLoadLocalIndexTask task) {
		LocalItem item = new LocalItem(file, type);
		if (addDescription) {
			updateItem(item);
		}
		items.add(item);

		if (task != null) {
			task.loadFile(item);
		}
	}
}