package net.osmand.search.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import static net.osmand.search.core.SearchCoreFactory.PREFERRED_DEFAULT_ZOOM;


public class SearchResult {

	public static final String DELIMITER = " ";
	private static final String HYPHEN = "-";
	static final int NEAREST_METERS_LIMIT = 30000;
	
	// MAX_TYPES_BASE_10 should be > ObjectType.getTypeWeight(objectType) = 5
	public static final double MAX_TYPES_BASE_10 = 10;
	// MAX_PHRASE_WEIGHT_TOTAL should be  > getSumPhraseMatchWeight
	public static final double MAX_PHRASE_WEIGHT_TOTAL = MAX_TYPES_BASE_10 * MAX_TYPES_BASE_10;

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
	public String cityName;
	public Collection<String> otherNames;

	public String localeRelatedObjectName;
	public Object relatedObject;
	public double distRelatedObjectName;

	private double unknownPhraseMatchWeight = 0;

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

	// maximum corresponds to the top entry
	public double getUnknownPhraseMatchWeight() {
		if (unknownPhraseMatchWeight != 0) {
			return unknownPhraseMatchWeight;
		}
		// normalize number to get as power, so we get numbers > 1
		unknownPhraseMatchWeight = getSumPhraseMatchWeight(null);
//		System.out.println(" ---- " + this + " " + unknownPhraseMatchWeight);
//		unknownPhraseMatchWeight /= Math.pow(MAX_PHRASE_WEIGHT_TOTAL, getDepth() - 1);
		return unknownPhraseMatchWeight;
	}

	private double getSumPhraseMatchWeight(SearchResult exactResult) {
		// FIXME unit tests
		// 101 South Main Street Ashley (101, South Main Street, Luzerne)
		// 211 Walnut Street Elmira (211, South Walnut Street, Elmira)
		
		// 423 Waverly Street Waverly 
		// 15 Blumenstraße Esslingen  (alt:name matchAddressByName) 
		// 12 Friedhofstraße Stuttgart (new name - Goslarer Straße)
		// 4 Beutelsbacher Straße Weinstadt (distance to boundary)
		// 26 Panoramastraße Weinstadt (distance to boundary)
		// 24 Kelterstraße Kernen im Remstal (distance to boundary)  
		double res = ObjectType.getTypeWeight(objectType);
		if (requiredSearchPhrase.getUnselectedPoiType() != null) {
			// search phrase matches poi type, then we lower all POI matches and don't check allWordsMatched
		} else if (objectType == ObjectType.POI_TYPE) {
			// don't overload with poi types
		} else {
			CheckWordsMatchCount completeMatchRes = new CheckWordsMatchCount();
			boolean matched = false;
			matched = allWordsMatched(localeName, exactResult, completeMatchRes);
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
				res = getPhraseWeightForCompleteMatch(completeMatchRes);
//				System.out.println(objectType + " " + localeName + " " + localeRelatedObjectName + "  "+ res);
			} else {
//				System.out.println(objectType + " ! " + localeName + " " + localeRelatedObjectName + "  "+ res);
			}
		}
		if (parentSearchResult != null) {
			// parent search result should not change weight of current result, so we divide by MAX_TYPES_BASE_10^2
			res = res + parentSearchResult.getSumPhraseMatchWeight(exactResult == null ? this : exactResult) / (MAX_PHRASE_WEIGHT_TOTAL);
		}
		return res;
	}

	private double getPhraseWeightForCompleteMatch(CheckWordsMatchCount completeMatchRes) {
		double res = ObjectType.getTypeWeight(objectType) * MAX_TYPES_BASE_10;
		// if all words from search phrase == the search result words - we prioritize it even higher
		if (completeMatchRes.allWordsEqual && requiredSearchPhrase.getLastTokenLocation() != null && this.location != null) {
//			boolean closeDistance = MapUtils.getDistance(requiredSearchPhrase.getLastTokenLocation(),
//					this.location) <= NEAREST_METERS_LIMIT;
//			if (objectType == ObjectType.CITY || objectType == ObjectType.VILLAGE || closeDistance) {
				res = ObjectType.getTypeWeight(objectType) * MAX_TYPES_BASE_10 + MAX_PHRASE_WEIGHT_TOTAL / 2;
//			}
		}
		return res;
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
	
	public boolean hasObjectTypePresent(ObjectType type) {
		if (objectType == type) {
			return true;
		}
		if (parentSearchResult != null) {
			return parentSearchResult.hasObjectTypePresent(type);
		}
		return false;
	}

	private boolean allWordsMatched(String name, SearchResult exactResult, CheckWordsMatchCount cnt) {
		List<String> searchPhraseNames = getSearchPhraseNames();
		List<String> localResultNames;
		if (name.indexOf('(') != -1) {
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
				searchPhraseNames.remove(l);
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
				return false;
			}
		}
		if (searchPhraseNames.size() == localResultNames.size()) {
			cnt.allWordsEqual = true;
		}
		cnt.allWordsInPhraseAreInResult = true;
		return true;
	}
	
	static class CheckWordsMatchCount {
		boolean allWordsEqual;
		boolean allWordsInPhraseAreInResult;
	}

	private List<String> getSearchPhraseNames() {
		List<String> searchPhraseNames = new ArrayList<>();

		String fw = requiredSearchPhrase.getFirstUnknownSearchWord();
		List<String> ow = requiredSearchPhrase.getUnknownSearchWords();
		if (fw != null && fw.length() > 0) {
			searchPhraseNames.add(fw);
		}
		if (ow != null) {
			searchPhraseNames.addAll(ow);
		}
		// when parent result was recreated with same phrase (it doesn't have preselected word)
		// SearchCoreFactory.subSearchApiOrPublish
		if (parentSearchResult != null && requiredSearchPhrase == parentSearchResult.requiredSearchPhrase
				&& parentSearchResult.getOtherWordsMatch() != null) {
			for (String s : parentSearchResult.getOtherWordsMatch()) {
				searchPhraseNames.remove(s);
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
				leftUnknownSearchWords.remove(otherWord); // remove 1 by 1
			}
		}
		
		return leftUnknownSearchWords;
	}
}
