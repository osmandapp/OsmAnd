package net.osmand.search.core.spatial;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.binary.NameIndexReader;
import net.osmand.binary.NameIndexReader.NameIndexReaderBytes;
import net.osmand.binary.NameIndexReader.NameIndexReaderMatcher;
import net.osmand.binary.NameIndexReader.PrefixNameValue;
import net.osmand.binary.OsmandOdb.AddressNameIndexDataAtom;
import net.osmand.binary.OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData;
import net.osmand.binary.OsmandOdb.OsmAndPoiNameIndexDataAtom;
import net.osmand.util.MapUtils;
import net.osmand.util.SearchAlgorithms;

/**
 * "Did you mean": the query with its first misspelled word corrected. A complete word of at least MIN_LETTERS letters
 * that names at most MAX_OBJECTS objects is corrected to a word one edit away that names RATIO times as many objects
 * (any number when it names none). The rule was chosen on query logs and synthetic typos, see osmandapp/OsmAnd#5409.
 */
class SpatialTypoSuggestions {

	// a shorter word has too many neighbours
	static final int MIN_LETTERS = 4;
	// a word naming more objects is a real name
	static final int MAX_OBJECTS = 1;
	// a word naming one object is corrected only to a neighbour naming this many times more
	static final int RATIO = 300;
	// Typos after the key letters are seen in the blocks the word was read from. A typo inside the key of the name
	// index puts the word into another block, so a word naming at most MAX_OBJECTS objects is looked up once more
	// with every one-edit variant of its first KEY_LETTERS letters. The key holds the first
	// charsToBuildPoiNameIndex / charsToBuildAddressNameIndex (4) code points of a word, see IndexCreatorSettings
	// in OsmAnd-tools (java-tools/OsmAndMapCreatorUtilities/src/main/java/net/osmand/obf/preparation/, options
	// --chars-build-poi-nameindex, --chars-build-addr-nameindex). KEY_LETTERS must be at least the longest key of
	// the opened maps: typos in the letters between the two lengths are not found; a shorter key only costs variants.
	static final int KEY_LETTERS = 4;
	// the maps are read nearest first until this many bytes of their name index are read for a word, so the
	// suggestion does not depend on the device speed
	static final int KEY_MAX_BYTES = 1024 * 1024;

	private final SpatialSearchContext ctx;
	// words one edit away from a token and the number of objects they name, counted while its atoms are read
	private final Map<SpatialSearchToken, Map<String, int[]>> neighbours = new IdentityHashMap<>();

	SpatialTypoSuggestions(SpatialSearchContext ctx) {
		this.ctx = ctx;
	}

	void clear() {
		neighbours.clear();
	}

	private boolean isCandidate(SpatialSearchToken t) {
		return ctx.settings.TYPO_SUGGESTION && !t.broad && t.wordNoDot.length() >= MIN_LETTERS
				&& SearchAlgorithms.letters(t.wordNoDot) == t.wordNoDot.length();
	}

	/**
	 * The words of the name index blocks one edit away from the token (a word still being typed: its beginning). The
	 * blocks are the ones the token itself was read from, so only typos after its key letters are seen.
	 */
	void countNeighbours(SpatialSearchToken t, List<PrefixNameValue> prefixes) throws IOException {
		if (isCandidate(t)) {
			for (PrefixNameValue prefix : prefixes) {
				countNeighbours(t, prefix);
			}
		}
	}

	/**
	 * A word that names at most MAX_OBJECTS objects is looked up once more with a typo in its first letters: the
	 * blocks of the index keys those variants fall into are read and their words one edit away are counted.
	 */
	void readKeyNeighbours() throws IOException {
		for (SpatialSearchToken t : ctx.tokens) {
			if (!isCandidate(t) || t.atoms.size() > MAX_OBJECTS || t.hasPoiCategoryKeys()) {
				continue;
			}
			// every beginning of every variant: a key of the index table matches when it is one of them
			Set<String> beginnings = new HashSet<>();
			for (String v : keyVariants(typoKey(t.wordNoDot), KEY_LETTERS)) {
				for (int i = 1; i <= v.length(); i++) {
					beginnings.add(v.substring(0, i));
				}
			}
			NameIndexReaderMatcher matcher = new NameIndexReaderMatcher(t.word) {
				@Override
				public boolean matchKey(String key) {
					return beginnings.contains(typoKey(key));
				}
			};
			String query = "typo-key " + t.word;
			long bytes = 0;
			for (int fileInd : filesNearestFirst()) {
				if (bytes > KEY_MAX_BYTES) {
					break;
				}
				for (NameIndexReader typoReader : ctx.internalFile.get(fileInd).typoReaders) {
					if (typoReader.poiRegion != null ? !ctx.settings.SEARCH_POI : !ctx.settings.SEARCH_ADDR) {
						continue;
					}
					typoReader.resetBytesStat();
					try {
						List<PrefixNameValue> prefixes = ctx.files.get(fileInd).readFullNameIndex(typoReader.setQuery(query, matcher));
						if (prefixes != null) {
							for (PrefixNameValue prefix : prefixes) {
								countNeighbours(t, prefix);
							}
						}
					} finally {
						NameIndexReaderBytes read = typoReader.getBytesStat();
						bytes += read.readTableBytes - read.skipTableBytes + read.readAtomBytes;
						typoReader.clearQuery();
						typoReader.clearPrefixes();
					}
				}
			}
		}
	}

