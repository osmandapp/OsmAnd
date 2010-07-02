package com.osmand;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import android.database.Cursor;

import com.osmand.data.TransportRoute;
import com.osmand.data.TransportStop;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.index.IndexConstants.IndexTransportRoute;
import com.osmand.data.index.IndexConstants.IndexTransportRouteStop;
import com.osmand.data.index.IndexConstants.IndexTransportStop;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class TransportIndexRepository extends BaseLocationIndexRepository<TransportStop> {
	 private static final Log log = LogUtil.getLog(TransportIndexRepository.class);


	public boolean initialize(final IProgress progress, File file) {
		return super.initialize(progress, file, IndexConstants.TRANSPORT_TABLE_VERSION, IndexTransportStop.getTable());
	}
	
	
	private final String[] columns = IndexConstants.generateColumnNames(IndexTransportStop.values());
	public List<TransportStop> searchTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int limit, List<TransportStop> stops){
		long now = System.currentTimeMillis();
		String squery = "? < latitude AND latitude < ? AND ? < longitude AND longitude < ?"; //$NON-NLS-1$
		
		if(limit != -1){
			squery += " ORDER BY RANDOM() LIMIT " +limit; //$NON-NLS-1$
		}
		Cursor query = db.query(IndexTransportStop.getTable(), columns, squery, 
				new String[]{Double.toString(bottomLatitude), 
				Double.toString(topLatitude), Double.toString(leftLongitude), Double.toString(rightLongitude)}, null, null, null);
		if(query.moveToFirst()){
			do {
				TransportStop st = new TransportStop();
				st.setId(query.getLong(IndexTransportStop.ID.ordinal()));
				st.setLocation(query.getDouble(IndexTransportStop.LATITUDE.ordinal()), 
							query.getDouble(IndexTransportStop.LONGITUDE.ordinal()));
				st.setName(query.getString(IndexTransportStop.NAME.ordinal() ));
				st.setEnName(query.getString(IndexTransportStop.NAME_EN.ordinal()));
				stops.add(st);
				if(limit != -1 && stops.size() >= limit){
					break;
				}
			} while(query.moveToNext());
		}
		query.close();
		
		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for %s done in %s ms found %s.",  //$NON-NLS-1$
					topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, stops.size())); //$NON-NLS-1$
		}
		return stops;
	}
	
	
	
	private static String cacheSQLRouteDescriptions = null;
	/**
	 * 
	 * @param stop
	 * @param format {0} - ref, {1} - type, {2} - name, {3} - name_en
	 * @return
	 */
	public List<String> getRouteDescriptionsForStop(TransportStop stop, String format) {
		long now = System.currentTimeMillis();
		List<String> res = new ArrayList<String>();
		MessageFormat f = new MessageFormat(format);

		if (cacheSQLRouteDescriptions == null) {
			StringBuilder sql = new StringBuilder(200);
			sql.append("SELECT DISTINCT ").append(IndexTransportRoute.REF).append(",").append(IndexTransportRoute.TYPE) //$NON-NLS-1$//$NON-NLS-2$
					.append(",").append(IndexTransportRoute.NAME).append(",").append(IndexTransportRoute.NAME_EN); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" FROM ").append(IndexTransportRoute.getTable()).append(" JOIN ").append(IndexTransportRouteStop.getTable()); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" ON ").append(IndexTransportRoute.getTable()).append(".").append(IndexTransportRoute.ID).append(" = "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sql.append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.ROUTE); //$NON-NLS-1$
			sql.append(" WHERE ").append(IndexTransportRouteStop.STOP).append(" = ?"); //$NON-NLS-1$ //$NON-NLS-2$
			cacheSQLRouteDescriptions = sql.toString();
		}
		Cursor query = db.rawQuery(cacheSQLRouteDescriptions, new String[] { stop.getId() + "" }); //$NON-NLS-1$
		if (query.moveToFirst()) {
			do {
				res.add(f.format(new String[] { query.getString(0), query.getString(1), query.getString(2), query.getString(3) }));
			} while (query.moveToNext());
		}
		query.close();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for stop %s done in %s ms found %s.", //$NON-NLS-1$
					stop.getId() + "", System.currentTimeMillis() - now, res.size())); //$NON-NLS-1$
		}
		return res;
	}
	
	
	public void evaluateCachedTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, int limit,  List<TransportStop> toFill){
		cTopLatitude = topLatitude + (topLatitude -bottomLatitude);
		cBottomLatitude = bottomLatitude - (topLatitude -bottomLatitude);
		cLeftLongitude = leftLongitude - (rightLongitude - leftLongitude);
		cRightLongitude = rightLongitude + (rightLongitude - leftLongitude);
		cZoom = zoom;
		// first of all put all entities in temp list in order to not freeze other read threads
		ArrayList<TransportStop> tempList = new ArrayList<TransportStop>();
		searchTransportStops(cTopLatitude, cLeftLongitude, cBottomLatitude, cRightLongitude, limit, tempList);
		synchronized (this) {
			cachedObjects.clear();
			cachedObjects.addAll(tempList);
		}
		
		checkCachedObjects(topLatitude, leftLongitude, bottomLatitude, rightLongitude, cZoom, toFill);
	}
	
	
	private static String cacheSQLRoutes = null;
	public List<RouteInfoLocation> searchTransportRouteStops(double latitude, double longitude, LatLon locationToGo, int zoom) {
		long now = System.currentTimeMillis();
		LatLon loc = new LatLon(latitude, longitude);
		double tileNumberX = MapUtils.getTileNumberX(zoom, longitude);
		double tileNumberY = MapUtils.getTileNumberY(zoom, latitude);
		double topLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY - 0.5);
		double bottomLatitude = MapUtils.getLatitudeFromTile(zoom, tileNumberY + 0.5);
		double leftLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX - 0.5);
		double rightLongitude = MapUtils.getLongitudeFromTile(zoom, tileNumberX + 0.5);
		assert IndexTransportRoute.values().length == 7;
		int shift = IndexTransportRoute.values().length;
		assert IndexTransportStop.values().length == 5;
		int shift2 = IndexTransportStop.values().length;
		if(cacheSQLRoutes == null){
			StringBuilder sql = new StringBuilder(200);
			sql.append("SELECT "); //$NON-NLS-1$
			String[] cols = IndexConstants.generateColumnNames(IndexTransportRoute.values());
			for(int i=0; i<cols.length; i++){
				if(i>0){
					sql.append(", "); //$NON-NLS-1$
				}
				sql.append(IndexTransportRoute.getTable()).append(".").append(cols[i]); //$NON-NLS-1$
			}
			cols = IndexConstants.generateColumnNames(IndexTransportStop.values());
			for(int i=0; i<cols.length; i++){
				sql.append(", ").append(IndexTransportStop.getTable()).append(".").append(cols[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sql.append(", ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.DIRECTION); //$NON-NLS-1$ //$NON-NLS-2$
			
			sql.append(" FROM ").append(IndexTransportStop.getTable()); //$NON-NLS-1$
			// join with stops table
			sql.append(" JOIN ").append(IndexTransportRouteStop.getTable()); //$NON-NLS-1$ 
			sql.append(" ON ").append(IndexTransportStop.getTable()).append(".").append(IndexTransportStop.ID); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" = ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.STOP); //$NON-NLS-1$ //$NON-NLS-2$
			// join with route table
			sql.append(" JOIN ").append(IndexTransportRoute.getTable()); //$NON-NLS-1$ 
			sql.append(" ON ").append(IndexTransportRoute.getTable()).append(".").append(IndexTransportRoute.ID); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" = ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.ROUTE); //$NON-NLS-1$ //$NON-NLS-2$

			sql.append(" WHERE ").append("? < latitude AND latitude < ? AND ? < longitude AND longitude < ?"); //$NON-NLS-1$ //$NON-NLS-2$
			cacheSQLRoutes = sql.toString();
		}
		Cursor query = db.rawQuery(cacheSQLRoutes, 
				new String[] {bottomLatitude + "" , topLatitude + "" , leftLongitude + "" , rightLongitude + "" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		Map<Long, RouteInfoLocation> registeredRoutes = new LinkedHashMap<Long, RouteInfoLocation>();
		if (query.moveToFirst()) {
			do {
				TransportRoute route = new TransportRoute();
				route.setId(query.getLong(IndexTransportRoute.ID.ordinal()));
				route.setDistance(query.getInt(IndexTransportRoute.DIST.ordinal()));
				route.setName(query.getString(IndexTransportRoute.NAME.ordinal()));
				route.setEnName(query.getString(IndexTransportRoute.NAME_EN.ordinal()));
				route.setRef(query.getString(IndexTransportRoute.REF.ordinal()));
				route.setOperator(query.getString(IndexTransportRoute.OPERATOR.ordinal()));
				route.setType(query.getString(IndexTransportRoute.TYPE.ordinal()));
				TransportStop s = new TransportStop();
				s.setId(query.getLong(shift + IndexTransportStop.ID.ordinal()));
				s.setName(query.getString(shift + IndexTransportStop.NAME.ordinal()));
				s.setEnName(query.getString(shift + IndexTransportStop.NAME_EN.ordinal()));
				s.setLocation(query.getDouble(shift + IndexTransportStop.LATITUDE.ordinal()), 
						query.getDouble(shift + IndexTransportStop.LONGITUDE.ordinal()));
				boolean direction = query.getInt(shift2 + shift) > 0; 
				long idToPut = route.getId() << 1 + (direction ? 1 : 0);
				if(registeredRoutes.containsKey(idToPut)){
					TransportStop st = registeredRoutes.get(idToPut).getStart();
					if(MapUtils.getDistance(loc, st.getLocation()) < MapUtils.getDistance(loc, s.getLocation())){
						continue;
					}
				}
				RouteInfoLocation r = new RouteInfoLocation();
				r.setRoute(route);
				r.setStart(s);
				r.setDirection(direction);
				registeredRoutes.put(idToPut, r);
				
				
			} while (query.moveToNext());
		}
		query.close();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Search for routes done in %s ms found %s.", //$NON-NLS-1$
					System.currentTimeMillis() - now, registeredRoutes.size())); 
		}
		
		List<RouteInfoLocation> list = preloadRouteStopsAndCalculateDistance(loc, locationToGo, registeredRoutes);
		return list;
		
	}

	protected List<RouteInfoLocation> preloadRouteStopsAndCalculateDistance(final LatLon loc, LatLon locationToGo,
			Map<Long, RouteInfoLocation> registeredRoutes) {
		if(registeredRoutes.isEmpty()){
			return Collections.emptyList();
		}
		long now = System.currentTimeMillis();
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT "); //$NON-NLS-1$
		String[] cols = IndexConstants.generateColumnNames(IndexTransportStop.values());
		for (int i = 0; i < cols.length; i++) {
			if (i > 0) {
				sql.append(", "); //$NON-NLS-1$
			}
			sql.append(IndexTransportStop.getTable()).append(".").append(cols[i]); //$NON-NLS-1$
		}
		sql.append(", ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.ROUTE); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(", ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.DIRECTION); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" FROM ").append(IndexTransportStop.getTable()); //$NON-NLS-1$
		// join with stops table
		sql.append(" JOIN ").append(IndexTransportRouteStop.getTable()); //$NON-NLS-1$ 
		sql.append(" ON ").append(IndexTransportStop.getTable()).append(".").append(IndexTransportStop.ID); //$NON-NLS-1$ //$NON-NLS-2$
		sql.append(" = ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.STOP); //$NON-NLS-1$ //$NON-NLS-2$

		sql.append(" WHERE "); //$NON-NLS-1$
		boolean f = true;
		for (RouteInfoLocation il : registeredRoutes.values()) {
			if (f) {
				f = false;
			} else {
				sql.append(" OR "); //$NON-NLS-1$
			}
			sql.append("("); //$NON-NLS-1$
			sql.append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.ROUTE); //$NON-NLS-1$
			sql.append(" = ").append(il.getRoute().getId()); //$NON-NLS-1$
			sql.append(" AND ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.DIRECTION); //$NON-NLS-1$ //$NON-NLS-2$
			sql.append(" = ").append(il.getDirection() ? 1 : 0); //$NON-NLS-1$
			sql.append(")"); //$NON-NLS-1$
		}
		sql.append(" ORDER BY ").append(IndexTransportRouteStop.getTable()).append(".").append(IndexTransportRouteStop.ORD).append(" ASC"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		int qShift = IndexTransportStop.values().length;

		Map<Long, Integer> distanceToLoc = new LinkedHashMap<Long, Integer>();

		Cursor query = db.rawQuery(sql.toString(), new String[] {}); 
		if (query.moveToFirst()) {
			// load only part of the route
			do {
				TransportStop st = null;

				long routeId = query.getLong(qShift);
				int direction = query.getInt(qShift + 1);
				long id = routeId << 1 + direction;
				boolean found = distanceToLoc.containsKey(id);
				RouteInfoLocation i = registeredRoutes.get(id);
				if (found) {
					st = new TransportStop();
					st.setId(query.getLong(IndexTransportStop.ID.ordinal()));
					st.setLocation(query.getDouble(IndexTransportStop.LATITUDE.ordinal()), query.getDouble(IndexTransportStop.LONGITUDE
							.ordinal()));
					st.setName(query.getString(IndexTransportStop.NAME.ordinal()));
					st.setEnName(query.getString(IndexTransportStop.NAME_EN.ordinal()));
				} else if (query.getLong(IndexTransportStop.ID.ordinal()) == i.getStart().getId()) {
					st = i.getStart();
					found = true;
					Integer dist = null;
					if (locationToGo != null) {
						dist = (int) MapUtils.getDistance(locationToGo, i.getStart().getLocation());
					}
					distanceToLoc.put(id, dist);
				}

				if (found) {
					if (locationToGo != null) {
						double d = MapUtils.getDistance(locationToGo, st.getLocation());
						if (d < distanceToLoc.get(id)) {
							distanceToLoc.put(id, (int) d);
						}
					}
					if (i.direction) {
						i.getRoute().getForwardStops().add(st);
					} else {
						i.getRoute().getBackwardStops().add(st);
					}
				}

			} while (query.moveToNext());
			query.close();

		}
		
		if (locationToGo != null) {
			for (Long l : registeredRoutes.keySet()) {
				Integer dist = distanceToLoc.get(l);
				if (dist != null) {
					registeredRoutes.get(l).setDistToLocation(dist);
				}
			}
		}

		ArrayList<RouteInfoLocation> listRoutes = new ArrayList<RouteInfoLocation>(registeredRoutes.values());
		if (log.isDebugEnabled()) {
			log.debug(String.format("Loading routes done in %s ms for %s routes.", //$NON-NLS-1$
					System.currentTimeMillis() - now, listRoutes.size()));
		}

		if (locationToGo != null) {
			Collections.sort(listRoutes, new Comparator<RouteInfoLocation>() {
				@Override
				public int compare(RouteInfoLocation object1, RouteInfoLocation object2) {
					return object1.getDistToLocation() - object2.getDistToLocation();
				}

			});
		} else {
			Collections.sort(listRoutes, new Comparator<RouteInfoLocation>() {
				@Override
				public int compare(RouteInfoLocation object1, RouteInfoLocation object2) {
					return Double.compare(MapUtils.getDistance(loc, object1.getStart().getLocation()), MapUtils.getDistance(loc, object2
							.getStart().getLocation()));
				}

			});
		}
		return listRoutes;
	}
	
	public static class RouteInfoLocation {
		private TransportStop start;
		private TransportRoute route;
		private int distToLocation;
		private boolean direction;
		
		public TransportStop getStart() {
			return start;
		}
		
		public TransportRoute getRoute() {
			return route;
		}
		
		public boolean getDirection(){
			return direction;
		}
		
		public void setDirection(boolean direction) {
			this.direction = direction;
		}
		
		public int getDistToLocation() {
			return distToLocation;
		}
		
		public void setStart(TransportStop start) {
			this.start = start;
		}
		
		public void setRoute(TransportRoute route) {
			this.route = route;
		}
		
		public void setDistToLocation(int distToLocation) {
			this.distToLocation = distToLocation;
		}
	}
}
