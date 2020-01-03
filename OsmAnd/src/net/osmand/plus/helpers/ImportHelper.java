package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.SettingsHelper.SettingsImportListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;

import static net.osmand.plus.myplaces.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

/**
 * @author Koen Rabaey
 */
public class ImportHelper {
	public final static Log log = PlatformUtil.getLog(ImportHelper.class);
	public static final String KML_SUFFIX = ".kml";
	public static final String KMZ_SUFFIX = ".kmz";
	public static final String GPX_SUFFIX = ".gpx";
	private final AppCompatActivity activity;
	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private OnGpxImportCompleteListener gpxImportCompleteListener;

	public interface OnGpxImportCompleteListener {
		void onComplete(boolean success);
	}

	public ImportHelper(final AppCompatActivity activity, final OsmandApplication app, final OsmandMapTileView mapView) {
		this.activity = activity;
		this.app = app;
		this.mapView = mapView;
	}

	public void setGpxImportCompleteListener(OnGpxImportCompleteListener gpxImportCompleteListener) {
		this.gpxImportCompleteListener = gpxImportCompleteListener;
	}

	public void handleContentImport(final Uri contentUri, Bundle extras, final boolean useImportDir) {
		final String name = getNameFromContentUri(app, contentUri);
		handleFileImport(contentUri, name, extras, useImportDir);
	}

