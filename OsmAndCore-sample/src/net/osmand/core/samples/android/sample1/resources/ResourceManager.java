package net.osmand.core.samples.android.sample1.resources;


import net.osmand.IndexConstants;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.CachedOsmandIndexes;
import net.osmand.core.samples.android.sample1.SampleApplication;
import net.osmand.core.samples.android.sample1.SampleUtils;
import net.osmand.data.Amenity;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {

	private final SampleApplication app;
	private final Map<String, AmenityIndexRepository> amenityRepositories =  new ConcurrentHashMap<>();

	public ResourceManager(SampleApplication app) {
		this.app = app;
		setRepositories();
	}

	private void setRepositories() {
		ArrayList<File> files = new ArrayList<>();
		File appPath = app.getAppPath(null);
		SampleUtils.collectFiles(appPath, IndexConstants.BINARY_MAP_INDEX_EXT, files);
		SampleUtils.collectFiles(app.getAppPath(IndexConstants.WIKI_INDEX_DIR), IndexConstants.BINARY_MAP_INDEX_EXT, files);

		CachedOsmandIndexes cachedOsmandIndexes = new CachedOsmandIndexes();
		File indCache = app.getAppPath("ind_core.cache");
		if (indCache.exists()) {
			try {
				cachedOsmandIndexes.readFromFile(indCache, CachedOsmandIndexes.VERSION);
			} catch (Exception e) {
			}
		}

		for (File f : files) {
			try {
				BinaryMapIndexReader reader = cachedOsmandIndexes.getReader(f);
				if (reader.containsPoiData()) {
					amenityRepositories.put(f.getName(), new AmenityIndexRepositoryBinary(reader));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<Amenity> searchAmenities(BinaryMapIndexReader.SearchPoiTypeFilter filter,
										 double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude, int zoom, final ResultMatcher<Amenity> matcher) {
		final List<Amenity> amenities = new ArrayList<>();
		try {
			if (!filter.isEmpty()) {
				int top31 = MapUtils.get31TileNumberY(topLatitude);
				int left31 = MapUtils.get31TileNumberX(leftLongitude);
				int bottom31 = MapUtils.get31TileNumberY(bottomLatitude);
				int right31 = MapUtils.get31TileNumberX(rightLongitude);
				for (AmenityIndexRepository index : amenityRepositories.values()) {
					if (matcher != null && matcher.isCancelled()) {
						break;
					}
					if (index.checkContainsInt(top31, left31, bottom31, right31)) {
						List<Amenity> r = index.searchAmenities(top31,
								left31, bottom31, right31, zoom, filter, matcher);
						if(r != null) {
							amenities.addAll(r);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return amenities;
	}
}
