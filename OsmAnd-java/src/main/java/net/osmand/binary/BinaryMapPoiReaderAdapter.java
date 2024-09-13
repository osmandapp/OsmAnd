package net.osmand.binary;


import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.hash.TLongHashSet;
import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.data.Amenity;
import net.osmand.data.Amenity.AmenityRoutePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.*;

public class BinaryMapPoiReaderAdapter {
	private static final Log LOG = PlatformUtil.getLog(BinaryMapPoiReaderAdapter.class);

	public static final int SHIFT_BITS_CATEGORY = 7;
	private static final int CATEGORY_MASK = (1 << SHIFT_BITS_CATEGORY) - 1;
	private static final int ZOOM_TO_SKIP_FILTER_READ = 6;
	private static final int ZOOM_TO_SKIP_FILTER = 3;
	private static final int BUCKET_SEARCH_BY_NAME = 15; // should be bigger 100?
	private static final int BASE_POI_SHIFT = SHIFT_BITS_CATEGORY;// 7
	private static final int FINAL_POI_SHIFT = BinaryMapIndexReader.SHIFT_COORDINATES;// 5
	private static final int BASE_POI_ZOOM = 31 - BASE_POI_SHIFT;// 24 zoom
	private static final int FINAL_POI_ZOOM = 31 - FINAL_POI_SHIFT;// 26 zoom


	public static class PoiSubType {
		public boolean text;
		public String name;
		//int estiatedSize;
		public List<String> possibleValues = null;
	}

	public static class PoiRegion extends BinaryIndexPart {
		List<String> categories = new ArrayList<String>();
		List<PoiCategory> categoriesType = new ArrayList<PoiCategory>();
		List<List<String>> subcategories = new ArrayList<List<String>>();
		List<PoiSubType> subTypes = new ArrayList<PoiSubType>();
		List<PoiSubType> topIndexSubTypes = new ArrayList<PoiSubType>();
		Map<Integer, List<TagValuePair>> tagGroups = new HashMap<>();
		QuadTree<Void> bboxIndexCache = new QuadTree<Void>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
				8, 0.55f);
		static boolean MAP_HAS_TAG_GROUPS = false;

		int left31;
		int right31;
		int top31;
		int bottom31;
		
		public int getLeft31() {
			return left31;
		}
		
		public int getRight31() {
			return right31;
		}
		
		public int getTop31() {
			return top31;
		}
		
		public int getBottom31() {
			return bottom31;
		}

		public String getPartName() {
			return "POI";
		}

		
		public List<String> getCategories() {
			return categories;
		}
		
		public List<List<String>> getSubcategories() {
			return subcategories;
		}
		
		public List<PoiSubType> getSubTypes() {
			return subTypes;
		}

		public List<PoiSubType> getTopIndexSubTypes() {
			return topIndexSubTypes;
		}
		
		public int getFieldNumber() {
			return OsmandOdb.OsmAndStructure.POIINDEX_FIELD_NUMBER;
		}

		public PoiSubType getSubtypeFromId(int id, StringBuilder returnValue) {
			int tl;
			int sl;
			if (id % 2 == 0) {
				tl = (id >> 1) & ((1 << 5) - 1);
				sl = id >> 6;
			} else {
				tl = (id >> 1) & ((1 << 15) - 1);
				sl = id >> 16;
			}
			if (subTypes.size() > tl) {
				PoiSubType st = subTypes.get(tl);
				if (st.text) {
					return st;
				} else if (st.possibleValues != null && st.possibleValues.size() > sl) {
					returnValue.append(st.possibleValues.get(sl));
					return st;
				}
			}
			return null;
		}

