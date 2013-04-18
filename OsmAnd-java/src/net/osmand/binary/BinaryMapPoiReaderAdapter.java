package net.osmand.binary;


import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.hash.TLongHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.sf.junidecode.Junidecode;

import org.apache.commons.logging.Log;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

public class BinaryMapPoiReaderAdapter {
	private static final Log LOG = PlatformUtil.getLog(BinaryMapPoiReaderAdapter.class);
	
	public static final int SHIFT_BITS_CATEGORY = 7;
	private static final int CATEGORY_MASK = (1 << SHIFT_BITS_CATEGORY) - 1 ;
	private static final int ZOOM_TO_SKIP_FILTER = 3;
	private static final int BUCKET_SEARCH_BY_NAME = 5;
	
	public static class PoiRegion extends BinaryIndexPart {

		List<String> categories = new ArrayList<String>();
		List<AmenityType> categoriesType = new ArrayList<AmenityType>();
		List<List<String> > subcategories = new ArrayList<List<String> >();
		
		double leftLongitude;
		double rightLongitude;
		double topLatitude;
		double bottomLatitude;
		
		public double getLeftLongitude() {
			return leftLongitude;
		}
		
		public double getRightLongitude() {
			return rightLongitude;
		}
		
		public double getTopLatitude() {
			return topLatitude;
		}
		
		public double getBottomLatitude() {
			return bottomLatitude;
		}
	}
	
	private CodedInputStream codedIS;
	private final BinaryMapIndexReader map;
	
	protected BinaryMapPoiReaderAdapter(BinaryMapIndexReader map){
		this.codedIS = map.codedIS;
		this.map = map;
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}
	
	private int readInt() throws IOException {
		return map.readInt();
	}
	
