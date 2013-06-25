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

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryIndexPart;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.LocalIndexesActivity.LoadLocalIndexTask;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.os.Build;

import com.ibm.icu.text.DateFormat;

public class LocalIndexHelper {
		
	private final OsmandApplication app;
	

	private DateFormat dateFormat;

	public LocalIndexHelper(OsmandApplication app){
		this.app = app;
	}
	
	public String formatDate(long t) {
		if(dateFormat == null) {
			dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		}
		return dateFormat.format(new Date(t));
	}
	
	public String getInstalledDate(File f){
		return getInstalledDate(f.lastModified());
	}
	
	public String getInstalledDate(long t){
		return app.getString(R.string.local_index_installed) + " : " + formatDate(t);
	}
	
	public void updateDescription(LocalIndexInfo info){
		File f = new File(info.getPathToData());
		if(info.getType() == LocalIndexType.MAP_DATA){
			updateObfFileInformation(info, f);
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
				SQLiteTileSource template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
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
		} else {
			OsmandPlugin.onUpdateLocalIndexDescription(info);
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
			float totalDistance = 0;
			int totalTracks = 0;
			long startTime = Long.MAX_VALUE;
			long endTime = Long.MIN_VALUE;
			long timeSpan = 0;
			long timeMoving = 0;
			float totalDistanceMoving = 0;

			double diffElevationUp = 0;
			double diffElevationDown = 0;
			double totalElevation = 0;
			double minElevation = 99999;
			double maxElevation = 0;
			
			float maxSpeed = 0;
			int speedCount = 0;
			double totalSpeedSum = 0;

			float[] calculations = new float[1];

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

						double elevation = point.ele;
						if (!Double.isNaN(elevation)) {
							totalElevation += elevation;
							minElevation = Math.min(elevation, minElevation);
							maxElevation = Math.max(elevation, maxElevation);
						}

						float speed = (float) point.speed;
						if(speed > 0){
							totalSpeedSum += speed;
							maxSpeed = Math.max(speed, maxSpeed);
							speedCount ++;
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

							//totalDistance += MapUtils.getDistance(prev.lat, prev.lon, point.lat, point.lon);
							// using ellipsoidal 'distanceBetween' instead of spherical haversine (MapUtils.getDistance) is a little more exact, also seems slightly faster:
							net.osmand.Location.distanceBetween(prev.lat, prev.lon, point.lat, point.lon, calculations);
							totalDistance += calculations[0];

							// Averaging speed values is less exact than totalDistance/timeMoving
							if(speed > 0 && point.time != 0 && prev.time != 0){
								timeMoving = timeMoving + (point.time - prev.time);
								totalDistanceMoving += calculations[0];
							}
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

			// OUTPUT:
			// 1. Total distance, Start time, End time
			info.setDescription(app.getString(R.string.local_index_gpx_info, totalTracks, points,
					result.points.size(), OsmAndFormatter.getFormattedDistance(totalDistance, app),
					startTime, endTime));

			// 2. Time span
			timeSpan = endTime - startTime;
			info.setDescription(info.getDescription() + app.getString(R.string.local_index_gpx_timespan, (int) ((timeSpan / 1000) / 3600), (int) (((timeSpan / 1000) / 60) % 60), (int) ((timeSpan / 1000) % 60)));

			// 3. Time moving, if any
			if(timeMoving > 0){
				info.setDescription(info.getDescription() +
					app.getString(R.string.local_index_gpx_timemoving, (int) ((timeMoving / 1000) / 3600), (int) (((timeMoving / 1000) / 60) % 60), (int) ((timeMoving / 1000) % 60)));
			}

			// 4. Elevation, eleUp, eleDown, if recorded
			if(totalElevation != 0 || diffElevationUp != 0 || diffElevationDown != 0){
				info.setDescription(info.getDescription() +  
						app.getString(R.string.local_index_gpx_info_elevation,
						OsmAndFormatter.getFormattedAlt(totalElevation / points, app),
						OsmAndFormatter.getFormattedAlt(minElevation, app),
						OsmAndFormatter.getFormattedAlt(maxElevation, app),
						OsmAndFormatter.getFormattedAlt(diffElevationUp, app),
						OsmAndFormatter.getFormattedAlt(diffElevationDown, app)));
			}

			// 5. Max speed and Average speed, if any. Average speed is NOT overall (effective) speed, but only calculated for "moving" periods.
			if(speedCount > 0){
				if(timeMoving > 0){
					info.setDescription(info.getDescription() +
						app.getString(R.string.local_index_gpx_info_speed,
						OsmAndFormatter.getFormattedSpeed((float) (totalDistanceMoving / timeMoving * 1000), app),
						OsmAndFormatter.getFormattedSpeed(maxSpeed, app)));
						// (Use totalDistanceMoving instead of totalDistance for av-speed to ignore effect of position fluctuations at rest)
				} else {
					// Averaging speed values is less exact than totalDistance/timeMoving
					info.setDescription(info.getDescription() +
						app.getString(R.string.local_index_gpx_info_speed,
						OsmAndFormatter.getFormattedSpeed((float) (totalSpeedSum / speedCount), app),
						OsmAndFormatter.getFormattedSpeed(maxSpeed, app)));
				}
			}

			// 6. 'Long-press for options' message
			info.setDescription(info.getDescription() +  
						app.getString(R.string.local_index_gpx_info_show));
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
		loadGPXData(app.getAppPath(IndexConstants.GPX_INDEX_DIR), result, false, loadTask);
		OsmandPlugin.onLoadLocalIndexes(result, loadTask);
		
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

	
	private void loadSrtmData(File mapPath, List<LocalIndexInfo> result, LoadLocalIndexTask loadTask) {
		if (mapPath.canRead()) {
			for (File mapFile : listFilesSorted(mapPath)) {
				if (mapFile.isFile() && mapFile.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.SRTM_DATA, mapFile, false);
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
			List<LocalIndexInfo> progress = new ArrayList<LocalIndexInfo>();
			loadGPXFolder(mapPath, result, backup, loadTask, progress, null);
			if (!progress.isEmpty()) {
				loadTask.loadFile(progress.toArray(new LocalIndexInfo[progress.size()]));
			}
		}
	}

	private void loadGPXFolder(File mapPath, List<LocalIndexInfo> result, boolean backup, LoadLocalIndexTask loadTask,
			List<LocalIndexInfo> progress, String gpxSubfolder) {
		for (File gpxFile : listFilesSorted(mapPath)) {
			if (gpxFile.isDirectory()) {
				String sub = gpxSubfolder == null ? gpxFile.getName() : gpxSubfolder + "/" + gpxFile.getName();
				loadGPXFolder(gpxFile, result, backup, loadTask, progress, sub);
			} else if (gpxFile.isFile() && gpxFile.getName().endsWith(".gpx")) {
				LocalIndexInfo info = new LocalIndexInfo(LocalIndexType.GPX_DATA, gpxFile, backup);
				info.setSubfolder(gpxSubfolder);
				result.add(info);
				progress.add(info);
				if (progress.size() > 7) {
					loadTask.loadFile(progress.toArray(new LocalIndexInfo[progress.size()]));
					progress.clear();
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
			BinaryMapIndexReader reader = new BinaryMapIndexReader(mf);
			
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
				} else if(part instanceof RouteRegion){
					RouteRegion mi = ((RouteRegion) part);
					builder.append(app.getString(R.string.local_index_routing_data)).append(": ").
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
			builder.append(getInstalledDate(reader.getDateCreated()));
			info.setDescription(builder.toString());
			reader.close();
		} catch (IOException e) {
			info.setCorrupted(true);
		}
		
	}



	public enum LocalIndexType {
		MAP_DATA(R.string.local_indexes_cat_map),
		TILES_DATA(R.string.local_indexes_cat_tile),
		SRTM_DATA(R.string.local_indexes_cat_srtm),
		VOICE_DATA(R.string.local_indexes_cat_voice),
		TTS_VOICE_DATA(R.string.local_indexes_cat_tts),
		GPX_DATA(R.string.local_indexes_cat_gpx),
		AV_DATA(R.string.local_indexes_cat_av);;
		
		private final int resId;

		private LocalIndexType(int resId){
			this.resId = resId;
			
		}
		public String getHumanString(Context ctx){
			return ctx.getString(resId);
		}
	}
	
	


}
