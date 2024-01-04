package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.track.fragments.CustomColorBottomSheet;
import net.osmand.plus.track.fragments.CustomColorBottomSheet.ColorPickerListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.FlowLayout.LayoutParams;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class ColorsCard extends BaseCard implements ColorPickerListener {

	public static final int MAX_CUSTOM_COLORS = 6;
	public static final double MINIMUM_CONTRAST_RATIO = 1.5;

	private static final Log log = PlatformUtil.getLog(TrackColoringCard.class);

	public static final int INVALID_VALUE = -1;

	private final Fragment targetFragment;

	private final ApplicationMode appMode;
	private final ListStringPreference colorsListPreference;

	private final List<Integer> colors;
	private final List<Integer> customColors;

	private int selectedColor;

	@Override
	public int getCardLayoutId() {
		return R.layout.colors_card;
	}

	public ColorsCard(@NonNull FragmentActivity activity,
	                  @Nullable ApplicationMode appMode,
	                  @Nullable Fragment targetFragment,
	                  @ColorInt int selectedColor,
	                  @NonNull List<Integer> colors,
	                  @NonNull ListStringPreference colorsListPreference,
	                  boolean usedOnMap) {
		super(activity, usedOnMap);
		this.targetFragment = targetFragment;
		this.selectedColor = selectedColor;
		this.colors = colors;
		this.colorsListPreference = colorsListPreference;
		this.customColors = getCustomColors(colorsListPreference, appMode);
		this.appMode = appMode;
	}

	@ColorInt
	public int getSelectedColor() {
		return selectedColor;
	}

	public void setSelectedColor(@ColorInt int selectedColor) {
		this.selectedColor = selectedColor;
		updateContent();
	}

	@Override
	public void onColorSelected(Integer prevColor, int newColor) {
		if (prevColor != null) {
			int index = customColors.indexOf(prevColor);
			if (index != INVALID_VALUE) {
				customColors.set(index, newColor);
			}
			if (selectedColor == prevColor) {
				selectedColor = newColor;
			}
		} else if (customColors.size() < MAX_CUSTOM_COLORS) {
			customColors.add(newColor);
			selectedColor = newColor;
		}
		saveCustomColors();
		updateContent();
	}

	@Override
	protected void updateContent() {
		createColorSelector();
		updateColorSelector(selectedColor);
	}

	private void createColorSelector() {
		FlowLayout selectCustomColor = view.findViewById(R.id.select_custom_color);
		selectCustomColor.removeAllViews();
		selectCustomColor.setHorizontalAutoSpacing(true);
		int minimalPaddingBetweenIcon = getDimen(R.dimen.favorites_select_icon_button_right_padding);

		for (int color : customColors) {
			selectCustomColor.addView(createColorItemView(color, selectCustomColor, true), new LayoutParams(minimalPaddingBetweenIcon, 0));
		}
		if (customColors.size() < 6) {
			selectCustomColor.addView(createAddCustomColorItemView(selectCustomColor), new LayoutParams(minimalPaddingBetweenIcon, 0));
		}

		FlowLayout selectDefaultColor = view.findViewById(R.id.select_default_color);
		selectDefaultColor.removeAllViews();
		selectDefaultColor.setHorizontalAutoSpacing(true);

		for (int color : colors) {
			selectDefaultColor.addView(createColorItemView(color, selectDefaultColor, false), new LayoutParams(minimalPaddingBetweenIcon, 0));
		}
		updateColorSelector(selectedColor);
	}

	private void updateColorSelector(int newColor) {
		View oldColorContainer = view.findViewWithTag(selectedColor);
		if (oldColorContainer != null) {
			oldColorContainer.findViewById(R.id.outline).setVisibility(View.INVISIBLE);
			ImageView icon = oldColorContainer.findViewById(R.id.icon);
			icon.setImageDrawable(UiUtilities.tintDrawable(
					icon.getDrawable(), ColorUtilities.getDefaultIconColor(app, nightMode)));
		}
		View newColorContainer = view.findViewWithTag(newColor);
		if (newColorContainer != null) {
			AppCompatImageView outline = newColorContainer.findViewById(R.id.outline);
			Drawable border = app.getUIUtilities().getPaintedIcon(R.drawable.bg_point_circle_contour, newColor);
			outline.setImageDrawable(border);
			outline.setVisibility(View.VISIBLE);
		}
	}

	private View createColorItemView(@ColorInt int color, FlowLayout rootView, boolean customColor) {
		View colorItemView = createCircleView(rootView);

		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);

		Drawable transparencyIcon = getTransparencyIcon(app, color);
		Drawable colorIcon = app.getUIUtilities().getPaintedIcon(R.drawable.bg_point_circle, color);
		Drawable layeredIcon = UiUtilities.getLayeredIcon(transparencyIcon, colorIcon);
		int listBgColor = ColorUtilities.getCardAndListBackgroundColor(app, nightMode);
		double contrastRatio = ColorUtils.calculateContrast(color, listBgColor);
		if (contrastRatio < MINIMUM_CONTRAST_RATIO) {
			backgroundCircle.setBackgroundResource(nightMode ? R.drawable.circle_contour_bg_dark : R.drawable.circle_contour_bg_light);
		}
		backgroundCircle.setImageDrawable(layeredIcon);
		backgroundCircle.setOnClickListener(v -> {
			updateColorSelector(color);
			selectedColor = color;
			notifyCardPressed();
		});
		if (customColor) {
			backgroundCircle.setOnLongClickListener(v -> {
				CustomColorBottomSheet.showInstance(activity.getSupportFragmentManager(), targetFragment, color);
				return false;
			});
		}
		colorItemView.setTag(color);
		return colorItemView;
	}

	private View createAddCustomColorItemView(FlowLayout rootView) {
		View colorItemView = createCircleView(rootView);
		ImageView backgroundCircle = colorItemView.findViewById(R.id.background);

		int bgColorId = ColorUtilities.getActivityBgColorId(nightMode);
		Drawable backgroundIcon = app.getUIUtilities().getIcon(R.drawable.bg_point_circle, bgColorId);

		ImageView icon = colorItemView.findViewById(R.id.icon);
		icon.setVisibility(View.VISIBLE);
		int activeColorResId = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_plus, activeColorResId));

		backgroundCircle.setImageDrawable(backgroundIcon);
		backgroundCircle.setOnClickListener(v -> CustomColorBottomSheet.showInstance(activity.getSupportFragmentManager(), targetFragment, null));
		return colorItemView;
	}

	@NonNull
	private View createCircleView(@NonNull FlowLayout rootView) {
		return themedInflater.inflate(R.layout.point_editor_button, rootView, false);
	}

	private Drawable getTransparencyIcon(OsmandApplication app, @ColorInt int color) {
		int colorWithoutAlpha = ColorUtilities.removeAlpha(color);
		int transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f);
		return app.getUIUtilities().getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor);
	}

	public static List<Integer> getCustomColors(ListStringPreference colorsListPreference) {
		return getCustomColors(colorsListPreference, null);
	}

	public static List<Integer> getCustomColors(ListStringPreference colorsListPreference, ApplicationMode appMode) {
		List<Integer> colors = new ArrayList<>();
		List<String> colorNames;
		if (appMode == null) {
			colorNames = colorsListPreference.getStringsList();
		} else {
			colorNames = colorsListPreference.getStringsListForProfile(appMode);
		}
		if (colorNames != null) {
			for (String colorHex : colorNames) {
				try {
					if (!Algorithms.isEmpty(colorHex)) {
						int color = Algorithms.parseColor(colorHex);
						colors.add(color);
					}
				} catch (IllegalArgumentException e) {
					log.error(e);
				}
			}
		}
		return colors;
	}

	public boolean isBaseColor(int color) {
		return colors.contains(color);
	}

	private void saveCustomColors() {
		List<String> colorNames = new ArrayList<>();
		for (Integer color : customColors) {
			String colorHex = Algorithms.colorToString(color);
			colorNames.add(colorHex);
		}
		if (appMode == null) {
			colorsListPreference.setStringsList(colorNames);
		} else {
			colorsListPreference.setStringsListForProfile(appMode, colorNames);
		}
	}
}