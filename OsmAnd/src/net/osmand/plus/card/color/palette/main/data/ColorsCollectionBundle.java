package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;

import java.util.List;

/**
 * Initial bundle for colors collection.
 * Optional fields are marked as 'nullable' and uses only for particular purposes.
 */
public class ColorsCollectionBundle {

	/**
	 * Uses only for profile dependent preferences.
 	 */
	@Nullable
	public ApplicationMode appMode;

	/**
	 * Predefined colors for the current palette.
	 */
	public List<PaletteColor> predefinedColors;

	/**
	 * Uses only for migration purposes from old to a new app version,
	 * when we started to use palette colors instead of the simple int colors.
	 */
	@Nullable
	public List<PaletteColor> paletteColors;

	/**
	 * Preference that include all (predefined and custom) colors for the current palette.
	 */
	public CommonPreference<String> palettePreference;

	/**
	 * Preference with custom palette colors.
	 * Uses only when the same custom colors are used for more then one palette preference,
	 * but when those palettes have different predefined colors.
	 */
	@Nullable
	public CommonPreference<String> customColorsPreference;

}
