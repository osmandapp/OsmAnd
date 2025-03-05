package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class ColorsCollection {

	protected static final Log LOG = PlatformUtil.getLog(ColorsCollection.class);

	protected final List<PaletteColor> originalOrder = new ArrayList<>();
	protected final List<PaletteColor> lastUsedOrder = new LinkedList<>();

	@Nullable
	public PaletteColor findPaletteColor(@ColorInt int colorInt) {
		return findPaletteColor(colorInt, false);
	}

	@Nullable
	public PaletteColor findPaletteColor(@ColorInt int colorInt, boolean registerIfNotFound) {
		for (PaletteColor paletteColor : originalOrder) {
			if (paletteColor.getColor() == colorInt) {
				return paletteColor;
			}
		}
		return registerIfNotFound ? addNewColor(colorInt, false) : null;
	}

	@NonNull
	public List<PaletteColor> getColors(@NonNull PaletteSortingMode sortingMode) {
		return new ArrayList<>(sortingMode == PaletteSortingMode.ORIGINAL ? originalOrder : lastUsedOrder);
	}

	public void setColors(@NonNull List<PaletteColor> originalColors,
	                      @NonNull List<PaletteColor> lastUsedColors) {
		this.originalOrder.clear();
		this.lastUsedOrder.clear();
		this.originalOrder.addAll(originalColors);
		this.lastUsedOrder.addAll(lastUsedColors);
		saveColors();
	}

	@NonNull
	public PaletteColor duplicateColor(@NonNull PaletteColor paletteColor) {
		PaletteColor duplicate = paletteColor.duplicate();
		addColorDuplicate(originalOrder, paletteColor, duplicate);
		addColorDuplicate(lastUsedOrder, paletteColor, duplicate);
		saveColors();
		return duplicate;
	}

	private void addColorDuplicate(@NonNull List<PaletteColor> list,
	                               @NonNull PaletteColor original,
	                               @NonNull PaletteColor duplicate) {
		int index = list.indexOf(original);
		if (index >= 0 && index < list.size()) {
			list.add(index + 1, duplicate);
		} else {
			list.add(duplicate);
		}
	}

	public boolean askRemoveColor(@NonNull PaletteColor paletteColor) {
		if (originalOrder.remove(paletteColor)) {
			lastUsedOrder.remove(paletteColor);
			saveColors();
			return true;
		}
		return false;
	}

	@Nullable
	public PaletteColor addOrUpdateColor(@Nullable PaletteColor oldColor,
	                                     @ColorInt int newColor) {
		return oldColor == null ? addNewColor(newColor, true) : updateColor(oldColor, newColor);
	}

	@NonNull
	private PaletteColor addNewColor(@ColorInt int newColor, boolean updateLastUsedOrder) {
		PaletteColor paletteColor = new PaletteColor(newColor);
		originalOrder.add(paletteColor);
		if (updateLastUsedOrder) {
			lastUsedOrder.add(0, paletteColor);
		} else {
			lastUsedOrder.add(paletteColor);
		}
		saveColors();
		return paletteColor;
	}

	@NonNull
	private PaletteColor updateColor(@NonNull PaletteColor paletteColor, @ColorInt int newColor) {
		paletteColor.setColor(newColor);
		saveColors();
		return paletteColor;
	}

	public void addAllUniqueColors(@NonNull Collection<Integer> colorInts) {
		List<PaletteColor> originalOrder = getColors(PaletteSortingMode.ORIGINAL);
		List<PaletteColor> lastUsedOrder = getColors(PaletteSortingMode.LAST_USED_TIME);
		Set<Integer> presentColors = new HashSet<>();
		for (PaletteColor paletteColor : originalOrder) {
			presentColors.add(paletteColor.getColor());
		}
		for (int colorInt : colorInts) {
			if (!presentColors.contains(colorInt)) {
				PaletteColor paletteColor = new PaletteColor(colorInt);
				originalOrder.add(paletteColor);
				lastUsedOrder.add(paletteColor);
			}
		}
		setColors(originalOrder, lastUsedOrder);
	}

	public void askRenewLastUsedTime(@Nullable PaletteColor paletteColor) {
		if (paletteColor != null) {
			lastUsedOrder.remove(paletteColor);
			lastUsedOrder.add(0, paletteColor);
			saveColors();
		}
	}

	protected void loadColors() {
		try {
			originalOrder.clear();
			lastUsedOrder.clear();
			loadColorsInLastUsedOrder();
			originalOrder.addAll(lastUsedOrder);
			originalOrder.sort((a, b) -> Double.compare(a.getIndex(), b.getIndex()));
		} catch (Exception e) {
			LOG.error("Error when trying to read file: " + e.getMessage());
		}
	}

	protected abstract void loadColorsInLastUsedOrder() throws IOException;

	protected abstract void saveColors();
}
