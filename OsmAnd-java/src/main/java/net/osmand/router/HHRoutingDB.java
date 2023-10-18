package net.osmand.router;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class HHRoutingDB {

	public static final String EXT = ".hhdb";
	public static final String CEXT = ".chdb";

	protected static final int XY_SHORTCUT_GEOM = 0;

	protected Connection conn;
	protected  PreparedStatement loadGeometry;
	protected  PreparedStatement loadSegmentEnd;
	protected  PreparedStatement loadSegmentStart;

	protected  final int BATCH_SIZE = 10000;
	protected int batchInsPoint = 0;

	protected boolean compactDB;
	
	public HHRoutingDB(Connection conn) throws SQLException {
		this.conn = conn;
		Statement st = conn.createStatement();
		compactDB = checkColumnExist(st, "ins", "segments");
		if (!compactDB) {
			st.execute("CREATE TABLE IF NOT EXISTS points(idPoint, pointGeoUniDir, pointGeoId, chInd, roadId, start, end, sx31, sy31, ex31, ey31, PRIMARY KEY(idPoint))");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS pointsUnique on points(pointGeoId)");
			
			st.execute("CREATE TABLE IF NOT EXISTS segments(idPoint, idConnPoint, dist, shortcut)");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS segmentsUnique on segments(idPoint, idConnPoint)");
			st.execute("CREATE INDEX IF NOT EXISTS segmentsPntInd on segments(idPoint)");
			st.execute("CREATE INDEX IF NOT EXISTS segmentsConnPntInd on segments(idConnPoint)");
			
			st.execute("CREATE TABLE IF NOT EXISTS geometry(idPoint, idConnPoint, geometry, shortcut)");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS geometryMainInd on geometry(idPoint,idConnPoint,shortcut)");
			
			st.execute("CREATE TABLE IF NOT EXISTS midpoints(ind, maxMidDepth, proc, PRIMARY key (ind))"); // ind unique

			loadGeometry = conn.prepareStatement("SELECT geometry, shortcut FROM geometry WHERE idPoint = ? AND idConnPoint = ? ");
			loadSegmentEnd = conn.prepareStatement("SELECT idPoint, idConnPoint, dist, shortcut from segments where idPoint = ? ");
			loadSegmentStart = conn.prepareStatement("SELECT idPoint, idConnPoint, dist, shortcut from segments where idConnPoint = ? ");
		} else {
			loadSegmentStart = conn.prepareStatement("SELECT id, ins, outs from segments where id = ?  ");
		}
		st.close();
	}
	
	private List<NetworkDBSegment> parseSegments(byte[] bytes, TLongObjectHashMap<NetworkDBPoint> pntsById,
			NetworkDBPoint pnt, boolean out) {
		List<NetworkDBSegment> l = new ArrayList<>();
		for (int i = 0; i < bytes.length; i += 12) {
			int connId = Algorithms.parseIntFromBytes(bytes, i);
			NetworkDBPoint pnt2 = pntsById.get(connId);
			NetworkDBPoint start = out ? pnt : pnt2;
			NetworkDBPoint end = out ? pnt2 : pnt;
			float dist = Float.intBitsToFloat(Algorithms.parseIntFromBytes(bytes, i + 4));
			NetworkDBSegment seg = new NetworkDBSegment(start, end, dist, out,
					Algorithms.parseIntFromBytes(bytes, i + 8) > 0);
			l.add(seg);
		}
		return l;
	}

	private boolean checkColumnExist(Statement st, String col, String table) throws SQLException {
		try {
			return st.execute("SELECT "+ col+ " FROM " + table + " limit 1 ");
		} catch (SQLException e) {
			return false;
		}
	}
	

	
	public void loadMidPointsIndex(TLongObjectHashMap<NetworkDBPoint> pntsMap, Collection<NetworkDBPoint> pointsList, boolean update) throws SQLException {
		Statement s = conn.createStatement();
		for (NetworkDBPoint p : pointsList) {
			p.rtPrevCnt = 0;
		}
		PreparedStatement ps = conn.prepareStatement("UPDATE midpoints SET maxMidDepth = ?, proc = ? where ind = ?");
		int batch = 0;
		ResultSet rs = s.executeQuery("SELECT ind, maxMidDepth, proc  FROM midpoints ");
		while (rs.next()) {
			int ind = rs.getInt(1);
			NetworkDBPoint pnt = pntsMap.get(ind);
			boolean upd = false;
			if (pnt.rtCnt > rs.getInt(2)) {
				upd = true;
			} else {
				pnt.rtCnt = rs.getInt(2);
			}
			if (pnt.rtIndex == 1 && rs.getInt(3) == 0) {
				upd = true;
			} else {
				pnt.rtIndex = rs.getInt(3);
			}
			if (upd) {
				ps.setLong(1, pnt.rtCnt);
				ps.setLong(2, pnt.rtIndex);
				ps.setLong(3, pnt.index);
				ps.addBatch();
				if (batch++ > 1000) {
					batch = 0;
					ps.executeBatch();
				}
			}
			pnt.rtPrevCnt = 1;
		}
		ps.executeBatch();
		ps = conn.prepareStatement("INSERT INTO midpoints(ind, maxMidDepth, proc) VALUES(?, ?, ?)");
		batch = 0;
		for (NetworkDBPoint p : pointsList) {
			if (p.rtPrevCnt == 0 && (p.rtCnt > 0 || p.rtIndex > 0)) {
				ps.setLong(1, p.index);
				ps.setLong(2, p.rtCnt);
				ps.setLong(3, p.rtIndex);
				ps.addBatch();
				if (batch++ > 1000) {
					batch = 0;
					ps.executeBatch();
				}
			}
		}
		ps.executeBatch();
	}
	
	public TLongObjectHashMap<NetworkDBPoint> loadNetworkPoints() throws SQLException {
		return loadNetworkPoints(true);
	}
	
	public TLongObjectHashMap<NetworkDBPoint> loadNetworkPointsByGeoId() throws SQLException {
		return loadNetworkPoints(false);
	}
	
	private TLongObjectHashMap<NetworkDBPoint> loadNetworkPoints(boolean byId) throws SQLException {
		Statement st = conn.createStatement();
		String pntGeoIdCol = compactDB ? "pointGeoId, id": "pointGeoId, idPoint";
		ResultSet rs = st.executeQuery("SELECT "+pntGeoIdCol+", chInd, roadId, start, end, sx31, sy31, ex31, ey31 from points");
		TLongObjectHashMap<NetworkDBPoint> mp = new TLongObjectHashMap<>();
		TLongObjectHashMap<NetworkDBPoint> duals = new TLongObjectHashMap<>();
		while (rs.next()) {
			NetworkDBPoint pnt = new NetworkDBPoint();
			int p = 1;
			pnt.pntGeoId = rs.getLong(p++);
			pnt.index = rs.getInt(p++);
			pnt.chInd = rs.getInt(p++);
			pnt.roadId = rs.getLong(p++);
			pnt.start = rs.getInt(p++);
			pnt.end = rs.getInt(p++);
			pnt.startX = rs.getInt(p++);
			pnt.startY = rs.getInt(p++);
			pnt.endX = rs.getInt(p++);
			pnt.endY = rs.getInt(p++);
//			mp.put(byGeoId ? pnt.pntGeoId : pnt.index, pnt);
			mp.put(byId ? pnt.index : pnt.pntGeoId, pnt);
			long rpid = HHRoutePlanner.calculateRoutePointInternalId(pnt.roadId, Math.min(pnt.start, pnt.end), 
					Math.max(pnt.start, pnt.end));
			if (duals.contains(rpid)) {
				pnt.dualPoint = duals.get(rpid);
				pnt.dualPoint.dualPoint = pnt;
			} else {
				duals.put(rpid, pnt);
			}
		}
		rs.close();
		st.close();
		return mp;
	}
	
	
	public void loadGeometry(NetworkDBSegment segment, boolean reload) throws SQLException {
		if (!segment.geometry.isEmpty() && !reload) {
			return;
		}
		if (compactDB) {
			segment.geometry.add(segment.start.getPoint());
			segment.geometry.add(segment.end.getPoint());
			return;
		}
		segment.geometry.clear();
		segment.geometry.addAll(parseGeometry(segment.start.index, segment.end.index, segment.shortcut));
	}
	
	
	public int loadNetworkSegmentPoint(TLongObjectHashMap<NetworkDBPoint> pntsById, NetworkDBPoint point,
			boolean reverse) throws SQLException {
		if (point.connected(reverse) != null) {
			return 0;
		}
		int loadedSegs = 0;
		if (compactDB) {
			loadSegmentStart.setInt(1, point.index);
			ResultSet rs = loadSegmentStart.executeQuery();
			if (rs.next()) {
				point.connectedSet(true, parseSegments(rs.getBytes(2), pntsById, point, false));
				point.connectedSet(false, parseSegments(rs.getBytes(3), pntsById, point, true));
				return point.connected(true).size() + point.connected(false).size();
			}
			return 0;
		} else {
			List<NetworkDBSegment> l = new ArrayList<>();
			@SuppressWarnings("resource")
			PreparedStatement pre = reverse ? loadSegmentStart : loadSegmentEnd;
			pre.setInt(1, point.index);
			ResultSet rs = pre.executeQuery();
			while (rs.next()) {
				loadedSegs++;
				NetworkDBPoint start = pntsById.get(rs.getLong(1));
				NetworkDBPoint end = pntsById.get(rs.getLong(2));
				double dist = rs.getDouble(3);
				boolean shortcut = rs.getInt(4) > 0;
				NetworkDBSegment rev = new NetworkDBSegment(start, end, dist, !reverse, shortcut);
				l.add(rev);
			}
			point.connectedSet(reverse, l);
			rs.close();
		}
		return loadedSegs;
	}
	
	

	public int loadNetworkSegments(Collection<NetworkDBPoint> points) throws SQLException {
		return loadNetworkSegments(points, false);
	}
	
	public int loadNetworkSegments(Collection<NetworkDBPoint> points, boolean excludeShortcuts) throws SQLException {
		TLongObjectHashMap<NetworkDBPoint> pntsById = new TLongObjectHashMap<>();
		for (NetworkDBPoint p : points) {
			pntsById.put(p.index, p);
		}
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT idPoint, idConnPoint, dist, shortcut from segments");
		int x = 0;
		while (rs.next()) {
			boolean shortcut = rs.getInt(4) > 0;
			if (excludeShortcuts && shortcut) {
				continue;
			}
			x++;
			NetworkDBPoint start = pntsById.get(rs.getLong(1));
			NetworkDBPoint end = pntsById.get(rs.getLong(2));
			double dist = rs.getDouble(3);
			NetworkDBSegment segment = new NetworkDBSegment(start, end, dist, true, shortcut);
			NetworkDBSegment rev = new NetworkDBSegment(start, end, dist, false, shortcut);
			start.connected.add(segment);
			end.connectedReverse.add(rev);
		}
		rs.close();
		st.close();
		return x;
	}

	private List<LatLon> parseGeometry(int start, int end, boolean shortcut) throws SQLException {
		List<LatLon> l = new ArrayList<LatLon>();
		loadGeometry.setLong(1, start);
		loadGeometry.setLong(2, end);
		int shortcutN = shortcut ? 1 : 0;
		ResultSet rs = loadGeometry.executeQuery();
		while (rs.next()) {
			if (shortcutN != rs.getShort(2)) {
				continue;
			}
			byte[] geom = rs.getBytes(1);
			if (geom.length > 8 &&
					Algorithms.parseIntFromBytes(geom, 0) == XY_SHORTCUT_GEOM && 
					Algorithms.parseIntFromBytes(geom, 4) == XY_SHORTCUT_GEOM) {
				for (int k = 8; k < geom.length; k += 8) {
					int st = Algorithms.parseIntFromBytes(geom, k);
					int en = Algorithms.parseIntFromBytes(geom, k + 4);
					List<LatLon> gg = parseGeometry(st, en, false);
					l.addAll(gg);
				}
			} else {
				for (int k = 0; k < geom.length; k += 8) {
					int x = Algorithms.parseIntFromBytes(geom, k);
					int y = Algorithms.parseIntFromBytes(geom, k + 4);
					l.add(new LatLon(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x)));
				}
			}
		}
		if(l.isEmpty()) {
			System.err.printf("Empty route geometry %d -> %d  %s\n", start, end, shortcut ? "sh" : "bs");
		}
		return l;
	}

	

	public void close() throws SQLException {
		conn.close();
	}
	
	
	
	
	
	static class NetworkDBSegment {
		final boolean direction;
		final NetworkDBPoint start;
		final NetworkDBPoint end;
		final boolean shortcut;
		final double dist;
		List<LatLon> geometry = new ArrayList<>();
		TIntArrayList segmentsStartEnd = new TIntArrayList();
		// routing extra info
				
		public NetworkDBSegment(NetworkDBPoint start, NetworkDBPoint end, double dist, boolean direction, boolean shortcut) {
			this.direction = direction;
			this.start = start;
			this.end = end;
			this.shortcut = shortcut;
			this.dist = dist;
		}
		
		@Override
		public String toString() {
			return String.format("Segment %s -> %s [%.2f] %s", start, end, dist, shortcut ? "sh" : "bs");
		}
		
	}
	
	static class NetworkDBPoint {
		NetworkDBPoint dualPoint;
		int index;
		long chInd;
		long pntGeoId;
		public long roadId;
		public int start;
		public int end;
		public int startX;
		public int startY;
		public int endX;
		public int endY;
		
		List<NetworkDBSegment> connected = new ArrayList<NetworkDBSegment>();
		List<NetworkDBSegment> connectedReverse = new ArrayList<NetworkDBSegment>();
		
		// TODO Lightweight clear non used fields to free memory
		// for routing
		int rtDepth = -1;
		NetworkDBPoint rtRouteToPoint;
		boolean rtVisited;
		double rtDistanceFromStart;
		double rtDistanceToEnd;
		double rtCost;
		FinalRouteSegment rtDetailedRoute;
		
		int rtDepthRev = -1;
		NetworkDBPoint rtRouteToPointRev;
		boolean rtVisitedRev;
		double rtDistanceFromStartRev;
		double rtDistanceToEndRev;
		double rtCostRev;
		FinalRouteSegment rtDetailedRouteRev;
		
		// exclude from routing
		boolean rtExclude;
		
		// indexing
		int rtCnt = 0;
		int rtPrevCnt = 0;
		int rtLevel = 0;
		int rtIndex = 0;
		public TIntArrayList clusters;
		
		
		public List<NetworkDBSegment> connected(boolean rev) {
			return rev ? connectedReverse : connected;
		}
		
		public double distanceFromStart(boolean rev) {
			return rev ? rtDistanceFromStartRev : rtDistanceFromStart ;
		}
		
		public double distanceToEnd(boolean rev) {
			return rev ? rtDistanceToEndRev : rtDistanceToEnd ;
		}
		
		public void setDistanceToEnd(boolean rev, double segmentDist) {
			if (rev) {
				rtDistanceToEndRev = segmentDist;
			} else {
				rtDistanceToEnd = segmentDist;
			}
		}
		
		public double rtCost(boolean rev) {
			return rev ? rtCostRev : rtCost ;
		}
		
		public boolean visited(boolean rev) {
			return rev ? rtVisitedRev : rtVisited;
		}
		
		public void markVisited(boolean rev) {
			if (rev) {
				rtVisitedRev = true;
			} else {
				rtVisited = true;
			}
		}
		
		public void connectedSet(boolean rev, List<NetworkDBSegment> l) {
			if (rev) {
				connectedReverse = l;
			} else {
				connected = l;
			}
		}
		
		public void setCostParentRt(boolean reverse, double cost, NetworkDBPoint point, double segmentDist) {
			if (reverse) {
				rtCostRev = cost;
				rtRouteToPointRev = point;
				rtDistanceFromStartRev = (point == null ? 0 : point.rtDistanceFromStartRev) + segmentDist;
			} else {
				rtCost = cost;
				rtRouteToPoint = point;
				rtDistanceFromStart = (point == null ? 0 : point.rtDistanceFromStart) + segmentDist;
			}
		}
		
		public void setDetailedParentRt(boolean reverse, FinalRouteSegment r) {
			if (reverse) {
				rtDetailedRouteRev = r;
			} else {
				rtDetailedRoute = r;
			}
		}


		public void markSegmentsNotLoaded() {
			connected = null;
			connectedReverse = null;
		}
		
		
		
		@Override
		public String toString() {
			return String.format("Point %d (%d %d-%d)", index, roadId / 64, start, end);
		}
		
		public LatLon getPoint() {
			return new LatLon(MapUtils.get31LatitudeY(this.startY / 2 + this.endY / 2),
					MapUtils.get31LongitudeX(this.startX / 2 + this.endX / 2));
		}
		
		public NetworkDBSegment getSegment(NetworkDBPoint target, boolean dir) {
			List<NetworkDBSegment> l = (dir ? connected : connectedReverse);
			if (l == null) {
				return null;
			}
			for (NetworkDBSegment s : l) {
				if (dir && s.end == target) {
					return s;
				} else if (!dir && s.start == target) {
					return s;
				}
			}
			return null;
		}

		public void clearRouting() {
			rtExclude = false;
			rtDepth = -1;
			rtRouteToPoint = null;
			rtDistanceFromStart = 0;
			rtDistanceToEnd = 0;
			rtCost = 0;
			rtVisited = false;
			rtDetailedRoute = null;
			
			rtDepthRev = -1;
			rtRouteToPointRev = null;
			rtDistanceFromStartRev = 0;
			rtDistanceToEndRev = 0;
			rtCostRev = 0;
			rtVisitedRev = false;
			rtDetailedRouteRev = null;
		}

		public int getDepth(boolean dir) {
			if(dir && rtDepth > 0) {
				return rtDepth;
			} else if(!dir && rtDepthRev > 0) {
				return rtDepthRev;
			}
			if (dir && rtRouteToPoint != null) {
				rtDepth = rtRouteToPoint.getDepth(dir) + 1; 
				return rtDepth ;
			} else if (dir && rtRouteToPointRev != null) {
				rtDepthRev = rtRouteToPointRev.getDepth(dir) + 1;
				return rtDepthRev;
			}
			return 0;
		}


		
	}


}