package net.osmand.plus.card.color.palette.moded;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.solid.data.PaletteMode;
import net.osmand.plus.card.color.palette.solid.SolidPaletteController;
import net.osmand.shared.palette.domain.PaletteItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ModedColorsPaletteController extends SolidPaletteController {

	private final List<PaletteMode> availablePaletteModes;
	private OnPaletteModeSelectedListener onPaletteModeSelectedListener;
	private PaletteMode selectedPaletteMode;

	public ModedColorsPaletteController(@NonNull OsmandApplication app) {
		super(app, 0);
		this.availablePaletteModes = collectAvailablePaletteModes();
		this.selectedPaletteMode = getInitialPaletteMode();
		this.setSelectedItem(provideSelectedPaletteItemForMode(selectedPaletteMode));
	}

	public void setPaletteModeSelectedListener(@Nullable OnPaletteModeSelectedListener listener) {
		this.onPaletteModeSelectedListener = listener;
	}

	public void selectPaletteMode(@NonNull PaletteMode paletteMode) {
		if (!Objects.equals(paletteMode.tag(), selectedPaletteMode.tag())) {
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
			onPaletteModeSelectedListener.onPaletteModeChanged();
		}
		PaletteItem oldModePaletteItem = provideSelectedPaletteItemForMode(oldPaletteMode);
		PaletteItem newModePaletteItem = provideSelectedPaletteItemForMode(paletteMode);
		selectPaletteItem(newModePaletteItem);
		if (newModePaletteItem != null) {
			notifyUpdatePaletteSelection(oldModePaletteItem, newModePaletteItem);
		}
	}

	@Override
	public void renewLastUsedTime() {
		PaletteMode selectedPaletteMode = getSelectedPaletteMode();
		List<PaletteItem> paletteItems = new ArrayList<>();
		for (PaletteMode paletteMode : getAvailablePaletteModes()) {
			if (!Objects.equals(selectedPaletteMode.tag(), paletteMode.tag())) {
				addPaletteItem(paletteMode, paletteItems);
			}
		}
		addPaletteItem(selectedPaletteMode, paletteItems);
		for (PaletteItem paletteItem : paletteItems) {
			renewLastUsedTime(paletteItem);
		}
	}

	private void addPaletteItem(@NonNull PaletteMode paletteMode,
	                            @NonNull List<PaletteItem> paletteItems) {
		PaletteItem paletteItem = provideSelectedPaletteItemForMode(paletteMode);
		if (paletteItem != null) {
			paletteItems.add(paletteItem);
		}
	}

	@NonNull
	protected abstract List<PaletteMode> collectAvailablePaletteModes();

	@NonNull
	protected abstract PaletteMode getInitialPaletteMode();

	@Nullable
	public abstract PaletteItem provideSelectedPaletteItemForMode(@NonNull PaletteMode paletteMode);

	public interface OnPaletteModeSelectedListener {
		void onPaletteModeChanged();
	}
}
