package net.osmand.router;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class HHRoutingPreparationDB {

	public static final String EXT = ".hhdb";
	public static final String CEXT = ".chdb";

	private static final int XY_SHORTCUT_GEOM = 0;

	private Connection conn;
	private PreparedStatement insSegment;
	private PreparedStatement insGeometry;
	private PreparedStatement insCluster;
	private PreparedStatement insPoint;
	private PreparedStatement loadGeometry;
	private PreparedStatement loadSegmentEnd;
	private PreparedStatement loadSegmentStart;

	private final int BATCH_SIZE = 10000;
	private int batchInsPoint = 0;

	private boolean compactDB;
	
	
	public HHRoutingPreparationDB(Connection conn) throws SQLException {
		this.conn = conn;
		Statement st = conn.createStatement();
		compactDB = checkColumnExist(st, "ins", "segments");
		if (!compactDB) {
			st.execute(
					"CREATE TABLE IF NOT EXISTS points(idPoint, ind, chInd, roadId, start, end, sx31, sy31, ex31, ey31,  PRIMARY key (idPoint))"); // ind
																																					// unique
			st.execute(
					"CREATE TABLE IF NOT EXISTS clusters(idPoint, indPoint, clusterInd, PRIMARY key (indPoint, clusterInd))");
			st.execute("CREATE TABLE IF NOT EXISTS segments(idPoint, idConnPoint, dist, shortcut)");
			st.execute("CREATE INDEX IF NOT EXISTS segmentsPntInd on segments(idPoint)");
			st.execute("CREATE INDEX IF NOT EXISTS segmentsConnPntInd on segments(idConnPoint)");
			st.execute("CREATE TABLE IF NOT EXISTS geometry(idPoint, idConnPoint, geometry, shortcut)");

			st.execute(
					"CREATE TABLE IF NOT EXISTS routeRegions(id, name, filePointer, size, filename, left, right, top, bottom, PRIMARY key (id))");
			st.execute("CREATE TABLE IF NOT EXISTS routeRegionPoints(id, pntId)");
			st.execute("CREATE INDEX IF NOT EXISTS routeRegionPointsIndex on routeRegionPoints(id)");
			st.execute("CREATE TABLE IF NOT EXISTS midpoints(ind, maxMidDepth, proc, PRIMARY key (ind))"); // ind unique

			st.execute("CREATE INDEX IF NOT EXISTS geometryMainInd on geometry(idPoint,idConnPoint,shortcut)");

			insPoint = conn
					.prepareStatement("INSERT INTO points(idPoint, ind, roadId, start, end, sx31, sy31, ex31, ey31) "
							+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
			insCluster = conn.prepareStatement("INSERT INTO clusters(idPoint, indPoint, clusterInd) VALUES(?, ?, ?)");
			loadGeometry = conn.prepareStatement("SELECT geometry, shortcut FROM geometry WHERE idPoint = ? AND idConnPoint = ? ");
			loadSegmentEnd = conn.prepareStatement("SELECT idPoint, idConnPoint, dist, shortcut from segments where idPoint = ? ");
			loadSegmentStart = conn.prepareStatement("SELECT idPoint, idConnPoint, dist, shortcut from segments where idConnPoint = ? ");
		} else {
			loadSegmentStart = conn.prepareStatement("SELECT id, ins, outs from segments where id = ?  ");
		}
		st.close();
	}
	
	public static void compact(Connection src, Connection tgt ) throws SQLException {
		Statement st = tgt.createStatement();
		st.execute("CREATE TABLE IF NOT EXISTS points(pointGeoId, id, chInd, roadId, start, end, sx31, sy31, ex31, ey31,  PRIMARY key (id))"); // ind unique
		st.execute("CREATE TABLE IF NOT EXISTS segments(id, ins, outs, PRIMARY key (id))");
		
		PreparedStatement pIns = tgt.prepareStatement("INSERT INTO points(pointGeoId, id, chInd, roadId, start, end, sx31, sy31, ex31, ey31)  "
				+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		ResultSet rs = src.createStatement().executeQuery(" select idPoint, ind, chInd, roadId, start, end, sx31, sy31, ex31, ey31 from points");
		TIntArrayList ids = new TIntArrayList();
		while(rs.next()) {
			ids.add(rs.getInt(2));
			for(int i = 0; i < 10; i++) {
				pIns.setObject(i + 1, rs.getObject(i + 1));
			}
			pIns.addBatch();
		}
		pIns.executeBatch();
		PreparedStatement sIns = tgt.prepareStatement("INSERT INTO segments(id, ins, outs)  VALUES (?, ?, ?)");
		PreparedStatement selOut = src.prepareStatement(" select idConnPoint, dist, shortcut from segments where idPoint = ?");
		PreparedStatement selIn = src.prepareStatement(" select idPoint, dist, shortcut from segments where idConnPoint = ?");
		for (int id : ids.toArray()) {
			selIn.setInt(1, id);
			selOut.setInt(1, id);
			sIns.setInt(1, id);
			sIns.setBytes(2, prepareSegments(selIn));
			sIns.setBytes(3, prepareSegments(selOut));
			sIns.addBatch();
		}
		
		sIns.executeBatch();
		
		tgt.close();
		
	}

	private static byte[] prepareSegments(PreparedStatement selIn) throws SQLException {
		TIntArrayList bs = new TIntArrayList();
		ResultSet q = selIn.executeQuery();
		while(q.next()) {
			int conn = q.getInt(1);
			bs.add(conn);
			float dist = q.getFloat(2);
			bs.add(Float.floatToIntBits(dist)); // distance
			bs.add(q.getInt(3)); // shortcut
		}
		
		byte[] bytes = new byte[bs.size() * 4];
		for (int i = 0; i < bs.size(); i ++) {
			Algorithms.putIntToBytes(bytes, i * 4, bs.get(i));
		}
		return bytes;
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
	
	public void recreateSegments() throws SQLException {
		Statement st = conn.createStatement();
 		st.execute("DELETE FROM segments");
 		st.execute("DELETE FROM geometry");
 		st.close();
	}

	public int getMaxClusterId() throws SQLException {
		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("select max(clusterInd) from clusters");
		if (rs.next()) {
			return rs.getInt(1) + 1;
		}
		rs.close();
		s.close();
		return 0;
	}
	
	public void loadNetworkPoints(TLongObjectHashMap<Integer> networkPointsCluster) throws SQLException {
		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("SELECT idPoint, ind FROM points ");
		while(rs.next()) {
			networkPointsCluster.put(rs.getLong(1), rs.getInt(2));
		}
		rs.close();
		s.close();
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
	
	public TLongObjectHashMap<NetworkDBPoint> getNetworkPoints(boolean byGeoId) throws SQLException {
		Statement st = conn.createStatement();
		String pntGeoIdCol = compactDB ? "pointGeoId, id": "idPoint, ind";
		ResultSet rs = st.executeQuery("SELECT "+pntGeoIdCol+", chInd, roadId, start, end, sx31, sy31, ex31, ey31 from points");
		TLongObjectHashMap<NetworkDBPoint> mp = new TLongObjectHashMap<>();
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
			mp.put(byGeoId ? pnt.pntGeoId : pnt.index, pnt);
		}
		rs.close();
		st.close();
		return mp;
	}
	
	public void loadClusterData(TLongObjectHashMap<NetworkDBPoint> pnts, boolean byId) throws SQLException {
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT " + (byId ? "idPoint" : "indPoint") + ", clusterInd from clusters");
		while (rs.next()) {
			NetworkDBPoint pnt = pnts.get(rs.getLong(1));
			if (pnt.clusters == null) {
				pnt.clusters = new TIntArrayList();
			}
			pnt.clusters.add(rs.getInt(2));
		}
		rs.close();
		st.close();		
	}
	
	public boolean hasVisitedPoints(NetworkRouteRegion nrouteRegion) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT pntId FROM routeRegionPoints WHERE id = ? ");
		ps.setLong(1, nrouteRegion.id);
		ResultSet rs = ps.executeQuery();
		boolean has = false;
		if (rs.next()) {
			has = true;
		}
		rs.close();
		ps.close();
		return has;
	}
	
	public void loadVisitedVertices(NetworkRouteRegion networkRouteRegion) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("SELECT pntId FROM routeRegionPoints WHERE id = ? ");
		ps.setLong(1, networkRouteRegion.id);
		ResultSet rs = ps.executeQuery();
		if(networkRouteRegion.visitedVertices != null) {
			throw new IllegalStateException();
		}
		networkRouteRegion.visitedVertices = new TLongObjectHashMap<>();
		while(rs.next()) {
			networkRouteRegion.visitedVertices.put(rs.getLong(1), null);
		}
		networkRouteRegion.points = -1;		
		rs.close();
	}
	
	public void updatePointsCHInd(Collection<NetworkDBPoint> pnts) throws SQLException {
		PreparedStatement updCHInd = conn.prepareStatement("UPDATE  points SET chInd = ? where idPoint = ?");
		int ind = 0;
		for (NetworkDBPoint p : pnts) {
			updCHInd.setLong(1, p.chInd);
			updCHInd.setLong(2, p.pntGeoId);
			updCHInd.addBatch();
			if (ind++ % BATCH_SIZE == 0) {
				updCHInd.executeBatch();
			}
		}
		updCHInd.executeBatch();
		updCHInd.close();
	}
	
	public void insertVisitedVertices(NetworkRouteRegion networkRouteRegion) throws SQLException {
		PreparedStatement ps = conn.prepareStatement("INSERT INTO routeRegionPoints (id, pntId) VALUES (?, ?)");
		int ind = 0;
		for (long k : networkRouteRegion.visitedVertices.keys()) {
			ps.setLong(1, networkRouteRegion.id);
			ps.setLong(2, k);
			ps.addBatch();
			if (ind++ > BATCH_SIZE) {
				ps.executeBatch();
			}
		}
		ps.executeBatch();
		insPoint.executeBatch();
		insCluster.executeBatch();
		
	}
	
	public void deleteShortcuts() throws SQLException {
		Statement st = conn.createStatement();
		st.execute("DELETE from segments where shortcut > 0");
		st.execute("DELETE from geometry where shortcut > 0");
		st.close();
	}
	
	public void insertSegments(List<NetworkDBSegment> segments) throws SQLException {
		if (insSegment == null) {
			insSegment = conn.prepareStatement("INSERT INTO segments(idPoint, idConnPoint, dist, shortcut) VALUES(?, ?, ?, ?)");
		}
		if (insGeometry == null) {
			insGeometry = conn.prepareStatement("INSERT INTO geometry(idPoint, idConnPoint, shortcut, geometry) " + " VALUES(?, ?, ?, ?)");
		}
		int ind= 0;
		for (NetworkDBSegment s : segments) {
			insSegment.setLong(1, s.start.index);
			insSegment.setLong(2, s.end.index);
			insSegment.setDouble(3, s.dist);
			insSegment.setInt(4, s.shortcut ? 1 : 0);
			insSegment.addBatch();
//			byte[] coordinates = new byte[0];
			if (s.geometry.size() > 0) {
				byte[] coordinates = new byte[8 * s.geometry.size()];
				for (int t = 0; t < s.geometry.size(); t++) {
					LatLon l = s.geometry.get(t);
					Algorithms.putIntToBytes(coordinates, 8 * t, MapUtils.get31TileNumberX(l.getLongitude()));
					Algorithms.putIntToBytes(coordinates, 8 * t + 4, MapUtils.get31TileNumberY(l.getLatitude()));
				}
				insGeometry.setBytes(4, coordinates);
			} else if (s.segmentsStartEnd.size() > 0) {
				byte[] coordinates = new byte[4 * s.segmentsStartEnd.size() + 8];
				Algorithms.putIntToBytes(coordinates, 0, XY_SHORTCUT_GEOM);
				Algorithms.putIntToBytes(coordinates, 4, XY_SHORTCUT_GEOM);
				for (int t = 0; t < s.segmentsStartEnd.size(); t++) {
					Algorithms.putIntToBytes(coordinates, 4 * t + 8, s.segmentsStartEnd.getQuick(t));
				}
				insGeometry.setBytes(4, coordinates);
			}
			insGeometry.setLong(1, s.start.index);
			insGeometry.setLong(2, s.end.index);
			insGeometry.setInt(3, s.shortcut ? 1 : 0);
			
			insGeometry.addBatch();
			if (ind++ % BATCH_SIZE == 0) {
				insSegment.executeBatch();
				insGeometry.executeBatch();
			}
		}
		insSegment.executeBatch();
		insGeometry.executeBatch();
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

	
	
	
	public void insertRegions(List<NetworkRouteRegion> regions) throws SQLException {
		PreparedStatement check = conn.prepareStatement("SELECT id from routeRegions where name = ? "); // and filePointer = ?
		PreparedStatement ins = conn
				.prepareStatement("INSERT INTO routeRegions(id, name, filePointer, size, filename, left, right, top, bottom) "
						+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
		int ind = 0;
		for(NetworkRouteRegion nr : regions) {
			// name is enough
			check.setString(1, nr.region.getName());
//			check.setInt(2, nr.region.getFilePointer());
			ResultSet ls = check.executeQuery();
			if (ls.next()) {
				nr.id = ls.getInt(1);
				continue;
			}
			
			int p = 1;
			nr.id = ind++;
			ins.setLong(p++, nr.id);
			ins.setString(p++, nr.region.getName());
			ins.setLong(p++, nr.region.getFilePointer());
			ins.setLong(p++, nr.region.getLength());
			ins.setString(p++, nr.file.getName());
			ins.setDouble(p++, nr.region.getLeftLongitude());
			ins.setDouble(p++, nr.region.getRightLongitude());
			ins.setDouble(p++, nr.region.getTopLatitude());
			ins.setDouble(p++, nr.region.getBottomLatitude());
			ins.addBatch();
		}
		ins.executeBatch();
		ins.close();
	}

	public void insertCluster(int clusterUniqueIndex, TLongObjectHashMap<? extends RouteSegment> toVisitVertices, TLongObjectHashMap<Integer> pointDbInd) throws SQLException {
		TLongObjectIterator<? extends RouteSegment> it = toVisitVertices.iterator();
		while (it.hasNext()) {
			batchInsPoint++;
			it.advance();
			long pntId = it.key();
			RouteSegment obj = it.value();
			int pointInd;
			if (!pointDbInd.contains(pntId)) {
				pointInd = pointDbInd.size();
				pointDbInd.put(pntId, pointInd);
				int p = 1;
				insPoint.setLong(p++, pntId);
				insPoint.setInt(p++, pointInd);
				insPoint.setLong(p++, obj.getRoad().getId());
				insPoint.setLong(p++, obj.getSegmentStart());
				insPoint.setLong(p++, obj.getSegmentEnd());
				insPoint.setInt(p++, obj.getRoad().getPoint31XTile(obj.getSegmentStart()));
				insPoint.setInt(p++, obj.getRoad().getPoint31YTile(obj.getSegmentStart()));
				insPoint.setInt(p++, obj.getRoad().getPoint31XTile(obj.getSegmentEnd()));
				insPoint.setInt(p++, obj.getRoad().getPoint31YTile(obj.getSegmentEnd()));
				insPoint.addBatch();
			} else {
				pointInd = pointDbInd.get(pntId);
			}
			
			int p2 = 1;
			insCluster.setLong(p2++, pntId);
			insCluster.setInt(p2++, pointInd);
			insCluster.setInt(p2++, clusterUniqueIndex);
			insCluster.addBatch();

		}
		if (batchInsPoint > BATCH_SIZE) {
			batchInsPoint = 0;
			insPoint.executeBatch();
			insCluster.executeBatch();
		}

	}

	public void close() throws SQLException {
		conn.close();
	}
	
	
	static class NetworkRouteRegion {
		int id = 0;
		RouteRegion region;
		File file;
		int points = -1; // -1 loaded points
		TLongObjectHashMap<RouteSegment> visitedVertices = new TLongObjectHashMap<>();

		public NetworkRouteRegion(RouteRegion r, File f) {
			region = r;
			this.file = f;

		}

		public int getPoints() {
			return points < 0 ? visitedVertices.size() : points;
		}

		public QuadRect getRect() {
			return new QuadRect(region.getLeftLongitude(), region.getTopLatitude(), region.getRightLongitude(), region.getBottomLatitude());
		}

		public boolean intersects(NetworkRouteRegion nrouteRegion) {
			return QuadRect.intersects(getRect(), nrouteRegion.getRect());
		}

		public void unload() {
			if (this.visitedVertices != null && this.visitedVertices.size() > 1000) {
				this.points = this.visitedVertices.size();
				this.visitedVertices = null;
			}
		}
		
		public TLongObjectHashMap<RouteSegment> getVisitedVertices(HHRoutingPreparationDB networkDB) throws SQLException {
			if (points > 0) {
				networkDB.loadVisitedVertices(this);
			}
			return visitedVertices;
		}
		
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
		
		public void setCostDetailedParentRt(boolean reverse, FinalRouteSegment r) {
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