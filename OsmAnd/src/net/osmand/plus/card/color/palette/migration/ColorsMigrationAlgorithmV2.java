package net.osmand.plus.card.color.palette.migration;

import static net.osmand.plus.utils.ColorUtilities.getColor;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.DefaultPaletteColors;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;
import net.osmand.plus.card.color.palette.migration.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.card.color.palette.migration.data.ColorsCollectionV1;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.card.color.palette.migration.data.PaletteColorV1;
import net.osmand.plus.card.color.palette.migration.data.PredefinedPaletteColor;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.StringPreference;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements a migration algorithm from using preferences
 * to using a separate file to store all user colors palette.
 */
public class ColorsMigrationAlgorithmV2 {

	public final CommonPreference<String> TRACK_COLORS_PALETTE;
	public final CommonPreference<String> POINT_COLORS_PALETTE;
	public final CommonPreference<String> CUSTOM_TRACK_PALETTE_COLORS;
	public final CommonPreference<String> PROFILE_COLORS_PALETTE;
	public final CommonPreference<String> ROUTE_LINE_COLORS_PALETTE;

	private final OsmandApplication app;

	public static void doMigration(@NonNull OsmandApplication app) {
		ColorsMigrationAlgorithmV2 migrationAlgorithm = new ColorsMigrationAlgorithmV2(app);
		migrationAlgorithm.execute();
	}

	public ColorsMigrationAlgorithmV2(@NonNull OsmandApplication app) {
		this.app = app;
		OsmandSettings settings = app.getSettings();

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
		// Collect available colors from the user palette file
		ColorsCollection newCollection = new FileColorsCollection(app);
		newCollection.addAllUniqueColors(DefaultPaletteColors.valuesList());
		List<PaletteColor> originalOrder = newCollection.getColors(PaletteSortingMode.ORIGINAL);

		// Collect available colors from the old preferences
		List<ColorsCollectionV1> oldCollections = new ArrayList<>();
		oldCollections.add(getTrackColorCollection(app));
		oldCollections.add(getFavoritesColorCollection());
		oldCollections.add(getRouteLineColorCollection());
		for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
			oldCollections.add(getProfileColorCollection(app, appMode));
		}
		// Prepare all old colors as one list
		List<PaletteColorV1> oldColors = new ArrayList<>();
		for (ColorsCollectionV1 oldCollection : oldCollections) {
			oldColors.addAll(oldCollection.getColors());
		}

		// Prepare last used time cache, we will need this for the sorting
		Map<Integer, Long> lastUsedTimeCache = new HashMap<>();
		for (PaletteColor paletteColor : originalOrder) {
			int colorInt = paletteColor.getColor();
			lastUsedTimeCache.put(colorInt, 0L);
		}

		// Prepare result list with only unique colors
		for (PaletteColorV1 oldPaletteColor : oldColors) {
			if (oldPaletteColor.isDefault()) {
				// Ignore default colors
				continue;
			}
			int colorInt = oldPaletteColor.getColor();
			long lastUsedTime = oldPaletteColor.getLastUsedTime();
			Long cachedLastUsedTime = lastUsedTimeCache.get(colorInt);
			if (cachedLastUsedTime != null) {
				// We already have this color in the result list
				if (cachedLastUsedTime < lastUsedTime) {
					// Update last used time to bigger value
					lastUsedTimeCache.put(colorInt, lastUsedTime);
				}
			} else {
				// Add a new color as it is not yet in the result list
				originalOrder.add(new PaletteColor(colorInt));
				lastUsedTimeCache.put(colorInt, lastUsedTime);
			}
		}

		// Save all unique colors to the user's palette file
		List<PaletteColor> lastUsedOrder = new ArrayList<>(originalOrder);
		lastUsedOrder.sort(new Comparator<PaletteColor>() {
			@Override
			public int compare(PaletteColor o1, PaletteColor o2) {
				return Long.compare(getLastUsedTime(o2), getLastUsedTime(o1));
			}

			private long getLastUsedTime(@NonNull PaletteColor paletteColor) {
				int colorInt = paletteColor.getColor();
				Long lastUsedTime = lastUsedTimeCache.get(colorInt);
				return lastUsedTime != null ? lastUsedTime : 0;
			}
		});
		newCollection.setColors(originalOrder, lastUsedOrder);
	}

	public ColorsCollectionV1 getFavoritesColorCollection() {
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.predefinedColors = Arrays.asList(DefaultColors.values());
		bundle.palettePreference = POINT_COLORS_PALETTE;
		bundle.customColorsPreference = CUSTOM_TRACK_PALETTE_COLORS;
		return new ColorsCollectionV1(bundle);
	}

	public ColorsCollectionV1 getProfileColorCollection(@NonNull OsmandApplication app,
	                                                    @NonNull ApplicationMode appMode) {
		List<PaletteColorV1> predefinedColors = new ArrayList<>();
		for (ProfileIconColors color : ProfileIconColors.values()) {
			String id = color.name().toLowerCase();
			int colorInt = getColor(app, color.getColor(false));
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, color.getName()));
		}
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.appMode = appMode;
		bundle.predefinedColors = predefinedColors;
		bundle.palettePreference = PROFILE_COLORS_PALETTE;
		return new ColorsCollectionV1(bundle);
	}

	public ColorsCollectionV1 getRouteLineColorCollection() {
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.predefinedColors = Arrays.asList(DefaultColors.values());
		bundle.palettePreference = ROUTE_LINE_COLORS_PALETTE;
		return new ColorsCollectionV1(bundle);
	}

	public ColorsCollectionV1 getTrackColorCollection(@NonNull OsmandApplication app) {
		List<PaletteColorV1> predefinedColors = new ArrayList<>();
		for (AppearanceListItem item : GpxAppearanceAdapter.getUniqueTrackColorItems(app)) {
			String id = item.getValue();
			int colorInt = item.getColor();
			String name = item.getLocalizedValue();
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, name));
		}
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.predefinedColors = predefinedColors;
		bundle.palettePreference = TRACK_COLORS_PALETTE;
		bundle.customColorsPreference = CUSTOM_TRACK_PALETTE_COLORS;
		return new ColorsCollectionV1(bundle);
	}
}
