package net.osmand.router;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.data.LatLon;
import net.osmand.router.HHRouteDataStructure.HHRouteRegionPointsCtx;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.HHRouteDataStructure.NetworkDBPointCh;
import net.osmand.router.HHRouteDataStructure.NetworkDBSegment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

public class HHRoutingDB {

	public static final String EXT = ".hhdb";
	public static final String CEXT = ".chdb";

	protected static final int XY_SHORTCUT_GEOM = 0;

	protected final Connection conn;
	protected final File file;
	protected PreparedStatement loadGeometry;
	protected PreparedStatement loadSegmentEnd;
	protected PreparedStatement loadSegmentStart;

	protected final int BATCH_SIZE = 10000;
	protected int batchInsPoint = 0;

	protected String routingProfile = "";
	protected TIntObjectHashMap<String> routingProfiles = new TIntObjectHashMap<String>();
	protected boolean compactDB;
	
	protected static Comparator<NetworkDBPoint> indexComparator = new Comparator<NetworkDBPoint>() {

		@Override
		public int compare(NetworkDBPoint o1, NetworkDBPoint o2) {
			return Integer.compare(o1.index, o2.index);
		}
	};
	
	public HHRoutingDB(File f, Connection conn) throws SQLException {
		this.conn = conn;
		this.file = f;
		Statement st = conn.createStatement();
		compactDB = checkColumnExist(st, "ins", "segments");
		st.execute("CREATE TABLE IF NOT EXISTS profiles(profile, id, params)");
		if (!compactDB) {
			st.execute("CREATE TABLE IF NOT EXISTS points(idPoint, pointGeoUniDir, pointGeoId, clusterId, fileDbId, dualIdPoint, dualClusterId, "
					+ "chInd, roadId, start, end, sx31, sy31, ex31, ey31, tagValues, PRIMARY KEY(idPoint))");
			st.execute("CREATE UNIQUE INDEX IF NOT EXISTS pointsUnique on points(pointGeoId)");
			st.execute("CREATE TABLE IF NOT EXISTS segments(idPoint, idConnPoint, dist, shortcut, profile)");
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
			loadSegmentStart = conn.prepareStatement("SELECT id, ins, outs from segments where id = ? and profile = ? ");
		}
		ResultSet rs = st.executeQuery("SELECT profile, id, params from profiles");
		while (rs.next()) {
			routingProfile = rs.getString(1);
			routingProfiles.put(rs.getInt(2), rs.getString(3));
		}
		st.close();
	}
	
	public String getRoutingProfile() {
		return routingProfile;
	}
	
	public File getFile() {
		return file;
	}
	
	public TIntObjectHashMap<String> getRoutingProfiles() {
		return routingProfiles;
	}
	
	public int insertRoutingProfile(String routingProfile, String profileParams) throws SQLException {
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
		s.execute(String.format("INSERT INTO profiles(profile, id, params) VALUES('%s', %d, '%s')",
				routingProfile, id, profileParams));
		return id;
	}
	

	private boolean checkColumnExist(Statement st, String col, String table) throws SQLException {
		try {
			return st.execute("SELECT "+ col+ " FROM " + table + " limit 1 ");
		} catch (SQLException e) {
			return false;
		}
	}
	
	public <T extends NetworkDBPoint> TLongObjectHashMap<T> loadNetworkPoints(short mapId, Class<T> cl) throws SQLException {
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT dualIdPoint, idPoint, clusterId, chInd, roadId, start, end, sx31, sy31, ex31, ey31, tagValues from points");
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
			pnt.mapId = mapId;
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
			// to implement
//			String string = rs.getString(p++);
//			pnt.tagValues = string;
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
	
	
	
	public boolean loadGeometry(NetworkDBSegment segment, int profile, boolean reload) throws SQLException {
		List<LatLon> geometry = segment.getGeometry();
		geometry.clear();
		geometry.addAll(parseGeometry(segment.start.index, segment.end.index, profile, segment.shortcut));
		return !geometry.isEmpty();
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
	
	
	public <T extends NetworkDBPoint> int loadNetworkSegmentPoint(HHRoutingContext<T>  ctx, HHRouteRegionPointsCtx<T> reg, T point, boolean reverse) throws SQLException {
		if (point.connected(reverse) != null) {
			return 0;
		}
		int loadedSegs = 0;
		if (compactDB) {
			loadSegmentStart.setInt(1, point.index);
			loadSegmentStart.setInt(2, reg.routingProfile);
			ResultSet rs = loadSegmentStart.executeQuery();
			if (rs.next()) {
				HHRouteDataStructure.setSegments(ctx, point, rs.getBytes(2), rs.getBytes(3));
				return point.connected(true).size() + point.connected(false).size();
			} else {
				point.connectedSet(true, new ArrayList<NetworkDBSegment>());
				point.connectedSet(false, new ArrayList<NetworkDBSegment>());
			}
			return 0;
		} else {
			List<NetworkDBSegment> l = new ArrayList<>();
			@SuppressWarnings("resource")
			PreparedStatement pre = reverse ? loadSegmentStart : loadSegmentEnd;
			pre.setInt(1, point.index);
			pre.setInt(2, reg.routingProfile);
			ResultSet rs = pre.executeQuery();
			while (rs.next()) {
				loadedSegs++;
				NetworkDBPoint start = ctx.pointsById.get(rs.getLong(1));
				NetworkDBPoint end = ctx.pointsById.get(rs.getLong(2));
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
		if (l.isEmpty()) {
			System.err.printf("Empty route geometry %d -> %d  %s\n", start, end, shortcut ? "sh" : "bs");
		}
		return l;
	}

	

	public void close() throws SQLException {
		conn.close();
	}
	
	

}