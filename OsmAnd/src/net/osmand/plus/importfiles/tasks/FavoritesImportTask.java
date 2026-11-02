package net.osmand.plus.importfiles.tasks;

import static net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import static net.osmand.plus.myplaces.MyPlacesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.myplaces.favorites.add.AddFavoriteOptions;
import net.osmand.shared.favorites.FavoriteFolderPath;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesImportTask extends BaseImportAsyncTask<Void, Void, GpxFile> {

	private final GpxFile gpxFile;
	private final String fileName;
	private final boolean forceImport;
	@Nullable
	private final String targetFolder;

	public FavoritesImportTask(@NonNull FragmentActivity activity, @NonNull GpxFile gpxFile,
	                           @NonNull String fileName, boolean forceImport, @Nullable String targetFolder) {
		super(activity);
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.forceImport = forceImport;
		this.targetFolder = targetFolder;
	}

	@Override
	protected GpxFile doInBackground(Void... nothing) {
		mergeFavorites();
		return null;
	}

	private void mergeFavorites() {
		boolean hasTargetFolder = targetFolder != null;
		String defCategory = forceImport && !hasTargetFolder ? fileName : "";
		List<FavouritePoint> favourites = wptAsFavourites(app, gpxFile.getPointsList(), defCategory);
		Map<String, PointsGroup> pointsGroups = gpxFile.getPointsGroups();
		if (hasTargetFolder) {
			pointsGroups = moveIntoFolder(favourites, pointsGroups);
		}
		checkDuplicateNames(favourites);

		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		ParkingPositionPlugin plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);

		for (FavouritePoint favourite : favourites) {
			// Replaced by the imported point below, so not a deletion to journal
			favoritesHelper.deleteFavourite(favourite, false, false);

			String category = favourite.getCategory();
			PointsGroup pointsGroup = pointsGroups.get(category);
			// Groups that already exist in the chosen folder keep the user's appearance.
			if (hasTargetFolder && favoritesHelper.getGroup(category) != null) {
				pointsGroup = null;
			}
			favoritesHelper.addFavourite(favourite, pointsGroup, new AddFavoriteOptions());

			if (plugin != null && favourite.getSpecialPointType() == SpecialPointType.PARKING) {
				plugin.updateParkingPoint(favourite);
			}
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile(false);
	}

	@NonNull
	private Map<String, PointsGroup> moveIntoFolder(@NonNull List<FavouritePoint> favourites,
	                                                @NonNull Map<String, PointsGroup> pointsGroups) {
		Map<String, PointsGroup> movedGroups = new HashMap<>();
		for (FavouritePoint favourite : favourites) {
			String sourceCategory = favourite.getCategory();
			SpecialPointType specialType = favourite.getSpecialPointType();
			PointsGroup pointsGroup = pointsGroups.get(sourceCategory);
			String category;
			if (specialType == null) {
				category = getPathInFolder(sourceCategory);
			} else {
				category = specialType.getCategory();
				// Home/Work/Parking taken out of another group must not bring that group's appearance along.
				if (!category.equals(sourceCategory)) {
					pointsGroup = null;
				}
			}
			if (pointsGroup != null) {
				movedGroups.put(category, pointsGroup);
			}
			favourite.setCategory(category);
			// setCategory() infers the type from the name in "personal"; point_type is authoritative.
			favourite.setSpecialPointType(specialType);
		}
		return movedGroups;
	}

	@NonNull
	private String getPathInFolder(@NonNull String category) {
		return FavoriteFolderPath.join(Arrays.asList(targetFolder, category));
	}

	private void checkDuplicateNames(@NonNull List<FavouritePoint> favourites) {
		for (FavouritePoint point : favourites) {
			int number = 1;
			String index;
			String name = point.getName();
			boolean duplicatesFound = false;
			for (FavouritePoint favouritePoint : favourites) {
				if (name.equals(favouritePoint.getName())
						&& point.getCategory().equals(favouritePoint.getCategory())
						&& !point.equals(favouritePoint)) {
					if (!duplicatesFound) {
						index = " (" + number + ")";
						point.setName(name + index);
					}
					duplicatesFound = true;
					number++;
					index = " (" + number + ")";
					favouritePoint.setName(favouritePoint.getName() + index);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(GpxFile result) {
		hideProgress();
		notifyImportFinished();
		FragmentActivity activity = activityRef.get();
		if (activity != null) {
			app.showToastMessage(R.string.fav_imported_sucessfully);
			if (targetFolder == null) {
				openFavorites(activity);
			}
		}
	}

	private void openFavorites(@NonNull FragmentActivity activity) {
		Intent newIntent = new Intent(activity, app.getAppCustomization().getMyPlacesActivity());
		newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		newIntent.putExtra(TAB_ID, FAV_TAB);
		activity.startActivity(newIntent);
	}

	public static List<FavouritePoint> wptAsFavourites(@NonNull OsmandApplication app,
	                                                   @NonNull List<WptPt> points,
	                                                   @NonNull String defaultCategory) {
		List<FavouritePoint> favourites = new ArrayList<>();
		for (WptPt point : points) {
			if (Algorithms.isEmpty(point.getName())) {
				point.setName(app.getString(R.string.shared_string_waypoint));
			}
			String category = point.getCategory() != null ? point.getCategory() : defaultCategory;
			favourites.add(FavouritePoint.fromWpt(point, category));
		}
		return favourites;
	}
}