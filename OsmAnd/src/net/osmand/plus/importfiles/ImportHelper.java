package net.osmand.plus.importfiles;

import static android.app.Activity.RESULT_OK;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_CHART_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.IndexConstants.WPT_CHART_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.helpers.IntentHelper.REQUEST_CODE_CREATE_FILE;
import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU;
import static net.osmand.plus.importfiles.OnSuccessfulGpxImport.OPEN_PLAN_ROUTE_FRAGMENT;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.PLAN_ROUTE_MODE;
import static net.osmand.plus.myplaces.MyPlacesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.MyPlacesActivity.TAB_ID;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.REPLACE_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.EXPORT_TYPE_LIST_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.SETTINGS_LATEST_CHANGES_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.SETTINGS_VERSION_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.SILENT_IMPORT_KEY;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.importfiles.tasks.CopyToFileTask;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.importfiles.tasks.FavoritesImportTask;
import net.osmand.plus.importfiles.tasks.GeoTiffImportTask;
import net.osmand.plus.importfiles.tasks.GpxImportTask;
import net.osmand.plus.importfiles.tasks.ObfImportTask;
import net.osmand.plus.importfiles.tasks.SaveGpxAsyncTask;
import net.osmand.plus.importfiles.tasks.SettingsImportTask;
import net.osmand.plus.importfiles.tasks.SqliteTileImportTask;
import net.osmand.plus.importfiles.tasks.UriImportTask;
import net.osmand.plus.importfiles.tasks.XmlImportTask;
import net.osmand.plus.importfiles.tasks.ZipImportTask;
import net.osmand.plus.importfiles.ui.FileExistBottomSheet;
import net.osmand.plus.importfiles.ui.FileExistBottomSheet.SaveExistingFileListener;
import net.osmand.plus.importfiles.ui.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.importfiles.ui.ImportTracksFragment;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Koen Rabaey
 */
public class ImportHelper {

	public static final Log LOG = PlatformUtil.getLog(ImportHelper.class);

	public static final String KML_SUFFIX = ".kml";
	public static final String KMZ_SUFFIX = ".kmz";

	public static final int IMPORT_FILE_REQUEST = 1006;

	private final OsmandApplication app;
	private List<ImportTaskListener> taskListeners = new ArrayList<>();

	@Nullable
	private FragmentActivity activity;
	private GpxImportListener gpxImportListener;

	private ActivityResultListener saveFileResultListener;

	public ImportHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void setUiActivity(@NonNull FragmentActivity activity) {
		this.activity = activity;
	}

	public void resetUIActivity(@NonNull FragmentActivity uiActivity) {
		if (this.activity == uiActivity) {
			this.activity = null;
		}
	}

	public void setGpxImportListener(@Nullable GpxImportListener gpxImportListener) {
		this.gpxImportListener = gpxImportListener;
	}

	public void addImportTaskListener(@NonNull ImportTaskListener listener) {
		if (!taskListeners.contains(listener)) {
			taskListeners = CollectionUtils.addToList(taskListeners, listener);
		}
	}

	public void removeImportTaskListener(@NonNull ImportTaskListener listener) {
		taskListeners = CollectionUtils.removeFromList(taskListeners, listener);
	}

	public void notifyImportFinished() {
		for (ImportTaskListener listener : taskListeners) {
			listener.onImportFinished();
		}
	}

	public void handleContentImport(Uri contentUri, Bundle extras, boolean useImportDir) {
		String name = getNameFromContentUri(app, contentUri);
		handleFileImport(contentUri, name, extras, useImportDir);
	}

	public void importFavoritesFromGpx(GpxFile gpxFile, String fileName) {
		importFavoritesImpl(gpxFile, fileName, false);
	}

	public void handleGpxImport(GpxFile result, String name, long fileSize, boolean save, boolean useImportDir, boolean showSnackbar) {
		handleResult(result, name, fileSize, save, useImportDir, showSnackbar);
	}

