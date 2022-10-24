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

import static net.osmand.search.SearchUICore.SEARCH_PRIORITY_COEF;

public class SearchResult {
	private static final double MAX_TYPE_WEIGHT = 10;
	private static final String HYPHEN = "-";
	private static final int NEAREST_METERS_LIMIT = 30000;
	private static final int COMPLETE_MATCH_COEF = 100;

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
	public int preferredZoom = 15;

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
		// if result is a complete match in the search we prioritize it higher
		double res = getSumPhraseMatchWeight() / Math.pow(MAX_TYPE_WEIGHT, getDepth() - 1);
		unknownPhraseMatchWeight = res;
		return res;
	}

	private double getSumPhraseMatchWeight() {
		// if result is a complete match in the search we prioritize it higher
		CompleteMatchResult completeMatchRes = new CompleteMatchResult();
		boolean allWordsMatched = allWordsMatched(localeName, completeMatchRes) || checkOtherNames(completeMatchRes);
		
		if (objectType == ObjectType.POI_TYPE) {
			allWordsMatched = false;
		}
		
		double res;
		if (allWordsMatched) {
			res = useCompleteMatch(completeMatchRes)
					? ObjectType.getTypeWeight(objectType) * SEARCH_PRIORITY_COEF + COMPLETE_MATCH_COEF
					: ObjectType.getTypeWeight(objectType) * SEARCH_PRIORITY_COEF;
		} else {
			res = ObjectType.getTypeWeight(objectType);
		}
		
		if (requiredSearchPhrase.getUnselectedPoiType() != null) {
			// search phrase matches poi type, then we lower all POI matches and don't check allWordsMatched
			res = ObjectType.getTypeWeight(objectType);
		}
		if (parentSearchResult != null) {
			res = res + parentSearchResult.getSumPhraseMatchWeight() / MAX_TYPE_WEIGHT;
		}
		return res;
	}
	
	private boolean useCompleteMatch(CompleteMatchResult completeMatchRes) {
		if (completeMatchRes.completeMatch) {
			if (objectType == ObjectType.CITY || objectType == ObjectType.VILLAGE) {
				return true;
			} else {
				return MapUtils.getDistance(requiredSearchPhrase.getLastTokenLocation(), this.location) <= NEAREST_METERS_LIMIT;
			}
		}
		return false;
	}
	
	private boolean checkOtherNames(CompleteMatchResult completeMatchRes) {
		if (otherNames != null) {
			for (String otherName : otherNames) {
				if (allWordsMatched(otherName, completeMatchRes)) {
					return true;
				}
			}
		}
		return false;
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

	private boolean allWordsMatched(String name, CompleteMatchResult completeMatchRes) {
		List<String> searchPhraseNames = getSearchPhraseNames();
		List<String> localResultNames;
		if (!requiredSearchPhrase.getFullSearchPhrase().contains(HYPHEN)) {
			localResultNames = SearchPhrase.splitWords(name, new ArrayList<String>(), SearchPhrase.ALLDELIMITERS_WITH_HYPHEN);
		} else {
			localResultNames = SearchPhrase.splitWords(name, new ArrayList<String>(), SearchPhrase.ALLDELIMITERS);
		}
		
		String matchedPhraseName = null;
		String matchedResultName = null;
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
					matchedPhraseName = localResultNames.get(i);
					matchedResultName = searchPhraseName;
					idxMatchedWord = i;
					break;
				}
			}
			if (!wordMatched) {
				return false;
			}
		}
		if (matchedPhraseName != null && matchedResultName != null && searchPhraseNames.size() == localResultNames.size()) {
			completeMatchRes.completeMatch = true;
		}
		
		return true;
	}
	
	static class CompleteMatchResult {
		boolean completeMatch;
		
		CompleteMatchResult(){}
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
