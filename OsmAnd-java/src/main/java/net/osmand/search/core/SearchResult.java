package net.osmand.search.core;

import static net.osmand.search.core.SearchCoreFactory.PREFERRED_DEFAULT_ZOOM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;


public class SearchResult {

	public static final String DELIMITER = " ";
	private static final String HYPHEN = "-";
	static final int NEAREST_METERS_LIMIT = 30000;
	
	// MAX_TYPES_BASE_10 should be > ObjectType.getTypeWeight(objectType) = 5
	public static final double MAX_TYPES_BASE_10 = 10;
	// MAX_PHRASE_WEIGHT_TOTAL should be  > getSumPhraseMatchWeight
	public static final double MAX_PHRASE_WEIGHT_TOTAL = MAX_TYPES_BASE_10 * MAX_TYPES_BASE_10;
	
	private static final int MIN_ELO_RATING = 1800;
	private static final int MAX_ELO_RATING = 4300;

	// search phrase that makes search result valid
	public SearchPhrase requiredSearchPhrase;

	// internal package fields (used for sorting)
	public SearchResult parentSearchResult;
	String wordsSpan ;
	boolean firstUnknownWordMatches;
	Collection<String> otherWordsMatch = null;

	public Object object;
	public ObjectType objectType;
	public BinaryMapIndexReader file;

	public double priority;
	public double priorityDistance;

	public LatLon location;
	public int preferredZoom = PREFERRED_DEFAULT_ZOOM;

	public String localeName;
	public String alternateName;
	public String addressName;
	public String cityName;
	public Collection<String> otherNames;

	public String localeRelatedObjectName;
	public Object relatedObject;
	public double distRelatedObjectName;

	private boolean impreciseCoordinates;
	private double unknownPhraseMatchWeight = 0;
	private CheckWordsMatchCount completeMatchRes = null;

	public enum SearchResultResource {
		DETAILED,
		WIKIPEDIA,
		BASEMAP,
		TRAVEL
	}

	private SearchResultResource searchResultResource;

	public SearchResult() {
		this.requiredSearchPhrase = SearchPhrase.emptyPhrase();
	}

	public SearchResult(SearchPhrase sp) {
		this.requiredSearchPhrase = sp;
	}

	public boolean hasImpreciseCoordinates() {
		return impreciseCoordinates;
	}

	public void setImpreciseCoordinates(boolean imprecise) {
		this.impreciseCoordinates = imprecise;
	}

	// maximum corresponds to the top entry
	public double getUnknownPhraseMatchWeight() {
		if (unknownPhraseMatchWeight != 0) {
			return unknownPhraseMatchWeight;
		}
		unknownPhraseMatchWeight = getSumPhraseMatchWeight(null);
		return unknownPhraseMatchWeight;
	}
	
	public CheckWordsMatchCount getCompleteMatchRes() {
		if (completeMatchRes != null) {
			return completeMatchRes;
		}
		getSumPhraseMatchWeight(null);
		return completeMatchRes;
	}


