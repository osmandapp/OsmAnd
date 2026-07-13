package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiAdditionalFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchFileCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class SpatialPoiSearch {

	final MapPoiTypes poiTypes;
	StringPrefixTree<SpatialPoiType> poiTypesIndex = new StringPrefixTree<>();
	ReentrantReadWriteLock poiTypesIndexLock = new ReentrantReadWriteLock();
	AtomicInteger ids = new AtomicInteger();
	Map<String, SpatialPoiType> byKey = new ConcurrentHashMap<>();
	Map<Integer, SpatialPoiType> byId = new ConcurrentHashMap<>();
	
	public static class SpatialPoiType {
		public final AbstractPoiType singleType;
		public final String poiAdditional;
		final List<String> names = new ArrayList<String>();
		final String key;
		final int id;
		List<AbstractPoiType> parentTypes;

		public SpatialPoiType(AbstractPoiType pt, int id) {
			this.singleType = pt;
			this.key = pt.getKeyName();
			this.id = id;
			this.poiAdditional = null;
		}
		
		public SpatialPoiType(String additional, String key, int id) {
			this.singleType = null;
			this.key = key;
			this.id = id;
			this.poiAdditional = additional;
		}

		public String getKey() {
			return key;
		}

		public List<AbstractPoiType> getParentTypes() {
			return parentTypes;
		}

		public boolean accept(Amenity a) {
			if (key.equals(a.getType().getKeyName())) {
				return true;
			}
			// contains for ';'
			boolean multi = a.getSubType().indexOf(';') != -1;
			if (key.equals(a.getSubType()) 
					|| (multi && key.startsWith(a.getSubType() + ";"))
					|| (multi && key.endsWith(";" + a.getSubType()))
					|| (multi && key.contains(";" + a.getSubType() + ";"))) {
				return true;
			}
			return false;
		}
		
	}

	public SpatialPoiSearch(MapPoiTypes types) {
		this.poiTypes = types;
		for (PoiCategory pc : poiTypes.getCategories()) {
			if (pc == poiTypes.getOtherMapCategory()) {
				continue;
			}
			addToIndex(pc, null);
			for (PoiFilter pt : pc.getPoiFilters()) {
				if (pt.isTopVisible()) {
					addToIndex(pt, null);
				}
			}
			for (PoiType pt : pc.getPoiTypes()) {
				if (pt.isReference()) {
					continue;
				}
				addToIndex(pt, null);
				for (PoiType add : pt.getPoiAdditionals()) {
					if (add.isTopVisible() && !"no".equals(poiTypes.getBasePoiName(add))) {
						addToIndex(add, pt);
					}
				}
			}
		}
	}


	private void addToIndex(AbstractPoiType pt, PoiType parent) {
		if (byKey.containsKey(pt.getKeyName())) {
			if (pt.isAdditional()) {
				byKey.get(pt.getKeyName()).parentTypes.add(parent); 
				return;
			} else {
				throw new IllegalStateException();
			}
		}
		String basePoiName = poiTypes.getBasePoiName(pt);
		SpatialPoiType poiType = new SpatialPoiType(pt, ids.getAndIncrement());
		if (parent != null) {
			poiType.parentTypes = new ArrayList<>();
			poiType.parentTypes.add(parent);
		}
		if (!basePoiName.equals(pt.getTranslation())) {
			String[] split = pt.getTranslation().split(";");
			for (String tr : split) {
				poiType.names.add(SearchAlgorithms.alignChars(tr.trim()));
			}
		}
		addToIndex(basePoiName, poiType);
	}


	private void addToIndex(String basePoiName, SpatialPoiType poiType) {
		poiType.names.add(basePoiName);
		WriteLock wl = poiTypesIndexLock.writeLock();
		try {
			wl.lock();
			SpatialSearchContext.checkPoiTypeId(poiType.id);
			byId.put(poiType.id, poiType);
			byKey.put(poiType.key, poiType);
			for (String name : poiType.names) {
				poiTypesIndex.put(name, poiType);
			}
		} finally {
			wl.unlock();
		}
	}

	public void init(SpatialSearchGlobalCache cache, SpatialSearchFileCache fc, BinaryMapIndexReader bir,
			PoiRegion poiRegion) {
		List<String> cats = poiRegion.getCategories();
		List<List<String>> subcategories = poiRegion.getSubcategories();
		TIntArrayList categoryFreqs = poiRegion.getCategoryFreqs();
		List<TIntArrayList> subcatFreqs = poiRegion.getSubcategoryFreqs();
		for (int i = 0; i < cats.size(); i++) {
			List<String> lst = subcategories.get(i);
			int f = categoryFreqs != null && i < categoryFreqs.size() ? categoryFreqs.get(i) : 0;
			fc.poiFrequencies.put(cats.get(i), f);
			for (int j = 0; j < lst.size(); j++) {
				int ft = subcatFreqs != null && i < subcatFreqs.size() && j < subcatFreqs.get(i).size() ? subcatFreqs.get(i).get(j) : 0;
				String vkey = lst.get(j);
				fc.poiFrequencies.put(vkey, ft);
			}
		}
		
		
		for (PoiSubType subType : poiRegion.getSubTypes()) {
			if (subType.text) {
				continue;
			}
			if (subType.isTopIndex()) {
				List<String> possibleValues = subType.possibleValues;
				for (int k = 0; k < possibleValues.size(); k++) {
					String topValueName = possibleValues.get(k);
					String valueKey = TopIndexFilter.getValueKey(topValueName);
					String fullKey = subType.name + "_" + valueKey;
					SpatialPoiType topValue = byKey.get(fullKey);
					if (topValue == null) {
						String poiTranslation = poiTypes.getPoiTranslation(valueKey, false);
						topValue = new SpatialPoiType(topValueName, fullKey, ids.getAndIncrement());
						if (!topValueName.equalsIgnoreCase(poiTranslation) && poiTranslation != null) {
							topValue.names.add(poiTranslation);
						}
						addToIndex(topValueName, topValue);
					}
					int freq = subType.possibleValuesFreqs != null && k < subType.possibleValuesFreqs.size() ? subType.possibleValuesFreqs.get(k) : 0;
					Integer fit = fc.poiFrequencies.get(topValue.key);
					if (fit != null) {
						freq += fit;
					}
					fc.poiFrequencies.put(topValue.key, freq);
				}
			}
			SpatialPoiType indSubType = byKey.get(subType.name);
			if (indSubType == null) {
				// skip top level additional
				continue;
			}
			fc.poiFrequencies.put(indSubType.key, subType.frequency);
		}
	}

	private record PoiCatSearch(SpatialPoiType pt, List<SpatialSearchToken> tokens, List<NameIndexAtom> atoms, int freq) implements Comparable<PoiCatSearch> {

		@Override
		public int compareTo(PoiCatSearch o) {
			int i1 = -Integer.compare(tokens.size(), o.tokens.size());
			if (i1 != 0) {
				return i1;
			}
			return -Integer.compare(freq, o.freq);
		}
	}
	
	public void processPoiCategories(SpatialSearchContext ctx, List<SpatialSearchToken> tokens) {
		Map<SpatialPoiType, PoiCatSearch> res = new LinkedHashMap<>();
		for (SpatialSearchToken t : tokens) {
			ReadLock readLock = poiTypesIndexLock.readLock();
			List<SpatialPoiType> poiTypes;
			try {
				readLock.lock();
				poiTypes = poiTypesIndex.match(t.getPrefixMatcher(ctx.stats));
			} finally {
				readLock.unlock();
			}
			for (SpatialPoiType a : poiTypes) {
				boolean match = false;
				for (String n : a.names) {
					if (t.getMainCollator().matches(n)) {
						match = true;
						break;
					}
				}
				if (match) {
					int total = 0;
					for (SpatialSearchFileCache l : ctx.internalFile) {
						if (l.poiFrequencies != null) {
							Integer freq = l.poiFrequencies.get(a.key);
							if (freq != null) {
								total += freq;
							}
							if (a.singleType instanceof PoiFilter pf) {
								for (PoiType p : pf.getPoiTypes()) {
									freq = l.poiFrequencies.get(p.getKeyName());
									if (freq != null) {
										total += freq;
									}
								}
							}
							// additional could be on top
//							if (a.parentTypes != null) {
//								for (AbstractPoiType p : a.parentTypes) {
//									freq = l.poiFrequencies.get(p.getKeyName());
//									if (freq != null) {
//										total += freq;
//									}
//								}
//							}
						}
					}
//					System.out.println(a.names + " " + a.key + " " + total);
					PoiCatSearch cs = res.get(a);
					if (cs == null) {
						cs = new PoiCatSearch(a, new ArrayList<>(), new ArrayList<>(), total);
						res.put(a, cs);
					}
					if (cs.tokens.contains(t)) {
						continue;
					}
					NameIndexAtomXY xy = new NameIndexAtomXY(null, null, null);
					NameIndexAtom atom = new NameIndexAtom(a.names.get(0), SpatialSearchToken.POI_CATEGORY_TYPE, 
							a.id, 0, null, false, -total, total, xy, 0);
					cs.atoms.add(atom);
					cs.tokens.add(t);
				}
			}
		}
		
		List<PoiCatSearch> finalRes = new ArrayList<>(res.values());
		Collections.sort(finalRes);
		if (finalRes.size() > ctx.settings.LIMIT_POI_CATEGORY_BY_FREQ) {
			finalRes = finalRes.subList(0, ctx.settings.LIMIT_POI_CATEGORY_BY_FREQ);
		}
		for (PoiCatSearch pc : finalRes) {
			for (int i = 0; i < pc.tokens.size(); i++) {
				pc.tokens.get(i).addAtom(pc.atoms.get(i));
			}
		}
	}
	
	public SpatialPoiType getById(int id) {
		return byId.get(id);
	}
	
	public SpatialPoiType getByKey(String key) {
		return byKey.get(key);
	}


	private List<BinaryMapIndexReader> filterByRadius(LatLon l, int rad, List<BinaryMapIndexReader> oFiles,
			List<BinaryMapIndexReader> res) {
		QuadRect rect = MapUtils.calculateBbox(rad, l);
		Iterator<BinaryMapIndexReader> it = oFiles.iterator();
		while (it.hasNext()) {
			BinaryMapIndexReader next = it.next();
			if (next.containsPoiData((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom)) {
				res.add(next);
				it.remove();
			}
		}
		return res;
	}


	public List<Amenity> loadPOIObjects(SpatialSearchContext ctx, long id, LatLon latLon, int radMeters, int limit)
			throws IOException {
		final SpatialPoiType spt = byId.get((int) id);
		List<Amenity> results = new ArrayList<Amenity>();
		int[] alimit = new int[] { limit };
		if (spt != null && ctx.files != null) {
			SearchPoiTypeFilter typeFilter = spt.poiAdditional != null ? null : new SearchPoiTypeFilter() {

				@Override
				public boolean accept(PoiCategory type, String subcategory) {
					if (spt.key.equals(type.getKeyName()) || spt.key.equals(subcategory)) {
						return true;
					}
					if (spt.parentTypes != null) {
						for (AbstractPoiType a : spt.parentTypes) {
							if (a.getKeyName().equals(type.getKeyName()) || a.getKeyName().equals(subcategory)) {
								return true;
							}
						}
					}
					return false;
				}

				@Override
				public boolean isEmpty() {
					return false;
				}
			};
			SearchPoiAdditionalFilter addFilter = spt.poiAdditional == null ? null : new SearchPoiAdditionalFilter() {

				@Override
				public String getName() {
					return spt.names.get(0);
				}

				@Override
				public String getIconResource() {
					return null;
				}

				@Override
				public boolean accept(PoiSubType poiSubType, String value) {
//					spt.key.startsWith(poiSubType.name)
					if (spt.poiAdditional.equals(value)) {
						return true;
					}
					return false;
				}
			};
			ResultMatcher<Amenity> matcher = new ResultMatcher<Amenity>() {

				@Override
				public boolean publish(Amenity object) {
					if (spt.parentTypes != null) {
						boolean match = object.getAdditionalInfo(spt.key) != null;
						if (!match) {
							return false;
						}
					}
					if (alimit[0] > 0) {
						alimit[0]--;
					}
					results.add(object);
					return false;
				}

				@Override
				public boolean isCancelled() {
					return false; // allow to read all for proper sorting
//					return alimit[0] == 0;
				}
			};
			SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
					0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, -1, typeFilter, addFilter, matcher);
			if (latLon != null) {
				QuadRect qr = MapUtils.calculateBbox(radMeters, latLon);
				req = BinaryMapIndexReader.buildSearchPoiRequest((int) qr.left, (int) qr.right, (int) qr.top,
						(int) qr.bottom, -1, typeFilter, addFilter, matcher);
			}
			List<BinaryMapIndexReader> oFiles = new LinkedList<BinaryMapIndexReader>(ctx.files);
			iterateSearch(ctx, req, filterByRadius(latLon, 5_000, oFiles, new ArrayList<BinaryMapIndexReader>()));
			if (alimit[0] != 0 && radMeters > 5_000) {
				iterateSearch(ctx, req, filterByRadius(latLon, 50_000, oFiles, new ArrayList<BinaryMapIndexReader>()));
			}
			if (alimit[0] != 0 && radMeters > 50_000) {
				iterateSearch(ctx, req, oFiles);
			}
		}
		return results;
	}


	private void iterateSearch(SpatialSearchContext ctx, SearchRequest<Amenity> req, List<BinaryMapIndexReader> res)
			throws IOException {
		for (BinaryMapIndexReader bir : res) {
			ctx.stats.poiByTypeTime.start();
			long br = bir.getBytesRead();
			bir.searchPoi(req);
			ctx.stats.poiByTypeBytes += (bir.getBytesRead() - br);
			ctx.stats.poiByTypeTime.finish();
			ctx.stats.poiByTypeBboxes += req.numberOfReadSubtrees;
		}
	}

}
