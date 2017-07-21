package net.osmand.search.core;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.CommonWords;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

//immutable object
public class SearchPhrase {
	
	private List<SearchWord> words = new ArrayList<>();
	private List<String> unknownWords = new ArrayList<>();
	private List<NameStringMatcher> unknownWordsMatcher = new ArrayList<>();
	private String unknownSearchWordTrim;
	private String unknownSearchPhrase = "";
	
	private NameStringMatcher sm;
	private SearchSettings settings;
	private List<BinaryMapIndexReader> indexes;
	
	private QuadRect cache1kmRect;
	private boolean lastUnknownSearchWordComplete;
	private static final String DELIMITER = " ";
	private static final String ALLDELIMITERS = "\\s|,";
	private static final Pattern reg = Pattern.compile(ALLDELIMITERS);
	private Collator clt;
	
	private static Set<String> conjunctions = new TreeSet<>();
	static {
		// the
		conjunctions.add("the");
		conjunctions.add("der");
		conjunctions.add("den");
		conjunctions.add("die");
		conjunctions.add("das");
		conjunctions.add("la");
		conjunctions.add("le");
		conjunctions.add("el");
		conjunctions.add("il");
		// and
		conjunctions.add("and");
		conjunctions.add("und");
		conjunctions.add("en");
		conjunctions.add("et");
		conjunctions.add("y");
		conjunctions.add("и");
		// short 
		conjunctions.add("f");
		conjunctions.add("u");
		conjunctions.add("jl.");
		conjunctions.add("j");
		conjunctions.add("sk");
		conjunctions.add("w");
		conjunctions.add("a.");
		conjunctions.add("of");
		conjunctions.add("k");
		conjunctions.add("r");
		conjunctions.add("h");
		conjunctions.add("mc");
		conjunctions.add("sw");
		conjunctions.add("g");
		conjunctions.add("v");
		conjunctions.add("m");
		conjunctions.add("c.");
		conjunctions.add("r.");
		conjunctions.add("ct");
		conjunctions.add("e.");
		conjunctions.add("dr.");
		conjunctions.add("j.");		
		conjunctions.add("in");
		conjunctions.add("al");
		conjunctions.add("út");
		conjunctions.add("per");
		conjunctions.add("ne");
		conjunctions.add("p");
		conjunctions.add("et");
		conjunctions.add("s.");
		conjunctions.add("f.");
		conjunctions.add("t");
		conjunctions.add("fe");
		conjunctions.add("à");
		conjunctions.add("i");
		conjunctions.add("c");
		conjunctions.add("le");
		conjunctions.add("s");
		conjunctions.add("av.");
		conjunctions.add("den");
		conjunctions.add("dr");
		conjunctions.add("y");
	}
	
	
	public enum SearchPhraseDataType {
		MAP, ADDRESS, ROUTING, POI
	}
	
	
	public SearchPhrase(SearchSettings settings, Collator clt) {
		this.settings = settings;
		this.clt = clt;
	}
	
	public Collator getCollator() {
		return clt;
	}
	
	public SearchPhrase generateNewPhrase(String text, SearchSettings settings) {
		SearchPhrase sp = new SearchPhrase(settings, this.clt);
		String restText = text;
		List<SearchWord> leftWords = this.words;
		String thisTxt = getText(true);
		if (text.startsWith(thisTxt)) {
			// string is longer
			restText = text.substring(getText(false).length());
			sp.words = new ArrayList<>(this.words);
			leftWords = leftWords.subList(leftWords.size(), leftWords.size());
		}
		for(SearchWord w : leftWords) {
			if(restText.startsWith(w.getWord() + DELIMITER)) {
				sp.words.add(w);
				restText = restText.substring(w.getWord().length() + DELIMITER.length()).trim();
			} else {
				break;
			}
		}
		sp.unknownSearchPhrase = restText;
		sp.unknownWords.clear();
		sp.unknownWordsMatcher.clear();
		
		if (!reg.matcher(restText).find()) {
			sp.unknownSearchWordTrim = sp.unknownSearchPhrase.trim();
		} else {
			sp.unknownSearchWordTrim = "";
			String[] ws = restText.split(ALLDELIMITERS);
			boolean first = true;
			for (int i = 0; i < ws.length ; i++) {
				String wd = ws[i].trim();
				if (wd.length() > 0 && !conjunctions.contains(wd.toLowerCase())) {
					if (first) {
						sp.unknownSearchWordTrim = wd;
						first = false;
					} else {
						sp.unknownWords.add(wd);
					}
				}
			}
		}
		sp.lastUnknownSearchWordComplete = false;
		if (text.length() > 0 ) {
			char ch = text.charAt(text.length() - 1);
			sp.lastUnknownSearchWordComplete = ch == ' ' || ch == ',' || ch == '\r' || ch == '\n'
					|| ch == ';';
		}
		
		return sp;
	}
	

