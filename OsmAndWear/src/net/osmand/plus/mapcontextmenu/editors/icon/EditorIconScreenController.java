package net.osmand.plus.mapcontextmenu.editors.icon;

import static net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController.LAST_USED_KEY;
import static net.osmand.plus.mapcontextmenu.editors.icon.EditorIconScreenAdapter.CATEGORY_ICONS;
import static net.osmand.plus.mapcontextmenu.editors.icon.EditorIconScreenAdapter.ICON_SEARCH_RESULT;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;
import static net.osmand.search.core.ObjectType.SEARCH_FINISHED;

import android.content.Context;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.ResultMatcher;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.containers.ScreenItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.icon.IconsPaletteElements;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconSearchResult;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class EditorIconScreenController implements IDialogController {

	private static final String CATEGORIES_LIST_KEY = "categories_list";

	private final OsmandApplication app;
	private final EditorIconController centralController;
	private IEditorIconPaletteScreen screen;

	private final SearchUICore searchUICore;
	private final List<PoiType> lastSearchResults = new ArrayList<>();
	private boolean inSearchMode;
	private boolean searchCancelled;

	public EditorIconScreenController(@NonNull OsmandApplication app, @NonNull EditorIconController centralController) {
		this.app = app;
		this.centralController = centralController;
		this.searchUICore = app.getSearchUICore().getCore();
	}

	public void bindScreen(@NonNull IEditorIconPaletteScreen screen) {
		this.screen = screen;
	}

	public void onDestroyScreen() {
		this.screen = null;
		exitSearchModeIfNeeded();
		if (centralController.getTargetFragment() instanceof BaseOsmAndFragment targetFragment) {
			targetFragment.updateStatusBar();
		}
	}

	@NonNull
	public List<ScreenItem> populateScreenItems() {
		List<ScreenItem> screenItems = new ArrayList<>();
		if (inSearchMode) {
			for (PoiType poiType : lastSearchResults) {
				EditorIconUtils.retrieveIconKey(poiType, iconKey -> {
					IconSearchResult searchResult = new IconSearchResult(iconKey, poiType);
					screenItems.add(new ScreenItem(ICON_SEARCH_RESULT, searchResult));
				});
			}
		} else {
			for (IconsCategory category : centralController.getCategories()) {
				screenItems.add(new ScreenItem(CATEGORY_ICONS, category));
			}
		}
		return screenItems;
	}

	@NonNull
	public List<ChipItem> collectCategoriesChipItems(boolean nightMode) {
		UiUtilities iconsCache = app.getUIUtilities();
		int activeColor = getActiveColor(app, nightMode);

		List<ChipItem> items = new ArrayList<>();
		ChipItem menuChip = new ChipItem(CATEGORIES_LIST_KEY);
		menuChip.icon = iconsCache.getIcon(R.drawable.ic_action_list_flat);
		menuChip.iconColor = activeColor;
		menuChip.contentDescription = app.getString(R.string.search_categories);
		items.add(menuChip);

		for (IconsCategory category : centralController.getCategories()) {
			String key = category.getKey();
			ChipItem item = new ChipItem(key);
			String categoryName = category.getTranslation();
			if (key.equals(LAST_USED_KEY)) {
				item.icon = iconsCache.getIcon(R.drawable.ic_action_history);
				item.iconColor = activeColor;
			} else {
				item.title = categoryName;
			}
			item.contentDescription = categoryName;
			item.tag = category;
			items.add(item);
		}
		return items;
	}

	public boolean onChipClick(@NonNull ChipItem chipItem) {
		String key = chipItem.id;
		if (CATEGORIES_LIST_KEY.equals(key)) {
			showCategoriesPopUpMenu(chipItem.boundView);
		} else {
			onSelectCategory((IconsCategory) chipItem.tag);
		}
		return false;
	}

	private void showCategoriesPopUpMenu(@NonNull View view) {
		boolean nightMode = centralController.isNightMode();
		boolean previousCategoryIsTop = false;
		List<PopUpMenuItem> items = new ArrayList<>();
		for (IconsCategory category : centralController.getCategories()) {
			items.add(new PopUpMenuItem.Builder(app)
					.setTitle(category.getTranslation())
					.showTopDivider(previousCategoryIsTop && !category.isTopCategory())
					.setTitleColor(getPrimaryTextColor(app, nightMode))
					.setTag(category)
					.create()
			);
			;
			previousCategoryIsTop = category.isTopCategory();
		}
		PopUpMenuDisplayData data = new PopUpMenuDisplayData();
		data.anchorView = view;
		data.menuItems = items;
		data.nightMode = nightMode;
		data.onItemClickListener = item -> onSelectCategory((IconsCategory) item.getTag());
		PopUpMenu.show(data);
	}

	private void onSelectCategory(@NonNull IconsCategory category) {
		centralController.setSelectedCategory(category);
	}

	public void updateSelectedCategory() {
		if (screen != null) {
			screen.updateSelectedCategory();
		}
	}

	private void exitSearchModeIfNeeded() {
		if (inSearchMode) {
			exitSearchMode();
		}
	}

	public void enterSearchMode() {
		inSearchMode = true;
		if (screen != null) {
			screen.onScreenModeChanged();
		}
	}

	public void exitSearchMode() {
		inSearchMode = false;
		searchCancelled = true;
		lastSearchResults.clear();
		if (screen != null) {
			screen.onScreenModeChanged();
		}
	}

	public boolean isInSearchMode() {
		return inSearchMode;
	}

	public void clearSearchQuery() {
		searchCancelled = true;
		lastSearchResults.clear();
	}

	public void searchIcons(@NonNull String text) {
		lastSearchResults.clear();
		if (Algorithms.isEmpty(text)) {
			if (screen != null) {
				screen.updateScreenContent();
			}
			return;
		}
		searchCancelled = false;
		SearchSettings searchSettings = searchUICore.getSearchSettings().setSearchTypes(ObjectType.POI_TYPE);
		searchUICore.updateSettings(searchSettings);
		searchUICore.search(text, true, new ResultMatcher<>() {
			@Override
			public boolean publish(SearchResult searchResult) {
				if (searchResult.objectType == SEARCH_FINISHED) {
					SearchResultCollection resultCollection = searchUICore.getCurrentSearchResult();
					List<PoiType> results = new ArrayList<>();
					for (SearchResult result : resultCollection.getCurrentSearchResults()) {
						Object poiObject = result.object;
						if (poiObject instanceof PoiType poiType) {
							if (!poiType.isAdditional()) {
								results.add(poiType);
							}
						}
					}
					lastSearchResults.addAll(results);
					app.runInUIThread(() -> {
						if (screen != null) {
							screen.updateScreenContent();
						}
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
	public IconsPaletteElements<String> getPaletteElements(@NonNull Context context, boolean nightMode) {
		return centralController.getPaletteElements(context, nightMode);
	}

	public void onIconSelectedFromPalette(@NonNull String iconKey, @NonNull String categoryKey) {
		centralController.onIconSelectedFromPalette(iconKey, categoryKey);
		if (screen != null) {
			screen.dismiss();
		}
	}

	@NonNull
	public IconsCategory getSelectedCategory() {
		return centralController.getSelectedCategory();
	}

	public boolean isSelectedIcon(@NonNull String iconKey) {
		return centralController.isSelectedIcon(iconKey);
	}

	@NonNull
	public String getToolbarTitle() {
		return app.getString(R.string.select_icon_profile_dialog_title);
	}

	@ColorInt
	public int getControlsAccentColor() {
		return centralController.getControlsAccentColor();
	}
}