	private List<Integer> filesNearestFirst() {
		List<Integer> order = new ArrayList<>();
		long[] dist = new long[ctx.files.size()];
		for (int i = 0; i < ctx.files.size(); i++) {
			order.add(i);
			dist[i] = Long.MAX_VALUE;
			if (ctx.location != null) {
				long x = MapUtils.get31TileNumberX(ctx.location.getLongitude()), y = MapUtils.get31TileNumberY(ctx.location.getLatitude());
				for (PoiRegion r : ctx.files.get(i).getPoiIndexes()) {
					long dx = Math.max(0, Math.max(r.getLeft31() - x, x - r.getRight31()));
					long dy = Math.max(0, Math.max(r.getTop31() - y, y - r.getBottom31()));
					dist[i] = Math.min(dist[i], dx * dx + dy * dy);
				}
			}
		}
		order.sort(Comparator.comparingLong(i -> dist[i]));
		return order;
	}

	private void countNeighbours(SpatialSearchToken t, PrefixNameValue prefix) throws IOException {
		AddressNameIndexData addrData = prefix.getAddr();
		OsmAndPoiNameIndexData poiData = addrData == null ? prefix.getPoi() : null;
		if (addrData == null && poiData == null) {
			return;
		}
		String typed = typoKey(t.wordNoDot);
		List<String> names = new ArrayList<>();
		String curSuffix = null;
		boolean any = false;
		for (String s : addrData != null ? addrData.getSuffixesDictionaryList() : poiData.getSuffixesDictionaryList()) {
			curSuffix = SearchAlgorithms.nameIndexDecodeDictionarySuffix(curSuffix, s);
			String name = typoKey(prefix.key + curSuffix);
			boolean near = name.indexOf(' ') == -1
					&& (isOneEdit(typed, name) || (t.incomplete && isOneEditPrefix(typed, name)));
			names.add(near ? name : null);
			any |= near;
		}
		if (!any) {
			return;
		}
		Set<String> inAtom = new HashSet<>();
		if (addrData != null) {
			for (AddressNameIndexDataAtom a : addrData.getAtomList()) {
				countNeighbours(t, names, a.getSuffixesBitsetIndexList(), inAtom);
			}
		} else {
			for (OsmAndPoiNameIndexDataAtom a : poiData.getAtomsList()) {
				if (a.getPoiIndInBlockCount() > 0) {
					countNeighbours(t, names, a.getSuffixesBitsetIndexList(), inAtom);
				}
			}
		}
	}

	private void countNeighbours(SpatialSearchToken t, List<String> names, List<Integer> suffixBits, Set<String> inAtom) {
		inAtom.clear();
		for (int bit : suffixBits) {
			int ind = bit / 2 - 1;
			if (bit != 0 && bit % 2 == 0 && ind < names.size() && names.get(ind) != null && inAtom.add(names.get(ind))) {
				neighbours.computeIfAbsent(t, k -> new HashMap<>()).computeIfAbsent(names.get(ind), k -> new int[1])[0]++;
			}
		}
	}

