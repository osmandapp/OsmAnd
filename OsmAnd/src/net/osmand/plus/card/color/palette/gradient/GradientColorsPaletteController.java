package net.osmand.plus.card.color.palette.gradient;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.IColorsPalette;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.gpx.GpxTrackAnalysis;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class GradientColorsPaletteController implements IColorsPaletteController {

	protected final OsmandApplication app;
	protected List<WeakReference<IColorsPalette>> palettes = new ArrayList<>();

	protected OnColorsPaletteListener externalListener;

	protected GradientColorsCollection gradientCollection;
	protected PaletteColor selectedPaletteColor;

	protected GpxTrackAnalysis analysis;

	public GradientColorsPaletteController(@NonNull OsmandApplication app, @Nullable GpxTrackAnalysis analysis) {
		this.app = app;
		this.analysis = analysis;
	}

	public void reloadGradientColors() {
		Object gradientType = gradientCollection.getGradientType();
		this.gradientCollection = new GradientColorsCollection(app, gradientType);
		if (selectedPaletteColor instanceof PaletteGradientColor) {
			notifyUpdatePaletteColors(null);
		}
	}

	public void updateContent(@NonNull GradientColorsCollection gradientCollection, @NonNull String selectedGradientName) {
		this.gradientCollection = gradientCollection;
		updateContent(selectedGradientName);
	}

	public void updateContent(@NonNull String selectedGradientName) {
		PaletteGradientColor newColor = null;
		for (PaletteColor paletteColor : gradientCollection.getPaletteColors()) {
			PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
			if (gradientColor.getPaletteName().equals(selectedGradientName)) {
				newColor = gradientColor;
			}
		}
		if (newColor == null) {
			selectedPaletteColor = gradientCollection.getDefaultGradientPalette();
		} else {
			selectedPaletteColor = newColor;
		}
		notifyUpdatePaletteColors(null);
	}

	@Nullable
	public GpxTrackAnalysis getAnalysis() {
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

	private void notifyUpdatePaletteSelection(@Nullable PaletteColor oldColor, @NonNull PaletteColor newColor) {
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

	public Object getGradientType(){
		return gradientCollection.getGradientType();
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
				gradientCollection.askRenewLastUsedTime(color);
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
	public void selectColor(@Nullable Integer color) {

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
