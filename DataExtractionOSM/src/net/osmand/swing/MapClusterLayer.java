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
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
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
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MapClusterLayer implements MapPanelLayer {

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
	
	private class ClusteringContext {
		// final
		boolean ANIMATE_CLUSTERING = false;
		boolean BASEMAP_CLUSTERING = true;

		int ZOOM_LIMIT = BASEMAP_CLUSTERING ? 11 : 15;
		int LOCAL_TILE_BOUNDARIES = BASEMAP_CLUSTERING? 4 : 4;
		int zm = 31 - ZOOM_LIMIT;
		
		
		// variable
		int outOfTile = 0;
		int outOfDistance = 0;
		int roadProcessed = 0;
		int segmentsProcessed = 0;
		float minRatio = 1f;
		int roadMinProcessed = 0;
	}
	
	
	private void clusteringRoadActions() {
		Point popupMenuPoint = map.getPopupMenuPoint();
		double fy = (popupMenuPoint.y - map.getCenterPointY()) / map.getTileSize();
		double fx = (popupMenuPoint.x - map.getCenterPointX()) / map.getTileSize();
		final double latitude = MapUtils.getLatitudeFromTile(map.getZoom(), map.getYTile() + fy);
		final double longitude = MapUtils.getLongitudeFromTile(map.getZoom(), map.getXTile() + fx);
		final ClusteringContext clusterCtx = new ClusteringContext();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DataTileManager<Way> points = new DataTileManager<Way>(11);
					List<RouteSegment> ways = clustering(clusterCtx, latitude, longitude, points);
					for (RouteSegment s : ways) {
						Way w = new Way(-1);
						int st = s.getSegmentStart();
						int end = s.getParentSegmentEnd();
						if (st > end) {
							int t = st;
							st = end;
							end = t;

							for (int i = st; i <= end; i++) {
								net.osmand.osm.Node n = new net.osmand.osm.Node(MapUtils.get31LatitudeY(s.getRoad().getPoint31YTile(i)),
										MapUtils.get31LongitudeX(s.getRoad().getPoint31XTile(i)), -1);
								w.addNode(n);
							}
							LatLon n = w.getLatLon();
							points.registerObject(n.getLatitude(), n.getLongitude(), w);
						}
					}
					map.setPoints(points);
					map.repaint();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}).start();
	}
	
	private List<RouteSegment> clustering(final ClusteringContext clusterCtx, double lat, double lon,
			final DataTileManager<Way> points ) throws IOException{
		List<BinaryMapIndexReader> rs = new ArrayList<BinaryMapIndexReader>();
		for (File f : new File(DataExtractionSettings.getSettings().getBinaryFilesDir()).listFiles()) {
			if (f.getName().endsWith(".obf")) {
				RandomAccessFile raf = new RandomAccessFile(f, "r"); //$NON-NLS-1$ //$NON-NLS-2$
				rs.add(new BinaryMapIndexReader(raf));
			}
		}
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd(true);
		Builder builder = RoutingConfiguration.getDefault();
		RoutingConfiguration config = builder.build("car", RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3);
		RoutingContext ctx = new RoutingContext(config, NativeSwingRendering.getDefaultFromSettings(),
				rs.toArray(new BinaryMapIndexReader[rs.size()]), clusterCtx.BASEMAP_CLUSTERING);
		// find closest way
		RouteSegment st = router.findRouteSegment(lat, lon, ctx);
		if (st != null) {
			RouteDataObject road = st.getRoad();
			String highway = getHighway(road);
			log.info("ROAD TO START " + highway + " " + //road.getName() + " " 
					+ road.id);
		}
		map.setPoints(points);
		
		ctx.setVisitor(new RouteSegmentVisitor() {
			private List<RouteSegment> cache = new ArrayList<RouteSegment>();
			
			@Override
			public void visitSegment(RouteSegment s, int endSegment, boolean poll) {
				if(!clusterCtx.ANIMATE_CLUSTERING){
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
					way.putTag("color", "white");
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
		List<RouteSegment> results = searchCluster(clusterCtx, ctx, st);
		return results;
	}
	
	
	
	
	
	private long calculateId(RouteSegment r){
		return (r.getRoad().getId() /*<< 8*/) /*+ segment*/;
	}
	
	
	private List<RouteSegment> searchCluster(ClusteringContext cCtx, RoutingContext ctx, RouteSegment st) throws IOException {
		
		List<RouteSegment> result = new ArrayList<BinaryRoutePlanner.RouteSegment>();
		TLongHashSet visitedIds = new TLongHashSet();
		
		RouteDataObject startRoad = st.getRoad();
		final int stx = startRoad.getPoint31XTile(st.getSegmentStart());
		final int sty = startRoad.getPoint31YTile(st.getSegmentStart());
		int tileX = startRoad.getPoint31XTile(st.getSegmentStart()) >> cCtx.zm;
		int tileY = startRoad.getPoint31YTile(st.getSegmentStart()) >> cCtx.zm;
						
		Queue<RouteSegment> queue  = new PriorityQueue<RouteSegment>(50, new Comparator<RouteSegment>() {

			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				double d1 = MapUtils.squareDist31TileMetric(stx, sty, o1.getRoad().getPoint31XTile(o1.getSegmentStart()),
						o1.getRoad().getPoint31YTile(o1.getSegmentStart()));
				double d2 = MapUtils.squareDist31TileMetric(stx, sty, o2.getRoad().getPoint31XTile(o2.getSegmentStart()),
						o2.getRoad().getPoint31YTile(o2.getSegmentStart()));
				return Double.compare(d1, d2);
			}
		});
		queue.add(st);
		
		while (!queue.isEmpty()) {
			RouteSegment segment = queue.poll();
			if (visitedIds.contains(calculateId(segment))) {
				// System.out.println("contains " + segment.getRoad());
				continue;
			}
			// System.out.println(segment.getRoad());
			visitedIds.add(calculateId(segment));
			// Visualization of steps !
			if (ctx.getVisitor() != null) {
				ctx.getVisitor().visitSegment(segment, -1, true);
			}
			cCtx.roadProcessed++;
			if (cCtx.roadProcessed > 50) {
				float ratio = (float) (queue.size() + cCtx.outOfTile + cCtx.outOfDistance) / cCtx.segmentsProcessed;
				if (ratio < cCtx.minRatio) {
					cCtx.minRatio = ratio;
					cCtx.roadMinProcessed = cCtx.roadProcessed;
				}
			}
			processSegment(cCtx, ctx, segment, queue, result, tileX, tileY, true);
			processSegment(cCtx, ctx, segment, queue, result, tileX, tileY, false);
		}
		System.out.println("Current ratio " + ((float) (queue.size() + cCtx.outOfTile + cCtx.outOfDistance) / cCtx.segmentsProcessed) + " min ratio " + cCtx.minRatio
				+ " min segments procesed " + cCtx.roadMinProcessed );
		String res = "Processed " + cCtx.roadProcessed + " / " + cCtx.segmentsProcessed+ " and borders are " + (cCtx.outOfTile + cCtx.outOfDistance) + " out because of distance " + cCtx.outOfDistance;
		log.info(res);
		
		return result;
	}
	
	private void addSegmentResult(List<RouteSegment> result , RouteSegment sgm, int segmentSt, int segmentEnd) {
		RouteSegment r = new RouteSegment(sgm.getRoad(), segmentSt);
		r.setParentSegmentEnd(segmentEnd);
		result.add(r);
	}

	private void processSegment(ClusteringContext cCtx, RoutingContext ctx, RouteSegment segment, Queue<RouteSegment> queue, List<RouteSegment> result,
			int tileX, int tileY, boolean direction ) {
			
			int d = 1;
			boolean directionAllowed = true;
			int prev = segment.getSegmentStart();
			while (directionAllowed) {
				int segmentEnd = segment.getSegmentStart() + (direction?d : -d);
				d++;
				if (segmentEnd < 0 || segmentEnd >= segment.getRoad().getPointsLength()) {
					directionAllowed = false;
					continue;
				}
				int x = segment.getRoad().getPoint31XTile(segmentEnd);
				int y = segment.getRoad().getPoint31YTile(segmentEnd);
				int tX = x >> cCtx.zm;
				int tY = y >> cCtx.zm;
				cCtx.segmentsProcessed ++;
				if(notClusterAtAll(cCtx, segment.getRoad())) {
					cCtx.outOfTile++;
					addSegmentResult(result, segment, prev, segmentEnd);
					return;
				} 
				if(Math.abs(tX - tileX) > cCtx.LOCAL_TILE_BOUNDARIES || Math.abs(tY - tileY) > 
						cCtx.LOCAL_TILE_BOUNDARIES) {
//					System.out.println("Res " + segment.getRoad());
					cCtx.outOfDistance++;
					addSegmentResult(result, segment, prev, segmentEnd);
					return;
				}
				RouteSegment next = ctx.loadRouteSegment(x, y, 0);
				while (next != null) {
//					System.out.println(" >> " + next.getRoad());
					queue.add(next);
					next = next.getNext();
					
				}
				prev = segmentEnd;
			}
			
		}
	
	public boolean notClusterAtAll(ClusteringContext clusterCtx, RouteDataObject obj) {
		return false;
	}
	
	public boolean isMajorHighway(ClusteringContext clusterCtx, String h) {
		if(h == null) {
			return false;
		}
		if(clusterCtx.BASEMAP_CLUSTERING) {
			return h.equals("motorway") || h.equals("trunk");
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
