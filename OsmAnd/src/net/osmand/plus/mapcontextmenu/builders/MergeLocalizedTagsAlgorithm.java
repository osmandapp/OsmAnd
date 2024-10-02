package net.osmand.plus.mapcontextmenu.builders;

import androidx.annotation.NonNull;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MergeLocalizedTagsAlgorithm {

	private static final List<String> NAME_TAG_PREFIXES = Arrays.asList(
			"name", "int_name", "nat_name", "reg_name", "loc_name",
			"old_name", "alt_name", "short_name", "official_name", "lock_name"
	);

	private final OsmandApplication app;

	private MergeLocalizedTagsAlgorithm(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	public static Map<String, Object> execute(@NonNull OsmandApplication app,
	                                          @NonNull Map<String, String> originalDict) {
		MergeLocalizedTagsAlgorithm instance = new MergeLocalizedTagsAlgorithm(app);
		return instance.executeImpl(new HashMap<>(originalDict));
	}

	@NonNull
	private Map<String, Object> executeImpl(@NonNull Map<String, Object> originalDict) {
		Map<String, Object> resultDict = new HashMap<>();
		Map<String, Map<String, Object>> localizationsDict = new HashMap<>();

		for (String key : originalDict.keySet()) {
			processAdditionalTypeWithKey(key, convertKey(key), originalDict, localizationsDict, resultDict);
		}

		List<String> keysToUpdate = findKeysToUpdate(localizationsDict);
		for (String baseKey : keysToUpdate) {
			Map<String, Object> localizations = localizationsDict.get(baseKey);
			if (localizations != null) {
				localizations.put(baseKey, originalDict.get(baseKey));
			}
		}
		Map<String, Object> finalDict = finalizeLocalizationDict(localizationsDict);
		addRemainingEntriesFrom(resultDict, finalDict);
		return finalDict;
	}

	private void processNameTagWithKey(@NonNull String key,
	                                   @NonNull String convertedKey,
	                                   @NonNull Map<String, Object> originalDict,
	                                   @NonNull Map<String, Map<String, Object>> localizationsDict) {
		if (key.contains(":")) {
			String[] components = convertedKey.split(":");
			if (components.length == 2) {
				String baseKey = components[0];
				String localeKey = baseKey + ":" + components[1];

				Map<String, Object> nameDict = dictionaryForKey("name", localizationsDict);
				nameDict.put(localeKey, originalDict.get(convertedKey));
			}
		} else {
			Map<String, Object> nameDict = dictionaryForKey("name", localizationsDict);
			nameDict.put(convertedKey, originalDict.get(key));
		}
	}

	private void processAdditionalTypeWithKey(@NonNull String key,
	                                          @NonNull String convertedKey,
	                                          @NonNull Map<String, Object> originalDict,
	                                          @NonNull Map<String, Map<String, Object>> localizationsDict,
	                                          @NonNull Map<String, Object> resultDict) {
		MapPoiTypes poiTypes = app.getPoiTypes();
		AbstractPoiType poiType = poiTypes.getAnyPoiAdditionalTypeByKey(convertedKey);
		if (poiType != null && poiType.getLang() != null && key.contains(":")) {
			String[] components = key.split(":");
			if (components.length == 2) {
				String baseKey = components[0];
				String localeKey = baseKey + ":" + components[1];

				Map<String, Object> baseDict = dictionaryForKey(baseKey, localizationsDict);
				baseDict.put(localeKey, originalDict.get(key));
			}
		} else {
			resultDict.put(key, originalDict.get(key));
		}
	}

	@NonNull
	private Map<String, Object> dictionaryForKey(@NonNull String key, @NonNull Map<String, Map<String, Object>> dict) {
		return dict.computeIfAbsent(key, k -> new HashMap<>());
	}

	@NonNull
	private List<String> findKeysToUpdate(@NonNull Map<String, Map<String, Object>> localizationsDict) {
		List<String> keysToUpdate = new ArrayList<>();
		for (String baseKey : localizationsDict.keySet()) {
			Map<String, Object> localizations = localizationsDict.get(baseKey);
			if (localizations != null && !localizations.containsKey(baseKey)) {
				keysToUpdate.add(baseKey);
			}
		}
		return keysToUpdate;
	}

	@NonNull
	private Map<String, Object> finalizeLocalizationDict(@NonNull Map<String, Map<String, Object>> localizationsDict) {
		Map<String, Object> finalDict = new HashMap<>();

		for (String baseKey : localizationsDict.keySet()) {
			Map<String, Object> entryDict = new HashMap<>();
			Map<String, Object> localizations = localizationsDict.get(baseKey);
			entryDict.put("localizations", localizations);
			finalDict.put(baseKey, entryDict);
		}
		return finalDict;
	}

	private void addRemainingEntriesFrom(@NonNull Map<String, Object> resultDict,
	                                     @NonNull Map<String, Object> finalDict) {
		for (String key : resultDict.keySet()) {
			finalDict.putIfAbsent(key, resultDict.get(key));
		}
	}

	public boolean isNameTag(String tag) {
		for (String prefix : NAME_TAG_PREFIXES) {
			if (tag.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private String convertKey(String key) {
		return key.replace("_-_", ":");
	}
}
