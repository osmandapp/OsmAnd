package net.osmand.plus.card.color.palette;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public abstract class BaseColorsPaletteController implements IColorsPaletteUIController {

	private static final Log LOG = PlatformUtil.getLog(BaseColorsPaletteController.class);

	private static final int INVALID_VALUE = -1;

	protected final OsmandApplication app;
	protected final ListStringPreference customColorsPreference;
	protected final List<Integer> defaultColors;
	protected final List<Integer> customColors;
	protected ApplicationMode appMode;

	protected OnColorsPaletteListener externalPaletteListener;
	protected List<WeakReference<IColorsPalette>> palettes = new ArrayList<>();
	@ColorInt
	private int selectedColor;

	public BaseColorsPaletteController(@NonNull OsmandApplication app,
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
	public void bindPalette(@NonNull IColorsPalette palette) {
		this.palettes.add(new WeakReference<>(palette));
	}

	@Override
	public void unbindPalette(@NonNull IColorsPalette palette) {
		WeakReference<IColorsPalette> referenceToRemove = null;
		for (WeakReference<IColorsPalette> reference : palettes) {
			if (Objects.equals(palette, reference.get())) {
				referenceToRemove = reference;
				break;
			}
		}
		if (referenceToRemove != null) {
			palettes.remove(referenceToRemove);
		}
	}

	private void notifyUpdatePaletteSelection(@ColorInt int oldColor, @ColorInt int newColor) {
		for (IColorsPalette palette : collectActivePalettes()) {
			palette.updatePaletteSelection(oldColor, newColor);
		}
	}

	private void notifyUpdatePalette() {
		for (IColorsPalette palette : collectActivePalettes()) {
			palette.updatePalette();
		}
	}

	@Override
	public void setPaletteListener(@NonNull OnColorsPaletteListener onColorsPaletteListener) {
		this.externalPaletteListener = onColorsPaletteListener;
	}

	@Override
	@ColorInt
	public int getControlsAccentColor(boolean nightMode) {
		return ColorUtilities.getActiveColor(app, nightMode);
	}

	@Override
	public void onSelectColorFromPalette(@ColorInt int color) {
		if (selectedColor != color) {
			int oldColor = selectedColor;
			selectedColor = color;
			notifyUpdatePaletteSelection(oldColor, selectedColor);
		}
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
		notifyUpdatePalette();
		if (externalPaletteListener != null) {
			externalPaletteListener.onColorAddedToPalette(oldColor, newColor);
		}
	}

	public void selectColor(@ColorInt int color) {
		selectedColor = color;
		if (externalPaletteListener != null) {
			externalPaletteListener.onColorSelectedFromPalette(color);
		}
	}

	@Override
	public void onColorItemLongClicked(@NonNull FragmentActivity activity, @NonNull View view,
	                                   @ColorInt int color, boolean nightMode) {
		if (isCustomColor(color)) {
			showItemPopUpMenu(activity, view, color, nightMode);
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

	private void showItemPopUpMenu(@NonNull FragmentActivity activity, @NonNull View view,
	                               @ColorInt int color, boolean nightMode) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();

		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_edit)
				.setIcon(getContentIcon(R.drawable.ic_action_appearance_outlined))
				.setOnClickListener(v -> {
					showColorPickerDialog(activity, color);
				})
				.create()
		);

		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_duplicate)
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> {
					duplicateCustomColor(color);
				})
				.create()
		);

		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_remove)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.showTopDivider(true)
				.setOnClickListener(v -> {
					deleteCustomColor(color);
				})
				.create()
		);

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void showColorPickerDialog(@NonNull FragmentActivity activity, @Nullable Integer initialColor) {
		ColorPickerDialogController.showDialog(activity, this, initialColor);
	}

	private void duplicateCustomColor(@ColorInt int color) { // TODO fix duplicates may cause the same color selection
		if (isCustomColor(color)) {
			int index = customColors.indexOf(color);
			if (index < customColors.size()) {
				customColors.set(index + 1, color);
			} else {
				customColors.add(color);
			}
			saveCustomColors();
			notifyUpdatePalette();
		}
	}

	private void deleteCustomColor(@ColorInt int color) {
		if (isCustomColor(color)) { // TODO fix possible problem if we remove selected color
			customColors.remove((Integer) color);
			saveCustomColors();
			notifyUpdatePalette();
		}
	}

	@Override
	public int getSelectedColor() {
		return selectedColor;
	}

	@Override
	public boolean isCustomColor(@ColorInt int color) {
		return !defaultColors.contains(color);
	}

	@NonNull
	@Override
	public List<Integer> getAllColors() {
		return CollectionUtils.asOneList(defaultColors, customColors);
	}

	@NonNull
	private List<IColorsPalette> collectActivePalettes() {
		List<IColorsPalette> result = new ArrayList<>();
		Iterator<WeakReference<IColorsPalette>> it = palettes.iterator();
		while (it.hasNext()) {
			IColorsPalette palette = it.next().get();
			if (palette != null) {
				result.add(palette);
			} else {
				it.remove();
			}
		}
		return result;
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

	protected Drawable getContentIcon(@DrawableRes int id) {
		return app.getUIUtilities().getThemedIcon(id);
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
