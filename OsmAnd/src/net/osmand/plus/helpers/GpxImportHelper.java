package net.osmand.plus.helpers;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Koen Rabaey
 */
public class GpxImportHelper {

	public static final String KML_SUFFIX = ".kml";
	public static final String GPX_SUFFIX = ".gpx";
	private final MapActivity mapActivity;
	private final OsmandApplication application;
	private final OsmandMapTileView mapView;

	public GpxImportHelper(final MapActivity mapActivity, final OsmandApplication application, final OsmandMapTileView mapView) {
		this.mapActivity = mapActivity;
		this.application = application;
		this.mapView = mapView;
	}

	public void handleContenImport(final Uri contentUri) {
		final String name = getNameFromContentUri(contentUri);

		handleFileImport(contentUri, name);
	}

	public void handleFileImport(final Uri intentUri, final String fileName) {
		final boolean isFileIntent = "file".equals(intentUri.getScheme());
		final boolean isOsmandSubdir = isSubDirectory(application.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(intentUri.getPath()));

		final boolean saveFile = !isFileIntent || !isOsmandSubdir;

		if (fileName != null && fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile);
//Issue 2275
//		} else if (fileName != null && (fileName.contains("favourite")|| 
//				fileName.contains("favorite"))) {
//			handleFavouritesImport(intentUri, fileName, saveFile);
		} else {
//			handleGpxImport(intentUri, fileName, saveFile);
			handleFavouritesImport(intentUri, fileName, saveFile);
		}
	}

	private String getNameFromContentUri(Uri contentUri) {
		final String name;
		final Cursor returnCursor = application.getContentResolver().query(contentUri, null, null, null, null);
		if (returnCursor != null && returnCursor.moveToFirst()) {
			name = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
			returnCursor.close();
		} else {
			name = null;
		}
		return name;
	}

	private void handleGpxImport(final Uri gpxFile, final String fileName, final boolean save) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading_smth, ""), application.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
					final ParcelFileDescriptor pFD = application.getContentResolver().openFileDescriptor(gpxFile, "r");

					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						return GPXUtilities.loadGPXFile(application, is);
					}
				} catch (FileNotFoundException e) {
					//
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(GPXUtilities.GPXFile result) {
				progress.dismiss();
				handleResult(result, fileName, save);
			}
		}.execute();
	}

	private void handleFavouritesImport(final Uri gpxFile, final String fileName, final boolean save) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading_smth, ""), application.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
					final ParcelFileDescriptor pFD = application.getContentResolver().openFileDescriptor(gpxFile, "r");

					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						return GPXUtilities.loadGPXFile(application, is);
					}
				} catch (FileNotFoundException e) {
					//
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(final GPXUtilities.GPXFile result) {
				progress.dismiss();
				importFavourites(result, fileName, save);
			}
		}.execute();
	}

	private void importFavoritesImpl(final GPXFile gpxFile) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading_smth, ""), application.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				final List<FavouritePoint> favourites = asFavourites(gpxFile.points);
				final FavouritesDbHelper favoritesHelper = application.getFavorites();
				for (final FavouritePoint favourite : favourites) {
					favoritesHelper.deleteFavourite(favourite, false);
					favoritesHelper.addFavourite(favourite, false);
				}
				favoritesHelper.sortAll();
				favoritesHelper.saveCurrentPointsIntoFile();
				return null;
			}

			@Override
			protected void onPostExecute(GPXUtilities.GPXFile result) {
				progress.dismiss();
				AccessibleToast.makeText(mapActivity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG).show();
				final Intent newIntent = new Intent(mapActivity, application.getAppCustomization().getFavoritesActivity());
				mapActivity.startActivity(newIntent);
			}
		}.execute();
	}

	private void handleKmlImport(final Uri kmlFile, final String name, final boolean save) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading_smth, ""), application.getString(R.string.loading_data));
			}

			@Override
			protected GPXUtilities.GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
					final ParcelFileDescriptor pFD = application.getContentResolver().openFileDescriptor(kmlFile, "r");
					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						final String result = Kml2Gpx.toGpx(is);
						if (result != null) {
							try {
								return GPXUtilities.loadGPXFile(application, new ByteArrayInputStream(result.getBytes("UTF-8")));
							} catch (UnsupportedEncodingException e) {
								return null;
							}
						}
					}
				} catch (FileNotFoundException e) {
					//
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(GPXUtilities.GPXFile result) {
				progress.dismiss();
				handleResult(result, name, save);
			}
		}.execute();
	}

	private void handleResult(final GPXUtilities.GPXFile result, final String name, final boolean save) {
		if (result != null) {
			if (result.warning != null) {
				AccessibleToast.makeText(mapActivity, result.warning, Toast.LENGTH_LONG).show();
			} else {
				if (save) {
					new SaveAsyncTask(result, name).execute();
				} else {
					showGpxOnMap(result);
				}
			}
		} else {
			AccessibleToast.makeText(mapActivity, R.string.error_reading_gpx, Toast.LENGTH_LONG).show();
		}
	}

	private String saveImport(final GPXUtilities.GPXFile gpxFile, final String fileName) {
		final String warning;

		if (gpxFile.isEmpty() || fileName == null) {
			warning = application.getString(R.string.error_reading_gpx);
		} else {
			final File importDir = application.getAppPath(IndexConstants.GPX_IMPORT_DIR);
			//noinspection ResultOfMethodCallIgnored
			importDir.mkdirs();
			if (importDir.exists() && importDir.isDirectory() && importDir.canWrite()) {
				final GPXUtilities.WptPt pt = gpxFile.findPointToShow();
				final File toWrite = getFileToSave(fileName, importDir, pt);

				warning = GPXUtilities.writeGpxFile(toWrite, gpxFile, application);
				if (warning == null) {
					gpxFile.path = toWrite.getAbsolutePath();
				}
			} else {
				warning = application.getString(R.string.sd_dir_not_accessible);
			}
		}

		return warning;
	}

	private File getFileToSave(final String fileName, final File importDir, final GPXUtilities.WptPt pt) {
		final StringBuilder builder = new StringBuilder(fileName);
		if ("".equals(fileName)) {
			builder.append("import_").append(new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time))).append(GPX_SUFFIX); //$NON-NLS-1$
		}
		if (fileName.endsWith(KML_SUFFIX)) {
			builder.replace(builder.length() - KML_SUFFIX.length(), builder.length(), GPX_SUFFIX);
		} else if (!fileName.endsWith(GPX_SUFFIX)) {
			builder.append(GPX_SUFFIX);
		}
		return new File(importDir, builder.toString());
	}

	private class SaveAsyncTask extends AsyncTask<Void, Void, String> {
		private final GPXUtilities.GPXFile result;
		private final String name;

		private SaveAsyncTask(GPXUtilities.GPXFile result, final String name) {
			this.result = result;
			this.name = name;
		}

		@Override
		protected String doInBackground(Void... nothing) {
			return saveImport(result, name);
		}

		@Override
		protected void onPostExecute(final String warning) {
			final String msg = warning == null ? MessageFormat.format(application.getString(R.string.gpx_saved_sucessfully), result.path) : warning;
			AccessibleToast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();

			showGpxOnMap(result);
		}

	}

	private void showGpxOnMap(final GPXUtilities.GPXFile result) {
		application.getSelectedGpxHelper().setGpxFileToDisplay(result);
		final GPXUtilities.WptPt moveTo = result.findPointToShow();
		if (moveTo != null) {
			mapView.getAnimatedDraggingThread().startMoving(moveTo.lat, moveTo.lon, mapView.getZoom(), true);
		}
		mapView.refreshMap();
	}

	private void importFavourites(final GPXUtilities.GPXFile gpxFile, final String fileName, final boolean save) {
		final DialogInterface.OnClickListener importFavouritesListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						importFavoritesImpl(gpxFile);
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						handleResult(gpxFile, fileName, save);
						break;
				}
			}
		};

		new AlertDialog.Builder(mapActivity)
				.setTitle(R.string.shared_string_import2osmand)
				.setMessage(R.string.import_file_favourites)
				.setPositiveButton(R.string.shared_string_import, importFavouritesListener)
				.setNegativeButton(R.string.shared_string_save, importFavouritesListener)
				.show();
	}

	private List<FavouritePoint> asFavourites(final List<GPXUtilities.WptPt> wptPts) {
		final List<FavouritePoint> favourites = new ArrayList<>();

		for (GPXUtilities.WptPt p : wptPts) {
			if (p.category != null) {
				final FavouritePoint fp = new FavouritePoint(p.lat, p.lon, p.name, p.category);
				if (p.desc != null) {
					fp.setDescription(p.desc);
				}
				favourites.add(fp);
			} else if (p.name != null) {
				favourites.add(new FavouritePoint(p.lat, p.lon, p.name, ""));
			}
		}

		return favourites;
	}

	/**
	 * Checks, whether the child directory is a subdirectory of the parent
	 * directory.
	 *
	 * @param parent the parent directory.
	 * @param child  the suspected child directory.
	 * @return true if the child is a subdirectory of the parent directory.
	 */
	public boolean isSubDirectory(File parent, File child) {
		try {
			parent = parent.getCanonicalFile();
			child = child.getCanonicalFile();

			File dir = child;
			while (dir != null) {
				if (parent.equals(dir)) {
					return true;
				}
				dir = dir.getParentFile();
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}
}