	private void readPoiBoundariesIndex(PoiRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndTileBox.LEFT_FIELD_NUMBER:
				region.leftLongitude = MapUtils.get31LongitudeX(codedIS.readUInt32());
				break;
			case OsmandOdb.OsmAndTileBox.RIGHT_FIELD_NUMBER:
				region.rightLongitude = MapUtils.get31LongitudeX(codedIS.readUInt32());
				break;
			case OsmandOdb.OsmAndTileBox.TOP_FIELD_NUMBER:
				region.topLatitude = MapUtils.get31LatitudeY(codedIS.readUInt32());
				break;
			case OsmandOdb.OsmAndTileBox.BOTTOM_FIELD_NUMBER:
				region.bottomLatitude = MapUtils.get31LatitudeY(codedIS.readUInt32());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	

	protected void readPoiIndex(PoiRegion region, boolean readCategories) throws IOException {
		int length;
		int oldLimit;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.NAME_FIELD_NUMBER :
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndPoiIndex.BOUNDARIES_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				readPoiBoundariesIndex(region);
				codedIS.popLimit(oldLimit);
				break; 
			case OsmandOdb.OsmAndPoiIndex.CATEGORIESTABLE_FIELD_NUMBER :
				if(!readCategories){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return;
				}
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimit(length);
				readCategory(region);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER :
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readCategory(PoiRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndCategoryTable.CATEGORY_FIELD_NUMBER :
				String cat = codedIS.readString().intern();
				region.categories.add(cat);
				region.categoriesType.add(AmenityType.fromString(cat));
				region.subcategories.add(new ArrayList<String>());
				break;
			case OsmandOdb.OsmAndCategoryTable.SUBCATEGORIES_FIELD_NUMBER :
				region.subcategories.get(region.subcategories.size() - 1).add(codedIS.readString().intern());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	public void initCategories(PoiRegion region) throws IOException {
		if(region.categories.isEmpty()) {
			codedIS.seek(region.filePointer);
			int oldLimit = codedIS.pushLimit(region.length);
			readPoiIndex(region, true);
			codedIS.popLimit(oldLimit);
		}
	}
	
	protected void searchPoiByName( PoiRegion region, SearchRequest<Amenity> req) throws IOException {
		TIntLongHashMap offsets = new TIntLongHashMap();
		CollatorStringMatcher matcher = new CollatorStringMatcher(req.nameQuery, 
				StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		long time = System.currentTimeMillis();
		int indexOffset = codedIS.getTotalBytesRead();
		while(true){
			if(req.isCancelled()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.NAMEINDEX_FIELD_NUMBER :
				int length = readInt();
				int oldLimit = codedIS.pushLimit(length);
				// here offsets are sorted by distance
				offsets = readPoiNameIndex(matcher.getCollator(), req.nameQuery, req);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.POIDATA_FIELD_NUMBER :
				// also offsets can be randomly skipped by limit
				Integer[] offKeys = new Integer[offsets.size()];
				if (offsets.size() > 0) {
					int[] keys = offsets.keys();
					for (int i = 0; i < keys.length; i++) {
						offKeys[i] = keys[i];
					}
					final TIntLongHashMap foffsets = offsets;
					Arrays.sort(offKeys, new Comparator<Integer>() {
						@Override
						public int compare(Integer object1, Integer object2) {
							return Double.compare(foffsets.get(object1), foffsets.get(object2));
						}
					});
					int p = BUCKET_SEARCH_BY_NAME * 3 ;
					if (p < offKeys.length) {
						for (int i = p + BUCKET_SEARCH_BY_NAME;; i += BUCKET_SEARCH_BY_NAME) {
							if (i > offKeys.length) {
								Arrays.sort(offKeys, p, offKeys.length);
								break;
							} else {
								Arrays.sort(offKeys, p, i);
							}
							p = i;
						}
					}
				}
				
				
				LOG.info("Searched poi structure in "+(System.currentTimeMillis() - time) + 
						"ms. Found " + offKeys.length +" subtress");
				for (int j = 0; j < offKeys.length; j++) {
					codedIS.seek(offKeys[j] + indexOffset);
					int len = readInt();
					int oldLim = codedIS.pushLimit(len);
					readPoiData(matcher, req, region);
					codedIS.popLimit(oldLim);
					if(req.isCancelled() || req.limitExceeded()){
						return;
					}
				}
				LOG.info("Whole poi by name search is done in "+(System.currentTimeMillis() - time) + 
						"ms. Found " + req.getSearchResults().size());
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private TIntLongHashMap readPoiNameIndex(Collator instance, String query, SearchRequest<Amenity> req) throws IOException {
		TIntLongHashMap offsets = new TIntLongHashMap();
		TIntArrayList dataOffsets = null;
		int offset = 0;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return offsets;
			case OsmandOdb.OsmAndPoiNameIndex.TABLE_FIELD_NUMBER : {
				int length = readInt();
				int oldLimit = codedIS.pushLimit(length);
				dataOffsets = new TIntArrayList();
				offset = codedIS.getTotalBytesRead();
				map.readIndexedStringTable(instance, query, "", dataOffsets, 0);
				codedIS.popLimit(oldLimit);
				break; }
			case OsmandOdb.OsmAndPoiNameIndex.DATA_FIELD_NUMBER : {
				if(dataOffsets != null){
					dataOffsets.sort(); // 1104125
					for (int i = 0; i < dataOffsets.size(); i++) {
						codedIS.seek(dataOffsets.get(i) + offset);
						int len = codedIS.readRawVarint32();
						int oldLim = codedIS.pushLimit(len);
						readPoiNameIndexData(offsets, req);
						codedIS.popLimit(oldLim);
						if (req.isCancelled()) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
							return offsets;
						}
					}
				}
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return offsets; }
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}

	private void readPoiNameIndexData(TIntLongHashMap offsets, SearchRequest<Amenity> req) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmAndPoiNameIndexData.ATOMS_FIELD_NUMBER :
				int len = codedIS.readRawVarint32();
				int oldLim = codedIS.pushLimit(len);
				readPoiNameIndexDataAtom(offsets, req);
				codedIS.popLimit(oldLim);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
		
	}

	private void readPoiNameIndexDataAtom(TIntLongHashMap offsets, SearchRequest<Amenity> req) throws IOException {
		int x = 0;
		int y = 0;
		int zoom = 15;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.X_FIELD_NUMBER :
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.Y_FIELD_NUMBER :
				y = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.ZOOM_FIELD_NUMBER :
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.SHIFTTO_FIELD_NUMBER :
				int x31 = (x << (31 - zoom));
				int y31 = (y << (31 - zoom));
				int shift = readInt();
				if (req.contains(x31, y31, x31, y31)) {
					long d = Math.abs(req.x - x31) + Math.abs(req.y - y31);
					offsets.put(shift, d);
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}


	protected void searchPoiIndex(int left31, int right31, int top31, int bottom31,
			SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		int indexOffset = codedIS.getTotalBytesRead();
		long time = System.currentTimeMillis();
		TLongHashSet skipTiles = null;
		int zoomToSkip = 31;
		if(req.zoom != -1){
			skipTiles = new TLongHashSet();
			zoomToSkip = req.zoom + ZOOM_TO_SKIP_FILTER;
		}
		int length ;
		int oldLimit ;
		TIntLongHashMap offsetsMap = new TIntLongHashMap();
		while(true){
			if(req.isCancelled()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER :
				length = readInt();
				oldLimit = codedIS.pushLimit(length);
				readBoxField(left31, right31, top31, bottom31, 0, 0, 0, offsetsMap,  skipTiles, req, region);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.POIDATA_FIELD_NUMBER :
				int[] offsets = offsetsMap.keys();
				// also offsets can be randomly skipped by limit
				Arrays.sort(offsets);
				if(skipTiles != null){
					skipTiles.clear();
				}
				LOG.info("Searched poi structure in "+(System.currentTimeMillis() - time) + 
						"ms. Found " + offsets.length +" subtress");
				for (int j = 0; j < offsets.length; j++) {
					codedIS.seek(offsets[j] + indexOffset);
					int len = readInt();
					int oldLim = codedIS.pushLimit(len);
					readPoiData(left31, right31, top31, bottom31, req, region, skipTiles, zoomToSkip);
					codedIS.popLimit(oldLim);
					if(req.isCancelled()){
						return;
					}
				}
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private void readPoiData(CollatorStringMatcher matcher, SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		int x = 0;
		int y = 0;
		int zoom = 0;
		while(true){
			if(req.isCancelled() || req.limitExceeded()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiBoxData.X_FIELD_NUMBER :
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.ZOOM_FIELD_NUMBER :
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.Y_FIELD_NUMBER :
				y = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.POIDATA_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				int oldLim = codedIS.pushLimit(len);
				Amenity am = readPoiPoint(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, x, y, zoom, req, region, false);
				codedIS.popLimit(oldLim);
				if (am != null) {
					if(matcher.matches(am.getName(false)) || matcher.matches(am.getName(true))) {
						req.publish(am);
					}
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readPoiData(int left31, int right31, int top31, int bottom31, 
			SearchRequest<Amenity> req, PoiRegion region, TLongHashSet toSkip, int zSkip) throws IOException {
		int x = 0;
		int y = 0;
		int zoom = 0;
		while(true){
			if(req.isCancelled()){
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiBoxData.X_FIELD_NUMBER :
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.ZOOM_FIELD_NUMBER :
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.Y_FIELD_NUMBER :
				y = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.POIDATA_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				int oldLim = codedIS.pushLimit(len);
				Amenity am = readPoiPoint(left31, right31, top31, bottom31, x, y, zoom, req, region, true);
				codedIS.popLimit(oldLim);
				if (am != null) {
					if (toSkip != null) {
						int xp = (int) MapUtils.getTileNumberX(zSkip, am.getLocation().getLongitude());
						int yp = (int) MapUtils.getTileNumberY(zSkip, am.getLocation().getLatitude());
						long val = (((long) xp) << zSkip) | yp;
						if (!toSkip.contains(val)) {
							boolean publish = req.publish(am);
							if(publish) {
								toSkip.add(val);
							}
						}
						if(zSkip <= zoom){
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
							return;
						}
					} else {
						req.publish(am);
					}
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private Amenity readPoiPoint(int left31, int right31, int top31, int bottom31, 
			int px, int py, int zoom, SearchRequest<Amenity> req, PoiRegion region, boolean checkBounds) throws IOException {
		Amenity am = null;
		int x = 0;
		int y = 0;
		AmenityType amenityType = null;
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			if(amenityType == null && (tag > OsmandOdb.OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER || tag == 0)) {
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return null;
			}
			switch (tag) {
			case 0:
				if(Algorithms.isEmpty(am.getEnName())){
					am.setEnName(Junidecode.unidecode(am.getName()));
				}
				req.numberOfAcceptedObjects++;
				return am;
			case OsmandOdb.OsmAndPoiBoxDataAtom.DX_FIELD_NUMBER :
				x = (codedIS.readSInt32() + (px << (24 - zoom))) << 7;
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.DY_FIELD_NUMBER :
				y = (codedIS.readSInt32() + (py << (24 - zoom))) << 7;
				req.numberOfVisitedObjects++;
				if (checkBounds) {
					if (left31 > x || right31 < x || top31 > y || bottom31 < y) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return null;
					}
				}
				am = new Amenity();
				am.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER :
				int cat = codedIS.readUInt32();
				int subcatId = cat >> SHIFT_BITS_CATEGORY;
				int catId = cat & CATEGORY_MASK;
				AmenityType type = AmenityType.OTHER;
				String subtype = "";
				if (catId < region.categoriesType.size()) {
					type = region.categoriesType.get(catId);
					List<String> subcats = region.subcategories.get(catId);
					if (subcatId < subcats.size()) {
						subtype = subcats.get(subcatId);
					}
				}
				if (req.poiTypeFilter == null || req.poiTypeFilter.accept(type, subtype)) {
					if (amenityType == null) {
						amenityType = type;
						am.setSubType(subtype);
						am.setType(amenityType);
					} else {
						am.setSubType(am.getSubType() + ";" + subtype);
					}
				}
				
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.ID_FIELD_NUMBER :
				am.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NAME_FIELD_NUMBER :
				am.setName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NAMEEN_FIELD_NUMBER :
				am.setEnName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.OPENINGHOURS_FIELD_NUMBER :
				am.setOpeningHours(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.SITE_FIELD_NUMBER :
				am.setSite(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.PHONE_FIELD_NUMBER:
				am.setPhone(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NOTE_FIELD_NUMBER:
				am.setDescription(codedIS.readString());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}
	
	private boolean checkCategories(SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		while(true){
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return false;
			case OsmandOdb.OsmAndPoiCategories.CATEGORIES_FIELD_NUMBER:
				AmenityType type = AmenityType.OTHER;
				String subcat = "";
				int cat = codedIS.readUInt32();
				int subcatId = cat >> SHIFT_BITS_CATEGORY;
				int catId = cat & CATEGORY_MASK;
				if(catId < region.categoriesType.size()){
					type = region.categoriesType.get(catId);
					List<String> subcats = region.subcategories.get(catId);
					if(subcatId < subcats.size()){
						subcat =  subcats.get(subcatId);
					}
				} else {
					type = AmenityType.OTHER;
				}
				if(req.poiTypeFilter.accept(type, subcat)){
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return true;
				}
				
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private boolean readBoxField(int left31, int right31, int top31, int bottom31,
			int px, int py, int pzoom, TIntLongHashMap offsetsMap, TLongHashSet skipTiles, SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		req.numberOfReadSubtrees++;
		int zoomToSkip = req.zoom + ZOOM_TO_SKIP_FILTER;
		boolean checkBox = true;
		boolean existsCategories = false;
		int zoom = pzoom;
		int dy = py;
		int dx = px;
		while(true){
			if(req.isCancelled()){
				return false;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return existsCategories;
			case OsmandOdb.OsmAndPoiBox.ZOOM_FIELD_NUMBER :
				zoom = codedIS.readUInt32() + pzoom;
				break;
			case OsmandOdb.OsmAndPoiBox.LEFT_FIELD_NUMBER :
				dx = codedIS.readSInt32();
				break;
			case OsmandOdb.OsmAndPoiBox.TOP_FIELD_NUMBER:
				dy = codedIS.readSInt32();
				break;
			case OsmandOdb.OsmAndPoiBox.CATEGORIES_FIELD_NUMBER:
				if(req.poiTypeFilter == null){
					skipUnknownField(t);
				} else {
					int length = codedIS.readRawVarint32();
					int oldLimit = codedIS.pushLimit(length);
					boolean check = checkCategories(req, region);
					codedIS.popLimit(oldLimit);
					if(!check){
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return false;
					}
					existsCategories = true;
				}
				break;
				
			case OsmandOdb.OsmAndPoiBox.SUBBOXES_FIELD_NUMBER: {
				int x = dx + (px << (zoom - pzoom));
				int y = dy + (py << (zoom - pzoom));
				if(checkBox){
					int xL = x << (31 - zoom);
					int xR = ((x + 1) << (31 - zoom)) - 1;
					int yT = y << (31 - zoom);
					int yB = ((y + 1) << (31 - zoom)) - 1;
					// check intersection
					if(left31 > xR || xL > right31 || bottom31 < yT || yB < top31){
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return false;
					}
					req.numberOfAcceptedSubtrees++;
					checkBox = false;
				}
				
				int length = readInt();
				int oldLimit = codedIS.pushLimit(length);
				boolean exists = readBoxField(left31, right31, top31, bottom31, x, y, zoom, offsetsMap, skipTiles, req, region);
				codedIS.popLimit(oldLimit);
				
				if (skipTiles != null && zoom >= zoomToSkip && exists) {
					long val = ((((long) x) >> (zoom - zoomToSkip)) << zoomToSkip) | (((long) y) >> (zoom - zoomToSkip));
					if(skipTiles.contains(val)){
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return true;
					}
				}
			} break;
			case OsmandOdb.OsmAndPoiBox.SHIFTTODATA_FIELD_NUMBER: {
				int x = dx + (px << (zoom - pzoom));
				int y = dy + (py << (zoom - pzoom));
				long l = ((((x << zoom) | y) << 5) | zoom);
				offsetsMap.put(readInt(), l);
				if(skipTiles != null && zoom >= zoomToSkip){
					long val = ((((long) x) >> (zoom - zoomToSkip)) << zoomToSkip) | (((long) y) >> (zoom - zoomToSkip));
					skipTiles.add(val);
				}
			}	break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

}
