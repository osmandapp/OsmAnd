package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// immutable object
public class SearchSettings {

	private LatLon originalLocation;
	private List<BinaryMapIndexReader> offlineIndexes = new ArrayList<>();
	private int radiusLevel = 1;
	private int totalLimit = -1;
	private String lang;
	private boolean transliterateIfMissing;
	private ObjectType[] searchTypes;
	private boolean emptyQueryAllowed;
	private boolean sortByName;

	public SearchSettings(SearchSettings s) {
		if(s != null) {
			this.radiusLevel = s.radiusLevel;
			this.lang = s.lang;
			this.totalLimit = s.totalLimit;
			this.offlineIndexes = s.offlineIndexes;
			this.originalLocation = s.originalLocation;
			this.searchTypes = s.searchTypes;
			this.emptyQueryAllowed = s.emptyQueryAllowed;
			this.sortByName = s.sortByName;
		}
	}
	
	public SearchSettings(List<BinaryMapIndexReader> offlineIndexes) {
		this.offlineIndexes = Collections.unmodifiableList(offlineIndexes);
	}
	
	
	public List<BinaryMapIndexReader> getOfflineIndexes() {
		return offlineIndexes;
	}

	public void setOfflineIndexes(List<BinaryMapIndexReader> offlineIndexes) {
		this.offlineIndexes = Collections.unmodifiableList(offlineIndexes);
	}

	public int getRadiusLevel() {
		return radiusLevel;
	}
	
	public String getLang() {
		return lang;
	}
	
	public SearchSettings setLang(String lang, boolean transliterateIfMissing) {
		SearchSettings s = new SearchSettings(this);
		s.lang = lang;
		s.transliterateIfMissing = transliterateIfMissing;
		return s;
	}
	
	public SearchSettings setRadiusLevel(int radiusLevel) {
		SearchSettings s = new SearchSettings(this);
		s.radiusLevel = radiusLevel;
		return s;
	}
	
	public int getTotalLimit() {
		return totalLimit;
	}
	
	public SearchSettings setTotalLimit(int totalLimit) {
		SearchSettings s = new SearchSettings(this);
		s.totalLimit = totalLimit;
		return s;
	}
	
	public LatLon getOriginalLocation() {
		return originalLocation;
	}

	public SearchSettings setOriginalLocation(LatLon l) {
		SearchSettings s = new SearchSettings(this);
		s.originalLocation = l;
		return s;
	}

	public boolean isTransliterate() {
		return transliterateIfMissing;
	}

	public ObjectType[] getSearchTypes() {
		return searchTypes;
	}

	public boolean isCustomSearch() {
		return searchTypes != null;
	}

	public SearchSettings setSearchTypes(ObjectType... searchTypes) {
		SearchSettings s = new SearchSettings(this);
		s.searchTypes = searchTypes;
		return s;
	}

	public SearchSettings resetSearchTypes() {
		SearchSettings s = new SearchSettings(this);
		s.searchTypes = null;
		return s;
	}

	public boolean isEmptyQueryAllowed() {
		return emptyQueryAllowed;
	}

	public SearchSettings setEmptyQueryAllowed(boolean emptyQueryAllowed) {
		SearchSettings s = new SearchSettings(this);
		s.emptyQueryAllowed = emptyQueryAllowed;
		return s;
	}

	public boolean isSortByName() {
		return sortByName;
	}

	public SearchSettings setSortByName(boolean sortByName) {
		SearchSettings s = new SearchSettings(this);
		s.sortByName = sortByName;
		return s;
	}

	public boolean hasCustomSearchType(ObjectType type) {
		if (searchTypes != null) {
			for (ObjectType t : searchTypes) {
				if (t == type) {
					return true;
				}
			}
		}
		return false;
	}
}