	public boolean handleGpxImport(final Uri contentUri, final boolean useImportDir) {
		String name = getNameFromContentUri(app, contentUri);
		boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(contentUri.getPath()));
		if (!isOsmandSubdir && name != null) {
			String nameLC = name.toLowerCase();
			if (nameLC.endsWith(GPX_SUFFIX)) {
				name = name.substring(0, name.length() - 4) + GPX_SUFFIX;
				handleGpxImport(contentUri, name, true, useImportDir);
				return true;
			} else if (nameLC.endsWith(KML_SUFFIX)) {
				name = name.substring(0, name.length() - 4) + KML_SUFFIX;
				handleKmlImport(contentUri, name, true, useImportDir);
				return true;
			} else if (nameLC.endsWith(KMZ_SUFFIX)) {
				name = name.substring(0, name.length() - 4) + KMZ_SUFFIX;
				handleKmzImport(contentUri, name, true, useImportDir);
				return true;
			}
		}
		return false;
	}

	public void handleFavouritesImport(@NonNull Uri uri) {
		String scheme = uri.getScheme();
		boolean isFileIntent = "file".equals(scheme);
		boolean isContentIntent = "content".equals(scheme);
		boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(uri.getPath()));
		final boolean saveFile = !isFileIntent || !isOsmandSubdir;
		String fileName = "";
		if (isFileIntent) {
			fileName = new File(uri.getPath()).getName();
		} else if (isContentIntent) {
			fileName = getNameFromContentUri(app, uri);
		}
		handleFavouritesImport(uri, fileName, saveFile, false, true);
	}

	public void handleFileImport(Uri intentUri, String fileName, Bundle extras, boolean useImportDir) {
		final boolean isFileIntent = "file".equals(intentUri.getScheme());
		final boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(intentUri.getPath()));

		final boolean saveFile = !isFileIntent || !isOsmandSubdir;

		if (fileName != null && fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName != null && fileName.endsWith(KMZ_SUFFIX)) {
			handleKmzImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName != null && fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			handleObfImport(intentUri, fileName);
		} else if (fileName != null && fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			handleSqliteTileImport(intentUri, fileName);
		} else if (fileName != null && fileName.endsWith(IndexConstants.OSMAND_SETTINGS_FILE_EXT)) {
			if (extras != null && extras.containsKey(SettingsHelper.SETTINGS_VERSION_KEY) && extras.containsKey(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY)) {
				int version = extras.getInt(SettingsHelper.SETTINGS_VERSION_KEY, -1);
				String latestChanges = extras.getString(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY);
				handleOsmAndSettingsImport(intentUri, fileName, latestChanges, version);
			} else {
				handleOsmAndSettingsImport(intentUri, fileName, null, -1);
			}
		} else {
			handleFavouritesImport(intentUri, fileName, saveFile, useImportDir, false);
		}
	}

	public static String getNameFromContentUri(OsmandApplication app, Uri contentUri) {
		final String name;
		final Cursor returnCursor = app.getContentResolver().query(contentUri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null);
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

	@SuppressLint("StaticFieldLeak")
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
						return GPXUtilities.loadGPXFile(is);
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
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				handleResult(result, fileName, save, useImportDir, false);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	private void handleFavouritesImport(final Uri gpxFile, final String fileName, final boolean save, final boolean useImportDir, final boolean forceImportFavourites) {
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
						return GPXUtilities.loadGPXFile(is);
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
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				importFavourites(result, fileName, save, useImportDir, forceImportFavourites);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	private void importFavoritesImpl(final GPXFile gpxFile, final String fileName, final boolean forceImportFavourites) {
		if(!app.isApplicationInitializing()) {
			new AsyncTask<Void, Void, GPXFile>() {
				ProgressDialog progress = null;

				@Override
				protected void onPreExecute() {
					progress = ProgressDialog
						.show(activity, app.getString(R.string.loading_smth, ""),
							app.getString(R.string.loading_data));
				}

				@Override
				protected GPXFile doInBackground(Void... nothing) {
					final List<FavouritePoint> favourites = asFavourites(gpxFile.getPoints(),
						fileName, forceImportFavourites);
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
					if (isActivityNotDestroyed(activity)) {
						progress.dismiss();
					}
					Toast.makeText(activity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG)
						.show();
					final Intent newIntent = new Intent(activity,
						app.getAppCustomization().getFavoritesActivity());
					newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					newIntent.putExtra(TAB_ID, FAV_TAB);
					activity.startActivity(newIntent);
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			app.getAppInitializer().addListener(new AppInitializeListener() {

				@Override
				public void onProgress(AppInitializer init, InitEvents event) {}

				@Override
				public void onFinish(AppInitializer init) {
					importFavoritesImpl(gpxFile, fileName, forceImportFavourites);
				}
			});
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void handleKmzImport(final Uri kmzFile, final String name, final boolean save, final boolean useImportDir) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... voids) {
				InputStream is = null;
				ZipInputStream zis = null;
				try {
					final ParcelFileDescriptor pFD = app.getContentResolver().openFileDescriptor(kmzFile, "r");
					if (pFD != null) {
						is = new FileInputStream(pFD.getFileDescriptor());
						zis = new ZipInputStream(is);
						zis.getNextEntry();
						final String result = Kml2Gpx.toGpx(zis);
						if (result != null) {
							try {
								return GPXUtilities.loadGPXFile(new ByteArrayInputStream(result.getBytes("UTF-8")));
							} catch (UnsupportedEncodingException e) {
								return null;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (is != null) {
							is.close();
						}
						if (zis != null) {
							zis.close();
						}
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(GPXFile result) {
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				handleResult(result, name, save, useImportDir, false);
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
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
								return GPXUtilities.loadGPXFile(new ByteArrayInputStream(result.getBytes("UTF-8")));
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
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				handleResult(result, name, save, useImportDir, false);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	private void handleObfImport(final Uri obfFile, final String name) {
		new AsyncTask<Void, Void, String>() {

			ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected String doInBackground(Void... voids) {
				String error = copyFile(app, getObfDestFile(name), obfFile, false);
				if (error == null) {
					app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
					app.getDownloadThread().updateLoadedFiles();
					return app.getString(R.string.map_imported_successfully);
				}
				return app.getString(R.string.map_import_error) + ": " + error;
			}

			@Override
			protected void onPostExecute(String message) {
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	private File getObfDestFile(@NonNull String name) {
		if (name.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
			return app.getAppPath(IndexConstants.ROADS_INDEX_DIR + name);
		} else if (name.endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
			return app.getAppPath(IndexConstants.WIKI_INDEX_DIR + name);
		}
		return app.getAppPath(name);
	}

	@Nullable
	public static String copyFile(OsmandApplication app, @NonNull File dest, @NonNull Uri uri, boolean overwrite) {
		if (dest.exists() && !overwrite) {
			return app.getString(R.string.file_with_name_already_exists);
		}
		String error = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			final ParcelFileDescriptor pFD = app.getContentResolver().openFileDescriptor(uri, "r");
			if (pFD != null) {
				in = new FileInputStream(pFD.getFileDescriptor());
				out = new FileOutputStream(dest);
				Algorithms.streamCopy(in, out);
				try {
					pFD.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			error = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			error = e.getMessage();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return error;
	}

	@SuppressLint("StaticFieldLeak")
	private void handleSqliteTileImport(final Uri uri, final String name) {
		new AsyncTask<Void, Void, String>() {

			ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected String doInBackground(Void... voids) {
				return copyFile(app, app.getAppPath(IndexConstants.TILES_INDEX_DIR + name), uri, false);
			}

			@Override
			protected void onPostExecute(String error) {
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				if (error == null) {
					OsmandRasterMapsPlugin plugin = OsmandPlugin.getPlugin(OsmandRasterMapsPlugin.class);
					if (plugin != null && !plugin.isActive() && !plugin.needsInstallation()) {
						OsmandPlugin.enablePlugin(getMapActivity(), app, plugin, true);
					}
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						mapActivity.getMapLayers().selectMapLayer(mapActivity.getMapView(), null, null);
					}
					Toast.makeText(app, app.getString(R.string.map_imported_successfully), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(app, app.getString(R.string.map_import_error) + ": " + error, Toast.LENGTH_SHORT).show();
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	private void handleOsmAndSettingsImport(final Uri uri, final String name, final String latestChanges, final int version) {
		final AsyncTask<Void, Void, String> settingsImportTask = new AsyncTask<Void, Void, String>() {

			ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected String doInBackground(Void... voids) {
				File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
				if (!tempDir.exists()) {
					tempDir.mkdirs();
				}
				File dest = new File(tempDir, name);
				return copyFile(app, dest, uri, true);
			}

			@Override
			protected void onPostExecute(String error) {
				File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
				File file = new File(tempDir, name);
				if (error == null && file.exists()) {
					app.getSettingsHelper().importSettings(file, latestChanges, version, new SettingsImportListener() {
						@Override
						public void onSettingsImportFinished(boolean succeed, boolean empty) {
							if (isActivityNotDestroyed(activity)) {
								progress.dismiss();
							}
							if (succeed) {
								app.showShortToastMessage(app.getString(R.string.file_imported_successfully, name));
							} else if (!empty) {
								app.showShortToastMessage(app.getString(R.string.file_import_error, name, app.getString(R.string.shared_string_unexpected_error)));
							}
						}
					});
				} else {
					if (isActivityNotDestroyed(activity)) {
						progress.dismiss();
					}
					app.showShortToastMessage(app.getString(R.string.file_import_error, name, error));
				}
			}
		};
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					settingsImportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			});
		} else {
			settingsImportTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private boolean isActivityNotDestroyed(Activity activity) {
		if (Build.VERSION.SDK_INT >= 17) {
			return !activity.isFinishing() && !activity.isDestroyed();
		}
		return !activity.isFinishing();
	}

	private void handleResult(final GPXFile result, final String name, final boolean save,
							  final boolean useImportDir, boolean forceImportFavourites) {
		if (result != null) {
			if (result.error != null) {
				Toast.makeText(activity, result.error.getMessage(), Toast.LENGTH_LONG).show();
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onComplete(false);
				}
			} else {
				if (save) {
					new SaveAsyncTask(result, name, useImportDir).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					showGpxInDetailsActivity(result);
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
		if (forceImportFavourites) {
			final Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(TAB_ID, GPX_TAB);
			activity.startActivity(newIntent);
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
				final WptPt pt = gpxFile.findPointToShow();
				final File toWrite = getFileToSave(fileName, importDir, pt);
				boolean destinationExists = toWrite.exists();
				Exception e = GPXUtilities.writeGpxFile(toWrite, gpxFile);
				if (e == null) {
					gpxFile.path = toWrite.getAbsolutePath();
					File file = new File(gpxFile.path);
					if (!destinationExists) {
						GPXDatabase.GpxDataItem item = new GPXDatabase.GpxDataItem(file, gpxFile.getColor(0));
						app.getGpxDbHelper().add(item);
					} else {
						GPXDatabase.GpxDataItem item = app.getGpxDbHelper().getItem(file);
						if (item != null) {
							app.getGpxDbHelper().clearAnalysis(item);
						}
					}

					warning = null;
				} else {
					warning = app.getString(R.string.error_reading_gpx);
				}
			} else {
				warning = app.getString(R.string.sd_dir_not_accessible);
			}
		}

		return warning;
	}

	private File getFileToSave(final String fileName, final File importDir, final WptPt pt) {
		final StringBuilder builder = new StringBuilder(fileName);
		if ("".equals(fileName)) {
			builder.append("import_").append(new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time))).append(GPX_SUFFIX); //$NON-NLS-1$
		}
		if (fileName.endsWith(KML_SUFFIX)) {
			builder.replace(builder.length() - KML_SUFFIX.length(), builder.length(), GPX_SUFFIX);
		} else if (fileName.endsWith(KMZ_SUFFIX)) {
			builder.replace(builder.length() - KMZ_SUFFIX.length(), builder.length(), GPX_SUFFIX);
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
			if(Algorithms.isEmpty(warning)) {
				showGpxInDetailsActivity(result);
			} else {
				Toast.makeText(activity, warning, Toast.LENGTH_LONG).show();
			}
		}
	}

	private MapActivity getMapActivity() {
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	private void showGpxInDetailsActivity(final GPXFile gpxFile) {
		if (gpxFile.path != null) {
			Intent newIntent = new Intent(activity, app.getAppCustomization().getTrackActivity());
			newIntent.putExtra(TrackActivity.TRACK_FILE_NAME, gpxFile.path);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(newIntent);
		}
	}

	private void showGpxOnMap(final GPXFile result) {
		if (mapView != null && getMapActivity() != null) {
			app.getSelectedGpxHelper().setGpxFileToDisplay(result);
			final WptPt moveTo = result.findPointToShow();
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
								  final boolean useImportDir, final boolean forceImportFavourites) {
		if (gpxFile == null || gpxFile.isPointsEmpty()) {
			if (forceImportFavourites) {
				final DialogInterface.OnClickListener importAsTrackListener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								handleResult(gpxFile, fileName, save, useImportDir, true);
								break;
							case DialogInterface.BUTTON_NEGATIVE:
								dialog.dismiss();
								break;
						}
					}
				};

				new AlertDialog.Builder(activity)
						.setTitle(R.string.import_track)
						.setMessage(activity.getString(R.string.import_track_desc, fileName))
						.setPositiveButton(R.string.shared_string_import, importAsTrackListener)
						.setNegativeButton(R.string.shared_string_cancel, importAsTrackListener)
						.show();
				return;
			} else {
				handleResult(gpxFile, fileName, save, useImportDir, false);
				return;
			}
		}

		if (forceImportFavourites) {
			importFavoritesImpl(gpxFile, fileName, true);
		} else if (fileName != null) {
			ImportGpxBottomSheetDialogFragment fragment = new ImportGpxBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.setImportHelper(this);
			fragment.setGpxFile(gpxFile);
			fragment.setFileName(fileName);
			fragment.setSave(save);
			fragment.setUseImportDir(useImportDir);
			activity.getSupportFragmentManager().beginTransaction()
					.add(fragment, ImportGpxBottomSheetDialogFragment.TAG)
					.commitAllowingStateLoss();
		}
	}

	private List<FavouritePoint> asFavourites(final List<WptPt> wptPts, String fileName, boolean forceImportFavourites) {
		final List<FavouritePoint> favourites = new ArrayList<>();
		for (WptPt p : wptPts) {
			if (p.name != null) {
				final String fpCat;
				if (p.category == null) {
					if (forceImportFavourites) {
						fpCat = fileName;
					} else {
						fpCat = "";
					}
				} else {
					fpCat = p.category;
				}
				final FavouritePoint fp = new FavouritePoint(p.lat, p.lon, p.name, fpCat);
				if (p.desc != null) {
					fp.setDescription(p.desc);
				}
				fp.setColor(p.getColor(0));
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

	public static class ImportGpxBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

		public static final String TAG = "ImportGpxBottomSheetDialogFragment";

		private ImportHelper importHelper;

		private GPXFile gpxFile;
		private String fileName;
		private boolean save;
		private boolean useImportDir;

		public void setImportHelper(ImportHelper importHelper) {
			this.importHelper = importHelper;
		}

		public void setGpxFile(GPXFile gpxFile) {
			this.gpxFile = gpxFile;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public void setSave(boolean save) {
			this.save = save;
		}

		public void setUseImportDir(boolean useImportDir) {
			this.useImportDir = useImportDir;
		}

		@Override
		public void createMenuItems(Bundle savedInstanceState) {
			items.add(new TitleItem(getString(R.string.import_file)));

			int nameColor = getResolvedColor(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
			int descrColor = getResolvedColor(nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light);
			String descr = getString(R.string.import_gpx_file_description);
			if(!descr.contains("%s")) {
				descr = "%s " +descr;
			}

			CharSequence txt = AndroidUtils.getStyledString(descr, fileName, new ForegroundColorSpan(descrColor),
					new ForegroundColorSpan(nameColor));
			items.add(new ShortDescriptionItem(txt));

			BaseBottomSheetItem asFavoritesItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_fav_dark))
					.setTitle(getString(R.string.import_as_favorites))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							importHelper.importFavoritesImpl(gpxFile, fileName, false);
							dismiss();
						}
					})
					.create();
			items.add(asFavoritesItem);

			items.add(new DividerHalfItem(getContext()));

			BaseBottomSheetItem asGpxItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
					.setTitle(getString(R.string.import_as_gpx))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							importHelper.handleResult(gpxFile, fileName, save, useImportDir, false);
							dismiss();
						}
					})
					.create();
			items.add(asGpxItem);
		}
	}
}
