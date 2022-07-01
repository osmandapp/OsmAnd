package net.osmand.plus.importfiles;

import static android.app.Activity.RESULT_OK;
import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_CHART_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.IndexConstants.WPT_CHART_FILE_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.FavouritePoint.PICKUP_DATE;
import static net.osmand.plus.mapmarkers.ItineraryDataHelper.CREATION_DATE;
import static net.osmand.plus.myplaces.ui.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.ui.FavoritesActivity.TAB_ID;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.REPLACE_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.SETTINGS_TYPE_LIST_KEY;
import static net.osmand.plus.settings.backend.backup.SettingsHelper.SILENT_IMPORT_KEY;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.importfiles.ui.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.importfiles.ui.ImportTracksFragment;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Koen Rabaey
 */
public class ImportHelper {

	public static final Log log = PlatformUtil.getLog(ImportHelper.class);

	public static final String KML_SUFFIX = ".kml";
	public static final String KMZ_SUFFIX = ".kmz";

	public static final int IMPORT_FILE_REQUEST = 1006;

	private final OsmandApplication app;
	private final AppCompatActivity activity;

	private OnGpxImportCompleteListener gpxImportCompleteListener;

	public enum ImportType {
		SETTINGS(OSMAND_SETTINGS_FILE_EXT),
		ROUTING(ROUTING_FILE_EXT),
		RENDERING(RENDERER_INDEX_EXT),
		GPX(GPX_FILE_EXT),
		KML(KML_SUFFIX),
		KMZ(KMZ_SUFFIX);

		private final String extension;

		ImportType(String extension) {
			this.extension = extension;
		}

		public String getExtension() {
			return extension;
		}
	}

	public enum OnSuccessfulGpxImport {
		OPEN_GPX_CONTEXT_MENU,
		OPEN_PLAN_ROUTE_FRAGMENT
	}

	public interface OnGpxImportCompleteListener {
		void onImportComplete(boolean success);

		void onSaveComplete(boolean success, GPXFile result);
	}

	public ImportHelper(AppCompatActivity activity, OsmandApplication app) {
		this.activity = activity;
		this.app = app;
	}

	public void setGpxImportCompleteListener(OnGpxImportCompleteListener gpxImportCompleteListener) {
		this.gpxImportCompleteListener = gpxImportCompleteListener;
	}

	public OnGpxImportCompleteListener getGpxImportCompleteListener() {
		return gpxImportCompleteListener;
	}

	public void handleContentImport(final Uri contentUri, Bundle extras, final boolean useImportDir) {
		String name = getNameFromContentUri(app, contentUri);
		handleFileImport(contentUri, name, extras, useImportDir);
	}

	public void importFavoritesFromGpx(final GPXFile gpxFile, final String fileName) {
		importFavoritesImpl(gpxFile, fileName, false);
	}

	public void handleGpxImport(GPXFile result, String name, long fileSize, boolean save, boolean useImportDir) {
		handleResult(result, name, fileSize, save, useImportDir, false);
	}

