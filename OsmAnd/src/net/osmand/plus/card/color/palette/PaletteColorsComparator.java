package net.osmand.plus.card.color.palette;

import androidx.annotation.NonNull;

import net.osmand.plus.card.color.palette.data.PaletteColor;
import net.osmand.plus.card.color.palette.data.PaletteSortingMode;

import java.util.Comparator;

public class PaletteColorsComparator implements Comparator<PaletteColor> {

	private final PaletteSortingMode sortingMode;

	public PaletteColorsComparator(@NonNull PaletteSortingMode sortingMode) {
		this.sortingMode = sortingMode;
	}

	@Override
	public int compare(PaletteColor o1, PaletteColor o2) {
		if (sortingMode == PaletteSortingMode.LAST_USED_TIME) {
			return Long.compare(o1.getLastUsedTime(), o2.getLastUsedTime());
		}
		// Otherwise, leave the order unchanged
		return 0;
	}

}
