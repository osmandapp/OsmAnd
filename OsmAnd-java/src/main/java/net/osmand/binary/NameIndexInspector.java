package net.osmand.binary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.map.hash.TLongObjectHashMap;
import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.data.City;
import net.osmand.data.QuadRect;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

public class NameIndexInspector {

	public Map<Long, PrefixNameValue> indexByRef = new HashMap<>();
	private long initialShift;
	
	private SuffixesStat suffixesStat = new SuffixesStat();
	private StreetsIndexStat streetsStat = new StreetsIndexStat();
	private BoundariesIndexStat bndsStat = new BoundariesIndexStat();
	

	public void setInitialShift(long totalBytesRead) {
		this.initialShift = totalBytesRead;
	}
	
	public static class BoundariesIndexStat {
		
		Map<String, ValueFreq> bnds = new HashMap<>();
		
		TLongObjectHashMap<QuadRect> rects = new TLongObjectHashMap<QuadRect>();
		
		public Map<String, ValueFreq> getBoundaries() {
			return bnds;
		}
		
		public void registerBoundaries(List<City> cities) {
			for (City c : cities) {
				long oid = ObfConstants.getOsmObjectId(c);
				int[] bbox31 = c.getBbox31();
				if (bbox31 == null) {
					continue;
				}
				rects.put(oid, new QuadRect(bbox31[0], bbox31[1], bbox31[2], bbox31[3]));
				addSubValue(ValueFreq.get(bnds, c.getName(), 0), oid);
				for (String s : c.getOtherNames()) {
					addSubValue(ValueFreq.get(bnds, s, 0), oid);
				}
			}
		}

		private void addSubValue(ValueFreq mainWord, long oid) {
			if (mainWord.subValues == null) {
				mainWord.subValues = new ArrayList<>();
			}
			for (ValueFreq v : mainWord.subValues) {
				if (v.extra == oid) {
					v.freq++;
					return;
				}
			}
			ValueFreq v = new ValueFreq(mainWord.value, 1);
			v.extra = oid;
			mainWord.subValues.add(v);
			mainWord.freq++;
		}
		
		public int calculateNumberOfDistinctBBox(List<ValueFreq> vls) {
			if(vls.size() < 2) {
				return vls.size();
			}
			List<List<QuadRect>> groups = new ArrayList<List<QuadRect>>();
			int KM15_IN31Z = 1_000_000;
			for (int i = 0; i < vls.size(); i++) {
				QuadRect q = rects.get(vls.get(i).extra);
				q.inset(-Math.max(KM15_IN31Z, q.width() / 6), -Math.max(KM15_IN31Z, q.height() / 6));
				List<QuadRect> group = null;
				main: for (List<QuadRect> gr : groups) {
					for (QuadRect r : gr) {
						if (QuadRect.trivialOverlap(r, q)) {
							group = gr;
							break main;
						}
					}
				}
				if (group == null) {
					group = new ArrayList<QuadRect>();
					groups.add(group);
				}
				group.add(q);
			}
			return groups.size();
		}

		public void merge(BoundariesIndexStat bndsStat) {
			rects.putAll(bndsStat.rects);
			for (ValueFreq from : bndsStat.bnds.values()) {
				ValueFreq to = bnds.get(from.value);
				if (to == null) {
					bnds.put(from.value, from);
				} else {
					for (ValueFreq fromSub : from.subValues) {
						ValueFreq toSub = null;
						for (ValueFreq t : from.subValues) {
							if (t.extra == fromSub.extra) {
								toSub = t;
								toSub.freq += fromSub.freq;
								break;
							}
						}
						if (toSub == null) {
							to.subValues.add(fromSub);
							to.freq++;
						}
					}
				}
			}
		}

	}
	
	public static class StreetsIndexStat {

		Map<String, ValueFreq> values = new HashMap<>();
		
		public void merge(StreetsIndexStat streetsStat) {
			ValueFreq.mergeArray(values, streetsStat.values);
		}