	public boolean handleGpxImport(final Uri contentUri, OnSuccessfulGpxImport onGpxImport, final boolean useImportDir) {
		String name = getNameFromContentUri(app, contentUri);
		boolean isOsmandSubDir = Algorithms.isSubDirectory(app.getAppPath(GPX_INDEX_DIR), new File(contentUri.getPath()));
		if (!isOsmandSubDir && name != null) {
			String nameLC = name.toLowerCase();
			if (nameLC.endsWith(GPX_FILE_EXT)) {
				name = name.substring(0, name.length() - GPX_FILE_EXT.length()) + GPX_FILE_EXT;
				handleGpxImport(contentUri, name, onGpxImport, useImportDir, true);
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

	public void handleFavouritesImport(@NonNull Uri uri) {
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
			handleUriImport(intentUri, saveFile, useImportDir);
		} else if (fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName.endsWith(KMZ_SUFFIX)) {
			handleKmzImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName.endsWith(BINARY_MAP_INDEX_EXT) || fileName.endsWith(BINARY_MAP_INDEX_EXT + ZIP_EXT)) {
			handleObfImport(intentUri, fileName);
		} else if (fileName.endsWith(SQLITE_EXT)) {
			handleSqliteTileImport(intentUri, fileName);
		} else if (fileName.endsWith(OSMAND_SETTINGS_FILE_EXT) || fileName.endsWith(OSMAND_SETTINGS_FILE_EXT + ZIP_EXT)) {
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

	@Nullable
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

	private void handleGpxImport(@NonNull Uri gpxFile, @NonNull String fileName, @Nullable OnSuccessfulGpxImport onGpxImport,
	                             boolean useImportDir, boolean save) {
		executeImportTask(new GpxImportTask(this, activity, gpxFile, fileName, onGpxImport, useImportDir, save));
	}

	protected void handleGpxOrFavouritesImport(Uri fileUri, String fileName, boolean save, boolean useImportDir,
	                                           boolean forceImportFavourites, boolean forceImportGpx) {
		executeImportTask(new GpxOrFavouritesImportTask(this, activity, fileUri, fileName, save, useImportDir, forceImportFavourites, forceImportGpx));
	}

	private void importFavoritesImpl(GPXFile gpxFile, String fileName, boolean forceImportFavourites) {
		executeImportTask(new FavoritesImportTask(activity, gpxFile, fileName, forceImportFavourites));
	}

	protected void handleKmzImport(Uri kmzFile, String name, boolean save, boolean useImportDir) {
		executeImportTask(new KmzImportTask(this, activity, kmzFile, name, save, useImportDir));
	}

	private void handleKmlImport(Uri kmlFile, String name, boolean save, boolean useImportDir) {
		executeImportTask(new KmlImportTask(this, activity, kmlFile, name, save, useImportDir));
	}

	protected void handleObfImport(Uri obfFile, String name) {
		executeImportTask(new ObfImportTask(activity, obfFile, name));
	}

	protected void handleSqliteTileImport(Uri uri, String name) {
		executeImportTask(new SqliteTileImportTask(activity, uri, name));
	}

	private void handleOsmAndSettingsImport(Uri intentUri, String fileName, Bundle extras, CallbackWithObject<List<SettingsItem>> callback) {
		fileName = fileName.replace(IndexConstants.ZIP_EXT, "");
		if (extras != null
				&& extras.containsKey(SettingsHelper.SETTINGS_VERSION_KEY)
				&& extras.containsKey(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY)) {
			int version = extras.getInt(SettingsHelper.SETTINGS_VERSION_KEY, -1);
			String latestChanges = extras.getString(SettingsHelper.SETTINGS_LATEST_CHANGES_KEY);
			boolean replace = extras.getBoolean(REPLACE_KEY);
			boolean silentImport = extras.getBoolean(SILENT_IMPORT_KEY);
			ArrayList<String> settingsTypeKeys = extras.getStringArrayList(SETTINGS_TYPE_LIST_KEY);
			List<ExportSettingsType> settingsTypes = null;
			if (settingsTypeKeys != null) {
				settingsTypes = new ArrayList<>();
				for (String key : settingsTypeKeys) {
					settingsTypes.add(ExportSettingsType.valueOf(key));
				}
			}
			handleOsmAndSettingsImport(intentUri, fileName, settingsTypes, replace, silentImport, latestChanges, version, callback);
		} else {
			handleOsmAndSettingsImport(intentUri, fileName, null, false, false, null, -1,
					callback);
		}
	}

	protected void handleOsmAndSettingsImport(Uri uri, String name, final List<ExportSettingsType> settingsTypes,
	                                          final boolean replace, boolean silentImport, String latestChanges, int version,
	                                          CallbackWithObject<List<SettingsItem>> callback) {
		executeImportTask(new SettingsImportTask(activity, uri, name, settingsTypes, replace, silentImport,
				latestChanges, version, callback));
	}

	protected void handleXmlFileImport(Uri intentUri, String fileName, CallbackWithObject routingCallback) {
		executeImportTask(new XmlImportTask(activity, intentUri, fileName, routingCallback));
	}

	private void handleUriImport(Uri uri, boolean save, boolean useImportDir) {
		executeImportTask(new UriImportTask(this, activity, uri, save, useImportDir));
	}

	protected void handleZipImport(Uri uri, boolean save, boolean useImportDir) {
		executeImportTask(new ZipImportTask(this, activity, uri, save, useImportDir));
	}

	@Nullable
	public static String copyFile(@NonNull OsmandApplication app, @NonNull File dest, @NonNull Uri uri, boolean overwrite, boolean unzip) {
		if (dest.exists() && !overwrite) {
			return app.getString(R.string.file_with_name_already_exists);
		}
		String error = null;
		InputStream in = null;
		OutputStream out = null;
		ZipInputStream zis = null;
		try {
			in = app.getContentResolver().openInputStream(uri);
			if (in != null) {
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

	public void chooseFileToImport(@NonNull ImportType importType, @Nullable CallbackWithObject<?> callback) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			try {
				Intent intent = ImportHelper.getImportTrackIntent();
				ActivityResultListener listener = getImportFileResultListener(importType, callback);
				mapActivity.startActivityForResult(intent, IMPORT_FILE_REQUEST);
				mapActivity.registerActivityResultListener(listener);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(mapActivity, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
			}
		}
	}

	private ActivityResultListener getImportFileResultListener(@NonNull ImportType importType, @Nullable CallbackWithObject callback) {
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
						handleOsmAndSettingsImport(data, fileName, resultData.getExtras(), callback);
					} else if (importType.equals(ImportType.ROUTING)) {
						handleXmlFileImport(data, fileName, callback);
					}
				} else {
					app.showToastMessage(app.getString(R.string.not_support_file_type_with_ext,
							importType.getExtension().replaceAll("\\.", "").toUpperCase()));
				}
			}
		});
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

	protected void handleResult(GPXFile result, String name, long fileSize, boolean save,
	                            boolean useImportDir, boolean forceImportFavourites) {
		handleResult(result, name, OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU, fileSize, save, useImportDir,
				forceImportFavourites);
	}

	protected void handleResult(final GPXFile result, final String name, OnSuccessfulGpxImport onGpxImport,
	                            long fileSize, final boolean save, final boolean useImportDir,
	                            boolean forceImportFavourites) {
		if (result != null) {
			if (result.error != null) {
				app.showToastMessage(result.error.getMessage());
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onImportComplete(false);
				}
			} else {
				if (save) {
					int tracksCount = result.getTracksCount();
					boolean showImportMultiTrackFragment = tracksCount > 1 && tracksCount < 50;
					if (showImportMultiTrackFragment) {
						ImportTracksFragment.showInstance(activity.getSupportFragmentManager(), result, name, fileSize);
					} else {
						importAsOneTrack(result, name, fileSize, useImportDir, onGpxImport);
					}
				} else {
					showNeededScreen(onGpxImport, result);
				}
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onImportComplete(true);
				}
			}
		} else if (AndroidUtils.isActivityNotDestroyed(activity)) {
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
							AndroidUtils.startActivityIfSafe(app, intent);
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
			Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(TAB_ID, GPX_TAB);
			activity.startActivity(newIntent);
		}
	}

