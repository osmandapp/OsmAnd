package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.plus.importfiles.tasks.FavoritesImportTask.wptAsFavourites;
import static net.osmand.plus.myplaces.favorites.FavouritesFileHelper.FAV_FILE_PREFIX;
import static net.osmand.plus.myplaces.favorites.FavouritesFileHelper.FAV_GROUP_NAME_SEPARATOR;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;
import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FavoritesSettingsItem extends CollectionSettingsItem<FavoriteGroup> {

	private static final int APPROXIMATE_FAVOURITE_SIZE_BYTES = 470;

	private FavouritesHelper favoritesHelper;
	private FavoriteGroup personalGroup;

	public FavoritesSettingsItem(@NonNull OsmandApplication app, @NonNull List<FavoriteGroup> items) {
		super(app, null, items);
	}

	public FavoritesSettingsItem(@NonNull OsmandApplication app, @Nullable FavoritesSettingsItem baseItem, @NonNull List<FavoriteGroup> items) {
		super(app, baseItem, items);
	}

	public FavoritesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		favoritesHelper = app.getFavoritesHelper();
		existingItems = new ArrayList<>(favoritesHelper.getFavoriteGroups());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.FAVOURITES;
	}

	@Nullable
	public FavoriteGroup getSingleGroup() {
		return !Algorithms.isEmpty(items) && items.size() == 1 ? items.get(0) : null;
	}

	@Override
	public long getLocalModifiedTime() {
		FavoriteGroup singleGroup = getSingleGroup();
		File groupFile = singleGroup != null ? favoritesHelper.getFileHelper().getExternalFile(singleGroup) : null;
		if (groupFile != null && groupFile.exists()) {
			return groupFile.lastModified();
		}
		File favoritesFile = favoritesHelper.getFileHelper().getLegacyExternalFile();
		return favoritesFile.exists() ? favoritesFile.lastModified() : 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		FavoriteGroup singleGroup = getSingleGroup();
		File groupFile = singleGroup != null ? favoritesHelper.getFileHelper().getExternalFile(singleGroup) : null;
		if (groupFile != null && groupFile.exists()) {
			groupFile.setLastModified(lastModifiedTime);
		} else {
			File favoritesFile = favoritesHelper.getFileHelper().getLegacyExternalFile();
			if (favoritesFile.exists()) {
				favoritesFile.setLastModified(lastModifiedTime);
			}
		}
	}

	@NonNull
	@Override
	public String getName() {
		FavoriteGroup singleGroup = getSingleGroup();
		String groupName = singleGroup != null ? singleGroup.getName() : null;
		return !Algorithms.isEmpty(groupName)
				? FAV_FILE_PREFIX + FAV_GROUP_NAME_SEPARATOR + groupName
				: FAV_FILE_PREFIX;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		FavoriteGroup singleGroup = getSingleGroup();
		String groupName = singleGroup != null ? singleGroup.getName() : null;
		String fileName = getFileName();
		if (!Algorithms.isEmpty(groupName)) {
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, ctx.getString(R.string.shared_string_favorites), groupName);
		} else if (!Algorithms.isEmpty(fileName)) {
			groupName = fileName.replace(FAV_FILE_PREFIX, "").replace(GPX_FILE_EXT, "");
			if (groupName.startsWith(FAV_GROUP_NAME_SEPARATOR)) {
				groupName = groupName.substring(1);
			}
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, ctx.getString(R.string.shared_string_favorites), groupName);
		} else {
			return ctx.getString(R.string.shared_string_favorites);
		}
	}

	@NonNull
	public String getDefaultFileExtension() {
		return GPX_FILE_EXT;
	}

	@Override
	public void apply() {
		List<FavoriteGroup> newItems = getNewItems();
		if (personalGroup != null) {
			duplicateItems.add(personalGroup);
		}
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (FavoriteGroup duplicate : duplicateItems) {
				boolean isPersonal = duplicate.isPersonal();
				boolean replace = shouldReplace || isPersonal;
				if (replace) {
					FavoriteGroup existingGroup = favoritesHelper.getGroup(duplicate.getName());
					if (existingGroup != null) {
						List<FavouritePoint> favouritePoints = new ArrayList<>(existingGroup.getPoints());
						for (FavouritePoint favouritePoint : favouritePoints) {
							favoritesHelper.deleteFavourite(favouritePoint, false);
						}
					}
				}
				if (!isPersonal) {
					appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
				} else {
					ParkingPositionPlugin plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
					for (FavouritePoint point : duplicate.getPoints()) {
						if (plugin != null && point.getSpecialPointType() == SpecialPointType.PARKING) {
							plugin.clearParkingPosition();
							plugin.updateParkingPoint(point);
						}
					}
				}
			}
			List<FavouritePoint> favourites = FavouritesHelper.getPointsFromGroups(appliedItems);
			for (FavouritePoint favourite : favourites) {
				favoritesHelper.addFavourite(favourite, false, false);
			}
			favoritesHelper.sortAll();
			favoritesHelper.saveCurrentPointsIntoFile(false);
			favoritesHelper.loadFavorites();
		}
	}

	@Override
	protected void deleteItem(FavoriteGroup item) {
		favoritesHelper.deleteGroup(item, false);
		favoritesHelper.saveCurrentPointsIntoFile(false);
	}

	@Override
	public boolean isDuplicate(@NonNull FavoriteGroup favoriteGroup) {
		String name = favoriteGroup.getName();
		if (favoriteGroup.isPersonal()) {
			personalGroup = favoriteGroup;
			return false;
		}
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

	@Override
	public long getEstimatedItemSize(@NonNull FavoriteGroup item) {
		return (long) item.getPoints().size() * APPROXIMATE_FAVOURITE_SIZE_BYTES;
	}

	@Nullable
	@Override
	public SettingsItemReader<FavoritesSettingsItem> getReader() {
		return new SettingsItemReader<FavoritesSettingsItem>(this) {

			@Override
			public void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
			                           @Nullable String entryName) throws IllegalArgumentException {
				GPXFile gpxFile = GPXUtilities.loadGPXFile(inputStream);
				if (gpxFile.error != null) {
					warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed read gpx file", gpxFile.error);
				} else {
					Map<String, FavoriteGroup> flatGroups = new LinkedHashMap<>();
					List<FavouritePoint> favourites = wptAsFavourites(app, gpxFile.getPoints(), "");
					for (FavouritePoint point : favourites) {
						FavoriteGroup group = flatGroups.get(point.getCategory());
						if (group == null) {
							group = new FavoriteGroup(point);
							flatGroups.put(group.getName(), group);
							items.add(group);
						}
						group.getPoints().add(point);
					}
				}
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		GPXFile gpxFile = favoritesHelper.getFileHelper().asGpxFile(items);
		return getGpxWriter(gpxFile);
	}
}
