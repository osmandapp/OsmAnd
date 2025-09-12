package net.osmand.search.core;

import java.util.*;
import java.util.regex.Pattern;

import net.osmand.Collator;
import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.StringMatcher;
import net.osmand.binary.Abbreviations;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.CommonWords;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.AbstractPoiType;
import net.osmand.util.Algorithms;
import net.osmand.util.ArabicNormalizer;
import net.osmand.util.LocationParser;
import net.osmand.util.MapUtils;

// Immutable object !
public class SearchPhrase {
	public static final String DELIMITER = " ";
	public static final String ALLDELIMITERS = "\\s|,";
	public static final String ALLDELIMITERS_WITH_HYPHEN = "\\s|,|-";
	private static final Pattern reg = Pattern.compile(ALLDELIMITERS);
	private static Comparator<String> commonWordsComparator;
	private static Set<String> conjunctions = new TreeSet<>();
	
	private final Collator clt;
	private final SearchSettings settings;
	private List<BinaryMapIndexReader> indexes;
	
	private BinaryMapIndexReader fileRequest;
	
	// Object consists of 2 part [known + unknown] 
	private String fullTextSearchPhrase = "";
	private String unknownSearchPhrase = "";

	// words to be used for words span
	private List<SearchWord> words = new ArrayList<>();
	
	// Words of 2 parts
	private String firstUnknownSearchWord = "";
	private List<String> otherUnknownWords = new ArrayList<>();
	private boolean lastUnknownSearchWordComplete;
	
	// Main unknown word used for search
	private String mainUnknownWordToSearch = null;
	private boolean mainUnknownSearchWordComplete;

	// Name Searchers
	private NameStringMatcher firstUnknownNameStringMatcher;
	private NameStringMatcher mainUnknownNameStringMatcher;
	private List<NameStringMatcher> unknownWordsMatcher = new ArrayList<>();

	private AbstractPoiType unselectedPoiType;
	private boolean acceptPrivate;
	private QuadRect cache1kmRect;
	
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

