package net.osmand.plus.mapcontextmenu.editors.icon;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.icon.CircleIconPaletteElements;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.card.icon.OnIconsPaletteListener;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class EditorIconController extends BaseDialogController {

	public static final String PROCESS_ID = "editor_process_select_icon";
	private static final String ORIGINAL_ICON_PROCESS_ID = "editor_process_select_icon_with_original";

	private static final Log LOG = PlatformUtil.getLog(EditorIconController.class);

	private static final int LAST_USED_ICONS_LIMIT = 12;

	private static final String POI_CATEGORIES_FILE = "poi_categories.json";
	public static final String ORIGINAL_KEY = "original";
	public static final String LAST_USED_KEY = "last_used_icons";
	public static final String SPECIAL_KEY = "special";
	public static final String SYMBOLS_KEY = "symbols";
	public static final String ACTIVITIES_KEY = "activities";
	public static final String TRAVEL_KEY = "travel";

	// Asset groups merged into a POI category with a different key; other groups use their own key
	private static final Map<String, String> GROUP_POI_CATEGORY_KEYS = Map.of(
			"transport", "transportation",
			"nautical", "seamark",
			"industrial", "man_made"
	);

	protected final List<IconsCategory> categories = new ArrayList<>();
	protected IconsCategory selectedCategory;
	protected List<String> lastUsedIcons;
	private String selectedIconKey;
	private String originalIconKey;

	protected EditorIconCardController cardController;
	private EditorIconScreenController screenController;
	private IconsPaletteElements<String> paletteElements;
	private Fragment targetFragment;
	@Nullable
	protected OnIconsPaletteListener<String> iconsPaletteListener;
	private int controlsAccentColor;

	public EditorIconController(@NonNull OsmandApplication app) {
		super(app);
	}

	public void init() {
		initIconCategories();
		this.selectedCategory = findInitialIconCategory();
		this.cardController = createCardController();
		this.screenController = createScreenController();
	}

	protected void initIconCategories() {
		Map<String, List<String>> assetGroups = readAssetIconGroups();
		Map<String, List<String>> standaloneAssetGroups = extractStandaloneGroups(assetGroups);
		initOriginalCategory();
		initLastUsedCategory();
		initStandaloneAssetCategories(standaloneAssetGroups);
		initActivitiesCategory();
		initPoiCategories(assetGroups);
		sortCategories();
	}

	protected void initOriginalCategory() {
		if (originalIconKey != null) {
			categories.add(new IconsCategory(ORIGINAL_KEY, app.getString(R.string.shared_string_original), List.of(originalIconKey), true));
		}
	}

	protected void initLastUsedCategory() {
		lastUsedIcons = readLastUsedIcons();
		if (!Algorithms.isEmpty(lastUsedIcons)) {
			categories.add(new IconsCategory(LAST_USED_KEY, app.getString(R.string.shared_string_last_used), lastUsedIcons, true));
		}
	}

	protected void initStandaloneAssetCategories(@NonNull Map<String, List<String>> standaloneAssetGroups) {
		for (Map.Entry<String, List<String>> entry : standaloneAssetGroups.entrySet()) {
			String categoryKey = entry.getKey();
			List<String> iconKeys = entry.getValue();
			if (!Algorithms.isEmpty(iconKeys)) {
				String translatedName = AndroidUtils.getIconStringPropertyName(app, categoryKey);
				boolean isTopCategory = categoryKey.equals(SPECIAL_KEY);
				categories.add(new IconsCategory(categoryKey, translatedName, iconKeys, isTopCategory));
			}
		}
	}

	protected void initActivitiesCategory() {
		List<String> iconKeys = new ArrayList<>();
		RouteActivityHelper routeActivityHelper = app.getRouteActivityHelper();
		for (RouteActivity activity : routeActivityHelper.getActivities()) {
			EditorIconUtils.retrieveIconKey(app, activity, key -> {
				if (!iconKeys.contains(key)) {
					iconKeys.add(key);
				}
			});
		}
		if (!Algorithms.isEmpty(iconKeys)) {
			String translatedName = getString(R.string.shared_string_activity);
			categories.add(new IconsCategory(ACTIVITIES_KEY, translatedName, iconKeys));
		}
	}

	protected void initPoiCategories(@NonNull Map<String, List<String>> assetGroups) {
		List<IconsCategory> poiCategories = readOriginalPoiCategories();
		mergeMissingAssetIcons(poiCategories, assetGroups);
		for (IconsCategory poiCategory : poiCategories) {
			// categories are identified by key, so a POI category with a taken key (like "travel") joins that category
			IconsCategory category = CollectionUtils.searchElementWithCondition(categories,
					c -> c.getKey().equals(poiCategory.getKey()));
			if (category != null) {
				CollectionUtils.addAllIfNotContains(category.getIconKeys(), poiCategory.getIconKeys());
			} else {
				categories.add(poiCategory);
			}
		}
	}

	@NonNull
	protected EditorIconCardController createCardController() {
		return new EditorIconCardController(app, this);
	}

	@NonNull
	protected EditorIconScreenController createScreenController() {
		return new EditorIconScreenController(app, this);
	}

	protected void sortCategories() {
		Collator collator = OsmAndCollator.primaryCollator();
		categories.sort((c1, c2) -> {
			if (c1.isTopCategory()) {
				return c2.isTopCategory() ? 0 : -1;
			} else if (c2.isTopCategory()) {
				return c1.isTopCategory() ? 0 : 1;
			}
			return collator.compare(c1.getTranslation(), c2.getTranslation());
		});
	}

	@NonNull
	protected List<String> readLastUsedIcons() {
		List<String> lastUsedIcons = app.getSettings().LAST_USED_FAV_ICONS.getStringsList();
		if (lastUsedIcons != null) {
			if (lastUsedIcons.size() > LAST_USED_ICONS_LIMIT) {
				lastUsedIcons = lastUsedIcons.subList(0, LAST_USED_ICONS_LIMIT);
			}
		} else {
			lastUsedIcons = Collections.emptyList();
		}
		return new ArrayList<>(lastUsedIcons);
	}

	@NonNull
	protected Map<String, List<String>> readAssetIconGroups() {
		Map<String, List<String>> assetGroups = new LinkedHashMap<>();
		try (InputStream is = app.getAssets().open(POI_CATEGORIES_FILE)) {
			JSONObject obj = new JSONObject(Algorithms.readFromInputStream(is).toString());
			JSONObject categoriesJson = obj.getJSONObject("categories");
			JSONArray names = categoriesJson.names();
			for (int i = 0; names != null && i < names.length(); i++) {
				String groupKey = names.getString(i);
				JSONArray iconJsonArray = categoriesJson.getJSONObject(groupKey).getJSONArray("icons");
				List<String> iconKeys = new ArrayList<>();
				for (int j = 0; j < iconJsonArray.length(); j++) {
					iconKeys.add(iconJsonArray.getString(j));
				}
				assetGroups.put(groupKey, iconKeys);
			}
		} catch (IOException | JSONException e) {
			LOG.error("Failed to read " + POI_CATEGORIES_FILE, e);
			assetGroups.clear();
		}
		return assetGroups;
	}

	@NonNull
	protected static Map<String, List<String>> extractStandaloneGroups(@NonNull Map<String, List<String>> assetGroups) {
		return CollectionUtils.extractEntries(assetGroups, SPECIAL_KEY, SYMBOLS_KEY, TRAVEL_KEY);
	}

	@NonNull
	private List<IconsCategory> readOriginalPoiCategories() {
		List<IconsCategory> categories = new ArrayList<>();
		List<PoiCategory> poiCategories = app.getPoiTypes().getCategories(false);
		poiCategories.sort(Comparator.comparing(AbstractPoiType::getTranslation));
		for (PoiCategory poiCategory : poiCategories) {
			List<PoiType> poiTypeList = new ArrayList<>(poiCategory.getPoiTypes());
			poiTypeList.sort(Comparator.comparing(AbstractPoiType::getTranslation));
			Set<String> iconKeys = new LinkedHashSet<>();
			for (PoiType poiType : poiTypeList) {
				EditorIconUtils.retrieveIconKey(poiType, iconKeys::add);
			}
			if (!Algorithms.isEmpty(iconKeys)) {
				categories.add(new IconsCategory(poiCategory.getKeyName(), poiCategory.getTranslation(), new ArrayList<>(iconKeys)));
			}
		}
		return categories;
	}

	private void mergeMissingAssetIcons(@NonNull List<IconsCategory> poiCategories,
	                                    @NonNull Map<String, List<String>> assetGroups) {
		Set<String> poiIconKeys = new HashSet<>();
		Map<String, IconsCategory> poiCategoriesByKey = new HashMap<>();
		for (IconsCategory category : poiCategories) {
			poiIconKeys.addAll(category.getIconKeys());
			poiCategoriesByKey.put(category.getKey(), category);
		}
		for (Map.Entry<String, List<String>> entry : assetGroups.entrySet()) {
			String groupKey = entry.getKey();
			Set<String> missingIconKeys = new LinkedHashSet<>();
			for (String iconKey : entry.getValue()) {
				if (!poiIconKeys.contains(iconKey) && RenderingIcons.containsBigIcon(iconKey)) {
					missingIconKeys.add(iconKey);
				}
			}
			if (missingIconKeys.isEmpty()) {
				continue;
			}
			String categoryKey = GROUP_POI_CATEGORY_KEYS.getOrDefault(groupKey, groupKey);
			if (!poiCategoriesByKey.containsKey(categoryKey)) {
				categoryKey = groupKey;
			}
			IconsCategory category = poiCategoriesByKey.get(categoryKey);
			if (category == null) {
				String translatedName = AndroidUtils.getIconStringPropertyName(app, groupKey);
				category = new IconsCategory(groupKey, translatedName, new ArrayList<>());
				poiCategories.add(category);
				poiCategoriesByKey.put(groupKey, category);
			}
			List<String> iconKeys = category.getIconKeys();
			for (String iconKey : missingIconKeys) {
				if (!iconKeys.contains(iconKey)) {
					iconKeys.add(iconKey);
				}
			}
		}
	}

	public void setSelectedCategory(@NonNull IconsCategory category) {
		this.selectedCategory = category;
		cardController.updateSelectedCardState();
		screenController.updateSelectedCategory();
	}

	@NonNull
	public IconsCategory getSelectedCategory() {
		return selectedCategory;
	}

	public void setTargetFragment(@NonNull Fragment targetFragment) {
		this.targetFragment = targetFragment;
	}

	public void setIconsPaletteListener(@Nullable OnIconsPaletteListener<String> iconsPaletteListener) {
		this.iconsPaletteListener = iconsPaletteListener;
	}

	@Nullable
	public Fragment getTargetFragment() {
		return targetFragment;
	}

	public void addIconToLastUsed(@NonNull String iconKey) {
		lastUsedIcons.remove(iconKey);
		lastUsedIcons.add(0, iconKey);
		if (lastUsedIcons.size() > LAST_USED_ICONS_LIMIT) {
			lastUsedIcons = lastUsedIcons.subList(0, LAST_USED_ICONS_LIMIT);
		}
		app.getSettings().LAST_USED_FAV_ICONS.setStringsList(lastUsedIcons);
	}

	public void updateAccentColor(@ColorInt int color) {
		this.controlsAccentColor = color;
		cardController.askUpdateColoredPaletteElements();
	}

	@NonNull
	public List<IconsCategory> getCategories() {
		return categories;
	}

	@Nullable
	public String getSelectedIconKey() {
		return selectedIconKey;
	}

	public void setSelectedIconKey(@Nullable String selectedIconKey) {
		this.selectedIconKey = selectedIconKey;
	}

	@Nullable
	public String getOriginalIconKey() {
		return originalIconKey;
	}

	public void setOriginalIconKey(@Nullable String originalIconKey) {
		this.originalIconKey = originalIconKey;
	}

	@ColorInt
	public int getControlsAccentColor() {
		return controlsAccentColor;
	}

	@NonNull
	public EditorIconCardController getCardController() {
		return cardController;
	}

	@NonNull
	public EditorIconScreenController getScreenController() {
		return screenController;
	}

	@NonNull
	public IconsPaletteElements<String> getPaletteElements(@NonNull Context context, boolean nightMode) {
		if (paletteElements == null || paletteElements.isNightMode() != nightMode) {
			paletteElements = new CircleIconPaletteElements<>(context, nightMode) {
				static final int DEFAULT_ICON_ID = R.drawable.ic_action_search_dark;

				@Override
				protected Drawable getIconDrawable(@NonNull String iconName, boolean isSelected) {
					return getContentIcon(AndroidUtils.getDrawableId(app, iconName, DEFAULT_ICON_ID));
				}
			};
		}
		return paletteElements;
	}

	public boolean isSelectedIcon(@NonNull String iconKey) {
		return Objects.equals(iconKey, getSelectedIconKey());
	}

	public void onIconSelectedFromPalette(@Nullable String iconKey, @Nullable String categoryKey) {
		setSelectedIconKey(iconKey);
		if (categoryKey != null) {
			setSelectedCategory(findCategoryByKey(categoryKey));
			cardController.updateIconsSelection();
		}
		if (targetFragment instanceof OnIconsPaletteListener<?>) {
			((OnIconsPaletteListener<String>) targetFragment).onIconSelectedFromPalette(iconKey);
		} else if (iconsPaletteListener != null) {
			iconsPaletteListener.onIconSelectedFromPalette(iconKey);
		}
	}

	@NonNull
	protected IconsCategory findInitialIconCategory() {
		String iconKey = getSelectedIconKey();
		IconsCategory category = findIconCategory(iconKey, Set.of(ORIGINAL_KEY));
		if (iconKey != null && !category.containsIcon(iconKey)) {
			// the icon is in no other category, e.g. the original icon of a POI
			return findIconCategory(iconKey);
		}
		return category;
	}

	@NonNull
	protected IconsCategory findIconCategory(@Nullable String iconKey) {
		return findIconCategory(iconKey, null);
	}

	@NonNull
	protected IconsCategory findIconCategory(@Nullable String iconKey, @Nullable Collection<String> categoriesToSkip) {
		if (iconKey != null) {
			for (IconsCategory category : categories) {
				if (categoriesToSkip != null && categoriesToSkip.contains(category.getKey())) {
					continue;
				}
				if (category.containsIcon(iconKey)) {
					return category;
				}
			}
		}
		return getDefaultCategory();
	}

	@NonNull
	private IconsCategory findCategoryByKey(@Nullable String categoryKey) {
		if (categoryKey != null) {
			for (IconsCategory category : categories) {
				if (Objects.equals(category.getKey(), categoryKey)) {
					return category;
				}
			}
		}
		return getDefaultCategory();
	}

	@NonNull
	protected IconsCategory getDefaultCategory() {
		IconsCategory category = categories.get(0);
		// "original" of the point editor holds a single icon, it is not a default category
		if (originalIconKey != null && category.getKey().equals(ORIGINAL_KEY) && categories.size() > 1) {
			category = categories.get(1);
		}
		return category;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return getProcessId(originalIconKey);
	}

	/**
	 * A controller with the original icon of a point is registered separately,
	 * so that a folder editor opened from the point editor does not get the original icon.
	 */
	@NonNull
	private static String getProcessId(@Nullable String originalIconKey) {
		return originalIconKey != null ? ORIGINAL_ICON_PROCESS_ID : PROCESS_ID;
	}

	public static void onDestroy(@NonNull OsmandApplication app) {
		onDestroy(app, null);
	}

	public static void onDestroy(@NonNull OsmandApplication app, @Nullable String originalIconKey) {
		DialogManager manager = app.getDialogManager();
		manager.unregister(getProcessId(originalIconKey));
	}

	@NonNull
	public static EditorIconController getInstance(@NonNull OsmandApplication app,
	                                               @NonNull Fragment targetFragment,
	                                               @Nullable String preselectedIconKey) {
		return getInstance(app, targetFragment, preselectedIconKey, null);
	}

	@NonNull
	public static EditorIconController getInstance(@NonNull OsmandApplication app,
	                                               @NonNull Fragment targetFragment,
	                                               @Nullable String preselectedIconKey,
	                                               @Nullable String originalIconKey) {
		DialogManager dialogManager = app.getDialogManager();
		String processId = getProcessId(originalIconKey);
		EditorIconController controller = (EditorIconController) dialogManager.findController(processId);
		if (controller != null && !Objects.equals(controller.getOriginalIconKey(), originalIconKey)) {
			// left by the editor of another point that was not destroyed normally
			dialogManager.unregister(processId);
			controller = null;
		}
		if (controller == null) {
			controller = new EditorIconController(app);
			controller.setSelectedIconKey(preselectedIconKey);
			controller.setOriginalIconKey(originalIconKey);
			controller.init();
			dialogManager.register(processId, controller);
		}
		controller.setTargetFragment(targetFragment);
		return controller;
	}
}
