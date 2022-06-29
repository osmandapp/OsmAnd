package net.osmand.plus.importfiles;

import static net.osmand.plus.importfiles.ImportHelper.asFavourites;
import static net.osmand.plus.myplaces.ui.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.ui.FavoritesActivity.TAB_ID;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.FavouritePoint;
import net.osmand.data.SpecialPointType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.myplaces.FavouritesHelper;

import java.util.List;

public class FavoritesImportTask extends BaseLoadAsyncTask<Void, Void, GPXFile> {

	private final GPXFile gpxFile;
	private final String fileName;
	private final boolean forceImportFavourites;

	public FavoritesImportTask(@NonNull FragmentActivity activity, @NonNull GPXFile gpxFile,
							   @NonNull String fileName, boolean forceImportFavourites) {
		super(activity);
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.forceImportFavourites = forceImportFavourites;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		mergeFavorites(app, gpxFile, fileName, forceImportFavourites);
		return null;
	}

	public static void mergeFavorites(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
									  @NonNull String fileName, boolean forceImportFavourites) {
		List<FavouritePoint> favourites = asFavourites(app, gpxFile.getPoints(), fileName, forceImportFavourites);
		checkDuplicateNames(favourites);

		FavouritesHelper favoritesHelper = app.getFavoritesHelper();
		for (FavouritePoint favourite : favourites) {
			favoritesHelper.deleteFavourite(favourite, false);
			favoritesHelper.addFavourite(favourite, false);
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile();
	}

	public static void checkDuplicateNames(List<FavouritePoint> favourites) {
		for (FavouritePoint fp : favourites) {
			int number = 1;
			String index;
			String name = fp.getName();
			boolean duplicatesFound = false;
			for (FavouritePoint fp2 : favourites) {
				if (name.equals(fp2.getName()) && fp.getCategory().equals(fp2.getCategory()) && !fp.equals(fp2)) {
					if (!duplicatesFound) {
						index = " (" + number + ")";
						fp.setName(name + index);
					}
					duplicatesFound = true;
					number++;
					index = " (" + number + ")";
					fp2.setName(fp2.getName() + index);
				}
			}
		}
	}

	@Override
	protected void onPostExecute(GPXFile result) {
		hideProgress();
		FragmentActivity activity = activityRef.get();
		if (activity != null) {
			app.showToastMessage(R.string.fav_imported_sucessfully);
			Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(TAB_ID, FAV_TAB);
			activity.startActivity(newIntent);
		}
	}
}