	public List<SearchWord> getWords() {
		return words;
	}
	

	public boolean isUnknownSearchWordComplete() {
		return lastUnknownSearchWordComplete || unknownWords.size() > 0;
	}
	
	public boolean isLastUnknownSearchWordComplete() {
		return lastUnknownSearchWordComplete;
	}


	public List<String> getUnknownSearchWords() {
		return unknownWords;
	}
	
	public List<String> getUnknownSearchWords(Collection<String> exclude) {
		if(exclude == null || unknownWords.size() == 0 || exclude.size() == 0) {
			return unknownWords;
		}
		List<String> l = new ArrayList<>();
		for(String uw : unknownWords) {
			if(exclude == null || !exclude.contains(uw)) {
				l.add(uw);
			}
		}
		return l;
	}
	
	
	public String getUnknownSearchWord() {
		return unknownSearchWordTrim;
	}
	
	public String getUnknownSearchPhrase() {
		return unknownSearchPhrase;
	}
	
	public boolean isUnknownSearchWordPresent() {
		return unknownSearchWordTrim.length() > 0;
	}
	
	public int getUnknownSearchWordLength() {
		return unknownSearchWordTrim.length() ;
	}
	
	
	public QuadRect getRadiusBBoxToSearch(int radius) {
		int radiusInMeters = getRadiusSearch(radius);
		QuadRect cache1kmRect = get1km31Rect();
		if(cache1kmRect == null) {
			return null;
		}
		int max = (1 << 31) - 1;
		double dx = (cache1kmRect.width() / 2) * radiusInMeters / 1000;
		double dy = (cache1kmRect.height() / 2) * radiusInMeters / 1000;
		double topLeftX = Math.max(0, cache1kmRect.left - dx);
		double topLeftY = Math.max(0, cache1kmRect.top - dy);
		double bottomRightX = Math.min(max, cache1kmRect.right + dx);
		double bottomRightY = Math.min(max, cache1kmRect.bottom + dy);
		return new QuadRect(topLeftX, topLeftY, bottomRightX, bottomRightY);
	}
	
	public QuadRect get1km31Rect() {
		if(cache1kmRect != null) {
			return cache1kmRect;
		}
		LatLon l = getLastTokenLocation();
		if (l == null) {
			return null;
		}
		float coeff = (float) (1000 / MapUtils.getTileDistanceWidth(SearchRequest.ZOOM_TO_SEARCH_POI));
		double tx = MapUtils.getTileNumberX(SearchRequest.ZOOM_TO_SEARCH_POI, l.getLongitude());
		double ty = MapUtils.getTileNumberY(SearchRequest.ZOOM_TO_SEARCH_POI, l.getLatitude());
		double topLeftX = Math.max(0, tx - coeff);
		double topLeftY = Math.max(0, ty - coeff);
		int max = (1 << SearchRequest.ZOOM_TO_SEARCH_POI)  - 1;
		double bottomRightX = Math.min(max, tx + coeff);
		double bottomRightY = Math.min(max, ty + coeff);
		double pw = MapUtils.getPowZoom(31 - SearchRequest.ZOOM_TO_SEARCH_POI);
		cache1kmRect = new QuadRect(topLeftX * pw, topLeftY * pw, bottomRightX * pw, bottomRightY * pw);
		return cache1kmRect;
	}
	
	
	public Iterator<BinaryMapIndexReader> getRadiusOfflineIndexes(int meters, final SearchPhraseDataType dt) {
		final QuadRect rect = meters > 0 ? getRadiusBBoxToSearch(meters) : null;
		return getOfflineIndexes(rect, dt);
		
	}

