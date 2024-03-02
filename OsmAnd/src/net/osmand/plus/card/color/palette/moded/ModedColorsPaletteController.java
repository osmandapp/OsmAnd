package net.osmand.plus.card.color.palette.moded;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;

import java.util.List;
import java.util.Objects;

public abstract class ModedColorsPaletteController extends ColorsPaletteController {

	private final List<PaletteMode> availablePaletteModes;
	private OnPaletteModeSelectedListener onPaletteModeSelectedListener;
	private PaletteMode selectedPaletteMode;

	public ModedColorsPaletteController(@NonNull OsmandApplication app,
	                                    @NonNull ColorsCollection colorsCollection) {
		super(app, colorsCollection, 0);
		this.availablePaletteModes = collectAvailablePaletteModes();
		this.selectedPaletteMode = getInitialPaletteMode();
		this.selectedPaletteColor = provideSelectedColorForPaletteMode(selectedPaletteMode);
	}

	public void setPaletteModeSelectedListener(@Nullable OnPaletteModeSelectedListener listener) {
		this.onPaletteModeSelectedListener = listener;
	}

	public void selectPaletteMode(@NonNull PaletteMode paletteMode) {
		if (!Objects.equals(paletteMode.getTag(), selectedPaletteMode.getTag())) {
			PaletteMode oldPaletteMode = this.selectedPaletteMode;
			this.selectedPaletteMode = paletteMode;
			onPaletteModeChanged(oldPaletteMode, paletteMode);
		}
	}

	@NonNull
	public List<PaletteMode> getAvailablePaletteModes() {
		return availablePaletteModes;
	}

	@NonNull
	public PaletteMode getSelectedPaletteMode() {
		return selectedPaletteMode;
	}

	protected void onPaletteModeChanged(@NonNull PaletteMode oldPaletteMode, @NonNull PaletteMode paletteMode) {
		if (onPaletteModeSelectedListener != null) {
			onPaletteModeSelectedListener.onColorsPaletteModeChanged();
		}
		PaletteColor oldPaletteColor = provideSelectedColorForPaletteMode(oldPaletteMode);
		PaletteColor paletteColor = provideSelectedColorForPaletteMode(paletteMode);
		selectColor(paletteColor);
		if (paletteColor != null) {
			notifyUpdatePaletteSelection(oldPaletteColor, paletteColor);
		}
	}

	@Override
	public void refreshLastUsedTime() {
		PaletteMode selectedPaletteMode = getSelectedPaletteMode();
		long now = System.currentTimeMillis();
		for (PaletteMode paletteMode : getAvailablePaletteModes()) {
			if (!Objects.equals(selectedPaletteMode.getTag(), paletteMode.getTag())) {
				setLastUsedTime(paletteMode, now++);
			}
		}
		setLastUsedTime(selectedPaletteMode, now);
		colorsCollection.syncSettings();
	}

	private void setLastUsedTime(@NonNull PaletteMode paletteMode, long lastUsedTime) {
		PaletteColor paletteColor = provideSelectedColorForPaletteMode(paletteMode);
		if (paletteColor != null) {
			paletteColor.setLastUsedTime(lastUsedTime);
		}
	}

	@NonNull
	protected abstract List<PaletteMode> collectAvailablePaletteModes();

	@NonNull
	protected abstract PaletteMode getInitialPaletteMode();

	@Nullable
	protected abstract PaletteColor provideSelectedColorForPaletteMode(@NonNull PaletteMode paletteMode);

	public interface OnPaletteModeSelectedListener {
		void onColorsPaletteModeChanged();
	}
}
