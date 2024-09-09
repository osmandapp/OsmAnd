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
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
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

	protected final List<IconsCategory> categories = new ArrayList<>();
	private IconsCategory selectedCategory;
	protected List<String> lastUsedIcons;
	private String selectedIconKey;

	private EditorIconCardController cardController;
	private EditorIconScreenController screenController;
	private IconsPaletteElements<String> paletteElements;
	private Fragment targetFragment;
	private int controlsAccentColor;

	public EditorIconController(@NonNull OsmandApplication app) {
		super(app);
	}

	protected void init() {
		initIconCategories();
		this.selectedCategory = findIconCategory(getSelectedIconKey());
		this.cardController = new EditorIconCardController(app, this);
		this.screenController = new EditorIconScreenController(app, this);
	}

	protected void initIconCategories() {
		initLastUsedCategory();
		initAssetsCategories();
		initPoiPoiCategories();
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
			categories.addAll(readCategoriesFromAssets(Arrays.asList(SPECIAL_KEY, SYMBOLS_KEY)));
		} catch (JSONException e) {
			LOG.error(e.getMessage());
		}
	}

	protected void initPoiPoiCategories() {
		categories.addAll(readOriginalPoiCategories());
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
			List<String> iconKeys = new ArrayList<>();
			for (PoiType poiType : poiTypeList) {
				EditorIconUtils.retrieveIconKey(poiType, iconKeys::add);
			}
			if (!Algorithms.isEmpty(iconKeys)) {
				categories.add(new IconsCategory(poiCategory.getKeyName(), poiCategory.getTranslation(), iconKeys));
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
				@Override
				protected Drawable getIconDrawable(@NonNull String iconName, boolean isSelected) {
					int iconId = AndroidUtils.getDrawableId(app, iconName);
					if (iconId <= 0) {
						iconId = RenderingIcons.getBigIconResourceId(iconName);
					}
					if (iconId <= 0) {
						iconId = R.drawable.ic_action_search_dark;
					}
					return getContentIcon(iconId);
				}
			};
		}
		return paletteElements;
	}

	public boolean isSelectedIcon(@NonNull String iconKey) {
		return Objects.equals(iconKey, getSelectedIconKey());
	}

	public void onIconSelectedFromPalette(@NonNull String iconKey, @Nullable String categoryKey) {
		setSelectedIconKey(iconKey);
		if (categoryKey != null) {
			setSelectedCategory(findCategoryByKey(categoryKey));
			cardController.updateIconsSelection();
		}
		if (targetFragment instanceof OnIconsPaletteListener<?>) {
			((OnIconsPaletteListener<String>) targetFragment).onIconSelectedFromPalette(iconKey);
		}
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
		return categories.get(0);
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
