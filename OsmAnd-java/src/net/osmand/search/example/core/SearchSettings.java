package net.osmand.search.example.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;

// immutable object
public class SearchSettings {
	
	private LatLon originalLocation;
	private List<BinaryMapIndexReader> offlineIndexes = new ArrayList<>();
	private int radiusLevel = 1;
	private int totalLimit = -1;
	private String lang;
	
	public SearchSettings(SearchSettings s) {
		if(s != null) {
			this.radiusLevel = s.radiusLevel;
			this.lang = s.lang;
			this.totalLimit = s.totalLimit;
			this.offlineIndexes = s.offlineIndexes;
			this.originalLocation = s.originalLocation;
		}
	}
	
	public SearchSettings(List<BinaryMapIndexReader> offlineIndexes) {
		this.offlineIndexes = Collections.unmodifiableList(offlineIndexes);
	}
	
	
	public List<BinaryMapIndexReader> getOfflineIndexes() {
		return offlineIndexes;
	}
	
	public int getRadiusLevel() {
		return radiusLevel;
	}
	
	public String getLang() {
		return lang;
	}
	
	public SearchSettings setLang(String lang) {
		SearchSettings s = new SearchSettings(this);
		s.lang = lang;
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


}
