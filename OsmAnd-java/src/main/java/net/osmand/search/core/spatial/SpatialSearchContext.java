package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.NameIndexReader.ValueFreq;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtom;
import net.osmand.search.core.spatial.SpatialSearchToken.NameIndexAtomXY;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchFileCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchGlobalCache;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;
import net.osmand.util.SearchAlgorithms;

public class SpatialSearchContext {

	private static int SHIFT_FILE_IND = 12; // maximum files 4096
	private static int SHIFT_POI_IND = 10; // maximum poi 1024

	final List<BinaryMapIndexReader> files;
	final List<SpatialSearchFileCache> internalFile = new ArrayList<>();
	final LatLon location; // could be null

	SpatialSearchStats stats = new SpatialSearchStats();

	public static class SpatialSearchStats {
		long time = System.nanoTime();
		long readTokensTime = 0;
		long readObjTime = 0;
		long computeTime = 0;
		long matchTime = 0;
		long atoms = 0;

		@Override
		public String toString() {
			return String.format(
					"Search Stats %.1f ms - read %.1f ms atoms (tokens %.1f ms, obj %.1f ms), match %.1f ms, comp %.1f ms",
					time / 1e6, atoms / 1e6, readTokensTime / 1e6, readObjTime / 1e6, matchTime / 1e6, computeTime / 1e6);
		}

		public void finish() {
			time = System.nanoTime() - time;
		}
	}

	public SpatialSearchContext(List<BinaryMapIndexReader> files, LatLon location) {
		this.files = files;
		this.location = location;
	}
	
	public LatLon getLocation() {
		return location;
	}

	public void initFiles(SpatialSearchGlobalCache cache) {
		int indexInd = 0;
		int fileInd = 0;
		for (BinaryMapIndexReader bir : files) {
			SpatialSearchFileCache fc = cache.filesCache.get(bir.getFile().getName());
			if (fc == null || !fc.test(bir)) {
				fc = new SpatialSearchFileCache(bir);
			}
			cache.filesCache.put(fc.file, fc);
			fc.indexInd = indexInd;
			fc.fileInd = fileInd;
			this.internalFile.add(fc);
			indexInd += fc.indexReaders.size();
			fileInd++;
		}
	}

	void readAtoms(List<SpatialSearchToken> tokens) throws IOException {
		int indxInd = 0;
		for (int fileInd = 0; fileInd < files.size(); fileInd++) {
			SpatialSearchFileCache iCache = internalFile.get(fileInd);
			BinaryMapIndexReader b = files.get(fileInd);
			for (NameIndexReader indx : iCache.indexReaders) {
				readAtoms(tokens, b, indx, indxInd);
				indxInd++;
			}
		}
	}
	
	private record ReadTokens(boolean init, boolean readCommonTokens, boolean readFreqTokens) {
		
	}
	
	private ReadTokens computeReadTokens(List<SpatialSearchToken> tokens, NameIndexReader indx) {
		Map<String, ValueFreq> frequentWords = indx.getCommonWordsStats();
		boolean readCommonTokens = true;
		boolean readFreqTokens = true;
		if (frequentWords != null) {
			for (SpatialSearchToken t : tokens) {
				boolean number2Letters = SearchAlgorithms.isNumber2Letters(t.word);
				if (number2Letters) {
					continue;
				}
				ValueFreq freqWord = frequentWords.get(t.word);
				if (freqWord == null) {
					// special case token "2" could match "2-nd" atom
					// rare word
					if (!SpatialTextSearchSettings.ALWAYS_READ_COMMON_WORDS_ATOMS) {
						readCommonTokens = false;
					}
					if (!SpatialTextSearchSettings.ALWAYS_READ_FREQ_WORDS_ATOMS) {
						readFreqTokens = false;
					}
				} else {
					int nonIndexed = (int) (freqWord.freq - freqWord.extra);
					if (nonIndexed == 0) {
						// frequent word is ok to specialize
						if (!SpatialTextSearchSettings.ALWAYS_READ_COMMON_WORDS_ATOMS) {
							readCommonTokens = false;
						}
					}
				}
			}
		}
		return new ReadTokens(frequentWords != null, readCommonTokens, readFreqTokens);
	}

