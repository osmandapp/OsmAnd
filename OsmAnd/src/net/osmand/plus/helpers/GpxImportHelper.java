package net.osmand.plus.helpers;

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
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.IndexConstants;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.myplaces.FavoritesActivity;
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
import java.util.zip.ZipInputStream;

/**
 * @author Koen Rabaey
 */
public class GpxImportHelper {

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

	public GpxImportHelper(final AppCompatActivity activity, final OsmandApplication app, final OsmandMapTileView mapView) {
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
		if (!isOsmandSubdir && name != null) {
			if (name.endsWith(GPX_SUFFIX)) {
				handleGpxImport(contentUri, name, true, useImportDir);
				return true;
			} else if (name.endsWith(KML_SUFFIX)) {
				handleKmlImport(contentUri, name, true, useImportDir);
				return true;
			} else if (name.endsWith(KMZ_SUFFIX)) {
				handleKmzImport(contentUri, name, true, useImportDir);
				return true;
			}
		}
		return false;
	}

	public void handleFavouritesImport(Uri uri) {
		String scheme = uri.getScheme();
		boolean isFileIntent = "file".equals(scheme);
		boolean isContentIntent = "content".equals(scheme);
		boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(uri.getPath()));
		final boolean saveFile = !isFileIntent || !isOsmandSubdir;
		String fileName = "";
		if (isFileIntent) {
			fileName = new File(uri.getPath()).getName();
		} else if (isContentIntent) {
			fileName = getNameFromContentUri(uri);
		}
		handleFavouritesImport(uri, fileName, saveFile, false, true);
	}

	public void handleFileImport(final Uri intentUri, final String fileName, final boolean useImportDir) {
		final boolean isFileIntent = "file".equals(intentUri.getScheme());
		final boolean isOsmandSubdir = isSubDirectory(app.getAppPath(IndexConstants.GPX_INDEX_DIR), new File(intentUri.getPath()));

		final boolean saveFile = !isFileIntent || !isOsmandSubdir;

		if (fileName != null && fileName.endsWith(KML_SUFFIX)) {
			handleKmlImport(intentUri, fileName, saveFile, useImportDir);
		} else if (fileName != null && fileName.endsWith(KMZ_SUFFIX)) {
			handleKmzImport(intentUri, fileName, saveFile, useImportDir);
		} else {
			handleFavouritesImport(intentUri, fileName, saveFile, useImportDir, false);
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
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				handleResult(result, fileName, save, useImportDir, false);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

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
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				importFavourites(result, fileName, save, useImportDir, forceImportFavourites);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void importFavoritesImpl(final GPXFile gpxFile, final String fileName, final boolean forceImportFavourites) {
		new AsyncTask<Void, Void, GPXFile>() {
			ProgressDialog progress = null;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, app.getString(R.string.loading_smth, ""), app.getString(R.string.loading_data));
			}

			@Override
			protected GPXFile doInBackground(Void... nothing) {
				final List<FavouritePoint> favourites = asFavourites(gpxFile.getPoints(), fileName, forceImportFavourites);
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
				Toast.makeText(activity, R.string.fav_imported_sucessfully, Toast.LENGTH_LONG).show();
				final Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
				newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				newIntent.putExtra(FavoritesActivity.OPEN_FAVOURITES_TAB, true);
				activity.startActivity(newIntent);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

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
								return GPXUtilities.loadGPXFile(app, new ByteArrayInputStream(result.getBytes("UTF-8")));
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
				if (isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
				handleResult(result, name, save, useImportDir, false);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
			if (result.warning != null) {
				Toast.makeText(activity, result.warning, Toast.LENGTH_LONG).show();
				if (gpxImportCompleteListener != null) {
					gpxImportCompleteListener.onComplete(false);
				}
			} else {
				if (save) {
					new SaveAsyncTask(result, name, useImportDir).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
		if (forceImportFavourites) {
			final Intent newIntent = new Intent(activity, app.getAppCustomization().getFavoritesActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(FavoritesActivity.OPEN_MY_PLACES_TAB, true);
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
		} else {
			ImportGpxBottomSheetDialogFragment fragment = new ImportGpxBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.setGpxImportHelper(this);
			fragment.setGpxFile(gpxFile);
			fragment.setFileName(fileName);
			fragment.setSave(save);
			fragment.setUseImportDir(useImportDir);
			fragment.show(activity.getSupportFragmentManager(), ImportGpxBottomSheetDialogFragment.TAG);
		}
	}

	private List<FavouritePoint> asFavourites(final List<GPXUtilities.WptPt> wptPts, String fileName, boolean forceImportFavourites) {
		final List<FavouritePoint> favourites = new ArrayList<>();
		for (GPXUtilities.WptPt p : wptPts) {
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

		private GpxImportHelper gpxImportHelper;

		private GPXFile gpxFile;
		private String fileName;
		private boolean save;
		private boolean useImportDir;

		public void setGpxImportHelper(GpxImportHelper gpxImportHelper) {
			this.gpxImportHelper = gpxImportHelper;
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

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

			final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_import_gpx_bottom_sheet_dialog, container);

			if (nightMode) {
				((TextView) mainView.findViewById(R.id.import_gpx_title)).setTextColor(ContextCompat.getColor(getActivity(), R.color.ctx_menu_info_text_dark));
			}

			((ImageView) mainView.findViewById(R.id.import_as_favorites_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_fav_dark));
			((ImageView) mainView.findViewById(R.id.import_as_gpx_icon)).setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));

			int nameColor = ContextCompat.getColor(getContext(), nightMode ? R.color.osmand_orange : R.color.dashboard_blue);
			int descrColor = ContextCompat.getColor(getContext(), nightMode ? R.color.dashboard_subheader_text_dark : R.color.dashboard_subheader_text_light);
			String descr = getString(R.string.import_gpx_file_description);
			SpannableStringBuilder text = new SpannableStringBuilder(fileName).append(" ").append(descr);
			text.setSpan(new ForegroundColorSpan(nameColor), 0, fileName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			text.setSpan(new ForegroundColorSpan(descrColor), fileName.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			((TextView) mainView.findViewById(R.id.import_gpx_description)).setText(text);

			mainView.findViewById(R.id.import_as_favorites_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					gpxImportHelper.importFavoritesImpl(gpxFile, fileName, false);
					dismiss();
				}
			});
			mainView.findViewById(R.id.import_as_gpx_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					gpxImportHelper.handleResult(gpxFile, fileName, save, useImportDir, false);
					dismiss();
				}
			});

			mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					dismiss();
				}
			});

			setupHeightAndBackground(mainView, R.id.import_gpx_scroll_view);

			return mainView;
		}
	}
}
