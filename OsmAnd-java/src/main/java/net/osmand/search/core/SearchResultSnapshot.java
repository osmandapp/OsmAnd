package net.osmand.search.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.search.core.SearchResult.SearchResultResource;
import net.osmand.search.core.spatial.SpatialSearchResult;
import net.osmand.util.MapUtils;

/**
 * Immutable search metadata published outside of the search worker.
 *
 * Domain objects referenced by {@link #getObject()} and {@link #getRelatedObject()} are intentionally
 * not cloned. Consumers must treat those objects as read-only. Mutable collections owned by
 * {@link SearchResult} are defensively copied.
 */
public final class SearchResultSnapshot {

	private final SearchPhrase requiredSearchPhrase;
	private final SearchResultSnapshot parentSearchResult;
	private final Object object;
	private final SpatialSearchResult spatialResult;
	private final ObjectType objectType;
	private final BinaryMapIndexReader file;
	private final double priority;
	private final double priorityDistance;
	private final int spatialSearchVisibleLevel;
	private final LatLon location;
	private final int preferredZoom;
	private final String localeName;
	private final String alternateName;
	private final String addressName;
	private final String cityName;
	private final List<String> otherNames;
	private final String localeRelatedObjectName;
	private final Object relatedObject;
	private final double distRelatedObjectName;
	private final boolean impreciseCoordinates;
	private final double unknownPhraseMatchWeight;
	private final int foundWordCount;
	private final int depth;
	private final boolean allWordsEqual;
	private final boolean allWordsInPhraseAreInResult;
	private final List<String> otherWordsMatch;
	private final SearchResultResource resourceType;
	private final boolean fullPhraseEqualLocaleName;
	private final String displayText;

	private SearchResultSnapshot(SearchResult result, SearchResultSnapshot parentSearchResult) {
		requiredSearchPhrase = result.requiredSearchPhrase;
		this.parentSearchResult = parentSearchResult;
		object = result.object;
		spatialResult = result.spatialResult;
		objectType = result.objectType;
		file = result.file;
		priority = result.priority;
		priorityDistance = result.priorityDistance;
		spatialSearchVisibleLevel = result.spatialSearchVisibleLevel;
		location = result.location;
		preferredZoom = result.preferredZoom;
		localeName = result.localeName;
		alternateName = result.alternateName;
		addressName = result.addressName;
		cityName = result.cityName;
		otherNames = copyStrings(result.otherNames);
		localeRelatedObjectName = result.localeRelatedObjectName;
		relatedObject = result.relatedObject;
		distRelatedObjectName = result.distRelatedObjectName;
		impreciseCoordinates = result.hasImpreciseCoordinates();
		unknownPhraseMatchWeight = result.getUnknownPhraseMatchWeight();
		foundWordCount = result.getFoundWordCount();
		depth = result.getDepth();
		SearchResult.CheckWordsMatchCount completeMatch = result.getCompleteMatchRes();
		allWordsEqual = completeMatch != null && completeMatch.allWordsEqual;
		allWordsInPhraseAreInResult = completeMatch != null && completeMatch.allWordsInPhraseAreInResult;
		otherWordsMatch = copyStrings(result.getOtherWordsMatch());
		resourceType = result.getResourceType();
		fullPhraseEqualLocaleName = result.isFullPhraseEqualLocaleName();
		displayText = result.toString();
	}

	public static SearchResultSnapshot from(SearchResult result) {
		return from(Objects.requireNonNull(result), new IdentityHashMap<>());
	}

	static List<SearchResultSnapshot> fromAll(List<SearchResult> results) {
		Map<SearchResult, SearchResultSnapshot> snapshots = new IdentityHashMap<>();
		List<SearchResultSnapshot> result = new ArrayList<>(results.size());
		for (SearchResult searchResult : results) {
			result.add(from(Objects.requireNonNull(searchResult), snapshots));
		}
		return result;
	}

	private static SearchResultSnapshot from(SearchResult result,
	                                         Map<SearchResult, SearchResultSnapshot> snapshots) {
		SearchResultSnapshot snapshot = snapshots.get(result);
		if (snapshot != null) {
			return snapshot;
		}
		SearchResultSnapshot parent = result.parentSearchResult != null
				? from(result.parentSearchResult, snapshots)
				: null;
		snapshot = new SearchResultSnapshot(result, parent);
		snapshots.put(result, snapshot);
		return snapshot;
	}

	private static List<String> copyStrings(Collection<String> source) {
		return source == null ? null : Collections.unmodifiableList(new ArrayList<>(source));
	}

	public SearchPhrase getRequiredSearchPhrase() {
		return requiredSearchPhrase;
	}

	public SearchResultSnapshot getParentSearchResult() {
		return parentSearchResult;
	}

	public Object getObject() {
		return object;
	}

	public SpatialSearchResult getSpatialResult() {
		return spatialResult;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public BinaryMapIndexReader getFile() {
		return file;
	}

	public double getPriority() {
		return priority;
	}

	public double getPriorityDistance() {
		return priorityDistance;
	}

	public int getSpatialSearchVisibleLevel() {
		return spatialSearchVisibleLevel;
	}

	public LatLon getLocation() {
		return location;
	}

	public int getPreferredZoom() {
		return preferredZoom;
	}

	public String getLocaleName() {
		return localeName;
	}

	public String getAlternateName() {
		return alternateName;
	}

	public String getAddressName() {
		return addressName;
	}

	public String getCityName() {
		return cityName;
	}

	public List<String> getOtherNames() {
		return otherNames;
	}

	public String getLocaleRelatedObjectName() {
		return localeRelatedObjectName;
	}

	public Object getRelatedObject() {
		return relatedObject;
	}

	public double getDistRelatedObjectName() {
		return distRelatedObjectName;
	}

	public boolean hasImpreciseCoordinates() {
		return impreciseCoordinates;
	}

	public double getUnknownPhraseMatchWeight() {
		return unknownPhraseMatchWeight;
	}

	public int getFoundWordCount() {
		return foundWordCount;
	}

	public int getDepth() {
		return depth;
	}

	public boolean isAllWordsEqual() {
		return allWordsEqual;
	}

	public boolean isAllWordsInPhraseAreInResult() {
		return allWordsInPhraseAreInResult;
	}

	public List<String> getOtherWordsMatch() {
		return otherWordsMatch;
	}

	public SearchResultResource getResourceType() {
		return resourceType;
	}

	public boolean isFullPhraseEqualLocaleName() {
		return fullPhraseEqualLocaleName;
	}

	public double getSearchDistance(LatLon origin) {
		double distance = origin != null && location != null ? MapUtils.getDistance(origin, location) : 0;
		return priority - 1 / (1 + priorityDistance * distance);
	}

	public double getSearchDistance(LatLon origin, double priorityDistance) {
		double distance = origin != null && location != null ? MapUtils.getDistance(origin, location) : 0;
		return priority - 1 / (1 + priorityDistance * distance);
	}

	@Override
	public String toString() {
		return displayText;
	}
}