		commonWordsComparator = new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int i1 = CommonWords.getCommonSearch(o1.toLowerCase());
				int i2 = CommonWords.getCommonSearch(o2.toLowerCase());
				if (i1 != i2) {
					if(i1 == -1) {
						return -1;
					} else if(i2 == -1) {
						return 1;
					}
					return -icompare(i1, i2);
				}
				// compare length without numbers to not include house numbers
				return -icompare(lengthWithoutNumbers(o1), lengthWithoutNumbers(o2));
			}
		};
	}
	
	
	public enum SearchPhraseDataType {
		MAP, ADDRESS, ROUTING, POI
	}
	
	
	private SearchPhrase(SearchSettings settings, Collator clt) {
		this.settings = settings;
		this.clt = clt;
	}
	
	public Collator getCollator() {
		return clt;
	}
	
	public BinaryMapIndexReader getFileRequest() {
		return fileRequest;
	}
	
	public SearchPhrase generateNewPhrase(SearchPhrase phrase, BinaryMapIndexReader file) {
		SearchPhrase nphrase = generateNewPhrase(phrase.getUnknownSearchPhrase(), phrase.getSettings());
		nphrase.fileRequest = file;
		return nphrase;
	}
	
	
	public SearchPhrase generateNewPhrase(String text, SearchSettings settings) {
		String textToSearch = Algorithms.normalizeSearchText(text);
		List<SearchWord> leftWords = this.words;
		String thisTxt = getText(true);
		List<SearchWord> foundWords = new ArrayList<>();
		thisTxt = Algorithms.normalizeSearchText(thisTxt);
		if (textToSearch.startsWith(thisTxt)) {
			// string is longer
			textToSearch = textToSearch.substring(getText(false).length());
			foundWords.addAll(this.words);
			leftWords = leftWords.subList(leftWords.size(), leftWords.size());
		}
		for (SearchWord w : leftWords) {
			if (textToSearch.startsWith(w.getWord() + DELIMITER)) {
				foundWords.add(w);
				textToSearch = textToSearch.substring(w.getWord().length() + DELIMITER.length());
			} else {
				break;
			}
		}
		return createNewSearchPhrase(settings, text, foundWords, textToSearch);
	}

	

	public static SearchPhrase emptyPhrase() {
		return emptyPhrase(null);
	}
	
	public static SearchPhrase emptyPhrase(SearchSettings settings) {
		return emptyPhrase(settings, OsmAndCollator.primaryCollator());
	}
	
	public static SearchPhrase emptyPhrase(SearchSettings settings, Collator clt) {
		return new SearchPhrase(settings, clt);
	}
	
	// init search phrase
	private SearchPhrase createNewSearchPhrase(final SearchSettings settings, String fullText, List<SearchWord> foundWords,
											   String textToSearch) {
		SearchPhrase sp = new SearchPhrase(settings, this.clt);
		sp.words = foundWords;
		sp.fullTextSearchPhrase = fullText;
		sp.unknownSearchPhrase = textToSearch;
		sp.lastUnknownSearchWordComplete = isTextComplete(fullText) ;
		if (!reg.matcher(textToSearch).find()) {
			sp.firstUnknownSearchWord = sp.unknownSearchPhrase.trim();
		} else {
			sp.firstUnknownSearchWord = "";
			String[] ws = textToSearch.split(ALLDELIMITERS);
			boolean first = true;
			for (int i = 0; i < ws.length ; i++) {
				String wd = ws[i].trim();
				boolean conjunction = conjunctions.contains(wd.toLowerCase());
				boolean lastAndIncomplete = i == ws.length - 1 && !sp.lastUnknownSearchWordComplete;
				boolean decryptAbbreviations = needDecryptAbbreviations();
				if (wd.length() > 0 && (!conjunction || lastAndIncomplete)) {
					if (first) {
						sp.firstUnknownSearchWord = decryptAbbreviations ? Abbreviations.replace(wd) : wd;
						first = false;
					} else {
						sp.otherUnknownWords.add(decryptAbbreviations ? Abbreviations.replace(wd) : wd);
					}
				}
			}
		}
		return sp;
	}

	private boolean needDecryptAbbreviations() {
		String langs = settings != null ? settings.getRegionLang() : null;
		if (langs != null) {
			String[] langArr = langs.split(",");
			for (String lang : langArr) {
				if (lang.equals("en")) {
					return true;
				}
			}
		}
		return false;
	}

	public static List<String> splitWords(String w, List<String> ws, String delimiters) {
		if (!Algorithms.isEmpty(w)) {
			String[] wrs = w.split(delimiters);
			for (String wr : wrs) {
				String wd = wr.trim();
				if (wd.length() > 0) {
					ws.add(wd);
				}
			}
		}
		return ws;
	}
	
	public static int countWords(String w) {
		int cnt = 0;
		if (!Algorithms.isEmpty(w)) {
			String[] ws = w.split(ALLDELIMITERS);
			for (int i = 0; i < ws.length; i++) {
				String wd = ws[i].trim();
				if (wd.length() > 0) {
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	public SearchPhrase selectWord(SearchResult res, List<String> unknownWords, boolean lastComplete) {
		SearchPhrase sp = new SearchPhrase(this.settings, this.clt);
		addResult(res, sp);
		SearchResult prnt = res.parentSearchResult;
		while (prnt != null) {
			addResult(prnt, sp);
			prnt = prnt.parentSearchResult;
		}
		sp.words.addAll(0, this.words);	
		if (unknownWords != null) {
			sp.lastUnknownSearchWordComplete = lastComplete;
			StringBuilder genUnknownSearchPhrase = new StringBuilder();
			for (int i = 0; i < unknownWords.size(); i++) {
				if (i == 0) {
					sp.firstUnknownSearchWord = unknownWords.get(0);
				} else {
					sp.otherUnknownWords.add(unknownWords.get(i));
				}
				genUnknownSearchPhrase.append(unknownWords.get(i)).append(" ");
			}
			sp.fullTextSearchPhrase = fullTextSearchPhrase; 
			sp.unknownSearchPhrase = genUnknownSearchPhrase.toString().trim();
		}
		return sp;
	}
	
	
	private void calcMainUnknownWordToSearch() {
		if (mainUnknownWordToSearch != null) {
			return;
		}
		List<String> unknownSearchWords = otherUnknownWords;
		mainUnknownWordToSearch = firstUnknownSearchWord;
		mainUnknownSearchWordComplete = lastUnknownSearchWordComplete;
		if (!unknownSearchWords.isEmpty()) {
			mainUnknownSearchWordComplete = true;
			List<String> searchWords = new ArrayList<>(unknownSearchWords);
			searchWords.add(0, getFirstUnknownSearchWord());
			Collections.sort(searchWords, commonWordsComparator);
			for (String s : searchWords) {
				if (s.length() > 0 && !LocationParser.isValidOLC(s)) {
					mainUnknownWordToSearch = s.trim();
					if (mainUnknownWordToSearch.endsWith(".")) {
						mainUnknownWordToSearch = mainUnknownWordToSearch.substring(0,
								mainUnknownWordToSearch.length() - 1);
						mainUnknownSearchWordComplete = false;
					}
					int unknownInd = unknownSearchWords.indexOf(s);
					if (!lastUnknownSearchWordComplete && unknownSearchWords.size() - 1 == unknownInd) {
						mainUnknownSearchWordComplete = false;
					}
					break;
				}
			}
		}
		if (ArabicNormalizer.isSpecialArabic(mainUnknownWordToSearch)) {
			String normalized = ArabicNormalizer.normalize(mainUnknownWordToSearch);
			mainUnknownWordToSearch = normalized == null ? mainUnknownWordToSearch : normalized;
		}
	}

	public List<SearchWord> getWords() {
		return words;
	}

	public AbstractPoiType getUnselectedPoiType() {
		return unselectedPoiType;
	}
	
	public void setUnselectedPoiType(AbstractPoiType unselectedPoiType) {
		this.unselectedPoiType = unselectedPoiType;
	}

	public boolean isMainUnknownSearchWordComplete() {
		// return lastUnknownSearchWordComplete || otherUnknownWords.size() > 0 || unknownSearchWordPoiType != null;
		return mainUnknownSearchWordComplete;
	}

	public boolean isLastUnknownSearchWordComplete() {
		return lastUnknownSearchWordComplete;
	}
	
	public boolean hasMoreThanOneUnknownSearchWord() {
		return otherUnknownWords.size() > 0;
	}

	public List<String> getUnknownSearchWords() {
		return otherUnknownWords;
	}
	
	
	public String getFirstUnknownSearchWord() {
		return firstUnknownSearchWord;
	}
	
	public boolean isFirstUnknownSearchWordComplete() {
		return hasMoreThanOneUnknownSearchWord() || isLastUnknownSearchWordComplete();
	}

	public boolean isAcceptPrivate() {
		return acceptPrivate;
	}

	public void setAcceptPrivate(boolean acceptPrivate) {
		this.acceptPrivate = acceptPrivate;
	}

	public String getFullSearchPhrase() {
		return fullTextSearchPhrase;
	}

	public String getUnknownSearchPhrase() {
		return unknownSearchPhrase;
	}
	
	public boolean isUnknownSearchWordPresent() {
		return firstUnknownSearchWord.length() > 0;
	}
	
	public QuadRect getRadiusBBoxToSearch(int radius) {
		QuadRect searchBBox31 = this.settings.getSearchBBox31();
		if (searchBBox31 != null) {
			return searchBBox31;
		}
		
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

	public boolean hasCustomSearchType(ObjectType type) {
		return settings.hasCustomSearchType(type);
	}

	public boolean isSearchTypeAllowed(ObjectType searchType) {
		return isSearchTypeAllowed(searchType, false);
	}

	public boolean isSearchTypeAllowed(ObjectType searchType, boolean exclusive) {
		ObjectType[] searchTypes = getSearchTypes();
		if (searchTypes == null) {
			return !exclusive;
		} else {
			if (exclusive && searchTypes.length > 1) {
				return false;
			}
			for (ObjectType type : searchTypes) {
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

	public SearchPhrase selectWord(SearchResult res) {
		return selectWord(res, null, false);
	}
	

	public void addResult(SearchResult res, SearchPhrase sp) {
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

	public NameStringMatcher getMainUnknownNameStringMatcher() {
		calcMainUnknownWordToSearch();
		if (mainUnknownNameStringMatcher == null) {
			mainUnknownNameStringMatcher = getNameStringMatcher(mainUnknownWordToSearch, mainUnknownSearchWordComplete);
		}
		return mainUnknownNameStringMatcher;
	}
	
	public NameStringMatcher getFirstUnknownNameStringMatcher() {
		if (firstUnknownNameStringMatcher == null) {
			firstUnknownNameStringMatcher = getNameStringMatcher(firstUnknownSearchWord, isFirstUnknownSearchWordComplete());
		}
		return firstUnknownNameStringMatcher;
	}

	public NameStringMatcher getUnknownNameStringMatcher(int i) {
		while (unknownWordsMatcher.size() <= i) {
			int ind = unknownWordsMatcher.size();
			boolean completeMatch = ind < otherUnknownWords.size() - 1 || isLastUnknownSearchWordComplete();
			unknownWordsMatcher.add(getNameStringMatcher(otherUnknownWords.get(ind), completeMatch));
		}
		return unknownWordsMatcher.get(i);
	}

	private NameStringMatcher getNameStringMatcher(String word, boolean complete) {
		return new NameStringMatcher(word,
				(complete ?
					StringMatcherMode.CHECK_EQUALS_FROM_SPACE :
					StringMatcherMode.CHECK_STARTS_FROM_SPACE));
	}

	public boolean hasObjectType(ObjectType p) {
		for (SearchWord s : words) {
			if (s.getType() == p) {
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

	public String getText(boolean includeUnknownPart) {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(DELIMITER);
		}
		if(includeUnknownPart) {
			sb.append(unknownSearchPhrase);
		}
		return sb.toString();
	}

	public String getTextWithoutLastWord() {
		StringBuilder sb = new StringBuilder();
		List<SearchWord> words = new ArrayList<>(this.words);
		if (Algorithms.isEmpty(unknownSearchPhrase.trim()) && words.size() > 0) {
			words.remove(words.size() - 1);
		}
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(DELIMITER);
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
		if (settings != null) {
			return settings.getOriginalLocation();
		}
		return null;
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
        if (indexes == null) {
            indexes = new ArrayList<>(getOfflineIndexes());
        }
        Map<String, List<BinaryMapIndexReader>> diffsByRegion = getDiffsByRegion();
        final LatLon ll = getLastTokenLocation();
        if (ll != null) {
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
                    if (locations.containsKey(o1)) {
                        return locations.get(o1);
                    }
                    LatLon rc1 = null;
                    if (o1.containsMapData()) {
                        rc1 = o1.getMapIndexes().get(0).getCenterLatLon();
                    } else {
                        rc1 = o1.getRegionCenter();
                    }
                    locations.put(o1, rc1);
                    return rc1;
                }
            });
            if (!diffsByRegion.isEmpty()) {
                List<BinaryMapIndexReader> finalSort = new ArrayList<>();
                for (int i = 0; i < indexes.size(); i++) {
                    BinaryMapIndexReader currFile = indexes.get(i);
                    if (diffsByRegion.get(currFile.getRegionName()) != null) {
                        finalSort.addAll(diffsByRegion.get(currFile.getRegionName()));
                        finalSort.add(currFile);
                    } else {
                        finalSort.add(currFile);
                    }
                }
                indexes.clear();
                indexes.addAll(finalSort);
            }
        }
    }

    private Map<String, List<BinaryMapIndexReader>> getDiffsByRegion() {
        Map<String, List<BinaryMapIndexReader>> result = new HashMap<>();
        Iterator<BinaryMapIndexReader> it = indexes.iterator();
        while (it.hasNext()) {
            BinaryMapIndexReader r = it.next();
            if(r == null || r.getFile() == null) {
            	continue;
            }
            String filename = r.getFile().getName();
            if (filename.matches("([a-zA-Z-]+_)+([0-9]+_){2}[0-9]+\\.obf")) {
                String currRegionName = r.getRegionName();
                if (result.containsKey(currRegionName)) {
                    result.get(currRegionName).add(r);
                } else {
                    result.put(currRegionName, new ArrayList<>(Collections.singletonList(r)));
                }
                it.remove();
            }
        }
        return result;
    }

	public static class NameStringMatcher implements StringMatcher {

		private CollatorStringMatcher sm;

		public NameStringMatcher(String namePart, StringMatcherMode mode) {
			sm = new CollatorStringMatcher(namePart, mode);
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
			if (name == null || name.length() == 0) {
				return false;
			}
			return sm.matches(name);
		}
		
	}
	
	public int countUnknownWordsMatchMainResult(SearchResult sr) {
		return countUnknownWordsMatchInternal(sr, null, 0);
	}
	
	public int countUnknownWordsMatchMainResult(SearchResult sr, int amountMatchingWords) {
		return countUnknownWordsMatchInternal(sr, null, amountMatchingWords);
	}
	
	public int countUnknownWordsMatchMainResult(SearchResult sr, String name, int amountMatchingWords) {
		return countUnknownWordsMatchInternal(sr, name, amountMatchingWords);
	}
	
	
	private int countUnknownWordsMatchInternal(SearchResult sr, String extraName, int amountMatchingWords) {
		int r = 0;
		if (otherUnknownWords.size() > 0) {
			for (int i = 0; i < otherUnknownWords.size(); i++) {
				boolean match = false;
				if (i < amountMatchingWords - 1) {
					match = true;
				} else {
					NameStringMatcher ms = getUnknownNameStringMatcher(i);
					if (ms.matches(sr.localeName) || ms.matches(sr.otherNames)
							|| ms.matches(sr.alternateName) || ms.matches(extraName) ) {
						match = true;
					}
				}
				if (match) {
					if (sr.otherWordsMatch == null) {
						sr.otherWordsMatch = new TreeSet<>();
					}
					sr.otherWordsMatch.add(otherUnknownWords.get(i));
					r++;
				}
			}
		}
		if (amountMatchingWords > 0) {
			sr.firstUnknownWordMatches = true;
			r++;
		} else {
			boolean match =
					getFirstUnknownNameStringMatcher().matches(sr.localeName) 
					|| getFirstUnknownNameStringMatcher().matches(sr.otherNames)
					|| getFirstUnknownNameStringMatcher().matches(sr.alternateName)
					|| getFirstUnknownNameStringMatcher().matches(extraName);
			if(match) {
				r++;
			}
			sr.firstUnknownWordMatches =  match || sr.firstUnknownWordMatches;
		}
		return r;
	}
	
	public String getLastUnknownSearchWord() {
		if(otherUnknownWords.size() > 0) {
			return otherUnknownWords.get(otherUnknownWords.size() - 1);
		}
		return firstUnknownSearchWord;
	}

	
	public int getRadiusSearch(int meters, int radiusLevel) {
		int res = meters;
		for(int k = 0; k < radiusLevel; k++) {
			res = res * (k % 2 == 0 ? 2 : 3);
		}
		return res;
	}
	
	public int getRadiusSearch(int meters) {
		return getRadiusSearch(meters, getRadiusLevel() - 1);
	}
	
	public int getNextRadiusSearch(int meters) {
		return getRadiusSearch(meters, getRadiusLevel());
	}

	public static int icompare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
	
	private int getUnknownWordToSearchBuildingInd() {
		if (otherUnknownWords.size() > 0 && Algorithms.extractFirstIntegerNumber(getFirstUnknownSearchWord()) == 0) {
			int ind = 0;
			for (String wrd : otherUnknownWords) {
				ind++;
				if (Algorithms.extractFirstIntegerNumber(wrd) != 0) {
					return ind;
				}
			}
		} 
		return 0;
	}
	
	public NameStringMatcher getUnknownWordToSearchBuildingNameMatcher() {
		int ind = getUnknownWordToSearchBuildingInd();
		if(ind > 0) {
			return getUnknownNameStringMatcher(ind - 1);
		} else {
			return getFirstUnknownNameStringMatcher();
		}
	}
	
	public String getUnknownWordToSearchBuilding() {
		int ind = getUnknownWordToSearchBuildingInd();
		if(ind > 0) {
			return otherUnknownWords.get(ind - 1);
		} else {
			return firstUnknownSearchWord;
		}
	}
	
	

	private static int lengthWithoutNumbers(String s) {
		int len = 0;
		for(int k = 0; k < s.length(); k++) {
			if (s.charAt(k) >= '0' && s.charAt(k) <= '9') {

			} else {
				len++;
			}
		}
		return len;
	}

	public String getUnknownWordToSearch() {
		calcMainUnknownWordToSearch();
		return mainUnknownWordToSearch;
	}

	private boolean isTextComplete(String fullText) {
		boolean lastUnknownSearchWordComplete = false;
		if (fullText.length() > 0) {
			char ch = fullText.charAt(fullText.length() - 1);
			lastUnknownSearchWordComplete = ch == ' ' || ch == ',' || ch == '\r' || ch == '\n'
					|| ch == ';';
		}
		return lastUnknownSearchWordComplete;
	}

	
}
