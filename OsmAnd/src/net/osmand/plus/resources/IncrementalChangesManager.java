package net.osmand.plus.resources;

import android.view.LayoutInflater;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class IncrementalChangesManager {
	private static final Log LOG = PlatformUtil.getLog(IncrementalChangesManager.class);
	private static final String URL = "https://osmand.net/check_live";
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(IncrementalChangesManager.class);
	private ResourceManager resourceManager;
	private final Map<String, RegionUpdateFiles> regions = new ConcurrentHashMap<String, IncrementalChangesManager.RegionUpdateFiles>();
	
	
	public IncrementalChangesManager(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}
	
	public List<File> collectChangesFiles(File dir, String ext, List<File> files) {
		if (dir.exists() && dir.canRead()) {
			File[] lf = dir.listFiles();
			if (lf == null || lf.length == 0) {
				return files;
			}
			Set<String> existingFiles = new HashSet<String>();
			for (File f : files) {
				if(!f.getName().endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT) && 
						!f.getName().endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)) {
					existingFiles.add(Algorithms.getFileNameWithoutExtension(f));
				}
			}
			for (File f : lf) {
				if (f.getName().endsWith(ext)) {
					String index = Algorithms.getFileNameWithoutExtension(f);
					if (index.length() >= 9 || index.charAt(index.length() - 9) != '_') {
						String nm = index.substring(0, index.length() - 9);
						if (existingFiles.contains(nm)) {
							files.add(f);
						}
					}

				}
			}
		}
		return files;
	}

	public synchronized void indexMainMap(File f, long dateCreated) {
		String nm = Algorithms.getFileNameWithoutExtension(f).toLowerCase();
		RegionUpdateFiles regionUpdateFiles = regions.get(nm);
		if(regionUpdateFiles == null) {
			regionUpdateFiles = new RegionUpdateFiles(nm);
			regions.put(nm, regionUpdateFiles);
		}
		regionUpdateFiles.mainFile = f;
		regionUpdateFiles.mainFileInit = dateCreated;
		if (!regionUpdateFiles.monthUpdates.isEmpty()) {
			List<String> list = new ArrayList<String>(regionUpdateFiles.monthUpdates.keySet());
			for (String month : list) {
				RegionUpdate ru = regionUpdateFiles.monthUpdates.get(month);
				if (ru.obfCreated <= dateCreated) {
					log.info("Delete overlapping month update " + ru.file.getName());
					resourceManager.closeFile(ru.file.getName());
					regionUpdateFiles.monthUpdates.remove(month);
					ru.file.delete();
					log.info("Delete overlapping month update " + ru.file.getName());
				}
			}
		}
		if (!regionUpdateFiles.dayUpdates.isEmpty()) {
			ArrayList<String> list = new ArrayList<String>(regionUpdateFiles.dayUpdates.keySet());
			for (String month : list) {
				List<RegionUpdate> newList = new ArrayList<>(regionUpdateFiles.dayUpdates.get(month));
				Iterator<RegionUpdate> it = newList.iterator();
				RegionUpdate monthRu = regionUpdateFiles.monthUpdates.get(month);
				while (it.hasNext()) {
					RegionUpdate ru = it.next();
					if(ru == null) {
						continue;
					}
					if (ru.obfCreated <= dateCreated ||
							(monthRu != null && ru.obfCreated < monthRu.obfCreated)) {
						log.info("Delete overlapping day update " + ru.file.getName());
						resourceManager.closeFile(ru.file.getName());
						it.remove();
						ru.file.delete();
						log.info("Delete overlapping day update " + ru.file.getName());
					}
				}
				regionUpdateFiles.dayUpdates.put(month, newList);
			}
		}
	}
	
	public synchronized boolean index(File f, long dateCreated, BinaryMapIndexReader mapReader) {
		String index = Algorithms.getFileNameWithoutExtension(f).toLowerCase();
		if(index.length() <= 9 || index.charAt(index.length() - 9) != '_'){
			return false;
		}
		String nm = index.substring(0, index.length() - 9);
		String date = index.substring(index.length() - 9 + 1);
		RegionUpdateFiles regionUpdateFiles = regions.get(nm);
		if(regionUpdateFiles == null) {
			regionUpdateFiles = new RegionUpdateFiles(nm);
			regions.put(nm, regionUpdateFiles);
		}
		return regionUpdateFiles.addUpdate(date, f, dateCreated);
	}
	
	protected static String formatSize(long vl) {
		return (vl * 1000 / (1 << 20l)) / 1000.0f + "";
	}
	
	public static long calculateSize(List<IncrementalUpdate> list) {
		long l = 0;
		for(IncrementalUpdate iu : list) {
			l += iu.containerSize;
		}
		return l;
	}	
	
	protected class RegionUpdate {
		protected File file;
		protected String date;
		protected long obfCreated; 
	}
	
	protected class RegionUpdateFiles {
		protected String nm;
		protected File mainFile;
		protected long mainFileInit;
		TreeMap<String, List<RegionUpdate>> dayUpdates = new TreeMap<String, List<RegionUpdate>>();
		TreeMap<String, RegionUpdate> monthUpdates = new TreeMap<String, RegionUpdate>();
		
		public RegionUpdateFiles(String nm) {
			this.nm = nm;
		}
		
		public boolean addUpdate(String date, File file, long dateCreated) {
			String monthYear = date.substring(0, 5);
			RegionUpdate ru = new RegionUpdate();
			ru.date = date;
			ru.file = file;
			ru.obfCreated = dateCreated;
			if(date.endsWith("00")) {
				monthUpdates.put(monthYear, ru);
			} else {
				List<RegionUpdate> list = dayUpdates.get(monthYear);
				if (list == null) {
					list = new ArrayList<IncrementalChangesManager.RegionUpdate>();
				}
				list.add(ru);
				dayUpdates.put(monthYear, list);
			}
			return true;
		}

	}
	
	public class IncrementalUpdateList {
		public TreeMap<String, IncrementalUpdateGroupByMonth> updateByMonth = 
				new TreeMap<String, IncrementalUpdateGroupByMonth>();
		public String errorMessage;
		public RegionUpdateFiles updateFiles;
		
		
		public boolean isPreferrableLimitForDayUpdates(String monthYearPart, List<IncrementalUpdate> dayUpdates) {
			List<RegionUpdate> lst = updateFiles.dayUpdates.get(monthYearPart);
			if(lst == null || lst.size() < 10) {
				return true;
			}
			return false;
		}
		
		public List<IncrementalUpdate> getItemsForUpdate() {
			Iterator<IncrementalUpdateGroupByMonth> it = updateByMonth.values().iterator();
			List<IncrementalUpdate> ll = new ArrayList<IncrementalUpdate>();
			while(it.hasNext()) {
				IncrementalUpdateGroupByMonth n = it.next();
				if(it.hasNext()) {
					if(!n.isMonthUpdateApplicable()) {
						return null;			
					}
					ll.addAll(n.getMonthUpdate());
				} else {
					if(n.isDayUpdateApplicable() && isPreferrableLimitForDayUpdates(n.monthYearPart, n.getDayUpdates())) {
						ll.addAll(n.getDayUpdates());
					} else if(n.isMonthUpdateApplicable()) {
						ll.addAll(n.getMonthUpdate());
					} else {
						return null;
					}
				}
			}
			return ll;
		}

		public void addUpdate(IncrementalUpdate iu) {
			String dtMonth = iu.date.substring(0, 5);
			if(!updateByMonth.containsKey(dtMonth)) {
				IncrementalUpdateGroupByMonth iubm = new IncrementalUpdateGroupByMonth(dtMonth);
				updateByMonth.put(dtMonth, iubm);
			}
			IncrementalUpdateGroupByMonth mm = updateByMonth.get(dtMonth);
			if(iu.isMonth()) {
				mm.monthUpdate = iu;
			} else {
				mm.dayUpdates.add(iu);
			}			
		}
	}
	
	protected static class IncrementalUpdateGroupByMonth {
		public final String monthYearPart ;
		public List<IncrementalUpdate> dayUpdates = new ArrayList<IncrementalUpdate>();
		public IncrementalUpdate monthUpdate;
		
		public long calculateSizeMonthUpdates() {
			return calculateSize(getMonthUpdate());
		}
		
		public long calculateSizeDayUpdates() {
			return calculateSize(getDayUpdates());
		}
		
		public boolean isMonthUpdateApplicable() {
			return monthUpdate != null;
		}
		
		public boolean isDayUpdateApplicable() {
			boolean inLimits = dayUpdates.size() > 0 && dayUpdates.size() < 4;
			if(!inLimits) {
				return false;
			}
			return true;
		}
		
		public List<IncrementalUpdate> getMonthUpdate() {
			List<IncrementalUpdate> ll = new ArrayList<IncrementalUpdate>();
			if(monthUpdate == null) {
				return ll;
			}
			ll.add(monthUpdate);
			for(IncrementalUpdate iu : dayUpdates) {
				if(iu.timestamp > monthUpdate.timestamp) {
					ll.add(iu);
				}
			}
			return ll;
		}
		
		public List<IncrementalUpdate> getDayUpdates() {
			return dayUpdates;
		}
		
		public IncrementalUpdateGroupByMonth(String monthYearPart ) {
			this.monthYearPart = monthYearPart;
		}
	}

	public static class IncrementalUpdate {
		String date = "";
		public long timestamp;
		public String sizeText = "";
		public long containerSize;
		public long contentSize;
		public String fileName;

		public boolean isMonth() {
			return date.endsWith("00");
		}

		@Override
		public String toString() {
			return "Update " + fileName + " " + sizeText + " MB " + date + ", timestamp: " + timestamp;
		}
	}
	
	private List<IncrementalUpdate> getIncrementalUpdates(String file, long timestamp) throws IOException,
			XmlPullParserException {
		String url = URL + "?aosmc=true&timestamp=" + timestamp + "&file=" + URLEncoder.encode(file);

		HttpURLConnection conn = NetworkUtils.getHttpURLConnection(url);
		conn.setUseCaches(false);
		XmlPullParser parser = PlatformUtil.newXMLPullParser();
		InputStream is = conn.getInputStream();
		parser.setInput(is, "UTF-8");
		List<IncrementalUpdate> lst = new ArrayList<IncrementalUpdate>();
		int elements = 0;
		while (parser.next() != XmlPullParser.END_DOCUMENT) {
			if (parser.getEventType() == XmlPullParser.START_TAG) {
				elements ++;
				if (parser.getName().equals("update")) {
					IncrementalUpdate dt = new IncrementalUpdate();
					dt.date = parser.getAttributeValue("", "updateDate");
					dt.containerSize = Long.parseLong(parser.getAttributeValue("", "containerSize"));
					dt.contentSize = Long.parseLong(parser.getAttributeValue("", "contentSize"));
					dt.sizeText = parser.getAttributeValue("", "size");
					dt.timestamp = Long.parseLong(parser.getAttributeValue("", "timestamp"));
					dt.fileName = parser.getAttributeValue("", "name");
					lst.add(dt);
				}
			}
		}
		LOG.debug(String.format("Incremental updates: %s, updates %d (total %d)", url, lst.size(), elements));
		is.close();
		conn.disconnect();
		return lst;
	}
	
	

	public IncrementalUpdateList getUpdatesByMonth(String fileName) {
		IncrementalUpdateList iul = new IncrementalUpdateList();
		RegionUpdateFiles ruf = regions.get(fileName.toLowerCase());
		iul.updateFiles = ruf;
		if(ruf == null) {
			iul.errorMessage = resourceManager.getContext().getString(R.string.no_updates_available);
			return iul;
		}
		long timestamp = getTimestamp(ruf);
		try {
			List<IncrementalUpdate> lst = getIncrementalUpdates(fileName, timestamp);
			for(IncrementalUpdate iu : lst) {
				iul.addUpdate(iu);
			}
		} catch (Exception e) {
			iul.errorMessage = e.getMessage();
			e.printStackTrace();
			log.error(e.getMessage(), e);
		}
		return iul;
	}

	public long getUpdatesSize(String fileName){
		RegionUpdateFiles ruf = regions.get(fileName.toLowerCase());
		if(ruf == null) {
			return 0;
		}
		long size = 0;
		for (List<RegionUpdate> regionUpdates : ruf.dayUpdates.values()) {
			for (RegionUpdate regionUpdate : regionUpdates) {
				size += regionUpdate.file.length();
			}
		}
		for (RegionUpdate regionUpdate : ruf.monthUpdates.values()) {
			size += regionUpdate.file.length();
		}
		return size;
	}

	public void deleteUpdates(String fileName){
		RegionUpdateFiles ruf = regions.get(fileName.toLowerCase());
		if(ruf == null) {
			return;
		}
		for (List<RegionUpdate> regionUpdates : ruf.dayUpdates.values()) {
			for (RegionUpdate regionUpdate : regionUpdates) {
				boolean successful = Algorithms.removeAllFiles(regionUpdate.file);
				if (successful) {
					resourceManager.closeFile(regionUpdate.file.getName());
				}
			}
		}
		for (RegionUpdate regionUpdate : ruf.monthUpdates.values()) {
			boolean successful = Algorithms.removeAllFiles(regionUpdate.file);
			if (successful) {
				resourceManager.closeFile(regionUpdate.file.getName());
			}
		}
		ruf.dayUpdates.clear();
		ruf.monthUpdates.clear();
	}

	public long getTimestamp(String fileName) {
		RegionUpdateFiles ruf = regions.get(fileName.toLowerCase());
		if(ruf == null) {
			return System.currentTimeMillis();
		}
		return getTimestamp(ruf);
	}

	public long getMapTimestamp(String fileName) {
		RegionUpdateFiles ruf = regions.get(fileName.toLowerCase());
		if(ruf == null) {
			return System.currentTimeMillis();
		}
		return ruf.mainFileInit;
	}

	private long getTimestamp(RegionUpdateFiles ruf) {
		long timestamp = ruf.mainFileInit;
		for (RegionUpdate ru : ruf.monthUpdates.values()) {
			timestamp = Math.max(ru.obfCreated, timestamp);
		}
		for (List<RegionUpdate> l : ruf.dayUpdates.values()) {
			for (RegionUpdate ru : l) {
				timestamp = Math.max(ru.obfCreated, timestamp);
			}
		}
		return timestamp;
	}
}
