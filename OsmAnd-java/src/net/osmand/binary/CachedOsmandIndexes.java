package net.osmand.binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

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
import net.osmand.binary.OsmandIndex.MapLevel;
import net.osmand.binary.OsmandIndex.MapPart;
import net.osmand.binary.OsmandIndex.OsmAndStoredIndex;
import net.osmand.binary.OsmandIndex.PoiPart;
import net.osmand.binary.OsmandIndex.RoutingPart;
import net.osmand.binary.OsmandIndex.RoutingSubregion;
import net.osmand.binary.OsmandIndex.TransportPart;
import net.osmand.util.MapUtils;
import net.osmand.utils.PlatformUtil;

import org.apache.commons.logging.Log;

public class CachedOsmandIndexes {
	
	private OsmAndStoredIndex storedIndex;
	private OsmAndStoredIndex.Builder storedIndexBuilder;
	private Log log = PlatformUtil.getLog(CachedOsmandIndexes.class);
	private boolean hasChanged = true;
	
	public static final int VERSION = 2;

	public void addToCache(BinaryMapIndexReader reader, File f) {
		hasChanged = true;
		if(storedIndexBuilder == null) {
			storedIndexBuilder = OsmandIndex.OsmAndStoredIndex.newBuilder();
			storedIndexBuilder.setVersion(VERSION);
			storedIndexBuilder.setDateCreated(System.currentTimeMillis());
			if(storedIndex != null) {
				for(FileIndex ex : storedIndex.getFileIndexList()) {
					storedIndexBuilder.addFileIndex(ex);
				}
			}
		}
		
		FileIndex.Builder fileIndex = OsmandIndex.FileIndex.newBuilder();
		long d = reader.getDateCreated();
		fileIndex.setDateModified(d== 0?f.lastModified() : d);
		fileIndex.setSize(f.length());
		fileIndex.setVersion(reader.getVersion());
		fileIndex.setFileName(f.getName());
		for(MapIndex index : reader.getMapIndexes()) {
			MapPart.Builder map = OsmandIndex.MapPart.newBuilder();
			map.setSize(index.getLength());
			map.setOffset(index.getFilePointer());
			if(index.getName() != null) {
				map.setName(index.getName());
			}
			for(MapRoot mr : index.getRoots() ) {
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
		
		for(AddressRegion index : reader.getAddressIndexes()) {
			AddressPart.Builder addr = OsmandIndex.AddressPart.newBuilder();
			addr.setSize(index.getLength());
			addr.setOffset(index.getFilePointer());
			if(index.getName() != null) {
				addr.setName(index.getName());
			}
			if(index.getEnName() != null) {
				addr.setNameEn(index.getEnName());
			}
			addr.setIndexNameOffset(index.getIndexNameOffset());
			for(CitiesBlock mr : index.getCities() ) {
				CityBlock.Builder cblock = OsmandIndex.CityBlock.newBuilder();
				cblock.setSize(mr.length);
				cblock.setOffset(mr.filePointer);
				cblock.setType(mr.type);
				addr.addCities(cblock);
			}
			fileIndex.addAddressIndex(addr);
		}
		
		for(PoiRegion index : reader.getPoiIndexes()) {
			PoiPart.Builder poi = OsmandIndex.PoiPart.newBuilder();
			poi.setSize(index.getLength());
			poi.setOffset(index.getFilePointer());
			if(index.getName() != null) {
				poi.setName(index.getName());
			}
			poi.setLeft(MapUtils.get31TileNumberX(index.getLeftLongitude()));
			poi.setRight(MapUtils.get31TileNumberX(index.getRightLongitude()));
			poi.setTop(MapUtils.get31TileNumberY(index.getTopLatitude()));
			poi.setBottom(MapUtils.get31TileNumberY(index.getBottomLatitude()));
			fileIndex.addPoiIndex(poi.build());
		}
		
		for(TransportIndex index : reader.getTransportIndexes()) {
			TransportPart.Builder transport = OsmandIndex.TransportPart.newBuilder();
			transport.setSize(index.getLength());
			transport.setOffset(index.getFilePointer());
			if(index.getName() != null) {
				transport.setName(index.getName());
			}
			transport.setLeft(index.getLeft());
			transport.setRight(index.getRight());
			transport.setTop(index.getTop());
			transport.setBottom(index.getBottom());
			transport.setStopsTableLength(index.stopsFileLength);
			transport.setStopsTableOffset(index.stopsFileOffset);
			transport.setStringTableLength(index.stringTable.length);
			transport.setStringTableOffset(index.stringTable.fileOffset);
			fileIndex.addTransportIndex(transport);
		}
		
		for(RouteRegion index : reader.getRoutingIndexes()) {
			RoutingPart.Builder routing = OsmandIndex.RoutingPart.newBuilder();
			routing.setSize(index.getLength());
			routing.setOffset(index.getFilePointer());
			if(index.getName() != null) {
				routing.setName(index.getName());
			}
			for(RouteSubregion sub : index.getSubregions()) {
				addRouteSubregion(routing, sub, false);
			}
			for(RouteSubregion sub : index.getBaseSubregions()) {
				addRouteSubregion(routing, sub, true);
			}
			fileIndex.addRoutingIndex(routing);
		}
		
		storedIndexBuilder.addFileIndex(fileIndex);
		
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
	
	public BinaryMapIndexReader getReader(File f) throws IOException {
		RandomAccessFile mf = new RandomAccessFile(f.getPath(), "r");
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
		BinaryMapIndexReader reader = null;
		if (found == null) {
			long val = System.currentTimeMillis();
			reader = new BinaryMapIndexReader(mf);
			addToCache(reader, f);
			if (log.isDebugEnabled()) {
				log.debug("Initializing db " + f.getAbsolutePath() + " " + (System.currentTimeMillis() - val ) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		} else {
			reader = initFileIndex(found, mf);
		}
		return reader;
	}
	
	private BinaryMapIndexReader initFileIndex(FileIndex found, RandomAccessFile mf) throws IOException {
		BinaryMapIndexReader reader = new BinaryMapIndexReader(mf, false);
		reader.version = found.getVersion();
		reader.dateCreated = found.getDateModified();
		
		for(MapPart index : found.getMapIndexList()) {
			MapIndex mi = new MapIndex();
			mi.length = (int) index.getSize();
			mi.filePointer = (int) index.getOffset();
			mi.name = index.getName();
			
			for(MapLevel mr : index.getLevelsList()) {
				MapRoot root = new MapRoot();
				root.length = (int) mr.getSize();
				root.filePointer = (int) mr.getOffset();
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
		
		for(AddressPart index : found.getAddressIndexList()) {
			AddressRegion mi = new AddressRegion();
			mi.length = (int) index.getSize();
			mi.filePointer = (int) index.getOffset();
			mi.name = index.getName();
			mi.enName = index.getNameEn();
			mi.indexNameOffset = index.getIndexNameOffset();
			for(CityBlock mr : index.getCitiesList() ) {
				CitiesBlock cblock = new CitiesBlock();
				cblock.length = (int) mr.getSize();
				cblock.filePointer = (int) mr.getOffset();
				cblock.type = mr.getType();
				mi.cities.add(cblock);
			}
			reader.addressIndexes.add(mi);
			reader.indexes.add(mi);
		}
		
		for(PoiPart index : found.getPoiIndexList()) {
			PoiRegion mi = new PoiRegion();
			mi.length = (int) index.getSize();
			mi.filePointer = (int) index.getOffset();
			mi.name = index.getName();
			mi.leftLongitude = MapUtils.get31LongitudeX(index.getLeft());
			mi.rightLongitude = MapUtils.get31LongitudeX(index.getRight());
			mi.topLatitude =MapUtils.get31LatitudeY(index.getTop());
			mi.bottomLatitude = MapUtils.get31LatitudeY(index.getBottom());
			reader.poiIndexes.add(mi);
			reader.indexes.add(mi);
		}
		
		for(TransportPart index : found.getTransportIndexList()) {
			TransportIndex mi = new TransportIndex();
			mi.length = (int) index.getSize();
			mi.filePointer = (int) index.getOffset();
			mi.name = index.getName();
			mi.left = index.getLeft();
			mi.right =index.getRight();
			mi.top = index.getTop();
			mi.bottom = index.getBottom();
			mi.stopsFileLength = index.getStopsTableLength();
			mi.stopsFileOffset = index.getStopsTableOffset();
			mi.stringTable = new IndexStringTable();
			mi.stringTable.fileOffset = index.getStringTableOffset();
			mi.stringTable.length = index.getStringTableLength();
			reader.transportIndexes.add(mi);
			reader.indexes.add(mi);
		}
		
		for(RoutingPart  index : found.getRoutingIndexList()) {
			RouteRegion mi = new RouteRegion();
			mi.length = (int) index.getSize();
			mi.filePointer = (int) index.getOffset();
			mi.name = index.getName();
			
			for(RoutingSubregion mr : index.getSubregionsList()) {
				RouteSubregion sub = new RouteSubregion(mi);
				sub.length = (int) mr.getSize();
				sub.filePointer = (int) mr.getOffset();
				sub.left = mr.getLeft();
				sub.right = mr.getRight();
				sub.top = mr.getTop();
				sub.bottom = mr.getBottom();
				sub.shiftToData = mr.getShifToData();
				if(mr.getBasemap()) {
					mi.basesubregions.add(sub);
				} else {
					mi.subregions.add(sub);
				}
			}
			reader.routingIndexes.add(mi);
			reader.indexes.add(mi);
		}
		
		return reader;
	}

	public void readFromFile(File f, int version) throws IOException {
		long time = System.currentTimeMillis();
		FileInputStream is = new FileInputStream(f);
		try {
			storedIndex = OsmandIndex.OsmAndStoredIndex.newBuilder().mergeFrom(is).build();
			hasChanged = false;
			if(storedIndex.getVersion() != version){
				storedIndex = null;
			}
		} finally {
			is.close();
		}
		log.info("Initialize cache " + (System.currentTimeMillis() - time));
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
