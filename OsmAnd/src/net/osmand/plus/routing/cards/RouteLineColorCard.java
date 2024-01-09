package net.osmand.plus.routing.cards;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.settings.fragments.HeaderInfo;
import net.osmand.plus.settings.fragments.HeaderUiAdapter;
import net.osmand.plus.track.AppearanceViewHolder;
import net.osmand.plus.track.cards.ColoringTypeCard;
import net.osmand.plus.track.cards.ColorsCard;
import net.osmand.plus.track.fragments.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.router.RouteStatisticsHelper.ROUTE_INFO_PREFIX;


public class RouteLineColorCard extends MapBaseCard implements CardListener, ColorPickerListener, HeaderInfo {

	private static final String IS_NIGHT_MAP_THEME = "is_night_map_theme";

	private static final int DAY_TITLE_ID = R.string.day;
	private static final int NIGHT_TITLE_ID = R.string.night;

	private final Fragment targetFragment;
	private final HeaderUiAdapter headerUiAdapter;

	private ColorsCard colorsCard;
	private ColoringTypeCard coloringTypeCard;
	private PromoBannerCard promoCard;
	private ColorTypeAdapter colorAdapter;
	private RecyclerView groupRecyclerView;
	private TextView tvDescription;
	private View themeToggleContainer;
	private ViewGroup cardsContainer;

	private ColoringType selectedType;
	private String selectedRouteInfoAttribute;
	private final PreviewRouteLineInfo previewRouteLineInfo;
	private boolean isNightMapTheme;
	private final ApplicationMode appMode;

	public RouteLineColorCard(@NonNull MapActivity mapActivity,
	                          @NonNull Fragment targetFragment,
	                          @NonNull PreviewRouteLineInfo previewRouteLineInfo,
	                          @NonNull HeaderUiAdapter headerUiAdapter,
	                          @NonNull ApplicationMode appMode,
	                          @Nullable Bundle savedInstanceState) {
		super(mapActivity);
		this.targetFragment = targetFragment;
		this.previewRouteLineInfo = previewRouteLineInfo;
		this.headerUiAdapter = headerUiAdapter;
		this.appMode = appMode;
		selectedType = previewRouteLineInfo.getRouteColoringType();
		selectedRouteInfoAttribute = previewRouteLineInfo.getRouteInfoAttribute();

		Boolean nightMapTheme = null;
		if (savedInstanceState != null) {
			nightMapTheme = savedInstanceState.getBoolean(IS_NIGHT_MAP_THEME);
		}
		if (nightMapTheme == null) {
			DayNightHelper dayNightHelper = app.getDaynightHelper();
			nightMapTheme = dayNightHelper.isNightModeForMapControls();
		}
		isNightMapTheme = nightMapTheme;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_line_color_card;
	}