	private void readAtoms(List<SpatialSearchToken> tokens, BinaryMapIndexReader b, NameIndexReader indx, int indxInd)
			throws IOException {
		ReadTokens read = computeReadTokens(tokens, indx);
		for (SpatialSearchToken t : tokens) {
			Map<String, ValueFreq> frequentWords = indx.getCommonWordsStats();
			if (!read.init && frequentWords != null) {
				read = computeReadTokens(tokens, indx);
			}
			boolean number2Letters = SearchAlgorithms.isNumber2Letters(t.word);
			// always search numbers as they could be very specific - "2" token could match "2-nd" atom
			if (!number2Letters && !read.readFreqTokens) {
				ValueFreq freqWord = frequentWords.get(t.word);
				if (freqWord != null) {
					continue;
				}
			} else if (!number2Letters && !read.readCommonTokens) {
				ValueFreq freqWord = frequentWords.get(t.word);
				// non indexed > 0 common
				if (freqWord != null && freqWord.freq - freqWord.extra > 0) {
					continue;
				}
			}
			List<PrefixNameValue> matchedPrefixes = indx.getMatchedPrefixes(t.word);
			if (matchedPrefixes == null) {
				stats.readTokensTime -= System.nanoTime();
				b.readFullNameIndex(indx, t.word);
				matchedPrefixes = indx.getMatchedPrefixes(t.word);
				stats.readTokensTime += System.nanoTime();
			}
			for (PrefixNameValue prefix : matchedPrefixes) {
				parseAtomSuffixes(t, indxInd, indx, prefix, tokens);
			}
		}
	}

	private long makeAddrId(int fileInd, long shiftToIndex) {
		if (fileInd > 1 << SHIFT_FILE_IND) {
			throw new IllegalStateException();
		}
		long id = (shiftToIndex << SHIFT_FILE_IND) + fileInd;
		return id;
	}
	
	private long makePoiId(int fileInd, long shiftToIndex, int poiInd) {
		if (fileInd > 1 << SHIFT_FILE_IND) {
			throw new IllegalStateException();
		}
		if (poiInd > 1 << SHIFT_POI_IND) {
			throw new IllegalStateException();
		}
		long id = (((shiftToIndex << SHIFT_POI_IND) + poiInd) << SHIFT_FILE_IND) + fileInd;
		return id;
	}

