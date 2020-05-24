package net.osmand.search.core;

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

import java.util.Collection;

public class SearchResult {
	// search phrase that makes search result valid 
	public SearchPhrase requiredSearchPhrase;
	
	public Object object;
	public ObjectType objectType;
	public BinaryMapIndexReader file;
	
	public double priority;
	public double priorityDistance;
	public String wordsSpan ;
	public SearchResult parentSearchResult;
	public Collection<String> otherWordsMatch = null;
	public boolean firstUnknownWordMatches = true;
	public boolean unknownPhraseMatches = false;

	public SearchResult(SearchPhrase sp) {
		this.requiredSearchPhrase = sp;
	}

	public double getUnknownPhraseMatchWeight() {
		// if result is a complete match in the search we prioritize it highers
		double res  = 0;
		if (unknownPhraseMatches) {
			res = ObjectType.getTypeWeight(objectType);
		}
		if (parentSearchResult != null) {
			// 5 is a maximum type
			res += parentSearchResult.getUnknownPhraseMatchWeight() / 5;
		}
		return res;
	}

	public int getFoundWordCount() {
		int inc = 0;
		if (firstUnknownWordMatches) {
			inc = 1;
		}
		if (otherWordsMatch != null) {
			inc += otherWordsMatch.size();
		}
		if (parentSearchResult != null) {
			inc += parentSearchResult.getFoundWordCount();
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
	
	public LatLon location;
	public int preferredZoom = 15;
	public String localeName;
	public String alternateName;
	
	public Collection<String> otherNames;
	
	public String localeRelatedObjectName;
	public Object relatedObject;
	public double distRelatedObjectName;

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
