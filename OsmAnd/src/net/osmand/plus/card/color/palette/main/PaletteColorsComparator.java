package net.osmand.plus.card.color.palette.main;

import androidx.annotation.NonNull;

import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;

import java.util.Comparator;

public class PaletteColorsComparator implements Comparator<PaletteColor> {

	private final PaletteSortingMode sortingMode;

	public PaletteColorsComparator(@NonNull PaletteSortingMode sortingMode) {
		this.sortingMode = sortingMode;
	}

	@Override
	public int compare(PaletteColor o1, PaletteColor o2) {
		if (sortingMode == PaletteSortingMode.LAST_USED_TIME) {
			return Long.compare(o2.getLastUsedTime(), o1.getLastUsedTime());
		}
		// Otherwise, use original order
		return 0;
	}

}
