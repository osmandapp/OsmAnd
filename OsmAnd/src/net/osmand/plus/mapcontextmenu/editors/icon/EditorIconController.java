package net.osmand.plus.mapcontextmenu.editors.icon;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class EditorIconController extends BaseDialogController {

	public static final String PROCESS_ID = "editor_process_select_icon";

	private static final Log LOG = PlatformUtil.getLog(EditorIconController.class);

	private static final int LAST_USED_ICONS_LIMIT = 12;

	private static final String POI_CATEGORIES_FILE = "poi_categories.json";
	public static final String LAST_USED_KEY = "last_used_icons";
	public static final String SPECIAL_KEY = "special";
	public static final String SYMBOLS_KEY = "symbols";
	public static final String ACTIVITIES_KEY = "activities";
	public static final String TRAVEL_KEY = "travel";

	protected final List<IconsCategory> categories = new ArrayList<>();
	protected IconsCategory selectedCategory;
	protected List<String> lastUsedIcons;
	private String selectedIconKey;

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
		initLastUsedCategory();
		initAssetsCategories();
		initActivitiesCategory();
		initPoiCategories();
		sortCategories();
	}

	protected void initLastUsedCategory() {
		lastUsedIcons = readLastUsedIcons();
		if (!Algorithms.isEmpty(lastUsedIcons)) {
			categories.add(new IconsCategory(LAST_USED_KEY, app.getString(R.string.shared_string_last_used), lastUsedIcons, true));
		}
	}

	protected void initAssetsCategories() {
		try {
			categories.addAll(readCategoriesFromAssets(Arrays.asList(SPECIAL_KEY, SYMBOLS_KEY, TRAVEL_KEY)));
		} catch (JSONException e) {
			LOG.error(e.getMessage());
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

	protected void initPoiCategories() {
		categories.addAll(readOriginalPoiCategories());
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
		categories.sort((c1, c2) -> {
			if (c1.isTopCategory()) {
				return c2.isTopCategory() ? 0 : -1;
			} else if (c2.isTopCategory()) {
				return c1.isTopCategory() ? 0 : 1;
			}
			return c1.getTranslation().compareTo(c2.getTranslation());
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
	private List<IconsCategory> readCategoriesFromAssets(@NonNull Collection<String> categoriesKeys) throws JSONException {
		String categoriesJsonStr = null;
		List<IconsCategory> categories = new ArrayList<>();
		try {
			InputStream is = app.getAssets().open(POI_CATEGORIES_FILE);
			categoriesJsonStr = Algorithms.readFromInputStream(is).toString();
		} catch (IOException e) {
			LOG.error("Failed to parse JSON", e);
		}
		if (categoriesJsonStr != null) {
			JSONObject obj = new JSONObject(categoriesJsonStr);
			JSONObject categoriesJson = obj.getJSONObject("categories");
			for (int i = 0; i < categoriesJson.length(); i++) {
				JSONArray names = categoriesJson.names();
				if (names != null) {
					String categoryKey = names.get(i).toString();
					if (categoriesKeys.contains(categoryKey)) {
						JSONObject categoryJson = categoriesJson.getJSONObject(categoryKey);
						JSONArray iconJsonArray = categoryJson.getJSONArray("icons");

						List<String> iconKeys = new ArrayList<>();
						for (int j = 0; j < iconJsonArray.length(); j++) {
							iconKeys.add(iconJsonArray.getString(j));
						}
						if (!Algorithms.isEmpty(iconKeys)) {
							String translatedName = AndroidUtils.getIconStringPropertyName(app, categoryKey);
							categories.add(new IconsCategory(categoryKey, translatedName, iconKeys, categoryKey.equals(SPECIAL_KEY)));
						}
					}
				}
			}
		}
		return categories;
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
		return findIconCategory(getSelectedIconKey());
	}

	@NonNull
	protected IconsCategory findIconCategory(@Nullable String iconKey) {
		if (iconKey != null) {
			for (IconsCategory category : categories) {
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
		return categories.get(0);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public static void onDestroy(@NonNull OsmandApplication app) {
		DialogManager manager = app.getDialogManager();
		manager.unregister(PROCESS_ID);
	}

	@NonNull
	public static EditorIconController getInstance(@NonNull OsmandApplication app, @NonNull Fragment targetFragment, @Nullable String preselectedIconKey) {
		DialogManager dialogManager = app.getDialogManager();
		EditorIconController controller = (EditorIconController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new EditorIconController(app);
			controller.setSelectedIconKey(preselectedIconKey);
			controller.init();
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setTargetFragment(targetFragment);
		return controller;
	}
}
