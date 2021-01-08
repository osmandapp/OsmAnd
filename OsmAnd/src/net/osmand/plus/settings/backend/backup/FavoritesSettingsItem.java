package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.plus.importfiles.ImportHelper.asFavourites;

public class FavoritesSettingsItem extends CollectionSettingsItem<FavoriteGroup> {

	private FavouritesDbHelper favoritesHelper;

	public FavoritesSettingsItem(@NonNull OsmandApplication app, @NonNull List<FavoriteGroup> items) {
		super(app, null, items);
	}

	public FavoritesSettingsItem(@NonNull OsmandApplication app, @Nullable FavoritesSettingsItem baseItem, @NonNull List<FavoriteGroup> items) {
		super(app, baseItem, items);
	}

	FavoritesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		favoritesHelper = app.getFavorites();
		existingItems = new ArrayList<>(favoritesHelper.getFavoriteGroups());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.FAVOURITES;
	}

	@NonNull
	@Override
	public String getName() {
		return "favourites";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.shared_string_favorites);
	}

	@NonNull
	public String getDefaultFileExtension() {
		return GPX_FILE_EXT;
	}

	@Override
	public void apply() {
		List<FavoriteGroup> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (FavoriteGroup duplicate : duplicateItems) {
				if (shouldReplace) {
					FavoriteGroup existingGroup = favoritesHelper.getGroup(duplicate.getName());
					if (existingGroup != null) {
						List<FavouritePoint> favouritePoints = new ArrayList<>(existingGroup.getPoints());
						for (FavouritePoint favouritePoint : favouritePoints) {
							favoritesHelper.deleteFavourite(favouritePoint, false);
						}
					}
				}
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
			List<FavouritePoint> favourites = getPointsFromGroups(appliedItems);
			for (FavouritePoint favourite : favourites) {
				favoritesHelper.addFavourite(favourite, false);
			}
			favoritesHelper.sortAll();
			favoritesHelper.saveCurrentPointsIntoFile();
		}
	}

	@Override
	public boolean isDuplicate(@NonNull FavoriteGroup favoriteGroup) {
		String name = favoriteGroup.getName();
		for (FavoriteGroup group : existingItems) {
			if (group.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public FavoriteGroup renameItem(@NonNull FavoriteGroup item) {
		int number = 0;
		while (true) {
			number++;
			String name = item.getName() + " (" + number + ")";
			FavoriteGroup renamedItem = new FavoriteGroup(name, item.getPoints(), item.getColor(), item.isVisible());
			if (!isDuplicate(renamedItem)) {
				for (FavouritePoint point : renamedItem.getPoints()) {
					point.setCategory(renamedItem.getName());
				}
				return renamedItem;
			}
		}
	}

	@Nullable
	@Override
	SettingsItemReader<FavoritesSettingsItem> getReader() {
		return new SettingsItemReader<FavoritesSettingsItem>(this) {

			@Override
			public void readFromStream(@NonNull InputStream inputStream, String entryName) throws IllegalArgumentException {
				GPXFile gpxFile = GPXUtilities.loadGPXFile(inputStream);
				if (gpxFile.error != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", gpxFile.error);
				} else {
					Map<String, FavoriteGroup> flatGroups = new LinkedHashMap<>();
					List<FavouritePoint> favourites = asFavourites(app, gpxFile.getPoints(), fileName, false);
					for (FavouritePoint point : favourites) {
						FavoriteGroup group = flatGroups.get(point.getCategory());
						if (group == null) {
							group = new FavoriteGroup(point.getCategory(), point.isVisible(), point.getColor());
							flatGroups.put(group.getName(), group);
							items.add(group);
						}
						group.getPoints().add(point);
					}
				}
			}
		};
	}

	private List<FavouritePoint> getPointsFromGroups(List<FavoriteGroup> groups) {
		List<FavouritePoint> favouritePoints = new ArrayList<>();
		for (FavoriteGroup group : groups) {
			favouritePoints.addAll(group.getPoints());
		}
		return favouritePoints;
	}

	@Nullable
	@Override
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		List<FavouritePoint> favourites = getPointsFromGroups(items);
		GPXFile gpxFile = favoritesHelper.asGpxFile(favourites);
		return getGpxWriter(gpxFile);
	}
}