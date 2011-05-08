package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Build;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;

public class LocalIndexHelper {
		
	private final OsmandApplication app;

	public LocalIndexHelper(OsmandApplication app){
		this.app = app;
	}
	
	public List<LocalIndexInfo> getAllLocalIndexData(){
		return getLocalIndexData(null);
	}
	
	public List<LocalIndexInfo> getLocalIndexData(LocalIndexType type){
		OsmandSettings settings = OsmandSettings.getOsmandSettings(app.getApplicationContext());
		List<LocalIndexInfo> result = new ArrayList<LocalIndexInfo>();
		
		loadVoiceData(settings.extendOsmandPath(ResourceManager.VOICE_PATH), result, false);
		loadVoiceData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true);
		
		return result;
	}
	
	
	private void loadVoiceData(File voiceDir, List<LocalIndexInfo> result, boolean backup) {
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
					}
				}
			}
		}
	}


	public enum LocalIndexType {
		VOICE_DATA,
		TTS_VOICE_DATA,
		TILES_DATA,
		GPX_DATA,
		MAP_DATA,
		;
	}
	
	public static class LocalIndexInfo {
		
		private LocalIndexType type;
		private String description;
		private String name;
		
		private boolean backupedData;
		private boolean corrupted = false;
		private boolean loaded;
		private String pathToData;
		private boolean singleFile;
		
		private LocalIndexInfo(LocalIndexType type, File f, boolean backuped){
			pathToData = f.getAbsolutePath();
			name = f.getName();
			this.type = type;
			singleFile = !f.isDirectory();
			this.loaded = backuped;
		}
		
		public void setCorrupted(boolean corrupted) {
			this.corrupted = corrupted;
			if(corrupted){
				this.loaded = false;
			}
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
