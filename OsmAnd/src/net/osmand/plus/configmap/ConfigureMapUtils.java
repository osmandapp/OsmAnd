package net.osmand.plus.configmap;

import static net.osmand.plus.dialogs.DetailsBottomSheet.STREET_LIGHTING;
import static net.osmand.plus.dialogs.DetailsBottomSheet.STREET_LIGHTING_NIGHT;
import static net.osmand.plus.settings.backend.OsmandSettings.RENDERER_PREFERENCE_PREFIX;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import java.util.*;
import java.util.stream.Collectors;

public class ConfigureMapUtils {

	public static final String[] MAP_LANGUAGES_IDS = {"", "en", "af", "als", "ar", "az", "be", "ber", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "ckb", "cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "fy", "ga", "gl", "he", "hi", "hsb", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "kab", "kk", "kn", "ko", "ku", "la", "lb", "lo", "lt", "lv", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn", "no", "nv", "oc", "os", "pl", "pms", "pt", "ro", "ru", "sat", "sc", "sh", "sk", "sl", "sq", "sr", "sr-latn", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh", "zh-Hans", "zh-Hant"};

	@NonNull
	public static Map<String, String> getSortedMapLanguageNameById(@NonNull OsmandApplication app) {
		final Map<String, String> mapLanguageNameById =
				Arrays
						.stream(MAP_LANGUAGES_IDS)
						.collect(
								Collectors.toMap(
										mapLanguageId -> mapLanguageId,
										mapLanguageId -> {
											final boolean localNames = Algorithms.isEmpty(mapLanguageId);
											return localNames
													? app.getString(R.string.local_map_names)
													: AndroidUtils.getLangTranslation(app, mapLanguageId);
										}));
		final Map<String, String> sortedMapLanguageNameById = new TreeMap<>(getLanguagesComparator(mapLanguageNameById));
		sortedMapLanguageNameById.putAll(mapLanguageNameById);
		return sortedMapLanguageNameById;
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

	@Nullable
	public static RenderingRuleProperty getPropertyForAttr(@NonNull OsmandApplication app, @NonNull String attrName) {
		return getPropertyForAttr(getCustomRules(app), attrName);
	}

	@Nullable
	public static RenderingRuleProperty getPropertyForAttr(@NonNull List<RenderingRuleProperty> customRules, @NonNull String attrName) {
		for (RenderingRuleProperty property : customRules) {
			if (Algorithms.stringsEqual(property.getAttrName(), attrName)) {
				return property;
			}
		}
		return null;
	}

	public static List<RenderingRuleProperty> getCustomRules(@NonNull OsmandApplication app, String... skipCategories) {
		return getCustomRules(
				app.getRendererRegistry().getCurrentSelectedRenderer(),
				skipCategories);
	}

	public static List<RenderingRuleProperty> getCustomRules(final RenderingRulesStorage renderer, final String... skipCategories) {
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

	protected static Map<String, CharSequence> getSortedItemByKey(final RenderingRuleProperty property,
																  final Context context) {
		final Map<String, CharSequence> itemByKey = new LinkedHashMap<>();
		itemByKey.put(property.getDefaultValueDescription(), AndroidUtils.getRenderingStringPropertyValue(context, property.getDefaultValueDescription()));
		for (final String possibleValue : property.getPossibleValues()) {
			itemByKey.put(possibleValue, AndroidUtils.getRenderingStringPropertyValue(context, possibleValue));
		}
		return itemByKey;
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
		return activity.getMyApplication().getSettings().DAYNIGHT_MODE.get().getDefaultIcon();
	}

	protected static String getScale(MapActivity activity) {
		int scale = (int) (activity.getMyApplication().getSettings().TEXT_SCALE.get() * 100);
		return scale + " %";
	}
}
