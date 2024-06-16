package net.osmand.plus.card.color.palette.migration.v2;

import static net.osmand.plus.utils.ColorUtilities.getColor;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionV1;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.card.color.palette.main.data.PaletteColorV1;
import net.osmand.plus.card.color.palette.main.data.PredefinedPaletteColor;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.fragments.controller.TrackColorController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a migration algorithm from using preferences
 * to using a separate file to store all user colors palette.
 */
public class ColorsMigrationAlgorithmV2 {

	public static ColorsCollectionV1 getFavoritesColorCollection(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.predefinedColors = Arrays.asList(DefaultColors.values());
		bundle.palettePreference = settings.POINT_COLORS_PALETTE;
		bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
		return new ColorsCollectionV1(bundle);
	}

	public static ColorsCollectionV1 getProfileColorCollection(@NonNull OsmandApplication app,
	                                                           @NonNull ApplicationMode appMode,
	                                                           boolean nightMode) {
		List<PaletteColorV1> predefinedColors = new ArrayList<>();
		for (ProfileIconColors color : ProfileIconColors.values()) {
			String id = color.name().toLowerCase();
			int colorInt = getColor(app, color.getColor(nightMode));
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, color.getName()));
		}

		OsmandSettings settings = app.getSettings();
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.appMode = appMode;
		bundle.predefinedColors = predefinedColors;
		bundle.palettePreference = settings.PROFILE_COLORS_PALETTE;
		return new ColorsCollectionV1(bundle);
	}

	public static ColorsCollectionV1 getRouteLineColorCollection(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.predefinedColors = Arrays.asList(DefaultColors.values());
		bundle.palettePreference = settings.ROUTE_LINE_COLORS_PALETTE;
		return new ColorsCollectionV1(bundle);
	}

	public static ColorsCollectionV1 getTrackColorCollection(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		ColorsCollectionBundle bundle = new ColorsCollectionBundle();
		bundle.predefinedColors = getTrackPredefinedColors(app);
		bundle.palettePreference = settings.TRACK_COLORS_PALETTE;
		bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
		return new ColorsCollectionV1(bundle);
	}

	@NonNull
	public static List<PaletteColorV1> getTrackPredefinedColors(@NonNull OsmandApplication app) {
		List<PaletteColorV1> predefinedColors = new ArrayList<>();
		for (AppearanceListItem item : GpxAppearanceAdapter.getUniqueTrackColorItems(app)) {
			String id = item.getValue();
			int colorInt = item.getColor();
			String name = item.getLocalizedValue();
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, name));
		}
		return predefinedColors;
	}

}
