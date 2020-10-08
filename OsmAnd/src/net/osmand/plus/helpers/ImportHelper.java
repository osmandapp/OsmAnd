package net.osmand.plus.helpers;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.data.FavouritePoint.BackgroundType;
import net.osmand.map.ITileSource;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.dialogs.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.SettingsHelper;
import net.osmand.plus.settings.backend.SettingsHelper.CheckDuplicatesListener;
import net.osmand.plus.settings.backend.SettingsHelper.PluginSettingsItem;
import net.osmand.plus.settings.backend.SettingsHelper.ProfileSettingsItem;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsImportListener;
import net.osmand.plus.settings.backend.SettingsHelper.SettingsItem;
import net.osmand.plus.settings.fragments.ImportCompleteFragment;
import net.osmand.plus.settings.fragments.ImportSettingsFragment;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static android.app.Activity.RESULT_OK;
import static net.osmand.IndexConstants.WPT_CHART_FILE_EXT;
import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.RENDERER_INDEX_EXT;
import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_CHART_FILE_EXT;
import static net.osmand.IndexConstants.SQLITE_EXT;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.plus.AppInitializer.loadRoutingFiles;
import static net.osmand.plus.myplaces.FavoritesActivity.FAV_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.GPX_TAB;
import static net.osmand.plus.myplaces.FavoritesActivity.TAB_ID;
import static net.osmand.plus.settings.backend.SettingsHelper.*;

/**
 * @author Koen Rabaey
 */
public class ImportHelper {
	public final static Log log = PlatformUtil.getLog(ImportHelper.class);
	public static final String KML_SUFFIX = ".kml";
	public static final String KMZ_SUFFIX = ".kmz";

	private final AppCompatActivity activity;
	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private OnGpxImportCompleteListener gpxImportCompleteListener;

