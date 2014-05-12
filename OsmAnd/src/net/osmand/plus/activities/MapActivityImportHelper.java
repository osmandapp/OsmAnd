package net.osmand.plus.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.Kml2Gpx;
import net.osmand.plus.views.OsmandMapTileView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Koen Rabaey
 */
public class MapActivityImportHelper {

	private final MapActivity mapActivity;
	private final OsmandApplication application;
	private final OsmandMapTileView mapView;

	public MapActivityImportHelper(final MapActivity mapActivity, final OsmandApplication application, final OsmandMapTileView mapView) {
		this.mapActivity = mapActivity;
		this.application = application;
		this.mapView = mapView;
	}

	public void handleImportedGpx(final File gpx) {
		if (gpx.getPath().endsWith("kml")) {
			showImportedKml(new File(gpx.getPath()));
		} else {
			showImportedGpx(new File(gpx.getPath()));
		}
	}

	private void showImportedGpx(final File gpxFile) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading), application.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				return GPXUtilities.loadGPXFile(application, gpxFile);
			}

			@Override
			protected void onPostExecute(final GPXUtilities.GPXFile result) {
				progress.dismiss();
				onImportReady(result);
			}
		}.execute();
	}

	private void showImportedKml(final File kmlFile) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading), application.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				final String result = Kml2Gpx.toGpx(kmlFile);
				if (result == null) {
					return null;
				}
				try {
					return GPXUtilities.loadGPXFile(application, new ByteArrayInputStream(result.getBytes("UTF-8")));
				} catch (UnsupportedEncodingException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(final GPXUtilities.GPXFile result) {
				progress.dismiss();
				onImportReady(result);
			}
		}.execute();
	}

	private void onImportReady(final GPXUtilities.GPXFile result) {
		if (result != null) {
			if (result.warning != null) {
				AccessibleToast.makeText(mapActivity, result.warning, Toast.LENGTH_LONG).show();
			} else {
				final List<GPXUtilities.WptPt> wayPoints;

				if (!result.routes.isEmpty()) {
					wayPoints = result.routes.get(0).points;
				} else if (!result.tracks.isEmpty() && !result.tracks.get(0).segments.isEmpty()) {
					wayPoints = result.tracks.get(0).segments.get(0).points;
				} else if (result.points != null) {
					List<FavouritePoint> favouritePoints = asFavourites(result.points);
					if (favouritePoints.isEmpty()) {
						wayPoints = null;
					} else {
						importFavourites(favouritePoints);
						return;
					}
				} else {
					wayPoints = null;
				}

				if (wayPoints == null || wayPoints.size() == 0) {
					AccessibleToast.makeText(mapActivity, R.string.import_file_no_waypoints, Toast.LENGTH_LONG).show();
					return;
				}

				showWaypoints(result, wayPoints);
			}
		}
	}

	private void importFavourites(final List<FavouritePoint> favourites) {
		final DialogInterface.OnClickListener importFavouritesListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						final FavouritesDbHelper favorites = application.getFavorites();

						for (FavouritePoint favourite : favourites) {
							favorites.deleteFavourite(favourite);
							favorites.addFavourite(favourite);
						}

						AccessibleToast.makeText(mapActivity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG).show();
						final Intent newIntent = new Intent(mapActivity, application.getAppCustomization().getFavoritesActivity());
						mapActivity.startActivity(newIntent);
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						AccessibleToast.makeText(mapActivity, R.string.import_file_favourites_no, Toast.LENGTH_LONG).show();
						break;
				}
			}
		};

		new AlertDialog.Builder(mapActivity)
				.setMessage(R.string.import_file_favourites)
				.setPositiveButton(R.string.default_buttons_yes, importFavouritesListener)
				.setNegativeButton(R.string.default_buttons_no, importFavouritesListener)
				.show();
	}

	private void showWaypoints(final GPXUtilities.GPXFile result, final List<GPXUtilities.WptPt> wayPoints) {
		application.setGpxFileToDisplay(result, true);
		showMapAt(wayPoints.get(0));
	}

	private void showMapAt(final GPXUtilities.WptPt point) {
		mapView.getAnimatedDraggingThread().startMoving(point.lat, point.lon, mapView.getZoom(), true);
		mapView.refreshMap();
	}

	public static List<FavouritePoint> asFavourites(final List<GPXUtilities.WptPt> wptPts) {
		final List<FavouritePoint> favourites = new ArrayList<FavouritePoint>();

		for (GPXUtilities.WptPt p : wptPts) {
			if (p.category != null) {
				favourites.add(new FavouritePoint(p.lat, p.lon, p.name, p.category));
			} else if (p.name != null) {
				int c;
				if ((c = p.name.lastIndexOf('_')) != -1) {
					favourites.add(new FavouritePoint(p.lat, p.lon, p.name.substring(0, c), p.name.substring(c + 1)));
				}
			}
		}

		return favourites;
	}
}
