package net.osmand.plus.card.color.palette.main;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ColorsPaletteController implements IColorsPaletteController {

	protected final OsmandApplication app;
	protected OnColorsPaletteListener externalListener;
	protected List<WeakReference<IColorsPalette>> palettes = new ArrayList<>();
	protected ColorsCollection colorsCollection;

	protected PaletteColor editedPaletteColor;
	protected PaletteColor selectedPaletteColor;

	public ColorsPaletteController(@NonNull OsmandApplication app,
	                               @NonNull ColorsCollection colorsCollection,
	                               @ColorInt int selectedColorInt) {
		this.app = app;
		this.colorsCollection = colorsCollection;
		this.selectedPaletteColor = colorsCollection.findPaletteColor(selectedColorInt);
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

	protected void notifyUpdatePaletteSelection(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
		for (IColorsPalette palette : collectActivePalettes()) {
			palette.updatePaletteSelection(oldColor, newColor);
		}
	}

	private void notifyUpdatePaletteColors(@Nullable PaletteColor targetPaletteColor) {
		for (IColorsPalette palette : collectActivePalettes()) {
			palette.updatePaletteColors(targetPaletteColor);
		}
	}

	@Override
	public void setPaletteListener(@NonNull OnColorsPaletteListener onColorsPaletteListener) {
		this.externalListener = onColorsPaletteListener;
	}

	@Override
	@ColorInt
	public int getControlsAccentColor(boolean nightMode) {
		return ColorUtilities.getActiveColor(app, nightMode);
	}

	@Override
	public boolean isAccentColorCanBeChanged() {
		return false;
	}

	@Override
	public void onSelectColorFromPalette(@NonNull PaletteColor color) {
		if (!Objects.equals(selectedPaletteColor, color)) {
			PaletteColor oldSelectedColor = selectedPaletteColor;
			selectColor(color);
			notifyUpdatePaletteSelection(oldSelectedColor, selectedPaletteColor);
		}
	}

	@Override
	public void onApplyColorPickerSelection(@Nullable Integer oldColor, @ColorInt int newColor) {
		PaletteColor paletteColor = colorsCollection.addOrUpdateColor(editedPaletteColor, newColor);
		if (paletteColor != null) {
			notifyUpdatePaletteColors(paletteColor);
			if (externalListener != null) {
				externalListener.onColorAddedToPalette(editedPaletteColor, paletteColor);
			}
			if (oldColor == null || Objects.equals(editedPaletteColor, selectedPaletteColor)) {
				selectColor(paletteColor);
			}
		}
		editedPaletteColor = null;
	}

	@Override
	public void selectColor(@ColorInt int colorInt) {
		selectColor(colorsCollection.findPaletteColor(colorInt));
	}

	@Override
	public void selectColor(@Nullable PaletteColor paletteColor) {
		selectedPaletteColor = paletteColor;
		onColorSelected(paletteColor);
	}

	protected void onColorSelected(@Nullable PaletteColor paletteColor) {
		if (externalListener != null && paletteColor != null) {
			externalListener.onColorSelectedFromPalette(paletteColor);
		}
	}

	@Override
	public void refreshLastUsedTime() {
		long now = System.currentTimeMillis();
		if (selectedPaletteColor != null) {
			selectedPaletteColor.setLastUsedTime(now);
			colorsCollection.syncSettings();
		}
	}

	@Override
	public void onColorLongClick(@NonNull FragmentActivity activity, @NonNull View view,
	                             @NonNull PaletteColor color, boolean nightMode) {
		showItemPopUpMenu(activity, view, color, nightMode);
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
	                               @NonNull PaletteColor paletteColor, boolean nightMode) {
		List<PopUpMenuItem> menuItems = new ArrayList<>();

		if (paletteColor.isCustom()) {
			menuItems.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(R.string.shared_string_edit)
					.setIcon(getContentIcon(R.drawable.ic_action_appearance_outlined))
					.setOnClickListener(v -> showColorPickerDialog(activity, paletteColor))
					.create()
			);
		}
		menuItems.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_duplicate)
				.setIcon(getContentIcon(R.drawable.ic_action_copy))
				.setOnClickListener(v -> duplicateColor(paletteColor))
				.create()
		);
		if (paletteColor.isCustom()) {
			menuItems.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(R.string.shared_string_remove)
					.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
					.showTopDivider(true)
					.setOnClickListener(v -> removeCustomColor(paletteColor))
					.create()
			);
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		PopUpMenu.show(displayData);
	}

	private void showColorPickerDialog(@NonNull FragmentActivity activity, @Nullable PaletteColor paletteColor) {
		editedPaletteColor = paletteColor;
		Integer color = editedPaletteColor != null ? editedPaletteColor.getColor() : null;
		ColorPickerDialogController.showDialog(activity, this, color);
	}

	private void duplicateColor(@NonNull PaletteColor paletteColor) {
		PaletteColor duplicate = colorsCollection.duplicateColor(paletteColor);
		notifyUpdatePaletteColors(duplicate);
	}

	private void removeCustomColor(@NonNull PaletteColor paletteColor) {
		if (colorsCollection.askRemoveColor(paletteColor)) {
			notifyUpdatePaletteColors(null);
		}
	}

	@Override
	@Nullable
	public PaletteColor getSelectedColor() {
		return selectedPaletteColor;
	}

	@Override
	public boolean isSelectedColor(@NonNull PaletteColor paletteColor) {
		return Objects.equals(paletteColor, selectedPaletteColor);
	}

	@NonNull
	@Override
	public List<PaletteColor> getColors(@NonNull PaletteSortingMode sortingMode) {
		return colorsCollection.getColors(sortingMode);
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

	protected Drawable getContentIcon(@DrawableRes int id) {
		return app.getUIUtilities().getThemedIcon(id);
	}

	@NonNull
	public String getColorName(@ColorInt int colorInt) {
		PaletteColor paletteColor = colorsCollection.findPaletteColor(colorInt);
		return paletteColor != null
				? paletteColor.toHumanString(app)
				: app.getString(R.string.shared_string_custom);
	}
}
