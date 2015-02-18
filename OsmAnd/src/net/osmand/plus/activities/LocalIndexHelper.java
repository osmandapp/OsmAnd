package net.osmand.plus.activities;


import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.download.LocalIndexesFragment.LoadLocalIndexTask;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import android.content.Context;
import android.os.Build;


public class LocalIndexHelper {
		
	private final OsmandApplication app;
	


	public LocalIndexHelper(OsmandApplication app){
		this.app = app;
	}
	
	
	public String getInstalledDate(File f){
		return getInstalledDateEdition(f.lastModified(), null);
	}
	
	public String getInstalledDateEdition(long t, TimeZone timeZone){
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		if(timeZone != null) {
			dateFormat.setTimeZone(timeZone);
		}
		return app.getString(R.string.local_index_installed) + " : " + dateFormat.format(new Date(t));
	}

	public String getInstalledDate(long t, TimeZone timeZone){
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		if(timeZone != null) {
			dateFormat.setTimeZone(timeZone);
		}
		return dateFormat.format(new Date(t));
	}

	public void updateDescription(LocalIndexInfo info){
		File f = new File(info.getPathToData());
		if(info.getType() == LocalIndexType.MAP_DATA){
			Map<String, String> ifns = app.getResourceManager().getIndexFileNames();
			if(ifns.containsKey(info.getFileName())) {
				info.setDescription(ifns.get(info.getFileName()));
			} else {
				info.setDescription(getInstalledDate(f));
			}
		} else if(info.getType() == LocalIndexType.VOICE_DATA){
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.TTS_VOICE_DATA){
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.TILES_DATA){
			ITileSource template ;
			if(f.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(f)){
				template = TileSourceManager.createTileSourceTemplate(new File(info.getPathToData()));
			} else if(f.isFile() && f.getName().endsWith(SQLiteTileSource.EXT)){
				template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
			} else {
				return;
			}
			String descr = "";
			descr += app.getString(R.string.local_index_tile_data_name, template.getName());
			if(template.getExpirationTimeMinutes() >= 0) {
				descr += "\n" + app.getString(R.string.local_index_tile_data_expire, template.getExpirationTimeMinutes());
			}
			info.setDescription(descr);
		}
	}


	public List<LocalIndexInfo> getLocalIndexData(LoadLocalIndexTask loadTask){
		Map<String, String> loadedMaps = app.getResourceManager().getIndexFileNames();
		List<LocalIndexInfo> result = new ArrayList<LocalIndexInfo>();
		
		loadObfData(app.getAppPath(IndexConstants.MAPS_PATH), result, false, loadTask, loadedMaps);
		loadObfData(app.getAppPath(IndexConstants.BACKUP_INDEX_DIR), result, true, loadTask, loadedMaps);
		loadTilesData(app.getAppPath(IndexConstants.TILES_INDEX_DIR), result, false, loadTask);
		loadSrtmData(app.getAppPath(IndexConstants.SRTM_INDEX_DIR), result, loadTask);
		loadVoiceData(app.getAppPath(IndexConstants.VOICE_INDEX_DIR), result, false, loadTask);
		loadVoiceData(app.getAppPath(IndexConstants.TTSVOICE_INDEX_EXT_ZIP), result, true, loadTask);
		
		return result;
	}
	

	private void loadVoiceData(File voiceDir, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (voiceDir.canRead()) {
			for (File voiceF : listFilesSorted(voiceDir)) {
				if (voiceF.isDirectory()) {
					LocalIndexInfo info = null;
					if (MediaCommandPlayerImpl.isMyData(voiceF)) {
						info = new LocalIndexInfo(LocalIndexType.VOICE_DATA, voiceF, backup);
					} else if (Build.VERSION.SDK_INT >= 4) {
						if (TTSCommandPlayerImpl.isMyData(voiceF)) {
							info = new LocalIndexInfo(LocalIndexType.TTS_VOICE_DATA, voiceF, backup);
						}
					}
					if(info != null){
						updateDescription(info);
						result.add(info);
						loadTask.loadFile(info);
					}
				}
			}
		}
	}
	
	private void loadTilesData(File tilesPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (tilesPath.canRead()) {
			for (File tileFile : listFilesSorted(tilesPath)) {
				if (tileFile.isFile() && tileFile.getName().endsWith(SQLiteTileSource.EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				} else if (tileFile.isDirectory()) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup);
					
					if(!TileSourceManager.isTileSourceMetaInfoExist(tileFile)){
						info.setCorrupted(true);
					}
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}
	
	private File[] listFilesSorted(File dir){
		File[] listFiles = dir.listFiles();
		if(listFiles == null) {
			return new File[0];
		}
		Arrays.sort(listFiles);
		return listFiles;
	}

	
	private void loadSrtmData(File mapPath, List<LocalIndexInfo> result, LoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.SRTM_DATA, mapFile, false);
					updateDescription(info);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}
	
	private void loadObfData(File mapPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask, Map<String, String> loadedMaps) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					boolean srtm = mapFile.getName().endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT);
					LocalIndexInfo info = new LocalIndexInfo(srtm ? LocalIndexType.SRTM_DATA :LocalIndexType.MAP_DATA, mapFile, backup);
					if(loadedMaps.containsKey(mapFile.getName()) && !backup){
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
		MAP_DATA(R.string.local_indexes_cat_map),
		TILES_DATA(R.string.local_indexes_cat_tile),
		SRTM_DATA(R.string.local_indexes_cat_srtm),
		VOICE_DATA(R.string.local_indexes_cat_voice),
		TTS_VOICE_DATA(R.string.local_indexes_cat_tts);
//		AV_DATA(R.string.local_indexes_cat_av);;
		
		private final int resId;

		private LocalIndexType(int resId){
			this.resId = resId;
			
		}
		public String getHumanString(Context ctx){
			return ctx.getString(resId);
		}
	}
	
	


}
