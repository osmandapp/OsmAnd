package net.osmand.plus.card.color.palette.migration.data_v2;

import androidx.annotation.NonNull;

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

	@NonNull
	public List<PaletteColor> getColorsInOriginalOrder() {
		return new ArrayList<>(originalOrder);
	}

	public void setColors(@NonNull List<PaletteColor> originalColors,
	                      @NonNull List<PaletteColor> lastUsedColors) {
		this.originalOrder.clear();
		this.lastUsedOrder.clear();
		this.originalOrder.addAll(originalColors);
		this.lastUsedOrder.addAll(lastUsedColors);
		saveColors();
	}

	public void addAllUniqueColors(@NonNull Collection<Integer> colorInts) {
		List<PaletteColor> originalOrder = new ArrayList<>(this.originalOrder);
		List<PaletteColor> lastUsedOrder = new ArrayList<>(this.lastUsedOrder);
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
