package net.osmand.plus.importfiles.tasks;

import static net.osmand.shared.gpx.GpxUtilities.PointsGroup;
import static net.osmand.plus.myplaces.MyPlacesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.parking.ParkingPositionPlugin;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoritesImportTask extends BaseImportAsyncTask<Void, Void, GpxFile> {

	private final GpxFile gpxFile;
	private final String fileName;
	private final boolean forceImport;

	public FavoritesImportTask(@NonNull FragmentActivity activity, @NonNull GpxFile gpxFile,
	                           @NonNull String fileName, boolean forceImport) {
		super(activity);
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.forceImport = forceImport;
	}

	@Override
	protected GpxFile doInBackground(Void... nothing) {
		mergeFavorites();
		return null;
	}

	private void mergeFavorites() {
		String defCategory = forceImport ? fileName : "";
		List<FavouritePoint> favourites = wptAsFavourites(app, gpxFile.getPointsList(), defCategory);
		checkDuplicateNames(favourites);

		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		ParkingPositionPlugin plugin = PluginsHelper.getPlugin(ParkingPositionPlugin.class);
		Map<String, PointsGroup> pointsGroups = gpxFile.getPointsGroups();

		for (FavouritePoint favourite : favourites) {
			favoritesHelper.deleteFavourite(favourite, false);

			PointsGroup pointsGroup = pointsGroups.get(favourite.getCategory());
			favoritesHelper.addFavourite(favourite, false, false, false, pointsGroup);

			if (plugin != null && favourite.getSpecialPointType() == SpecialPointType.PARKING) {
				plugin.updateParkingPoint(favourite);
			}
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile(false);
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
			Intent newIntent = new Intent(activity, app.getAppCustomization().getMyPlacesActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(TAB_ID, FAV_TAB);
			activity.startActivity(newIntent);
		}
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