	private void importAsOneTrack(@NonNull GPXFile gpxFile, @NonNull String name, long fileSize,
	                              boolean useImportDir, @Nullable OnSuccessfulGpxImport onSuccessfulGpxImport) {
		String existingFilePath = getExistingFilePath(app, name, fileSize);
		if (existingFilePath != null) {
			app.showToastMessage(R.string.file_already_imported);
			showNeededScreen(onSuccessfulGpxImport, gpxFile);
		} else {
			File destinationDir = useImportDir
					? app.getAppPath(GPX_IMPORT_DIR)
					: app.getAppPath(GPX_INDEX_DIR);
			SaveImportedGpxListener listener = new SaveImportedGpxListener() {

				@Override
				public void onGpxSavingStarted() {
				}

				@Override
				public void onGpxSavingFinished(@NonNull List<String> warnings) {
					boolean success = Algorithms.isEmpty(warnings);
					if (success) {
						SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
						if (selectedGpxFile != null) {
							selectedGpxFile.setGpxFile(gpxFile, app);
						}
						showNeededScreen(onSuccessfulGpxImport, gpxFile);
					} else {
						app.showToastMessage(warnings.get(0));
					}
					if (gpxImportCompleteListener != null) {
						gpxImportCompleteListener.onSaveComplete(success, gpxFile);
					}
				}
			};
			executeImportTask(new SaveGpxAsyncTask(app, gpxFile, destinationDir, name, listener));
		}
	}

