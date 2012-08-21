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

	private /*final */ static boolean ANIMATE_CLUSTERING = true;
	private /*final */ static int SIZE_OF_ROUTES_TO_ANIMATE = 3;
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
				List<Way> ways;
				try {
					ways = clustering(latitude, longitude);
//					DataTileManager<Way> points = new DataTileManager<Way>();
//					points.setZoom(11);
//					for (Way w : ways) {
//						LatLon n = w.getLatLon();
//						points.registerObject(n.getLatitude(), n.getLongitude(), w);
//					}
//					map.setPoints(points);
//					map.repaint();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}).start();
	}
	
	private List<Way> clustering(double lat, double lon) throws IOException{
		List<Way> res = new ArrayList<Way>();
		List<BinaryMapIndexReader> rs = new ArrayList<BinaryMapIndexReader>();
		for (File f : new File(DataExtractionSettings.getSettings().getBinaryFilesDir()).listFiles()) {
			if (f.getName().endsWith(".obf")) {
				RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
				rs.add(new BinaryMapIndexReader(raf, false));
			}
		}
		BinaryRoutePlanner router = new BinaryRoutePlanner(NativeSwingRendering.getDefaultFromSettings(),
				rs.toArray(new BinaryMapIndexReader[rs.size()]));
		Builder builder = RoutingConfiguration.getDefault();
		RoutingConfiguration config = builder.build("car");
		config.NUMBER_OF_DESIRABLE_TILES_IN_MEMORY = 300;
		RoutingContext ctx = new RoutingContext(config);
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
		searchCluster(ctx, st, router, res);
		if (ANIMATE_CLUSTERING) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
		}
		return res;
	}
	
	
	
	
	
	private long calculateId(RouteSegment r, int segment){
		return (r.getRoad().getId() << 8) + segment;
	}
	
	private List<RouteSegment> searchCluster(RoutingContext ctx, RouteSegment st, BinaryRoutePlanner router, List<Way> res) throws IOException {
		Queue<RouteSegment> queue  = new LinkedList<RouteSegment>();
		TLongHashSet visitedIds = new TLongHashSet();
		queue.add(st);
		RouteDataObject startRoad = st.getRoad();
		long lstart = (((long) startRoad.getPoint31XTile(st.getSegmentStart())) << 31) + 
				(long) startRoad.getPoint31YTile(st.getSegmentStart());
		int zlimit = 13;
		int zm = 31 - zlimit;
		int tileX = startRoad.getPoint31XTile(st.getSegmentStart()) >> zm;
		int tileY = startRoad.getPoint31YTile(st.getSegmentStart()) >> zm;
		RouteSegment next = ctx.getRoutingTile((int) lstart >> 31, (int) (lstart - (lstart >> 31) << 31)).getSegment(lstart, ctx);
		while (next != null) {
			if(next.getRoad().getId() != st.getRoad().getId()){
				queue.add(next);
			}
			next = next.getNext();
		}
		
		int outOfTile = 0;
		int segmentsProcessed = 0;
		float minRatio = 1f;
		TLongHashSet onTheMap = new TLongHashSet();
		nextSegment : while(!queue.isEmpty()){
			RouteSegment segment = queue.poll();
			RouteDataObject road = segment.getRoad();
			segmentsProcessed++;
			if(visitedIds.contains(calculateId(segment, segment.getSegmentStart()))){
				continue;
			}
			if(segmentsProcessed > 50) {
				minRatio = Math.min(minRatio, (float) (queue.size() + outOfTile) / segmentsProcessed);
			}
			visitedIds.add(calculateId(segment, segment.getSegmentStart()));
			Way w = new Way(-1);
			res.add(w);
			int xst = road.getPoint31XTile(segment.getSegmentStart());
			int yst = road.getPoint31YTile(segment.getSegmentStart());
			w.addNode(new Node(MapUtils.get31LatitudeY(yst), 
					MapUtils.get31LongitudeX(xst), -1));
			
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
				if (segmentEnd > segment.getSegmentStart()) {
					w.addNode(new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1));
				} else {
					w.addNode(new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1), 0);
				}

				router.loadRoutes(ctx, x, y);
				long l = (((long) x) << 31) + (long) y;
				next = ctx.getRoutingTile(x, y).getSegment(l, ctx);
				RouteSegment toAdd = segment;
				if (!onTheMap.contains(toAdd.getRoad().getId())) {
					onTheMap.add(toAdd.getRoad().getId());
					ctx.getVisitor().visitSegment(toAdd, true);
				}
				while (next != null) {
					if (!visitedIds.contains(calculateId(next, next.getSegmentStart()))) {
						int tX = next.getRoad().getPoint31XTile(next.getSegmentStart()) >> zm;
						int tY = next.getRoad().getPoint31YTile(next.getSegmentStart()) >> zm;
						String highway = next.getRoad().getHighway();
						if(tX == tileX && tY == tileY) {
							queue.add(next);
						} else {
							double r = 1; //Math.random();
							if (!isMajorHighway(highway) && Math.abs(tX - tileX) < (2 * r) && Math.abs(tY - tileY) < (2 * r)) {
								queue.add(next);
							} else {
								outOfTile++;
							}
						}
					}
					next = next.getNext();
				}
			}
		}
		System.out.println("Current ratio " + ((float) outOfTile / segmentsProcessed ) + " min ratio " + minRatio);
		System.out.println("Processed " + segmentsProcessed + " and borders are " + outOfTile);
		
		return null;
	}
	
	public boolean isMajorHighway(String h) {
		if(h == null) {
			return false;
		}
		return h.startsWith("trunk") || h.startsWith("primary") || h.startsWith("motorway")
				//|| h.startsWith("track");
				|| h.startsWith("secondary") || h.startsWith("tertiary")
				;
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
