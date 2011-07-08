package net.osmand.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
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
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.DataTileManager;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Way;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class MapRouterLayer implements MapPanelLayer {

	private /*final */ static boolean ANIMATE_CALCULATING_ROUTE = false;
	private /*final */ static int SIZE_OF_ROUTES_TO_ANIMATE = 1;
	
	
	private MapPanel map;
//	private LatLon startRoute;
//	private LatLon endRoute;
	// test route purpose
	private LatLon startRoute = new LatLon(53.910886,27.579095);
	private LatLon endRoute = new LatLon(53.95386,27.68131);
	
	
	@Override
	public void destroyLayer() {
		
	}

	@Override
	public void initLayer(MapPanel map) {
		this.map = map;
		fillPopupMenuWithActions(map.getPopupMenu());
	}

	public void fillPopupMenuWithActions(JPopupMenu menu) {
		Action start = new AbstractAction("Mark start point") {
			private static final long serialVersionUID = 507156107455281238L;

			public void actionPerformed(ActionEvent e) {
				Point popupMenuPoint = map.getPopupMenuPoint();
				double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
				double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
				startRoute = new LatLon(latitude, longitude);
				map.repaint();
			}
		};
		menu.add(start);
		Action end= new AbstractAction("Mark end point") {
			private static final long serialVersionUID = 4446789424902471319L;

			public void actionPerformed(ActionEvent e) {
				Point popupMenuPoint = map.getPopupMenuPoint();
				double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
				double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
				double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
				double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
				endRoute = new LatLon(latitude, longitude);
				map.repaint();
			}
		};
		menu.add(end);
		Action route = new AbstractAction("Calculate YOURS route") {
			private static final long serialVersionUID = 507156107455281238L;

			public void actionPerformed(ActionEvent e) {
				new Thread(){
					@Override
					public void run() {
						List<Way> ways = route(startRoute, endRoute);
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
		menu.add(route);
		Action altroute = new AbstractAction("Calculate CloudMade route") {
			private static final long serialVersionUID = 507156107455281238L;

			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						List<Way> ways = alternateRoute(startRoute, endRoute);
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
		menu.add(altroute);
		Action selfRoute = new AbstractAction("Calculate OsmAnd route") {
			private static final long serialVersionUID = 507156107455281238L;

			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						List<Way> ways = selfRoute(startRoute, endRoute);
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

	public static List<Way> route(LatLon start, LatLon end){
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
	
	

		
	public List<Way> alternateRoute(LatLon start, LatLon end) {
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
	
	public List<Way> selfRoute(LatLon start, LatLon end) {
		List<Way> res = new ArrayList<Way>();
		long time = System.currentTimeMillis();
		File[] files = DataExtractionSettings.getSettings().getDefaultRoutingFile();
		if(files == null){
			JOptionPane.showMessageDialog(OsmExtractionUI.MAIN_APP.getFrame(), "Please specify obf file in settings", "Obf file not found", 
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
		System.out.println("Self made route from " + start + " to " + end);
		if (start != null && end != null) {
			try {
				BinaryMapIndexReader[] rs = new BinaryMapIndexReader[files.length];
				for(int i=0; i<files.length; i++){
					RandomAccessFile raf = new RandomAccessFile(files[i], "r"); //$NON-NLS-1$ //$NON-NLS-2$
					rs[i] = new BinaryMapIndexReader(raf, true);
					
				}
				
				BinaryRoutePlanner router = new BinaryRoutePlanner(rs);
				RoutingContext ctx = new RoutingContext();
				
				// find closest way
				RouteSegment st = router.findRouteSegment(start.getLatitude(), start.getLongitude(), ctx);
				if (st != null) {
					BinaryMapDataObject road = st.getRoad();
					TagValuePair pair = road.getTagValue(0);
					System.out.println("ROAD TO START " + pair.tag + " " + pair.value + " " + road.getName() + " " 
							+ (road.getId() >> 3));
				}
				
				RouteSegment e = router.findRouteSegment(end.getLatitude(), end.getLongitude(), ctx);
				if ( e != null) {
					BinaryMapDataObject road =  e.getRoad();
					TagValuePair pair = road.getTagValue(0);
					System.out.println("ROAD TO END " + pair.tag + " " + pair.value + " " + road.getName()+ " " +  
							+ (road.getId()  >> 3));
				}
				
				final DataTileManager<Way> points = new DataTileManager<Way>();
				points.setZoom(11);
				map.setPoints(points);
				ctx.setVisitor(new RouteSegmentVisitor() {
					
					private List<RouteSegment> cache = new ArrayList<RouteSegment>();
					
					@Override
					public void visitSegment(RouteSegment s) {
						if(!ANIMATE_CALCULATING_ROUTE){
							return;
						}
						cache.add(s);
						if(cache.size() < SIZE_OF_ROUTES_TO_ANIMATE){
							return;
						}
						for (RouteSegment segment : cache) {
							Way way = new Way(-1);
							for (int i = 0; i < segment.getRoad().getPointsLength(); i++) {
								net.osmand.osm.Node n = new net.osmand.osm.Node(MapUtils.get31LatitudeY(segment.getRoad()
										.getPoint31YTile(i)), MapUtils.get31LongitudeX(segment.getRoad().getPoint31XTile(i)), -1);
								way.addNode(n);
							}
							LatLon n = way.getLatLon();
							points.registerObject(n.getLatitude(), n.getLongitude(), way);
						}
						cache.clear();
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
				});
				List<RouteSegmentResult> searchRoute = router.searchRoute(ctx, st, e);
				if (ANIMATE_CALCULATING_ROUTE) {
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e1) {
					}
				}

				net.osmand.osm.Node prevWayNode = null;
				for (RouteSegmentResult s : searchRoute) {
					// double dist = MapUtils.getDistance(s.startPoint, s.endPoint);
					Way way = new Way(-1);
					boolean plus = s.startPointIndex < s.endPointIndex;
					int i = s.startPointIndex;
					while (true) {
						net.osmand.osm.Node n = new net.osmand.osm.Node(MapUtils.get31LatitudeY(s.object.getPoint31YTile(i)), MapUtils
								.get31LongitudeX(s.object.getPoint31XTile(i)), -1);
						if (prevWayNode != null) {
							if (MapUtils.getDistance(prevWayNode, n) > 0) {
								System.out.println("Warning not connected road " + " " + s.object.getName() + " dist "
										+ MapUtils.getDistance(prevWayNode, n));
							}
							prevWayNode = null;
						}
						way.addNode(n);
						if (i == s.endPointIndex) {
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
			}
			System.out.println("Finding self routes " + res.size() + " " + (System.currentTimeMillis() - time) + " ms");
		}
		return res;
	}
	
	@Override
	public void prepareToDraw() {
	}

	
	@Override
	public void paintLayer(Graphics g) {
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
	}

}
