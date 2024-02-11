package net.osmand.plus.card.color.palette;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class ColorsPaletteCardController implements IColorsPaletteUIController {

	private static final Log LOG = PlatformUtil.getLog(ColorsPaletteCardController.class);

	private static final int INVALID_VALUE = -1;

	protected final OsmandApplication app;
	protected final ListStringPreference customColorsPreference;
	protected final List<Integer> defaultColors;
	protected final List<Integer> customColors;
	protected ApplicationMode appMode;

	protected OnColorsPaletteListener onColorsPaletteListener;
	protected ColorsPaletteCard card;
	@ColorInt
	private int selectedColor;

	public ColorsPaletteCardController(@NonNull OsmandApplication app,
	                                   @NonNull List<Integer> defaultColors,
	                                   @NonNull ListStringPreference customColorsPreference,
	                                   @Nullable ApplicationMode appMode,
									   @ColorInt int selectedColor) {
		this.app = app;
		this.customColorsPreference = customColorsPreference;
		this.defaultColors = defaultColors;
		this.customColors = collectCustomColors(customColorsPreference, appMode);
		this.selectedColor = selectedColor;
		this.appMode = appMode;
	}

	@Override
	public void bindCard(@NonNull ColorsPaletteCard card) {
		this.card = card;
	}

	@Override
	public void setColorsPaletteListener(@NonNull OnColorsPaletteListener onColorsPaletteListener) {
		this.onColorsPaletteListener = onColorsPaletteListener;
	}

	@Override
	@ColorInt
	public int getControlsAccentColor() {
		return ColorUtilities.getActiveColor(app, isNightMode());
	}

	@Override
	public boolean onSelectColorFromPalette(@ColorInt int color) {
		if (selectedColor != color) {
			selectedColor = color;
			return true;
		}
		return false;
	}

	@Override
	public void onApplyColorPickerSelection(@Nullable Integer oldColor, @ColorInt int newColor) {
		if (oldColor != null) {
			int index = customColors.indexOf(oldColor);
			if (index != INVALID_VALUE) {
				customColors.set(index, newColor);
			}
			if (selectedColor == oldColor) {
				selectColor(newColor);
			}
		} else {
			customColors.add(newColor);
			selectColor(newColor);
		}
		saveCustomColors();
		card.updateColorsPalette();
		if (onColorsPaletteListener != null) {
			onColorsPaletteListener.onColorAddedToPalette(oldColor, newColor);
		}
	}

	public void selectColor(@ColorInt int color) {
		selectedColor = color;
		if (onColorsPaletteListener != null) {
			onColorsPaletteListener.onColorSelectedFromPalette(color);
		}
	}

	@Override
	public void onColorItemLongClicked(@NonNull FragmentActivity activity, @ColorInt int color) {
		// TODO show pop-up menu for custom colors
		if (isCustomColor(color)) {
			showColorPickerDialog(activity, color);
		}
	}

	@Override
	public void onAddColorButtonClicked(@NonNull FragmentActivity activity) {
		showColorPickerDialog(activity, null);
	}

	@Override
	public void onAllColorsButtonClicked(@NonNull FragmentActivity activity) {
		ColorsPaletteFragment.showInstance(activity, this);
	}

	private void showColorPickerDialog(@NonNull FragmentActivity activity, @Nullable Integer initialColor) {
		ColorPickerDialogController.showDialog(activity, this, initialColor);
	}

	@Override
	public int getSelectedColor() {
		return selectedColor;
	}

	@Override
	public boolean isCustomColor(@ColorInt int color) {
		return !defaultColors.contains(color);
	}

	private boolean isNightMode() {
		return card.isNightMode();
	}

	@NonNull
	@Override
	public List<Integer> getAllColors() {
		return CollectionUtils.asOneList(defaultColors, customColors);
	}

	private void saveCustomColors() {
		List<String> colorNames = new ArrayList<>();
		for (Integer color : customColors) {
			String colorHex = Algorithms.colorToString(color);
			colorNames.add(colorHex);
		}
		if (appMode == null) {
			customColorsPreference.setStringsList(colorNames);
		} else {
			customColorsPreference.setStringsListForProfile(appMode, colorNames);
		}
	}

	public static List<Integer> collectCustomColors(@NonNull ListStringPreference colorsListPreference, ApplicationMode appMode) {
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
					LOG.error(e);
				}
			}
		}
		return colors;
	}
}
