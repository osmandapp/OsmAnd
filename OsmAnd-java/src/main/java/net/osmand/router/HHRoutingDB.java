package net.osmand.router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.google.protobuf.CodedInputStream;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
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
	protected PreparedStatement loadGeometry;
	protected PreparedStatement loadSegmentEnd;
	protected PreparedStatement loadSegmentStart;

	protected final int BATCH_SIZE = 10000;
	protected int batchInsPoint = 0;

	protected TIntObjectHashMap<String> routingProfiles = new TIntObjectHashMap<String>();
	protected boolean compactDB;
	protected int routingProfile;
	
	protected static Comparator<NetworkDBPoint> indexComparator = new Comparator<NetworkDBPoint>() {

		@Override
		public int compare(NetworkDBPoint o1, NetworkDBPoint o2) {
			return Integer.compare(o1.index, o2.index);
		}
	};
	
	public HHRoutingDB(Connection conn) throws SQLException {
		this.conn = conn;
		Statement st = conn.createStatement();
		compactDB = checkColumnExist(st, "ins", "segments");
		st.execute("CREATE TABLE IF NOT EXISTS profiles(id, params)");
		if (!compactDB) {
			st.execute("CREATE TABLE IF NOT EXISTS points(idPoint, pointGeoUniDir, pointGeoId, clusterId, fileDbId, dualIdPoint, dualClusterId, chInd, roadId, start, end, sx31, sy31, ex31, ey31, PRIMARY KEY(idPoint))");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS pointsUnique on points(pointGeoId)");
			st.execute("CREATE TABLE IF NOT EXISTS segments(idPoint, idConnPoint, dist, shortcut, profile)");
			if (!checkColumnExist(st, "profile", "segments")) {
				st.execute("DROP INDEX segmentsUnique");
				st.execute("DROP INDEX segmentsConnPntInd");
				st.execute("DROP INDEX segmentsPntInd");
				st.execute("DROP INDEX geometryMainInd");
				st.execute("INSERT INTO profiles(id, params) VALUES(0, '')");
				st.execute("ALTER TABLE geometry ADD COLUMN profile");
				st.execute("ALTER TABLE segments ADD COLUMN profile");
				st.execute("UPDATE segments SET profile = 0 where profile is null");
				st.execute("UPDATE geometry SET profile = 0 where profile is null");
			}
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS segmentsUnique on segments(idPoint, idConnPoint, profile)");
			st.execute("CREATE INDEX IF NOT EXISTS segmentsPntInd on segments(idPoint, profile)");
			st.execute("CREATE INDEX IF NOT EXISTS segmentsConnPntInd on segments(idConnPoint, profile)");
			
			st.execute("CREATE TABLE IF NOT EXISTS geometry(idPoint, idConnPoint, geometry, shortcut, profile)");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS geometryMainInd on geometry(idPoint,idConnPoint,shortcut, profile)");
			
			st.execute("CREATE TABLE IF NOT EXISTS midpoints(ind, maxMidDepth, proc, PRIMARY key (ind))"); // ind unique
			loadGeometry = conn.prepareStatement("SELECT geometry, shortcut FROM geometry WHERE idPoint = ? AND idConnPoint = ? AND profile = ?");
			loadSegmentEnd = conn.prepareStatement("SELECT idPoint, idConnPoint, dist, shortcut from segments where idPoint = ? AND profile = ?");
			loadSegmentStart = conn.prepareStatement("SELECT idPoint, idConnPoint, dist, shortcut from segments where idConnPoint = ? AND profile = ?");
		} else {
			if (!checkColumnExist(st, "profile", "segments")) {
				st.execute("ALTER TABLE segments ADD COLUMN profile");
				st.execute("INSERT INTO profiles(id, params) VALUES(0, '')");
				st.execute("UPDATE segments SET profile = 0 where profile is null");
				// we can't remove primary key so only one profile is possible for old file
			}
			loadSegmentStart = conn.prepareStatement("SELECT id, ins, outs from segments where id = ? and profile = ? ");
		}
		ResultSet rs = st.executeQuery("SELECT id, params from profiles");
		while (rs.next()) {
			routingProfiles.put(rs.getInt(1), rs.getString(2));
		}
		st.close();
	}
	
	public void selectRoutingProfile(int routingProfile) {
		this.routingProfile = routingProfile;
	}
	
	public int getRoutingProfile() {
		return routingProfile;
	}
	
	public TIntObjectHashMap<String> getRoutingProfiles() {
		return routingProfiles;
	}
	
	public int insertRoutingProfile(String profileParams) throws SQLException {
		TIntObjectIterator<String> it = routingProfiles.iterator();
		while(it.hasNext()) {
			it.advance();
			String p = it.value();
			if(p.equals(profileParams)) {
				return it.key();
			}
		}
		Statement s = conn.createStatement();
		int id = routingProfiles.size();
		routingProfiles.put(id, profileParams);
		s.execute("INSERT INTO profiles(id, params) VALUES("+id+", '"+profileParams+"')");
		return id;
	}
	
	private List<NetworkDBSegment> parseSegments(byte[] bytes, TLongObjectHashMap<? extends NetworkDBPoint> pntsById,
			List<? extends NetworkDBPoint> lst, NetworkDBPoint pnt, boolean out)  {
		try {
			List<NetworkDBSegment> l = new ArrayList<>();
			ByteArrayInputStream str = new ByteArrayInputStream(bytes);
			for (int i = 0; i < lst.size(); i++) {
				int d = CodedInputStream.readRawVarint32(str);
				if (d <= 0) {
					continue;
				}
				double dist = d / 10.0;
				NetworkDBPoint start = out ? pnt : lst.get(i);
				NetworkDBPoint end = out ? lst.get(i) : pnt;
				NetworkDBSegment seg = new NetworkDBSegment(start, end, dist, out, false);
				l.add(seg);
			}
			return l;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
	}

	private boolean checkColumnExist(Statement st, String col, String table) throws SQLException {
		try {
			return st.execute("SELECT "+ col+ " FROM " + table + " limit 1 ");
		} catch (SQLException e) {
			return false;
		}
	}
	

	
	
	public <T extends NetworkDBPoint> TLongObjectHashMap<T> loadNetworkPoints(Class<T> cl) throws SQLException {
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT dualIdPoint, pointGeoId, idPoint, clusterId, chInd, roadId, start, end, sx31, sy31, ex31, ey31 from points");
		TLongObjectHashMap<T> mp = new TLongObjectHashMap<>();
		while (rs.next()) {
			T pnt;
			try {
				pnt = cl.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			int p = 1;
			int dualIdPoint = rs.getInt(p++);
			if (dualIdPoint == 0) {
				// ignore non-dual point as they don't exist
				continue;
			}
			pnt.pntGeoId = rs.getLong(p++);
			pnt.index = rs.getInt(p++);
			pnt.clusterId = rs.getInt(p++);
			int chInd = rs.getInt(p++);
			if (pnt instanceof NetworkDBPointCh) {
				((NetworkDBPointCh) pnt).chInd = chInd;
			}
			pnt.roadId = rs.getLong(p++);
			pnt.start = rs.getShort(p++);
			pnt.end = rs.getShort(p++);
			pnt.startX = rs.getInt(p++);
			pnt.startY = rs.getInt(p++);
			pnt.endX = rs.getInt(p++);
			pnt.endY = rs.getInt(p++);
			mp.put(pnt.index, pnt);
			if (mp.contains(dualIdPoint)) {
				pnt.dualPoint = mp.get(dualIdPoint);
				pnt.dualPoint.dualPoint = pnt;
			} else {
				// will be processed later
			}
		}
		rs.close();
		st.close();
		return mp;
	}
	
	
	public <T extends NetworkDBPoint> TIntObjectHashMap<List<T>> groupByClusters(TLongObjectHashMap<T> pointsById, boolean out) {
		TIntObjectHashMap<List<T>> res = new TIntObjectHashMap<>();
		for (T p : pointsById.valueCollection()) {
			int cid = out ? p.clusterId : p.dualPoint.clusterId;
			if (!res.containsKey(cid)) {
				res.put(cid, new ArrayList<T>());
			}
			res.get(cid).add(p);
		}
		for(List<T> l : res.valueCollection()) {
			l.sort(indexComparator);
		}
		return res;
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
	
	
	public <T extends NetworkDBPoint> int loadNetworkSegmentPoint(TLongObjectHashMap<T> pntsById, 
			TIntObjectHashMap<List<T>> clusterInPoints, TIntObjectHashMap<List<T>> clusterOutPoints, 
			NetworkDBPoint point, boolean reverse) throws SQLException {
		if (point.connected(reverse) != null) {
			return 0;
		}
		int loadedSegs = 0;
		if (compactDB) {
			loadSegmentStart.setInt(1, point.index);
			loadSegmentStart.setInt(2, routingProfile);
			ResultSet rs = loadSegmentStart.executeQuery();
			if (rs.next()) {
				point.connectedSet(true, parseSegments(rs.getBytes(2), pntsById, clusterInPoints.get(point.clusterId), point, false));
				point.connectedSet(false, parseSegments(rs.getBytes(3), pntsById, clusterOutPoints.get(point.dualPoint.clusterId), point, true));
				return point.connected(true).size() + point.connected(false).size();
			}
			return 0;
		} else {
			List<NetworkDBSegment> l = new ArrayList<>();
			@SuppressWarnings("resource")
			PreparedStatement pre = reverse ? loadSegmentStart : loadSegmentEnd;
			pre.setInt(1, point.index);
			pre.setInt(2, routingProfile);
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
	
	

	public int loadNetworkSegments(Collection<? extends NetworkDBPoint> points) throws SQLException {
		return loadNetworkSegmentsInternal(points, false);
	}
	
	public int loadNetworkSegmentsInternal(Collection<? extends NetworkDBPoint> points, boolean excludeShortcuts) throws SQLException {
		TLongObjectHashMap<NetworkDBPoint> pntsById = new TLongObjectHashMap<>();
		for (NetworkDBPoint p : points) {
			pntsById.put(p.index, p);
		}
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT idPoint, idConnPoint, dist, shortcut from segments where profile = " + routingProfile);
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
		loadGeometry.setInt(3, routingProfile);
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
	
	
	static class NetworkDBPointRouteInfo {
		int rtDepth = -1;
		NetworkDBPoint rtRouteToPoint;
		// TODO do we need all fields?
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
		
		public int getDepth(boolean dir) {
			if (dir && rtDepth > 0) {
				return rtDepth;
			} else if(!dir && rtDepthRev > 0) {
				return rtDepthRev;
			}
			if (dir && rtRouteToPoint != null) {
				rtDepth = rtRouteToPoint.route.getDepth(dir) + 1; 
				return rtDepth ;
			} else if (dir && rtRouteToPointRev != null) {
				rtDepthRev = rtRouteToPointRev.route.getDepth(dir) + 1;
				return rtDepthRev;
			}
			return 0;
		}
		
		public void setDetailedParentRt(boolean reverse, FinalRouteSegment r) {
			double segmentDist = r.getDistanceFromStart();
			if (reverse) {
				rtRouteToPointRev = null;
				rtCostRev = rtDistanceToEndRev + segmentDist;
				rtDetailedRouteRev = r;
				rtDistanceFromStartRev = segmentDist;
			} else {
				rtRouteToPoint = null;
				rtCost = rtDistanceToEnd + segmentDist;
				rtDetailedRoute = r;
				rtDistanceFromStart = segmentDist;
			}
		}
		
		public void setCostParentRt(boolean reverse, double cost, NetworkDBPoint point, double segmentDist) {
			if (reverse) {
				rtCostRev = cost;
				rtRouteToPointRev = point;
				rtDistanceFromStartRev = (point == null ? 0 : point.rt().rtDistanceFromStartRev) + segmentDist;
			} else {
				rtCost = cost;
				rtRouteToPoint = point;
				rtDistanceFromStart = (point == null ? 0 : point.rt().rtDistanceFromStart) + segmentDist;
			}
		}
	}
	
	
	
	static class NetworkDBPoint {
		NetworkDBPoint dualPoint;
		int index;
		int clusterId;
		long pntGeoId; // could be calculated
		
		public long roadId;
		public short start;
		public short end;
		public int startX;
		public int startY;
		public int endX;
		public int endY;
		
		boolean rtExclude;
		private NetworkDBPointRouteInfo route;
//		private NetworkDBPointRouteInfoExtra extra;
		
		List<NetworkDBSegment> connected = new ArrayList<NetworkDBSegment>();
		List<NetworkDBSegment> connectedReverse = new ArrayList<NetworkDBSegment>();
		
		public int midX() {
			return startX / 2 + endX / 2 ;
		}
		
		public int midY() {
			return startY / 2 + endY/ 2 ;
		}
		
		public NetworkDBPointRouteInfo rt() {
			if (route == null) {
				route = new NetworkDBPointRouteInfo();
			}
			return route;
		}
		
		public List<NetworkDBSegment> connected(boolean rev) {
			return rev ? connectedReverse : connected;
		}
		
		public double distanceFromStart(boolean rev) {
			return rev ? rt().rtDistanceFromStartRev : rt().rtDistanceFromStart ;
		}
		
		public double distanceToEnd(boolean rev) {
			return rev ? rt().rtDistanceToEndRev : rt().rtDistanceToEnd ;
		}
		
		public void setDistanceToEnd(boolean rev, double segmentDist) {
			if (rev) {
				rt().rtDistanceToEndRev = segmentDist;
			} else {
				rt().rtDistanceToEnd = segmentDist;
			}
		}
		
		public double rtCost(boolean rev) {
			return rev ? rt().rtCostRev : rt().rtCost ;
		}
		
		public boolean visited(boolean rev) {
			return rev ? rt().rtVisitedRev : rt().rtVisited;
		}
		
		public void markVisited(boolean rev) {
			if (rev) {
				rt().rtVisitedRev = true;
			} else {
				rt().rtVisited = true;
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
			rt().setCostParentRt(reverse, cost, point, segmentDist);
		}
		
		public void setDetailedParentRt(boolean reverse, FinalRouteSegment r) {
			rt().setDetailedParentRt(reverse, r);
		}
		
		public FinalRouteSegment getDetailedRouteRt(boolean reverse) {
			return reverse ? rt().rtDetailedRouteRev : rt().rtDetailedRoute;
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
			route = new NetworkDBPointRouteInfo();
		}

		public int chInd() {
			return 0;
		}

		public int midPntDepth() {
			return 0;
		}
		
	}

	static class NetworkDBPointMid extends NetworkDBPoint  {
		
		int rtMidPointDepth = 0;
		
		public int midPntDepth() {
			return rtMidPointDepth;
		}
	}

	static class NetworkDBPointCh extends NetworkDBPoint {
		int chInd;
		
		public int chInd() {
			return chInd;
		}
	}

}