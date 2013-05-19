package net.osmand.plus.download;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.IndexConstants;

public class IndexFileList implements Serializable {
	private static final long serialVersionUID = 1L;

	private boolean downloadedFromInternet = false;
	IndexItem basemap;
	ArrayList<IndexItem> indexFiles = new ArrayList<IndexItem>();
	private String mapversion;
	
	private Comparator<IndexItem> comparator = new Comparator<IndexItem>(){
		@Override
		public int compare(IndexItem o1, IndexItem o2) {
			String object1 = o1.getFileName();
			String object2 = o2.getFileName();
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
	};
	
	public void setDownloadedFromInternet(boolean downloadedFromInternet) {
		this.downloadedFromInternet = downloadedFromInternet;
	}
	
	public boolean isDownloadedFromInternet() {
		return downloadedFromInternet;
	}

	public void setMapVersion(String mapversion) {
		this.mapversion = mapversion;
	}

	public void add(IndexItem indexItem) {
		if (indexItem.isAccepted()) {
			indexFiles.add(indexItem);
		}
		if(indexItem.getFileName().toLowerCase().startsWith("world_basemap")) {
			basemap = indexItem;
		}
	}
	
	public void sort(){
		Collections.sort(indexFiles, comparator);
	}

	public boolean isAcceptable() {
		return (indexFiles != null && !indexFiles.isEmpty()) || (mapversion != null);
	}

	public List<IndexItem> getIndexFiles() {
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

	public IndexItem getIndexFilesByName(String key) {
		for(IndexItem i : indexFiles) {
			if(i.getFileName().equals(key)) {
				return i;
			}
		}
		return null;
	}
}
