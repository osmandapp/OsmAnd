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
		ResultSet rs = st.executeQuery("SELECT dualIdPoint, idPoint, clusterId, chInd, roadId, start, end, sx31, sy31, ex31, ey31 from points");
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
	
	
	
	public void loadGeometry(NetworkDBSegment segment, int profile, boolean reload) throws SQLException {
		if (segment.geom != null && !reload) {
			return;
		}
		List<LatLon> geometry = segment.getGeometry();
		geometry.clear();
		geometry.addAll(parseGeometry(segment.start.index, segment.end.index, profile, segment.shortcut));
	}
	
	public void loadSegmentPointInternal(int id, int profile, byte[][] res) throws SQLException {
		loadSegmentStart.setInt(1, id);
		loadSegmentStart.setInt(2, profile);
		ResultSet rs = loadSegmentStart.executeQuery();
		if (rs.next()) {
			byte[] ins = rs.getBytes(2);
			res[0] = ins;
			byte[] outs = rs.getBytes(3);
			res[1] = outs;
		}
	}
	
	
	public <T extends NetworkDBPoint> int loadNetworkSegmentPoint(TLongObjectHashMap<T> pntsById, 
			TIntObjectHashMap<List<T>> clusterInPoints, TIntObjectHashMap<List<T>> clusterOutPoints, 
			int profile, NetworkDBPoint point, boolean reverse) throws SQLException {
		if (point.connected(reverse) != null) {
			return 0;
		}
		int loadedSegs = 0;
		if (compactDB) {
			loadSegmentStart.setInt(1, point.index);
			loadSegmentStart.setInt(2, profile);
			ResultSet rs = loadSegmentStart.executeQuery();
			if (rs.next()) {
				point.connectedSet(true, parseSegments(rs.getBytes(2), pntsById, clusterInPoints.get(point.clusterId), point, false));
				point.connectedSet(false, parseSegments(rs.getBytes(3), pntsById, clusterOutPoints.get(point.dualPoint.clusterId), point, true));
				return point.connected(true).size() + point.connected(false).size();
			} else {
				point.connectedSet(true, new ArrayList<>());
				point.connectedSet(false, new ArrayList<>());
			}
			return 0;
		} else {
			List<NetworkDBSegment> l = new ArrayList<>();
			@SuppressWarnings("resource")
			PreparedStatement pre = reverse ? loadSegmentStart : loadSegmentEnd;
			pre.setInt(1, point.index);
			pre.setInt(2, profile);
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
	
	

	public int loadNetworkSegments(Collection<? extends NetworkDBPoint> points, int routingProfile) throws SQLException {
		return loadNetworkSegmentsInternal(points, routingProfile, false);
	}
	
	public int loadNetworkSegmentsInternal(Collection<? extends NetworkDBPoint> points, int routingProfile, boolean excludeShortcuts) throws SQLException {
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

	private List<LatLon> parseGeometry(int start, int end, int profile, boolean shortcut) throws SQLException {
		List<LatLon> l = new ArrayList<LatLon>();
		loadGeometry.setLong(1, start);
		loadGeometry.setLong(2, end);
		loadGeometry.setInt(3, profile);
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
					List<LatLon> gg = parseGeometry(st, en, profile, false);
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
		List<LatLon> geom;
		
		public NetworkDBSegment(NetworkDBPoint start, NetworkDBPoint end, double dist, boolean direction, boolean shortcut) {
			this.direction = direction;
			this.start = start;
			this.end = end;
			this.shortcut = shortcut;
			this.dist = dist;
		}
		
		public List<LatLon> getGeometry() {
			if (geom == null) {
				geom = new ArrayList<LatLon>();
			}
			return geom;
		}
		
		@Override
		public String toString() {
			return String.format("Segment %s -> %s [%.2f] %s", start, end, dist, shortcut ? "sh" : "bs");
		}
		
	}
	
	
	static class NetworkDBPointRouteInfo {
		NetworkDBPoint rtRouteToPoint;
		boolean rtVisited;
		double rtDistanceFromStart;
		int rtDepth = -1; // possibly not needed (used 1)
		double rtDistanceToEnd; // possibly not needed (used 1)
		double rtCost;
		FinalRouteSegment rtDetailedRoute;
		
		public int getDepth(boolean rev) {
			if (rtDepth > 0) {
				return rtDepth;
			}
			if (rtRouteToPoint != null) {
				rtDepth = rtRouteToPoint.rt(rev).getDepth(rev) + 1; 
				return rtDepth ;
			}
			return 0;
		}
		
		public void setDetailedParentRt(FinalRouteSegment r) {
			double segmentDist = r.getDistanceFromStart();
			rtRouteToPoint = null;
			rtCost = rtDistanceToEnd + segmentDist;
			rtDetailedRoute = r;
			rtDistanceFromStart = segmentDist;
		}
		
		public void setCostParentRt(boolean rev, double cost, NetworkDBPoint point, double segmentDist) {
			rtCost = cost;
			rtRouteToPoint = point;
			rtDistanceFromStart = (point == null ? 0 : point.rt(rev).rtDistanceFromStart) + segmentDist;
		}
	}
	
	
	
	public static class NetworkDBPoint {
		public NetworkDBPoint dualPoint;
		public int index;
		public int clusterId;
		public int fileId;
		
		public long roadId;
		public short start;
		public short end;
		public int startX;
		public int startY;
		public int endX;
		public int endY;
		
		boolean rtExclude;
		NetworkDBPointRouteInfo rtRev;
		NetworkDBPointRouteInfo rtPos;
		
		List<NetworkDBSegment> connected = new ArrayList<NetworkDBSegment>();
		List<NetworkDBSegment> connectedReverse = new ArrayList<NetworkDBSegment>();
		
		public int midX() {
			return startX / 2 + endX / 2 ;
		}
		
		public int midY() {
			return startY / 2 + endY/ 2 ;
		}
		
		public NetworkDBPointRouteInfo rt(boolean rev) {
			if (rev) {
				if (rtRev == null) {
					rtRev = new NetworkDBPointRouteInfo();
				}
				return rtRev;
			} else {
				if (rtPos == null) {
					rtPos = new NetworkDBPointRouteInfo();
				}
				return rtPos;
			}
		}
		
		public List<NetworkDBSegment> connected(boolean rev) {
			return rev ? connectedReverse : connected;
		}
		
		public void setDistanceToEnd(boolean rev, double segmentDist) {
			rt(rev).rtDistanceToEnd = segmentDist;
		}
		

		public void markVisited(boolean rev) {
			rt(rev).rtVisited = true;
		}
		
		public void connectedSet(boolean rev, List<NetworkDBSegment> l) {
			if (rev) {
				connectedReverse = l;
			} else {
				connected = l;
			}
		}
		
		public void setCostParentRt(boolean reverse, double cost, NetworkDBPoint point, double segmentDist) {
			rt(reverse).setCostParentRt(reverse, cost, point, segmentDist);
		}
		
		public void setDetailedParentRt(boolean reverse, FinalRouteSegment r) {
			rt(reverse).setDetailedParentRt(r);
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
			rtPos = null;
			rtRev = null;
		}

		public int chInd() {
			return 0;
		}

		public int midPntDepth() {
			return 0;
		}

		public long getGeoPntId() {
			return HHRoutePlanner.calculateRoutePointInternalId(roadId, start, end);
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