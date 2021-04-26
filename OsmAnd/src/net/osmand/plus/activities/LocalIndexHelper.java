package net.osmand.plus.activities;


import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.Version;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.voice.JSMediaCommandPlayerImpl;
import net.osmand.plus.voice.JSTTSCommandPlayerImpl;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


public class LocalIndexHelper {

	private final OsmandApplication app;

	public LocalIndexHelper(OsmandApplication app) {
		this.app = app;
	}


	public String getInstalledDate(File f) {
		return android.text.format.DateFormat.getMediumDateFormat(app).format(getInstalationDate(f));
	}

	public Date getInstalationDate(File f) {
		final long t = f.lastModified();
		return new Date(t);
	}

	public String getInstalledDate(long t, TimeZone timeZone) {
		return android.text.format.DateFormat.getMediumDateFormat(app).format(new Date(t));
	}

	public void updateDescription(LocalIndexInfo info) {
		File f = new File(info.getPathToData());
		if (info.getType() == LocalIndexType.MAP_DATA) {
			Map<String, String> ifns = app.getResourceManager().getIndexFileNames();
			if (ifns.containsKey(info.getFileName())) {
				try {
					Date dt = app.getResourceManager().getDateFormat().parse(ifns.get(info.getFileName()));
					info.setDescription(getInstalledDate(dt.getTime(), null));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else {
				info.setDescription(getInstalledDate(f));
			}
		} else if (info.getType() == LocalIndexType.TILES_DATA) {
			ITileSource template;
			if (f.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(f)) {
				template = TileSourceManager.createTileSourceTemplate(new File(info.getPathToData()));
			} else if (f.isFile() && f.getName().endsWith(SQLiteTileSource.EXT)) {
				template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
			} else {
				return;
			}
			String descr = "";
			if (template.getExpirationTimeMinutes() >= 0) {
				descr += app.getString(R.string.local_index_tile_data_expire, String.valueOf(template.getExpirationTimeMinutes()));
			}
			info.setAttachedObject(template);
			info.setDescription(descr);
		} else if (info.getType() == LocalIndexType.SRTM_DATA) {
			info.setDescription(app.getString(R.string.download_srtm_maps));
		} else if (info.getType() == LocalIndexType.WIKI_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.TRAVEL_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.TTS_VOICE_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.DEACTIVATED) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.VOICE_DATA) {
			info.setDescription(getInstalledDate(f));
		} else if (info.getType() == LocalIndexType.FONT_DATA) {
			info.setDescription(getInstalledDate(f));
		}
	}

	private LocalIndexInfo getLocalIndexInfo(LocalIndexType type, String downloadName, boolean roadMap, boolean backuped) {

		File fileDir = null;
		String fileName = null;

		if (type == LocalIndexType.MAP_DATA) {
			if (!roadMap) {
				fileDir = app.getAppPath(IndexConstants.MAPS_PATH);
				fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
						+ IndexConstants.BINARY_MAP_INDEX_EXT;
			} else {
				fileDir = app.getAppPath(IndexConstants.ROADS_INDEX_DIR);
				fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
						+ IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
			}
		} else if (type == LocalIndexType.SRTM_DATA) {
			fileDir = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
					+ IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
		} else if (type == LocalIndexType.WIKI_DATA) {
			fileDir = app.getAppPath(IndexConstants.WIKI_INDEX_DIR);
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
					+ IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
		} else if (type == LocalIndexType.TRAVEL_DATA) {
			fileDir = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
					+ IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
		}

		if (backuped) {
			fileDir = app.getAppPath(IndexConstants.BACKUP_INDEX_DIR);
		}

		if (fileDir != null && fileName != null) {
			File f = new File(fileDir, fileName);
			if (f.exists()) {
				LocalIndexInfo info = new LocalIndexInfo(type, f, backuped, app);
				updateDescription(info);
				return info;
			}
		}

		return null;
	}

	public List<LocalIndexInfo> getLocalIndexInfos(String downloadName) {
		List<LocalIndexInfo> list = new ArrayList<>();
		LocalIndexInfo info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, false, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, true, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.SRTM_DATA, downloadName, false, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.WIKI_DATA, downloadName, false, false);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, false, true);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.MAP_DATA, downloadName, true, true);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.SRTM_DATA, downloadName, false, true);
		if (info != null) {
			list.add(info);
		}
		info = getLocalIndexInfo(LocalIndexType.WIKI_DATA, downloadName, false, true);
		if (info != null) {
			list.add(info);
		}

		return list;
	}

	public List<LocalIndexInfo> getLocalIndexData(AbstractLoadLocalIndexTask loadTask) {
		Map<String, String> loadedMaps = app.getResourceManager().getIndexFileNames();
		List<LocalIndexInfo> result = new ArrayList<>();

		loadObfData(app.getAppPath(IndexConstants.MAPS_PATH), result, false, loadTask, loadedMaps);
		loadObfData(app.getAppPath(IndexConstants.ROADS_INDEX_DIR), result, false, loadTask, loadedMaps);
		loadTilesData(app.getAppPath(IndexConstants.TILES_INDEX_DIR), result, false, loadTask);
		loadSrtmData(app.getAppPath(IndexConstants.SRTM_INDEX_DIR), result, loadTask);
		loadWikiData(app.getAppPath(IndexConstants.WIKI_INDEX_DIR), result, loadTask);
		loadTravelData(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), result, loadTask);
		//loadVoiceData(app.getAppPath(IndexConstants.TTSVOICE_INDEX_EXT_ZIP), result, true, loadTask);
		loadVoiceData(app.getAppPath(IndexConstants.VOICE_INDEX_DIR), result, false, loadTask);
		loadFontData(app.getAppPath(IndexConstants.FONT_INDEX_DIR), result, false, loadTask);
		loadObfData(app.getAppPath(IndexConstants.BACKUP_INDEX_DIR), result, true, loadTask, loadedMaps);

		return result;
	}

	public List<LocalIndexInfo> getLocalTravelFiles(AbstractLoadLocalIndexTask loadTask) {
		List<LocalIndexInfo> result = new ArrayList<>();
		loadTravelData(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR), result, loadTask);
		return result;
	}

	public List<LocalIndexInfo> getLocalFullMaps(AbstractLoadLocalIndexTask loadTask) {
		Map<String, String> loadedMaps = app.getResourceManager().getIndexFileNames();
		List<LocalIndexInfo> result = new ArrayList<>();
		loadObfData(app.getAppPath(IndexConstants.MAPS_PATH), result, false, loadTask, loadedMaps);

		return result;
	}

	private void loadVoiceData(File voiceDir, List<LocalIndexInfo> result, boolean backup, AbstractLoadLocalIndexTask loadTask) {
		if (voiceDir.canRead()) {
			//First list TTS files, they are preferred
			for (File voiceF : listFilesSorted(voiceDir)) {
				if (voiceF.isDirectory() && (JSTTSCommandPlayerImpl.isMyData(voiceF)
						|| TTSCommandPlayerImpl.isMyData(voiceF))) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TTS_VOICE_DATA, voiceF, backup, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);

				}
			}

			//Now list recorded voices
			for (File voiceF : listFilesSorted(voiceDir)) {
				if (voiceF.isDirectory() && (JSMediaCommandPlayerImpl.isMyData(voiceF)
						|| MediaCommandPlayerImpl.isMyData(voiceF))) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.VOICE_DATA, voiceF, backup, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	private void loadFontData(File fontDir, List<LocalIndexInfo> result, boolean backup, AbstractLoadLocalIndexTask loadTask) {
		if (fontDir.canRead()) {
			for (File fontFile : listFilesSorted(fontDir)) {
				if (fontFile.isFile() && fontFile.getName().endsWith(IndexConstants.FONT_INDEX_EXT)) {
					LocalIndexType lt = LocalIndexType.FONT_DATA;
					LocalIndexInfo info = new LocalIndexInfo(lt, fontFile, backup, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	private void loadTilesData(File tilesPath, List<LocalIndexInfo> result, boolean backup, AbstractLoadLocalIndexTask loadTask) {
		if (tilesPath.canRead()) {
			for (File tileFile : listFilesSorted(tilesPath)) {
				if (tileFile.isFile() && tileFile.getName().endsWith(SQLiteTileSource.EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				} else if (tileFile.isDirectory()) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup, app);

					if (!TileSourceManager.isTileSourceMetaInfoExist(tileFile)) {
						info.setCorrupted(true);
					}
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	private File[] listFilesSorted(File dir) {
		File[] listFiles = dir.listFiles();
		if (listFiles == null) {
			return new File[0];
		}
		Arrays.sort(listFiles);
		return listFiles;
	}


	private void loadSrtmData(File mapPath, List<LocalIndexInfo> result, AbstractLoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.SRTM_DATA, mapFile, false, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	private void loadWikiData(File mapPath, List<LocalIndexInfo> result, AbstractLoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.WIKI_DATA, mapFile, false, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	private void loadTravelData(File mapPath, List<LocalIndexInfo> result, AbstractLoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)
						|| (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
						&& Version.isDeveloperVersion(app))) { //todo remove when .travel.obf will be used in production
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TRAVEL_DATA, mapFile, false, app);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	private void loadObfData(File mapPath, List<LocalIndexInfo> result, boolean backup, AbstractLoadLocalIndexTask loadTask, Map<String, String> loadedMaps) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					String fileName = mapFile.getName();
					LocalIndexType lt = LocalIndexType.MAP_DATA;
					if (SrtmDownloadItem.isSrtmFile(fileName)) {
						lt = LocalIndexType.SRTM_DATA;
					} else if (fileName.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
						lt = LocalIndexType.WIKI_DATA;
					}
					LocalIndexInfo info = new LocalIndexInfo(lt, mapFile, backup, app);
					if (loadedMaps.containsKey(fileName) && !backup) {
						info.setLoaded(true);
					}
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}

	public enum LocalIndexType {
		MAP_DATA(R.string.local_indexes_cat_map, R.drawable.ic_map, 10),
		TILES_DATA(R.string.local_indexes_cat_tile, R.drawable.ic_map, 60),
		SRTM_DATA(R.string.local_indexes_cat_srtm, R.drawable.ic_plugin_srtm, 40),
		WIKI_DATA(R.string.local_indexes_cat_wiki, R.drawable.ic_plugin_wikipedia, 50),
		TRAVEL_DATA(R.string.download_maps_travel, R.drawable.ic_plugin_wikipedia, 60),
		TTS_VOICE_DATA(R.string.local_indexes_cat_tts, R.drawable.ic_action_volume_up, 20),
		VOICE_DATA(R.string.local_indexes_cat_voice, R.drawable.ic_action_volume_up, 30),
		FONT_DATA(R.string.fonts_header, R.drawable.ic_action_map_language, 35),
		DEACTIVATED(R.string.local_indexes_cat_backup, R.drawable.ic_type_archive, 1000);
//		AV_DATA(R.string.local_indexes_cat_av);

		@StringRes
		private final int resId;
		@DrawableRes
		private int iconResource;
		private final int orderIndex;

		LocalIndexType(@StringRes int resId, @DrawableRes int iconResource, int orderIndex) {
			this.resId = resId;
			this.iconResource = iconResource;
			this.orderIndex = orderIndex;
		}

		public String getHumanString(Context ctx) {
			return ctx.getString(resId);
		}

		public int getIconResource() {
			return iconResource;
		}

		public int getOrderIndex(LocalIndexInfo info) {
			String fileName = info.getFileName();
			int index = info.getOriginalType().orderIndex;
			if (info.getType() == DEACTIVATED) {
				index += DEACTIVATED.orderIndex;
			}
			if (fileName.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
				index++;
			}
			return index;
		}

		public String getBasename(LocalIndexInfo localIndexInfo) {
			String fileName = localIndexInfo.getFileName();
			if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
				return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
			}
			if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length());
			}
			if (localIndexInfo.getType() == TRAVEL_DATA &&
					fileName.endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
				return fileName.substring(0, fileName.length() - IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT.length());
			}
			if (this == VOICE_DATA) {
				int l = fileName.lastIndexOf('_');
				if (l == -1) {
					l = fileName.length();
				}
				return fileName.substring(0, l);
			}
			if (this == FONT_DATA) {
				int l = fileName.indexOf('.');
				if (l == -1) {
					l = fileName.length();
				}
				return fileName.substring(0, l).replace('_', ' ').replace('-', ' ');
			}
			int ls = fileName.lastIndexOf('_');
			if (ls >= 0) {
				return fileName.substring(0, ls);
			} else if (fileName.indexOf('.') > 0) {
				return fileName.substring(0, fileName.indexOf('.'));
			}
			return fileName;
		}
	}
}
