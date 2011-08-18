package net.osmand.plus.activities;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.IndexConstants;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.LocalIndexesActivity.LoadLocalIndexTask;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;

public class LocalIndexHelper {
		
	private final OsmandApplication app;

	public LocalIndexHelper(OsmandApplication app){
		this.app = app;
	}
	
	public List<LocalIndexInfo> getAllLocalIndexData(LoadLocalIndexTask loadTask){
		return getLocalIndexData(null, loadTask);
	}
	
	public List<LocalIndexInfo> getLocalIndexData(LocalIndexType type, LoadLocalIndexTask loadTask){
		OsmandSettings settings = OsmandSettings.getOsmandSettings(app.getApplicationContext());
		List<LocalIndexInfo> result = new ArrayList<LocalIndexInfo>();
		
		loadVoiceData(settings.extendOsmandPath(ResourceManager.VOICE_PATH), result, false, loadTask);
		loadObfData(settings.extendOsmandPath(ResourceManager.MAPS_PATH), result, false, loadTask);
		loadPoiData(settings.extendOsmandPath(ResourceManager.POI_PATH), result, false, loadTask);
		
		loadVoiceData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask);
		loadObfData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask);
		loadPoiData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask);
		
		return result;
	}
	
	
	private void loadVoiceData(File voiceDir, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (voiceDir.canRead()) {
			for (File voiceF : voiceDir.listFiles()) {
				if (voiceF.isDirectory()) {
					LocalIndexInfo info = null;
					if (MediaCommandPlayerImpl.isMyData(voiceF)) {
						info = new LocalIndexInfo(LocalIndexType.VOICE_DATA, voiceF, backup);
					} else if (Integer.parseInt(Build.VERSION.SDK) >= 4) {
						if (TTSCommandPlayerImpl.isMyData(voiceF)) {
							info = new LocalIndexInfo(LocalIndexType.TTS_VOICE_DATA, voiceF, backup);
						}
					}
					if(info != null){
						result.add(info);
						loadTask.loadFile(info);
					}
				}
			}
		}
	}
	
	private void loadObfData(File mapPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File mapFile : mapPath.listFiles()) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.MAP_DATA, mapFile, backup);
					if (!backup) {
						checkObfFileVersion(info, mapFile);
					}
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}
	
	private void loadPoiData(File mapPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File poiFile : mapPath.listFiles()) {
				if (poiFile.isFile() && poiFile.getName().endsWith(IndexConstants.POI_INDEX_EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.POI_DATA, poiFile, backup);
					if (!backup) {
						checkPoiFileVersion(info, poiFile);
					}
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}
	


	private void checkPoiFileVersion(LocalIndexInfo info, File poiFile) {
		try {
			SQLiteDatabase db = SQLiteDatabase.openDatabase(poiFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
			int version = db.getVersion();
			info.setNotSupported(version != IndexConstants.POI_TABLE_VERSION);
		} catch(RuntimeException e){
			info.setCorrupted(true);
		}
		
	}

	private void checkObfFileVersion(LocalIndexInfo info, File mapFile) {
		try {
			RandomAccessFile mf = new RandomAccessFile(mapFile, "r");
			BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, true);
			info.setNotSupported(reader.getVersion() != IndexConstants.BINARY_MAP_VERSION);
		} catch (IOException e) {
			info.setCorrupted(true);
		}
		
	}



	public enum LocalIndexType {
		VOICE_DATA(R.string.local_indexes_cat_voice),
		TTS_VOICE_DATA(R.string.local_indexes_cat_tts),
		TILES_DATA(R.string.local_indexes_cat_tile),
		GPX_DATA(R.string.local_indexes_cat_gpx),
		MAP_DATA(R.string.local_indexes_cat_map),
		POI_DATA(R.string.local_indexes_cat_poi);
		
		private final int resId;

		private LocalIndexType(int resId){
			this.resId = resId;
			
		}
		public String getHumanString(Context ctx){
			return ctx.getString(resId);
		}
	}
	
	public static class LocalIndexInfo {
		
		private LocalIndexType type;
		private String description;
		private String name;
		
		private boolean backupedData;
		private boolean corrupted = false;
		private boolean notSupported = false;
		private boolean loaded;
		private String pathToData;
		private boolean singleFile;
		private int kbSize;
		
		public LocalIndexInfo(LocalIndexType type, File f, boolean backuped){
			pathToData = f.getAbsolutePath();
			name = formatName(f.getName());
			this.type = type;
			singleFile = !f.isDirectory();
			if(singleFile){
				kbSize = (int) (f.length() >> 10);
			}
			this.loaded = backuped;
		}
		
		private String formatName(String name) {
			int ext = name.indexOf('.');
			if(ext != -1){
				name = name.substring(0, ext);
			}
			return name.replace('_', ' ');
		}

		// Special domain object represents category
		public LocalIndexInfo(LocalIndexType type, boolean backup){
			this.type = type;
			backupedData = backup;
		}
		
		public void setCorrupted(boolean corrupted) {
			this.corrupted = corrupted;
			if(corrupted){
				this.loaded = false;
			}
		}
		
		public void setSize(int size) {
			this.kbSize = size;
		}
		
		
		public void setNotSupported(boolean notSupported) {
			this.notSupported = notSupported;
			if(notSupported){
				this.loaded = false;
			}
		}
		
		public int getSize() {
			return kbSize;
		}
		
		public boolean isNotSupported() {
			return notSupported;
		}
		
		
		public String getName() {
			return name;
		}
		
		public LocalIndexType getType() {
			return type;
		}

		public boolean isSingleFile() {
			return singleFile;
		}
		
		public boolean isLoaded() {
			return loaded;
		}
		
		public boolean isCorrupted() {
			return corrupted;
		}
		
		public boolean isBackupedData() {
			return backupedData;
		}
		
		public String getPathToData() {
			return pathToData;
		}
		
		public String getDescription() {
			return description;
		}
		
	}


}
