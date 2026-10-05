package net.osmand.plus;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapPoiTypesTranslator implements PoiTranslator {

	private static final Log LOG = PlatformUtil.getLog(MapPoiTypesTranslator.class);

	// resource ids do not change within a process
	private static final Map<String, Integer> STRING_IDS = new ConcurrentHashMap<>();

	private final OsmandApplication app;
	private final Resources enResources;
	private final Resources localizedResources;

	public MapPoiTypesTranslator(@NonNull OsmandApplication app) {
		this(app, Locale.getDefault());
	}

	public MapPoiTypesTranslator(@NonNull OsmandApplication app, @NonNull Locale locale) {
		this.app = app;

		LocaleHelper helper = app.getLocaleHelper();
		enResources = helper.getLocalizedResources(new Locale("en"));
		localizedResources = helper.getLocalizedResources(locale);
	}

	@Override
	public String getTranslation(AbstractPoiType type) {
		AbstractPoiType baseLangType = type.getBaseLangType();
		if (baseLangType != null) {
			String translation = getTranslation(baseLangType);
			String langTranslation = " (" + AndroidUtils.getLangTranslation(app, type.getLang()).toLowerCase() + ")";
			if (translation != null) {
				return translation + langTranslation;
			} else {
				return app.poiTypes.getBasePoiName(baseLangType) + langTranslation;
			}
		}
		return getTranslation(type.getFormattedKeyName());
	}

	@Override
	public String getTranslation(String keyName) {
		try {
			int in = getStringId(keyName);
			if (in != 0) {
				String val = localizedResources.getString(in);
				if (val != null) {
					int ind = val.indexOf(';');
					if (ind > 0) {
						return val.substring(0, ind);
					}
				}
				return val;
			}
		} catch (Throwable e) {
			if (PluginsHelper.isDevelopment()) {
				LOG.info("No translation: " + keyName);
			}
		}
		return null;
	}

	@Override
	public String getSynonyms(AbstractPoiType type) {
		AbstractPoiType baseLangType = type.getBaseLangType();
		if (baseLangType != null) {
			return getSynonyms(baseLangType);
		}
		return getSynonyms(type.getFormattedKeyName());
	}

	@Override
	public String getSynonyms(String keyName) {
		try {
			int in = getStringId(keyName);
			if (in != 0) {
				String val = localizedResources.getString(in);
				if (val != null) {
					int ind = val.indexOf(';');
					if (ind > 0) {
						return val.substring(ind + 1);
					}
					return "";
				}
				return val;
			}
		} catch (Exception e) {
			if (PluginsHelper.isDevelopment()) {
				LOG.info("No synonyms: " + keyName);
			}
		}
		return "";
	}

	@Override
	public String getAllLanguagesTranslationSuffix() {
		return localizedResources.getString(R.string.shared_string_all_languages).toLowerCase();
	}

	@Override
	public String getEnTranslation(AbstractPoiType type) {
		AbstractPoiType baseLangType = type.getBaseLangType();
		if (baseLangType != null) {
			return getEnTranslation(baseLangType) + " (" + AndroidUtils.getLangTranslation(app, type.getLang()).toLowerCase() + ")";
		}
		return getEnTranslation(type.getFormattedKeyName());
	}

	@Override
	public String getEnTranslation(String keyName) {
		if (enResources == null) {
			return Algorithms.capitalizeFirstLetter(keyName.replace('_', ' '));
		}
		try {
			int in = getStringId(keyName);
			if (in != 0) {
				String val = enResources.getString(in);
				if (val != null) {
					int ind = val.indexOf(';');
					if (ind > 0) {
						return val.substring(0, ind);
					}
				}
				return val;
			}
		} catch (Exception e) {
			if (PluginsHelper.isDevelopment()) {
				LOG.info("No EnTranslation: " + keyName);
			}
		}
		return null;
	}

	private static int getStringId(String keyName) {
		if (Algorithms.isEmpty(keyName)) {
			return 0;
		}
		Integer cached = STRING_IDS.get(keyName);
		if (cached != null) {
			return cached;
		}
		// getField() copies a Field on every call and throws for every key without a poi_ string;
		// AbstractPoiType keeps only the translations that were found, so a missing key is cached as 0.
		int id = 0;
		try {
			id = R.string.class.getField("poi_" + keyName).getInt(null);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			if (PluginsHelper.isDevelopment()) {
				LOG.info("No translation: " + keyName);
			}
		}
		STRING_IDS.put(keyName, id);
		return id;
	}
}
