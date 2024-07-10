package net.osmand.plus.mapcontextmenu.editors.controller;

import static net.osmand.plus.mapcontextmenu.editors.PointIconScreenAdapter.CATEGORY_ICONS;
import static net.osmand.plus.mapcontextmenu.editors.PointIconScreenAdapter.CATEGORY_SELECTOR;
import static net.osmand.plus.mapcontextmenu.editors.PointIconScreenAdapter.ICON_SEARCH_RESULT;
import static net.osmand.plus.mapcontextmenu.editors.PointIconScreenAdapter.NO_ICONS_FOUND;
import static net.osmand.plus.utils.ColorUtilities.getDefaultIconColor;
import static net.osmand.search.core.ObjectType.SEARCH_FINISHED;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.base.multistate.BaseMultiStateCardController;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.icon.IconsPaletteCard;
import net.osmand.plus.card.icon.IconsPaletteController;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.card.icon.OnIconsPaletteListener;
import net.osmand.plus.mapcontextmenu.editors.data.IconSearchResult;
import net.osmand.plus.mapcontextmenu.editors.data.IconsCategory;
import net.osmand.plus.mapcontextmenu.editors.data.IconData;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PointIconControllerJava extends BaseMultiStateCardController implements IDialogController {

	public static final String PROCESS_ID = "select_point_icon";

	private static final Log LOG = PlatformUtil.getLog(PointIconControllerJava.class);

	private static final int LAST_USED_ICONS_LIMIT = 12;

	private static final String CATEGORY_LAST_USED_ICONS = "last used icons";
	private static final String CATEGORY_SPECIAL_ICONS = "special";
	private static final String CATEGORY_SYMBOLS_ICONS = "symbols";

	private final List<IconsCategory> categories;
	private String preselectedIconKey;
	private List<String> lastUsedIcons;

	private final SearchUICore searchUICore;
	private List<PoiType> lastSearchResults;
	private boolean inSearchMode;
	private boolean searchCancelled;

	private OnIconsPaletteListener<String> listener;
	private IconsPaletteController<String> paletteController;
	private int selectedColor;

	public PointIconControllerJava(@NonNull OsmandApplication app, @Nullable String preselectedIconKey) {
		super(app);
		this.lastUsedIcons = fetchLastUsedIcons();
		this.categories = collectIconCategories();
		this.searchUICore = app.getSearchUICore().getCore();
		this.preselectedIconKey = preselectedIconKey;
		this.selectedState = findCardState(getInitialCategory());
	}

	public void setListener(@NonNull OnIconsPaletteListener<String> listener) {
		this.listener = listener;
	}

	@NonNull
	private IconsCategory getInitialCategory() {
		for (IconsCategory category : categories) {
			if (category.getIconKeys().contains(preselectedIconKey)) {
				return category;
			}
		}
		return categories.get(0);
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();
		if (inSearchMode) {
			if (!Algorithms.isEmpty(lastSearchResults)) {
				for (PoiType poiType : lastSearchResults) {
					String iconKey = getIconKey(poiType);
					int iconId = getIconId(iconKey);
					if (iconId != 0) {
						IconData pointIcon = new IconData(poiType.getIconKeyName(), iconId, getIconName(poiType));
						IconSearchResult searchResult = new IconSearchResult(pointIcon, getCategoryName(poiType.getCategory()));
						screenItems.add(new ScreenItem(ICON_SEARCH_RESULT, searchResult));
					}
				}
			} else {
				screenItems.add(new ScreenItem(NO_ICONS_FOUND));
			}
		} else {
			screenItems.add(new ScreenItem(CATEGORY_SELECTOR, categories));
			for (IconsCategory category : categories) {
				screenItems.add(new ScreenItem(CATEGORY_ICONS, category));
			}
		}
		return screenItems;
	}

	@NonNull
	private List<ChipItem> collectCategoriesChipItems() {
		// TODO
		return new ArrayList<>();
	}

	// todo we should ignore all icons without appropriate resource id
	@NonNull
	private List<IconsCategory> collectIconCategories() {
		List<IconsCategory> categories = new ArrayList<>();

		// Add "Last used" category
		if (!Algorithms.isEmpty(lastUsedIcons)) {
			categories.add(new IconsCategory(CATEGORY_LAST_USED_ICONS, app.getString(R.string.shared_string_last_used), lastUsedIcons));
		}

		// Add categories "Special" and "Symbols" from Assets file
		try {
			categories.addAll(readCategoriesFromAsset(Arrays.asList(CATEGORY_SPECIAL_ICONS, CATEGORY_SYMBOLS_ICONS)));
		} catch (JSONException e) {
			LOG.error(e.getMessage());
		}

		// Add original POI categories
		List<PoiCategory> poiCategoryList = app.getPoiTypes().getCategories(false);
		poiCategoryList.sort(Comparator.comparing(AbstractPoiType::getTranslation));
		for (PoiCategory poiCategory : poiCategoryList) {
			List<PoiType> poiTypeList = new ArrayList<>(poiCategory.getPoiTypes());
			poiTypeList.sort(Comparator.comparing(AbstractPoiType::getTranslation));
			List<String> iconKeys = new ArrayList<>();
			for (PoiType poiType : poiTypeList) {
				String iconKey = getIconKey(poiType);
				int iconId = getIconId(iconKey);
				if (iconId != 0) {
					iconKeys.add(iconKey);
				}
			}
			if (!Algorithms.isEmpty(iconKeys)) {
				categories.add(new IconsCategory(poiCategory.getKeyName(), poiCategory.getTranslation(), iconKeys));
			}
		}
		return categories;
	}

	@NonNull
	private List<IconsCategory> readCategoriesFromAsset(@NonNull Collection<String> categoriesKeys) throws JSONException {
		String categoriesJsonStr = null;
		List<IconsCategory> categories = new ArrayList<>();
		try {
			InputStream is = app.getAssets().open("poi_categories.json");
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
							categories.add(new IconsCategory(categoryKey, translatedName, iconKeys));
						}
					}
				}
			}
		}
		return categories;
	}

	private void searchIcons(@NonNull String text) {
		searchCancelled = false;
		SearchSettings searchSettings = searchUICore.getSearchSettings().setSearchTypes(ObjectType.POI_TYPE);
		searchUICore.updateSettings(searchSettings);
		searchUICore.search(text, true, new ResultMatcher<SearchResult>() {
			@Override
			public boolean publish(SearchResult searchResult) {
				if (searchResult.objectType == SEARCH_FINISHED) {
					SearchResultCollection resultCollection = searchUICore.getCurrentSearchResult();
					List<PoiType> results = new ArrayList<>();
					for (SearchResult result : resultCollection.getCurrentSearchResults()) {
						Object poiObject = result.object;
						if (poiObject instanceof PoiType) {
							PoiType poiType = (PoiType) poiObject;
							if (!poiType.isAdditional()) {
								results.add(poiType);
							}
						}
					}
					lastSearchResults = results;
					app.runInUIThread(() -> {
						// todo update UI after search finished
					});
				}
				return true;
			}

			@Override
			public boolean isCancelled() {
				return searchCancelled;
			}
		});
	}

	@NonNull
	private List<String> fetchLastUsedIcons() {
		List<String> iconsList = app.getSettings().LAST_USED_FAV_ICONS.getStringsList();
		return new ArrayList<>(iconsList == null ? Collections.emptyList() : iconsList);
	}

	public void addLastUsedIcon(@NonNull String iconName) {
		lastUsedIcons.remove(iconName);
		lastUsedIcons.add(0, iconName);
		saveLastUsedIcons();
	}

	private void saveLastUsedIcons() {
		if (lastUsedIcons.size() > LAST_USED_ICONS_LIMIT) {
			lastUsedIcons = lastUsedIcons.subList(0, LAST_USED_ICONS_LIMIT);
		}
		app.getSettings().LAST_USED_FAV_ICONS.setStringsList(lastUsedIcons);
	}

	@NonNull
	private String getCategoryName(@NonNull PoiCategory poiCategory) {
		return poiCategory.getTranslation();
	}

	private String getIconName(@NonNull PoiType poiType) {
		return poiType.getTranslation();
	}

	@NonNull
	private String getIconKey(@NonNull PoiType poiType) {
		String key = poiType.getIconKeyName();
		if (RenderingIcons.containsBigIcon(key)) {
			return key;
		}
		return poiType.getOsmTag() + "_" + poiType.getOsmValue();
	}

	@DrawableRes
	private int getIconId(@NonNull String iconKey) {
		return RenderingIcons.getBigIconResourceId(iconKey);
	}

	public void updateAccentColor(@ColorInt int color) {
		this.selectedColor = color;
		paletteController.askUpdateColoredPaletteElements();
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		boolean shouldAddTopDivider = false;
		List<CardState> result = new ArrayList<>();
		for (IconsCategory category : categories) {
			CardState cardState = new CardState(category.getTranslatedName());
			cardState.setTag(category);
			cardState.setShowTopDivider(shouldAddTopDivider);
			shouldAddTopDivider = Objects.equals(CATEGORY_LAST_USED_ICONS, category.getKey());
			result.add(cardState);
		}
		return result;
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		selectedState = cardState;
		card.updateSelectedCardState();
	}

	@NonNull
	@Override
	public String getCardTitle() {
		return app.getString(R.string.select_icon_profile_dialog_title);
	}

	@NonNull
	@Override
	public String getCardStateSelectorTitle() {
		IconsCategory selectedCategory = (IconsCategory) selectedState.getTag();
		return selectedCategory.getTranslatedName();
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		IconsCategory selectedCategory = (IconsCategory) selectedState.getTag();
		paletteController =  new IconsPaletteController<String>(app, selectedCategory.getIconKeys(), preselectedIconKey) {
			@NonNull
			@Override
			public IconsPaletteElements<String> getPaletteElements(@NonNull Context context, boolean nightMode) {
				return new IconsPaletteElements<String>(context, nightMode) {
					@Override
					protected int getLayoutId() {
						return R.layout.preference_circle_item;
					}

					@Override
					public void bindView(@NonNull View itemView, @NonNull String icon, int controlsColor, boolean isSelected) {
						int iconId = getIconId(icon);
						View background = itemView.findViewById(R.id.background);
						int bgColor = isSelected ? controlsColor : getDefaultIconColor(app, nightMode);
						int bgColorWithAlpha = ColorUtilities.getColorWithAlpha(bgColor, 0.1f);
						Drawable bgDrawable = AppCompatResources.getDrawable(app, R.drawable.circle_background_light);
						AndroidUtils.setBackground(background, UiUtilities.tintDrawable(bgDrawable, bgColorWithAlpha));

						ImageView outlineCircle = itemView.findViewById(R.id.outline);
						if (isSelected) {
							GradientDrawable circleContourDrawable = (GradientDrawable)
									AppCompatResources.getDrawable(app, R.drawable.circle_contour_bg_light);
							if (circleContourDrawable != null) {
								circleContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), controlsColor);
							}
							outlineCircle.setImageDrawable(circleContourDrawable);
							outlineCircle.setVisibility(View.VISIBLE);
						} else {
							outlineCircle.setVisibility(View.GONE);
						}

						ImageView checkMark = itemView.findViewById(R.id.checkMark);
						if (isSelected) {
							checkMark.setImageDrawable(getPaintedIcon(iconId, controlsColor));
						} else {
							checkMark.setImageDrawable(getIcon(iconId, R.color.icon_color_default_light));
						}
					}
				};
			}

			@Override
			public String getPaletteTitle() {
				return getCardTitle();
			}

			@Override
			public int getControlsAccentColor(boolean nightMode) {
				return selectedColor;
			}

			@Override
			public boolean isAccentColorCanBeChanged() {
				return true;
			}
		};
		paletteController.setPaletteListener(icon -> listener.onIconSelectedFromPalette(icon));
		container.addView(new IconsPaletteCard<>(activity, paletteController).build());
	}

	@NonNull
	public static PointIconControllerJava getInstance(@NonNull OsmandApplication app,
	                                                  @NonNull OnIconsPaletteListener<String> listener,
	                                                  @Nullable String preselectedIconKey) {
		DialogManager dialogManager = app.getDialogManager();
		PointIconControllerJava controller = (PointIconControllerJava) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new PointIconControllerJava(app, preselectedIconKey);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setListener(listener);
		return controller;
	}
}
