package net.osmand.plus.dialogs;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigureMapUtils {

	public static String[] MAP_NAMES_IDS = new String[] {"", "en", "af", "als", "ar", "az", "be", "ber", "bg", "bn", "bpy", "br", "bs", "ca", "ceb", "cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "fy", "ga", "gl", "he", "hi", "hsb", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "ka", "kab", "kn", "ko", "ku", "la", "lb", "lo", "lt", "lv", "mk", "ml", "mr", "ms", "nds", "new", "nl", "nn", "no", "nv", "oc", "os", "pl", "pms", "pt", "ro", "ru", "sc", "sh", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "vi", "vo", "zh"};

	public static String[] getSortedMapNamesIds(@NonNull Context ctx, @NonNull String[] ids, @NonNull String[] values) {
		final Map<String, String> mp = new HashMap<>();
		for (int i = 0; i < ids.length; i++) {
			mp.put(ids[i], values[i]);
		}
		ArrayList<String> lst = new ArrayList<>(mp.keySet());
		final String systemLocale = ctx.getString(R.string.system_locale) + " (" + ctx.getString(R.string.system_locale_no_translate) + ")";
		final String englishLocale = ctx.getString(R.string.lang_en);
		Collections.sort(lst, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				int i1 = Algorithms.isEmpty(lhs) ? 0 : (lhs.equals("en") ? 1 : 2);
				int i2 = Algorithms.isEmpty(rhs) ? 0 : (rhs.equals("en") ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				i1 = systemLocale.equals(lhs) ? 0 : (englishLocale.equals(lhs) ? 1 : 2);
				i2 = systemLocale.equals(rhs) ? 0 : (englishLocale.equals(rhs) ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				return mp.get(lhs).compareTo(mp.get(rhs));
			}
		});
		return lst.toArray(new String[0]);
	}

	public static String[] getMapNamesValues(Context ctx, String[] ids) {
		String[] translates = new String[ids.length];
		for (int i = 0; i < translates.length; i++) {
			if (Algorithms.isEmpty(ids[i])) {
				translates[i] = ctx.getString(R.string.local_map_names);
			} else {
				translates[i] = ((OsmandApplication) ctx.getApplicationContext()).getLangTranslation(ids[i]);
			}
		}

		return translates;
	}

	public static List<RenderingRuleProperty> getCustomRules(@NonNull OsmandApplication app, String... skipCategories) {
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (renderer == null) {
			return new ArrayList<>();
		}
		List<RenderingRuleProperty> customRules = new ArrayList<>();
		boolean useDepthContours = app.getResourceManager().hasDepthContours()
				&& (InAppPurchaseHelper.isSubscribedToAny(app)
				|| InAppPurchaseHelper.isDepthContoursPurchased(app));
		for (RenderingRuleProperty p : renderer.PROPS.getCustomRules()) {
			if (useDepthContours || !"depthContours".equals(p.getAttrName())) {
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

	protected static String getDescription(List<CommonPreference<Boolean>> prefs) {
		int count = 0;
		int enabled = 0;
		for (CommonPreference<Boolean> p : prefs) {
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
