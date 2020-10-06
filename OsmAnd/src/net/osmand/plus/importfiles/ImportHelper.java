package net.osmand.plus.importfiles;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.dialogs.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.settings.backend.SettingsHelper;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsItem;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_CHART_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.IndexConstants.WPT_CHART_FILE_EXT;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.plus.myplaces.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;

/**
 * @author Koen Rabaey
 */
public class ImportHelper {

	public static final Log log = PlatformUtil.getLog(ImportHelper.class);

	public static final String KML_SUFFIX = ".kml";
	public static final String KMZ_SUFFIX = ".kmz";

	public static final int IMPORT_FILE_REQUEST = 1006;

	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private final AppCompatActivity activity;

	private OnGpxImportCompleteListener gpxImportCompleteListener;

	public enum ImportType {
		SETTINGS(OSMAND_SETTINGS_FILE_EXT),
		ROUTING(ROUTING_FILE_EXT),
		RENDERING(RENDERER_INDEX_EXT);

		ImportType(String extension) {
			this.extension = extension;
		}

		private String extension;

		public String getExtension() {
			return extension;
		}
	}

	public interface OnGpxImportCompleteListener {
		void onImportComplete(boolean success);

		void onSaveComplete(boolean success, GPXFile result);
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
		String name = getNameFromContentUri(app, contentUri);
		handleFileImport(contentUri, name, extras, useImportDir);
	}

	public void importFavoritesFromGpx(final GPXFile gpxFile, final String fileName) {
		importFavoritesImpl(gpxFile, fileName, false);
	}

	public void handleGpxImport(GPXFile result, String name, boolean save, boolean useImportDir) {
		handleResult(result, name, save, useImportDir, false);
	}

	public boolean handleGpxImport(final Uri contentUri, final boolean useImportDir) {
		return handleGpxImport(contentUri, useImportDir, true);
	}

