package net.osmand.binary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.util.Algorithms;
import net.osmand.util.SearchAlgorithms;

public class NameIndexInspector {

	public Map<Long, PrefixNameValue> indexByRef = new HashMap<>();
	private long initialShift;
	
	private SuffixesStat suffixesStat = new SuffixesStat(); 
	

	public void setInitialShift(long totalBytesRead) {
		this.initialShift = totalBytesRead;
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
		public int extra;
		public List<ValueFreq> subValues = null;
		
		public static boolean SORT_BY_NAME = false;
		public static boolean SORT_BY_TOP_FREQ = true;
		
		public ValueFreq(String name, int frequency) {
			this.value = name;
			this.freq = frequency;
		}
		
		public ValueFreq copy() {
			ValueFreq vf = new ValueFreq(value, freq);
			vf.extra = extra;
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
			if (subValues == null && s.subValues != null) {
				s.subValues = new ArrayList<>();
			}
			this.extra += s.extra;
			if (subValues != null) {
				subValues = new ArrayList<>(
						mergeArray(mergeArray(new TreeMap<String, ValueFreq>(), subValues), s.subValues).values());
			}
		}

		@Override
		public String toString() {
			return String.format("%s (%,d)", value ,freq);
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
				int c = -Integer.compare(freq, o.freq);
				if (SORT_BY_TOP_FREQ) {
					c = -Integer.compare(getTopFreq(), o.getTopFreq());
				}
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
			List<ValueFreq> suffixes = collectFrequencies(null);
			if(data != null) {
				return String.format("%s (%d, %s)", key, data.getAtomsCount(), suffixes);
			} else if(addr != null) {
				return String.format("%s (%d, %s)", key, addr.getAtomCount(), suffixes);
			} else {
				return key + " <NOT SET>";
			}
		}

		private List<ValueFreq> collectAddrFrequencies(SuffixesStat stats, int f) {
			List<ValueFreq> suffixes = new ArrayList<>();
			String curSuffix = "";
			if (stats != null) {
				stats.prefixesCount++;
				stats.suffixesLenSum += addr.getSuffixesDictionaryList().size();
			}
			for (String s : addr.getSuffixesDictionaryList()) {
				curSuffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s);
				ValueFreq vf = new ValueFreq(key + curSuffix, 0);
				suffixes.add(vf);
			}
			if (stats != null && stats.longestSuffixes.size() < suffixes.size()) {
				stats.longestSuffixes = suffixes;
				stats.longestSuffixesKey = key;
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
							setBits++;
							ValueFreq s = suffixes.get(i * INT_BITS + j);
							s.freq++;
						}
						suffBit >>= 1;
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
			
			return suffixes;
		}
		
		private List<ValueFreq> collectFrequencies(SuffixesStat stats) {
			List<ValueFreq> suffixes = new ArrayList<>();
			if (data != null) {
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
					for(int i = 0; i < a.getSuffixesBitsetCount(); i++) {
						int suffBit = a.getSuffixesBitset(i);
						for(int j = 0; j < INT_BITS && suffBit != 0; j++) {
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
			} else if (addr != null) {
				suffixes = collectAddrFrequencies(stats, -1);
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
	
	public void putKey(String key, int val, String prefix) {
		PrefixNameValue nameValue = new PrefixNameValue();
		nameValue.key = key;
		indexByRef.put(initialShift + val, nameValue);
	}
	
	
	public List<ValueFreq> getPrefixes(String prefix) {
		List<ValueFreq> ls = new ArrayList<NameIndexInspector.ValueFreq>();
		for (PrefixNameValue p : indexByRef.values()) {
			if (prefix != null && !(p.key.toLowerCase().startsWith(prefix) || prefix.toLowerCase().startsWith(p.key))) {
				continue;
			}
			ValueFreq vf = new ValueFreq(p.key, p.data.getAtomsCount());
			vf.subValues = p.collectFrequencies(suffixesStat);
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
			List<ValueFreq> subvalues = p.collectAddrFrequencies(filter == -1 ? suffixesStat : null, filter);
			int total = p.addr.getAtomCount();
			if (filter >= 0 || !Algorithms.isEmpty(prefix)) {
				total = 0;
				List<ValueFreq> sublist = new ArrayList<>();
				for (ValueFreq s : subvalues) {
					total += s.freq;
					if (s.freq > 0) {
						sublist.add(s);
					}
				}
				if (sublist.size() == 0) {
					continue;
				}
				subvalues = sublist;
			}
			ValueFreq vf = new ValueFreq(p.key, total);
			vf.subValues = subvalues;
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
