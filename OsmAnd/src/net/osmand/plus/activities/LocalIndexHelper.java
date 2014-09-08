package net.osmand.plus.activities;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.download.LocalIndexesFragment.LoadLocalIndexTask;
import net.osmand.plus.voice.MediaCommandPlayerImpl;
import net.osmand.plus.voice.TTSCommandPlayerImpl;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.os.Build;


public class LocalIndexHelper {
		
	private final OsmandApplication app;
	


	public LocalIndexHelper(OsmandApplication app){
		this.app = app;
	}
	
	
	public String getInstalledDate(File f){
		return getInstalledDate(f.lastModified(), null);
	}
	
	public String getInstalledDate(long t, TimeZone timeZone){
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		if(timeZone != null) {
			dateFormat.setTimeZone(timeZone);
		}
		return app.getString(R.string.local_index_installed) + " : " + dateFormat.format(new Date(t));
	}
	
	public void updateDescription(LocalIndexInfo info){
		File f = new File(info.getPathToData());
		if(info.getType() == LocalIndexType.MAP_DATA){
			updateObfFileInformation(info, f);
		} else if(info.getType() == LocalIndexType.VOICE_DATA){
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.TTS_VOICE_DATA){
			info.setDescription(getInstalledDate(f));
		} else if(info.getType() == LocalIndexType.TILES_DATA){
			Set<Integer> zooms = new TreeSet<Integer>();
			ITileSource template ;
			if(f.isDirectory() && TileSourceManager.isTileSourceMetaInfoExist(f)){
				template = TileSourceManager.createTileSourceTemplate(new File(info.getPathToData()));
				for(String s : f.list()){
					try {
						zooms.add(Integer.parseInt(s));
					} catch (NumberFormatException e) {
					}
				}
			} else if(f.isFile() && f.getName().endsWith(SQLiteTileSource.EXT)){
				template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
			} else {
				return;
			}
			String descr = "";
			descr += app.getString(R.string.local_index_tile_data_name, template.getName());
			descr += "\n" + app.getString(R.string.local_index_tile_data_minzoom, template.getMinimumZoomSupported());
			descr += "\n" + app.getString(R.string.local_index_tile_data_maxzoom, template.getMaximumZoomSupported());
			descr += "\n" + app.getString(R.string.local_index_tile_data_downloadable, template.couldBeDownloadedFromInternet());
			if(template.getExpirationTimeMinutes() >= 0) {
				descr += "\n" + app.getString(R.string.local_index_tile_data_expire, template.getExpirationTimeMinutes());
			}
			descr += "\n" + app.getString(R.string.local_index_tile_data_zooms, zooms.toString());
			info.setDescription(descr);
		} else {
			OsmandPlugin.onUpdateLocalIndexDescription(info);
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
					} else if (Build.VERSION.SDK_INT >= 4) {
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
					result.add(info);
					loadTask.loadFile(info);
				}
			}
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
			builder.append(getInstalledDate(reader.getDateCreated(), null));
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
