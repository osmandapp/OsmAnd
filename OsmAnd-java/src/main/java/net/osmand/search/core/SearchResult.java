package net.osmand.search.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
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
	private static final int NEAREST_METERS_LIMIT = 30000;
	
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
	public Collection<String> otherNames;

	public String localeRelatedObjectName;
	public Object relatedObject;
	public double distRelatedObjectName;

	private double unknownPhraseMatchWeight = 0;

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
		unknownPhraseMatchWeight = getSumPhraseMatchWeight() / Math.pow(MAX_PHRASE_WEIGHT_TOTAL, getDepth() - 1);
		return unknownPhraseMatchWeight;
	}

	private double getSumPhraseMatchWeight() {
		double res = ObjectType.getTypeWeight(objectType);
		if (requiredSearchPhrase.getUnselectedPoiType() != null) {
			// search phrase matches poi type, then we lower all POI matches and don't check allWordsMatched
		} else if (objectType == ObjectType.POI_TYPE) {
			// don't overload with poi types
		} else {
			CheckWordsMatchCount completeMatchRes = new CheckWordsMatchCount();
			if (allWordsMatched(localeName, completeMatchRes)) {
				// ignore other names
			} else if (otherNames != null) {
				for (String otherName : otherNames) {
					if (allWordsMatched(otherName, completeMatchRes)) {
						break;
					}
				}
			}
			// if all words from search phrase match (<) the search result words - we prioritize it higher
			if (completeMatchRes.allWordsInPhraseAreInResult) {
				res = getPhraseWeightForCompleteMatch(completeMatchRes);
			}
		}
		if (parentSearchResult != null) {
			// parent search result should not change weight of current result, so we divide by MAX_TYPES_BASE_10^2
			res = res + parentSearchResult.getSumPhraseMatchWeight() / (MAX_PHRASE_WEIGHT_TOTAL);
		}
		return res;
	}

	private double getPhraseWeightForCompleteMatch(CheckWordsMatchCount completeMatchRes) {
		double res = ObjectType.getTypeWeight(objectType) * MAX_TYPES_BASE_10;
		// if all words from search phrase == the search result words - we prioritize it even higher
		if (completeMatchRes.allWordsEqual && requiredSearchPhrase.getLastTokenLocation() != null && this.location != null) {
			boolean closeDistance = MapUtils.getDistance(requiredSearchPhrase.getLastTokenLocation(),
					this.location) <= NEAREST_METERS_LIMIT;
			if (objectType == ObjectType.CITY || objectType == ObjectType.VILLAGE || closeDistance) {
				res = ObjectType.getTypeWeight(objectType) * MAX_TYPES_BASE_10 + MAX_PHRASE_WEIGHT_TOTAL / 2;
			}
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

	private boolean allWordsMatched(String name, CheckWordsMatchCount cnt) {
		List<String> searchPhraseNames = getSearchPhraseNames();
		List<String> localResultNames;
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
}
