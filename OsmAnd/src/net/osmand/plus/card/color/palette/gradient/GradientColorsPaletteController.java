package net.osmand.plus.card.color.palette.gradient;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.IColorsPalette;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class GradientColorsPaletteController implements IColorsPaletteController {

	protected final OsmandApplication app;
	protected List<WeakReference<IColorsPalette>> palettes = new ArrayList<>();

	protected OnColorsPaletteListener externalListener;

	protected GradientCollection gradientCollection;
	protected PaletteColor selectedPaletteColor;

	protected GPXTrackAnalysis analysis;


	public GradientColorsPaletteController(@NonNull OsmandApplication app, @NonNull GradientCollection gradientCollection, @NonNull String selectedGradientName, @Nullable GPXTrackAnalysis analysis) {
		this.app = app;
		this.analysis = analysis;
		updateContent(gradientCollection, selectedGradientName);
	}

	public void updateContent(@NonNull GradientCollection gradientCollection, @NonNull String selectedGradientName) {
		this.gradientCollection = gradientCollection;
		for (PaletteGradientColor gradientColor : gradientCollection.getPaletteColors()) {
			if (gradientColor.getPaletteName().equals(selectedGradientName)) {
				selectedPaletteColor = gradientColor;
			}
		}
		String colorizationName = gradientCollection.getColorizationType().name().toLowerCase();
		if (selectedPaletteColor == null
				|| (selectedPaletteColor instanceof PaletteGradientColor && !Algorithms.stringsEqual(((PaletteGradientColor) selectedPaletteColor).getColorizationTypeName(), colorizationName))) {
			selectedPaletteColor = gradientCollection.getDefaultGradientPalette();
		}
	}

	@Nullable
	public GPXTrackAnalysis getAnalysis() {
		return analysis;
	}

	@Override
	public void bindPalette(@NonNull IColorsPalette palette) {
		if (!(palette instanceof AllGradientsPaletteFragment)) {
			palettes.clear();
		}
		palettes.add(new WeakReference<>(palette));
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
	public void onSelectColorFromPalette(@NonNull PaletteColor color, boolean renewLastUsedTime) {
		if (!Objects.equals(selectedPaletteColor, color)) {
			PaletteColor oldSelectedColor = selectedPaletteColor;
			selectColor(color);
			if (renewLastUsedTime) {
				color.renewLastUsedTime();
				notifyUpdatePaletteColors(color);
			} else {
				notifyUpdatePaletteSelection(oldSelectedColor, selectedPaletteColor);
			}
		}
	}

	@Override
	public void onApplyColorPickerSelection(@Nullable Integer oldColor, @ColorInt int newColor) {
	}

	@Override
	public void selectColor(@ColorInt int colorInt) {
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
		gradientCollection.askRenewLastUsedTime(selectedPaletteColor);
	}

	@Override
	public void onColorLongClick(@NonNull FragmentActivity activity, @NonNull View view,
								 @NonNull PaletteColor color, boolean nightMode) {
	}

	@Override
	public void onAddColorButtonClicked(@NonNull FragmentActivity activity) {
	}

	@Override
	public void onAllColorsButtonClicked(@NonNull FragmentActivity activity) {
		AllGradientsPaletteFragment.showInstance(activity, this);
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
		return gradientCollection.getColors(sortingMode);
	}
}
