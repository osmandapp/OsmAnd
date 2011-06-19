package net.osmand.osm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.osmand.data.MapAlgorithms;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.osm.io.OsmStorageWriter;

import org.xml.sax.SAXException;

public class FixLinkedCoastline {
	
	public static void main(String[] args) throws IOException, SAXException, XMLStreamException {
		String fileToRead = args != null && args.length > 0 ? args[0] : null; 
		if(fileToRead == null) {
			fileToRead = "/home/victor/projects/OsmAnd/download/basemap/10m_coastline.osm";
		}
		File read = new File(fileToRead);
		File write ;
		String fileToWrite =  args != null && args.length > 1 ? args[1] : null;
		if(fileToWrite != null){
			write = new File(fileToWrite);
			 
		} else {
			String fileName = read.getName();
			int i = fileName.lastIndexOf('.');
			fileName = fileName.substring(0, i) + "_out"+ fileName.substring(i);
			write = new File(read.getParentFile(), fileName);
		}
		
		write.createNewFile();
		
		process(read, write);
	}
	
	private static void process(File read, File write) throws  IOException, SAXException, XMLStreamException {
		OsmBaseStorage storage = new OsmBaseStorage();
		storage.parseOSM(new FileInputStream(read), new ConsoleProgressImplementation());
		
		Map<EntityId, Entity> entities = new HashMap<EntityId, Entity>( storage.getRegisteredEntities());
		List<EntityId> toWrite = new ArrayList<EntityId>(); 
		
		for(EntityId e : entities.keySet()){
			if(e.getType() == EntityType.WAY){
				Entity oldWay = storage.getRegisteredEntities().remove(e);
				List<Way> result = processWay((Way) oldWay);
				alignAndAddtoStorage(storage, toWrite, result);
			}
		}
		
		System.out.println("ERROR Ways : ");
		int errors = 0;
		for(List<Way> w : endWays.values()){
			Way way = w.get(0);
			Way lway = w.get(w.size() - 1);
			LatLon first = way.getNodes().get(0).getLatLon();
			LatLon last = lway.getNodes().get(lway.getNodes().size() - 1).getLatLon();
			double dist = MapUtils.getDistance(first, last);
			if(dist < 500 && w.size() >= 3){
				alignAndAddtoStorage(storage, toWrite, w);
			} else {
				errors++;
				String val = "First " +  first+ "Last " +  last + " id " + way.getId() + " dist " + MapUtils.getDistance(first, last) + " m";
				System.out.println("Ways in chain - " + w.size() + " - " + val);
			}
		}
		System.out.println("Fixed errors : " + ERRORS +", errors not fixed : " + errors );
		OsmStorageWriter writer = new OsmStorageWriter();
		writer.saveStorage(new FileOutputStream(write), storage, toWrite, true);
	}

	private static void alignAndAddtoStorage(OsmBaseStorage storage, List<EntityId> toWrite, List<Way> result) {
		// align start/end node and add to strage
		for (int i = 0; i < result.size(); i++) {
			Node nextStart;
			if (i < result.size() - 1) {
				nextStart = result.get(i + 1).getNodes().get(0);
			} else {
				nextStart = result.get(0).getNodes().get(0);
			}

			Way w = result.get(i);
			int sz = w.getNodes().size();
			w.removeNodeByIndex(sz - 1);
			w.addNode(nextStart);
			if("land_coastline".equals(w.getTag(OSMTagKey.NATURAL))) {
				w.putTag(OSMTagKey.NATURAL.getValue(), "coastline");
			}

			EntityId eId = EntityId.valueOf(w);
			storage.getRegisteredEntities().put(eId, w);
			toWrite.add(eId);
		}
	}
	
	private static long calcCoordinate(net.osmand.osm.Node node){
		LatLon l = node.getLatLon();
		double lon  =l.getLongitude();
		if(180 - Math.abs(l.getLongitude()) < 0.0001){
			if(l.getLongitude() < 0){
				lon = -179.9999;
			} else {
				lon = 180;
			}
		}
		return ((long)MapUtils.getTileNumberY(21, l.getLatitude()) << 32l) + ((long)MapUtils.getTileNumberX(21, lon));  
	}
	