	private double getSumPhraseMatchWeight(SearchResult exactResult) {
		double res = getTypeWeight(exactResult, objectType);
		completeMatchRes = new CheckWordsMatchCount();
		if (requiredSearchPhrase.getUnselectedPoiType() != null) {
			// search phrase matches poi type, then we lower all POI matches and don't check allWordsMatched
		} else if (objectType == ObjectType.POI_TYPE) {
			// don't overload with poi types
		} else {
			boolean matched = localeName != null && allWordsMatched(localeName, exactResult, completeMatchRes);
			// incorrect fix
//			if (!matched && object instanceof Street s) { // parentSearchResult == null &&
//				matched = allWordsMatched(localeName + " " + s.getCity().getName(requiredSearchPhrase.getSettings().getLang()), exactResult, completeMatchRes);
//			}
			if (!matched && alternateName != null && !Algorithms.objectEquals(cityName, alternateName)) {
				matched = allWordsMatched(alternateName, exactResult, completeMatchRes);
			}
			if (!matched && otherNames != null) {
				for (String otherName : otherNames) {
					if (allWordsMatched(otherName, exactResult, completeMatchRes)) {
						matched = true;
						break;
					}
				}
			}
			City selectedCity = null;
			if (exactResult != null && exactResult.object instanceof Street s) {
				selectedCity = s.getCity();
			} else if (exactResult != null && 
					exactResult.parentSearchResult != null && exactResult.parentSearchResult.object instanceof Street s) {
				selectedCity = s.getCity();
			}
			if (matched && selectedCity != null && object instanceof City c) {
				// city don't match because of boundary search -> lower priority
				if (!Algorithms.objectEquals(selectedCity.getName(), c.getName())) {
					matched = false;
					// for unmatched cities calculate how close street is to boundary
					// 1 - very close, 0 - very far
					int[] bbox31 = selectedCity.getBbox31();
					LatLon latlon = selectedCity.getLocation();
					if (bbox31 != null) {
						// even center is shifted probably best to do combination of bbox & center
						double lon = MapUtils.get31LongitudeX(bbox31[0] / 2 + bbox31[2] / 2);
						double lat = MapUtils.get31LatitudeY(bbox31[1] / 2 + bbox31[3] / 2);
						latlon = new LatLon(lat, lon);
					}
					res += 100 / Math.max(100, MapUtils.getDistance(location, latlon));
				}
			}
			// if all words from search phrase match (<) the search result words - we prioritize it higher
			if (matched) {
				res = getPhraseWeightForCompleteMatch(exactResult, completeMatchRes);
			}
			if (object instanceof Amenity a) {
				int elo = a.getTravelEloNumber();
				if (elo > MIN_ELO_RATING) {
					double rat = ((double)elo - MIN_ELO_RATING) / (MAX_ELO_RATING - MIN_ELO_RATING);
					res += rat * MAX_PHRASE_WEIGHT_TOTAL * 2 / 3; 
				}
			}
		}
		if (parentSearchResult != null) {
			// parent search result should not change weight of current result, so we divide by MAX_TYPES_BASE_10^2
			res = res + parentSearchResult.getSumPhraseMatchWeight(exactResult == null ? this : exactResult) / (MAX_PHRASE_WEIGHT_TOTAL);
		}
		return res;
	}

	private double getPhraseWeightForCompleteMatch(SearchResult exactResult, CheckWordsMatchCount completeMatchRes) {
		double res = getTypeWeight(exactResult, objectType) * MAX_TYPES_BASE_10;
		// if all words from search phrase == the search result words - we prioritize it even higher
		if (completeMatchRes.allWordsEqual) {
			boolean closeDistance = requiredSearchPhrase.getLastTokenLocation() != null && this.location != null 
					&& MapUtils.getDistance(requiredSearchPhrase.getLastTokenLocation(), this.location) <= NEAREST_METERS_LIMIT;
			if (objectType != ObjectType.POI || closeDistance) {
				res = getTypeWeight(exactResult, objectType) * MAX_TYPES_BASE_10 + MAX_PHRASE_WEIGHT_TOTAL / 2;
			}
		}
		return res;
	}
	
	

	private double getTypeWeight(SearchResult exactResult, ObjectType ot) {
		if (exactResult == null && !requiredSearchPhrase.isLikelyAddressSearch()) {
			return 1;
		}
		return ObjectType.getTypeWeight(ot);
	}

	public int getDepth() {
		if (parentSearchResult != null) {
			return 1 + parentSearchResult.getDepth();
		}
		return 1;
	}

	public int getFoundWordCount() {
		int inc = getSelfWordCount();
		if (parentSearchResult != null) {
			inc += parentSearchResult.getFoundWordCount();
		}
		return inc;
	}

	private boolean allWordsMatched(String name, SearchResult exactResult, CheckWordsMatchCount cnt) {
		List<String> searchPhraseNames = getSearchPhraseNames();
		name = CollatorStringMatcher.alignChars(name);
		List<String> localResultNames;
		if (!Algorithms.isEmpty(name) && name.indexOf('(') != -1) {
			name = SearchPhrase.stripBraces(name);
		}
		if (!requiredSearchPhrase.getFullSearchPhrase().contains(HYPHEN)) {
			// we split '-' words in result, so user can input same without '-'
			localResultNames = SearchPhrase.splitWords(name, new ArrayList<String>(), SearchPhrase.ALLDELIMITERS_WITH_HYPHEN);
		} else {
			localResultNames = SearchPhrase.splitWords(name, new ArrayList<String>(), SearchPhrase.ALLDELIMITERS);
		}

		boolean wordMatched;
		if (searchPhraseNames.isEmpty()) {
			return false;
		}
		while (exactResult != null && exactResult != this) {
			List<String> lst = exactResult.getSearchPhraseNames();
			for (String l : lst) {
				int i = searchPhraseNames.indexOf(l);
				if (i != -1) {
					searchPhraseNames.remove(i);
				}
			}
			exactResult = exactResult.parentSearchResult;
		}
		
		
		int idxMatchedWord = -1;
		for (String searchPhraseName : searchPhraseNames) {
			wordMatched = false;
			for (int i = idxMatchedWord + 1; i < localResultNames.size(); i++) {
				int r = requiredSearchPhrase.getCollator().compare(searchPhraseName, localResultNames.get(i));
				if (r == 0) {
					wordMatched = true;
					idxMatchedWord = i;
					break;
				}
			}
			if (!wordMatched) {
//				cnt.allWordsInPhraseAreInResult = false;
				return false;
			}
		}
		if (searchPhraseNames.size() == localResultNames.size()) {
			cnt.allWordsEqual = true;
		}
		cnt.allWordsInPhraseAreInResult = true;
		return true;
	}
	
