package net.osmand.swing;

import gnu.trove.set.hash.TLongHashSet;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.data.DataTileManager;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentVisitor;


public class MapClusterLayer implements MapPanelLayer {

	private /*final */ static boolean ANIMATE_CLUSTERING = true;
	private /*final */ static int SIZE_OF_ROUTES_TO_ANIMATE = 50;
	
	
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

			public void actionPerformed(ActionEvent e) {
				clusteringRoadActions(true);
			}

			
		};
		menu.add(clustering);
		clustering= new AbstractAction("Clustering roads with tertiary") {
			private static final long serialVersionUID = 444678942490247134L;

			public void actionPerformed(ActionEvent e) {
				clusteringRoadActions(false);
			}
		};
		menu.add(clustering);

	}
	
	
	private Set<String> notClusterRoads = new LinkedHashSet<String>(Arrays.asList( "trunk", "trunk_link","motorway", "motorway_link",
			"primary", "primary_link", "secondary", "secondary_link", 
			"tertiary", "tertiary_link"));
	
	
	private Set<String> notClusterRoadsWOTertiary = new LinkedHashSet<String>(Arrays.asList( "trunk", "trunk_link","motorway", "motorway_link",
			"primary", "primary_link", "secondary", "secondary_link"));
	private void clusteringRoadActions(final boolean tertiary) {
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
					ways = clustering(latitude, longitude, tertiary ? notClusterRoads : notClusterRoadsWOTertiary);
					DataTileManager<Way> points = new DataTileManager<Way>();
					points.setZoom(11);
					for (Way w : ways) {
						LatLon n = w.getLatLon();
						points.registerObject(n.getLatitude(), n.getLongitude(), w);
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
	
	private List<Way> clustering(double lat, double lon, Set<String> roads) throws IOException{
		List<Way> res = new ArrayList<Way>();
		File[] files = DataExtractionSettings.getSettings().getDefaultRoutingFile();
		BinaryMapIndexReader[] rs = new BinaryMapIndexReader[files.length];
		for(int i=0; i<files.length; i++){
			RandomAccessFile raf = new RandomAccessFile(files[i], "r"); //$NON-NLS-1$ //$NON-NLS-2$
			rs[i] = new BinaryMapIndexReader(raf, true);
			
		}
		
		BinaryRoutePlanner router = new BinaryRoutePlanner(rs);
		RoutingContext ctx = new RoutingContext();
		
		// find closest way
		RouteSegment st = router.findRouteSegment(lat, lon, ctx);
		if (st != null) {
			BinaryMapDataObject road = st.getRoad();
			TagValuePair pair = road.getTagValue(0);
			System.out.println("ROAD TO START " + pair.tag + " " + pair.value + " " + road.getName() + " " 
					+ (road.getId() >> 3));
		}
		
		
		final DataTileManager<Way> points = new DataTileManager<Way>();
		points.setZoom(11);
		map.setPoints(points);
		ctx.setVisitor(new RouteSegmentVisitor() {
			
			private List<RouteSegment> cache = new ArrayList<RouteSegment>();
			
			@Override
			public void visitSegment(RouteSegment s) {
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
		searchCluster(ctx, st, router, res, roads);
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
	
	private List<RouteSegment> searchCluster(RoutingContext ctx, RouteSegment st, BinaryRoutePlanner router, List<Way> res, Set<String> roads) throws IOException {
		Queue<RouteSegment> queue  = new LinkedList<RouteSegment>();
		TLongHashSet visitedIds = new TLongHashSet();
		queue.add(st);
		BinaryMapDataObject startRoad = st.getRoad();
		long lstart = (((long) startRoad.getPoint31XTile(st.getSegmentStart())) << 31) + 
				(long) startRoad.getPoint31YTile(st.getSegmentStart());
		RouteSegment next = ctx.getLoadedRoutes().get(lstart);
		while (next != null) {
			if(next.getRoad().getId() != st.getRoad().getId()){
				queue.add(next);
			}
			next = next.getNext();
		}
		
		nextSegment : while(!queue.isEmpty()){
			RouteSegment segment = queue.poll();
			BinaryMapDataObject road = segment.getRoad();
			ctx.getVisitor().visitSegment(segment);
			if(visitedIds.contains(calculateId(segment, segment.getSegmentStart()))){
				continue;
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
			
			while(minusAllowed || plusAllowed){
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
				if(visitedIds.contains(calculateId(segment, segmentEnd))){
					if(currentD > 0){
						plusAllowed = false;
					} else {
						minusAllowed = false;
					}
					continue;
				}
				visitedIds.add(calculateId(segment, segmentEnd));

				int x = road.getPoint31XTile(segmentEnd);
				int y = road.getPoint31YTile(segmentEnd);
				if(segmentEnd > segment.getSegmentStart()){
					w.addNode(new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1));
				} else {
					w.addNode(new Node(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x), -1), 0);
				}

				router.loadRoutes(ctx, (x >> (31 - ctx.getZoomToLoadTileWithRoads())),
						(y >> (31 - ctx.getZoomToLoadTileWithRoads())));
				long l = (((long) x) << 31) + (long) y;
				next = ctx.getLoadedRoutes().get(l);
				boolean addToQueue = true;;
				while (next != null) {
					TagValuePair pair = next.getRoad().getTagValue(0);
					if (pair.tag.equals("highway") && roads.contains(pair.value)) {
						if(currentD > 0){
							plusAllowed = false;
						} else {
							minusAllowed = false;
						}
						addToQueue = false;
						break;
					}
					next = next.getNext();
				}
				
				if (addToQueue) {
					next = ctx.getLoadedRoutes().get(l);
					while (next != null) {
						if (!visitedIds.contains(calculateId(next, next.getSegmentStart()))) {
							queue.add(next);
						}
						next = next.getNext();
					}
				}
			}
				
			
		}
		
		return null;
	}

	@Override
	public void prepareToDraw() {
	}

	@Override
	public void paintLayer(Graphics g) {
		
	}

	


}
