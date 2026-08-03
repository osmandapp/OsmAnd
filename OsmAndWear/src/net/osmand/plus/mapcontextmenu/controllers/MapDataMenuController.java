package net.osmand.plus.mapcontextmenu.controllers;

import static net.osmand.IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.TIF_EXT;
import static net.osmand.plus.download.DownloadActivityType.HILLSHADE_FILE;
import static net.osmand.plus.download.DownloadActivityType.SLOPE_FILE;
import static net.osmand.plus.download.DownloadActivityType.SRTM_COUNTRY_FILE;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.liveupdates.LiveUpdatesHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.MapDataMenuBuilder;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.DownloadedRegionsLayer.DownloadMapObject;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapDataMenuController extends MenuController {

	private DownloadMapObject mapObject;
	private List<IndexItem> otherIndexItems;
	private final LocalItem localItem;
	private List<LocalItem> otherLocalItems;

	private final boolean srtmDisabled;
	private final boolean srtmNeedsInstallation;
	private boolean backuped;

	private final DownloadIndexesThread downloadThread;

	public MapDataMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull DownloadMapObject mapObject) {
		super(new MapDataMenuBuilder(mapActivity), pointDescription, mapActivity);
		this.mapObject = mapObject;
		indexItem = mapObject.getIndexItem();
		localItem = mapObject.getLocalItem();
		OsmandApplication app = mapActivity.getMyApplication();
		downloadThread = app.getDownloadThread();
		if (indexItem != null) {
			downloaded = indexItem.isDownloaded();
			backuped = indexItem.getBackupFile(app).exists();
			otherIndexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(mapObject.getWorldRegion()));
			otherIndexItems.remove(indexItem);
		} else if (localItem != null) {
			downloaded = true;
			backuped = localItem.isBackuped(app);
			LocalIndexHelper helper = new LocalIndexHelper(app);
			otherLocalItems = helper.getLocalItems(mapObject.getWorldRegion().getRegionDownloadName());
			for (Iterator<LocalItem> iterator = otherLocalItems.iterator(); iterator.hasNext(); ) {
				LocalItem info = iterator.next();
				if (info.getPath().equals(localItem.getPath())) {
					iterator.remove();
					break;
				}
			}
		}

		srtmDisabled = !PluginsHelper.isActive(SRTMPlugin.class)
				&& !InAppPurchaseUtils.isContourLinesAvailable(app);
		OsmandPlugin srtmPlugin = PluginsHelper.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();

		leftDownloadButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (backuped) {
					restoreFromBackup();
				} else if (indexItem != null && activity != null) {
					if ((indexItem.getType() == SRTM_COUNTRY_FILE
							|| indexItem.getType() == HILLSHADE_FILE
							|| indexItem.getType() == SLOPE_FILE)
							&& srtmDisabled) {
						activity.getContextMenu().close();

						if (srtmNeedsInstallation) {
							OsmandPlugin plugin = PluginsHelper.getPlugin(SRTMPlugin.class);
							if (plugin != null) {
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL()));
								AndroidUtils.startActivityIfSafe(activity, intent);
							} else {
								Toast.makeText(activity.getApplicationContext(),
										activity.getString(R.string.activate_srtm_plugin), Toast.LENGTH_LONG).show();
							}
						} else {
							PluginsFragment.showInstance(activity.getSupportFragmentManager());
							Toast.makeText(activity, activity.getString(R.string.activate_srtm_plugin),
									Toast.LENGTH_SHORT).show();
						}
					} else if (!downloaded || indexItem.isOutdated()) {
						startDownload(mapActivity, indexItem);
					} else if (isLiveUpdatesOn()) {
						LiveUpdatesHelper.runLiveUpdate(activity, indexItem.getTargetFileName(), true, null);
					}
				}
			}
		};
		leftDownloadButtonController.caption = mapActivity.getString(R.string.shared_string_download);
		leftDownloadButtonController.startIconId = R.drawable.ic_action_import;

		rightDownloadButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity activity = getMapActivity();
				if (activity != null) {
					activity.getContextMenu().close();

					Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
					IContextMenuProvider provider = activity.getMapLayers().getDownloadedRegionsLayer();
					if (otherIndexItems != null && otherIndexItems.size() > 0) {
						for (IndexItem item : otherIndexItems) {
							selectedObjects.put(
									new DownloadMapObject(mapObject.getDataObject(), mapObject.getWorldRegion(), item, null),
									provider);
						}
					} else if (otherLocalItems != null && otherLocalItems.size() > 0) {
						for (LocalItem info : otherLocalItems) {
							selectedObjects.put(
									new DownloadMapObject(mapObject.getDataObject(), mapObject.getWorldRegion(), null, info),
									provider);
						}
					}
					activity.getContextMenu().getMultiSelectionMenu().show(
							activity.getContextMenu().getLatLon(), selectedObjects);
				}
			}
		};
		rightDownloadButtonController.caption = mapActivity.getString(R.string.download_select_map_types);
		rightDownloadButtonController.startIconId = R.drawable.ic_plugin_srtm;

		bottomTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					if (backuped) {
						deleteItem(indexItem.getBackupFile(app));
					} else {
						deleteItem(indexItem.getTargetFile(app));
					}
				} else if (localItem != null) {
					deleteItem(new File(localItem.getPath()));
				}
			}
		};
		bottomTitleButtonController.caption = mapActivity.getString(R.string.shared_string_delete);
		bottomTitleButtonController.startIconId = R.drawable.ic_action_delete_dark;

		titleProgressController = new TitleProgressController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					downloadThread.cancelDownload(indexItem);
				}
			}
		};

		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			if (mapActivity.getMyApplication().getSettings().isInternetConnectionAvailable()) {
				downloadThread.runReloadIndexFiles();
			}
		}

		updateData();
	}

	private boolean isLiveUpdatesOn() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getMyApplication().getSettings().IS_LIVE_UPDATES_ON.get();
		} else {
			return false;
		}
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public int getAdditionalInfoColorId() {
		return R.color.icon_color_default_light;
	}

	@Override
	public CharSequence getAdditionalInfoStr() {
		double mb = 0;
		if (backuped) {
			if (localItem != null) {
				mb = localItem.getSize();
			} else if (indexItem != null) {
				mb = indexItem.getArchiveSizeMB();
			}
		} else if (indexItem != null) {
			mb = indexItem.getArchiveSizeMB();
		}
		if (mb != 0) {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				return mapActivity.getString(R.string.file_size_in_mb, mb);
			}
		}
		return "";
	}

	@Override
	public int getAdditionalInfoIconRes() {
		return R.drawable.ic_sdcard_16;
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof DownloadMapObject) {
			this.mapObject = (DownloadMapObject) object;
			updateData();
		}
	}

	@Override
	protected Object getObject() {
		return mapObject;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getRightIcon() {
		int iconResId;
		if (getDownloadActivityType() != null) {
			iconResId = getDownloadActivityType().getIconResource();
		} else {
			iconResId = R.drawable.ic_map;
		}
		if (backuped) {
			return getIcon(iconResId);
		} else {
			return getIcon(iconResId, R.color.osmand_orange);
		}
	}

	@NonNull
	@Override
	public String getTypeStr() {
		String res = "";
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (mapObject.getWorldRegion().getSuperregion() != null) {
				res = mapObject.getWorldRegion().getSuperregion().getLocaleName();
			} else {
				res = mapActivity.getString(R.string.shared_string_map);
			}
			String mapType = getMapType();
			if (!Algorithms.isEmpty(mapType)) {
				res += ", " + mapType;
			}
		}
		return res;
	}

	@NonNull
	private String getMapType() {
		String type = "";
		MapActivity mapActivity = getMapActivity();
		DownloadActivityType downloadActivityType = getDownloadActivityType();
		if (downloadActivityType != null && mapActivity != null) {
			type = downloadActivityType.getString(mapActivity);
			if (downloadActivityType == SRTM_COUNTRY_FILE) {
				Object object = indexItem != null ? indexItem : localItem;
				if (object != null) {
					type += " " + SrtmDownloadItem.getAbbreviationInScopes(mapActivity, object);
				}
			}
		}
		return type;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		if (indexItem != null) {
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, getMapType(), false, false, null);
			StringBuilder sizeStr = new StringBuilder();
			sizeStr.append(indexItem.getSizeDescription(mapActivity));
			if (backuped) {
				sizeStr.append(" — ").append(mapActivity.getString(R.string.local_indexes_cat_backup));
			}
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, sizeStr.toString(), false, false, null);
		} else if (localItem != null) {
			if (getDownloadActivityType() != null) {
				addPlainMenuItem(R.drawable.ic_action_info_dark, null, getMapType(), false, false, null);
			}
			StringBuilder sizeStr = new StringBuilder();
			if (localItem.getSize() >= 0) {
				sizeStr.append(AndroidUtils.formatSize(mapActivity, localItem.getSize() * 1024L));
			}
			if (backuped) {
				if (sizeStr.length() > 0) {
					sizeStr.append(" — ").append(mapActivity.getString(R.string.local_indexes_cat_backup));
				} else {
					sizeStr.append(mapActivity.getString(R.string.local_indexes_cat_backup));
				}
			}
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, sizeStr.toString(), false, false, null);
		}
		if (!Algorithms.isEmpty(mapObject.getWorldRegion().getParams().getWikiLink())) {
			String[] items = mapObject.getWorldRegion().getParams().getWikiLink().split(":");
			String url;
			if (items.length > 1) {
				url = "https://" + items[0] + ".wikipedia.org/wiki/" + items[1].replace(' ', '_');
			} else {
				url = "https://wikipedia.org/wiki/" + items[0].replace(' ', '_');
			}
			addPlainMenuItem(R.drawable.ic_world_globe_dark, null, url, false, true, null);
		}
		if (!Algorithms.isEmpty(mapObject.getWorldRegion().getParams().getPopulation())) {
			String population = mapObject.getWorldRegion().getParams().getPopulation();
			StringBuilder b = new StringBuilder();
			int k = 0;
			for (int i = population.length() - 1; i >= 0; i--) {
				if (k == 3) {
					b.insert(0, " ");
					k = 0;
				}
				b.insert(0, population.charAt(i));
				k++;
			}
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, mapActivity.getResources().getString(R.string.poi_population)
					+ ": " + b, false, false, null);
		}
		if (indexItem != null) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(mapActivity);
			addPlainMenuItem(R.drawable.ic_action_data, null, indexItem.getRemoteDate(dateFormat), false, false, null);
		} else if (localItem != null) {
			addPlainMenuItem(R.drawable.ic_action_data, null, localItem.getDescription(mapActivity.getMyApplication()), false, false, null);
		}
	}

	@Override
	public boolean supportZoomIn() {
		return false;
	}

	@Override
	public boolean navigateButtonVisible() {
		return false;
	}

	@Override
	public boolean buttonsVisible() {
		return false;
	}

	@Override
	public void updateData() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		if (indexItem == null) {
			otherIndexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(mapObject.getWorldRegion()));
			Iterator<IndexItem> it = otherIndexItems.iterator();
			while (it.hasNext()) {
				IndexItem i = it.next();
				if (i.getType() == DownloadActivityType.NORMAL_FILE) {
					indexItem = i;
					it.remove();
					break;
				}
			}
		}

		if (indexItem != null) {
			downloaded = indexItem.isDownloaded();
		}

		leftDownloadButtonController.visible = true;
		leftDownloadButtonController.startIconId = R.drawable.ic_action_import;
		if (backuped) {
			leftDownloadButtonController.caption = mapActivity.getString(R.string.local_index_mi_restore);
		} else if (indexItem != null) {
			if ((indexItem.getType() == SRTM_COUNTRY_FILE
					|| indexItem.getType() == HILLSHADE_FILE
					|| indexItem.getType() == SLOPE_FILE)
					&& srtmDisabled) {
				leftDownloadButtonController.caption = mapActivity.getString(R.string.shared_string_get);
				leftDownloadButtonController.clearIcon(true);
			} else if (indexItem.isOutdated()) {
				leftDownloadButtonController.caption = mapActivity.getString(R.string.shared_string_update);
			} else if (!downloaded) {
				leftDownloadButtonController.caption = mapActivity.getString(R.string.shared_string_download);
			} else if (isLiveUpdatesOn()) {
				leftDownloadButtonController.caption = mapActivity.getString(R.string.live_update);
			} else {
				leftDownloadButtonController.visible = false;
			}
		} else {
			leftDownloadButtonController.visible = false;
		}

		bottomTitleButtonController.visible = downloaded;
		rightDownloadButtonController.visible = (otherIndexItems != null && otherIndexItems.size() > 0)
				|| (otherLocalItems != null && otherLocalItems.size() > 0);

		boolean internetConnectionAvailable =
				mapActivity.getMyApplication().getSettings().isInternetConnectionAvailable();

		boolean isDownloading = indexItem != null && downloadThread.isDownloading(indexItem);
		if (isDownloading) {
			titleProgressController.setMapDownloadMode();
			if (downloadThread.getCurrentDownloadingItem() == indexItem) {
				titleProgressController.indeterminate = false;
				titleProgressController.progress = downloadThread.getCurrentDownloadProgress();
			} else {
				titleProgressController.indeterminate = true;
				titleProgressController.progress = 0;
			}
			double mb = indexItem.getArchiveSizeMB();
			String v;
			if (titleProgressController.progress != -1) {
				v = mapActivity.getString(R.string.value_downloaded_of_max, mb * titleProgressController.progress / 100, mb);
			} else {
				v = mapActivity.getString(R.string.file_size_in_mb, mb);
			}
			if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
				titleProgressController.caption = indexItem.getType().getString(mapActivity) + " • " + v;
			} else {
				titleProgressController.caption = v;
			}
			titleProgressController.visible = true;
		} else if (downloadThread.shouldDownloadIndexes()) {
			titleProgressController.setIndexesDownloadMode(mapActivity);
			titleProgressController.visible = true;
		} else if (!internetConnectionAvailable) {
			titleProgressController.setNoInternetConnectionMode(mapActivity);
			titleProgressController.visible = true;
		} else {
			titleProgressController.visible = false;
		}
	}

	private void deleteItem(File file) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			if (file.exists()) {
				AlertDialog.Builder confirm = new AlertDialog.Builder(getMapActivity());
				confirm.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						new DeleteFileTask(activity, file).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
					}
				});
				confirm.setNegativeButton(R.string.shared_string_no, null);
				String fn;
				if (indexItem != null) {
					fn = FileNameTranslationHelper.getFileName(getMapActivity(), app.getRegions(),
							indexItem.getVisibleName(getMapActivity(), app.getRegions()));
				} else {
					fn = getPointDescription().getName();
				}
				confirm.setMessage(mapActivity.getString(R.string.delete_confirmation_msg, fn));
				confirm.show();
			}
		}
	}

	private void restoreFromBackup() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RestoreFromBackupTask restoreFromBackupTask = null;
			if (localItem != null) {
				restoreFromBackupTask = new RestoreFromBackupTask(mapActivity, localItem);
			} else if (indexItem != null) {
				restoreFromBackupTask = new RestoreFromBackupTask(mapActivity, indexItem);
			}
			if (restoreFromBackupTask != null) {
				restoreFromBackupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
			}
		}
	}

	private DownloadActivityType getDownloadActivityType() {
		if (indexItem != null) {
			return indexItem.getType();
		} else if (localItem != null) {
			if (localItem.getType() == LocalItemType.MAP_DATA) {
				return DownloadActivityType.NORMAL_FILE;
			} else if (localItem.getType() == LocalItemType.ROAD_DATA) {
				return DownloadActivityType.ROADS_FILE;
			} else if (localItem.getType() == LocalItemType.TERRAIN_DATA) {
				if (SrtmDownloadItem.isSrtmFile(localItem.getFileName())) {
					return SRTM_COUNTRY_FILE;
				} else if (localItem.getFileName().endsWith(TIF_EXT)) {
					return DownloadActivityType.GEOTIFF_FILE;
				}
			} else if (localItem.getType() == LocalItemType.WIKI_AND_TRAVEL_MAPS) {
				if (localItem.getFileName().endsWith(BINARY_WIKI_MAP_INDEX_EXT)) {
					return DownloadActivityType.WIKIPEDIA_FILE;
				} else {
					return DownloadActivityType.WIKIVOYAGE_FILE;
				}
			} else if (localItem.getType() == LocalItemType.TTS_VOICE_DATA
					|| localItem.getType() == LocalItemType.VOICE_DATA) {
				return DownloadActivityType.VOICE_FILE;
			} else if (localItem.getType() == LocalItemType.FONT_DATA) {
				return DownloadActivityType.FONT_FILE;
			} else if (localItem.getType() == LocalItemType.DEPTH_DATA) {
				return DownloadActivityType.DEPTH_MAP_FILE;
			}
		}
		return null;
	}

	private static class DeleteFileTask extends AsyncTask<Void, Void, Void> {

		private final File file;
		private final WeakReference<MapActivity> mapActivityRef;
		private final OsmandApplication app;

		DeleteFileTask(@NonNull MapActivity mapActivity, @NonNull File file) {
			this.file = file;
			this.mapActivityRef = new WeakReference<>(mapActivity);
			this.app = mapActivity.getMyApplication();
		}

		@Override
		protected void onPreExecute() {
			MapActivity mapActivity = mapActivityRef.get();
			if (mapActivity != null) {
				mapActivity.getContextMenu().close();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			boolean successfull = Algorithms.removeAllFiles(file.getAbsoluteFile());
			if (successfull) {
				app.getResourceManager().closeFile(file.getName());
			}
			app.getDownloadThread().updateLoadedFiles();
			return null;
		}

		protected void onPostExecute(Void result) {
			MapActivity mapActivity = mapActivityRef.get();
			if (mapActivity != null) {
				mapActivity.refreshMap();
			}
		}

	}

	private static class RestoreFromBackupTask extends AsyncTask<Void, Void, Void> {

		private final WeakReference<MapActivity> mapActivityRef;
		private final OsmandApplication app;

		private LocalItem localItem;
		private IndexItem indexItem;

		RestoreFromBackupTask(@NonNull MapActivity mapActivity, @NonNull LocalItem LocalItem) {
			this.mapActivityRef = new WeakReference<>(mapActivity);
			this.app = mapActivity.getMyApplication();
			this.localItem = LocalItem;
		}

		RestoreFromBackupTask(@NonNull MapActivity mapActivity, @NonNull IndexItem indexItem) {
			this.mapActivityRef = new WeakReference<>(mapActivity);
			this.app = mapActivity.getMyApplication();
			this.indexItem = indexItem;
		}

		@Override
		protected void onPreExecute() {
			MapActivity mapActivity = mapActivityRef.get();
			if (mapActivity != null) {
				mapActivity.getContextMenu().close();
			}
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (localItem != null) {
				LocalItem info = localItem;
				if (move(new File(info.getPath()), getFileToRestore(info))) {
					app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
					app.getDownloadThread().updateLoadedFiles();
				}
			} else if (indexItem != null) {
				if (move(indexItem.getBackupFile(app), indexItem.getTargetFile(app))) {
					app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<>());
					app.getDownloadThread().updateLoadedFiles();
				}
			}
			return null;
		}

		protected void onPostExecute(Void result) {
			MapActivity mapActivity = mapActivityRef.get();
			if (mapActivity != null) {
				mapActivity.refreshMap();
			}
		}

		@NonNull
		private File getFileToRestore(@NonNull LocalItem item) {
			String fileName = item.getFileName();
			if (item.isBackuped(app)) {
				File parent = new File(item.getPath()).getParentFile();
				if (item.getType() == LocalItemType.MAP_DATA) {
					parent = app.getAppPath(IndexConstants.MAPS_PATH);
				} else if (item.getType() == LocalItemType.ROAD_DATA) {
					parent = app.getAppPath(IndexConstants.ROADS_INDEX_DIR);
				} else if (item.getType() == LocalItemType.TILES_DATA) {
					 if (fileName.endsWith(IndexConstants.TIF_EXT)) {
						parent = app.getAppPath(IndexConstants.GEOTIFF_DIR);
					} else {
						parent = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
					}
				} else if (item.getType() == LocalItemType.TERRAIN_DATA) {
					if (SrtmDownloadItem.isSrtmFile(localItem.getFileName())) {
						parent = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
					} else if (localItem.getFileName().endsWith(TIF_EXT)) {
						parent = app.getAppPath(IndexConstants.GEOTIFF_DIR);
					}
				} else if (item.getType() == LocalItemType.WIKI_AND_TRAVEL_MAPS) {
					if (fileName.endsWith(BINARY_WIKI_MAP_INDEX_EXT)) {
						parent = app.getAppPath(IndexConstants.WIKI_INDEX_DIR);
					} else {
						parent = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
					}
				} else if (item.getType() == LocalItemType.TTS_VOICE_DATA) {
					parent = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
				} else if (item.getType() == LocalItemType.VOICE_DATA) {
					parent = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
				} else if (item.getType() == LocalItemType.DEPTH_DATA) {
					parent = app.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR);
				}
				return new File(parent, fileName);
			}
			return new File(item.getPath());
		}

		private boolean move(File from, File to) {
			if (!to.getParentFile().exists()) {
				if (!to.getParentFile().mkdirs()) {
					return false;
				}
			}
			return from.renameTo(to);
		}
	}
}