	public final static int IMPORT_FILE_REQUEST = 1006;
	
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
		final String name = getNameFromContentUri(app, contentUri);
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
		boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(contentUri.getPath()));
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
		boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(uri.getPath()));
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
		final boolean isFileIntent = "file".equals(intentUri.getScheme());
		final boolean isOsmandSubdir = Algorithms.isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(intentUri.getPath()));

		final boolean saveFile = !isFileIntent || !isOsmandSubdir;

		if (fileName == null) {
			handleGpxOrFavouritesImport(intentUri, fileName, saveFile, useImportDir, false, false);
		} else  if (fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName.endsWith(KMZ_SUFFIX)) {
			handleKmzImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			handleObfImport(intentUri, fileName);
		} else if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
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
			final String name;
			final Cursor returnCursor = app.getContentResolver().query(contentUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
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

	@SuppressLint("StaticFieldLeak")
	private void handleGpxImport(final Uri gpxFile, final String fileName, final boolean save, final boolean useImportDir,
	                             final boolean showInDetailsActivity) {
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
					is = app.getContentResolver().openInputStream(gpxFile);
					if (is != null) {
						return GPXUtilities.loadGPXFile(is);
					}
				} catch (FileNotFoundException e) {
					//
				} catch (SecurityException e) {
					log.error(e.getMessage(), e);
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
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				handleResult(result, fileName, save, useImportDir, false, showInDetailsActivity);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	private void handleGpxOrFavouritesImport(final Uri fileUri, final String fileName, final boolean save,
											 final boolean useImportDir, final boolean forceImportFavourites,
											 final boolean forceImportGpx) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... nothing) {
				InputStream is = null;
				ZipInputStream zis = null;
				try {
					is = app.getContentResolver().openInputStream(fileUri);
					if (is != null) {
						if (fileName != null && fileName.endsWith(KML_SUFFIX)) {
							final String result = Kml2Gpx.toGpx(is);
							if (result != null) {
								try {
									return GPXUtilities.loadGPXFile(new ByteArrayInputStream(result.getBytes("UTF-8")));
								} catch (UnsupportedEncodingException e) {
									return null;
								}
							}
						} else if (fileName != null && fileName.endsWith(KMZ_SUFFIX)) {
							try {
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
							} catch (Exception e) {
								return null;
							}
						} else {
							return GPXUtilities.loadGPXFile(is);
						}
					}
				} catch (FileNotFoundException e) {
					//
				} catch (SecurityException e) {
					log.error(e.getMessage(), e);
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException ignore) {
					}
					if (zis != null) try {
						zis.close();
					} catch (IOException ignore) {
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(final GPXFile result) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				importGpxOrFavourites(result, fileName, save, useImportDir, forceImportFavourites, forceImportGpx);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	private void importFavoritesImpl(final GPXFile gpxFile, final String fileName, final boolean forceImportFavourites) {
		final AsyncTask<Void, Void, GPXFile> favoritesImportTask = new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""),
							app.getString(R.string.loading_data));
				}
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
				if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				Toast.makeText(activity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG).show();
				final Intent newIntent = new Intent(activity,
						app.getAppCustomization().getFavoritesActivity());
				newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				newIntent.putExtra(TAB_ID, FAV_TAB);
				activity.startActivity(newIntent);
			}
		};
		executeImportTask(favoritesImportTask);
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
					is = app.getContentResolver().openInputStream(kmzFile);
					if (is != null) {
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
					log.error(e.getMessage(), e);
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
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
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
					is = app.getContentResolver().openInputStream(kmlFile);
					if (is != null) {
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
				} catch (SecurityException e) {
					log.error(e.getMessage(), e);
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
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
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
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
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
		}  finally {
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
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
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
						} else if (importType.equals(ImportType.ROUTING)){
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

	private void handleOsmAndSettingsImport(Uri intentUri, String fileName, Bundle extras, CallbackWithObject<List<SettingsItem>> callback) {
		if (extras != null && extras.containsKey(SETTINGS_VERSION_KEY)
				&& extras.containsKey(SETTINGS_LATEST_CHANGES_KEY)) {
			int version = extras.getInt(SETTINGS_VERSION_KEY, -1);
			String latestChanges = extras.getString(SETTINGS_LATEST_CHANGES_KEY);
			boolean replace = extras.getBoolean(REPLACE_KEY);
			ArrayList<String> settingsTypeKeys = extras.getStringArrayList(SETTINGS_TYPE_LIST_KEY);
			List<ExportSettingsType> settingsTypes = new ArrayList<>();
			if (settingsTypeKeys != null) {
				for (String key : settingsTypeKeys) {
					settingsTypes.add(ExportSettingsType.valueOf(key));
				}
			}
			handleOsmAndSettingsImport(intentUri, fileName, settingsTypes, replace, latestChanges, version, callback);
		} else {
			handleOsmAndSettingsImport(intentUri, fileName, null, false, null, -1, callback);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void handleOsmAndSettingsImport(final Uri uri, final String name,
	                                        final List<ExportSettingsType> settingsTypes,
	                                        final boolean replace,
	                                        final String latestChanges, final int version,
	                                        final CallbackWithObject<List<SettingsItem>> callback) {
		final AsyncTask<Void, Void, String> settingsImportTask = new AsyncTask<Void, Void, String>() {

			ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
				}
			}

			@Override
			protected String doInBackground(Void... voids) {
				File tempDir = FileUtils.getTempDir(app);
				File dest = new File(tempDir, name);
				return copyFile(app, dest, uri, true);
			}

			@Override
			protected void onPostExecute(String error) {
				File tempDir = FileUtils.getTempDir(app);
				final File file = new File(tempDir, name);
				if (error == null && file.exists()) {
					final SettingsHelper settingsHelper = app.getSettingsHelper();
					settingsHelper.collectSettings(file, latestChanges, version, new SettingsCollectListener() {
						@Override
						public void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
							if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
								progress.dismiss();
							}
							if (succeed) {
								List<SettingsItem> pluginIndependentItems = new ArrayList<>();
								List<PluginSettingsItem> pluginSettingsItems = new ArrayList<>();
								for (SettingsItem item : items) {
									if (item instanceof PluginSettingsItem) {
										pluginSettingsItems.add((PluginSettingsItem) item);
									} else if (Algorithms.isEmpty(item.getPluginId())) {
										pluginIndependentItems.add(item);
									}
								}
								for (PluginSettingsItem pluginItem : pluginSettingsItems) {
									handlePluginImport(pluginItem, file);
								}
								if (!pluginIndependentItems.isEmpty()) {
									if (settingsTypes == null) {
										FragmentManager fragmentManager = activity.getSupportFragmentManager();
										ImportSettingsFragment.showInstance(fragmentManager, pluginIndependentItems, file);
									} else {
										Map<ExportSettingsType, List<?>> allSettingsList = getSettingsToOperate(pluginIndependentItems, false);
										List<SettingsItem> settingsList = settingsHelper.getFilteredSettingsItems(allSettingsList, settingsTypes);
										settingsHelper.checkDuplicates(file, settingsList, settingsList, getDuplicatesListener(file, replace));
									}
								}
							} else if (empty) {
								app.showShortToastMessage(app.getString(R.string.file_import_error, name, app.getString(R.string.shared_string_unexpected_error)));
							}
						}
					});
				} else {
					if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
						progress.dismiss();
					}
					app.showShortToastMessage(app.getString(R.string.file_import_error, name, error));
				}
			}
		};
		executeImportTask(settingsImportTask);
	}

	private CheckDuplicatesListener getDuplicatesListener(final File file, final boolean replace) {
		return new CheckDuplicatesListener() {
			@Override
			public void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items) {
				if (replace) {
					for (SettingsItem item : items) {
						item.setShouldReplace(true);
					}
				}
				app.getSettingsHelper().importSettings(file, items, "", 1, getImportListener(file));
			}
		};
	}

	private SettingsImportListener getImportListener(final File file) {
		return new SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null && succeed) {
					FragmentManager fm = mapActivity.getSupportFragmentManager();
					app.getRendererRegistry().updateExternalRenderers();
					AppInitializer.loadRoutingFiles(app, null);
					if (file != null) {
						ImportCompleteFragment.showInstance(fm, items, file.getName());
					}
				}
			}
		};
	}

	public static Map<ExportSettingsType, List<?>> getSettingsToOperate(List<SettingsItem> settingsItems, boolean importComplete) {
		Map<ExportSettingsType, List<?>> settingsToOperate = new HashMap<>();
		List<ApplicationMode.ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<AvoidSpecificRoads.AvoidRoadInfo> avoidRoads = new ArrayList<>();
		for (SettingsItem item : settingsItems) {
			switch (item.getType()) {
				case PROFILE:
					profiles.add(((ProfileSettingsItem) item).getModeBean());
					break;
				case FILE:
					FileSettingsItem fileItem = (FileSettingsItem) item;
					if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.RENDERING_STYLE) {
						renderFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.ROUTING_CONFIG) {
						routingFilesList.add(fileItem.getFile());
					}
					break;
				case QUICK_ACTIONS:
					QuickActionsSettingsItem quickActionsItem = (QuickActionsSettingsItem) item;
					if (importComplete) {
						quickActions.addAll(quickActionsItem.getAppliedItems());
					} else {
						quickActions.addAll(quickActionsItem.getItems());
					}
					break;
				case POI_UI_FILTERS:
					PoiUiFiltersSettingsItem poiUiFilterItem = (PoiUiFiltersSettingsItem) item;
					if (importComplete) {
						poiUIFilters.addAll(poiUiFilterItem.getAppliedItems());
					} else {
						poiUIFilters.addAll(poiUiFilterItem.getItems());
					}
					break;
				case MAP_SOURCES:
					MapSourcesSettingsItem mapSourcesItem = (MapSourcesSettingsItem) item;
					if (importComplete) {
						tileSourceTemplates.addAll(mapSourcesItem.getAppliedItems());
					} else {
						tileSourceTemplates.addAll(mapSourcesItem.getItems());
					}
					break;
				case AVOID_ROADS:
					AvoidRoadsSettingsItem avoidRoadsItem = (AvoidRoadsSettingsItem) item;
					if (importComplete) {
						avoidRoads.addAll(avoidRoadsItem.getAppliedItems());
					} else {
						avoidRoads.addAll(avoidRoadsItem.getItems());
					}
					break;
				default:
					break;
			}
		}

		if (!profiles.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.PROFILE, profiles);
		}
		if (!quickActions.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.QUICK_ACTIONS, quickActions);
		}
		if (!poiUIFilters.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.POI_TYPES, poiUIFilters);
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.MAP_SOURCES, tileSourceTemplates);
		}
		if (!renderFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.CUSTOM_RENDER_STYLE, renderFilesList);
		}
		if (!routingFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.CUSTOM_ROUTING, routingFilesList);
		}
		if (!avoidRoads.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.AVOID_ROADS, avoidRoads);
		}
		return settingsToOperate;
	}

	private void handlePluginImport(final PluginSettingsItem pluginItem, final File file) {
		final ProgressDialog progress = new ProgressDialog(activity);
		progress.setTitle(app.getString(R.string.loading_smth, ""));
		progress.setMessage(app.getString(R.string.importing_from, pluginItem.getPublicName(app)));
		progress.setIndeterminate(true);
		progress.setCancelable(false);

		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.show();
		}

		final SettingsImportListener importListener = new SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				CustomOsmandPlugin plugin = pluginItem.getPlugin();
				plugin.loadResources();

				for (SettingsItem item : items) {
					if (item instanceof ProfileSettingsItem) {
						((ProfileSettingsItem) item).applyAdditionalPrefs();
					}
				}
				if (!Algorithms.isEmpty(plugin.getDownloadMaps())) {
					app.getDownloadThread().runReloadIndexFilesSilent();
				}
				if (!Algorithms.isEmpty(plugin.getRendererNames())) {
					app.getRendererRegistry().updateExternalRenderers();
				}
				if (!Algorithms.isEmpty(plugin.getRouterNames())) {
					loadRoutingFiles(app, null);
				}
				if (activity != null) {
					plugin.onInstall(app, activity);
				}
				String pluginId = pluginItem.getPluginId();
				File pluginDir = new File(app.getAppPath(null), IndexConstants.PLUGINS_DIR + pluginId);
				app.getSettingsHelper().exportSettings(pluginDir, "items", null, items, false);
			}
		};
		List<SettingsItem> pluginItems = new ArrayList<>(pluginItem.getPluginDependentItems());
		pluginItems.add(0, pluginItem);
		app.getSettingsHelper().checkDuplicates(file, pluginItems, pluginItems, new CheckDuplicatesListener() {
			@Override
			public void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items) {
				for (SettingsItem item : items) {
					item.setShouldReplace(true);
				}
				app.getSettingsHelper().importSettings(file, items, "", 1, importListener);
			}
		});
	}

	@SuppressLint("StaticFieldLeak")
	private void handleXmlFileImport(final Uri intentUri, final String fileName,
	                                 final CallbackWithObject routingCallback) {
		final AsyncTask<Void, Void, String> renderingImportTask = new AsyncTask<Void, Void, String>() {

			private String destFileName;
			private ImportType importType;
			private ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
				}
				destFileName = fileName;
			}

			@Override
			protected String doInBackground(Void... voids) {
				checkImportType();
				if (importType != null) {
					File dest = getDestinationFile();
					if (dest != null) {
						return copyFile(app, dest, intentUri, true);
					}
				}
				return app.getString(R.string.file_import_error, destFileName, app.getString(R.string.unsupported_type_error));
			}

			@Override
			protected void onPostExecute(String error) {
				File destDir = getDestinationDir();
				File file = new File(destDir, destFileName);
				if (error == null && file.exists()) {
					if (importType == ImportType.RENDERING) {
						app.getRendererRegistry().updateExternalRenderers();
						app.showShortToastMessage(app.getString(R.string.file_imported_successfully, destFileName));
						hideProgress();
					} else if (importType == ImportType.ROUTING) {
						loadRoutingFiles(app, new AppInitializer.LoadRoutingFilesCallback() {
							@Override
							public void onRoutingFilesLoaded() {
								hideProgress();
								RoutingConfiguration.Builder builder = app.getCustomRoutingConfig(destFileName);
								if (builder != null) {
									if (routingCallback != null) {
										routingCallback.processResult(builder);
									}
									app.showShortToastMessage(app.getString(R.string.file_imported_successfully, destFileName));
								} else {
									app.showToastMessage(app.getString(R.string.file_does_not_contain_routing_rules, destFileName));
								}
							}
						});
					}
				} else {
					hideProgress();
					app.showShortToastMessage(app.getString(R.string.file_import_error, destFileName, error));
				}
			}

			private void hideProgress() {
				if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
			}

			private File getDestinationDir() {
				if (importType == ImportType.ROUTING) {
					return app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
				} else if (importType == ImportType.RENDERING) {
					return app.getAppPath(IndexConstants.RENDERERS_DIR);
				}
				return null;
			}

			private File getDestinationFile() {
				File destDir = getDestinationDir();
				if (destDir != null) {
					if (!destDir.exists()) {
						destDir.mkdirs();
					}
					if (importType == ImportType.RENDERING && !destFileName.endsWith(RENDERER_INDEX_EXT)) {
						String fileName = Algorithms.getFileNameWithoutExtension(destFileName);
						destFileName = fileName + RENDERER_INDEX_EXT;
					}
					File destFile = new File(destDir, destFileName);
					while (destFile.exists()) {
						destFileName = AndroidUtils.createNewFileName(destFileName);
						destFile = new File(destDir, destFileName);
					}
					return destFile;
				}
				return null;
			}

			private void checkImportType() {
				InputStream is = null;
				try {
					is = app.getContentResolver().openInputStream(intentUri);
					if (is != null) {
						XmlPullParser parser = PlatformUtil.newXMLPullParser();
						parser.setInput(is, "UTF-8");
						int tok;
						while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
							if (tok == XmlPullParser.START_TAG) {
								String name = parser.getName();
								if ("osmand_routing_config".equals(name)) {
									importType = ImportType.ROUTING;
								} else if ("renderingStyle".equals(name)) {
									importType = ImportType.RENDERING;
								}
								break;
							}
						}
						try {
							is.close();
						} catch (IOException e) {
							log.error(e);
						}
					}
				} catch (FileNotFoundException | XmlPullParserException e) {
					log.error(e);
				} catch (IOException e) {
					log.error(e);
				} catch (SecurityException e) {
					log.error(e.getMessage(), e);
				} finally {
					if (is != null) try {
						is.close();
					} catch (IOException e) {
						log.error(e);
					}
				}
			}
		};
		executeImportTask(renderingImportTask);
	}

	private void handleResult(GPXFile result, String name, boolean save,
	                          boolean useImportDir, boolean forceImportFavourites) {
		handleResult(result, name, save, useImportDir, forceImportFavourites, true);
	}

	private void handleResult(final GPXFile result, final String name, final boolean save,
	                          final boolean useImportDir, boolean forceImportFavourites, boolean showInDetailsActivity) {
		if (result != null) {
			if (result.error != null) {
				Toast.makeText(activity, result.error.getMessage(), Toast.LENGTH_LONG).show();
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onImportComplete(false);
				}
			} else {
				if (save) {
					new SaveAsyncTask(result, name, useImportDir, showInDetailsActivity)
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else if (showInDetailsActivity) {
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

	private void importGpxOrFavourites(final GPXFile gpxFile, final String fileName, final boolean save,
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
				return;
			} else {
				handleResult(gpxFile, fileName, save, useImportDir, false);
				return;
			}
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