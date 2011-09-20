package net.osmand.osm.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.xml.stream.XMLStreamException;

import net.osmand.data.DataTileManager;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.Entity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Relation;
import net.osmand.osm.Way;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.io.IOsmStorageFilter;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.osm.io.OsmStorageWriter;
import net.osmand.swing.DataExtractionSettings;
import net.osmand.swing.MapPanel;

import org.xml.sax.SAXException;


public class MinskTransReader {
	// Routes RouteNum; Authority; City; Transport; Operator; ValidityPeriods; SpecialDates;RouteTag;RouteType;Commercial;RouteName;Weekdays;RouteID;Entry;RouteStops;Datestart
	public static class TransportRoute {
		public String routeNum; // 0
		public String transport; // 3
		public String routeType; // 8
		public String routeName; // 10
		public String routeId; // 12
		public List<String> routeStops = new ArrayList<String>(); // 14
	}
	
	// ID;City;Area;Street;Name;Lng;Lat;Stops
	public static class TransportStop {
		public String stopId; // 0
		public double longitude; // 5
		public double latitude; // 6
		public String name ; //4
	}
	
	public static final int default_dist_to_stop = 60;
	
	public static final String pathToRoutes = "E:/routes.txt";
	public static final String pathToStops = "E:/stops.txt";
	public static final String pathToMinsk = "E:\\Information\\OSM maps\\minsk_streets.osm";
	public static final String pathToSave = "E:\\Information\\OSM maps\\data_edit.osm";
	
	public static void main(String[] args) throws IOException, SAXException, XMLStreamException {
		FileInputStream fis = new FileInputStream(new File(pathToRoutes));
		BufferedReader reader = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
		List<TransportRoute> routes = readRoutes(reader);
		
		fis = new FileInputStream(new File(pathToStops));
		reader = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
		List<TransportStop> stops = readStopes(reader);
		Map<String, TransportStop> stopsMap = new LinkedHashMap<String, TransportStop>();
		for(TransportStop s : stops){
			stopsMap.put(s.stopId, s);
		}
		// checking that stops are good
		for(TransportRoute r : routes){
			for(String string : r.routeStops){
				if(!stopsMap.containsKey(string)){
					throw new IllegalArgumentException("Check stop " + string + " of route " + r.routeName);
				}
			}
		}
//		showMapPanelWithCorrelatedBusStops(stopsMap, busStops);
		
		OsmBaseStorage storage = filterBusStops(stopsMap, routes);
		OsmStorageWriter writer = new OsmStorageWriter();
		writer.saveStorage(new FileOutputStream(pathToSave), storage, null, true);

	}

	public static void showMapPanelWithCorrelatedBusStops(Map<String, TransportStop> stopsMap, DataTileManager<Node> busStops) {
		Map<String, Node> result = correlateExistingBusStopsWithImported(busStops, stopsMap);
		DataTileManager<Entity> nodes = new DataTileManager<Entity>();
		for (String trId : result.keySet()) {
			TransportStop r = stopsMap.get(trId);
			Way way = new Way(-1);
			way.addNode(result.get(trId));
			way.addNode(new Node(r.latitude, r.longitude, -1));
			nodes.registerObject(r.latitude, r.longitude, way);
		}
		for(String trId : stopsMap.keySet()){
			if(!result.containsKey(trId)){
				TransportStop r = stopsMap.get(trId);
				nodes.registerObject(r.latitude, r.longitude, new Node(r.latitude, r.longitude, -1));
			}
		}
		showMapPanel(nodes);
	}
	