		public void processAtom(ValueFreq v, ValueFreq sPref, AddressNameIndexDataAtom a) {
			if (a.getType() == CityBlocks.STREET_TYPE.index) {
				sPref.extra++;
				v.extra++;
				sPref.freq++;
				v.freq++;
			} else if (a.getEnclosingObjects() > 0) {
				sPref.enclosing += a.getEnclosingObjects();
				v.enclosing += a.getEnclosingObjects();
				sPref.freq += a.getEnclosingObjects();
				v.freq += a.getEnclosingObjects();
				sPref.maxSingleAtomEnc = Math.max(sPref.maxSingleAtomEnc, a.getEnclosingObjects());
				v.maxSingleAtomEnc = Math.max(v.maxSingleAtomEnc, a.getEnclosingObjects());
			}
		}
		
		public Map<String, ValueFreq> getValues() {
			return values;
		}
		
		
	}
	
	public static class SuffixesStat {
		List<ValueFreq> longestSuffixes = new ArrayList<>();
		String longestSuffixesKey;
		int suffixesLenSum;
		int prefixesCount;
		int atomOneBitSuffix;
		int atomTwoBitSuffix;
		int atomCount;
		
		public void merge(SuffixesStat suffixesStat) {
			if (longestSuffixes.size() < suffixesStat.longestSuffixes.size()) {
				this.longestSuffixes = suffixesStat.longestSuffixes;
				this.longestSuffixesKey = suffixesStat.longestSuffixesKey;
				
			}
			this.atomCount += suffixesStat.atomCount;
			this.atomOneBitSuffix += suffixesStat.atomOneBitSuffix;
			this.atomTwoBitSuffix += suffixesStat.atomTwoBitSuffix;
			this.prefixesCount += suffixesStat.prefixesCount;
			this.suffixesLenSum += suffixesStat.suffixesLenSum;
		}
		
		public String toString(String nl) {
			int sz = longestSuffixes.size();
			String longestStr = String.format("Longest suffixes '%s' (%d): %s...", longestSuffixesKey,
					longestSuffixes.size(), longestSuffixes.subList(0, Math.min(30, sz)));
			String msg = String.format(
					"Name Suffixes - "
//					+ "%.1f avg suffixes per prefix, " // duplicate 23,193 prefixes, 45,283 tokens division
					+ "suffixes in atom set: 2 - %,d, 3+ - %,d. ",
//					suffixesLenSum * 1.0 / (prefixesCount + 1), 
					atomTwoBitSuffix, (atomCount - atomOneBitSuffix - atomTwoBitSuffix));
			return msg + longestStr;
		}

		@Override
		public String toString() {
			return toString("\n");
		}
		
	}
	
	public static class ValueFreq implements Comparable<ValueFreq> {
		public String value;
		public int freq;
		public long extra;
		public int enclosing;
		public int maxSingleAtomEnc;
		public int maxSingleSubValueEnc;
		public List<ValueFreq> subValues = null;
		
		public static boolean SORT_BY_NAME = false;
		public static boolean SORT_BY_TOP_FREQ = true;
		
		public ValueFreq(String name, int frequency) {
			this.value = name;
			this.freq = frequency;
		}
		
		
		public static void sort(List<ValueFreq> lst) {
			Collections.sort(lst);
		}
		
		public static void sortMain(List<ValueFreq> lst) {
			Collections.sort(lst, new Comparator<ValueFreq>() {

				@Override
				public int compare(ValueFreq o1, ValueFreq o2) {
					return -Integer.compare(o1.freq, o2.freq);
				}
				
			});
		}
		
		public ValueFreq copy() {
			ValueFreq vf = new ValueFreq(value, freq);
			vf.extra = extra;
			vf.enclosing = enclosing;
			vf.maxSingleAtomEnc = maxSingleAtomEnc;
			vf.maxSingleSubValueEnc = maxSingleSubValueEnc;
			if (subValues != null) {
				vf.subValues = new ArrayList<>();
				for (ValueFreq s : subValues) {
					vf.subValues.add(s.copy());
				}
			}
			return vf;
		}

