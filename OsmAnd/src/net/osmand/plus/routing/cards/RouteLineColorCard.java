package net.osmand.plus.routing.cards;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ColorDialogs;
import net.osmand.plus.helpers.enums.DayNightMode;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.backend.ListStringPreference;
import net.osmand.plus.settings.fragments.HeaderInfo;
import net.osmand.plus.settings.fragments.HeaderUiAdapter;
import net.osmand.plus.track.AppearanceViewHolder;
import net.osmand.plus.track.ColorsCard;
import net.osmand.plus.track.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.track.GradientCard;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class RouteLineColorCard extends MapBaseCard implements CardListener, ColorPickerListener, HeaderInfo {

	private static final int DAY_TITLE_ID = R.string.day;
	private static final int NIGHT_TITLE_ID = R.string.night;

	private final Fragment targetFragment;
	private HeaderUiAdapter headerUiAdapter;

	private ColorsCard colorsCard;
	private GradientCard gradientCard;
	private ColorTypeAdapter colorAdapter;
	private RecyclerView groupRecyclerView;
	private TextView tvDescription;
	private View themeToggleContainer;
	private ViewGroup cardsContainer;

	private ColorMode selectedMode;
	private PreviewRouteLineInfo previewRouteLineInfo;
	private DayNightMode initMapTheme;
	private DayNightMode selectedMapTheme;

	private enum ColorMode {
		DEFAULT(R.string.map_widget_renderer, R.drawable.ic_action_map_style),
		CUSTOM(R.string.shared_string_custom, R.drawable.ic_action_settings),
		ALTITUDE(R.string.altitude, R.drawable.ic_action_hillshade_dark),
		SLOPE(R.string.shared_string_slope, R.drawable.ic_action_altitude_ascent);

		ColorMode(int titleId, int iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}

		int titleId;
		int iconId;
	}

	public RouteLineColorCard(@NonNull MapActivity mapActivity,
	                          @NonNull Fragment targetFragment,
	                          @NonNull PreviewRouteLineInfo previewRouteLineInfo,
	                          @NonNull DayNightMode initMapTheme,
	                          @NonNull DayNightMode selectedMapTheme,
	                          @NonNull HeaderUiAdapter headerUiAdapter) {
		super(mapActivity);
		this.targetFragment = targetFragment;
		this.previewRouteLineInfo = previewRouteLineInfo;
		this.initMapTheme = initMapTheme;
		this.selectedMapTheme = selectedMapTheme;
		this.headerUiAdapter = headerUiAdapter;
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
		LinearLayout radioGroup = (LinearLayout) view.findViewById(R.id.custom_radio_buttons);
		setupRadioGroup(radioGroup);

		cardsContainer = (ViewGroup) view.findViewById(R.id.colors_card_container);
		createCards(cardsContainer);

		initSelectedMode();
	}

	private void initSelectedMode() {
		if (previewRouteLineInfo.getGradientScaleType() == GradientScaleType.ALTITUDE) {
			selectedMode = ColorMode.ALTITUDE;
		} else if (previewRouteLineInfo.getGradientScaleType() == GradientScaleType.SLOPE) {
			selectedMode = ColorMode.SLOPE;
		} else {
			selectedMode = getRouteLineColor() == null ? ColorMode.DEFAULT : ColorMode.CUSTOM;
		}
		modeChanged();
	}

	private void modeChanged() {
		if (selectedMode == ColorMode.DEFAULT) {
			themeToggleContainer.setVisibility(View.GONE);
			AndroidUiHelper.updateVisibility(colorsCard.getView(), false);
			AndroidUiHelper.updateVisibility(gradientCard.getView(), false);
			previewRouteLineInfo.setUseDefaultColor(true);
			changeMapTheme(initMapTheme);
		} else if (selectedMode == ColorMode.CUSTOM) {
			themeToggleContainer.setVisibility(View.VISIBLE);
			AndroidUiHelper.updateVisibility(colorsCard.getView(), true);
			AndroidUiHelper.updateVisibility(gradientCard.getView(), false);
			previewRouteLineInfo.setUseDefaultColor(false);
			changeMapTheme(isNightMap() ? DayNightMode.NIGHT : DayNightMode.DAY);
		} else {
			gradientCard.setSelectedScaleType(getGradientScaleTypeFromMode());
			AndroidUiHelper.updateVisibility(colorsCard.getView(), false);
			AndroidUiHelper.updateVisibility(gradientCard.getView(), true);
			themeToggleContainer.setVisibility(View.GONE);
			previewRouteLineInfo.setUseDefaultColor(false);
			changeMapTheme(initMapTheme);
		}
		previewRouteLineInfo.setGradientScaleType(getGradientScaleTypeFromMode());
		updateColorItems();
		updateDescription();
	}

	private void setupRadioGroup(LinearLayout buttonsContainer) {
		TextRadioItem day = createMapThemeButton(false);
		TextRadioItem night = createMapThemeButton(true);

		TextToggleButton radioGroup = new TextToggleButton(app, buttonsContainer, nightMode);
		radioGroup.setItems(day, night);

		radioGroup.setSelectedItem(!isNightMap() ? day : night);
	}

	private TextRadioItem createMapThemeButton(final boolean isNight) {
		TextRadioItem item = new TextRadioItem(app.getString(!isNight ? DAY_TITLE_ID : NIGHT_TITLE_ID));
		item.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				selectedMapTheme = isNight ? DayNightMode.NIGHT : DayNightMode.DAY;
				changeMapTheme(selectedMapTheme);
				updateDescription();
				return true;
			}
		});
		return item;
	}

	private void changeMapTheme(DayNightMode mapTheme) {
		if (targetFragment instanceof OnMapThemeUpdateListener) {
			((OnMapThemeUpdateListener) targetFragment).onMapThemeUpdated(mapTheme);
		}
		if (selectedMode == ColorMode.CUSTOM) {
			Integer color = getRouteLineColor();
			if (color != null) {
				colorsCard.setSelectedColor(color);
			}
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
			colorsCard = new ColorsCard(mapActivity, selectedColor, targetFragment, colors, preference, null);
			colorsCard.setListener(this);
			container.addView(colorsCard.build(mapActivity));

			gradientCard = new GradientCard(mapActivity, previewRouteLineInfo.getGradientScaleType());
			container.addView(gradientCard.build(mapActivity));
		}
	}

	private int getSelectedColorForTheme(List<Integer> colors, boolean nightMode) {
		Integer color = previewRouteLineInfo.getColorIgnoreDefault(nightMode);
		if (color != null) {
			if (!ColorDialogs.isPaletteColor(color)) {
				colors.add(color);
			}
		} else {
			color = colors.get(0);
			previewRouteLineInfo.setUseDefaultColor(true);
			previewRouteLineInfo.setColor(color, nightMode);
		}
		return color;
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		colorsCard.onColorSelected(prevColor, newColor);
		updateSelectedColor();
	}

	@Nullable
	private Integer getRouteLineColor() {
		return previewRouteLineInfo.getColor(isNightMap());
	}

	@Nullable
	private GradientScaleType getGradientScaleTypeFromMode() {
		if (selectedMode == ColorMode.ALTITUDE) {
			return GradientScaleType.ALTITUDE;
		} else if (selectedMode == ColorMode.SLOPE) {
			return GradientScaleType.SLOPE;
		} else {
			return null;
		}
	}

	private void updateSelectedColor() {
		int selectedColor = colorsCard.getSelectedColor();
		previewRouteLineInfo.setColor(selectedColor, isNightMap());
		updateColorItems();
	}

	private void updateColorItems() {
		if (targetFragment instanceof OnSelectedColorChangeListener) {
			((OnSelectedColorChangeListener) targetFragment).onSelectedColorChanged();
		}
		updateColorName();
	}

	@Override
	public void onNeedUpdateHeader() {
		updateColorName();
	}

	private void updateColorName() {
		String title = app.getString(R.string.shared_string_color);
		String colorName = "";
		if (selectedMode == ColorMode.DEFAULT) {
			colorName = app.getString(R.string.map_widget_renderer);
		} else if (selectedMode == ColorMode.ALTITUDE || selectedMode == ColorMode.SLOPE) {
			colorName = app.getString(selectedMode.titleId);
		} else if (getRouteLineColor() != null) {
			int colorNameId = ColorDialogs.getColorName(getRouteLineColor());
			colorName = app.getString(colorNameId);
		}
		headerUiAdapter.onUpdateHeader(this, title, colorName);
	}

	private void updateDescription() {
		String description;
		if (selectedMode == ColorMode.DEFAULT) {
			String pattern = app.getString(R.string.route_line_use_map_style_color);
			description = String.format(pattern, app.getRendererRegistry().getSelectedRendererName());
		} else if (selectedMode == ColorMode.CUSTOM) {
			String pattern = app.getString(R.string.specify_color_for_map_mode);
			String mapModeTitle = app.getString(isNightMap() ? NIGHT_TITLE_ID : DAY_TITLE_ID);
			description = String.format(pattern, mapModeTitle.toLowerCase());
		} else {
			description = app.getString(R.string.route_line_use_gradient_coloring);
		}
		tvDescription.setText(description);
	}

	private boolean isNightMap() {
		return selectedMapTheme.isNight();
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {

	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ColorsCard) {
			updateSelectedColor();
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {

	}

	private class ColorTypeAdapter extends RecyclerView.Adapter<AppearanceViewHolder> {

		private List<ColorMode> items = Arrays.asList(ColorMode.values());

		@NonNull
		@Override
		public AppearanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater themedInflater = UiUtilities.getInflater(parent.getContext(), nightMode);
			View view = themedInflater.inflate(R.layout.point_editor_group_select_item, parent, false);
			view.getLayoutParams().width = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_width);
			view.getLayoutParams().height = app.getResources().getDimensionPixelSize(R.dimen.gpx_group_button_height);

			AppearanceViewHolder holder = new AppearanceViewHolder(view);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, holder.button, nightMode, R.drawable.ripple_solid_light_6dp,
						R.drawable.ripple_solid_dark_6dp);
			}
			return holder;
		}

		@Override
		public void onBindViewHolder(@NonNull final AppearanceViewHolder holder, int position) {
			ColorMode item = items.get(position);
			holder.title.setText(app.getString(item.titleId));

			updateButtonBg(holder, item);
			updateTextAndIconColor(holder, item);

			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectedMode = items.get(holder.getAdapterPosition());
					notifyItemRangeChanged(0, getItemCount());

					modeChanged();

					CardListener listener = getListener();
					if (listener != null) {
						listener.onCardPressed(RouteLineColorCard.this);
					}
				}
			});
		}

		private void updateButtonBg(AppearanceViewHolder holder, ColorMode item) {
			GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(app, R.drawable.bg_select_group_button_outline);
			if (rectContourDrawable != null) {
				if (selectedMode == item) {
					int strokeColor = ContextCompat.getColor(app, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
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

		private void updateTextAndIconColor(AppearanceViewHolder holder, ColorMode item) {
			Context ctx = holder.itemView.getContext();
			int iconColorId;
			int textColorId;

			if (selectedMode == item) {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.default_icon_color);
				textColorId = AndroidUtils.getColorFromAttr(ctx, android.R.attr.textColor);
			} else {
				iconColorId = AndroidUtils.getColorFromAttr(ctx, R.attr.colorPrimary);
				textColorId = iconColorId;
			}

			holder.icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(item.iconId, iconColorId));
			holder.title.setTextColor(textColorId);
		}

		@Override
		public int getItemCount() {
			return items.size();
		}
	}

	public interface OnSelectedColorChangeListener {
		void onSelectedColorChanged();
	}

	public interface OnMapThemeUpdateListener {
		void onMapThemeUpdated(@NonNull DayNightMode mapTheme);
	}

}