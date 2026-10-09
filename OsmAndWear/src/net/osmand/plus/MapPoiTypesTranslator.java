package net.osmand.plus;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.reflect.Field;

public class MapPoiTypesTranslator implements PoiTranslator {

	private static final Log LOG = PlatformUtil.getLog(MapPoiTypesTranslator.class);

	private final OsmandApplication app;
	private final Resources resources;

	public MapPoiTypesTranslator(@NonNull OsmandApplication app) {
		this.app = app;
		resources = app.getLocaleHelper().getLocalizedResources("en");
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
			Field f = R.string.class.getField("poi_" + keyName);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				String val = app.getString(in);
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
			Field f = R.string.class.getField("poi_" + keyName);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				String val = app.getString(in);
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
		return app.getString(R.string.shared_string_all_languages).toLowerCase();
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
		if (resources == null) {
			return Algorithms.capitalizeFirstLetter(keyName.replace('_', ' '));
		}
		try {
			Field f = R.string.class.getField("poi_" + keyName);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				String val = resources.getString(in);
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
}