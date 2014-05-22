package net.osmand.plus.helpers;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.widget.Toast;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.GPXUtilities;
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
import java.util.Date;

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
		if (fileName != null && fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName);
		} else {
			handleGpxImport(intentUri, fileName);
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

	private void handleGpxImport(final Uri gpxFile, final String fileName) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading), application.getString(R.string.loading_data));
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
				handleResult(result, fileName);
			}
		}.execute();
	}

	private void handleKmlImport(final Uri kmlFile, final String name) {
		new AsyncTask<Void, Void, GPXUtilities.GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(mapActivity, application.getString(R.string.loading), application.getString(R.string.loading_data));
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
				handleResult(result, name);
			}
		}.execute();
	}

	private void handleResult(final GPXUtilities.GPXFile result, final String name) {
		if (result != null) {
			if (result.warning != null) {
				AccessibleToast.makeText(mapActivity, result.warning, Toast.LENGTH_LONG).show();
			} else {
				new SaveAsyncTask(result, name).execute();
			}
		} else {
			AccessibleToast.makeText(mapActivity, R.string.error_reading_gpx, Toast.LENGTH_LONG).show();
		}
	}

	private String saveImport(final GPXUtilities.GPXFile gpxFile, final String fileName) {
		final String warning;

		if (gpxFile.isEmpty()) {
			warning = application.getString(R.string.error_reading_gpx);
		} else {
			final File importDir = application.getAppPath(IndexConstants.GPX_IMPORT_DIR);
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
			builder.append("import_").append(new SimpleDateFormat("HH-mm_EEE").format(new Date(pt.time))).append(GPX_SUFFIX); //$NON-NLS-1$
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

			application.setGpxFileToDisplay(result, true);
			final GPXUtilities.WptPt moveTo = result.findPointToShow();
			if (moveTo != null) {
				mapView.getAnimatedDraggingThread().startMoving(moveTo.lat, moveTo.lon, mapView.getZoom(), true);
			}
			mapView.refreshMap();
		}
	}
}
