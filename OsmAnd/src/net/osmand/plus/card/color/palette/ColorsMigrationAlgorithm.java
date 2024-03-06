package net.osmand.plus.card.color.palette;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.ListStringPreference;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorsMigrationAlgorithm {

	private final OsmandSettings settings;

	private final ListStringPreference CUSTOM_TRACK_COLORS;
	private final ListStringPreference CUSTOM_ROUTE_LINE_COLORS;
	private final ListStringPreference CUSTOM_ICON_COLORS;

	private long timestamp;

	private ColorsMigrationAlgorithm(@NonNull OsmandApplication app) {
		this.settings = app.getSettings();

		CUSTOM_TRACK_COLORS = (ListStringPreference) new ListStringPreference(
				settings, "custom_track_colors", null, ",").makeGlobal();

		CUSTOM_ROUTE_LINE_COLORS = (ListStringPreference) new ListStringPreference(
				settings, "custom_route_line_colors", null, ",").makeGlobal();

		CUSTOM_ICON_COLORS = (ListStringPreference) new ListStringPreference(
				settings, "custom_icon_colors", null, ",").makeProfile();
	}

	private void execute() {
		timestamp = System.currentTimeMillis();

		List<MigrationBundle> migrationBundles = Arrays.asList(
				new MigrationBundle(
						CUSTOM_TRACK_COLORS, settings.TRACK_COLORS_PALETTE,
						settings.CUSTOM_TRACK_PALETTE_COLORS
				),
				new MigrationBundle(
						CUSTOM_TRACK_COLORS, settings.POINT_COLORS_PALETTE,
						settings.CUSTOM_TRACK_PALETTE_COLORS
				),
				new MigrationBundle(
						CUSTOM_ROUTE_LINE_COLORS, settings.ROUTE_LINE_COLORS_PALETTE, null
				),
				new MigrationBundle(
						CUSTOM_ICON_COLORS, settings.PROFILE_COLORS_PALETTE, null
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

		List<PaletteColor> customColors = new ArrayList<>();
		if (!Algorithms.isEmpty(customColorInts)) {
			for (int colorInt : customColorInts) {
				String id = PaletteColor.generateId(timestamp);
				customColors.add(new PaletteColor(id, colorInt, timestamp));
				timestamp += 10;
			}
		}

		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.appMode = appMode;
		bundle.paletteColors = customColors;
		bundle.palettePreference = migrationBundle.newPreference;
		bundle.customColorsPreference = migrationBundle.customColorsPreference;
		ColorsCollection colorsCollection = new ColorsCollection(bundle);
		colorsCollection.syncSettings();
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
		ColorsMigrationAlgorithm migrationAlgorithm = new ColorsMigrationAlgorithm(app);
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
