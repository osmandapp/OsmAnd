package net.osmand.plus.mapcontextmenu.editors;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.FlowLayout.LayoutParams;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static net.osmand.data.FavouritePoint.DEFAULT_UI_ICON_ID;

public class IconsCard extends MapBaseCard {

	private static final Log log = PlatformUtil.getLog(IconsCard.class);

	private static final int LAST_USED_ICONS_LIMIT = 20;
	private static final String KEY_LAST_USED_ICONS = "last used icons";

	private List<String> lastUsedIcons;
	private final Map<String, JSONArray> iconsCategories;

	private final String preselectedIconName;
	private String selectedIconCategory;
	@DrawableRes
	private int selectedIconId;
	@ColorInt
	private int selectedColor;

	private FlowLayout iconsSelector;

	@Override
	public int getCardLayoutId() {
		return R.layout.icons_card;
	}

	public IconsCard(@NonNull MapActivity mapActivity,
	                 @DrawableRes int selectedIconId,
	                 @Nullable String preselectedIconName,
	                 @ColorInt int selectedColor) {
		super(mapActivity);
		this.lastUsedIcons = fetchLastUsedIcons();
		this.iconsCategories = collectIconsCategories();
		this.preselectedIconName = preselectedIconName;
		this.selectedIconCategory = getInitialCategory(RenderingIcons.getBigIconName(selectedIconId));
		this.selectedIconId = selectedIconId;
		this.selectedColor = selectedColor;
	}

	@NonNull
	private List<String> fetchLastUsedIcons() {
		List<String> iconsList = app.getSettings().LAST_USED_FAV_ICONS.getStringsList();
		return new ArrayList<>(iconsList == null ? Collections.emptyList() : iconsList);
	}

	@NonNull
	private Map<String, JSONArray> collectIconsCategories() {
		Map<String, JSONArray> iconsCategories = new LinkedHashMap<>();
		if (!Algorithms.isEmpty(lastUsedIcons)) {
			iconsCategories.put(KEY_LAST_USED_ICONS, new JSONArray(lastUsedIcons));
		}

		String categoriesJson = loadCategoriesJsonFromAsset();
		if (categoriesJson != null) {
			iconsCategories.putAll(getPoiIconsByCategories(categoriesJson));
		}

		return iconsCategories;
	}

	@Nullable
	private String loadCategoriesJsonFromAsset() {
		try {
			InputStream is = app.getAssets().open("poi_categories.json");
			return Algorithms.readFromInputStream(is).toString();
		} catch (IOException e) {
			log.error("Failed to parse JSON", e);
			return null;
		}
	}

	@NonNull
	private Map<String, JSONArray> getPoiIconsByCategories(@NonNull String categoriesJson) {
		try {
			Map<String, JSONArray> poiIconsCategories = new LinkedHashMap<>();
			JSONObject obj = new JSONObject(categoriesJson);
			JSONObject categories = obj.getJSONObject("categories");
			for (int i = 0; i < categories.length(); i++) {
				JSONArray names = categories.names();
				if (names != null) {
					String name = names.get(i).toString();
					JSONObject icons = categories.getJSONObject(name);
					String translatedName = AndroidUtils.getIconStringPropertyName(app, name);
					poiIconsCategories.put(translatedName, icons.getJSONArray("icons"));
				}
			}
			return poiIconsCategories;
		} catch (JSONException e) {
			log.error(e.getMessage());
			return Collections.emptyMap();
		}
	}

	@NonNull
	private String getInitialCategory(@Nullable String selectedIconName) {
		String firstCategory = iconsCategories.keySet().iterator().next();
		if (Algorithms.isEmpty(selectedIconName)) {
			return firstCategory;
		}

		for (int j = 0; j < iconsCategories.values().size(); j++) {
			JSONArray iconJsonArray = (JSONArray) iconsCategories.values().toArray()[j];
			for (int i = 0; i < iconJsonArray.length(); i++) {
				try {
					if (iconJsonArray.getString(i).equals(selectedIconName)) {
						return (String) iconsCategories.keySet().toArray()[j];
					}
				} catch (JSONException e) {
					log.error(e.getMessage());
				}
			}
		}
		return firstCategory;
	}

	@Override
	protected void updateContent() {
		setupCategoriesSelector();
		fillIconsSelector();
	}

	@SuppressLint("NotifyDataSetChanged")
	private void setupCategoriesSelector() {
		List<ChipItem> items = new ArrayList<>();
		for (String category : iconsCategories.keySet()) {
			ChipItem item = new ChipItem(category);
			if (!category.equals(KEY_LAST_USED_ICONS)) {
				item.title = category;
			}
			items.add(item);
		}

		HorizontalChipsView categorySelector = view.findViewById(R.id.icons_categories_selector);
		categorySelector.setItems(items);

		ChipItem selected = categorySelector.getChipById(selectedIconCategory);
		categorySelector.setSelected(selected);

		categorySelector.setOnSelectChipListener(chip -> {
			selectedIconCategory = chip.id;
			fillIconsSelector();
			reselectIcon(selectedIconId, false);
			categorySelector.notifyDataSetChanged();
			categorySelector.smoothScrollTo(chip);
			return true;
		});

		ChipItem lastUsedCategory = categorySelector.getChipById(KEY_LAST_USED_ICONS);
		if (lastUsedCategory != null) {
			lastUsedCategory.icon = getIcon(R.drawable.ic_action_history);
			lastUsedCategory.iconColor = ColorUtilities.getActiveColor(app, nightMode);
		}

		categorySelector.notifyDataSetChanged();
		categorySelector.scrollTo(selected);
	}