	private static Map<Long, List<Way>> startWays = new LinkedHashMap<Long, List<Way>>();
	private static Map<Long, List<Way>> endWays = new LinkedHashMap<Long, List<Way>>();
	private static Map<Way, LatLon> duplicatedSimpleIslands = new LinkedHashMap<Way, LatLon>();
	private static int ERRORS = 0;
	
	private static Way revertWay(Way way){
		ArrayList<net.osmand.osm.Node> revNodes = new ArrayList<net.osmand.osm.Node>(way.getNodes());
		Collections.reverse(revNodes);
		Way ws = new Way(way.getId());
		for(String key : way.getTagKeySet()){
			ws.putTag(key, way.getTag(key));
		}
		for(net.osmand.osm.Node n : revNodes){
			ws.addNode(n);
		}
		return ws;
	}
	
	private static boolean pointContains(long start, long end){
		return startWays.containsKey(start) || endWays.containsKey(end) || startWays.containsKey(end) || endWays.containsKey(start);
	}
	
	private static long lastPoint(Way w){
		return calcCoordinate(w.getNodes().get(w.getNodes().size() - 1));
	}
	private static long lastPoint(List<Way> w){
		return lastPoint(w.get(w.size() - 1));
	}
	
	private static long firstPoint(List<Way> w){
		return firstPoint(w.get(0));
	}
	
	private static long firstPoint(Way way) {
		return calcCoordinate(way.getNodes().get(0));
	}

	private static List<Way> processWay(Way way) {
		// F Lat 8.27039215702537 Lon 73.0661727222713L Lat 8.27039215702537 Lon 73.0661727222713 id -1211228
		long start = firstPoint(way);
		long end = lastPoint(way);
		LatLon first = way.getNodes().get(0).getLatLon();
		LatLon last = way.getNodes().get(way.getNodes().size() - 1).getLatLon();
		String val = "F " + first + "L " +  last + " id " + way.getId();
		List<Way> cycle = null;
		if (start == end || MapUtils.getDistance(first, last) < 20) {
			LatLon c = way.getLatLon();
			cycle = Collections.singletonList(way);
			for(Way w : duplicatedSimpleIslands.keySet()){
				LatLon center = duplicatedSimpleIslands.get(w);
				if(MapUtils.getDistance(center, c) < 4000){
					//System.out.println("DUPLICATED " + first);
					return Collections.emptyList();
				}
			}
			duplicatedSimpleIslands.put(way, c);
		} else {
			List<Way> list = new ArrayList<Way>();
			list.add(way);
			
//			System.out.println(val);
			
			while (pointContains(start, end)) {
				if (startWays.containsKey(start) || endWays.containsKey(end)) {
					ERRORS++;
					Collections.reverse(list);
					for (int i = 0; i < list.size(); i++) {
						list.set(i, revertWay(list.get(i)));
					}
					long t = start;
					start = end;
					end = t;
				}
				if (endWays.containsKey(start)) {
					List<Way> tlist = endWays.remove(start);
					startWays.remove(firstPoint(tlist));
					tlist.addAll(list);
					list = tlist;
					
				} else if (startWays.containsKey(end)) {
					List<Way> tlist = startWays.remove(end);
					endWays.remove(lastPoint(tlist));
					list.addAll(tlist);
				}
				start = firstPoint(list);
				end = lastPoint(list);
				if (start == end) {
					cycle = list;
					break;
				} 
			}
			if (cycle == null) {
				startWays.put(start, list);
				endWays.put(end, list);
			}
		}
		
		if (cycle != null) {
			boolean clockwiseWay = MapAlgorithms.isClockwiseWay(cycle);
			if (clockwiseWay) {
				List<Way> ways = new ArrayList<Way>();
				ERRORS ++;
				for (int i = cycle.size() - 1; i >= 0; i--) {
					// System.out.println("Cycle error " + way.getId());
					ways.add(revertWay(cycle.get(i)));
				}
				return ways;
			}
			return cycle;
			
		}
		return Collections.emptyList();
		
	}

}
