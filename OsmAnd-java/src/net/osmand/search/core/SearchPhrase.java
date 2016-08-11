package net.osmand.search.core;

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

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

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
	
	private static Set<String> conjunctionsThe = new TreeSet<>();
	private static Set<String> conjunctionsAnd = new TreeSet<>();
	static {
		// the
		conjunctionsThe.add("the");
		conjunctionsThe.add("der");
		conjunctionsThe.add("den");
		conjunctionsThe.add("die");
		conjunctionsThe.add("das");
		conjunctionsThe.add("la");
		conjunctionsThe.add("le");
		conjunctionsThe.add("el");
		conjunctionsThe.add("il");
		// and
		conjunctionsAnd .add("and");
		conjunctionsAnd .add("und");
		conjunctionsAnd .add("en");
		conjunctionsAnd .add("et");
		conjunctionsAnd .add("y");
		conjunctionsAnd .add("и");
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
				if (wd.length() > 0 && !conjunctionsThe.contains(wd.toLowerCase())) {
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
	
	public NameStringMatcher getNameStringMatcher() {
		if(sm != null) {
			return sm;
		}
		sm = new NameStringMatcher(unknownSearchWordTrim, 
				(lastUnknownSearchWordComplete ?  
					StringMatcherMode.CHECK_EQUALS_FROM_SPACE : 
					StringMatcherMode.CHECK_STARTS_FROM_SPACE));
		return sm;
	}
	
	public boolean hasObjectType(ObjectType p) {
		for(SearchWord s : words) {
			if(s.getType() == p) {
				return true;
			}
		}
		return false;
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
 					LatLon rc1 = getLocation(o1);
					LatLon rc2 = getLocation(o2);
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
	}
	public int getRadiusSearch(int meters) {
		return (1 << (getRadiusLevel() - 1)) * meters;
	}

	
}