	@Override
	protected void updateContent() {
		tvDescription = view.findViewById(R.id.description);

		colorAdapter = new ColorTypeAdapter();
		groupRecyclerView = view.findViewById(R.id.recycler_view);
		groupRecyclerView.setAdapter(colorAdapter);
		groupRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));

		themeToggleContainer = view.findViewById(R.id.theme_toggle_container);
		LinearLayout radioGroup = view.findViewById(R.id.custom_radio_buttons);
		setupRadioGroup(radioGroup);

		cardsContainer = view.findViewById(R.id.colors_card_container);
		createCards(cardsContainer);
		refreshSelectedMode();
	}

	private void refreshSelectedMode() {
		selectedType = previewRouteLineInfo.getRouteColoringType();
		selectedRouteInfoAttribute = previewRouteLineInfo.getRouteInfoAttribute();
		onModeChanged();
	}

	private void onModeChanged() {
		AndroidUiHelper.updateVisibility(themeToggleContainer, selectedType.isCustomColor());
		colorsCard.updateVisibility(selectedType.isCustomColor());
		coloringTypeCard.setColoringType(selectedType);
		onMapThemeChanged();

		previewRouteLineInfo.setRouteColoringType(selectedType);
		previewRouteLineInfo.setRouteInfoAttribute(selectedRouteInfoAttribute);
		updatePromoCardVisibility();
		updateColorItems();
		updateDescription();
	}

	private void updatePromoCardVisibility() {
		boolean available = isSelectedModeAvailable();
		if (!available) {
			promoCard.updateVisibility(true);
			coloringTypeCard.updateVisibility(false);
			colorsCard.updateVisibility(false);
		} else {
			promoCard.updateVisibility(false);
		}
	}

	public boolean isSelectedModeAvailable() {
		return selectedType.isAvailableInSubscription(app, selectedRouteInfoAttribute, true);
	}

	private void setupRadioGroup(LinearLayout buttonsContainer) {
		TextRadioItem day = createMapThemeButton(false);
		TextRadioItem night = createMapThemeButton(true);

		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(day, night);

		radioGroup.setSelectedItem(isNightMapTheme ? night : day);
	}

	private TextRadioItem createMapThemeButton(boolean isNight) {
		TextRadioItem item = new TextRadioItem(app.getString(!isNight ? DAY_TITLE_ID : NIGHT_TITLE_ID));
		item.setOnClickListener((radioItem, view) -> {
			isNightMapTheme = isNight;
			onMapThemeChanged();
			updateDescription();
			return true;
		});
		return item;
	}

	private void onMapThemeChanged() {
		if (targetFragment instanceof OnMapThemeChangeListener) {
			((OnMapThemeChangeListener) targetFragment).onMapThemeChanged();
		}
		if (selectedType == ColoringType.CUSTOM_COLOR) {
			colorsCard.setSelectedColor(getCustomRouteColor());
			updateColorItems();
		}
	}

	private void createCards(ViewGroup container) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<Integer> colors = new ArrayList<>();
			for (int color : ColorDialogs.pallette) {
				colors.add(color);
			}
			int selectedColorDay = getSelectedColorForTheme(colors, false);
			int selectedColorNight = getSelectedColorForTheme(colors, true);
			int selectedColor = isNightMap() ? selectedColorNight : selectedColorDay;
			ListStringPreference preference = app.getSettings().CUSTOM_ROUTE_LINE_COLORS;
			colorsCard = new ColorsCard(mapActivity, null, targetFragment, selectedColor,
					colors, preference, true);
			colorsCard.setListener(this);
			container.addView(colorsCard.build(mapActivity));

			coloringTypeCard = new ColoringTypeCard(mapActivity, null, previewRouteLineInfo.getRouteColoringType());
			container.addView(coloringTypeCard.build(mapActivity));

			promoCard = new PromoBannerCard(mapActivity, true);
			container.addView(promoCard.build(mapActivity));
		}
	}

	private int getSelectedColorForTheme(List<Integer> colors, boolean nightMode) {
		int color = previewRouteLineInfo.getCustomColor(nightMode);
		if (!ColorDialogs.isPaletteColor(color)) {
			colors.add(color);
		}
		return color;
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		updateSelectedCustomColor();
	}

	private int getCustomRouteColor() {
		return previewRouteLineInfo.getCustomColor(isNightMap());
	}

	private void updateSelectedCustomColor() {
		int selectedColor = colorsCard.getSelectedColor();
		previewRouteLineInfo.setCustomColor(selectedColor, isNightMap());
		updateColorItems();
	}

	private void updateColorItems() {
		if (targetFragment instanceof OnSelectedColorChangeListener) {
			((OnSelectedColorChangeListener) targetFragment).onSelectedColorChanged();
		}
		updateHeader();
	}

	@Override
	public void onNeedUpdateHeader() {
		updateHeader();
	}

	private void updateHeader() {
		String title = app.getString(R.string.shared_string_color);
		String colorName = getColorName();
		headerUiAdapter.onUpdateHeader(this, title, colorName);
	}

	@NonNull
	private String getColorName() {
		String colorName = "";
		if (selectedType.isDefault() || selectedType.isGradient() || selectedType.isRouteInfoAttribute()) {
			colorName = selectedType.getHumanString(app, selectedRouteInfoAttribute);
		} else {
			int colorNameId = ColorDialogs.getColorName(getCustomRouteColor());
			colorName = app.getString(colorNameId);
		}
		return colorName;
	}

	private void updateDescription() {
		String description;
		if (selectedType.isDefault()) {
			String pattern = app.getString(R.string.route_line_use_map_style_color);
			description = String.format(pattern, app.getRendererRegistry().getSelectedRendererName());
		} else if (selectedType.isCustomColor()) {
			String pattern = app.getString(R.string.specify_color_for_map_mode);
			String mapModeTitle = app.getString(isNightMap() ? NIGHT_TITLE_ID : DAY_TITLE_ID);
			description = String.format(pattern, mapModeTitle.toLowerCase());
		} else if (selectedType.isRouteInfoAttribute()) {
			String key = selectedRouteInfoAttribute.replaceAll(ROUTE_INFO_PREFIX, "");
			description = AndroidUtils.getStringRouteInfoPropertyDescription(app, key);
		} else {
			description = app.getString(R.string.route_line_use_gradient_coloring);
		}
		AndroidUiHelper.updateVisibility(tvDescription, description != null);
		tvDescription.setText(description != null ? description : "");
	}

	private boolean isNightMap() {
		if (selectedType.isCustomColor()) {
			return isNightMapTheme;
		} else {
			return app.getDaynightHelper().isNightModeForMapControlsForProfile(appMode);
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) { }

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ColorsCard) {
			updateSelectedCustomColor();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) { }

	public void saveToBundle(@NonNull Bundle outState) {
		outState.putBoolean(IS_NIGHT_MAP_THEME, isNightMapTheme);
	}

	@Nullable
	public DayNightMode getSelectedMapTheme() {
		return selectedType.isCustomColor() ?
				isNightMapTheme ? DayNightMode.NIGHT : DayNightMode.DAY
				: null;
	}

	private class ColorTypeAdapter extends RecyclerView.Adapter<AppearanceViewHolder> {

		private final List<String> coloringTypes = new ArrayList<>();

		public ColorTypeAdapter() {
			coloringTypes.addAll(listStaticColoringTypes());
			coloringTypes.addAll(listRouteInfoAttributes());
		}

		private List<String> listStaticColoringTypes() {
			List<String> coloringTypes = new ArrayList<>();
			for (ColoringType coloringType : ColoringType.getRouteColoringTypes()) {
				if (!coloringType.isRouteInfoAttribute()) {
					coloringTypes.add(coloringType.getName(null));
				}
			}
			return coloringTypes;
		}

		private List<String> listRouteInfoAttributes() {
			RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
			RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
			return RouteStatisticsHelper.getRouteStatisticAttrsNames(currentRenderer, defaultRenderer, true);
		}

		@NonNull
		@Override
		public AppearanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = getDimen(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = getDimen(R.dimen.gpx_group_button_height);
			((TextView) view.findViewById(R.id.groupName)).setMaxLines(1);

			AppearanceViewHolder holder = new AppearanceViewHolder(view);
			AndroidUtils.setBackground(app, holder.button, nightMode, R.drawable.ripple_solid_light_6dp,
					R.drawable.ripple_solid_dark_6dp);
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull AppearanceViewHolder holder, int position) {
			String coloringTypeName = coloringTypes.get(position);
			ColoringType coloringType = ColoringType.getRouteColoringTypeByName(coloringTypeName);

			holder.title.setText(coloringType.getHumanString(app, coloringTypeName));
			updateButtonBg(holder, coloringType, coloringTypeName);
			updateTextAndIconColor(holder, coloringType, coloringTypeName);

			holder.itemView.setOnClickListener(view -> {
				selectedType = coloringType;
				selectedRouteInfoAttribute = coloringTypeName;
				notifyItemRangeChanged(0, getItemCount());

				onModeChanged();
				notifyCardPressed();
			});
		}

		private void updateButtonBg(AppearanceViewHolder holder, ColoringType coloringType,
		                            String coloringTypeName) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources
					.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				if (isItemSelected(coloringType, coloringTypeName)) {
					int strokeColorRes = ColorUtilities.getActiveColorId(nightMode);
					int strokeColor = ContextCompat.getColor(app, strokeColorRes);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 2), strokeColor);
				} else {
					int strokeColor = ContextCompat.getColor(app, nightMode ?
							R.color.stroked_buttons_and_links_outline_dark :
							R.color.stroked_buttons_and_links_outline_light);
					rectContourDrawable.setStroke(AndroidUtils.dpToPx(app, 1), strokeColor);
				}
				holder.button.setImageDrawable(rectContourDrawable);
			}
		}

		private void updateTextAndIconColor(AppearanceViewHolder holder, ColoringType coloringType,
		                                    String coloringTypeName) {
			Context ctx = holder.itemView.getContext();
			int iconColorId;
			int textColorId;

			if (isItemSelected(coloringType, coloringTypeName)) {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.default_icon_color);
				textColorId = AndroidUtils.getColorFromAttr(ctx, android.R.attr.textColor);
			} else {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.colorPrimary);
				textColorId = iconColorId;
			}

			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(coloringType.getIconId(), iconColorId));
			holder.title.setTextColor(textColorId);
		}

		private boolean isItemSelected(ColoringType coloringType, String coloringTypeName) {
			if (coloringType.isRouteInfoAttribute()) {
				return Algorithms.objectEquals(selectedRouteInfoAttribute, coloringTypeName)
						&& selectedType == coloringType;
			} else {
				return selectedType == coloringType;
			}
		}

		@Override
		public int getItemCount() {
			return coloringTypes.size();
		}
	}

	public interface OnSelectedColorChangeListener {
		void onSelectedColorChanged();
	}

	public interface OnMapThemeChangeListener {
		void onMapThemeChanged();
	}
}