	@Nullable
	public static String getExistingFilePath(@NonNull OsmandApplication app, @NonNull String name, long fileSize) {
		File dir = app.getAppPath(GPX_INDEX_DIR);
		List<GPXInfo> gpxInfoList = GpxUiHelper.getSortedGPXFilesInfoByDate(dir, true);
		for (GPXInfo gpxInfo : gpxInfoList) {
			String fileName = gpxInfo.getFileName();
			String nameWithoutDirs = Algorithms.getFileWithoutDirs(fileName);
			if (nameWithoutDirs.equals(name) && gpxInfo.getFileSize() == fileSize) {
				return fileName;
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

	protected void showNeededScreen(OnSuccessfulGpxImport onGpxImport, @NonNull GPXFile gpxFile) {
		if (onGpxImport == OnSuccessfulGpxImport.OPEN_GPX_CONTEXT_MENU) {
			showGpxContextMenu(gpxFile.path);
		} else if (onGpxImport == OnSuccessfulGpxImport.OPEN_PLAN_ROUTE_FRAGMENT) {
			showPlanRouteFragment(gpxFile);
		}
	}

	private void showGpxContextMenu(String gpxFilePath) {
		if (!Algorithms.isEmpty(gpxFilePath)) {
			TrackMenuFragment.openTrack(activity, new File(gpxFilePath), null);
		}
	}

	private void showPlanRouteFragment(@NonNull GPXFile gpxFile) {
		GpxData gpxData = new GpxData(gpxFile);
		MeasurementEditingContext editingContext = new MeasurementEditingContext(app);
		editingContext.setGpxData(gpxData);

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		int mode = MeasurementToolFragment.PLAN_ROUTE_MODE;
		MeasurementToolFragment.showInstance(fragmentManager, editingContext, mode, false);
	}

	protected void importGpxOrFavourites(final GPXFile gpxFile, final String fileName, final long fileSize,
	                                     final boolean save, final boolean useImportDir,
	                                     final boolean forceImportFavourites, final boolean forceImportGpx) {
		if (gpxFile == null || gpxFile.isPointsEmpty()) {
			if (forceImportFavourites) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					OnClickListener importAsTrackListener = new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
								case DialogInterface.BUTTON_POSITIVE:
									handleResult(gpxFile, fileName, fileSize, save, useImportDir, true);
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
				}
			} else {
				handleResult(gpxFile, fileName, fileSize, save, useImportDir, false);
			}
			return;
		}

		if (forceImportFavourites) {
			importFavoritesImpl(gpxFile, fileName, true);
		} else if (fileName != null) {
			if (forceImportGpx || !Algorithms.isEmpty(gpxFile.tracks)) {
				handleResult(gpxFile, fileName, fileSize, save, useImportDir, false);
			} else {
				ImportGpxBottomSheetDialogFragment.showInstance(activity.getSupportFragmentManager(),
						this, gpxFile, fileName, fileSize, save, useImportDir);
			}
		}
	}

	public static List<FavouritePoint> asFavourites(OsmandApplication app, List<WptPt> wptPts, String fileName, boolean forceImportFavourites) {
		List<FavouritePoint> favourites = new ArrayList<>();
		for (WptPt p : wptPts) {
			if (Algorithms.isEmpty(p.name)) {
				p.name = app.getString(R.string.shared_string_waypoint);
			}
			if (!Algorithms.isEmpty(p.name)) {
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
				FavouritePoint point = new FavouritePoint(p.lat, p.lon, p.name, fpCat, p.ele, p.time);
				if (p.desc != null) {
					point.setDescription(p.desc);
				}
				Map<String, String> extensions = p.getExtensionsToRead();
				point.setAddress(extensions.get("address"));
				point.setColor(p.getColor(0));
				point.setVisible(!Boolean.parseBoolean(extensions.get("hidden")));
				String iconName = p.getIconName();
				if (iconName != null) {
					point.setIconIdFromName(iconName);
				}
				point.setBackgroundType(BackgroundType.getByTypeName(p.getBackgroundType(), DEFAULT_BACKGROUND_TYPE));

				String time = extensions.get(PICKUP_DATE);
				if (time == null) {
					time = extensions.get(CREATION_DATE);
				}
				if (!Algorithms.isEmpty(time)) {
					point.setPickupDate(GPXUtilities.parseTime(time));
				}
				favourites.add(point);
			}
		}
		return favourites;
	}

	@SuppressWarnings("unchecked")
	private <P> void executeImportTask(final AsyncTask<P, ?, ?> importTask, final P... requests) {
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onStart(AppInitializer init) {

				}

				@Override
				public void onProgress(AppInitializer init, InitEvents event) {
				}

				@Override
				public void onFinish(AppInitializer init) {
					if (importTask.getStatus() == Status.PENDING) {
						importTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
					}
				}
			});
		} else {
			importTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requests);
		}
	}
}