package net.osmand.plus.helpers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import net.osmand.IndexConstants;
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
	private final Activity activity;
	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private OnGpxImportCompleteListener gpxImportCompleteListener;

	public interface OnGpxImportCompleteListener {
		void onComplete(boolean success);
	}

	public GpxImportHelper(final Activity activity, final OsmandApplication app, final OsmandMapTileView mapView) {
		this.activity = activity;
		this.app = app;
		this.mapView = mapView;
	}

	public void setGpxImportCompleteListener(OnGpxImportCompleteListener gpxImportCompleteListener) {
		this.gpxImportCompleteListener = gpxImportCompleteListener;
	}

	public void handleContentImport(final Uri contentUri, final boolean useImportDir) {
		final String name = getNameFromContentUri(contentUri);
		handleFileImport(contentUri, name, useImportDir);
	}

	public boolean handleGpxImport(final Uri contentUri, final boolean useImportDir) {
		final String name = getNameFromContentUri(contentUri);
		final boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(contentUri.getPath()));
		if (!isOsmandSubdir && name != null && name.endsWith(GPX_SUFFIX)) {
			handleGpxImport(contentUri, name, true, useImportDir);
			return true;
		}
		return false;
	}

	public void handleFileImport(final Uri intentUri, final String fileName, final boolean useImportDir) {
		final boolean isFileIntent = "file".equals(intentUri.getScheme());
		final boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(intentUri.getPath()));

		final boolean saveFile = !isFileIntent || !isOsmandSubdir;

		if (fileName != null && fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile, useImportDir);
		} else {
			handleFavouritesImport(intentUri, fileName, saveFile, useImportDir);
		}
	}

	private String getNameFromContentUri(Uri contentUri) {
		final String name;
		final Cursor returnCursor = app.getContentResolver().query(contentUri, null, null, null, null);
		if (returnCursor != null && returnCursor.moveToFirst()) {
			int columnIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
			if (columnIndex != -1) {
				name = returnCursor.getString(columnIndex);
			} else {
				name = contentUri.getLastPathSegment();
			}
		} else {
			name = null;
		}
		if (returnCursor != null && !returnCursor.isClosed()) {
			returnCursor.close();
		}
		return name;
	}

	private void handleGpxImport(final Uri gpxFile, final String fileName, final boolean save, final boolean useImportDir) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
					final ParcelFileDescriptor pFD = app.getContentResolver().openFileDescriptor(gpxFile, "r");

					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						return GPXUtilities.loadGPXFile(app, is);
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
			protected void onPostExecute(GPXFile result) {
				progress.dismiss();
				handleResult(result, fileName, save, useImportDir);
			}
		}.execute();
	}

	private void handleFavouritesImport(final Uri gpxFile, final String fileName, final boolean save, final boolean useImportDir) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
					final ParcelFileDescriptor pFD = app.getContentResolver().openFileDescriptor(gpxFile, "r");

					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						return GPXUtilities.loadGPXFile(app, is);
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
			protected void onPostExecute(final GPXFile result) {
				progress.dismiss();
				importFavourites(result, fileName, save, useImportDir);
			}
		}.execute();
	}

	private void importFavoritesImpl(final GPXFile gpxFile) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... nothing) {
				final List<FavouritePoint> favourites = asFavourites(gpxFile.points);
				final FavouritesDbHelper favoritesHelper = app.getFavorites();
				for (final FavouritePoint favourite : favourites) {
					favoritesHelper.deleteFavourite(favourite, false);
					favoritesHelper.addFavourite(favourite, false);
				}
				favoritesHelper.sortAll();
				favoritesHelper.saveCurrentPointsIntoFile();
				return null;
			}

			@Override
			protected void onPostExecute(GPXFile result) {
				progress.dismiss();
				Toast.makeText(activity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG).show();
				final Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
				activity.startActivity(newIntent);
			}
		}.execute();
	}

	private void handleKmlImport(final Uri kmlFile, final String name, final boolean save, final boolean useImportDir) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				try {
					final ParcelFileDescriptor pFD = app.getContentResolver().openFileDescriptor(kmlFile, "r");
					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						final String result = Kml2Gpx.toGpx(is);
						if (result != null) {
							try {
								return GPXUtilities.loadGPXFile(app, new ByteArrayInputStream(result.getBytes("UTF-8")));
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
			protected void onPostExecute(GPXFile result) {
				progress.dismiss();
				handleResult(result, name, save, useImportDir);
			}
		}.execute();
	}

	private void handleResult(final GPXFile result, final String name, final boolean save,
							  final boolean useImportDir) {
		if (result != null) {
			if (result.warning != null) {
				Toast.makeText(activity, result.warning, Toast.LENGTH_LONG).show();
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onComplete(false);
				}
			} else {
				if (save) {
					new SaveAsyncTask(result, name, useImportDir).execute();
				} else {
					showGpxOnMap(result);
				}
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onComplete(true);
				}
			}
		} else {
			new AlertDialog.Builder(activity)
					.setTitle(R.string.shared_string_import2osmand)
					.setMessage(R.string.import_gpx_failed_descr)
					.setNeutralButton(R.string.shared_string_permissions, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							Uri uri = Uri.fromParts("package", app.getPackageName(), null);
							intent.setData(uri);
							app.startActivity(intent);
							if (gpxImportCompleteListener != null) {
								gpxImportCompleteListener.onComplete(false);
							}
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (gpxImportCompleteListener != null) {
								gpxImportCompleteListener.onComplete(false);
							}
						}
					})
					.show();
		}
	}

	private String saveImport(final GPXFile gpxFile, final String fileName, final boolean useImportDir) {
		final String warning;

		if (gpxFile.isEmpty() || fileName == null) {
			warning = app.getString(R.string.error_reading_gpx);
		} else {
			final File importDir;
			if (useImportDir) {
				importDir = app.getAppPath(IndexConstants.GPX_IMPORT_DIR);
			} else {
				importDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			}
			//noinspection ResultOfMethodCallIgnored
			importDir.mkdirs();
			if (importDir.exists() && importDir.isDirectory() && importDir.canWrite()) {
				final GPXUtilities.WptPt pt = gpxFile.findPointToShow();
				final File toWrite = getFileToSave(fileName, importDir, pt);
				warning = GPXUtilities.writeGpxFile(toWrite, gpxFile, app);
				if (warning == null) {
					gpxFile.path = toWrite.getAbsolutePath();
				}
			} else {
				warning = app.getString(R.string.sd_dir_not_accessible);
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
		private final GPXFile result;
		private final String name;
		private final boolean useImportDir;

		private SaveAsyncTask(GPXFile result, final String name, boolean useImportDir) {
			this.result = result;
			this.name = name;
			this.useImportDir = useImportDir;
		}

		@Override
		protected String doInBackground(Void... nothing) {
			return saveImport(result, name, useImportDir);
		}

		@Override
		protected void onPostExecute(final String warning) {
			final String msg = warning == null ? MessageFormat.format(app.getString(R.string.gpx_saved_sucessfully), result.path) : warning;
			Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();

			showGpxOnMap(result);
		}

	}

	private MapActivity getMapActivity() {
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	private void showGpxOnMap(final GPXFile result) {
		if (mapView != null && getMapActivity() != null) {
			app.getSelectedGpxHelper().setGpxFileToDisplay(result);
			final GPXUtilities.WptPt moveTo = result.findPointToShow();
			if (moveTo != null) {
				mapView.getAnimatedDraggingThread().startMoving(moveTo.lat, moveTo.lon, mapView.getZoom(), true);
			}
			mapView.refreshMap();
			if (getMapActivity().getDashboard().isVisible()) {
				getMapActivity().getDashboard().refreshContent(true);
			}
		}
	}

	private void importFavourites(final GPXFile gpxFile, final String fileName, final boolean save,
								  final boolean useImportDir) {
		if (gpxFile == null || gpxFile.points == null || gpxFile.points.size() == 0) {
			handleResult(gpxFile, fileName, save, useImportDir);
			return;
		}
		final DialogInterface.OnClickListener importFavouritesListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						importFavoritesImpl(gpxFile);
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						handleResult(gpxFile, fileName, save, useImportDir);
						break;
				}
			}
		};

		new AlertDialog.Builder(activity)
				.setTitle(R.string.shared_string_import2osmand)
				.setMessage(R.string.import_file_favourites)
				.setPositiveButton(R.string.shared_string_import, importFavouritesListener)
				.setNegativeButton(R.string.shared_string_save, importFavouritesListener)
				.show();
	}

	private List<FavouritePoint> asFavourites(final List<GPXUtilities.WptPt> wptPts) {
		final List<FavouritePoint> favourites = new ArrayList<>();
		for (GPXUtilities.WptPt p : wptPts) {
			if (p.name != null) {
				final String fpCat = (p.category != null) ? p.category : "";
				final FavouritePoint fp = new FavouritePoint(p.lat, p.lon, p.name, fpCat);
				if (p.desc != null) {
					fp.setDescription(p.desc);
				}
				favourites.add(fp);
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
