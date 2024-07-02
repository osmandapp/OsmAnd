package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class ColorsCollection {

	protected static final Log LOG = PlatformUtil.getLog(ColorsCollection.class);

	protected final List<PaletteColor> originalOrder = new ArrayList<>();
	protected final List<PaletteColor> lastUsedOrder = new LinkedList<>();

	@Nullable
	public PaletteColor findPaletteColor(@ColorInt int colorInt) {
		for (PaletteColor paletteColor : originalOrder) {
			if (paletteColor.getColor() == colorInt) {
				return paletteColor;
			}
		}
		return null;
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
		return oldColor == null ? addNewColor(newColor) : updateColor(oldColor, newColor);
	}

	@NonNull
	private PaletteColor addNewColor(@ColorInt int newColor) {
		long now = System.currentTimeMillis();
		PaletteColor paletteColor = new PaletteColor(newColor, now);
		originalOrder.add(paletteColor);
		lastUsedOrder.add(0, paletteColor);
		saveColors();
		return paletteColor;
	}

	@NonNull
	private PaletteColor updateColor(@NonNull PaletteColor paletteColor, @ColorInt int newColor) {
		paletteColor.setColor(newColor);
		saveColors();
		return paletteColor;
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
