package net.osmand.plus.importfiles;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;

import java.util.List;

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
		for (FavouritePoint favourite : favourites) {
			favoritesHelper.deleteFavourite(favourite, false);
			favoritesHelper.addFavourite(favourite, false);
		}
		favoritesHelper.sortAll();
		favoritesHelper.saveCurrentPointsIntoFile();
		return null;
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