	public static class CheckWordsMatchCount {
		public boolean allWordsEqual;
		public boolean allWordsInPhraseAreInResult;
	}

	private List<String> getSearchPhraseNames() {
		List<String> searchPhraseNames = new ArrayList<>();

		String fw = requiredSearchPhrase.getFirstUnknownSearchWord();
		List<String> ow = requiredSearchPhrase.getUnknownSearchWords();
		if (fw != null && fw.length() > 0) {
			searchPhraseNames.add(CollatorStringMatcher.alignChars(fw));
		}
		if (ow != null) {
			for(String o : ow) {
				searchPhraseNames.add(CollatorStringMatcher.alignChars(o));
			}
			
		}
		// when parent result was recreated with same phrase (it doesn't have preselected word)
		// SearchCoreFactory.subSearchApiOrPublish
		if (parentSearchResult != null && requiredSearchPhrase == parentSearchResult.requiredSearchPhrase
				&& parentSearchResult.getOtherWordsMatch() != null) {
			for (String s : parentSearchResult.getOtherWordsMatch()) {
				int i = searchPhraseNames.indexOf(CollatorStringMatcher.alignChars(s));
				if (i != -1) {
					searchPhraseNames.remove(i);
				}
			}
		}

		return searchPhraseNames;
	}

	private int getSelfWordCount() {
		int inc = 0;
		if (firstUnknownWordMatches) {
			inc = 1;
		}
		if (otherWordsMatch != null) {
			inc += otherWordsMatch.size();
		}
		return inc;
	}

	public double getSearchDistance(LatLon location) {
		double distance = 0;
		if (location != null && this.location != null) {
			distance = MapUtils.getDistance(location, this.location);
		}
		return priority - 1 / (1 + priorityDistance * distance);
	}

	public double getSearchDistance(LatLon location, double pd) {
		double distance = 0;
		if (location != null && this.location != null) {
			distance = MapUtils.getDistance(location, this.location);
		}
		return priority - 1 / (1 + pd * distance);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if (!Algorithms.isEmpty(localeName)) {
			b.append(localeName);
		}
		if (!Algorithms.isEmpty(localeRelatedObjectName)) {
			if (b.length() > 0) {
				b.append(", ");
			}
			b.append(localeRelatedObjectName);
			if (relatedObject instanceof Street) {
				Street street = (Street) relatedObject;
				City city = street.getCity();
				if (city != null) {
					b.append(", ").append(city.getName(requiredSearchPhrase.getSettings().getLang(),
							requiredSearchPhrase.getSettings().isTransliterate()));
				}
			}
		} else if (object instanceof AbstractPoiType) {
			if (b.length() > 0) {
				b.append(" ");
			}
			AbstractPoiType poiType = (AbstractPoiType) object;
			if (poiType instanceof PoiCategory) {
				b.append("(Category)");
			} else if (poiType instanceof PoiFilter) {
				b.append("(Filter)");
			} else if (poiType instanceof PoiType) {
				PoiType p = (PoiType) poiType;
				final AbstractPoiType parentType = p.getParentType();
				if (parentType != null) {
					final String translation = parentType.getTranslation();
					b.append("(").append(translation);
					if (parentType instanceof PoiCategory) {
						b.append(" / Category)");
					} else if (parentType instanceof PoiFilter) {
						b.append(" / Filter)");
					} else if (parentType instanceof PoiType) {
						PoiType pp = (PoiType) poiType;
						PoiFilter filter = pp.getFilter();
						PoiCategory category = pp.getCategory();
						if (filter != null && !filter.getTranslation().equals(translation)) {
							b.append(" / ").append(filter.getTranslation()).append(")");
						} else if (category != null && !category.getTranslation().equals(translation)) {
							b.append(" / ").append(category.getTranslation()).append(")");
						} else {
							b.append(")");
						}
					}
				} else if (p.getFilter() != null) {
					b.append("(").append(p.getFilter().getTranslation()).append(")");
				} else if (p.getCategory() != null) {
					b.append("(").append(p.getCategory().getTranslation()).append(")");
				}
			}
		}
		return b.toString();
	}

