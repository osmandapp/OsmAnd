package net.osmand.swing;

import gnu.trove.set.hash.TLongHashSet;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.DataTileManager;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MapClusterLayer implements MapPanelLayer {

	private /*final */ static boolean ANIMATE_CLUSTERING = false;
	private /*final */ static int SIZE_OF_ROUTES_TO_ANIMATE = 5;
	private Log log = LogFactory.getLog(MapClusterLayer.class);
	
	
	private MapPanel map;
	
	
	@Override
	public void destroyLayer() {
		
	}

	@Override
	public void initLayer(MapPanel map) {
		this.map = map;
		fillPopupMenuWithActions(map.getPopupMenu());
	}

	public void fillPopupMenuWithActions(JPopupMenu menu) {
		Action clustering= new AbstractAction("Clustering roads") {
			private static final long serialVersionUID = 444678942490247133L;

			@Override
			public void actionPerformed(ActionEvent e) {
				clusteringRoadActions();
			}

			
		};
		menu.add(clustering);
		

	}
	
	
	private void clusteringRoadActions() {
		Point popupMenuPoint = map.getPopupMenuPoint();
		double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
		double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
		final double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
		final double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					List<RouteSegment> ways = clustering(latitude, longitude);
					if (!ANIMATE_CLUSTERING) {
						DataTileManager<Way> points = new DataTileManager<Way>();
						points.setZoom(11);
						for (RouteSegment s : ways) {
							Way w = new Way(-1);
							for (int i = 0; i < s.getRoad().getPointsLength(); i++) {
								double lat = MapUtils.get31LatitudeY(s.getRoad().getPoint31YTile(i));
								double lon = MapUtils.get31LongitudeX(s.getRoad().getPoint31XTile(i));
								w.addNode(new Node(lat, lon, -1));
							}
							LatLon n = w.getLatLon();
							points.registerObject(n.getLatitude(), n.getLongitude(), w);
						}
						map.setPoints(points);
						map.repaint();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}).start();
	}
	
	private List<RouteSegment> clustering(double lat, double lon) throws IOException{
		List<BinaryMapIndexReader> rs = new ArrayList<BinaryMapIndexReader>();
		for (File f : new File(DataExtractionSettings.getSettings().getBinaryFilesDir()).listFiles()) {
			if (f.getName().endsWith(".obf")) {
				RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
				rs.add(new BinaryMapIndexReader(raf));
			}
		}
		BinaryRoutePlanner router = new BinaryRoutePlanner();
		Builder builder = RoutingConfiguration.getDefault();
		RoutingConfiguration config = builder.build("car", RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3);
		RoutingContext ctx = new RoutingContext(config, NativeSwingRendering.getDefaultFromSettings(),
				rs.toArray(new BinaryMapIndexReader[rs.size()]));
		// find closest way
		RouteSegment st = router.findRouteSegment(lat, lon, ctx);
		if (st != null) {
			RouteDataObject road = st.getRoad();
			String highway = getHighway(road);
			log.info("ROAD TO START " + highway + " " + //road.getName() + " " 
					+ road.id);
		}
		final DataTileManager<Way> points = new DataTileManager<Way>();
		points.setZoom(11);
		map.setPoints(points);
		
		ctx.setVisitor(new RouteSegmentVisitor() {
			private List<RouteSegment> cache = new ArrayList<RouteSegment>();
			@Override
			public void visitSegment(RouteSegment s, boolean poll) {
				if(!ANIMATE_CLUSTERING){
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
		List<RouteSegment> results = searchCluster(ctx, st);
		return results;
	}
	
	
	
	
	
	private long calculateId(RouteSegment r, int segment){
		return (r.getRoad().getId() << 8) + segment;
	}
	
	
	private List<RouteSegment> searchCluster(RoutingContext ctx, RouteSegment st) throws IOException {
		Queue<List<RouteSegment>> queue  = new LinkedList<List<RouteSegment>>();
		List<RouteSegment> result = new ArrayList<BinaryRoutePlanner.RouteSegment>();
		TLongHashSet visitedIds = new TLongHashSet();
		ArrayList<RouteSegment> start = new ArrayList<RouteSegment>();
		start.add(st);
		queue.add(start);
		RouteDataObject startRoad = st.getRoad();
		int ZOOM_LIMIT = 13;
		int TILE_BOUNDARIES = 10;
		int LOCAL_TILE_BOUNDARIES = 2;
		int zm = 31 - ZOOM_LIMIT;
		int tileX = startRoad.getPoint31XTile(st.getSegmentStart()) >> zm;
		int tileY = startRoad.getPoint31YTile(st.getSegmentStart()) >> zm;
		int outOfTile = 0;
		int outOfDistance = 0;
		int segmentsProcessed = 0;
		float minRatio = 1f;
		int segmentsMinProcessed = 0;
		TLongHashSet onTheMap = new TLongHashSet();
		while(!queue.isEmpty()){
			List<RouteSegment> segments = queue.peek();
			if(segments.size() == 0) {
				queue.poll();
				continue;
			}
			RouteSegment segment = segments.remove(segments.size() - 1);
			RouteDataObject road = segment.getRoad();
			if(visitedIds.contains(calculateId(segment, segment.getSegmentStart()))){
				continue;
			}
			segmentsProcessed++;
			if(segmentsProcessed > 50) {
				float ratio = (float) (queue.size() + outOfTile + outOfDistance) / segmentsProcessed;
				if(ratio < minRatio) {
					minRatio = ratio;
					segmentsMinProcessed = segmentsProcessed;
				}
				
			}
			visitedIds.add(calculateId(segment, segment.getSegmentStart()));
			boolean minusAllowed = true;
			boolean plusAllowed = true;
			int d = 1;
			while (minusAllowed || plusAllowed) {
				int segmentEnd = segment.getSegmentStart() + d;
				int currentD = d;
				if (!minusAllowed && d > 0) {
					d++;
				} else if (!plusAllowed && d < 0) {
					d--;
				} else {
					if (d <= 0) {
						d = -d + 1;
					} else {
						d = -d;
					}
				}
				if (segmentEnd < 0) {
					minusAllowed = false;
					continue;
				}
				if (segmentEnd >= road.getPointsLength()) {
					plusAllowed = false;
					continue;
				}
				if (visitedIds.contains(calculateId(segment, segmentEnd))) {
					if (currentD > 0) {
						plusAllowed = false;
					} else {
						minusAllowed = false;
					}
					continue;
				}
				visitedIds.add(calculateId(segment, segmentEnd));

				int x = road.getPoint31XTile(segmentEnd);
				int y = road.getPoint31YTile(segmentEnd);
				RouteSegment next = ctx.loadRouteSegment(x, y, 0);
				RouteSegment toAdd = segment;
				if (!onTheMap.contains(toAdd.getRoad().getId())) {
					onTheMap.add(toAdd.getRoad().getId());
					// Visualization of steps !
					ctx.getVisitor().visitSegment(toAdd, true);
				}
				List<RouteSegment> nextSegments = new ArrayList<BinaryRoutePlanner.RouteSegment>();
				boolean out = false;
				boolean outDistance = false;
				while (next != null) {
					if (!visitedIds.contains(calculateId(next, next.getSegmentStart()))) {
						int tX = next.getRoad().getPoint31XTile(next.getSegmentStart()) >> zm;
						int tY = next.getRoad().getPoint31YTile(next.getSegmentStart()) >> zm;
						String highway = next.getRoad().getHighway();
						if(notClusterAtAll(next.getRoad())) {
							out = true;
						} else if(Math.abs(tX - tileX) < LOCAL_TILE_BOUNDARIES && Math.abs(tY - tileY) < LOCAL_TILE_BOUNDARIES) {
							nextSegments.add(next);
						} else {
							if (!isMajorHighway(highway) && Math.abs(tX - tileX) < TILE_BOUNDARIES && Math.abs(tY - tileY) < TILE_BOUNDARIES) {
								nextSegments.add(next);
							} else {
								outDistance = true;
							}
						}
					}
					next = next.getNext();
				}
				if(out) {
					outOfTile++;
					result.add(segment);
				} else if(outDistance) {
					outOfDistance++;
					result.add(segment);
				} else if(nextSegments.size() > 0) {
					queue.add(nextSegments);
				}
			}
		}
		log.info("Current ratio " + ((float) (queue.size() + outOfTile + outOfDistance) / segmentsProcessed) + " min ratio " + minRatio
				+ " min segments procesed " + segmentsMinProcessed );
		String res = "Processed " + segmentsProcessed + " and borders are " + outOfTile + " out because of distance " + outOfDistance;
		log.info(res);
		System.out.println(res);
		
		return result;
	}
	
	public boolean notClusterAtAll(RouteDataObject obj) {
		String highway = obj.getHighway();
		if(highway != null) {
			return highway.equals("trunk") ||  highway.equals("motorway") 
				   || highway.equals("primary")
				   || highway.equals("track");
			
		}
		String rte = obj.getRoute();
		if(rte != null) {
			return rte.equals("ferry");
		}
		return false;
	}
	
	public boolean isMajorHighway(String h) {
		if(h == null) {
			return false;
		}
		return h.equals("primary")
				|| h.equals("secondary");
	}

	@Override
	public void prepareToDraw() {
	}

	@Override
	public void paintLayer(Graphics2D g) {
	}

	

	public static String getHighway(RouteDataObject road) {
		return road.getHighway();
	}
}