	public boolean handleGpxImport(final Uri contentUri, final boolean useImportDir, boolean showInDetailsActivity) {
		String name = getNameFromContentUri(app, contentUri);
		boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(contentUri.getPath()));
		if (!isOsmandSubdir && name != null) {
			String nameLC = name.toLowerCase();
			if (nameLC.endsWith(GPX_FILE_EXT)) {
				name = name.substring(0, name.length() - GPX_FILE_EXT.length()) + GPX_FILE_EXT;
				handleGpxImport(contentUri, name, true, useImportDir, showInDetailsActivity);
				return true;
			} else if (nameLC.endsWith(KML_SUFFIX)) {
				name = name.substring(0, name.length() - KML_SUFFIX.length()) + KML_SUFFIX;
				handleKmlImport(contentUri, name, true, useImportDir);
				return true;
			} else if (nameLC.endsWith(KMZ_SUFFIX)) {
				name = name.substring(0, name.length() - KMZ_SUFFIX.length()) + KMZ_SUFFIX;
				handleKmzImport(contentUri, name, true, useImportDir);
				return true;
			}
		}
		return false;
	}

	public void handleGpxOrFavouritesImport(@NonNull Uri uri) {
		String scheme = uri.getScheme();
		boolean isFileIntent = "file".equals(scheme);
		boolean isContentIntent = "content".equals(scheme);
		boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(uri.getPath()));
		final boolean saveFile = !isFileIntent || !isOsmandSubdir;
		String fileName = "";
		if (isFileIntent) {
			fileName = new File(uri.getPath()).getName();
		} else if (isContentIntent) {
			fileName = getNameFromContentUri(app, uri);
		}
		handleGpxOrFavouritesImport(uri, fileName, saveFile, false, true, false);
	}

	public void handleFileImport(Uri intentUri, String fileName, Bundle extras, boolean useImportDir) {
		boolean isFileIntent = "file".equals(intentUri.getScheme());
		boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(intentUri.getPath()));
		boolean saveFile = !isFileIntent || !isOsmandSubdir;

		if (fileName == null) {
			handleGpxOrFavouritesImport(intentUri, fileName, saveFile, useImportDir, false, false);
		} else if (fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName.endsWith(KMZ_SUFFIX)) {
			handleKmzImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName.endsWith(BINARY_MAP_INDEX_EXT)) {
			handleObfImport(intentUri, fileName);
		} else if (fileName.endsWith(SQLITE_EXT)) {
			handleSqliteTileImport(intentUri, fileName);
		} else if (fileName.endsWith(OSMAND_SETTINGS_FILE_EXT)) {
			handleOsmAndSettingsImport(intentUri, fileName, extras, null);
		} else if (fileName.endsWith(ROUTING_FILE_EXT)) {
			handleXmlFileImport(intentUri, fileName, null);
		} else if (fileName.endsWith(WPT_CHART_FILE_EXT)) {
			handleGpxOrFavouritesImport(intentUri, fileName.replace(WPT_CHART_FILE_EXT, GPX_FILE_EXT), saveFile, useImportDir, false, true);
		} else if (fileName.endsWith(SQLITE_CHART_FILE_EXT)) {
			handleSqliteTileImport(intentUri, fileName.replace(SQLITE_CHART_FILE_EXT, SQLITE_EXT));
		} else {
			handleGpxOrFavouritesImport(intentUri, fileName, saveFile, useImportDir, false, false);
		}
	}

	public static String getNameFromContentUri(OsmandApplication app, Uri contentUri) {
		try {
			String name;
			Cursor returnCursor = app.getContentResolver().query(contentUri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null);
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
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	private void handleGpxImport(Uri gpxFile, String fileName, boolean save, boolean useImportDir, boolean showInDetailsActivity) {
		executeImportTask(new GpxImportTask(this, activity, gpxFile, fileName, save, useImportDir, showInDetailsActivity));
	}

	private void handleGpxOrFavouritesImport(Uri fileUri, String fileName, boolean save, boolean useImportDir,
											 boolean forceImportFavourites, boolean forceImportGpx) {
		executeImportTask(new GpxOrFavouritesImportTask(this, activity, fileUri, fileName, save, useImportDir, forceImportFavourites, forceImportGpx));
	}

	private void importFavoritesImpl(GPXFile gpxFile, String fileName, boolean forceImportFavourites) {
		executeImportTask(new FavoritesImportTask(activity, gpxFile, fileName, forceImportFavourites));
	}

	private void handleKmzImport(Uri kmzFile, String name, boolean save, boolean useImportDir) {
		executeImportTask(new KmzImportTask(this, activity, kmzFile, name, save, useImportDir));
	}

	private void handleKmlImport(Uri kmlFile, String name, boolean save, boolean useImportDir) {
		executeImportTask(new KmlImportTask(this, activity, kmlFile, name, save, useImportDir));
	}

	private void handleObfImport(Uri obfFile, String name) {
		executeImportTask(new ObfImportTask(activity, obfFile, name));
	}

	private void handleSqliteTileImport(Uri uri, String name) {
		executeImportTask(new SqliteTileImportTask(activity, uri, name));
	}

	private void handleOsmAndSettingsImport(Uri intentUri, String fileName, Bundle extras, CallbackWithObject<List<SettingsItem>> callback) {
		if (extras != null && extras.containsKey(SettingsHelper.SETTINGS_VERSION_KEY) && extras.containsKey(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY)) {
			int version = extras.getInt(SettingsHelper.SETTINGS_VERSION_KEY, -1);
			String latestChanges = extras.getString(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY);
			handleOsmAndSettingsImport(intentUri, fileName, latestChanges, version, callback);
		} else {
			handleOsmAndSettingsImport(intentUri, fileName, null, -1, callback);
		}
	}

	private void handleOsmAndSettingsImport(Uri uri, String name, String latestChanges, int version,
											CallbackWithObject<List<SettingsItem>> callback) {
		executeImportTask(new SettingsImportTask(activity, uri, name, latestChanges, version, callback));
	}

	private void handleXmlFileImport(Uri intentUri, String fileName, CallbackWithObject routingCallback) {
		executeImportTask(new XmlImportTask(activity, intentUri, fileName, routingCallback));
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
			in = app.getContentResolver().openInputStream(uri);
			if (in != null) {
				out = new FileOutputStream(dest);
				Algorithms.streamCopy(in, out);
				try {
					in.close();
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
		} catch (SecurityException e) {
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

	public void chooseFileToImport(final ImportType importType, final CallbackWithObject callback) {
		final MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		final OsmandApplication app = mapActivity.getMyApplication();
		Intent intent = ImportHelper.getImportTrackIntent();

		ActivityResultListener listener = new ActivityResultListener(IMPORT_FILE_REQUEST, new ActivityResultListener.OnActivityResultListener() {
			@Override
			public void onResult(int resultCode, Intent resultData) {
				MapActivity mapActivity = getMapActivity();
				if (resultCode == RESULT_OK) {
					Uri data = resultData.getData();
					if (mapActivity == null || data == null) {
						return;
					}
					String scheme = data.getScheme();
					String fileName = "";
					if ("file".equals(scheme)) {
						final String path = data.getPath();
						if (path != null) {
							fileName = new File(path).getName();
						}
					} else if ("content".equals(scheme)) {
						fileName = getNameFromContentUri(app, data);
					}

					if (fileName.endsWith(importType.getExtension())) {
						if (importType.equals(ImportType.SETTINGS)) {
							handleOsmAndSettingsImport(data, fileName, resultData.getExtras(), callback);
						} else if (importType.equals(ImportType.ROUTING)) {
							handleXmlFileImport(data, fileName, callback);
						}
					} else {
						app.showToastMessage(app.getString(R.string.not_support_file_type_with_ext,
								importType.getExtension().replaceAll("\\.", "").toUpperCase()));
					}
				}
			}
		});

		mapActivity.registerActivityResultListener(listener);
		mapActivity.startActivityForResult(intent, IMPORT_FILE_REQUEST);
	}

	public static Intent getImportTrackIntent() {
		Intent intent = new Intent();
		String action;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			action = Intent.ACTION_OPEN_DOCUMENT;
		} else {
			action = Intent.ACTION_GET_CONTENT;
		}
		intent.setAction(action);
		intent.setType("*/*");
		return intent;
	}

	protected void handleResult(GPXFile result, String name, boolean save,
								boolean useImportDir, boolean forceImportFavourites) {
		handleResult(result, name, save, useImportDir, forceImportFavourites, true);
	}

	protected void handleResult(final GPXFile result, final String name, final boolean save,
								final boolean useImportDir, boolean forceImportFavourites, boolean showInDetailsActivity) {
		if (result != null) {
			if (result.error != null) {
				Toast.makeText(activity, result.error.getMessage(), Toast.LENGTH_LONG).show();
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onImportComplete(false);
				}
			} else {
				if (save) {
					executeImportTask(new SaveAsyncTask(result, name, useImportDir, showInDetailsActivity));
				} else {
					showGpxInDetailsActivity(result);
				}
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onImportComplete(true);
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
								gpxImportCompleteListener.onImportComplete(false);
							}
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (gpxImportCompleteListener != null) {
								gpxImportCompleteListener.onImportComplete(false);
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
				importDir = app.getAppPath(GPX_INDEX_DIR);
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
						GpxDataItem item = new GpxDataItem(file, gpxFile);
						app.getGpxDbHelper().add(item);
					} else {
						GpxDataItem item = app.getGpxDbHelper().getItem(file);
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
			builder.append("import_").append(new SimpleDateFormat("HH-mm_EEE", Locale.US).format(new Date(pt.time))).append(GPX_FILE_EXT); //$NON-NLS-1$
		}
		if (fileName.endsWith(KML_SUFFIX)) {
			builder.replace(builder.length() - KML_SUFFIX.length(), builder.length(), GPX_FILE_EXT);
		} else if (fileName.endsWith(KMZ_SUFFIX)) {
			builder.replace(builder.length() - KMZ_SUFFIX.length(), builder.length(), GPX_FILE_EXT);
		} else if (!fileName.endsWith(GPX_FILE_EXT)) {
			builder.append(GPX_FILE_EXT);
		}
		return new File(importDir, builder.toString());
	}

	private class SaveAsyncTask extends AsyncTask<Void, Void, String> {
		private final GPXFile result;
		private final String name;
		private final boolean useImportDir;
		private boolean showInDetailsActivity;

		private SaveAsyncTask(GPXFile result, final String name, boolean useImportDir, boolean showInDetailsActivity) {
			this.result = result;
			this.name = name;
			this.useImportDir = useImportDir;
			this.showInDetailsActivity = showInDetailsActivity;
		}

		@Override
		protected String doInBackground(Void... nothing) {
			return saveImport(result, name, useImportDir);
		}

		@Override
		protected void onPostExecute(final String warning) {
			boolean success = Algorithms.isEmpty(warning);

			if (gpxImportCompleteListener != null) {
				gpxImportCompleteListener.onSaveComplete(success, result);
			}
			if (success) {
				if (showInDetailsActivity) {
					showGpxInDetailsActivity(result);
				} else {
					showPlanRouteFragment();
				}
			} else {
				Toast.makeText(activity, warning, Toast.LENGTH_LONG).show();
			}
		}

		private void showPlanRouteFragment() {
			MeasurementToolFragment.showInstance(activity.getSupportFragmentManager(), result);
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

	protected void importGpxOrFavourites(final GPXFile gpxFile, final String fileName, final boolean save,
										 final boolean useImportDir, final boolean forceImportFavourites,
										 final boolean forceImportGpx) {
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
			} else {
				handleResult(gpxFile, fileName, save, useImportDir, false);
			}
			return;
		}

		if (forceImportFavourites) {
			importFavoritesImpl(gpxFile, fileName, true);
		} else if (fileName != null) {
			if (forceImportGpx) {
				handleResult(gpxFile, fileName, save, useImportDir, false);
			} else {
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
	}

	protected static List<FavouritePoint> asFavourites(OsmandApplication app, List<WptPt> wptPts, String fileName, boolean forceImportFavourites) {
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
				fp.setAddress(p.getExtensionsToRead().get("address"));
				fp.setColor(p.getColor(0));
				fp.setIconIdFromName(app, p.getIconName());
				fp.setBackgroundType(BackgroundType.getByTypeName(p.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));
				favourites.add(fp);
			}
		}
		return favourites;
	}

	@SuppressWarnings("unchecked")
	private <P> void executeImportTask(final AsyncTask<P, ?, ?> importTask, final P... requests) {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					importTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
				}
			});
		} else {
			importTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
		}
	}
}