		public List<ValueFreq> getSubvalues(double percent, int min) {
			if (subValues == null || subValues.size() == 0) {
				return Collections.emptyList();
			}
			int limit = Math.min(min, subValues.size());
			for (; limit < subValues.size(); limit++) {
				if (subValues.get(limit).freq < percent * freq) {
					break;
				}
			}
			return subValues.subList(0, limit);
		}
		
		public static ValueFreq get(Map<String, ValueFreq> values, String key, int def) {
			ValueFreq vf = values.get(key);
			if (vf == null) {
				vf = new ValueFreq(key, def);
				values.put(key, vf);
			}
			return vf;
		}

		public static Map<String, ValueFreq> mergeArray(Map<String, ValueFreq> res, List<ValueFreq> m) {
			for (ValueFreq s : m) {
				if (res.containsKey(s.value)) {
					res.get(s.value).merge(s);
				} else {
					res.put(s.value, s.copy());
				}
			}
			return res;
		}
		
		public static Map<String, ValueFreq> mergeFlatten(Map<String, ValueFreq> r, Collection<ValueFreq> ms) {
			for (ValueFreq s : ms) {
				if (s.subValues != null) {
					mergeFlatten(r, s.subValues);
				} else if (!r.containsKey(s.value)) {
					r.put(s.value, s.copy());
				} else {
					r.get(s.value).merge(s);
				}
			}
			return r;
		}

		public static Map<String, ValueFreq> mergeArray(Map<String, ValueFreq> res, Map<String, ValueFreq> ms) {
			for (ValueFreq s : ms.values()) {
				ValueFreq vf = res.get(s.value);
				if (vf != null) {
					vf.merge(s);
				} else {
					res.put(s.value, s.copy());
				}
			}
			return res;
		}
		
		
		public void merge(ValueFreq s) {
			this.freq += s.freq;
			this.enclosing += s.enclosing;
			this.maxSingleAtomEnc = Math.max(maxSingleAtomEnc, s.maxSingleAtomEnc);
			if (subValues == null && s.subValues != null) {
				s.subValues = new ArrayList<>();
			}
			this.extra += s.extra;
			if (subValues != null) {
				subValues = new ArrayList<>(
						mergeArray(mergeArray(new TreeMap<String, ValueFreq>(), subValues), s.subValues).values());
				for (ValueFreq v : subValues) {
					this.maxSingleSubValueEnc = Math.max(this.maxSingleSubValueEnc, v.enclosing);
				}
			}
		}

		@Override
		public String toString() {
			if (enclosing > 0) {
				String enc = String.format(", enc %,d/%,d", enclosing, maxSingleAtomEnc);
				if (subValues != null) {
					enc = String.format(", enc %,d/%,d/%,d", enclosing, maxSingleSubValueEnc, maxSingleAtomEnc);
				}
				String extraS = "";
				if (extra > 0) {
					extraS = String.format(", ex %,d", extra);
				}
				return String.format("%s (%,d%s%s)", value, freq, extraS, enc);
			}
			return String.format("%s (%,d)", value, freq);
		}
		
		public int getTopFreq() {
			if(subValues != null && subValues.size() > 0) {
				Collections.sort(subValues);
				return subValues.get(0).freq;
			}
			return freq;
		}

		@Override
		public int compareTo(ValueFreq o) {
			if (!SORT_BY_NAME) {
				if (SORT_BY_TOP_FREQ) {
					int c = -Integer.compare(getTopFreq(), o.getTopFreq());
					if (c != 0) {
						return c;
					}
				}
				int c = -Integer.compare(freq, o.freq);
				if (c != 0) {
					return c;
				}
			}
			return value.compareTo(o.value);
		}
	}
	
	private static class PrefixNameValue implements Comparable<PrefixNameValue> {
		public String key;
		public OsmAndPoiNameIndexData data = null;
		public AddressNameIndexData addr = null;
		
		@Override
		public String toString() {
			if (data != null) {
				List<ValueFreq> suffixes = collectPOIFrequencies(null);
				return String.format("%s (%d, %s)", key, data.getAtomsCount(), suffixes);
			} else if (addr != null) {
				List<ValueFreq> suffixes =  collectAddrFrequencies(key, null, null, -1);
				return String.format("%s (%d, %s)", key, addr.getAtomCount(), suffixes);
			} else {
				return key + " <NOT SET>";
			}
		}

