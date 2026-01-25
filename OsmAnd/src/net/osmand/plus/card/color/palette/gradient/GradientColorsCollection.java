package net.osmand.plus.card.color.palette.gradient;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.palette.data.gradient.GradientSettingsHelper;
import net.osmand.shared.palette.data.gradient.GradientSettingsItem;
import net.osmand.shared.routing.RouteColorize.ColorizationType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GradientColorsCollection extends ColorsCollection {

	private final Map<String, Pair<ColorPalette, Long>> gradientPalettes;
	private final GradientSettingsHelper settingsHelper;
	private final Object gradientType;
	private String type;

	public GradientColorsCollection(@NonNull OsmandApplication app,
	                                @NonNull Object gradientType) {
		this.settingsHelper = new GradientSettingsHelper();
		this.gradientType = gradientType;
		this.gradientPalettes = app.getColorPaletteHelper().getPalletsForType(gradientType);

		if (gradientType instanceof ColorizationType) {
			type = ((ColorizationType) gradientType).name().toLowerCase();
		} else if (gradientType instanceof TerrainType) {
			type = ((TerrainType) gradientType).name();
		}
		loadColors();
	}

	@NonNull
	public List<PaletteColor> getPaletteColors() {
		return getColors(PaletteSortingMode.LAST_USED_TIME);
	}

	@NonNull
	public Object getGradientType() {
		return gradientType;
	}

	@Nullable
	public PaletteGradientColor getDefaultGradientPalette() {
		for (PaletteColor paletteColor : getPaletteColors()) {
			PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
			if (Objects.equals(gradientColor.getPaletteName(), PaletteGradientColor.DEFAULT_NAME)) {
				return gradientColor;
			}
		}
		return null;
	}

	@Override
	protected void loadColorsInLastUsedOrder() throws IOException {
		Set<String> addedPaletteIds = new HashSet<>();

		// 1. Firstly collect all items those already have last used order
		List<GradientSettingsItem> savedItems = settingsHelper.getItems(type);
		for (GradientSettingsItem item : savedItems) {
			int index = item.getIndex();
			String typeName = item.getTypeName();
			String paletteName = item.getPaletteName();

			Pair<ColorPalette, Long> paletteInfo = gradientPalettes.get(paletteName);
			if (paletteInfo != null) {
				ColorPalette palette = paletteInfo.first;
				PaletteGradientColor gradientColor = new PaletteGradientColor(typeName, paletteName, palette, index);
				lastUsedOrder.add(gradientColor);
				addedPaletteIds.add(gradientColor.getStringId());
			}
		}

		// 2. Collect all new palette files (those are not in settings cache)
		for (String key : gradientPalettes.keySet()) {
			Pair<ColorPalette, Long> pair = gradientPalettes.get(key);
			if (pair != null) {
				ColorPalette palette = pair.first;
				long creationTime = pair.second;
				PaletteGradientColor gradientColor = new PaletteGradientColor(type, key, palette, (int) creationTime);
				String id = gradientColor.getStringId();
				if (!addedPaletteIds.contains(id)) {
					lastUsedOrder.add(gradientColor);
					addedPaletteIds.add(id);
				}
			}
		}
	}

	@Override
	protected void saveColors() {
		// Update indexes in Original Order
		for (PaletteColor paletteColor : originalOrder) {
			int index = originalOrder.indexOf(paletteColor);
			paletteColor.setIndex(index + 1);
		}

		// Collect and save items, we only replace items of current collection 'type'
		List<GradientSettingsItem> itemsToSave = new ArrayList<>();
		for (PaletteColor paletteColor : lastUsedOrder) {
			if (paletteColor instanceof PaletteGradientColor gradientColor) {
				itemsToSave.add(new GradientSettingsItem(
						gradientColor.getTypeName(),
						gradientColor.getPaletteName(),
						gradientColor.getIndex()
				));
			}
		}
		settingsHelper.saveItems(type, itemsToSave);
	}
}
