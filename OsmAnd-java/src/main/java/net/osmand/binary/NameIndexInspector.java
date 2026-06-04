package net.osmand.binary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;

public class NameIndexInspector {

	public Map<Long, PrefixNameValue> indexByRef = new HashMap<>();
	private long initialShift;
	

	public void setInitialShift(long totalBytesRead) {
		this.initialShift = totalBytesRead;
	}
	
	public static class ValueFreq implements Comparable<ValueFreq> {
		public String value;
		public int freq;
		public List<ValueFreq> subValues = null;
		
		public static boolean SORT_BY_NAME = false;
		
		public ValueFreq(String name, int frequency) {
			this.value = name;
			this.freq = frequency;
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
					res.put(s.value, s);
				}
			}
			return res;
		}
		
		
		public void merge(ValueFreq s) {
			this.freq += s.freq;
			if (subValues == null && s.subValues != null) {
				s.subValues = new ArrayList<>();
			}
			if (subValues != null) {
				subValues = new ArrayList<>(
						mergeArray(mergeArray(new TreeMap<String, ValueFreq>(), subValues), s.subValues).values());
			}
		}

		@Override
		public String toString() {
			return String.format("%s (%,d)", value ,freq);
		}

		@Override
		public int compareTo(ValueFreq o) {
			if (!SORT_BY_NAME) {
				int c = -Integer.compare(freq, o.freq);
				if (c != 0) {
					return c;
				}
			}
			return value.compareTo(o.value);
		}
	}
	
	public static class PrefixNameValue implements Comparable<PrefixNameValue> {
		public String key;
		public OsmAndPoiNameIndexData data = null;
		public static double LIMIT_PERCENT = 0.1;
		
		@Override
		public String toString() {
			List<ValueFreq> suffixes = collectFrequencies();
			return String.format("%s (%d, %s)", key, data.getAtomsCount(), suffixes);
		}

		private List<ValueFreq> collectFrequencies() {
			List<ValueFreq> suffixes = new ArrayList<>();
			for (String s : data.getSuffixesDictionaryList()) {
				ValueFreq vf = new ValueFreq(key + s, 0);
				suffixes.add(vf);
			}
			int intBits = 32;
			for (OsmAndPoiNameIndexDataAtom a : data.getAtomsList()) {
				for(int i = 0; i < a.getSuffixesBitsetCount(); i++) {
					int suffBit = a.getSuffixesBitset(i);
					for(int j = 0; j < intBits && suffBit != 0; j++) {
						if (suffBit % 2 == 1) {
							ValueFreq s = suffixes.get(i * intBits + j);
							s.freq++;
						}
						suffBit >>= 1;
					}
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
	
	public void putKey(String key, int val, String prefix) {
		PrefixNameValue nameValue = new PrefixNameValue();
		nameValue.key = key;
		indexByRef.put(initialShift + val, nameValue);
	}
	
	
	public List<ValueFreq> getPrefixes() {
		List<ValueFreq> ls = new ArrayList<NameIndexInspector.ValueFreq>();
		for(PrefixNameValue p : indexByRef.values()) {
			ValueFreq vf = new ValueFreq(p.key, p.data.getAtomsCount());
			vf.subValues = p.collectFrequencies();
			ls.add(vf);
		}
		return ls;
	}
	
	
	
	public List<PrefixNameValue> getTop(int limit) {
		List<PrefixNameValue> r = new ArrayList<>(indexByRef.values());
		Collections.sort(r);
		if (r.size() > limit) {
			r = r.subList(0, limit);
		}
		return r;
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



}