		private List<ValueFreq> collectAddrFrequencies(String prefix, 
				SuffixesStat suffStats, StreetsIndexStat streetsStat, int f) {
			List<ValueFreq> suffixes = new ArrayList<>();
			ValueFreq streetsPrefix = null;
			if (streetsStat != null) {
				streetsPrefix = new ValueFreq(prefix, 0);
				streetsPrefix.subValues = new ArrayList<>();
				streetsStat.values.put(prefix, streetsPrefix);
			}
			
			String curSuffix = "";
			if (suffStats != null) {
				suffStats.prefixesCount++;
				suffStats.suffixesLenSum += addr.getSuffixesDictionaryList().size();
			}
			for (String s : addr.getSuffixesDictionaryList()) {
				curSuffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s);
				suffixes.add(new ValueFreq(key + curSuffix, 0));
				if (streetsStat != null) {
					streetsPrefix.subValues.add(new ValueFreq(key + curSuffix, 0));
				}
			}
			if (suffStats != null && suffStats.longestSuffixes.size() < suffixes.size()) {
				suffStats.longestSuffixes = suffixes;
				suffStats.longestSuffixesKey = key;
			}
			int INT_BITS = 32;
			for (AddressNameIndexDataAtom a : addr.getAtomList()) {
				if (a.getType() != f && f >= 0) {
					continue;
				}
				int setBits = 0;
				for (int i = 0; i < a.getSuffixesBitsetCount(); i++) {
					int suffBit = a.getSuffixesBitset(i);
					for (int j = 0; j < INT_BITS && suffBit != 0; j++) {
						if (suffBit % 2 == 1) {
							int ind = i * INT_BITS + j;
							setBits++;
							ValueFreq s = suffixes.get(ind);
							s.freq++;
							s.enclosing += a.getEnclosingObjects();
							s.maxSingleAtomEnc = Math.max(s.maxSingleAtomEnc, a.getEnclosingObjects());
							if (streetsStat != null) {
								ValueFreq sPref = streetsPrefix.subValues.get(ind);
								streetsStat.processAtom(streetsPrefix, sPref, a);
							}
						}
						suffBit >>= 1;
					}
				}
				if (suffStats != null && f >= 0) {
					if (setBits == 1) {
						suffStats.atomOneBitSuffix++;
					} else if (setBits == 2) {
						suffStats.atomTwoBitSuffix++;
					}
					suffStats.atomCount++;
				}
			}
			
			return suffixes;
		}
		