	private void parseAtomSuffixes(SpatialSearchToken t, int indInd, NameIndexReader indx, PrefixNameValue prefix,
			List<SpatialSearchToken> allTokens) throws IOException {
		String curSuffix = null;
		List<String> suffixes = new ArrayList<>();
		List<String> commonSuffixes = new ArrayList<>();
		boolean addr = prefix.addr != null;
		for (String s : addr ? prefix.addr.getSuffixesDictionaryList() : prefix.poi.getSuffixesDictionaryList()) {
			curSuffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s);
			suffixes.add(prefix.key + curSuffix);
		}
		for (Integer i : addr ? prefix.addr.getSuffixesCommonDictionaryList()
				: prefix.poi.getSuffixesCommonDictionaryList()) {
			commonSuffixes.add(indx.getCommonIndexed(i));
		}
		if (addr && SpatialTextSearchSettings.SEARCH_ADDR) {
			for (AddressNameIndexDataAtom a : prefix.addr.getAtomList()) {
				long lid = makeAddrId(indInd, prefix.shift - a.getShiftToIndex(0));
				long pid = 0;
				if (a.getType() == CityBlocks.STREET_TYPE.index) {
					pid = makeAddrId(indInd, prefix.shift - a.getShiftToCityIndex(0));
				} else if (a.getType() != CityBlocks.BOUNDARY_TYPE.index && a.getType() != CityBlocks.CITY_TOWN_TYPE.index
						&& a.getType() != CityBlocks.VILLAGES_TYPE.index && a.getType() != CityBlocks.POSTCODES_TYPE.index) {
					continue;
				}
				MapObject obj = null;
				if (SpatialTextSearchSettings.READ_ADDR_OBJECTS) {
					obj = readAddrObject(lid, pid, null);
				}
				parseSuffixes(t, suffixes, commonSuffixes, a, null, lid, pid, obj, allTokens);
			}
		} else if (!addr && SpatialTextSearchSettings.SEARCH_POI) {
			for (OsmAndPoiNameIndexDataAtom a : prefix.poi.getAtomsList()) {
				if (a.getPoiIndInBlockCount() == 0) {
					// intermediate version ignore
					continue;
				}
				long lid = makePoiId(indInd, BinaryMapIndexReader.convertFixed32ToRef(a.getShiftTo()),
						a.getPoiIndInBlock(0));
				MapObject amenity = null;
				if (SpatialTextSearchSettings.READ_POI_OBJECTS) {
					amenity = readPoiObject(lid, null);
				}
				parseSuffixes(t, suffixes, commonSuffixes, null, a, lid, 0, amenity, allTokens);
			}
		}
	}
	
	public MapObject readPoiObject(long id, TLongObjectHashMap<MapObject> cache) throws IOException {
		if (cache != null) {
			MapObject mapObject = cache.get(id);
			if (mapObject != null) {
				return mapObject;
			}
		}
		long oid = id;
		int indInd = (int) (id & ((1l << SHIFT_FILE_IND) - 1));
		id >>= SHIFT_FILE_IND;
		int poiInd = (int) (id & ((1l << SHIFT_POI_IND) - 1));
		id >>= SHIFT_POI_IND;
		long shift = id;

		NameIndexReader nameIndex = null;
		SpatialSearchFileCache c = null;
		for (int k = 0; k < internalFile.size(); k++) {
			c = internalFile.get(k);
			if (indInd < c.indexInd + c.indexReaders.size()) {
				nameIndex = c.indexReaders.get(indInd - c.indexInd);
				break;
			}
		}

		long tm = System.nanoTime();
		List<Amenity> lst = files.get(c.fileInd).readAmenityBlock(nameIndex.poiRegion, shift, poiInd);
		if (cache != null) {
			long ofirstid = oid - (poiInd << SHIFT_FILE_IND);
			for (int i = 0; i < lst.size(); i++) {
				cache.put(ofirstid + (i << SHIFT_FILE_IND), lst.get(i));
			}
		}
		MapObject amenity = lst.get(poiInd);
		stats.readObjTime += (System.nanoTime() - tm);
		return amenity;
	}

	public MapObject readAddrObject(long id, long pid, TLongObjectHashMap<MapObject> cache) throws IOException {
		if (cache != null) {
			MapObject obj = cache.get(id);
			if (obj != null) {
				return obj;
			}
		}
		long opid = pid;
		int indInd = (int) (id & ((1l << SHIFT_FILE_IND) - 1));
		id >>= SHIFT_FILE_IND;
		long shift = id;
		
		NameIndexReader nameIndex = null;
		SpatialSearchFileCache c = null;
		for (int k = 0; k < internalFile.size(); k++) {
			c = internalFile.get(k);
			if (indInd < c.indexInd + c.indexReaders.size()) {
				nameIndex = c.indexReaders.get(indInd - c.indexInd);
				break;
			}
		}		
		
		long tm = System.nanoTime();
		MapObject obj;
		if (pid != 0) {
			int pIndInd = (int) (pid & ((1l << SHIFT_FILE_IND) - 1));
			pid >>= SHIFT_FILE_IND;
			long pshift = pid;
			if (pIndInd != indInd) {
				throw new UnsupportedOperationException();
			}
			City city = null;
			if (cache != null) {
				city = (City) cache.get(opid);
			}
			if (city == null) {
				city = files.get(c.fileInd).readCityObject(nameIndex.addressRegion, pshift);
			}
			obj = files.get(c.fileInd).readStreetObject(nameIndex.addressRegion, city, shift);
		} else  {
			obj = files.get(c.fileInd).readCityObject(nameIndex.addressRegion, shift);
		}
		stats.readObjTime += (System.nanoTime() - tm);
		return obj;
	}

	private void parseSuffixes(SpatialSearchToken t, List<String> suffixes, List<String> commonSuffixes,
			AddressNameIndexDataAtom a, OsmAndPoiNameIndexDataAtom b, long cid, long pid, MapObject obj,
			List<SpatialSearchToken> allTokens) {
		int cnt = a != null ? a.getSuffixesBitsetIndexCount() : b.getSuffixesBitsetIndexCount();
		String name = "";
		int wInd = 0;
		int type = a != null ? a.getType() : SpatialSearchToken.POI_TYPE;
		for (int i = 0; i < cnt; i++) {
			int suffBit = a != null ? a.getSuffixesBitsetIndex(i) : b.getSuffixesBitsetIndex(i);
			if (suffBit % 2 == 0) {
				int ind = suffBit / 2 - 1;
				if (ind == -1) {
					if (acceptName(t, name)) {
						int other;
						if (a != null) {
							other = wInd < a.getOtherWordsCountCount() ? a.getOtherWordsCount(wInd) : 0;
						} else {
							other = wInd < b.getOtherWordsCountCount() ? b.getOtherWordsCount(wInd) : 0;
						}
						addObject(t, name, type, cid, pid, obj, other, new NameIndexAtomXY(a, b), allTokens);
					}
					wInd++;
					name = "";
				} else if (ind < suffixes.size()) {
					name += suffixes.get(ind);
				} else {
					// common suffix
					name += " " + commonSuffixes.get(ind - suffixes.size());
				}
			} else {
				if (suffBit % 4 == 1) {
					// separated number
					name += " " + (suffBit >> 2);
				} else {
					// partial
					name += (suffBit >> 2);
				}
			}
		}
		if (name.length() != 0 && acceptName(t, name)) {
			int other;
			if (a != null) {
				other = wInd < a.getOtherWordsCountCount() ? a.getOtherWordsCount(wInd) : 0;
			} else {
				other = wInd < b.getOtherWordsCountCount() ? b.getOtherWordsCount(wInd) : 0;
			}
			addObject(t, name, type, cid, pid, obj, other, new NameIndexAtomXY(a, b), allTokens);
		}
	}

	private boolean acceptName(SpatialSearchToken t, String name) {
		stats.matchTime -= System.nanoTime();
		boolean acceptName = t.acceptName(name);
		stats.matchTime += System.nanoTime();
		return acceptName;
	}

	private void addObject(SpatialSearchToken t, String name, int type, long lid, long pid, MapObject obj, int other,
			NameIndexAtomXY coords, List<SpatialSearchToken> allTokens) {
		List<SpatialSearchToken> otherTokens = null;
		if (name.indexOf(' ') != -1) {
			List<String> split = SearchAlgorithms.splitAndNormalize(name, false);
			for (int k = 1; k < split.size(); k++) {
				boolean matched = false;
				for (SpatialSearchToken token : allTokens) {
					if (t != token && acceptName(token, name)
							&& (otherTokens == null || !otherTokens.contains(token))) {
						if (otherTokens == null) {
							otherTokens = new ArrayList<>();
						}
						otherTokens.add(token);
						matched = true;
						break;
					}
				}
				if (!matched) {
					other++;
				}
			}
		}
		if (otherTokens != null) {
			other += otherTokens.size();
		}
		NameIndexAtom atom = new NameIndexAtom(name, type, lid, pid, obj, other, coords);
		t.addAtom(atom);
		if (otherTokens != null) {
			for (SpatialSearchToken token : otherTokens) {
				token.addAtom(atom);
			}
		}

	}



}