	public static Map<String, Node>  correlateExistingBusStopsWithImported(DataTileManager<Node> busStops, Map<String, TransportStop> stopsMap){
		Map<String, Node> correlated = new LinkedHashMap<String, Node>();
		Map<Node, String> reverse = new LinkedHashMap<Node, String>();
		List<TransportStop> stopsToCheck = new ArrayList<TransportStop>(stopsMap.values());
		int sTCsize = stopsToCheck.size();
		for(int k =0; k<sTCsize; k++){
			TransportStop r = stopsToCheck.get(k);
			List<Node> closestObjects = busStops.getClosestObjects(r.latitude, r.longitude, 0, 1);
			// filter closest objects
			for(int i=0; i<closestObjects.size(); ){
				if(MapUtils.getDistance(closestObjects.get(i), r.latitude, r.longitude) > default_dist_to_stop){
					closestObjects.remove(i);
				} else{
					i++;
				}
			}
			MapUtils.sortListOfEntities(closestObjects, r.latitude, r.longitude);
			int ind = 0; 
			boolean ccorrelated = false;
			int cOsize = closestObjects.size();
			while(ind < cOsize && !ccorrelated){
				Node foundNode = closestObjects.get(ind);
				if(!reverse.containsKey(foundNode)){
					// all is good no one registered to that stop
					reverse.put(foundNode, r.stopId);
					correlated.put(r.stopId, foundNode);
					ccorrelated = true;
				} else {
					// recorrelate existing node and add to todo list
					String stopId = reverse.get(foundNode);
					TransportStop st = stopsMap.get(stopId);
					if(MapUtils.getDistance(foundNode, r.latitude, r.longitude) < MapUtils.getDistance(foundNode, st.latitude, st.longitude)){
						// check that stop again
						stopsToCheck.add(st);
						reverse.put(foundNode, r.stopId);
						correlated.put(r.stopId, foundNode);
						correlated.remove(st.stopId);
						ccorrelated = true;
					}
				}
				ind++;
			}
		}
		return correlated;
		
	}
	
	public static  void showMapPanel(DataTileManager<? extends Entity> points){
		JFrame frame = new JFrame("Map view");
	    try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		final MapPanel panel = new MapPanel(DataExtractionSettings.getSettings().getTilesDirectory());
		panel.setPoints(points);
	    frame.addWindowListener(new WindowAdapter(){
	    	@Override
	    	public void windowClosing(WindowEvent e) {
	    		DataExtractionSettings settings = DataExtractionSettings.getSettings();
				settings.saveDefaultLocation(panel.getLatitude(), panel.getLongitude());
				settings.saveDefaultZoom(panel.getZoom());
	    		System.exit(0);
	    	}
	    });
	    Container content = frame.getContentPane();
	    content.add(panel, BorderLayout.CENTER);

	    JMenuBar bar = new JMenuBar();
	    bar.add(MapPanel.getMenuToChooseSource(panel));
	    frame.setJMenuBar(bar);
	    frame.setSize(512, 512);
	    frame.setVisible(true);

	}
	
	protected static void removeGeneratedNotUsedBusStops(Map<String, Node> correlated, 
			Map<String, Relation> definedRoutes, DataTileManager<Node> busStops, OsmBaseStorage storage){
		Set<Node> usedNodes = new LinkedHashSet<Node>(correlated.values());
		for(Relation r : definedRoutes.values()){
			for(Entity e : r.getMembers(null)){
				if(e instanceof Node){
					usedNodes.add((Node) e);
				}
			}
		}
		for(Node stop : busStops.getAllObjects()){
			if(!usedNodes.contains(stop) && "yes".equals(stop.getTag("generated"))){
				EntityInfo info = storage.getRegisteredEntityInfo().get(stop.getId());
				info.setAction("delete");
				System.out.println("[DEL] Remove generated not used stop " + stop.getId() + " " + stop.getTag("name"));
			}
		}
		
	}
	
	protected static OsmBaseStorage filterBusStops(Map<String, TransportStop> stopsMap, List<TransportRoute> routes) throws FileNotFoundException, IOException, SAXException{
		long time = System.currentTimeMillis();
		System.out.println("Start : ");
		OsmBaseStorage storage = new OsmBaseStorage();
		
		final Map<String, Relation> definedRoutes = new HashMap<String, Relation>();
		final DataTileManager<Node> busStops = new DataTileManager<Node>();
		busStops.setZoom(17);
		storage.getFilters().add(new IOsmStorageFilter(){

			@Override
			public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity) {
				if(entity.getTag("route") != null){
					String route = entity.getTag("route");
					if(route.equals("bus") || route.equals("tram") || route.equals("trolleybus") || route.equals("subway")){
						definedRoutes.put(entity.getTag("route") + "_" + entity.getTag("ref"), (Relation) entity);
						return true;
					}
				}
				if(entity.getTag(OSMTagKey.HIGHWAY) != null && entity.getTag(OSMTagKey.HIGHWAY).equals("bus_stop")){
					LatLon e = entity.getLatLon();
					busStops.registerObject(e.getLatitude(), e.getLongitude(), (Node) entity);
				}
				return entity instanceof Node;
			}
			
		});
		storage.parseOSM(new FileInputStream(pathToMinsk), new ConsoleProgressImplementation());
		
		Map<String, Node> correlated = correlateExistingBusStopsWithImported(busStops, stopsMap);
		removeGeneratedNotUsedBusStops(correlated, definedRoutes, busStops, storage);
		
	
		