	private void fillIconsSelector() {
		iconsSelector = view.findViewById(R.id.icons_selector);
		iconsSelector.removeAllViews();
		iconsSelector.setHorizontalAutoSpacing(true);

		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		for (String iconName : getIconNameListToShow()) {
			int width = getDimen(R.dimen.favorites_select_icon_button_right_padding);
			LayoutParams layoutParams = new LayoutParams(width, 0);
			iconsSelector.addView(createIconItemView(inflater, iconName), layoutParams);
		}
	}

	@NonNull
	private List<String> getIconNameListToShow() {
		JSONArray iconJsonArray = iconsCategories.get(selectedIconCategory);
		if (iconJsonArray == null) {
			return Collections.emptyList();
		}

		List<String> iconNameList = new ArrayList<>();
		for (int i = 0; i < iconJsonArray.length(); i++) {
			try {
				String iconName = iconJsonArray.getString(i);
				iconNameList.add(iconName);
			} catch (JSONException e) {
				log.error(e);
			}
		}

		if (!Algorithms.isEmpty(preselectedIconName)) {
			iconNameList.remove(preselectedIconName);
			iconNameList.add(0, preselectedIconName);
		}

		return iconNameList;
	}

	@NonNull
	private View createIconItemView(@NonNull LayoutInflater inflater, @NonNull String iconName) {
		View iconItemView = inflater.inflate(R.layout.point_editor_button, iconsSelector, false);

		int iconId = RenderingIcons.getBigIconResourceId(iconName);
		int validIconId = iconId != 0 ? iconId : DEFAULT_UI_ICON_ID;
		iconItemView.setTag(validIconId);

		ImageView icon = iconItemView.findViewById(R.id.icon);
		AndroidUiHelper.updateVisibility(icon, true);
		setUnselectedIconColor(icon, validIconId);

		ImageView backgroundCircle = iconItemView.findViewById(R.id.background);
		setUnselectedBackground(backgroundCircle);
		backgroundCircle.setOnClickListener(v -> reselectIcon(validIconId, true));

		ImageView outline = iconItemView.findViewById(R.id.outline);
		int outlineColorId = ColorUtilities.getStrokedButtonsOutlineColorId(nightMode);
		outline.setImageDrawable(getColoredIcon(R.drawable.bg_point_circle_contour, outlineColorId));

		return iconItemView;
	}

	private void reselectIcon(@DrawableRes int newIconId, boolean notifyListener) {
		unselectOldIcon(selectedIconId);
		selectNewIcon(newIconId);
		selectedIconId = newIconId;
		if (notifyListener) {
			notifyCardPressed();
		}
	}

	private void unselectOldIcon(@DrawableRes int oldIconId) {
		View oldIconContainer = iconsSelector.findViewWithTag(oldIconId);
		if (oldIconContainer != null) {
			setUnselectedIconColor(oldIconContainer.findViewById(R.id.icon), oldIconId);

			ImageView background = oldIconContainer.findViewById(R.id.background);
			setUnselectedBackground(background);

			oldIconContainer.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
		}
	}

	private void selectNewIcon(@DrawableRes int newIconId) {
		View newIconContainer = iconsSelector.findViewWithTag(newIconId);
		if (newIconContainer != null) {
			ImageView icon = newIconContainer.findViewById(R.id.icon);
			// Intentionally not accessing icons cache here, because cached icons are wrongly
			// positioned in FavoritePointEditorFragment and WptPtEditorFragmentNew
			int whiteColor = ContextCompat.getColor(mapActivity, R.color.color_white);
			icon.setImageDrawable(UiUtilities.createTintedDrawable(mapActivity, newIconId, whiteColor));

			ImageView backgroundCircle = newIconContainer.findViewById(R.id.background);
			backgroundCircle.setImageDrawable(getPaintedIcon(R.drawable.bg_point_circle, selectedColor));

			AndroidUiHelper.updateVisibility(newIconContainer.findViewById(R.id.outline), true);
		}
	}

	private void setUnselectedBackground(@NonNull ImageView background) {
		int inactiveColorId = ColorUtilities.getInactiveButtonsAndLinksColorId(nightMode);
		Drawable backgroundIcon = getColoredIcon(R.drawable.bg_point_circle, inactiveColorId);
		background.setImageDrawable(backgroundIcon);
	}

	private void setUnselectedIconColor(@NonNull ImageView icon, @DrawableRes int iconId) {
		icon.setImageDrawable(UiUtilities.createTintedDrawable(mapActivity, iconId, ContextCompat.getColor(mapActivity, R.color.icon_color_default_light)));
	}

	public void updateSelectedColor(@ColorInt int newColor) {
		selectedColor = newColor;
		reselectIcon(selectedIconId, false);
	}

	@DrawableRes
	public int getSelectedIconId() {
		return selectedIconId;
	}

	@Nullable
	public String getLastUsedIconName() {
		return Algorithms.isEmpty(lastUsedIcons) ? null : lastUsedIcons.get(0);
	}

	public void addLastUsedIcon(@NonNull String iconName) {
		lastUsedIcons.remove(iconName);
		if (lastUsedIcons.size() >= LAST_USED_ICONS_LIMIT - 1) {
			lastUsedIcons = lastUsedIcons.subList(0, LAST_USED_ICONS_LIMIT - 1);
		}
		lastUsedIcons.add(0, iconName);
		app.getSettings().LAST_USED_FAV_ICONS.setStringsList(lastUsedIcons);
	}
}