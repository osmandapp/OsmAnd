package net.osmand.binary;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRoutePointsBox;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CitiesBlock;
import net.osmand.binary.BinaryMapIndexReader.MapIndex;
import net.osmand.binary.BinaryMapIndexReader.MapRoot;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteSubregion;
import net.osmand.binary.BinaryMapTransportReaderAdapter.IndexStringTable;
import net.osmand.binary.BinaryMapTransportReaderAdapter.TransportIndex;
import net.osmand.binary.OsmandIndex.AddressPart;
import net.osmand.binary.OsmandIndex.CityBlock;
import net.osmand.binary.OsmandIndex.FileIndex;
import net.osmand.binary.OsmandIndex.HHRoutingPart;
import net.osmand.binary.OsmandIndex.MapLevel;
import net.osmand.binary.OsmandIndex.MapPart;
import net.osmand.binary.OsmandIndex.OsmAndStoredIndex;
import net.osmand.binary.OsmandIndex.PoiPart;
import net.osmand.binary.OsmandIndex.RoutingPart;
import net.osmand.binary.OsmandIndex.RoutingSubregion;
import net.osmand.binary.OsmandIndex.TransportPart;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class CachedOsmandIndexes {

	private OsmAndStoredIndex storedIndex;
	private OsmAndStoredIndex.Builder storedIndexBuilder;
	private Log log = PlatformUtil.getLog(CachedOsmandIndexes.class);
	private boolean hasChanged = false;
	public static final String INDEXES_DEFAULT_FILENAME = "indexes.cache";

	public static final int VERSION = 5;// synchronize with binaryRead.cpp CACHE_VERSION

	public FileIndex addToCache(BinaryMapIndexReader reader, File f) {
		hasChanged = true;
		if (storedIndexBuilder == null) {
			storedIndexBuilder = OsmandIndex.OsmAndStoredIndex.newBuilder();
			storedIndexBuilder.setVersion(VERSION);
			storedIndexBuilder.setDateCreated(System.currentTimeMillis());
			if (storedIndex != null) {
				for (FileIndex ex : storedIndex.getFileIndexList()) {
					if (!ex.getFileName().equals(f.getName())) {
						storedIndexBuilder.addFileIndex(ex);
					}
				}
			}
		} else {
			int found = -1;
			List<FileIndex> fileIndexList = storedIndexBuilder.getFileIndexList();
			for (int i = 0; i < fileIndexList.size(); i++) {
				if (fileIndexList.get(i).getFileName().equals(f.getName())) {
					found = i;
					break;
				}
			}
			if (found >= 0) {
				storedIndexBuilder.removeFileIndex(found);
			}
		}

		FileIndex.Builder fileIndex = OsmandIndex.FileIndex.newBuilder();
		long d = reader.getDateCreated();
		fileIndex.setDateModified(d == 0 ? f.lastModified() : d);
		fileIndex.setSize(f.length());
		fileIndex.setVersion(reader.getVersion());
		fileIndex.setFileName(f.getName());
		for (MapIndex index : reader.getMapIndexes()) {
			MapPart.Builder map = OsmandIndex.MapPart.newBuilder();
			map.setSize(index.getLength());
			map.setOffset(index.getFilePointer());
			if (index.getName() != null) {
				map.setName(index.getName());
			}
			for (MapRoot mr : index.getRoots()) {
				MapLevel.Builder lev = OsmandIndex.MapLevel.newBuilder();
				lev.setSize(mr.length);
				lev.setOffset(mr.filePointer);
				lev.setLeft(mr.left);
				lev.setRight(mr.right);
				lev.setTop(mr.top);
				lev.setBottom(mr.bottom);
				lev.setMinzoom(mr.minZoom);
				lev.setMaxzoom(mr.maxZoom);
				map.addLevels(lev);
			}
			fileIndex.addMapIndex(map);
		}

		for (AddressRegion index : reader.getAddressIndexes()) {
			AddressPart.Builder addr = OsmandIndex.AddressPart.newBuilder();
			addr.setSize(index.getLength());
			addr.setOffset(index.getFilePointer());
			if (index.getName() != null) {
				addr.setName(index.getName());
			}
			if (index.getEnName() != null) {
				addr.setNameEn(index.getEnName());
			}
			addr.setIndexNameOffset(index.getIndexNameOffset());
			for (CitiesBlock mr : index.getCities()) {
				CityBlock.Builder cblock = OsmandIndex.CityBlock.newBuilder();
				cblock.setSize(mr.length);
				cblock.setOffset(mr.filePointer);
				cblock.setType(mr.type);
				addr.addCities(cblock);
			}
			for (String s : index.getAttributeTagsTable()) {
				addr.addAdditionalTags(s);
			}
			fileIndex.addAddressIndex(addr);
		}

		for (PoiRegion index : reader.getPoiIndexes()) {
			PoiPart.Builder poi = OsmandIndex.PoiPart.newBuilder();
			poi.setSize(index.getLength());
			poi.setOffset(index.getFilePointer());
			if (index.getName() != null) {
				poi.setName(index.getName());
			}
			poi.setLeft(index.left31);
			poi.setRight(index.right31);
			poi.setTop(index.top31);
			poi.setBottom(index.bottom31);
			fileIndex.addPoiIndex(poi.build());
		}

		for (TransportIndex index : reader.getTransportIndexes()) {
			TransportPart.Builder transport = OsmandIndex.TransportPart.newBuilder();
			transport.setSize(index.getLength());
			transport.setOffset(index.getFilePointer());
			if (index.getName() != null) {
				transport.setName(index.getName());
			}
			transport.setLeft(index.getLeft());
			transport.setRight(index.getRight());
			transport.setTop(index.getTop());
			transport.setBottom(index.getBottom());
			transport.setStopsTableLength(index.stopsFileLength);
			transport.setStopsTableOffset(index.stopsFileOffset);
			// if(index.incompleteRoutesLength > 0) {
			transport.setIncompleteRoutesLength(index.incompleteRoutesLength);
			transport.setIncompleteRoutesOffset(index.incompleteRoutesOffset);
			// }
			transport.setStringTableLength(index.stringTable.length);
			transport.setStringTableOffset(index.stringTable.fileOffset);
			fileIndex.addTransportIndex(transport);
		}

		for (RouteRegion index : reader.getRoutingIndexes()) {
			RoutingPart.Builder routing = OsmandIndex.RoutingPart.newBuilder();
			routing.setSize(index.getLength());
			routing.setOffset(index.getFilePointer());
			if (index.getName() != null) {
				routing.setName(index.getName());
			}
			for (RouteSubregion sub : index.getSubregions()) {
				addRouteSubregion(routing, sub, false);
			}
			for (RouteSubregion sub : index.getBaseSubregions()) {
				addRouteSubregion(routing, sub, true);
			}
			fileIndex.addRoutingIndex(routing);
		}
		
		for (HHRouteRegion index : reader.getHHRoutingIndexes()) {
			HHRoutingPart.Builder routing = OsmandIndex.HHRoutingPart.newBuilder();
			routing.setSize(index.getLength());
			routing.setOffset(index.getFilePointer());
			routing.setEdition(index.edition);
			routing.addAllProfileParams(index.profileParams);
			routing.setProfile(index.profile);
			routing.setPointsLength(index.top.length);
			routing.setPointsOffset(index.top.filePointer);
			routing.setBottom(index.top.bottom);
			routing.setTop(index.top.top);
			routing.setLeft(index.top.left);
			routing.setRight(index.top.right);
			fileIndex.addHhRoutingIndex(routing);
		}

		FileIndex fi = fileIndex.build();
		storedIndexBuilder.addFileIndex(fi);
		return fi;
	}

	private void addRouteSubregion(RoutingPart.Builder routing, RouteSubregion sub, boolean base) {
		OsmandIndex.RoutingSubregion.Builder rpart = OsmandIndex.RoutingSubregion.newBuilder();
		rpart.setSize(sub.length);
		rpart.setOffset(sub.filePointer);
		rpart.setLeft(sub.left);
		rpart.setRight(sub.right);
		rpart.setTop(sub.top);
		rpart.setBasemap(base);
		rpart.setBottom(sub.bottom);
		rpart.setShifToData(sub.shiftToData);
		routing.addSubregions(rpart);
	}

	public BinaryMapIndexReader getReader(File f, boolean useStoredIndex) throws IOException {
		FileIndex found = useStoredIndex ? getFileIndex(f, false) : null;
		BinaryMapIndexReader reader = null;
		RandomAccessFile mf = new RandomAccessFile(f.getPath(), "r");
		if (found == null) {
			long val = System.currentTimeMillis();
			reader = new BinaryMapIndexReader(mf, f);
			found = addToCache(reader, f);
			if (log.isDebugEnabled()) {
				log.debug("Initializing db " + f.getAbsolutePath() + " " + (System.currentTimeMillis() - val) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} else {
			reader = initReaderFromFileIndex(found, mf, f);
		}
		return reader;
	}


	public FileIndex getFileIndex(File f, boolean init) throws IOException {
		FileIndex found = null;
		if (storedIndex != null) {
			for (int i = 0; i < storedIndex.getFileIndexCount(); i++) {
				FileIndex fi = storedIndex.getFileIndex(i);
				if (f.length() == fi.getSize() && f.getName().equals(fi.getFileName())) {
					// f.lastModified() == fi.getDateModified()
					found = fi;
					break;
				}
			}
		}
		if (found == null && init) {
			RandomAccessFile mf = new RandomAccessFile(f.getPath(), "r");
			long val = System.currentTimeMillis();
			BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, f);
			found = addToCache(reader, f);
			if (log.isDebugEnabled()) {
				log.debug("Initializing db " + f.getAbsolutePath() + " " + (System.currentTimeMillis() - val) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			reader.close();
			mf.close();
		}
 		return found;
	}

	public BinaryMapIndexReader initReaderFromFileIndex(FileIndex found, RandomAccessFile mf, File f) throws IOException {
		BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, f, false);
		reader.version = found.getVersion();
		reader.dateCreated = found.getDateModified();

		for (MapPart index : found.getMapIndexList()) {
			MapIndex mi = new MapIndex();
			mi.length = index.getSize();
			mi.filePointer = index.getOffset();
			mi.name = index.getName();

			for (MapLevel mr : index.getLevelsList()) {
				MapRoot root = new MapRoot();
				root.length = mr.getSize();
				root.filePointer = mr.getOffset();
				root.left = mr.getLeft();
				root.right = mr.getRight();
				root.top = mr.getTop();
				root.bottom = mr.getBottom();
				root.minZoom = mr.getMinzoom();
				root.maxZoom = mr.getMaxzoom();
				mi.roots.add(root);
			}
			reader.mapIndexes.add(mi);
			reader.indexes.add(mi);
			reader.basemap = reader.basemap || mi.isBaseMap();
		}

		for (AddressPart index : found.getAddressIndexList()) {
			AddressRegion mi = new AddressRegion();
			mi.length = index.getSize();
			mi.filePointer = index.getOffset();
			mi.name = index.getName();
			mi.enName = index.getNameEn();
			mi.indexNameOffset = index.getIndexNameOffset();
			for (CityBlock mr : index.getCitiesList()) {
				CitiesBlock cblock = new CitiesBlock();
				cblock.length = mr.getSize();
				cblock.filePointer = mr.getOffset();
				cblock.type = mr.getType();
				mi.cities.add(cblock);
			}
			mi.attributeTagsTable.addAll(index.getAdditionalTagsList());
			reader.addressIndexes.add(mi);
			reader.indexes.add(mi);
		}

		for (PoiPart index : found.getPoiIndexList()) {
			PoiRegion mi = new PoiRegion();
			mi.length = index.getSize();
			mi.filePointer = index.getOffset();
			mi.name = index.getName();
			mi.left31 = index.getLeft();
			mi.right31 = index.getRight();
			mi.top31 = index.getTop();
			mi.bottom31 = index.getBottom();
			reader.poiIndexes.add(mi);
			reader.indexes.add(mi);
		}

		for (TransportPart index : found.getTransportIndexList()) {
			TransportIndex mi = new TransportIndex();
			mi.length = index.getSize();
			mi.filePointer = index.getOffset();
			mi.name = index.getName();
			mi.left = index.getLeft();
			mi.right = index.getRight();
			mi.top = index.getTop();
			mi.bottom = index.getBottom();
			mi.stopsFileLength = index.getStopsTableLength();
			mi.stopsFileOffset = index.getStopsTableOffset();
			mi.incompleteRoutesLength = index.getIncompleteRoutesLength();
			mi.incompleteRoutesOffset = index.getIncompleteRoutesOffset();
			mi.stringTable = new IndexStringTable();
			mi.stringTable.fileOffset = index.getStringTableOffset();
			mi.stringTable.length = index.getStringTableLength();
			reader.transportIndexes.add(mi);
			reader.indexes.add(mi);
		}

		for (RoutingPart index : found.getRoutingIndexList()) {
			RouteRegion mi = new RouteRegion();
			mi.length = index.getSize();
			mi.filePointer = index.getOffset();
			mi.name = index.getName();

			for (RoutingSubregion mr : index.getSubregionsList()) {
				RouteSubregion sub = new RouteSubregion(mi);
				sub.length = mr.getSize();
				sub.filePointer = mr.getOffset();
				sub.left = mr.getLeft();
				sub.right = mr.getRight();
				sub.top = mr.getTop();
				sub.bottom = mr.getBottom();
				sub.shiftToData = mr.getShifToData();
				if (mr.getBasemap()) {
					mi.basesubregions.add(sub);
				} else {
					mi.subregions.add(sub);
				}
			}
			reader.routingIndexes.add(mi);
			reader.indexes.add(mi);
		}
		
		for (HHRoutingPart index : found.getHhRoutingIndexList()) {
			HHRouteRegion mi = new HHRouteRegion();
			mi.length = index.getSize();
			mi.filePointer = index.getOffset();
			mi.edition = index.getEdition();
			mi.profile = index.getProfile();
			mi.profileParams = index.getProfileParamsList();
			mi.top = new HHRoutePointsBox();
			mi.top.bottom = index.getBottom();
			mi.top.right = index.getRight();
			mi.top.left = index.getLeft();
			mi.top.top = index.getTop();
			reader.hhIndexes.add(mi);
			reader.indexes.add(mi);
		}

		return reader;
	}
	

	public void readFromFile(File f) throws IOException {
		long time = System.currentTimeMillis();
		FileInputStream is = new FileInputStream(f);
		try {
			storedIndex = OsmandIndex.OsmAndStoredIndex.newBuilder().mergeFrom(is).build();
			hasChanged = false;
			if (storedIndex.getVersion() != CachedOsmandIndexes.VERSION) {
				storedIndex = null;
			}
		} finally {
			is.close();
		}
		log.info("Initialize cache " + f.getName() + " " + (System.currentTimeMillis() - time) + " ms");
	}

	public void writeToFile(File f) throws IOException {
		if (hasChanged) {
			FileOutputStream outputStream = new FileOutputStream(f);
			try {
				storedIndexBuilder.build().writeTo(outputStream);
			} finally {
				outputStream.close();
			}
		}
	}

	

}