	public SearchResultResource getResourceType() {
		if (searchResultResource == null) {
			searchResultResource = SearchResultResource.DETAILED;
			if (object != null && object instanceof Amenity amenity) {
				searchResultResource = amenity.getType().isWiki() ? SearchResultResource.WIKIPEDIA : searchResultResource;
			}
			if (file != null) {
				searchResultResource = file.getFile().getName().contains(".travel") ? SearchResultResource.TRAVEL : searchResultResource;
				searchResultResource = file.isBasemap() ? SearchResultResource.BASEMAP : searchResultResource;
			}
		}
		return searchResultResource;
	}

	public Collection<String> getOtherWordsMatch() {
		return otherWordsMatch;
	}

	public void setOtherWordsMatch(Collection<String> set) {
		otherWordsMatch = set;
	}

	public void setUnknownPhraseMatchWeight(double weight) {
		unknownPhraseMatchWeight = weight;
	}

	public boolean isFullPhraseEqualLocaleName() {
		return requiredSearchPhrase.getFullSearchPhrase().equalsIgnoreCase(localeName);
	}
	
	
	public List<String> filterUnknownSearchWord(List<String> leftUnknownSearchWords) {
		if (leftUnknownSearchWords == null) {
			leftUnknownSearchWords = new ArrayList<String>(requiredSearchPhrase.getUnknownSearchWords());
			leftUnknownSearchWords.add(0, requiredSearchPhrase.getFirstUnknownSearchWord());
		}
		if (firstUnknownWordMatches) {
			leftUnknownSearchWords.remove(requiredSearchPhrase.getFirstUnknownSearchWord());
		}
		if (otherWordsMatch != null) {
//			removeAll(res.otherWordsMatch); // incorrect 
			for (String otherWord : otherWordsMatch) {
				int ind = firstUnknownWordMatches ? leftUnknownSearchWords.indexOf(otherWord)
						: leftUnknownSearchWords.lastIndexOf(otherWord);
				if (ind != -1) {
					leftUnknownSearchWords.remove(ind); // remove 1 by 1
				}
			}
		}
		
		return leftUnknownSearchWords;
	}
	
	
	public void restoreBraceNames(String[] backup) {
		if (backup != null) {
			if (backup[0] != null) {
				localeName = backup[0];
			}
			if (backup[1] != null) {
				localeName = backup[1];
			}
			if (backup.length > 2) {
				List<String> oth = new ArrayList<String>();
				for (int i = 2; i < backup.length; i++) {
					oth.add(backup[i]);
				}
				otherNames = oth;
			}
		}
	}
	
	public String[] stripBracesNames() {
		char[] brace = new char[] { '(' };
		boolean noBrace = true;
		noBrace &= !Algorithms.containsChar(localeName, brace);
		noBrace &= !Algorithms.containsChar(alternateName, brace);
		if (otherNames != null) {
			for (String o : otherNames) {
				noBrace &= !Algorithms.containsChar(o, brace);
				if (!noBrace) {
					break;
				}
			}
		}
		if (noBrace) {
			return null;
		}
		
		String[] backup = new String[2 + (otherNames == null ? 0 : otherNames.size())];
		if (localeName != null) {
			backup[0] = localeName;
			localeName = SearchPhrase.stripBraces(localeName);
		}
		if (alternateName != null) {
			backup[1] = alternateName;
			alternateName = SearchPhrase.stripBraces(alternateName);
		}
		if (otherNames != null) {
			Iterator<String> it = otherNames.iterator();
			List<String> oth = new ArrayList<String>();
			for (int i = 0; i < otherNames.size(); i++) {
				String o = SearchPhrase.stripBraces(it.next());
				backup[2 + i] = o;
				oth.add(o);
			}
			otherNames = oth;
		}
		return backup;
	}
}
