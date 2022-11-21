package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FavoritesGroupSettingsItem extends FileSettingsItem {

	private FavouritesHelper favoritesHelper;

	public FavoritesGroupSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	public FavoritesGroupSettingsItem(@NonNull OsmandApplication app, @NonNull File file) throws IllegalArgumentException {
		super(app, file);
	}

	@Override
	protected void init() {
		super.init();
		favoritesHelper = app.getFavoritesHelper();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.FAVOURITES;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.shared_string_favorites);
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		subtype = FileSubtype.FAVORITES_GROUPS;
		super.readFromJson(json);
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		String fileName = getFileName();
		if (!Algorithms.isEmpty(fileName)) {
			if (fileName.endsWith(File.separator)) {
				fileName = fileName.substring(0, fileName.length() - 1);
			}
			json.put("file", fileName);
		}
	}

	@Override
	public boolean applyFileName(@NonNull String fileName) {
		if (fileName.endsWith(File.separator)) {
			return false;
		}
		String itemFileName = getFileName();
		if (itemFileName != null && itemFileName.endsWith(File.separator)) {
			if (fileName.startsWith(itemFileName)) {
				this.file = new File(getPluginPath(), fileName);
				return true;
			} else {
				return false;
			}
		} else {
			return super.applyFileName(fileName);
		}
	}

	@Override
	public void applyAdditionalParams(@Nullable @org.jetbrains.annotations.Nullable SettingsItemReader<? extends SettingsItem> reader) {
		Map<String, FavouritePoint> favouritePoints = new HashMap<>();
		favoritesHelper.getFileHelper().loadPointsFromFile(getFile(),favouritePoints);
		for (FavouritePoint favourite : favouritePoints.values()) {
			favoritesHelper.addFavourite(favourite, false, false);
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile();
		favoritesHelper.loadFavorites();
	}

	@Override
	public void apply() {
//		List<FavoriteGroup> newItems = getNewItems();
//		if (personalGroup != null) {
//			duplicateItems.add(personalGroup);
//		}
//		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
//			appliedItems = new ArrayList<>(newItems);
//
//			for (FavoriteGroup duplicate : duplicateItems) {
//				boolean isPersonal = duplicate.isPersonal();
//				boolean replace = shouldReplace || isPersonal;
//				if (replace) {
//					FavoriteGroup existingGroup = favoritesHelper.getGroup(duplicate.getName());
//					if (existingGroup != null) {
//						List<FavouritePoint> favouritePoints = new ArrayList<>(existingGroup.getPoints());
//						for (FavouritePoint favouritePoint : favouritePoints) {
//							favoritesHelper.deleteFavourite(favouritePoint, false);
//						}
//					}
//				}
//				if (!isPersonal) {
//					appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
//				} else {
//					ParkingPositionPlugin plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
//					for (FavouritePoint point : duplicate.getPoints()) {
//						if (plugin != null && point.getSpecialPointType() == SpecialPointType.PARKING) {
//							plugin.clearParkingPosition();
//							plugin.updateParkingPoint(point);
//						}
//					}
//				}
//			}
//			Map<String, FavouritePoint> favouritePoints = new HashMap<>();
//			 favoritesHelper.getFileHelper().loadPointsFromFile(getFile(),favouritePoints);
//			for (FavouritePoint favourite : favouritePoints.values()) {
//				favoritesHelper.addFavourite(favourite, false, false);
//			}
//			favoritesHelper.sortAll();
//			favoritesHelper.saveCurrentPointsIntoFile();
//			favoritesHelper.loadFavorites();
//		}
	}

}
