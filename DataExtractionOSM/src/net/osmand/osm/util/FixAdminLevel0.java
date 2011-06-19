package net.osmand.osm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.osm.io.OsmStorageWriter;

import org.xml.sax.SAXException;

public class FixAdminLevel0 {
	
	public static void main(String[] args) throws IOException, SAXException, XMLStreamException {
		String fileToRead = args != null && args.length > 0 ? args[0] : null; 
		if(fileToRead == null) {
			fileToRead = "/home/victor/projects/OsmAnd/download/basemap/10m_admin_level.osm";
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
		long id = -1;
		for(EntityId e : entities.keySet()){
			Entity es = storage.getRegisteredEntities().get(e);
			if(e.getId() < id){
				id = e.getId() - 1;
			}
			if(e.getType() == EntityType.WAY){
				processWay((Way) es);
			}
		}
		for(String country : countryNames.keySet()){
			List<Way> list = countryNames.get(country);
			for(Way w : list){
				LatLon latLon = w.getLatLon();
				Node node = new Node(latLon.getLatitude(), latLon.getLongitude(), id--);
				node.putTag("name", country);
				node.putTag("place", "country");
				storage.getRegisteredEntities().put(EntityId.valueOf(node), node);
			}
		}
		OsmStorageWriter writer = new OsmStorageWriter();
		writer.saveStorage(new FileOutputStream(write), storage, null, true);
	}

	
	
	private static Map<String, List<Way>> countryNames = new LinkedHashMap<String, List<Way>>();
	
	private static void processWay(Way way) {
		if("0".equals(way.getTag("admin_level")) && way.getTag(OSMTagKey.NAME) != null){
			String name = way.getTag(OSMTagKey.NAME);
			if(way.getNodes().size() < 20){
				// too small
				return;
			}
			if(!countryNames.containsKey(name)){
				ArrayList<Way> list = new ArrayList<Way>();
				list.add(way);
				countryNames.put(name, list);
			} else {
				List<Way> r = countryNames.get(name);
				for (int i = 0; i < r.size();) {
					if (way.getNodes().size() > 4 * r.get(i).getNodes().size()) {
						r.remove(i);
					} else {
						i++;
					}
				}
				r.add(way);
			}
		}
		
	}

}