	public boolean handleGpxImport(@NonNull Uri uri, @Nullable OnSuccessfulGpxImport onGpxImport, boolean useImportDir) {
		String name = getNameFromContentUri(app, uri);
		String fileName = getGpxFileName(name);
		boolean isOsmAndSubDir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(uri.getPath()));
		if (!isOsmAndSubDir && fileName != null) {
			handleGpxImport(uri, fileName, onGpxImport, useImportDir, true, false);
		}
		return false;
	}

	@Nullable
	public static String getGpxFileName(@Nullable String name) {
		if (!Algorithms.isEmpty(name)) {
			String nameLC = name.toLowerCase();
			if (nameLC.endsWith(GPX_FILE_EXT)) {
				return name.substring(0, name.length() - GPX_FILE_EXT.length()) + GPX_FILE_EXT;
			} else if (nameLC.endsWith(KML_SUFFIX)) {
				return name.substring(0, name.length() - KML_SUFFIX.length()) + KML_SUFFIX;
			} else if (nameLC.endsWith(KMZ_SUFFIX)) {
				return name.substring(0, name.length() - KMZ_SUFFIX.length()) + KMZ_SUFFIX;
			}
		}
		return null;
	}

	public void handleFavouritesImport(@NonNull Uri uri) {
		String scheme = uri.getScheme();
		boolean isFileIntent = "file".equals(scheme);
		boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(uri.getPath()));
		boolean saveFile = !isFileIntent || !isOsmandSubdir;
		String fileName = isFileIntent ? new File(uri.getPath()).getName() : getNameFromContentUri(app, uri);
		handleGpxOrFavouritesImport(uri, fileName, saveFile, false, true, false, false);
	}

	public void handleGpxFilesImport(@NonNull List<Uri> filesUri, @NonNull File destinationDir,
	                                 @Nullable OnSuccessfulGpxImport onGpxImport,
	                                 boolean showSnackbar, boolean singleImport) {
		if (gpxImportListener != null) {
			gpxImportListener.onImportStarted();
		}
		for (Uri uri : filesUri) {
			String fileName = getGpxFileName(getNameFromContentUri(app, uri));
			boolean isOsmAndSubDir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(uri.getPath()));
			if (!isOsmAndSubDir && fileName != null) {
				CallbackWithObject<Pair<GpxFile, Long>> callback = pair -> {
					handleResult(pair.first, fileName, onGpxImport, pair.second, true, destinationDir, showSnackbar, singleImport);
					return true;
				};
				GpxImportTask gpxImportTask = new GpxImportTask(activity, uri, fileName, callback);
				gpxImportTask.setShouldShowProgress(false);
				executeImportTask(gpxImportTask);
			} else if (gpxImportListener != null) {
				gpxImportListener.onImportComplete(false);
			}
		}
	}

	public void handleImport(@NonNull Intent intent) {
		Uri uri = intent.getData();
		if (uri != null) {
			String scheme = intent.getScheme();
			if ("file".equals(scheme)) {
				String path = uri.getPath();
				if (!Algorithms.isEmpty(path)) {
					handleFileImport(uri, new File(path).getName(), intent.getExtras(), true);
				}
			} else if ("content".equals(scheme)) {
				handleContentImport(uri, intent.getExtras(), true);
			}
		}
	}

	public void handleFileImport(Uri intentUri, String fileName, Bundle extras, boolean useImportDir) {
		boolean isFileIntent = "file".equals(intentUri.getScheme());
		boolean isOsmAndSubDir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(intentUri.getPath()));
		boolean saveFile = !isFileIntent || !isOsmAndSubDir;

		if (fileName == null) {
			handleUriImport(intentUri, saveFile, useImportDir);
		} else if (fileName.endsWith(KML_SUFFIX) || fileName.endsWith(KMZ_SUFFIX)) {
			handleGpxImport(intentUri, fileName, OPEN_GPX_CONTEXT_MENU, saveFile, useImportDir, false);
		} else if (fileName.endsWith(BINARY_MAP_INDEX_EXT) || fileName.endsWith(BINARY_MAP_INDEX_EXT + ZIP_EXT)) {
			handleObfImport(intentUri, fileName);
		} else if (fileName.endsWith(SQLITE_EXT)) {
			handleSqliteTileImport(intentUri, fileName);
		} else if (fileName.endsWith(OSMAND_SETTINGS_FILE_EXT) || fileName.endsWith(OSMAND_SETTINGS_FILE_EXT + ZIP_EXT)) {
			handleOsmAndSettingsImport(intentUri, fileName, extras);
		} else if (fileName.endsWith(ROUTING_FILE_EXT)) {
			handleXmlFileImport(intentUri, fileName);
		} else if (fileName.endsWith(WPT_CHART_FILE_EXT)) {
			handleGpxOrFavouritesImport(intentUri, fileName.replace(WPT_CHART_FILE_EXT, GPX_FILE_EXT), saveFile, useImportDir, false, true, false);
		} else if (fileName.endsWith(SQLITE_CHART_FILE_EXT)) {
			handleSqliteTileImport(intentUri, fileName.replace(SQLITE_CHART_FILE_EXT, SQLITE_EXT));
		} else {
			handleGpxOrFavouritesImport(intentUri, fileName, saveFile, useImportDir, false, false, false);
		}
	}

	@Nullable
	public static String getNameFromContentUri(@NonNull OsmandApplication app, @NonNull Uri contentUri) {
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
			LOG.error(e.getMessage(), e);
			return null;
		}
	}

	public void handleGpxImport(@NonNull Uri uri, @NonNull String fileName, @Nullable OnSuccessfulGpxImport onGpxImport,
	                            boolean useImportDir, boolean save, boolean showSnackbar) {
		CallbackWithObject<Pair<GpxFile, Long>> callback = pair -> {
			handleResult(pair.first, fileName, onGpxImport, pair.second, save, getGpxDestinationDir(app, useImportDir), showSnackbar, true);
			return true;
		};
		executeImportTask(new GpxImportTask(activity, uri, fileName, callback));
	}

	public void handleGpxOrFavouritesImport(Uri uri, String fileName, boolean save, boolean useImportDir,
	                                        boolean forceImportFavourites, boolean forceImportGpx,
	                                        boolean showSnackbar) {
		CallbackWithObject<Pair<GpxFile, Long>> callback = pair -> {
			importGpxOrFavourites(pair.first, fileName, pair.second, save, useImportDir,
					forceImportFavourites, forceImportGpx, showSnackbar);
			return true;
		};
		executeImportTask(new GpxImportTask(activity, uri, fileName, callback));
	}

	private void importFavoritesImpl(GpxFile gpxFile, String fileName, boolean forceImportFavourites) {
		executeImportTask(new FavoritesImportTask(activity, gpxFile, fileName, forceImportFavourites));
	}

	public void handleObfImport(Uri obfFile, String name) {
		executeImportTask(new ObfImportTask(activity, obfFile, name));
	}

	public void handleSqliteTileImport(Uri uri, String name) {
		executeImportTask(new SqliteTileImportTask(activity, uri, name));
	}

	protected void handleGeoTiffImport(Uri uri, String name) {
		executeImportTask(new GeoTiffImportTask(activity, uri, name));
	}


	public void handleCopyFileToFile(@NonNull File originalFile, @NonNull Uri destinationUri) {
		executeImportTask(new CopyToFileTask(activity, originalFile, destinationUri));
	}

	private void handleOsmAndSettingsImport(Uri intentUri, String fileName, Bundle extras) {
		fileName = fileName.replace(ZIP_EXT, "");
		if (extras != null && CollectionUtils.containsAny(extras.keySet(), SETTINGS_VERSION_KEY, SETTINGS_LATEST_CHANGES_KEY)) {
			int version = extras.getInt(SETTINGS_VERSION_KEY, -1);
			String latestChanges = extras.getString(SETTINGS_LATEST_CHANGES_KEY);
			boolean replace = extras.getBoolean(REPLACE_KEY);
			boolean silentImport = extras.getBoolean(SILENT_IMPORT_KEY);
			ArrayList<String> exportTypeKeys = extras.getStringArrayList(EXPORT_TYPE_LIST_KEY);
			List<ExportType> exportTypes = null;
			if (exportTypeKeys != null) {
				exportTypes = ExportType.valuesOf(exportTypeKeys);
			}
			handleOsmAndSettingsImport(intentUri, fileName, exportTypes, replace, silentImport, latestChanges, version);
		} else {
			handleOsmAndSettingsImport(intentUri, fileName, null, false, false, null, -1);
		}
	}

	public void handleOsmAndSettingsImport(Uri uri, String name, List<ExportType> settingsTypes,
	                                       boolean replace, boolean silentImport, String latestChanges, int version) {
		executeImportTask(new SettingsImportTask(activity, uri, name, settingsTypes, replace, silentImport, latestChanges, version));
	}

	public void handleXmlFileImport(@NonNull Uri intentUri, @NonNull String fileName) {
		if (fileExists(intentUri, fileName)) {
			app.runInUIThread(() -> {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					FileExistBottomSheet.showInstance(activity.getSupportFragmentManager(), fileName, overwrite -> {
						handleXmlFileImportImpl(intentUri, fileName, overwrite);
					});
				}
			});
		} else {
			handleXmlFileImportImpl(intentUri, fileName, true);
		}
	}

	private void handleXmlFileImportImpl(@NonNull Uri intentUri, @NonNull String fileName, boolean overwrite) {
		executeImportTask(new XmlImportTask(activity, intentUri, fileName, overwrite));
	}

	private void handleUriImport(@NonNull Uri uri, boolean save, boolean useImportDir) {
		executeImportTask(new UriImportTask(this, activity, uri, save, useImportDir));
	}

	public void handleZipImport(@NonNull Uri uri, boolean save, boolean useImportDir) {
		executeImportTask(new ZipImportTask(this, activity, uri, save, useImportDir));
	}

	private boolean fileExists(@NonNull Uri intentUri, @NonNull String fileName) {
		ImportType importType = XmlImportTask.checkImportType(app, intentUri);
		if (importType != null) {
			File destDir = XmlImportTask.getDestinationDir(app, importType);
			File destFile = new File(destDir, fileName);
			return destFile.exists();
		}
		return false;
	}

	@Nullable
	public static String copyFile(@NonNull OsmandApplication app, @NonNull File dest, @NonNull Uri uri, boolean overwrite, boolean unzip) {
		if (dest.exists() && !overwrite) {
			return app.getString(R.string.file_with_name_already_exists);
		}
		String error = null;
		InputStream in = null;
		try {
			in = app.getContentResolver().openInputStream(uri);
			if (in != null) {
				error = copyFile(app, dest, in, overwrite, unzip);
			}
		} catch (IOException | SecurityException e) {
			e.printStackTrace();
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(in);
		}
		return error;
	}

	@Nullable
	public static String copyFile(@NonNull OsmandApplication app, @NonNull File dest, @NonNull InputStream in, boolean overwrite, boolean unzip) {
		if (dest.exists() && !overwrite) {
			return app.getString(R.string.file_with_name_already_exists);
		}
		String error = null;
		OutputStream out = null;
		ZipInputStream zis = null;
		try {
			if (unzip) {
				ZipEntry entry;
				zis = new ZipInputStream(in);
				String extension = Algorithms.getFileExtension(dest);
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().endsWith(extension)) {
						out = new FileOutputStream(dest);
						Algorithms.streamCopy(zis, out);
						break;
					}
				}
			} else {
				out = new FileOutputStream(dest);
				Algorithms.streamCopy(in, out);
			}
		} catch (IOException | SecurityException e) {
			e.printStackTrace();
			error = e.getMessage();
		} finally {
			Algorithms.closeStream(in);
			Algorithms.closeStream(out);
			Algorithms.closeStream(zis);
		}
		return error;
	}

	public void chooseFileToImport(@NonNull ImportType importType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				Intent intent = getImportFileIntent();
				ActivityResultListener listener = getImportFileResultListener(importType);
				mapActivity.startActivityForResult(intent, IMPORT_FILE_REQUEST);
				mapActivity.registerActivityResultListener(listener);
			} catch (ActivityNotFoundException e) {
				app.showToastMessage(R.string.no_activity_for_intent);
			}
		}
	}

	@NonNull
	private ActivityResultListener getImportFileResultListener(@NonNull ImportType importType) {
		return new ActivityResultListener(IMPORT_FILE_REQUEST, (resultCode, resultData) -> {
			if (resultCode == RESULT_OK) {
				Uri data = resultData.getData();
				if (data == null) {
					return;
				}
				String scheme = data.getScheme();
				String fileName = "";
				if ("file".equals(scheme)) {
					String path = data.getPath();
					if (path != null) {
						fileName = new File(path).getName();
					}
				} else if ("content".equals(scheme)) {
					fileName = getNameFromContentUri(app, data);
				}
				if (fileName != null && fileName.endsWith(importType.getExtension())) {
					if (importType.equals(ImportType.SETTINGS)) {
						handleOsmAndSettingsImport(data, fileName, resultData.getExtras());
					} else if (importType.equals(ImportType.ROUTING)) {
						handleXmlFileImport(data, fileName);
					}
				} else {
					app.showToastMessage(R.string.not_support_file_type_with_ext,
							importType.getExtension().replaceAll("\\.", "").toUpperCase());
				}
			}
		});
	}

	@NonNull
	public static Intent getImportFileIntent() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
		intent.setType("*/*");
		return intent;
	}

	protected void handleResult(GpxFile result, String name, long fileSize, boolean save, boolean useImportDir, boolean showSnackbar) {
		handleResult(result, name, OPEN_GPX_CONTEXT_MENU, fileSize, save, getGpxDestinationDir(app, useImportDir), showSnackbar, true);
	}

	private boolean checkGpxFile(@Nullable GpxFile gpxFile, boolean singleImport) {
		if (gpxFile == null) {
			if (singleImport) {
				showPermissionsAlert();
			}
		} else if (gpxFile.getError() != null) {
			app.showToastMessage(SharedUtil.jException(gpxFile.getError()).getMessage());
		}
		return gpxFile != null && gpxFile.getError() == null;
	}

	private void handleResult(GpxFile result, String name, OnSuccessfulGpxImport onGpxImport,
	                          long fileSize, boolean save, @NonNull File destinationDir,
	                          boolean showSnackbar, boolean singleImport) {
		boolean success = checkGpxFile(result, singleImport);
		if (success) {
			if (save) {
				int tracksCount = result.getTracksCount();
				if (singleImport && (tracksCount > 1 && tracksCount < 50)) {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						FragmentManager manager = activity.getSupportFragmentManager();
						ImportTracksFragment.showInstance(manager, result, name,
								destinationDir.getAbsolutePath(), gpxImportListener, fileSize);
					}
				} else {
					importAsOneTrack(result, name, destinationDir, showSnackbar, onGpxImport);
				}
			} else {
				showNeededScreen(onGpxImport, result);
			}
		}
		if (gpxImportListener != null) {
			gpxImportListener.onImportComplete(success);
		}
	}

	private void showPermissionsAlert() {
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			new AlertDialog.Builder(activity)
					.setTitle(R.string.shared_string_import2osmand)
					.setMessage(R.string.import_gpx_failed_descr)
					.setNeutralButton(R.string.shared_string_permissions, (dialog, which) -> {
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						Uri uri = Uri.fromParts("package", app.getPackageName(), null);
						intent.setData(uri);
						AndroidUtils.startActivityIfSafe(app, intent);
						if (gpxImportListener != null) {
							gpxImportListener.onImportComplete(false);
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, (dialog, which) -> {
						if (gpxImportListener != null) {
							gpxImportListener.onImportComplete(false);
						}
					})
					.show();
		}
	}

	private void importAsOneTrack(@NonNull GpxFile gpxFile, @NonNull String name, @NonNull File destinationDir,
	                              boolean showSnackbar, @Nullable OnSuccessfulGpxImport onGpxImport) {
		String existingFilePath = getExistingFilePath(name, destinationDir);
		SaveImportedGpxListener listener = getSaveGpxListener(gpxFile, showSnackbar, onGpxImport);

		if (existingFilePath != null) {
			SaveExistingFileListener saveFileListener = overwrite -> executeImportTask(
					new SaveGpxAsyncTask(app, gpxFile, destinationDir, name, listener, overwrite));
			app.runInUIThread(() -> {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					FileExistBottomSheet.showInstance(activity.getSupportFragmentManager(), name, saveFileListener);
				}
			});
		} else {
			executeImportTask(new SaveGpxAsyncTask(app, gpxFile, destinationDir, name, listener, false));
		}
	}

	@NonNull
	public static File getGpxDestinationDir(@NonNull OsmandApplication app, boolean useImportDir) {
		return app.getAppPath(useImportDir ? GPX_IMPORT_DIR : GPX_INDEX_DIR);
	}

	@NonNull
	private SaveImportedGpxListener getSaveGpxListener(@NonNull GpxFile gpxFile, boolean showSnackbar, @Nullable OnSuccessfulGpxImport onGpxImport) {
		return new SaveImportedGpxListener() {
			String importedFileName;

			@Override
			public void onGpxSavingStarted() {
			}

			@Override
			public void onGpxSaved(@Nullable String error, @NonNull GpxFile gpxFile) {
				importedFileName = new File(gpxFile.getPath()).getName();
			}

			@Override
			public void onGpxSavingFinished(@NonNull List<String> warnings) {
				boolean success = Algorithms.isEmpty(warnings);
				if (success) {
					if (showSnackbar) {
						Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
										app.getString(R.string.is_imported, importedFileName),
										BaseTransientBottomBar.LENGTH_LONG)
								.setAction(R.string.shared_string_open, view -> openTrack(gpxFile, onGpxImport));

						boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
						UiUtilities.setupSnackbar(snackbar, nightMode);
						snackbar.show();
					} else {
						openTrack(gpxFile, onGpxImport);
					}
				} else {
					app.showToastMessage(warnings.get(0));
				}
				if (gpxImportListener != null) {
					gpxImportListener.onSaveComplete(success, gpxFile);
				}
			}

			private void openTrack(@NonNull GpxFile gpxFile, @Nullable OnSuccessfulGpxImport onGpxImport) {
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.getPath());
				if (selectedGpxFile != null) {
					selectedGpxFile.setGpxFile(gpxFile, app);
				}
				showNeededScreen(onGpxImport, gpxFile);
			}
		};
	}

	@Nullable
	public static String getExistingFilePath(@NonNull String name, @NonNull File destinationDir) {
		List<GPXInfo> gpxInfoList = GpxUiHelper.getGPXFiles(destinationDir, true, false);
		for (GPXInfo gpxInfo : gpxInfoList) {
			String filePath = gpxInfo.getFileName();
			String fileName = Algorithms.getFileWithoutDirs(filePath);
			if (Algorithms.objectEquals(name, fileName)) {
				return filePath;
			}
		}
		return null;
	}

	@Nullable
	public static String getExistingFilePath(@NonNull OsmandApplication app, @NonNull String name, long fileSize) {
		File dir = app.getAppPath(GPX_INDEX_DIR);
		List<GPXInfo> gpxInfoList = GpxUiHelper.getSortedGPXFilesInfoByDate(dir, true);
		for (GPXInfo gpxInfo : gpxInfoList) {
			String filePath = gpxInfo.getFileName();
			String fileName = Algorithms.getFileWithoutDirs(filePath);
			if (Algorithms.objectEquals(name, fileName) && gpxInfo.getFileSize() == fileSize) {
				return filePath;
			}
		}
		return null;
	}

	private MapActivity getMapActivity() {
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	protected void showNeededScreen(@Nullable OnSuccessfulGpxImport onGpxImport, @NonNull GpxFile gpxFile) {
		if (onGpxImport == OPEN_GPX_CONTEXT_MENU) {
			showGpxContextMenu(gpxFile.getPath());
		} else if (onGpxImport == OPEN_PLAN_ROUTE_FRAGMENT) {
			showPlanRouteFragment(gpxFile);
		}
	}

	private void showGpxContextMenu(String gpxFilePath) {
		if (!Algorithms.isEmpty(gpxFilePath) && AndroidUtils.isActivityNotDestroyed(activity)) {
			TrackMenuFragment.openTrack(activity, new File(gpxFilePath), null);
		}
	}

	private void showPlanRouteFragment(@NonNull GpxFile gpxFile) {
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			GpxData gpxData = new GpxData(gpxFile);
			MeasurementEditingContext editingContext = new MeasurementEditingContext(app);
			editingContext.setGpxData(gpxData);

			FragmentManager manager = activity.getSupportFragmentManager();
			MeasurementToolFragment.showInstance(manager, editingContext, PLAN_ROUTE_MODE, false);
		}
	}

	protected void importGpxOrFavourites(GpxFile gpxFile, String fileName, long fileSize, boolean save,
	                                     boolean useImportDir, boolean forceImportFavourites,
	                                     boolean forceImportGpx, boolean showSnackbar) {
		if (gpxFile == null || gpxFile.isPointsEmpty()) {
			if (forceImportFavourites) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					OnClickListener importAsTrackListener = (dialog, which) -> {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								handleResult(gpxFile, fileName, fileSize, save, useImportDir, showSnackbar);
								openFavorites();
								break;
							case DialogInterface.BUTTON_NEGATIVE:
								dialog.dismiss();
								break;
						}
					};
					new AlertDialog.Builder(activity)
							.setTitle(R.string.import_track)
							.setMessage(activity.getString(R.string.import_track_desc, fileName))
							.setPositiveButton(R.string.shared_string_import, importAsTrackListener)
							.setNegativeButton(R.string.shared_string_cancel, importAsTrackListener)
							.show();
				}
			} else {
				handleResult(gpxFile, fileName, fileSize, save, useImportDir, showSnackbar);
			}
			return;
		}

		if (forceImportFavourites) {
			importFavoritesImpl(gpxFile, fileName, true);
		} else if (fileName != null) {
			if (forceImportGpx || !Algorithms.isEmpty(gpxFile.getTracks())) {
				handleResult(gpxFile, fileName, fileSize, save, useImportDir, showSnackbar);
			} else {
				ImportGpxBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager(),
						this, gpxFile, fileName, fileSize, save, useImportDir, showSnackbar);
			}
		}
	}

	private void openFavorites() {
		Intent intent = new Intent(activity, app.getAppCustomization().getMyPlacesActivity());
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(TAB_ID, GPX_TAB);
		activity.startActivity(intent);
	}

	private void removeResultListener(@NonNull Activity activity){
		if (activity instanceof OsmandActionBarActivity actionBarActivity) {
			actionBarActivity.removeActivityResultListener(saveFileResultListener);
		}
	}

	public void registerResultListener(@NonNull OsmandApplication app, @NonNull Activity activity, @NonNull File file) {
		removeResultListener(activity);

		saveFileResultListener = new ActivityResultListener(REQUEST_CODE_CREATE_FILE, (resultCode, resultData) -> {
			if (resultCode == Activity.RESULT_OK && resultData != null && resultData.getData() != null) {
				app.getImportHelper().handleCopyFileToFile(file, resultData.getData());
			}
		});
		if (activity instanceof OsmandActionBarActivity actionBarActivity) {
			actionBarActivity.registerActivityResultListener(saveFileResultListener);
		}
	}

	@Nullable
	public ActivityResultListener getSaveFileResultListener() {
		return saveFileResultListener;
	}

	private <P> void executeImportTask(AsyncTask<P, ?, ?> importTask, P... requests) {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {

				@Override
				public void onFinish(@NonNull AppInitializer init) {
					if (importTask.getStatus() == Status.PENDING) {
						OsmAndTaskManager.executeTask(importTask, requests);
					}
				}
			});
		} else {
			OsmAndTaskManager.executeTask(importTask, requests);
		}
	}
}