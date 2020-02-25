package net.osmand.plus.resources;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResource;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResourceType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class TransportIndexRepositoryBinary implements TransportIndexRepository {
	private static final Log log = PlatformUtil.getLog(TransportIndexRepositoryBinary.class);
	private BinaryMapReaderResource resource;

	public TransportIndexRepositoryBinary(BinaryMapReaderResource resource) {
		this.resource = resource;
	}

	@Nullable
	public BinaryMapIndexReader getOpenFile() {
		return resource.getReader(BinaryMapReaderResourceType.TRANSPORT);
	}

	@Override
	public boolean checkContains(double latitude, double longitude) {
		BinaryMapIndexReader shallowReader = resource.getShallowReader();
		return shallowReader != null && shallowReader.containTransportData(latitude, longitude);
	}
	@Override
	public boolean checkContains(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		BinaryMapIndexReader shallowReader = resource.getShallowReader();
		return shallowReader != null && shallowReader.containTransportData(topLatitude, leftLongitude, bottomLatitude, rightLongitude);
	}
	
	@Override
	public synchronized void searchTransportStops(double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude,
			int limit, List<TransportStop> stops, ResultMatcher<TransportStop> matcher) {
		long now = System.currentTimeMillis();
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				reader.searchTransportIndex(BinaryMapIndexReader.buildSearchTransportRequest(MapUtils.get31TileNumberX(leftLongitude),
						MapUtils.get31TileNumberX(rightLongitude), MapUtils.get31TileNumberY(topLatitude),
						MapUtils.get31TileNumberY(bottomLatitude), limit, stops));
				if (log.isDebugEnabled()) {
					log.debug(String.format("Search for %s done in %s ms found %s.", //$NON-NLS-1$
							topLatitude + " " + leftLongitude, System.currentTimeMillis() - now, stops.size())); //$NON-NLS-1$
				}
			}
		} catch (IOException e) {
			log.error("Disk error ", e); //$NON-NLS-1$
		}
	}

	@Override
	public synchronized List<TransportRoute> getRoutesForStop(TransportStop stop) {
		return getRoutesForReferences(stop.getReferencesToRoutes());
	}

	@Override
	public List<TransportRoute> getRoutesForReferences(int[] referencesToRoutes) {
		try {
			BinaryMapIndexReader reader = getOpenFile();
			if (reader != null) {
				Collection<TransportRoute> res = reader.getTransportRoutes(referencesToRoutes).valueCollection();
				if (res != null) {
					List<TransportRoute> lst = new ArrayList<>(res);
					Collections.sort(lst, new Comparator<TransportRoute>() {
						@Override
						public int compare(TransportRoute o1, TransportRoute o2) {
							int i1 = Algorithms.extractFirstIntegerNumber(o1.getRef());
							int i2 = Algorithms.extractFirstIntegerNumber(o2.getRef());
							int r = Algorithms.compare(i1, i2);
							if (r == 0) {
								r = Algorithms.compare(o1.getName(), o2.getName());
							}
							return r;
						}
					});
					return lst;
				}
			}
		} catch (IOException e) {
			log.error("Disk error ", e); //$NON-NLS-1$
		}
		return Collections.emptyList();
	}

	@Override
	public boolean acceptTransportStop(TransportStop stop) {
		BinaryMapIndexReader shallowReader = resource.getShallowReader();
		return shallowReader != null && shallowReader.transportStopBelongsTo(stop);
	}

	@Override
	public boolean isUseForPublicTransport() {
		return resource.isUseForPublicTransport();
	}
}
