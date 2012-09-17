package net.osmand.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.DataTileManager;
import net.osmand.osm.Entity;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Way;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingContext;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;



public class MapRouterLayer implements MapPanelLayer {
	
	private final static Log log = LogUtil.getLog(MapRouterLayer.class);

	private MapPanel map;
	private LatLon startRoute ;
	private LatLon endRoute ;
	private List<LatLon> intermediates = new ArrayList<LatLon>();
	private boolean nextAvailable = true;
	private boolean pause = true;
	private boolean stop = false;
	private int steps = 1;
	private JButton nextTurn;
	private JButton playPauseButton;
	private JButton stopButton;

	private List<RouteSegmentResult> previousRoute;
	
	
	@Override
	public void destroyLayer() {
		
	}

	@Override
	public void initLayer(MapPanel map) {
		this.map = map;
		fillPopupMenuWithActions(map.getPopupMenu());
		startRoute =  DataExtractionSettings.getSettings().getStartLocation();
		endRoute =  DataExtractionSettings.getSettings().getEndLocation();
		
		nextTurn = new JButton(">>"); //$NON-NLS-1$
		nextTurn.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				nextAvailable = true;
				synchronized (MapRouterLayer.this) {
					MapRouterLayer.this.notify();
				}
			}
		});
		nextTurn.setVisible(false);
		nextTurn.setAlignmentY(Component.TOP_ALIGNMENT);
		map.add(nextTurn, 0);
		playPauseButton = new JButton("Play"); //$NON-NLS-1$
		playPauseButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				pause = !pause; 
				playPauseButton.setText(pause ? "Play" : "Pause");
				nextAvailable = true;
				synchronized (MapRouterLayer.this) {
					MapRouterLayer.this.notify();
				}
			}
		});
		playPauseButton.setVisible(false);
		playPauseButton.setAlignmentY(Component.TOP_ALIGNMENT);
		map.add(playPauseButton, 0);
		stopButton = new JButton("Stop"); //$NON-NLS-1$
		stopButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				stop = true;
				nextAvailable = true;
				synchronized (MapRouterLayer.this) {
					MapRouterLayer.this.notify();
				}
			}
		});
		stopButton.setVisible(false);
		stopButton.setAlignmentY(Component.TOP_ALIGNMENT);
		map.add(stopButton);
	}

	public void fillPopupMenuWithActions(JPopupMenu menu) {
		Action start = new AbstractAction("Mark start point") {
			private static final long serialVersionUID = 507156107455281238L;

			@Override
			public void actionPerformed(ActionEvent e) {
				Point popupMenuPoint = map.getPopupMenuPoint();
				double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
				double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
				startRoute = new LatLon(latitude, longitude);
				DataExtractionSettings.getSettings().saveStartLocation(latitude, longitude);
				map.repaint();
			}
		};
		menu.add(start);
		Action end= new AbstractAction("Mark end point") {
			private static final long serialVersionUID = 4446789424902471319L;

			@Override
			public void actionPerformed(ActionEvent e) {
				Point popupMenuPoint = map.getPopupMenuPoint();
				double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
				double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
				endRoute = new LatLon(latitude, longitude);
				DataExtractionSettings.getSettings().saveEndLocation(latitude, longitude);
				map.repaint();
			}
		};
		menu.add(end);
		Action selfRoute = new AbstractAction("Calculate OsmAnd route") {
			private static final long serialVersionUID = 507156107455281238L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						List<Way> ways = selfRoute(startRoute, endRoute, intermediates, null);
						if (ways != null) {
							DataTileManager<Way> points = new DataTileManager<Way>();
							points.setZoom(11);
							for (Way w : ways) {
								LatLon n = w.getLatLon();
								points.registerObject(n.getLatitude(), n.getLongitude(), w);
							}
							map.setPoints(points);
						}
					}
				}.start();
			}
		};
		
		Action recalculate = new AbstractAction("Recalculate OsmAnd route") {
			private static final long serialVersionUID = 507156107455281238L;
			
			@Override
			public boolean isEnabled() {
//				return previousRoute != null;
				return true;
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						List<Way> ways = selfRoute(startRoute, endRoute, intermediates,  previousRoute);
						if (ways != null) {
							DataTileManager<Way> points = new DataTileManager<Way>();
							points.setZoom(11);
							for (Way w : ways) {
								LatLon n = w.getLatLon();
								points.registerObject(n.getLatitude(), n.getLongitude(), w);
							}
							map.setPoints(points);
						}
					}
				}.start();
			}
		};
		
		menu.add(selfRoute);
		menu.add(recalculate);
		Action route_YOURS = new AbstractAction("Calculate YOURS route") {
			private static final long serialVersionUID = 507156107455281238L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(){
					@Override
					public void run() {
						List<Way> ways = route_YOURS(startRoute, endRoute);
						DataTileManager<Way> points = new DataTileManager<Way>();
						points.setZoom(11);
						for(Way w : ways){
							LatLon n = w.getLatLon();
							points.registerObject(n.getLatitude(), n.getLongitude(), w);
						}
						map.setPoints(points);
					}
				}.start();
			}
		};
		menu.add(route_YOURS);
		Action route_CloudMate = new AbstractAction("Calculate CloudMade route") {
			private static final long serialVersionUID = 507156107455281238L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						List<Way> ways = route_CloudMate(startRoute, endRoute);
						DataTileManager<Way> points = new DataTileManager<Way>();
						points.setZoom(11);
						for (Way w : ways) {
							LatLon n = w.getLatLon();
							points.registerObject(n.getLatitude(), n.getLongitude(), w);
						}
						map.setPoints(points);
					}
				}.start();
			}
		};
		menu.add(route_CloudMate);
		Action swapLocations = new AbstractAction("Swap locations") {
			private static final long serialVersionUID = 507156107455281238L;

			@Override
			public void actionPerformed(ActionEvent e) {
				LatLon l = endRoute;
				endRoute = startRoute;
				startRoute = l;
				map.repaint();
			}
		};
		menu.add(swapLocations);
		Action addIntermediate = new AbstractAction("Add transit point") {

			@Override
			public void actionPerformed(ActionEvent e) {
				Point popupMenuPoint = map.getPopupMenuPoint();
				double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
				double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
				intermediates.add(new LatLon(latitude, longitude));
				map.repaint();
			}
		};
		menu.add(addIntermediate);
		
		Action remove = new AbstractAction("Remove transit point") {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(intermediates.size() > 0){
					intermediates.remove(0);
				}
				map.repaint();
			}
		};
		menu.add(remove);

	}
	
	
	// for vector rendering we should extract from osm
	// 1. Ways (different kinds) with tag highway= ?,highway=stop ...
	// 2. Junction = roundabout
	// 3. barrier, traffic_calming=bump
	// 4. Save {name, ref} of way to unify it
	
	// + for future routing we should extract from osm
	// 1. oneway 
	// 2. max_speed
	// 3. toll
	// 4. traffic_signals
	// 5. max_heigtht, max_width, min_speed, ...
	// 6. incline ?

	public static List<Way> route_YOURS(LatLon start, LatLon end){
		List<Way> res = new ArrayList<Way>();
		long time = System.currentTimeMillis();
		System.out.println("Route from " + start + " to " + end);
		if (start != null && end != null) {
			try {
				StringBuilder uri = new StringBuilder();
				uri.append("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml");
				uri.append("&flat=").append(start.getLatitude());
				uri.append("&flon=").append(start.getLongitude());
				uri.append("&tlat=").append(end.getLatitude());
				uri.append("&tlon=").append(end.getLongitude());
				uri.append("&v=motorcar").append("&fast=1").append("&layer=mapnik");

				URL url = new URL(uri.toString());
				URLConnection connection = url.openConnection();
				StringBuilder content = new StringBuilder();
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				{
					String s = null;
					boolean fist = true;
					while ((s = reader.readLine()) != null) {
						if (fist) {
							fist = false;
						}
						content.append(s).append("\n");
					}
					System.out.println(content);
				}
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dom = factory.newDocumentBuilder();
				Document doc = dom.parse(new InputSource(new StringReader(content.toString())));
				NodeList list = doc.getElementsByTagName("coordinates");
				for(int i=0; i<list.getLength(); i++){
					Node item = list.item(i);
					String str = item.getTextContent();
					int st = 0;
					int next = 0;
					Way w = new Way(-1);
					while((next = str.indexOf('\n', st)) != -1){
						String coordinate = str.substring(st, next + 1);
						int s = coordinate.indexOf(',');
						if (s != -1) {
							try {
								double lon = Double.parseDouble(coordinate.substring(0, s));
								double lat = Double.parseDouble(coordinate.substring(s + 1));
								w.addNode(new net.osmand.osm.Node(lat, lon, -1));
							} catch (NumberFormatException e) {
							}
						}
						st = next + 1;
					}
					if(!w.getNodes().isEmpty()){
						res.add(w);
					}
					
				}
			} catch (IOException e) {
				ExceptionHandler.handle(e);
			} catch (ParserConfigurationException e) {
				ExceptionHandler.handle(e);
			} catch (SAXException e) {
				ExceptionHandler.handle(e);
			}
			System.out.println("Finding routes " + res.size() + " " + (System.currentTimeMillis() - time) + " ms");
		}
		return res;
	}
	
	

		
	public List<Way> route_CloudMate(LatLon start, LatLon end) {
		List<Way> res = new ArrayList<Way>();
		long time = System.currentTimeMillis();
		System.out.println("Cloud made route from " + start + " to " + end);
		if (start != null && end != null) {
			try {
				StringBuilder uri = new StringBuilder();
				// possibly hide that API key because it is privacy of osmand
				uri.append("http://routes.cloudmade.com/A6421860EBB04234AB5EF2D049F2CD8F/api/0.3/");
				 
				uri.append(start.getLatitude()+"").append(",");
				uri.append(start.getLongitude()+"").append(",");
				uri.append(end.getLatitude()+"").append(",");
				uri.append(end.getLongitude()+"").append("/");
				uri.append("car.gpx").append("?lang=ru");

				URL url = new URL(uri.toString());
				URLConnection connection = url.openConnection();	
				StringBuilder content = new StringBuilder();
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				{
					String s = null;
					boolean fist = true;
					while ((s = reader.readLine()) != null) {
						if (fist) {
							fist = false;
						}
						content.append(s).append("\n");
					}
					System.out.println(content);
				}
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dom = factory.newDocumentBuilder();
				Document doc = dom.parse(new InputSource(new StringReader(content.toString())));
				NodeList list = doc.getElementsByTagName("wpt");
				Way w = new Way(-1);
				for (int i = 0; i < list.getLength(); i++) {
					Element item = (Element) list.item(i);
					try {
						double lon = Double.parseDouble(item.getAttribute("lon"));
						double lat = Double.parseDouble(item.getAttribute("lat"));
						w.addNode(new net.osmand.osm.Node(lat, lon, -1));
					} catch (NumberFormatException e) {
					}
				}
				list = doc.getElementsByTagName("rtept");
				for (int i = 0; i < list.getLength(); i++) {
					Element item = (Element) list.item(i);
					try {
						double lon = Double.parseDouble(item.getAttribute("lon"));
						double lat = Double.parseDouble(item.getAttribute("lat"));
						System.out.println("Lat " + lat + " lon " + lon);
						System.out.println("Distance : " + item.getElementsByTagName("distance").item(0).getTextContent());
						System.out.println("Time : " + item.getElementsByTagName("time").item(0).getTextContent());
						System.out.println("Offset : " + item.getElementsByTagName("offset").item(0).getTextContent());
						System.out.println("Direction : " + item.getElementsByTagName("direction").item(0).getTextContent());
					} catch (NumberFormatException e) {
					}
				}
				
				if (!w.getNodes().isEmpty()) {
					res.add(w);
				}
			} catch (IOException e) {
				ExceptionHandler.handle(e);
			} catch (ParserConfigurationException e) {
				ExceptionHandler.handle(e);
			} catch (SAXException e) {
				ExceptionHandler.handle(e);
			}
			System.out.println("Finding cloudmade routes " + res.size() + " " + (System.currentTimeMillis() - time) + " ms");
		}
		return res;
	}
	
	private static Double[] decodeGooglePolylinesFlow(String encodedData) {
        final List<Double> decodedValues = new ArrayList<Double>(); 
        int rawDecodedValue = 0;
        int carriage = 0;
        for (int x = 0, xx = encodedData.length(); x < xx; ++x) {
            int i = encodedData.charAt(x);
            i -= 63;
            int _5_bits = i << (32 - 5) >>> (32 - 5);
            rawDecodedValue |= _5_bits << carriage;
            carriage += 5;
            boolean isLast = (i & (1 << 5)) == 0;
            if (isLast) {
                boolean isNegative = (rawDecodedValue & 1) == 1;
                rawDecodedValue >>>= 1;
                if (isNegative) {
                	rawDecodedValue = ~rawDecodedValue;
                }
                decodedValues.add(((double)rawDecodedValue) / 1e5);
                carriage = 0;
                rawDecodedValue = 0;
            }
        }
        return decodedValues.toArray(new Double[decodedValues.size()]);
    }
	public static List<Way> route_OSRM(LatLon start, LatLon end){
		List<Way> res = new ArrayList<Way>();
		long time = System.currentTimeMillis();
		System.out.println("Route from " + start + " to " + end);
		if (start != null && end != null) {
			try {
				StringBuilder uri = new StringBuilder();
				uri.append(DataExtractionSettings.getSettings().getOsrmServerAddress());
				uri.append("/viaroute?");
				uri.append("&loc=").append(start.getLatitude()).append(",").append(start.getLongitude());
				uri.append("&loc=").append(end.getLatitude()).append(",").append(end.getLongitude());
				uri.append("&output=json");
				uri.append("&instructions=false");
				uri.append("&geomformat=cmp");

				URL url = new URL(uri.toString());
				URLConnection connection = url.openConnection();
				StringBuilder content = new StringBuilder();
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				{
					String s = null;
					boolean fist = true;
					while ((s = reader.readLine()) != null) {
						if (fist) {
							fist = false;
						}
						content.append(s).append("\n");
					}
					System.out.println(content);
				}
				
				final JSONObject jsonContent = (JSONObject)new JSONTokener(content.toString()).nextValue();
				
				// Encoded as https://developers.google.com/maps/documentation/utilities/polylinealgorithm
				final String routeGeometry = jsonContent.getString("route_geometry");
				final Double[] route = decodeGooglePolylinesFlow(routeGeometry);
				double latitude = 0.0;
				double longitude = 0.0;
				Way w = new Way(-1);
				for(int routePointIdx = 0; routePointIdx < route.length / 2; routePointIdx++) {
					latitude += route[routePointIdx * 2 + 0];
					longitude += route[routePointIdx * 2 + 1];
					
					w.addNode(new net.osmand.osm.Node(latitude, longitude, -1));
				}
				
				if (!w.getNodes().isEmpty()) {
					res.add(w);
				}

				/*DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dom = factory.newDocumentBuilder();
				Document doc = dom.parse(new InputSource(new StringReader(content.toString())));
				NodeList list = doc.getElementsByTagName("coordinates");
				for(int i=0; i<list.getLength(); i++){
					Node item = list.item(i);
					String str = item.getTextContent();
					int st = 0;
					int next = 0;
					Way w = new Way(-1);
					while((next = str.indexOf('\n', st)) != -1){
						String coordinate = str.substring(st, next + 1);
						int s = coordinate.indexOf(',');
						if (s != -1) {
							try {
								double lon = Double.parseDouble(coordinate.substring(0, s));
								double lat = Double.parseDouble(coordinate.substring(s + 1));
								w.addNode(new net.osmand.osm.Node(lat, lon, -1));
							} catch (NumberFormatException e) {
							}
						}
						st = next + 1;
					}
					if(!w.getNodes().isEmpty()){
						res.add(w);
					}
					
				}*/
			} catch (IOException e) {
				ExceptionHandler.handle(e);
			} catch (JSONException e) {
				ExceptionHandler.handle(e);
			}
			System.out.println("Finding routes " + res.size() + " " + (System.currentTimeMillis() - time) + " ms");
		}
		return res;
	}
	
	public List<Way> selfRoute(LatLon start, LatLon end, List<LatLon> intermediates, List<RouteSegmentResult> previousRoute) {
		List<Way> res = new ArrayList<Way>();
		long time = System.currentTimeMillis();
		List<File> files = new ArrayList<File>();
		for(File f :new File(DataExtractionSettings.getSettings().getBinaryFilesDir()).listFiles()){
			if(f.getName().endsWith(".obf")){
				files.add(f);
			}
		}
		String xmlPath = DataExtractionSettings.getSettings().getRoutingXmlPath();
		Builder builder;
		if(xmlPath.equals("routing.xml")){
			builder = RoutingConfiguration.getDefault() ;
		} else{
			try {
				builder = RoutingConfiguration.parseFromInputStream(new FileInputStream(xmlPath));
			} catch (IOException e) {
				throw new IllegalArgumentException("Error parsing routing.xml file",e);
			} catch (SAXException e) {
				throw new IllegalArgumentException("Error parsing routing.xml file",e);
			}
		}
		final boolean animateRoutingCalculation = DataExtractionSettings.getSettings().isAnimateRouting();
		if(animateRoutingCalculation) {
			nextTurn.setVisible(true);
			playPauseButton.setVisible(true);
			stopButton.setVisible(true);
			pause = true;
			playPauseButton.setText("Play");
		}
		stop = false;
		if(files.isEmpty()){
			JOptionPane.showMessageDialog(OsmExtractionUI.MAIN_APP.getFrame(), "Please specify obf file in settings", "Obf file not found", 
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		System.out.println("Self made route from " + start + " to " + end);
		if (start != null && end != null) {
			try {
				BinaryMapIndexReader[] rs = new BinaryMapIndexReader[files.size()];
				int it = 0;
				for (File f : files) {
					RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
					rs[it++] = new BinaryMapIndexReader(raf);
				}
				String m = DataExtractionSettings.getSettings().getRouteMode();
				String[] props = m.split("\\,");
				BinaryRoutePlanner router = new BinaryRoutePlanner(NativeSwingRendering.getDefaultFromSettings(), rs);
				RoutingConfiguration config = builder.build(props[0], props);
				// config.NUMBER_OF_DESIRABLE_TILES_IN_MEMORY = 300;
				// config.ZOOM_TO_LOAD_TILES = 14;
				RoutingContext ctx = new RoutingContext(config);
				ctx.previouslyCalculatedRoute = previousRoute;
				log.info("Use " + config.routerName + "mode for routing");
				
				// find closest way
				RouteSegment st = router.findRouteSegment(start.getLatitude(), start.getLongitude(), ctx);
				if (st == null) {
					throw new RuntimeException("Starting point for route not found");
				}
				System.out.println("ROAD TO START " + st.getRoad().getHighway() + " " + st.getRoad().id);
				
				RouteSegment e = router.findRouteSegment(end.getLatitude(), end.getLongitude(), ctx);
				if (e == null) {
					throw new RuntimeException("End point to calculate route was not found");
				}
				System.out.println("ROAD TO END " + e.getRoad().getHighway() + " " + e.getRoad().id);
				
				List<RouteSegment> inters  = new ArrayList<BinaryRoutePlanner.RouteSegment>();
				if (intermediates != null) {
					int ind = 1;
					for (LatLon il : intermediates) {
						RouteSegment is = router.findRouteSegment(il.getLatitude(), il.getLongitude(), ctx);
						if (is == null) {
							throw new RuntimeException("Intremediate point "+ind+" was not found.");
						}
						inters.add(is);
						ind++;
					}
				}
				
				final DataTileManager<Entity> points = new DataTileManager<Entity>();
				points.setZoom(11);
				map.setPoints(points);
				ctx.setVisitor(new RouteSegmentVisitor() {
					
					private List<RouteSegment> cache = new ArrayList<RouteSegment>();
					private List<RouteSegment> pollCache = new ArrayList<RouteSegment>();
					
					@Override
					public void visitSegment(RouteSegment s, boolean poll) {
						if(stop) {
							throw new RuntimeException("Interrupted");
						}
						if (!animateRoutingCalculation) {
							return;
						}
						if (!poll && pause) {
							pollCache.add(s);
							return;
						}

						cache.add(s);
						if (cache.size() < steps) {
							return;
						}
						if(pause) {
							registerObjects(points, poll, pollCache);
							pollCache.clear();
						}
						registerObjects(points, !poll, cache);
						cache.clear();
						redraw();
						if (pause) {
							waitNextPress();
						}
					}

					private void registerObjects(final DataTileManager<Entity> points, boolean white, 
							List<RouteSegment> registerCache) {
						for (RouteSegment segment : registerCache) {
							Way way = new Way(-1);
							way.putTag(OSMTagKey.NAME.getValue(), segment.getTestName());
							if(white) {
								way.putTag("color", "white");
							}
							for (int i = 0; i < segment.getRoad().getPointsLength(); i++) {
								net.osmand.osm.Node n = createNode(segment, i);
								way.addNode(n);
							}
							LatLon n = way.getLatLon();
							points.registerObject(n.getLatitude(), n.getLongitude(), way);
						}
					}

				});
				
				List<RouteSegmentResult> searchRoute = router.searchRoute(ctx, st, e, inters, false);
				this.previousRoute = searchRoute;
				if (animateRoutingCalculation) {
					playPauseButton.setVisible(false);
					nextTurn.setText("FINISH");
					waitNextPress();
					nextTurn.setText(">>");
				}
				net.osmand.osm.Node prevWayNode = null;
				for (RouteSegmentResult s : searchRoute) {
					// double dist = MapUtils.getDistance(s.startPoint, s.endPoint);
					Way way = new Way(-1);
//					String name = String.format("time %.2f ", s.getSegmentTime());
					String name = s.getDescription();
					if(s.getTurnType() != null) {
						name += " (TA " + s.getTurnType().getTurnAngle() + ") ";
					}
//					String name = String.format("beg %.2f end %.2f ", s.getBearingBegin(), s.getBearingEnd());
					way.putTag(OSMTagKey.NAME.getValue(),name);
					boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
					int i = s.getStartPointIndex();
					while (true) {
						LatLon l = s.getPoint(i);
						net.osmand.osm.Node n = new net.osmand.osm.Node(l.getLatitude(), l.getLongitude(), -1);
						if (prevWayNode != null) {
							if (MapUtils.getDistance(prevWayNode, n) > 0) {
								System.out.println("Warning not connected road " + " " + s.getObject().getHighway() + " dist "
										+ MapUtils.getDistance(prevWayNode, n));
							}
							prevWayNode = null;
						}
						way.addNode(n);
						if (i == s.getEndPointIndex()) {
							break;
						}
						if (plus) {
							i++;
						} else {
							i--;
						}
					}
					if (way.getNodes().size() > 0) {
						prevWayNode = way.getNodes().get(way.getNodes().size() - 1);
					}
					res.add(way);
				}
			} catch (IOException e) {
				ExceptionHandler.handle(e);
			} finally {
				playPauseButton.setVisible(false);
				nextTurn.setVisible(false);
				stopButton.setVisible(false);
				if(map.getPoints() != null) {
					map.getPoints().clear();
				}
			}
			System.out.println("Finding self routes " + res.size() + " " + (System.currentTimeMillis() - time) + " ms");
		}
		return res;
	}
	
	private net.osmand.osm.Node createNode(RouteSegment segment, int i) {
		net.osmand.osm.Node n = new net.osmand.osm.Node(MapUtils.get31LatitudeY(segment.getRoad().getPoint31YTile(i)),
				MapUtils.get31LongitudeX(segment.getRoad().getPoint31XTile(i)), -1);
		return n;
	}
	
	private void redraw() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					map.prepareImage();
				}
			});
		} catch (InterruptedException e1) {
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	private void waitNextPress() {
		nextTurn.setVisible(true);
		while (!nextAvailable) {
			try {
				synchronized (MapRouterLayer.this) {
					MapRouterLayer.this.wait();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		nextTurn.setVisible(false);
		nextAvailable = false;
	}
	
	@Override
	public void prepareToDraw() {
	}

	
	@Override
	public void paintLayer(Graphics2D g) {
		g.setColor(Color.green);
		if(startRoute != null){
			int x = map.getMapXForPoint(startRoute.getLongitude());
			int y = map.getMapYForPoint(startRoute.getLatitude());
			g.drawOval(x, y, 12, 12);
			g.fillOval(x, y, 12, 12);
		}
		g.setColor(Color.red);
		if(endRoute != null){
			int x = map.getMapXForPoint(endRoute.getLongitude());
			int y = map.getMapYForPoint(endRoute.getLatitude());
			g.drawOval(x, y, 12, 12);
			g.fillOval(x, y, 12, 12);
		}
		g.setColor(Color.yellow);
		for(LatLon i : intermediates) {
			int x = map.getMapXForPoint(i.getLongitude());
			int y = map.getMapYForPoint(i.getLatitude());
			g.drawOval(x, y, 12, 12);
			g.fillOval(x, y, 12, 12);
		}
	}

}
