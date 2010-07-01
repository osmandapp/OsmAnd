package com.osmand;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import android.database.Cursor;

import com.osmand.data.TransportStop;
import com.osmand.data.index.IndexConstants;
import com.osmand.data.index.IndexConstants.IndexTransportRoute;
import com.osmand.data.index.IndexConstants.IndexTransportRouteStop;
import com.osmand.data.index.IndexConstants.IndexTransportStop;

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
			sql.append("SELECT ").append(IndexTransportRoute.REF).append(",").append(IndexTransportRoute.TYPE) //$NON-NLS-1$//$NON-NLS-2$
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
}
