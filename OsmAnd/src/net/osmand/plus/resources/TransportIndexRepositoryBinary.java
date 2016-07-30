package net.osmand.plus.resources;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class TransportIndexRepositoryBinary implements TransportIndexRepository {
	private static final Log log = PlatformUtil.getLog(TransportIndexRepositoryBinary.class);
	private final BinaryMapIndexReader file;
	

	public TransportIndexRepositoryBinary(BinaryMapIndexReader file) {
		this.file = file;
	}

	@Override
	public boolean checkContains(double latitude, double longitude) {
		return file.containTransportData(latitude, longitude);
	}
	@Override
	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		return file.containTransportData(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
	}
	
	@Override
	public void searchTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
			int limit, List<TransportStop> stops, ResultMatcher<TransportStop> matcher) {
		long now = System.currentTimeMillis();
		try {
			file.searchTransportIndex(BinaryMapIndexReader.buildSearchTransportRequest(MapUtils.get31TileNumberX(leftLongitude),
					MapUtils.get31TileNumberX(rightLongitude), MapUtils.get31TileNumberY(topLatitude), 
					MapUtils.get31TileNumberY(bottomLatitude), limit, stops));
			if (log.isDebugEnabled()) {
				log.debug(String.format("Search for %s done in %s ms found %s.", //$NON-NLS-1$
						topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, stops.size())); //$NON-NLS-1$
			}
		} catch (IOException e) {
			log.error("Disk error ", e); //$NON-NLS-1$
		}
	}

	@Override
	public Collection<TransportRoute> getRouteForStop(TransportStop stop){
		try {
			Collection<TransportRoute> res = file.getTransportRoutes(stop.getReferencesToRoutes()).valueCollection();
			if(res != null){
				return res;
			}
		} catch (IOException e) {
			log.error("Disk error ", e); //$NON-NLS-1$
		}
		return Collections.emptyList();
	}
	
	@Override
	public boolean acceptTransportStop(TransportStop stop) {
		return file.transportStopBelongsTo(stop);
	}

	@Override
	public void close() {
		try {
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