	public Iterator<BinaryMapIndexReader> getOfflineIndexes(final QuadRect rect, final SearchPhraseDataType dt) {
		List<BinaryMapIndexReader> list = indexes != null ? indexes : settings.getOfflineIndexes();
		final Iterator<BinaryMapIndexReader> lit = list.iterator();
		return new Iterator<BinaryMapIndexReader>() {
			BinaryMapIndexReader next = null;
			@Override
			public boolean hasNext() {
				while (lit.hasNext()) {
					next = lit.next();
					if(rect != null) {
						if(dt == SearchPhraseDataType.POI) {
							if(next.containsPoiData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom)) {
								return true;
							}
						} else if(dt == SearchPhraseDataType.ADDRESS) {
							// containsAddressData not all maps supported
							if(next.containsPoiData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom) && 
									next.containsAddressData()) {
								return true;
							}
						} else if(dt == SearchPhraseDataType.ROUTING) {
							if(next.containsRouteData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom, 15)) {
								return true;
							}
						} else {
							if(next.containsMapData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom, 15)) {
								return true;
							}
						}
					} else {
						return true;
					}
				}
				return false;
			}

			@Override
			public BinaryMapIndexReader next() {
				return next;
			}

			@Override
			public void remove() {
			}
		};
	}
	
	public List<BinaryMapIndexReader> getOfflineIndexes() {
		if(indexes != null) {
			return indexes; 
		}
		return settings.getOfflineIndexes();
	}
	
	public SearchSettings getSettings() {
		return settings;
	}
	
	
	public int getRadiusLevel() {
		return settings.getRadiusLevel();
	}

	public ObjectType[] getSearchTypes() {
		return settings == null ? null : settings.getSearchTypes();
	}

	public boolean isCustomSearch() {
		return getSearchTypes() != null;
	}

	public boolean isSearchTypeAllowed(ObjectType searchType) {
		if (getSearchTypes() == null) {
			return true;
		} else {
			for (ObjectType type : getSearchTypes()) {
				if (type == searchType) {
					return true;
				}
			}
			return false;
		}
	}

	public boolean isEmptyQueryAllowed() {
		return settings.isEmptyQueryAllowed();
	}

	public boolean isSortByName() {
		return settings.isSortByName();
	}

	public boolean isInAddressSearch() {
		return settings.isInAddressSearch();
	}

	public SearchPhrase selectWord(SearchResult res) {
		return selectWord(res, null, false);
	}
	
	public SearchPhrase selectWord(SearchResult res, List<String> unknownWords, boolean lastComplete) {
		SearchPhrase sp = new SearchPhrase(this.settings, this.clt);
		addResult(res, sp);
		SearchResult prnt = res.parentSearchResult;
		while(prnt != null) {
			addResult(prnt, sp);
			prnt = prnt.parentSearchResult;
		}
		sp.words.addAll(0, this.words);	
		if(unknownWords != null) {
			sp.lastUnknownSearchWordComplete = lastComplete;
			for (int i = 0; i < unknownWords.size(); i++) {
				if (i == 0) {
					sp.unknownSearchWordTrim = unknownWords.get(0);
				} else {
					sp.unknownWords.add(unknownWords.get(i));
				}
			}
		}
		return sp;
	}

	private void addResult(SearchResult res, SearchPhrase sp) {
		SearchWord sw = new SearchWord(res.wordsSpan != null ? res.wordsSpan : res.localeName.trim(), res);
		sp.words.add(0, sw);
	}
	
	public boolean isLastWord(ObjectType... p) {
		for (int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			for(ObjectType o : p) {
				if (sw.getType() == o) {
					return true;
				}
			}
			if (sw.getType() != ObjectType.UNKNOWN_NAME_FILTER) {
				return false;
			}
		}
		return false;
	}

	public ObjectType getExclusiveSearchType() {
		SearchWord lastWord = getLastSelectedWord();
		if (lastWord != null) {
			return ObjectType.getExclusiveSearchType(lastWord.getType());
		}
		return null;
	}

	public NameStringMatcher getNameStringMatcher() {
		if(sm != null) {
			return sm;
		}
		sm = getNameStringMatcher(unknownSearchWordTrim, lastUnknownSearchWordComplete);
		return sm;
	}
	
	
	public NameStringMatcher getNameStringMatcher(String word, boolean complete) {
		return new NameStringMatcher(word, 
				(complete ?  
					StringMatcherMode.CHECK_EQUALS_FROM_SPACE : 
					StringMatcherMode.CHECK_STARTS_FROM_SPACE));
	}
	
	public boolean hasObjectType(ObjectType p) {
		for(SearchWord s : words) {
			if(s.getType() == p) {
				return true;
			}
		}
		return false;
	}

	public void syncWordsWithResults() {
		for(SearchWord w : words) {
			w.syncWordWithResult();
		}
	}

	public String getText(boolean includeLastWord) {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(DELIMITER.trim() + " ");
		}
		if(includeLastWord) {
			sb.append(unknownSearchPhrase);
		}
		return sb.toString();
	}

	public String getTextWithoutLastWord() {
		StringBuilder sb = new StringBuilder();
		List<SearchWord> words = new ArrayList<>(this.words);
		if(Algorithms.isEmpty(unknownSearchWordTrim) && words.size() > 0) {
			words.remove(words.size() - 1);
		}
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(DELIMITER.trim() + " ");
		}
		return sb.toString();
	}

	public String getStringRerpresentation() {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(" [" + s.getType() + "], ");
		}
		sb.append(unknownSearchPhrase);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return getStringRerpresentation();
	}

	public boolean isNoSelectedType() {
		return words.isEmpty();
	}

	public boolean isEmpty() {
		return words.isEmpty() && unknownSearchPhrase.isEmpty();
	}

	public SearchWord getLastSelectedWord() {
		if(words.isEmpty()) {
			return null;
		}
		return words.get(words.size() - 1);
	}
	
	public LatLon getWordLocation() {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getLocation() != null) {
				return sw.getLocation();
			}
		}
		return null;
	}
	
	public LatLon getLastTokenLocation() {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getLocation() != null) {
				return sw.getLocation();
			}
		}
		// last token or myLocationOrVisibleMap if not selected 
		return settings.getOriginalLocation();
	}

	public void selectFile(BinaryMapIndexReader object) {
		if(indexes == null) {
			indexes = new ArrayList<>();
		}
		if(!this.indexes.contains(object)) {
			this.indexes.add(object);
		}
	}

	public void sortFiles() {
		if(indexes == null) {
			indexes = new ArrayList<>(getOfflineIndexes());
		}
		final LatLon ll = getLastTokenLocation();
		if(ll != null) {
			Collections.sort(indexes, new Comparator<BinaryMapIndexReader>() {
				Map<BinaryMapIndexReader, LatLon> locations = new HashMap<>();

				@Override
				public int compare(BinaryMapIndexReader o1, BinaryMapIndexReader o2) {
 					LatLon rc1 = o1 == null ? null : getLocation(o1);
					LatLon rc2 = o2 == null ? null : getLocation(o2);
					double d1 = rc1 == null ? 10000000d : MapUtils.getDistance(rc1, ll);
					double d2 = rc2 == null ? 10000000d : MapUtils.getDistance(rc2, ll);
					return Double.compare(d1, d2);
				}

				private LatLon getLocation(BinaryMapIndexReader o1) {
					if(locations.containsKey(o1)) {
						return locations.get(o1);
					}
					LatLon rc1 = null;
					if(o1.containsMapData()) {
						rc1 = o1.getMapIndexes().get(0).getCenterLatLon();
					} else {
						rc1 = o1.getRegionCenter();
					}
					locations.put(o1, rc1);
					return rc1;
				}
			});
		}
	}
	
	public static class NameStringMatcher implements StringMatcher {

		private CollatorStringMatcher sm;

		public NameStringMatcher(String lastWordTrim, StringMatcherMode mode) {
			sm = new CollatorStringMatcher(lastWordTrim, mode);
		}
		
		public boolean matches(Collection<String> map) {
			if(map == null) {
				return false;
			}
			for(String v : map) {
				if(sm.matches(v)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean matches(String name) {
			return sm.matches(name);
		}
		
	}
	
	public void countUnknownWordsMatch(SearchResult sr) {
		countUnknownWordsMatch(sr, sr.localeName, sr.otherNames);
	}
	
	public void countUnknownWordsMatch(SearchResult sr, String localeName, Collection<String> otherNames) {
		if(unknownWords.size() > 0) {
			for(int i = 0; i < unknownWords.size(); i++) {
				if(unknownWordsMatcher.size() == i) {
					unknownWordsMatcher.add(new NameStringMatcher(unknownWords.get(i), 
							i < unknownWords.size() - 1 ? StringMatcherMode.CHECK_EQUALS_FROM_SPACE :
								StringMatcherMode.CHECK_STARTS_FROM_SPACE));
				}
				NameStringMatcher ms = unknownWordsMatcher.get(i);
				if(ms.matches(localeName) || ms.matches(otherNames)) {
					if(sr.otherWordsMatch == null) {
						sr.otherWordsMatch = new TreeSet<>();
					}
					sr.otherWordsMatch.add(unknownWords.get(i));
				}
			}
		}
		if(!sr.firstUnknownWordMatches) {
			sr.firstUnknownWordMatches = localeName.equals(getUnknownSearchWord()) ||
					getNameStringMatcher().matches(localeName) || 
					getNameStringMatcher().matches(otherNames);	
		}
		
	}
	public int getRadiusSearch(int meters) {
		return (1 << (getRadiusLevel() - 1)) * meters;
	}

	public static int icompare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
	
	public String getUnknownWordToSearchBuilding() {
		List<String> unknownSearchWords = getUnknownSearchWords();
		if(unknownSearchWords.size() > 0 && Algorithms.extractFirstIntegerNumber(getUnknownSearchWord()) == 0) {
			for(String wrd : unknownSearchWords) {
				if(Algorithms.extractFirstIntegerNumber(wrd) != 0) {
					return wrd;
				}
			}
		}
		return getUnknownSearchWord();
	}
	
	public String getUnknownWordToSearch() {
		List<String> unknownSearchWords = getUnknownSearchWords();
		
		String wordToSearch = getUnknownSearchWord();
		if (unknownSearchWords.size() > 0) {
			List<String> searchWords = new ArrayList<>(unknownSearchWords);
			searchWords.add(0, getUnknownSearchWord());
			Collections.sort(searchWords, new Comparator<String>() {

				private int lengthWithoutNumbers(String s) {
					int len = 0;
					for(int k = 0; k < s.length(); k++) {
						if(s.charAt(k) >= '0' && s.charAt(k) <= '9') {
							
						} else {
							len++;
						}
					}
					return len;
				}
				
				@Override
				public int compare(String o1, String o2) {
					int i1 = CommonWords.getCommonSearch(o1.toLowerCase());
					int i2 = CommonWords.getCommonSearch(o2.toLowerCase());
					if (i1 != i2) {
						return icompare(i1, i2);
					}
					// compare length without numbers to not include house numbers
					return -icompare(lengthWithoutNumbers(o1), lengthWithoutNumbers(o2));
				}
			});						
			wordToSearch = searchWords.get(0);
		}

		return wordToSearch;
	}

	
}
