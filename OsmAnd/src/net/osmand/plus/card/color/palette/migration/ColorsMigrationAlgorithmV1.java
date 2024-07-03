package net.osmand.plus.card.color.palette.migration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.migration.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.migration.data.ColorsCollectionV1;
import net.osmand.plus.card.color.palette.migration.data.PaletteColorV1;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.plus.settings.backend.preferences.StringPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a migration from the old color preferences that stored simple color ints
 * to the new preferences that store objects of the PaletteColor wrapper class.
 */
public class ColorsMigrationAlgorithmV1 {

	// Color ints list preferences
	private final ListStringPreference CUSTOM_TRACK_COLORS;
	private final ListStringPreference CUSTOM_ROUTE_LINE_COLORS;
	private final ListStringPreference CUSTOM_ICON_COLORS;

	// Palette preferences
	public final CommonPreference<String> TRACK_COLORS_PALETTE;
	public final CommonPreference<String> POINT_COLORS_PALETTE;
	public final CommonPreference<String> CUSTOM_TRACK_PALETTE_COLORS;
	public final CommonPreference<String> PROFILE_COLORS_PALETTE;
	public final CommonPreference<String> ROUTE_LINE_COLORS_PALETTE;

	private long timestamp;

	private ColorsMigrationAlgorithmV1(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();

		CUSTOM_TRACK_COLORS = (ListStringPreference) new ListStringPreference(
				settings, "custom_track_colors", null, ",").makeGlobal();

		CUSTOM_ROUTE_LINE_COLORS = (ListStringPreference) new ListStringPreference(
				settings, "custom_route_line_colors", null, ",").makeGlobal();

		CUSTOM_ICON_COLORS = (ListStringPreference) new ListStringPreference(
				settings, "custom_icon_colors", null, ",").makeProfile();

		TRACK_COLORS_PALETTE = new StringPreference(
				settings, "track_colors_palette", null).makeGlobal();

		POINT_COLORS_PALETTE = new StringPreference(
				settings, "point_colors_palette", null).makeGlobal();

		CUSTOM_TRACK_PALETTE_COLORS = new StringPreference(
				settings, "custom_track_paletee_colors", null).makeGlobal();

		PROFILE_COLORS_PALETTE = new StringPreference(
				settings, "profile_colors_palette", null).makeProfile();

		ROUTE_LINE_COLORS_PALETTE = new StringPreference(
				settings, "route_line_colors_palette", null).makeGlobal();
	}

	private void execute() {
		timestamp = System.currentTimeMillis();

		List<MigrationBundle> migrationBundles = Arrays.asList(
				new MigrationBundle(
						CUSTOM_TRACK_COLORS, TRACK_COLORS_PALETTE,
						CUSTOM_TRACK_PALETTE_COLORS
				),
				new MigrationBundle(
						CUSTOM_TRACK_COLORS, POINT_COLORS_PALETTE,
						CUSTOM_TRACK_PALETTE_COLORS
				),
				new MigrationBundle(
						CUSTOM_ROUTE_LINE_COLORS, ROUTE_LINE_COLORS_PALETTE, null
				),
				new MigrationBundle(
						CUSTOM_ICON_COLORS, PROFILE_COLORS_PALETTE, null
				)
		);

		for (MigrationBundle migrationBundle : migrationBundles) {
			doMigration(migrationBundle);
		}
	}

	private void doMigration(@NonNull MigrationBundle migrationBundle) {
		if (migrationBundle.isGlobalPalette()) {
			doMigration(migrationBundle, null);
		} else {
			for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
				doMigration(migrationBundle, appMode);
			}
		}
	}

	private void doMigration(@NonNull MigrationBundle migrationBundle,
	                         @Nullable ApplicationMode appMode) {
		ListStringPreference oldPreference = migrationBundle.oldPreference;
		List<Integer> customColorInts = collectCustomColorInts(oldPreference, appMode);

		List<PaletteColorV1> customColors = new ArrayList<>();
		if (!Algorithms.isEmpty(customColorInts)) {
			for (int colorInt : customColorInts) {
				String id = PaletteColorV1.generateId(timestamp);
				customColors.add(new PaletteColorV1(id, colorInt, timestamp));
				timestamp += 10;
			}
		}

		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.appMode = appMode;
		bundle.paletteColors = customColors;
		bundle.palettePreference = migrationBundle.newPreference;
		bundle.customColorsPreference = migrationBundle.customColorsPreference;
		ColorsCollectionV1 colorsCollection = new ColorsCollectionV1(bundle);
		colorsCollection.saveToPreferences();
	}

	public static List<Integer> collectCustomColorInts(@NonNull ListStringPreference colorsPreference,
	                                                   @Nullable ApplicationMode appMode) {
		List<Integer> colors = new ArrayList<>();
		List<String> colorNames;
		if (appMode == null) {
			colorNames = colorsPreference.getStringsList();
		} else {
			colorNames = colorsPreference.getStringsListForProfile(appMode);
		}
		if (colorNames != null) {
			for (String colorHex : colorNames) {
				try {
					if (!Algorithms.isEmpty(colorHex)) {
						int color = Algorithms.parseColor(colorHex);
						colors.add(color);
					}
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
		return colors;
	}

	public static void doMigration(@NonNull OsmandApplication app) {
		ColorsMigrationAlgorithmV1 migrationAlgorithm = new ColorsMigrationAlgorithmV1(app);
		migrationAlgorithm.execute();
	}

	static class MigrationBundle {

		ListStringPreference oldPreference;
		CommonPreference<String> newPreference;
		CommonPreference<String> customColorsPreference;

		public MigrationBundle(@NonNull ListStringPreference oldPreference,
		                       @NonNull CommonPreference<String> newPreference,
							   @Nullable CommonPreference<String> customColorsPreference) {
			this.oldPreference = oldPreference;
			this.newPreference = newPreference;
			this.customColorsPreference = customColorsPreference;
		}

		public boolean isGlobalPalette() {
			return oldPreference.isGlobal();
		}
	}
}