	/**
	 * The input with its first misspelled word corrected, null when there is none.
	 */
	String suggestion(String input, List<SpatialSearchToken> tokens) {
		List<SpatialSearchToken> ordered = new ArrayList<>(tokens);
		ordered.sort(Comparator.comparingInt(t -> t.originalOrder));
		for (SpatialSearchToken t : ordered) {
			Map<String, int[]> near = neighbours.get(t);
			if (!isCandidate(t) || near == null || t.hasPoiCategoryKeys()) {
				continue;
			}
			int objects = t.atoms.size();
			if (objects > MAX_OBJECTS) {
				continue;
			}
			// the word naming most objects; at a tie a word one edit away before a name whose beginning is (typing)
			String typed = typoKey(t.wordNoDot);
			String best = null;
			int bestCount = 0;
			boolean bestWhole = false;
			for (Entry<String, int[]> e : near.entrySet()) {
				int c = e.getValue()[0];
				boolean whole = isOneEdit(typed, e.getKey());
				if (best == null || c > bestCount || (c == bestCount && ((whole && !bestWhole)
						|| (whole == bestWhole && e.getKey().compareTo(best) < 0)))) {
					best = e.getKey();
					bestCount = c;
					bestWhole = whole;
				}
			}
			if (best != null && (objects == 0 || bestCount >= (long) RATIO * objects)) {
				return replaceWord(input, t.originalWord, best);
			}
		}
		return null;
	}

	// the word replaced in the input; a capital letter of the word stays
	static String replaceWord(String input, String word, String replacement) {
		String text = stripIncompleteDot(input);
		word = stripIncompleteDot(word);
		int i = text.indexOf(word);
		if (i < 0) {
			i = text.toLowerCase().indexOf(word.toLowerCase());
		}
		if (i < 0) {
			return null;
		}
		if (Character.isUpperCase(text.charAt(i)) && !replacement.isEmpty()) {
			replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
		}
		return text.substring(0, i) + replacement + text.substring(i + word.length());
	}

	private static String stripIncompleteDot(String s) {
		return s.endsWith(SpatialSearchToken.DOT_INCOMPLETE_STRING) ? s.substring(0, s.length() - 1) : s;
	}

	// the words one edit away from a word whose edit is in its first letters (the edits after them keep its index key)
	static Set<String> keyVariants(String w, int keyLetters) {
		Set<String> res = new HashSet<>();
		String alphabet = w.chars().anyMatch(c -> c >= 0x400 && c <= 0x4ff)
				? "абвгдеёжзийклмнопрстуфхцчшщъыьэюяіїєґ" : "abcdefghijklmnopqrstuvwxyz";
		int n = Math.min(keyLetters, w.length());
		for (int i = 0; i < n; i++) {
			res.add(w.substring(0, i) + w.substring(i + 1));
			if (i + 1 < w.length()) {
				res.add(w.substring(0, i) + w.charAt(i + 1) + w.charAt(i) + w.substring(i + 2));
			}
			for (char c : alphabet.toCharArray()) {
				res.add(w.substring(0, i) + c + w.substring(i + 1));
				res.add(w.substring(0, i) + c + w.substring(i));
			}
		}
		res.remove(w);
		return res;
	}

	// letters only, lower case, without accents: the form in which a typo and its correction are compared
	static String typoKey(String s) {
		boolean plain = true;
		for (int i = 0; i < s.length() && plain; i++) {
			char c = s.charAt(i);
			plain = (c >= 'a' && c <= 'z') || c == ' ';
		}
		if (plain) {
			return s;
		}
		String n = Normalizer.normalize(s.toLowerCase().replace("ß", "ss"), Normalizer.Form.NFD);
		StringBuilder b = new StringBuilder(n.length());
		for (int i = 0; i < n.length(); i++) {
			char c = n.charAt(i);
			if (Character.getType(c) != Character.NON_SPACING_MARK && Character.getType(c) != Character.ENCLOSING_MARK
					&& Character.getType(c) != Character.COMBINING_SPACING_MARK) {
				b.append(c);
			}
		}
		return b.toString();
	}

	// a word still being typed: the beginning of the name is one edit away from it
	static boolean isOneEditPrefix(String typed, String name) {
		if (name.startsWith(typed)) {
			return false; // a continuation of the typed letters, not a typo
		}
		int n = typed.length();
		for (int len = n - 1; len <= n + 1; len++) {
			if (len > 0 && len < name.length() && isOneEdit(typed, name.substring(0, len))) {
				return true;
			}
		}
		return false;
	}

	// one insertion, deletion, substitution or swap of two neighbouring letters
	static boolean isOneEdit(String a, String b) {
		int la = a.length(), lb = b.length();
		if (Math.abs(la - lb) > 1 || a.equals(b)) {
			return false;
		}
		int i = 0;
		while (i < la && i < lb && a.charAt(i) == b.charAt(i)) {
			i++;
		}
		if (la == lb) {
			if (a.substring(i + 1).equals(b.substring(i + 1))) {
				return true;
			}
			return i + 1 < la && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i)
					&& a.substring(i + 2).equals(b.substring(i + 2));
		}
		return la > lb ? a.substring(i + 1).equals(b.substring(i)) : a.substring(i).equals(b.substring(i + 1));
	}
}