		public List<TagValuePair> getTagValues(int id) {
			return tagGroups.getOrDefault(id, new ArrayList<>());
		}

	}

	private CodedInputStream codedIS;
	private final BinaryMapIndexReader map;

	private MapPoiTypes poiTypes;

	protected BinaryMapPoiReaderAdapter(BinaryMapIndexReader map) {
		this.codedIS = map.codedIS;
		this.map = map;
		this.poiTypes = MapPoiTypes.getDefault();
	}

	private void skipUnknownField(int t) throws IOException {
		map.skipUnknownField(t);
	}

	private long readInt() throws IOException {
		return map.readInt();
	}

	private void readPoiBoundariesIndex(PoiRegion region) throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndTileBox.LEFT_FIELD_NUMBER:
				region.left31 = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndTileBox.RIGHT_FIELD_NUMBER:
				region.right31 = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndTileBox.TOP_FIELD_NUMBER:
				region.top31 = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndTileBox.BOTTOM_FIELD_NUMBER:
				region.bottom31 = codedIS.readUInt32();
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	protected void readPoiIndex(PoiRegion region, boolean readCategories) throws IOException {
		int length;
		long oldLimit;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.NAME_FIELD_NUMBER:
				region.name = codedIS.readString();
				break;
			case OsmandOdb.OsmAndPoiIndex.BOUNDARIES_FIELD_NUMBER:
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				readPoiBoundariesIndex(region);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.CATEGORIESTABLE_FIELD_NUMBER:
				if (!readCategories) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return;
				}
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				readCategory(region);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.SUBTYPESTABLE_FIELD_NUMBER:
				if (!readCategories) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return;
				}
				length = codedIS.readRawVarint32();
				oldLimit = codedIS.pushLimitLong((long) length);
				readSubtypes(region);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER:
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readCategory(PoiRegion region) throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndCategoryTable.CATEGORY_FIELD_NUMBER:
				String cat = codedIS.readString().intern();
				region.categories.add(cat);
				region.categoriesType.add(poiTypes.getPoiCategoryByName(cat.toLowerCase(), true));
				region.subcategories.add(new ArrayList<String>());
				break;
			case OsmandOdb.OsmAndCategoryTable.SUBCATEGORIES_FIELD_NUMBER:
				region.subcategories.get(region.subcategories.size() - 1).add(codedIS.readString().intern());
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readSubtypes(PoiRegion region) throws IOException {
		while (true) {
			int outT = codedIS.readTag();
			int outTag = WireFormat.getTagFieldNumber(outT);
			switch (outTag) {
			case 0:
				return;
			case OsmandOdb.OsmAndSubtypesTable.SUBTYPES_FIELD_NUMBER:
				int length = codedIS.readRawVarint32();
				long oldLimit = codedIS.pushLimitLong((long) length);
				PoiSubType st = new PoiSubType();
				cycle: while(true){
					int inT = codedIS.readTag();
					int inTag = WireFormat.getTagFieldNumber(inT);
					switch (inTag) {
					case 0:
						break cycle;
					case OsmandOdb.OsmAndPoiSubtype.NAME_FIELD_NUMBER:
						st.name = codedIS.readString().intern();
						break;
					case OsmandOdb.OsmAndPoiSubtype.SUBTYPEVALUE_FIELD_NUMBER:
						if (st.possibleValues == null) {
							st.possibleValues = new ArrayList<String>();
						}
						st.possibleValues.add(codedIS.readString().intern());
						break;
					case OsmandOdb.OsmAndPoiSubtype.ISTEXT_FIELD_NUMBER:
						st.text = codedIS.readBool();
						break;
					default:
						skipUnknownField(inT);
						break;
					}
				}
				region.subTypes.add(st);
				if (poiTypes.topIndexPoiAdditional.containsKey(st.name)) {
					region.topIndexSubTypes.add(st);
				}
				codedIS.popLimit(oldLimit);
				break;
			default:
				skipUnknownField(outT);
				break;
			}
		}
	}

	public void initCategories(PoiRegion region) throws IOException {
		if (region.categories.isEmpty()) {
			codedIS.seek(region.filePointer);
			long oldLimit = codedIS.pushLimitLong((long) region.length);
			readPoiIndex(region, true);
			codedIS.popLimit(oldLimit);
		}
	}

	private String normalizeSearchPoiByNameQuery(String query) {
		return query.replace("\"", "").toLowerCase();
	}

	protected void searchPoiByName(PoiRegion region, SearchRequest<Amenity> req) throws IOException {
		TIntLongHashMap offsets = new TIntLongHashMap();
		String query = normalizeSearchPoiByNameQuery(req.nameQuery);
		CollatorStringMatcher matcher = new CollatorStringMatcher(query,
				StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		long indexOffset = codedIS.getTotalBytesRead();
		TIntLongHashMap offsetsMap = new TIntLongHashMap();
		List<Integer> nameIndexCoordinates = new ArrayList<>();
		QuadTree<Void> nameIndexTree = null;
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.NAMEINDEX_FIELD_NUMBER:
				long length = readInt();
				long oldLimit = codedIS.pushLimitLong((long) length);
				// here offsets are sorted by distance
				offsets = readPoiNameIndex(matcher.getCollator(), query, req, region, nameIndexCoordinates);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER:
				length = readInt();
				oldLimit = codedIS.pushLimitLong((long) length);
				if (nameIndexCoordinates.size() > 0 && nameIndexTree == null) {
					nameIndexTree = new QuadTree<Void>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
							8, 0.55f);
					for (int i = 0; i < nameIndexCoordinates.size(); i = i + 2) {
						int x = nameIndexCoordinates.get(i);
						int y = nameIndexCoordinates.get(i + 1);
						nameIndexTree.insert(null, new QuadRect(x, y, x, y));
					}
				}
				BinaryMapIndexReader.SearchPoiTypeFilter filter = req.poiTypeFilter;
				req.poiTypeFilter = null;//init for all categories
				readBoxField(0, 0, 0, 0, 0, 0, 0, offsetsMap, null, req, region, nameIndexTree);
				req.poiTypeFilter = filter;
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.POIDATA_FIELD_NUMBER:
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
					int p = BUCKET_SEARCH_BY_NAME * 3;
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

//				LOG.info("Searched poi structure in " + (System.currentTimeMillis() - time) +
//						"ms. Found " + offKeys.length + " subtrees");
				for (int j = 0; j < offKeys.length; j++) {
					codedIS.seek(offKeys[j] + indexOffset);
					long len = readInt();
					long oldLim = codedIS.pushLimitLong((long) len);
					readPoiData(matcher, req, region);
					codedIS.popLimit(oldLim);
					if (req.isCancelled() || req.limitExceeded()) {
						return;
					}
				}
//				LOG.info("Whole poi by name search is done in " + (System.currentTimeMillis() - time) +
//						"ms. Found " + req.getSearchResults().size());
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private TIntLongHashMap readPoiNameIndex(Collator instance, String query, SearchRequest<Amenity> req, PoiRegion region, List<Integer> nameIndexCoordinates) throws IOException {
		TIntLongHashMap offsets = new TIntLongHashMap();
		List<TIntArrayList> listOffsets = null;
		List<TIntLongHashMap> listOfSepOffsets = new ArrayList<TIntLongHashMap>();
		long offset = 0;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return offsets;
			case OsmandOdb.OsmAndPoiNameIndex.TABLE_FIELD_NUMBER: {
				long length = readInt();
				long oldLimit = codedIS.pushLimitLong((long) length);
				offset = codedIS.getTotalBytesRead();
				List<String> queries = Algorithms.splitByWordsLowercase(query);
				TIntArrayList charsList = new TIntArrayList(queries.size());
				listOffsets = new ArrayList<TIntArrayList>(queries.size());
				while (listOffsets.size() < queries.size()) {
					charsList.add(0);
					listOffsets.add(new TIntArrayList());
				}
				map.readIndexedStringTable(instance, queries, "", listOffsets, charsList);
				codedIS.popLimit(oldLimit);
				break;
			}
			case OsmandOdb.OsmAndPoiNameIndex.DATA_FIELD_NUMBER: {
				if (listOffsets != null) {
					for (TIntArrayList dataOffsets : listOffsets) {
						TIntLongHashMap offsetMap = new TIntLongHashMap();
						listOfSepOffsets.add(offsetMap);
						dataOffsets.sort(); // 1104125
						for (int i = 0; i < dataOffsets.size(); i++) {
							codedIS.seek(dataOffsets.get(i) + offset);
							int len = codedIS.readRawVarint32();
							long oldLim = codedIS.pushLimitLong((long) len);
							readPoiNameIndexData(offsetMap, req, region, nameIndexCoordinates);
							codedIS.popLimit(oldLim);
							if (req.isCancelled()) {
								codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
								return offsets;
							}
						}
					}
				}
				if (listOfSepOffsets.size() > 0) {
					offsets.putAll(listOfSepOffsets.get(0));
					for (int j = 1; j < listOfSepOffsets.size(); j++) {
						TIntLongHashMap mp = listOfSepOffsets.get(j);
						// offsets.retainAll(mp); -- calculate intresection of mp & offsets
						for (int chKey : offsets.keys()) {
							if (!mp.containsKey(chKey)) {
								offsets.remove(chKey);
							}
						}
					}
				}
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return offsets;
			}
			default:
				skipUnknownField(t);
				break;
			}
		}

	}

	private void readPoiNameIndexData(TIntLongHashMap offsets, SearchRequest<Amenity> req, PoiRegion region, List<Integer> nameIndexCoordinates) throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
				case 0:
					return;
				case OsmAndPoiNameIndexData.ATOMS_FIELD_NUMBER:
					int len = codedIS.readRawVarint32();
					long oldLim = codedIS.pushLimitLong((long) len);
					readPoiNameIndexDataAtom(offsets, req, region, nameIndexCoordinates);
					codedIS.popLimit(oldLim);
					break;
				default:
					skipUnknownField(t);
				break;
			}
		}
	}

	private void readPoiNameIndexDataAtom(TIntLongHashMap offsets, SearchRequest<Amenity> req, PoiRegion region, List<Integer> nameIndexCoordinates) throws IOException {
		int x = 0;
		int y = 0;
		int zoom = 15;
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.X_FIELD_NUMBER:
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.Y_FIELD_NUMBER:
				y = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.ZOOM_FIELD_NUMBER:
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiNameIndexDataAtom.SHIFTTO_FIELD_NUMBER:
				int x31 = (x << (31 - zoom));
				int y31 = (y << (31 - zoom));
				long l = readInt();
				if(l > Integer.MAX_VALUE) {
					throw new IllegalStateException();
				}
				int shift = (int) l;
				if (req.contains(x31, y31, x31, y31)) {
					long d = Math.abs(req.x - x31) + Math.abs(req.y - y31);
					offsets.put(shift, d);
				}

				List<Void> bboxResult = new ArrayList<>();
				region.bboxIndexCache.queryInBox(new QuadRect(x31, y31, x31, y31), bboxResult);
				if (bboxResult.size() == 0) {
					nameIndexCoordinates.add(x31);
					nameIndexCoordinates.add(y31);
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
		long indexOffset = codedIS.getTotalBytesRead();
		long time = System.currentTimeMillis();
		TLongHashSet skipTiles = null;
		if (req.zoom >= 0 && req.zoom < 16) {
			skipTiles = new TLongHashSet();
		}
		long length;
		long oldLimit;
		TIntLongHashMap offsetsMap = new TIntLongHashMap();
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiIndex.BOXES_FIELD_NUMBER:
				length = readInt();
				oldLimit = codedIS.pushLimitLong((long) length);
				readBoxField(left31, right31, top31, bottom31, 0, 0, 0, offsetsMap, skipTiles, req, region, null);
				codedIS.popLimit(oldLimit);
				break;
			case OsmandOdb.OsmAndPoiIndex.POIDATA_FIELD_NUMBER:
				int[] offsets = offsetsMap.keys();
				// also offsets can be randomly skipped by limit
				Arrays.sort(offsets);
				if (skipTiles != null) {
					skipTiles.clear();
				}
//				LOG.info("Searched poi structure in " + (System.currentTimeMillis() - time) + " ms. Found "
//						+ offsets.length + " subtrees");
				for (int j = 0; j < offsets.length; j++) {
					long skipVal = offsetsMap.get(offsets[j]);
					if (skipTiles != null && skipVal != -1) {
						int dzoom = ZOOM_TO_SKIP_FILTER_READ - ZOOM_TO_SKIP_FILTER;
						long dx = (skipVal >> ZOOM_TO_SKIP_FILTER_READ);
						long dy = skipVal - (dx << ZOOM_TO_SKIP_FILTER_READ);
						skipVal = ((dx >> dzoom) << ZOOM_TO_SKIP_FILTER) | (dy >> dzoom);
						if (skipVal != -1 && skipTiles.contains(skipVal)) {
							continue;
						}
					}
					codedIS.seek(offsets[j] + indexOffset);
					long len = readInt();
					long oldLim = codedIS.pushLimitLong((long) len);
					boolean read = readPoiData(left31, right31, top31, bottom31, req, region, skipTiles,
							req.zoom == -1 ? 31 : req.zoom + ZOOM_TO_SKIP_FILTER);
					if (read && skipVal != -1 && skipTiles != null) {
						skipTiles.add(skipVal);
					}
					codedIS.popLimit(oldLim);
					if (req.isCancelled()) {
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
		while (true) {
			if (req.isCancelled() || req.limitExceeded()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return;
			case OsmandOdb.OsmAndPoiBoxData.X_FIELD_NUMBER:
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.ZOOM_FIELD_NUMBER:
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.Y_FIELD_NUMBER:
				y = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.POIDATA_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				long oldLim = codedIS.pushLimitLong((long) len);
				Amenity am = readPoiPoint(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, x, y, zoom, req, region, false);
				codedIS.popLimit(oldLim);
				if (am != null) {
					boolean matches = matcher.matches(am.getName().toLowerCase())
							|| matcher.matches(am.getEnName(true).toLowerCase());
					if (!matches) {
						for (String s : am.getOtherNames()) {
							matches = matcher.matches(s.toLowerCase());
							if (matches) {
								break;
							}
						}
						if (!matches) {
							for (String key : am.getAdditionalInfoKeys()) {
								if (!key.contains("_name") && !key.equals("brand")) {
									continue;
								}
								matches = matcher.matches(am.getAdditionalInfo(key));
								if (matches) {
									break;
								}
							}
						}
					}
					if (matches) {
						req.collectRawData(am);
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

	private boolean readPoiData(int left31, int right31, int top31, int bottom31,
			SearchRequest<Amenity> req, PoiRegion region, TLongHashSet toSkip, int zSkip) throws IOException {
		int x = 0;
		int y = 0;
		int zoom = 0;
		boolean read = false;
		while (true) {
			if (req.isCancelled()) {
				return read;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return read;
			case OsmandOdb.OsmAndPoiBoxData.X_FIELD_NUMBER:
				x = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.ZOOM_FIELD_NUMBER:
				zoom = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.Y_FIELD_NUMBER:
				y = codedIS.readUInt32();
				break;
			case OsmandOdb.OsmAndPoiBoxData.POIDATA_FIELD_NUMBER:
				int len = codedIS.readRawVarint32();
				long oldLim = codedIS.pushLimitLong((long) len);
				Amenity am = readPoiPoint(left31, right31, top31, bottom31, x, y, zoom, req, region, true);
				codedIS.popLimit(oldLim);
				if (am != null) {
					if (toSkip != null) {
						int xp = (int) MapUtils.getTileNumberX(zSkip, am.getLocation().getLongitude());
						int yp = (int) MapUtils.getTileNumberY(zSkip, am.getLocation().getLatitude());
						long valSkip = (((long) xp) << zSkip) | yp;
						if (!toSkip.contains(valSkip)) {
							req.collectRawData(am);
							boolean publish = req.publish(am);
							if (publish) {
								read = true;
								toSkip.add(valSkip);
							}
						} else if (zSkip <= zoom) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
							return read;
						}
					} else {
						req.collectRawData(am);
						if (req.publish(am)) {
							read = true;
						}
					}
				}
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private AmenityRoutePoint dist(LatLon l, List<Location> locations, double radius) {
		float dist = (float) (radius + 0.1);
		AmenityRoutePoint arp = null;
		// Special iterations because points stored by pairs!
		for (int i = 1; i < locations.size(); i += 2) {
			float d = (float) MapUtils.getOrthogonalDistance(l.getLatitude(), l.getLongitude(), locations.get(i - 1)
					.getLatitude(), locations.get(i - 1).getLongitude(), locations.get(i).getLatitude(),
					locations.get(i).getLongitude());
			if (d < dist) {
				arp = new Amenity.AmenityRoutePoint();
				dist = d;
				arp.deviateDistance = dist;
				arp.pointA = locations.get(i - 1);
				arp.pointB = locations.get(i);
			}
		}
		if (arp != null && arp.deviateDistance != 0 && arp.pointA != null && arp.pointB != null) {
			arp.deviationDirectionRight = MapUtils.rightSide(l.getLatitude(), l.getLongitude(),
					arp.pointA.getLatitude(), arp.pointA.getLongitude(),
					arp.pointB.getLatitude(), arp.pointB.getLongitude());
		}
		return arp;
	}

	private Amenity readPoiPoint(int left31, int right31, int top31, int bottom31,
			int px, int py, int zoom, SearchRequest<Amenity> req, PoiRegion region, boolean checkBounds) throws IOException {
		Amenity am = null;
		int x = 0;
		int y = 0;
		int precisionXY = 0;
		boolean hasLocation = false;
		StringBuilder retValue = new StringBuilder();
		PoiCategory amenityType = null;
		LinkedList<String> textTags = null;
		boolean hasSubcategoriesField = false;
		boolean topIndexAdditonalFound = false;
		Map<String, PoiCategory> otherSubTypes = new HashMap<>();
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			if (amenityType == null && (tag > OsmandOdb.OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER || tag == 0)) {
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
				return null;
			}
			if (req.poiAdditionalFilter != null && (tag > OsmandOdb.OsmAndPoiBoxDataAtom.SUBCATEGORIES_FIELD_NUMBER || tag == 0)) {
				if (!hasSubcategoriesField || !topIndexAdditonalFound) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return null;
				}
			}
			switch (tag) {
			case 0:
				req.numberOfAcceptedObjects++;
				if (hasLocation) {
					if (precisionXY != 0) {
						int[] xy = MapUtils.calculateFinalXYFromBaseAndPrecisionXY(BASE_POI_ZOOM, FINAL_POI_ZOOM, precisionXY, x >> BASE_POI_SHIFT, y >> BASE_POI_SHIFT, true);
						int x31 = xy[0] << FINAL_POI_SHIFT;
						int y31 = xy[1] << FINAL_POI_SHIFT;
						am.setLocation(MapUtils.get31LatitudeY(y31), MapUtils.get31LongitudeX(x31));
					} else {
						am.setLocation(MapUtils.get31LatitudeY(y), MapUtils.get31LongitudeX(x));
					}
				} else {
					return null;
				}

				if (req.radius > 0) {
					LatLon loc = am.getLocation();
					List<Location> locs = req.tiles.get(req.getTileHashOnPath(loc.getLatitude(), loc.getLongitude()));
					if (locs == null) {
						return null;
					}
					AmenityRoutePoint arp = dist(am.getLocation(), locs, req.radius);
					if (arp == null) {
						return null;
					} else {
						am.setRoutePoint(arp);
					}
				}
				if (req.poiTypeFilter != null) {
					//multivalue amenity, add other subtypes
					for (Map.Entry<String, PoiCategory> entry : otherSubTypes.entrySet()) {
						PoiCategory cat = entry.getValue();
						if (am.getType() == cat) {
							String sub = entry.getKey();
							am.setSubType(am.getSubType() + ";" + sub);
						}
					}
				}
				return am;
			case OsmandOdb.OsmAndPoiBoxDataAtom.DX_FIELD_NUMBER:
				x = (codedIS.readSInt32() + (px << (BASE_POI_ZOOM - zoom))) << BASE_POI_SHIFT;
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.DY_FIELD_NUMBER:
				y = (codedIS.readSInt32() + (py << (BASE_POI_ZOOM - zoom))) << BASE_POI_SHIFT;
				req.numberOfVisitedObjects++;
				if (checkBounds) {
					if (left31 > x || right31 < x || top31 > y || bottom31 < y) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return null;
					}
				}
				am = new Amenity();
				hasLocation = true;
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.SUBCATEGORIES_FIELD_NUMBER:
				int subtypev = codedIS.readUInt32();
				retValue.setLength(0);
				hasSubcategoriesField = true;
				PoiSubType st = region.getSubtypeFromId(subtypev, retValue);
				boolean topIndex = region.topIndexSubTypes.contains(st);
				if (req.poiAdditionalFilter != null) {
					if (st != null && req.poiAdditionalFilter.accept(st, retValue.toString())) {
						topIndexAdditonalFound = true;
					}
				}
				if (st != null && !topIndex) {
					am.setAdditionalInfo(st.name, retValue.toString());
				}
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.TEXTCATEGORIES_FIELD_NUMBER:
				int texttypev = codedIS.readUInt32();
				retValue.setLength(0);
				PoiSubType textt = region.getSubtypeFromId(texttypev, retValue);
				if (textt != null && textt.text) {
					if (textTags == null) {
						textTags = new LinkedList<String>();
					}
					textTags.add(textt.name);
				}
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.TEXTVALUES_FIELD_NUMBER:
				String str = codedIS.readString();
				if (textTags != null && !textTags.isEmpty()) {
					am.setAdditionalInfo(textTags.poll(), str);
				}
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER:
				int cat = codedIS.readUInt32();
				int subcatId = cat >> SHIFT_BITS_CATEGORY;
				int catId = cat & CATEGORY_MASK;
				PoiCategory type = poiTypes.getOtherPoiCategory();
				String subtype = "";
				if (catId < region.categoriesType.size()) {
					type = region.categoriesType.get(catId);
					List<String> subcats = region.subcategories.get(catId);
					if (subcatId < subcats.size()) {
						subtype = subcats.get(subcatId);
					}
				}
				subtype = poiTypes.replaceDeprecatedSubtype(type, subtype);
				boolean isForbidden = poiTypes.isTypeForbidden(subtype);
				if (!isForbidden && (req.poiTypeFilter == null || req.poiTypeFilter.accept(type, subtype))) {
					if (amenityType == null) {
						amenityType = type;
						am.setSubType(subtype);
						am.setType(amenityType);
					} else {
						am.setSubType(am.getSubType() + ";" + subtype);
					}
				} else {
					otherSubTypes.put(subtype, type);
				}
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.ID_FIELD_NUMBER:
				am.setId(codedIS.readUInt64());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NAME_FIELD_NUMBER:
				am.setName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NAMEEN_FIELD_NUMBER:
				am.setEnName(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.OPENINGHOURS_FIELD_NUMBER:
				am.setOpeningHours(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.SITE_FIELD_NUMBER:
				am.setSite(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.PHONE_FIELD_NUMBER:
				am.setPhone(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.NOTE_FIELD_NUMBER:
				am.setDescription(codedIS.readString());
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.PRECISIONXY_FIELD_NUMBER:
				if (hasLocation) {
					precisionXY = codedIS.readInt32();
				}
				break;
			case OsmandOdb.OsmAndPoiBoxDataAtom.TAGGROUPS_FIELD_NUMBER:
				PoiRegion.MAP_HAS_TAG_GROUPS = true;
				long sz = codedIS.readRawVarint32();
				long old = codedIS.pushLimitLong((long) sz);
				while (codedIS.getBytesUntilLimit() > 0) {
					int tagGroupId = codedIS.readUInt32();
					List<TagValuePair> list = region.getTagValues(tagGroupId);
					if (list.size() > 0) {
						am.addTagGroup(tagGroupId, list);
					}
				}
				codedIS.popLimit(old);
				break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private boolean checkCategories(SearchRequest<Amenity> req, PoiRegion region) throws IOException {
		while (true) {
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return false;
			case OsmandOdb.OsmAndPoiCategories.SUBCATEGORIES_FIELD_NUMBER:
				int subcat = codedIS.readUInt32();
				StringBuilder subType = new StringBuilder();
				PoiSubType poiSubType = region.getSubtypeFromId(subcat, subType);
				String val = subType.toString();
				if (poiSubType != null && !val.isEmpty()
						&& req.poiAdditionalFilter != null
						&& req.poiAdditionalFilter.accept(poiSubType, val)) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
					return true;
				}
				break;
			case OsmandOdb.OsmAndPoiCategories.CATEGORIES_FIELD_NUMBER:
				PoiCategory type = poiTypes.getOtherPoiCategory();
				String subtype = "";
				int cat = codedIS.readUInt32();
				int subcatId = cat >> SHIFT_BITS_CATEGORY;
				int catId = cat & CATEGORY_MASK;
				if (catId < region.categoriesType.size()) {
					type = region.categoriesType.get(catId);
					List<String> subcats = region.subcategories.get(catId);
					if (subcatId < subcats.size()) {
						subtype = subcats.get(subcatId);
					}
				}
				subtype = poiTypes.replaceDeprecatedSubtype(type, subtype);
				if (req.poiTypeFilter.accept(type, subtype)) {
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
			int px, int py, int pzoom, TIntLongHashMap offsetsMap, TLongHashSet skipTiles,
			SearchRequest<Amenity> req, PoiRegion region, QuadTree<Void> nameIndexTree) throws IOException {
		req.numberOfReadSubtrees++;
		int zoomToSkip = req.zoom == -1 ? 31 : req.zoom + ZOOM_TO_SKIP_FILTER_READ;
		boolean checkBox = true;
		boolean existsCategories = false;
		int zoom = pzoom;
		int dy = py;
		int dx = px;
		while (true) {
			if (req.isCancelled()) {
				return false;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
			case 0:
				return existsCategories;
			case OsmandOdb.OsmAndPoiBox.ZOOM_FIELD_NUMBER:
				zoom = codedIS.readUInt32() + pzoom;
				break;
			case OsmandOdb.OsmAndPoiBox.LEFT_FIELD_NUMBER:
				dx = codedIS.readSInt32();
				break;
			case OsmandOdb.OsmAndPoiBox.TOP_FIELD_NUMBER:
				dy = codedIS.readSInt32();
				break;
			case OsmandOdb.OsmAndPoiBox.CATEGORIES_FIELD_NUMBER:
				if (req.poiTypeFilter == null) {
					skipUnknownField(t);
				} else {
					int length = codedIS.readRawVarint32();
					long oldLimit = codedIS.pushLimitLong((long) length);
					boolean check = checkCategories(req, region);
					codedIS.popLimit(oldLimit);
					if (!check) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return false;
					}
					existsCategories = true;
				}
				break;
			case OsmandOdb.OsmAndPoiBox.TAGGROUPS_FIELD_NUMBER:
				PoiRegion.MAP_HAS_TAG_GROUPS = true;
				int tagGroupLength = codedIS.readRawVarint32();
				long old = codedIS.pushLimitLong((long) tagGroupLength);
				readTagGroups(region.tagGroups, req);
				codedIS.popLimit(old);
				break;
			case OsmandOdb.OsmAndPoiBox.SUBBOXES_FIELD_NUMBER: {
				int x = dx + (px << (zoom - pzoom));
				int y = dy + (py << (zoom - pzoom));
				if (checkBox) {
					int xL = x << (31 - zoom);
					int xR = ((x + 1) << (31 - zoom)) - 1;
					int yT = y << (31 - zoom);
					int yB = ((y + 1) << (31 - zoom)) - 1;

					boolean intersectWithNameIndex = false;
					QuadRect rect = new QuadRect(xL, yT, xR, yB);
					if (PoiRegion.MAP_HAS_TAG_GROUPS && nameIndexTree != null) {
						List<Void> resCache = new ArrayList<>();
						region.bboxIndexCache.queryInBox(rect, resCache);
						if (resCache.size() == 0) {
							List<Void> res = new ArrayList<>();
							nameIndexTree.queryInBox(rect, res);
							intersectWithNameIndex = res.size() > 0;
						}
					}
					// check intersection
					if ((left31 > xR || xL > right31 || bottom31 < yT || yB < top31) && !intersectWithNameIndex) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return false;
					}
					req.numberOfAcceptedSubtrees++;
					checkBox = false;
					region.bboxIndexCache.insert(null, rect);
				}

				long length = readInt();
				long oldLimit = codedIS.pushLimitLong((long) length);
				boolean exists = readBoxField(left31, right31, top31, bottom31, x, y, zoom, offsetsMap, skipTiles, req, region, nameIndexTree);
				codedIS.popLimit(oldLimit);

				if (skipTiles != null && zoom >= zoomToSkip && exists) {
					long val = ((((long) x) >> (zoom - zoomToSkip)) << zoomToSkip) | (((long) y) >> (zoom - zoomToSkip));
					if (skipTiles.contains(val)) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit());
						return true;
					}
				}
			}
			break;
			case OsmandOdb.OsmAndPoiBox.SHIFTTODATA_FIELD_NUMBER: {
				int x = dx + (px << (zoom - pzoom));
				int y = dy + (py << (zoom - pzoom));
				boolean read = true;
				if (req.tiles != null) {
					long zx = x << (SearchRequest.ZOOM_TO_SEARCH_POI - zoom);
					long zy = y << (SearchRequest.ZOOM_TO_SEARCH_POI - zoom);
					read = req.tiles.contains((zx << SearchRequest.ZOOM_TO_SEARCH_POI) + zy);
				}
				long l = readInt();
				if(l > Integer.MAX_VALUE) {
					throw new IllegalStateException();
				}
				int offset = (int) l;
				if (read) {
					if (skipTiles != null && zoom >= zoomToSkip) {
						long valSkip = ((((long) x) >> (zoom - zoomToSkip)) << zoomToSkip)
								| (((long) y) >> (zoom - zoomToSkip));
						offsetsMap.put(offset, valSkip);
						skipTiles.add(valSkip);
					} else {
						offsetsMap.put(offset, -1);
					}
				}
			}
			break;
			default:
				skipUnknownField(t);
				break;
			}
		}
	}

	private void readTagGroups(Map<Integer, List<TagValuePair>> tagGroups, SearchRequest<Amenity> req) throws IOException {
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
				case 0:
					return;
				case OsmandOdb.OsmAndPoiTagGroups.GROUPS_FIELD_NUMBER:
					int length = codedIS.readRawVarint32();
					long oldLimit = codedIS.pushLimitLong((long) length);
					readTagGroup(tagGroups, req);
					codedIS.popLimit(oldLimit);
					break;
				default:
					skipUnknownField(t);
					break;
			}
		}
	}

	private void readTagGroup(Map<Integer, List<TagValuePair>> tagGroups, SearchRequest<Amenity> req) throws IOException {
		List<String> tagValues = new ArrayList<>();
		int id = -1;
		while (true) {
			if (req.isCancelled()) {
				return;
			}
			int t = codedIS.readTag();
			int tag = WireFormat.getTagFieldNumber(t);
			switch (tag) {
				case 0:
					if (id > 0 && tagValues.size() > 1 && tagValues.size() % 2 == 0) {
						List<TagValuePair> tagValuePairs = new ArrayList<>();
						for (int i = 0; i < tagValues.size(); i = i + 2) {
							tagValuePairs.add(new TagValuePair(tagValues.get(i), tagValues.get(i + 1), -1));
						}
						tagGroups.put(id, tagValuePairs);
					}
					return;
				case OsmandOdb.OsmAndPoiTagGroup.ID_FIELD_NUMBER:
					id = codedIS.readUInt32();
					break;
				case OsmandOdb.OsmAndPoiTagGroup.TAGVALUES_FIELD_NUMBER:
					tagValues.add(codedIS.readString().intern());
					break;
				default:
					skipUnknownField(t);
					break;
			}
		}
	}
}
