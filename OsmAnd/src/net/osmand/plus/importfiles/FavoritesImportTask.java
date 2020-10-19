package net.osmand.plus.importfiles;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.helpers.Kml2Gpx.LOG;
import static net.osmand.plus.importfiles.ImportHelper.asFavourites;
import static net.osmand.plus.myplaces.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

class FavoritesImportTask extends BaseImportAsyncTask<Void, Void, GPXFile> {

	private GPXFile gpxFile;
	private String fileName;
	private boolean forceImportFavourites;

	public FavoritesImportTask(@NonNull FragmentActivity activity, @NonNull GPXFile gpxFile,
	                           @NonNull String fileName, boolean forceImportFavourites) {
		super(activity);
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.forceImportFavourites = forceImportFavourites;
	}

	@Override
	protected GPXFile doInBackground(Void... nothing) {
		List<FavouritePoint> favourites = asFavourites(app, gpxFile.getPoints(), fileName, forceImportFavourites);
		FavouritesDbHelper favoritesHelper = app.getFavorites();
		checkDuplicateNames(favourites);
		for (FavouritePoint favourite : favourites) {
			favoritesHelper.deleteFavourite(favourite, false);
			favoritesHelper.addFavourite(favourite, false);
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile();
		return null;
	}

	public void checkDuplicateNames(List<FavouritePoint> favourites) {
		for (FavouritePoint fp : favourites) {
			int number = 0;
			String index;
			for (FavouritePoint fp2 : favourites) {
				if (fp.getName().equals(fp2.getName()) && !fp.equals(fp2)) {
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