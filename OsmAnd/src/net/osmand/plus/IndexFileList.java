package net.osmand.plus;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.data.IndexConstants;
import net.osmand.plus.DownloadOsmandIndexesHelper.IndexItem;

/**
 * @author Pavol Zibrita <pavol.zibrita@gmail.com>
 */
public class IndexFileList implements Serializable {
	private static final long serialVersionUID = 1L;

	private boolean downloadedFromInternet = false;
	IndexItem basemap;
	TreeMap<String, IndexItem> indexFiles = new TreeMap<String, IndexItem>(new Comparator<String>(){
		
		@Override
		public int compare(String object1, String object2) {
			if(object1.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
				if(object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
					return object1.compareTo(object2);
				} else {
					return -1;
				}
			} else if(object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
				return 1;
			}
			return object1.compareTo(object2);
		}
	});
	
	private String mapversion;
	
	public IndexFileList() {
	}
	
	public void setDownloadedFromInternet(boolean downloadedFromInternet) {
		this.downloadedFromInternet = downloadedFromInternet;
	}
	
	public boolean isDownloadedFromInternet() {
		return downloadedFromInternet;
	}

	public void setMapVersion(String mapversion) {
		this.mapversion = mapversion;
	}

	public void add(String name, IndexItem indexItem) {
		if (indexItem.isAccepted()) {
			indexFiles.put(name, indexItem);
		}
		if(name.toLowerCase().startsWith("world_basemap")) {
			basemap = indexItem;
		}
	}

	public boolean isAcceptable() {
		return (indexFiles != null && !indexFiles.isEmpty()) || (mapversion != null);
	}

	public Map<String, IndexItem> getIndexFiles() {
		return indexFiles;
	}
	
	public IndexItem getBasemap() {
		return basemap;
	}

	public boolean isIncreasedMapVersion() {
		try {
			int mapVersionInList = Integer.parseInt(mapversion);
			return IndexConstants.BINARY_MAP_VERSION < mapVersionInList;
		} catch (NumberFormatException e) {
			//ignore this...
		}
		return false;
	}
}
