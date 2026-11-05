package net.osmand.plus.resources;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadResources;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class OsmandRegionSearcher {

	private static final Log LOG = PlatformUtil.getLog(OsmandRegionSearcher.class);

	private final int zoom;
	private final boolean routeData;
	private final ResourceManager resourceManager;
	private final OsmandRegions osmandRegions;
	private final DownloadResources downloadResources;

	private final int point31x;
	private final int point31y;

	private BinaryMapDataObject binaryMapDataObject;
	private WorldRegion worldRegion;
	private String regionFullName;

	public OsmandRegionSearcher(@NonNull OsmandApplication app, @NonNull LatLon latLon) {
		this(app, latLon, 15, false);
	}

	public OsmandRegionSearcher(@NonNull OsmandApplication app, @NonNull LatLon latLon, int zoom) {
		this(app, latLon, zoom, false);
	}

	public OsmandRegionSearcher(@NonNull OsmandApplication app, @NonNull LatLon latLon, int zoom, boolean routeData) {
		this.zoom = zoom;
		this.routeData = routeData;
		this.resourceManager = app.getResourceManager();
		this.osmandRegions = resourceManager.getOsmandRegions();
		this.downloadResources = app.getDownloadThread().getIndexes();
		this.point31x = MapUtils.get31TileNumberX(latLon.getLongitude());
		this.point31y = MapUtils.get31TileNumberY(latLon.getLatitude());
	}

	public OsmandRegionSearcher(@NonNull OsmandApplication app, int point31x, int point31y, int zoom, boolean routeData) {
		this.zoom = zoom;
		this.routeData = routeData;
		this.resourceManager = app.getResourceManager();
		this.osmandRegions = resourceManager.getOsmandRegions();
		this.downloadResources = app.getDownloadThread().getIndexes();
		this.point31x = point31x;
		this.point31y = point31y;
	}

	public BinaryMapDataObject getBinaryMapDataObject() {
		return binaryMapDataObject;
	}

	public WorldRegion getWorldRegion() {
		return worldRegion;
	}

	public String getRegionFullName() {
		return regionFullName;
	}

	public void search() {
		if (!downloadResources.getExternalMapFileNamesAt(point31x, point31y, routeData).isEmpty()) {
			return;
		}
		List<BinaryMapDataObject> mapDataObjects;
		try {
			mapDataObjects = osmandRegions.query(point31x, point31x, point31y, point31y);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			return;
		}
		if (mapDataObjects != null) {
			Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (o.getTypes() != null) {
					boolean isRegion = true;
					for (int i = 0; i < o.getTypes().length; i++) {
						BinaryMapIndexReader.TagValuePair tp = o.getMapIndex().decodeType(o.getTypes()[i]);
						if ("boundary".equals(tp.value)) {
							isRegion = false;
							break;
						}
					}
					if (!isRegion || !osmandRegions.contain(o, point31x, point31y)) {
						it.remove();
					}
				}
			}
			double smallestArea = -1;
			for (BinaryMapDataObject o : mapDataObjects) {
				String downloadName = osmandRegions.getDownloadName(o);
				if (!Algorithms.isEmpty(downloadName)) {
					boolean downloaded = checkIfObjectDownloaded(resourceManager, downloadName);
					if (downloaded) {
						regionFullName = null;
						binaryMapDataObject = null;
						worldRegion = null;
						break;
					} else {
						String fullName = osmandRegions.getFullName(o);
						WorldRegion region = osmandRegions.getRegionData(fullName);
						if (region != null && region.isRegionMapDownload()) {
							double area = OsmandRegions.getArea(o);
							if (smallestArea == -1) {
								smallestArea = area;
								regionFullName = fullName;
								binaryMapDataObject = o;
								worldRegion = region;
							} else if (area < smallestArea) {
								smallestArea = area;
								regionFullName = fullName;
								binaryMapDataObject = o;
								worldRegion = region;
							}
						}
					}
				}
			}
		}
	}

	private boolean checkIfObjectDownloaded(ResourceManager rm, String downloadName) {
		boolean downloaded = rm.checkIfObjectDownloaded(downloadName);
		if (!downloaded) {
			WorldRegion region = rm.getOsmandRegions().getRegionDataByDownloadName(downloadName);
			if (region != null && region.getSuperregion() != null && region.getSuperregion().isRegionMapDownload()) {
				return checkIfObjectDownloaded(rm, region.getSuperregion().getRegionDownloadName());
			}
		}
		return downloaded;
	}
}
