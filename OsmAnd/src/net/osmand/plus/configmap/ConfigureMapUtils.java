package net.osmand.plus.configmap;

import static net.osmand.plus.dialogs.DetailsBottomSheet.STREET_LIGHTING;
import static net.osmand.plus.dialogs.DetailsBottomSheet.STREET_LIGHTING_NIGHT;
import static net.osmand.plus.settings.backend.OsmandSettings.RENDERER_PREFERENCE_PREFIX;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ConfigureMapUtils {

	public static final String[] MAP_LANGUAGES_IDS = {"", "en", "af", "als", "ar", "az", "be", "ber", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "ckb", "cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "fy", "ga", "gl", "he", "hi", "hsb", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "kab", "kk", "kn", "ko", "ku", "la", "lb", "lo", "lt", "lv", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn", "no", "nv", "oc", "os", "pl", "pms", "pt", "ro", "ru", "sat", "sc", "sh", "sk", "sl", "sq", "sr", "sr-latn", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh", "zh-Hans", "zh-Hant"};

	@NonNull
	public static Map<String, String> getSorterMapLanguages(@NonNull OsmandApplication app) {
		Map<String, String> mapLanguages = new HashMap<>();
		for (String mapLanguageId : MAP_LANGUAGES_IDS) {
			boolean localNames = Algorithms.isEmpty(mapLanguageId);
			String mapLanguageName = localNames
					? app.getString(R.string.local_map_names)
					: AndroidUtils.getLangTranslation(app, mapLanguageId);

			mapLanguages.put(mapLanguageId, mapLanguageName);
		}

		Map<String, String> sortedMapLanguages = new TreeMap<>(getLanguagesComparator(mapLanguages));
		sortedMapLanguages.putAll(mapLanguages);
		return sortedMapLanguages;
	}

	/**
	 * @param unsortedLanguages map of entries like en:English. Empty key stands for device locale or default locale
	 */
	@NonNull
	public static Comparator<String> getLanguagesComparator(@NonNull Map<String, String> unsortedLanguages) {
		return (leftKey, rightKey) -> {
			int i1 = Algorithms.isEmpty(leftKey) ? 0 : (leftKey.equals("en") ? 1 : 2);
			int i2 = Algorithms.isEmpty(rightKey) ? 0 : (rightKey.equals("en") ? 1 : 2);
			if (i1 != i2) {
				return i1 < i2 ? -1 : 1;
			}

			String leftValue = unsortedLanguages.get(leftKey);
			String rightValue = unsortedLanguages.get(rightKey);
			return Algorithms.compare(leftValue, rightValue);
		};
	}

	public static List<RenderingRuleProperty> getCustomRules(@NonNull OsmandApplication app, String... skipCategories) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer == null) {
			return new ArrayList<>();
		}
		List<RenderingRuleProperty> customRules = new ArrayList<>();
		for (RenderingRuleProperty p : renderer.PROPS.getCustomRules()) {
			boolean skip = false;
			if (skipCategories != null) {
				for (String category : skipCategories) {
					if (category.equals(p.getCategory())) {
						skip = true;
						break;
					}
				}
			}
			if (!skip) {
				customRules.add(p);
			}
		}
		return customRules;
	}

	protected static String[] getRenderingPropertyPossibleValues(OsmandApplication app, RenderingRuleProperty p) {
		String[] possibleValuesString = new String[p.getPossibleValues().length + 1];
		possibleValuesString[0] = AndroidUtils.getRenderingStringPropertyValue(app, p.getDefaultValueDescription());

		for (int j = 0; j < p.getPossibleValues().length; j++) {
			possibleValuesString[j + 1] = AndroidUtils.getRenderingStringPropertyValue(app, p.getPossibleValues()[j]);
		}
		return possibleValuesString;
	}

	protected static String getDescription(@NonNull OsmandSettings settings,
	                                       @NonNull List<CommonPreference<Boolean>> prefs) {
		int count = 0;
		int enabled = 0;

		CommonPreference<Boolean> streetLightingPref = settings.getCustomRenderBooleanProperty(STREET_LIGHTING);
		boolean hasStreetLightingSwitch = prefs.contains(streetLightingPref);
		String streetLightingNightModePrefId = RENDERER_PREFERENCE_PREFIX + STREET_LIGHTING_NIGHT;

		for (CommonPreference<Boolean> p : prefs) {
			boolean skipPref = p.getId().equals(streetLightingNightModePrefId) && hasStreetLightingSwitch;
			if (skipPref) {
				continue;
			}

			count++;
			if (p.get()) {
				enabled++;
			}
		}
		return enabled + "/" + count;
	}

	protected static String getRenderDescr(OsmandApplication app) {
		RendererRegistry registry = app.getRendererRegistry();
		RenderingRulesStorage storage = registry.getCurrentSelectedRenderer();
		if (storage == null) {
			return "";
		}
		String translation = RendererRegistry.getTranslatedRendererName(app, storage.getName());
		return translation == null ? storage.getName() : translation;
	}

	protected static String getDayNightDescr(MapActivity activity) {
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().toHumanString(activity);
	}

	@DrawableRes
	protected static int getDayNightIcon(MapActivity activity) {
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().getIconRes();
	}

	protected static String getScale(MapActivity activity) {
		int scale = (int) (activity.getMyApplication().getSettings().TEXT_SCALE.get() * 100);
		return scale + " %";
	}
}
