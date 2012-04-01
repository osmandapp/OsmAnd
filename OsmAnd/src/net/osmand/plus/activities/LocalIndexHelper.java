package net.osmand.plus.activities;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.OsmAndFormatter;
import net.osmand.binary.BinaryIndexPart;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.data.IndexConstants;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.MapUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.LocalIndexesActivity.LoadLocalIndexTask;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

public class LocalIndexHelper {
		
	private final OsmandApplication app;
	
	private MessageFormat dateformat = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US);

	public LocalIndexHelper(OsmandApplication app){
		this.app = app;
	}
	
	public List<LocalIndexInfo> getAllLocalIndexData(LoadLocalIndexTask loadTask){
		return getLocalIndexData(null, loadTask);
	}
	
	public String getInstalledDate(File f){
		return app.getString(R.string.local_index_installed) + " : " + dateformat.format(new Object[]{new Date(f.lastModified())});
	}
	
	public void updateDescription(LocalIndexInfo info){
		File f = new File(info.getPathToData());
		if(info.getType() == LocalIndexType.MAP_DATA){
			updateObfFileInformation(info, f);
			info.setDescription(info.getDescription() + getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.POI_DATA){
			checkPoiFileVersion(info, f);
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.GPX_DATA){
			updateGpxInfo(info, f);
		} else if(info.getType() == LocalIndexType.VOICE_DATA){
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.TTS_VOICE_DATA){
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.TILES_DATA){
			if(f.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(f)){
				TileSourceTemplate template = TileSourceManager.createTileSourceTemplate(new File(info.getPathToData()));
				Set<Integer> zooms = new TreeSet<Integer>();
				for(String s : f.list()){
					try {
						zooms.add(Integer.parseInt(s));
					} catch (NumberFormatException e) {
					}
				}
				
				String descr = app.getString(R.string.local_index_tile_data, 
					template.getName(), template.getMinimumZoomSupported(), template.getMaximumZoomSupported(),
					template.getUrlTemplate() != null, zooms.toString());
				info.setDescription(descr);
			} else if(f.isFile() && f.getName().endsWith(SQLiteTileSource.EXT)){
				SQLiteTileSource template = new SQLiteTileSource(f, TileSourceManager.getKnownSourceTemplates());
//				Set<Integer> zooms = new TreeSet<Integer>();
//				for(int i=1; i<22; i++){
//					if(template.exists(i)){
//						zooms.add(i);
//					}
//				}
				String descr = app.getString(R.string.local_index_tile_data, 
						template.getName(), template.getMinimumZoomSupported(), template.getMaximumZoomSupported(),
						template.couldBeDownloadedFromInternet(), "");
				info.setDescription(descr);
			}
		}
	}

	private void updateGpxInfo(LocalIndexInfo info, File f) {
		if(info.getGpxFile() == null){
			info.setGpxFile(GPXUtilities.loadGPXFile(app, f, true));
		}
		GPXFile result = info.getGpxFile();
		if(result.warning != null){
			info.setCorrupted(true);
			info.setDescription(result.warning);
		} else {
			int totalDistance = 0;
			int totalTracks = 0;
			long startTime = Long.MAX_VALUE;
			long endTime = Long.MIN_VALUE;
			
			double diffElevationUp = 0;
			double diffElevationDown = 0;
			double totalElevation = 0;
			double minElevation = 99999;
			double maxElevation = 0;
			
			float maxSpeed = 0;
			int speedCount = 0;
			double totalSpeedSum = 0;
			
			int points = 0;
			for(int i = 0; i< result.tracks.size() ; i++){
				Track subtrack = result.tracks.get(i);
				for(TrkSegment segment : subtrack.segments){
					totalTracks++;
					points += segment.points.size();
					for (int j = 0; j < segment.points.size(); j++) {
						WptPt point = segment.points.get(j);
						long time = point.time;
						if(time != 0){
							startTime = Math.min(startTime, time);
							endTime = Math.max(startTime, time);
						}
						float speed = (float) point.speed;
						if(speed > 0){
							totalSpeedSum += speed;
							maxSpeed = Math.max(speed, maxSpeed);
							speedCount ++;
						}
						
						double elevation = point.ele;
						if (!Double.isNaN(elevation)) {
							totalElevation += elevation;
							minElevation = Math.min(elevation, minElevation);
							maxElevation = Math.max(elevation, maxElevation);
						}
						if (j > 0) {
							WptPt prev = segment.points.get(j - 1);
							if (!Double.isNaN(point.ele) && !Double.isNaN(prev.ele)) {
								double diff = point.ele - prev.ele;
								if (diff > 0) {
									diffElevationUp += diff;
								} else {
									diffElevationDown -= diff;
								}
							}
							totalDistance += MapUtils.getDistance(prev.lat, prev.lon, point.lat, point.lon);
						}
					}
					
				}
				
				
			}
			if(startTime == Long.MAX_VALUE){
				startTime = f.lastModified();
			}
			if(endTime == Long.MIN_VALUE){
				endTime = f.lastModified();
			}
			
			info.setDescription(app.getString(R.string.local_index_gpx_info, totalTracks, points,
					result.points.size(), OsmAndFormatter.getFormattedDistance(totalDistance, app),
					startTime, endTime));
			if(totalElevation != 0 || diffElevationUp != 0 || diffElevationDown != 0){
				info.setDescription(info.getDescription() +  
						app.getString(R.string.local_index_gpx_info_elevation,
						totalElevation / points, minElevation, maxElevation, diffElevationUp, diffElevationDown));
			}
			if(speedCount > 0){
				info.setDescription(info.getDescription() +  
						app.getString(R.string.local_index_gpx_info_speed,
						OsmAndFormatter.getFormattedSpeed((float) (totalSpeedSum / speedCount), app),
						OsmAndFormatter.getFormattedSpeed(maxSpeed, app)));
				
			}
			
			info.setDescription(info.getDescription() +  
						app.getString(R.string.local_index_gpx_info_show));
		}
	}
	
	public List<LocalIndexInfo> getLocalIndexData(LocalIndexType type, LoadLocalIndexTask loadTask){
		OsmandSettings settings = OsmandApplication.getSettings();
		Map<String, String> loadedMaps = app.getResourceManager().getIndexFileNames();
		List<LocalIndexInfo> result = new ArrayList<LocalIndexInfo>();
		
		loadTilesData(settings.extendOsmandPath(ResourceManager.TILES_PATH), result, false, loadTask);
		loadTilesData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, false, loadTask);
		loadObfData(settings.extendOsmandPath(ResourceManager.MAPS_PATH), result, false, loadTask, loadedMaps);
		loadObfData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask, loadedMaps);
		loadPoiData(settings.extendOsmandPath(ResourceManager.POI_PATH), result, false, loadTask);
		loadPoiData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask);
		loadVoiceData(settings.extendOsmandPath(ResourceManager.VOICE_PATH), result, false, loadTask);
		loadVoiceData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask);
		loadGPXData(settings.extendOsmandPath(ResourceManager.GPX_PATH), result, false, loadTask);
		loadGPXData(settings.extendOsmandPath(ResourceManager.BACKUP_PATH), result, true, loadTask);
		
		
		return result;
	}
	
	

	private void loadVoiceData(File voiceDir, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (voiceDir.canRead()) {
			for (File voiceF : listFilesSorted(voiceDir)) {
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
	
	private void loadTilesData(File tilesPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (tilesPath.canRead()) {
			for (File tileFile : listFilesSorted(tilesPath)) {
				if (tileFile.isFile() && tileFile.getName().endsWith(SQLiteTileSource.EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup);
					result.add(info);
					loadTask.loadFile(info);
				} else if (tileFile.isDirectory()) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.TILES_DATA, tileFile, backup);
					
					if(!TileSourceManager.isTileSourceMetaInfoExist(tileFile)){
						info.setCorrupted(true);
					}
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
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.MAP_DATA, mapFile, backup);
					if(loadedMaps.containsKey(mapFile.getName()) && !backup){
						info.setLoaded(true);
					}
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}
	
	private void loadGPXData(File mapPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File gpxFile : listFilesSorted(mapPath)) {
				if (gpxFile.isFile() && gpxFile.getName().endsWith(".gpx")) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.GPX_DATA, gpxFile, backup);
					result.add(info);
					loadTask.loadFile(info);
				}
			}
		}
	}
	
	private File[] listFilesSorted(File dir){
		File[] listFiles = dir.listFiles();
		Arrays.sort(listFiles);
		return listFiles;
	}
	
	private void loadPoiData(File mapPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File poiFile : listFilesSorted(mapPath)) {
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
			db.close();
		} catch(RuntimeException e){
			info.setCorrupted(true);
		}
		
	}
	
	private MessageFormat format = new MessageFormat("\t {0}, {1} NE \n\t {2}, {3} NE", Locale.US);

	private String formatLatLonBox(int left, int right, int top, int bottom) {
		double l = MapUtils.get31LongitudeX(left);
		double r = MapUtils.get31LongitudeX(right);
		double t = MapUtils.get31LatitudeY(top);
		double b = MapUtils.get31LatitudeY(bottom);
		return format.format(new Object[] { l, t, r, b });
	}
	
	private String formatLatLonBox(double l, double r, double t, double b) {
		return format.format(new Object[] { l, t, r, b });
	}

	private void updateObfFileInformation(LocalIndexInfo info, File mapFile) {
		try {
			RandomAccessFile mf = new RandomAccessFile(mapFile, "r");
			BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, false);
			
			info.setNotSupported(reader.getVersion() != IndexConstants.BINARY_MAP_VERSION);
			List<BinaryIndexPart> indexes = reader.getIndexes();
			StringBuilder builder = new StringBuilder();
			for(BinaryIndexPart part : indexes){
				if(part instanceof MapIndex){
					MapIndex mi = ((MapIndex) part);
					builder.append(app.getString(R.string.local_index_map_data)).append(": ").
						append(mi.getName()).append("\n");
					if(mi.getRoots().size() > 0){
						MapRoot mapRoot = mi.getRoots().get(0);
						String box = formatLatLonBox(mapRoot.getLeft(), mapRoot.getRight(), mapRoot.getTop(), mapRoot.getBottom());
						builder.append(box).append("\n");
					}
				} else if(part instanceof PoiRegion){
					PoiRegion mi = ((PoiRegion) part);
					builder.append(app.getString(R.string.local_index_poi_data)).append(": ").
						append(mi.getName()).append("\n");
					String box = formatLatLonBox(mi.getLeftLongitude(), mi.getRightLongitude(), 
							mi.getTopLatitude(), mi.getBottomLatitude());
					builder.append(box).append("\n");
				} else if(part instanceof TransportIndex){
					TransportIndex mi = ((TransportIndex) part);
					int sh = (31 - BinaryMapIndexReader.TRANSPORT_STOP_ZOOM);
					builder.append(app.getString(R.string.local_index_transport_data)).append(": ").
						append(mi.getName()).append("\n");
					String box = formatLatLonBox(mi.getLeft() << sh, mi.getRight() << sh, mi.getTop() << sh, mi.getBottom() << sh);
					builder.append(box).append("\n");
				} else if(part instanceof AddressRegion){
					AddressRegion mi = ((AddressRegion) part);
					builder.append(app.getString(R.string.local_index_address_data)).append(": ").
						append(mi.getName()).append("\n");
				}
			}
			info.setDescription(builder.toString());
			reader.close();
		} catch (IOException e) {
			info.setCorrupted(true);
		}
		
	}



	public enum LocalIndexType {
		TILES_DATA(R.string.local_indexes_cat_tile),
		MAP_DATA(R.string.local_indexes_cat_map),
		POI_DATA(R.string.local_indexes_cat_poi),
		VOICE_DATA(R.string.local_indexes_cat_voice),
		TTS_VOICE_DATA(R.string.local_indexes_cat_tts),
		GPX_DATA(R.string.local_indexes_cat_gpx);
		
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
		private String description = "";
		private String name;
		
		private boolean backupedData;
		private boolean corrupted = false;
		private boolean notSupported = false;
		private boolean loaded;
		private String pathToData;
		private String fileName;
		private boolean singleFile;
		private int kbSize = -1;
		
		// UI state expanded
		private boolean expanded;
		
		private GPXFile gpxFile;
		
		public LocalIndexInfo(LocalIndexType type, File f, boolean backuped){
			pathToData = f.getAbsolutePath();
			fileName = f.getName();
			name = formatName(f.getName());
			this.type = type;
			singleFile = !f.isDirectory();
			if(singleFile){
				kbSize = (int) (f.length() >> 10);
			}
			this.backupedData = backuped;
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
		
		public void setBackupedData(boolean backupedData) {
			this.backupedData = backupedData;
		}
		
		public void setSize(int size) {
			this.kbSize = size;
		}
		
		public void setGpxFile(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
		}
		
		public GPXFile getGpxFile() {
			return gpxFile;
		}
		
		public boolean isExpanded() {
			return expanded;
		}
		
		public void setExpanded(boolean expanded) {
			this.expanded = expanded;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public void setLoaded(boolean loaded) {
			this.loaded = loaded;
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
		
		public String getFileName() {
			return fileName;
		}
		
	}


}
