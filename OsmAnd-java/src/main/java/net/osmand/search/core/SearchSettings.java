package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
	private SearchExportSettings exportSettings; // = new SearchExportSettings(true, true, -1);

	public SearchSettings(SearchSettings s) {
		if(s != null) {
			this.radiusLevel = s.radiusLevel;
			this.lang = s.lang;
			this.transliterateIfMissing = s.transliterateIfMissing;
			this.totalLimit = s.totalLimit;
			this.offlineIndexes = s.offlineIndexes;
			this.originalLocation = s.originalLocation;
			this.searchTypes = s.searchTypes;
			this.emptyQueryAllowed = s.emptyQueryAllowed;
			this.sortByName = s.sortByName;
			this.exportSettings = s.exportSettings;
		}
	}
	
	public SearchSettings(List<? extends BinaryMapIndexReader> offlineIndexes) {
		this.offlineIndexes = Collections.unmodifiableList(offlineIndexes);
	}

	public List<BinaryMapIndexReader> getOfflineIndexes() {
		return offlineIndexes;
	}

	public void setOfflineIndexes(List<? extends BinaryMapIndexReader> offlineIndexes) {
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

	public SearchExportSettings getExportSettings() {
		return exportSettings;
	}

	public SearchSettings setExportSettings(SearchExportSettings exportSettings) {
		SearchSettings s = new SearchSettings(this);
		this.exportSettings = exportSettings;
		return s;
	}

	public boolean isExportObjects() {
		return exportSettings != null;
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

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		if (originalLocation != null) {
			json.put("lat", String.format(Locale.US, "%.5f", originalLocation.getLatitude()));
			json.put("lon", String.format(Locale.US, "%.5f", originalLocation.getLongitude()));
		}
		json.put("radiusLevel", radiusLevel);
		json.put("totalLimit", totalLimit);
		json.put("lang", lang);
		json.put("transliterateIfMissing", transliterateIfMissing);
		json.put("emptyQueryAllowed", emptyQueryAllowed);
		json.put("sortByName", sortByName);
		if (searchTypes != null && searchTypes.length > 0) {
			JSONArray searchTypesArr = new JSONArray();
			for (ObjectType type : searchTypes) {
				searchTypesArr.put(type.name());
			}
			json.put("searchTypes", searchTypes);
		}

		return json;
	}

	public static SearchSettings parseJSON(JSONObject json) {
		SearchSettings s = new SearchSettings(new ArrayList<BinaryMapIndexReader>());
		if (json.has("lat") && json.has("lon")) {
			s.originalLocation = new LatLon(json.getDouble("lat"), json.getDouble("lon"));
		}
		s.radiusLevel = json.optInt("radiusLevel", 1);
		s.totalLimit = json.optInt("totalLimit", -1);
		s.transliterateIfMissing = json.optBoolean("transliterateIfMissing", false);
		s.emptyQueryAllowed = json.optBoolean("emptyQueryAllowed", false);
		s.sortByName = json.optBoolean("sortByName", false);
		if (json.has("lang")) {
			s.lang = json.getString("lang");
		}
		if (json.has("searchTypes")) {
			JSONArray searchTypesArr = json.getJSONArray("searchTypes");
			ObjectType[] searchTypes = new ObjectType[searchTypesArr.length()];
			for (int i = 0; i < searchTypesArr.length(); i++) {
				String name = searchTypesArr.getString(i);
				searchTypes[i] = ObjectType.valueOf(name);
			}
			s.searchTypes = searchTypes;
		}
		return s;
	}
}