		private List<ValueFreq> collectPOIFrequencies(SuffixesStat stats) {
			List<ValueFreq> suffixes = new ArrayList<>();
			String curSuffix = "";
			stats.prefixesCount++;
			stats.suffixesLenSum += data.getSuffixesDictionaryList().size();
			for (String s : data.getSuffixesDictionaryList()) {
				curSuffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s);
				ValueFreq vf = new ValueFreq(key + curSuffix, 0);
				suffixes.add(vf);
			}
			if (stats != null && stats.longestSuffixes.size() < suffixes.size()) {
				stats.longestSuffixes = suffixes;
				stats.longestSuffixesKey = key;
			}
			int INT_BITS = 32;
			for (OsmAndPoiNameIndexDataAtom a : data.getAtomsList()) {
				int setBits = 0;
				for (int i = 0; i < a.getSuffixesBitsetCount(); i++) {
					int suffBit = a.getSuffixesBitset(i);
					for (int j = 0; j < INT_BITS && suffBit != 0; j++) {
						if ((suffBit & 1) == 1) {
							ValueFreq s = suffixes.get(i * INT_BITS + j);
							s.freq++;
							setBits++;
						}
						suffBit >>>= 1;
					}
				}
				if (stats != null) {
					if (setBits == 1) {
						stats.atomOneBitSuffix++;
					} else if (setBits == 2) {
						stats.atomTwoBitSuffix++;
					}
					stats.atomCount++;
				}
			}
			Collections.sort(suffixes);
			return suffixes;
		}

		@Override
		public int compareTo(PrefixNameValue o) {
			int c = -Integer.compare(data.getAtomsCount(), o.data.getAtomsCount());
			if (c == 0) {
				c = key.compareTo(o.key);
			}
			return c;
		}
	}
	
	public SuffixesStat getSuffixesStat() {
		return suffixesStat;
	}
	
	public StreetsIndexStat getStreetsStat() {
		return streetsStat;
	}
	

	public void setStreetsStat(StreetsIndexStat streetsStat) {
		this.streetsStat = streetsStat;
	}
	
	public void setSuffixesStat(SuffixesStat suffixesStat) {
		this.suffixesStat = suffixesStat;
	}
	
	public void setBoundariesStat(BoundariesIndexStat bndsStat) {
		this.bndsStat = bndsStat;
	}
	
	public BoundariesIndexStat getBoundariesStat() {
		return bndsStat;
	}
	
	public void putKey(String key, int val, String prefix) {
		PrefixNameValue nameValue = new PrefixNameValue();
		nameValue.key = key;
		indexByRef.put(initialShift + val, nameValue);
	}
	
	
	public List<ValueFreq> getPOIPrefixes(String prefix) {
		List<ValueFreq> ls = new ArrayList<NameIndexInspector.ValueFreq>();
		for (PrefixNameValue p : indexByRef.values()) {
			if (prefix != null && !(p.key.toLowerCase().startsWith(prefix) || prefix.toLowerCase().startsWith(p.key))) {
				continue;
			}
			ValueFreq vf = new ValueFreq(p.key, p.data.getAtomsCount());
			vf.subValues = p.collectPOIFrequencies(suffixesStat);
			ls.add(vf);
		}
		return ls;
	}
	

	public List<ValueFreq> getAddrPrefixes(int filter, String prefix) {
		List<ValueFreq> ls = new ArrayList<NameIndexInspector.ValueFreq>();
		for (PrefixNameValue p : indexByRef.values()) {
			if (prefix != null && !(p.key.toLowerCase().startsWith(prefix) || prefix.toLowerCase().startsWith(p.key))) {
				continue;
			}
			List<ValueFreq> subvalues = filter == -1 ? 
					p.collectAddrFrequencies(p.key, suffixesStat, streetsStat, filter)
					: p.collectAddrFrequencies(p.key, null, null, filter);
//			int total = p.addr.getAtomCount();
			// always recalculate other fields too
//			if (filter >= 0 || !Algorithms.isEmpty(prefix)) {
			int enclosing = 0, maxSingleAtomEnc = 0, maxSingleSubValueEnc = 0;
			int total = 0;
			List<ValueFreq> sublist = new ArrayList<>();
			for (ValueFreq s : subvalues) {
				total += s.freq;
				enclosing += s.enclosing;
				maxSingleSubValueEnc = Math.max(s.enclosing, maxSingleSubValueEnc);
				maxSingleAtomEnc = Math.max(s.maxSingleAtomEnc, maxSingleAtomEnc);
				if (s.freq > 0) {
					sublist.add(s);
				}
			}
			if (sublist.size() == 0) {
				continue;
			}
			subvalues = sublist;
			ValueFreq vf = new ValueFreq(p.key, total);
			vf.subValues = subvalues;
			vf.enclosing = enclosing;
			vf.maxSingleAtomEnc = maxSingleAtomEnc;
			vf.maxSingleSubValueEnc = maxSingleSubValueEnc;
			ls.add(vf);
		}
		return ls;
	}

	
	@Override
	public String toString() {
		return indexByRef.toString();
	}

	public void addData(OsmAndPoiNameIndexData from, long currentShift) {
		PrefixNameValue obj = indexByRef.get(currentShift);
		if (obj.data != null) {
			throw new IllegalStateException(obj.toString());
		}
		obj.data = from;
	}


	public void addData(AddressNameIndexData from, long currentShift) {
		PrefixNameValue obj = indexByRef.get(currentShift);
		if (obj.addr != null) {
			throw new IllegalStateException(obj.toString());
		}
		obj.addr = from;		
	}


}