		registerNewRoutesAndEditExisting(stopsMap, routes, storage, definedRoutes, correlated);
		System.out.println("End time : " + (System.currentTimeMillis() - time));
		return storage;
	}
	
	protected static boolean validateRoute(String routeStr, Map<String, TransportStop> trStops, Map<String, Node> correlated, Relation relation, TransportRoute route, boolean direct){
		Collection<Entity> stops = relation.getMembers("stop");
		routeStr += direct ? "_forward" : "_backward";
		if(stops.size() != 2){
			System.out.println("[INVALID ] " + routeStr + " : doesn't contain start/final stop.");
			return false;
		}
		List<Entity> list = new ArrayList<Entity>(relation.getMembers(direct?"forward:stop" : "backward:stop"));
		if((list.size() + 2) != route.routeStops.size()){
			System.out.println("[INVALID ] " + routeStr + " number of stops isn't equal (" +  
					((list.size() + 2)) + " relation != " + route.routeStops.size() + " route) ");
			return false;
		}
		Iterator<Entity> it = stops.iterator();
		Entity start = it.next();
		Entity end = it.next();
		if(direct){
			list.add(0, start);
			list.add(end);
		} else {
			list.add(0, end);
			list.add(start);
		}
		int lsize = list.size();
		for(int i=0; i<lsize; i++){
			String st = route.routeStops.get(i);
			Node correlatedNode = correlated.get(st);
			TransportStop trStop = trStops.get(st);
			String stStr = trStop.stopId + " " + trStop.name;
			Entity e = list.get(i);
			if(correlatedNode == null){
				double dist = MapUtils.getDistance(e.getLatLon(), trStop.latitude, trStop.longitude);
				if(dist > 20){
					System.out.println("[INVALID ]" + routeStr + " stop " + (i+1) + " was not correlated " + stStr + " distance = " + dist);
					return false;
				}
				
			} else if(correlatedNode.getId() != e.getId()){
				double dist = MapUtils.getDistance(correlatedNode, e.getLatLon());
				if(i==lsize - 1 && !direct && dist < 150){
					continue;
				} 
				String eStop = e.getId() + " " + e.getTag(OSMTagKey.NAME);
				System.out.println("[INVALID ] " + routeStr + " stop " + (i+1) + " wrong : " + stStr + " != "  + eStop + " dist = " + dist + 
						" current correlated to " + correlatedNode.getId());
				return false;
			}
			
		}
		
		return true;
	}

	protected static long id = -55000;
	protected static void registerNewRoutesAndEditExisting(Map<String, TransportStop> stopsMap, List<TransportRoute> routes,
			OsmBaseStorage storage, final Map<String, Relation> definedRoutes, Map<String, Node> correlated) {
		Map<String, Relation> checkedRoutes = new LinkedHashMap<String, Relation>();
		// because routes can changed on schedule that's  why for 1 relation many routes.
		Set<String> visitedRoutes = new HashSet<String>();
		
		for (TransportRoute r : routes) {
			// register only bus/trolleybus
			if (!r.transport.equals("bus") && !r.transport.equals("trolleybus")) {
				continue;
			}
			String s = r.transport + "_" + r.routeNum;

			boolean reverse = r.routeType.equals("B>A");
			boolean direct = r.routeType.equals("A>B");
			if (!reverse && !direct) {
				// that's additinal route skip it
				continue;
			}
			if (!visitedRoutes.add(s + "_" + direct)) {
				// skip it : duplicated route (schedule changed)
				continue;
			}

			if (definedRoutes.containsKey(s)) {
				checkedRoutes.put(s, definedRoutes.get(s));
				boolean valid = validateRoute(s, stopsMap, correlated, definedRoutes.get(s), r, direct);
				if(valid){
					System.err.println("VALID " + s + " " + direct);
				}
				// System.out.println("Already registered " + s);
			} else {
				if (!checkedRoutes.containsKey(s)) {
					if(reverse){
						System.err.println("Strange route skipped : " + s);
						continue;
					}
					Relation relation = new Relation(id--);
					relation.putTag("route", r.transport);
					relation.putTag("ref", r.routeNum);
					relation.putTag("name", r.routeName);
					relation.putTag("operator", "КУП \"Минсктранс\"");
					relation.putTag("type", "route");
					relation.putTag("generated", "yes");
					checkedRoutes.put(s, relation);
					storage.getRegisteredEntities().put(new EntityId(EntityType.RELATION, relation.getId()), relation);
					System.out.println("[ADD] Registered new route " + s);
				} 
				Relation relation = checkedRoutes.get(s);

				// correlating stops
				int rSsize = r.routeStops.size();
				for (int i = 0; i < rSsize; i++) {
					String stop = r.routeStops.get(i);
					if (!stopsMap.containsKey(stop)) {
						throw new IllegalArgumentException("Stops file is not corresponded to routes file");
					}
					if (!correlated.containsKey(stop)) {
						TransportStop st = stopsMap.get(stop);
						Node node = new Node(st.latitude, st.longitude, id--);
						node.putTag("highway", "bus_stop");
						if (st.name != null) {
							node.putTag("name", st.name);
						} else {
							throw new IllegalArgumentException("Something wrong check " + st.stopId);
						}
						node.putTag("generated", "yes");
						storage.getRegisteredEntities().put(new EntityId(EntityType.NODE, node.getId()), node);
						System.out.println("[ADD] Added new bus_stop : " + node.getId() + " " + st.name + " minsktrans_stop_id " + st.stopId);
						correlated.put(stop, node);
					}
					if (i == 0 || i == rSsize - 1) {
						if (direct) {
							relation.addMember(correlated.get(stop).getId(), EntityType.NODE, "stop");
						}
					} else {
						if (direct) {
							relation.addMember(correlated.get(stop).getId(), EntityType.NODE, "forward:stop");
						} else {
							relation.addMember(correlated.get(stop).getId(), EntityType.NODE, "backward:stop");
						}
					}

				}
			}
		}
		
		// check relations that are not exist
		for(String s : definedRoutes.keySet()){
			if(!checkedRoutes.containsKey(s)){
				Relation rel = definedRoutes.get(s);
				storage.getRegisteredEntityInfo().get(rel.getId()).setAction("delete");
				System.out.println("[DEL] Route is deprecated : " + rel.getTag("route")+"_"+rel.getTag("ref") + "  " + rel.getTag("name"));
				
			}
		}
	}

	protected static List<TransportRoute> readRoutes(BufferedReader reader) throws IOException {
		String st = null;
		int line = 0;
		TransportRoute previous = null;
		List<TransportRoute> routes = new ArrayList<TransportRoute>(); 
		while((st = reader.readLine()) != null){
			if(line++ == 0){
				continue;
			}
			
			TransportRoute current = new TransportRoute();
			int stI=0;
			int endI = 0;
			int i=0;
			while ((endI = st.indexOf(';', stI)) != -1) {
				
				String newS = st.substring(stI, endI);
				if(i==0){
					if(newS.length() > 0){
						current.routeNum = newS;
					} else if(previous != null){
						current.routeNum = previous.routeNum;
					}
				} else if(i==3){
					if(newS.length() > 0){
						current.transport = newS;
					} else if(previous != null){
						current.transport = previous.transport;
					}
				} else if(i==8){
					if(newS.length() > 0){
						current.routeType = newS;
					} else if(previous != null){
						current.routeType  = previous.routeType ;
					}
				} else if(i==10){
					if(newS.length() > 0){
						current.routeName = newS;
					} else if(previous != null){
						current.routeName  = previous.routeName ;
					}		
				} else if(i==12){
					current.routeId = newS;
				} else if(i==14){
					String[] strings = newS.split(",");
					for(String s : strings){
						s = s.trim();
						if(s.length() > 0){
							current.routeStops.add(s);
						}
					}
				}
				stI = endI + 1;
				i++;
			}
			previous = current;
			routes.add(current);
		}
		return routes;
	}
	
	protected static List<TransportStop> readStopes(BufferedReader reader) throws IOException {
		String st = null;
		int line = 0;
		List<TransportStop> stopes = new ArrayList<TransportStop>();
		TransportStop previous = null;
		while((st = reader.readLine()) != null){
			if(line++ == 0){
				continue;
			}
			
			TransportStop current = new TransportStop();
			int stI=0;
			int endI = 0;
			int i=0;
			while ((endI = st.indexOf(';', stI)) != -1) {
				
				String newS = st.substring(stI, endI);
				if(i==0){
					current.stopId = newS.trim();
				} else if(i==4){
					if(newS.length() == 0 && previous != null){
						current.name = previous.name;
					} else {
						current.name = newS;
					}
				} else if(i==5){
					current.longitude = Double.parseDouble(newS)/1e5;
				} else if(i==6){
					current.latitude =  Double.parseDouble(newS)/1e5;
				}
				stI = endI + 1;
				i++;
			}
			previous = current;
			stopes.add(current);
		}
		return stopes;
	}
	
}