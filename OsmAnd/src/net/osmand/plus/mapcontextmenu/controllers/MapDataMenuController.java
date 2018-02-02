package net.osmand.plus.mapcontextmenu.controllers;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.liveupdates.LiveUpdatesHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.DownloadedRegionsLayer.DownloadMapObject;
import net.osmand.util.Algorithms;

import java.io.File;
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
	private LocalIndexInfo localIndexInfo;
	private List<LocalIndexInfo> otherLocalIndexInfos;
	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;
	private boolean backuped;

	private DownloadIndexesThread downloadThread;

	public MapDataMenuController(final MapActivity mapActivity, PointDescription pointDescription, final DownloadMapObject mapObject) {
		super(new MenuBuilder(mapActivity), pointDescription, mapActivity);
		this.mapObject = mapObject;
		indexItem = mapObject.getIndexItem();
		localIndexInfo = mapObject.getLocalIndexInfo();
		final OsmandApplication app = mapActivity.getMyApplication();
		downloadThread = app.getDownloadThread();
		if (indexItem != null) {
			downloaded = indexItem.isDownloaded();
			backuped = indexItem.getBackupFile(getMapActivity().getMyApplication()).exists();
			otherIndexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(mapObject.getWorldRegion()));
			otherIndexItems.remove(indexItem);
		} else if (localIndexInfo != null) {
			downloaded = true;
			backuped = localIndexInfo.isBackupedData();
			LocalIndexHelper helper = new LocalIndexHelper(app);
			otherLocalIndexInfos = helper.getLocalIndexInfos(mapObject.getWorldRegion().getRegionDownloadName());
			for (Iterator<LocalIndexInfo> iterator = otherLocalIndexInfos.iterator(); iterator.hasNext(); ) {
				LocalIndexInfo info = iterator.next();
				if (info.getPathToData().equals(localIndexInfo.getPathToData())) {
					iterator.remove();
					break;
				}
			}
		}

		srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
		OsmandPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();

		leftDownloadButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (backuped) {
					restoreFromBackup();
				} else if (indexItem != null) {
					if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
							|| indexItem.getType() == DownloadActivityType.HILLSHADE_FILE)
							&& srtmDisabled) {
						getMapActivity().getContextMenu().close();

						if (srtmNeedsInstallation) {
							OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
							if (plugin != null) {
								mapActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
							} else {
								Toast.makeText(mapActivity.getApplicationContext(),
										mapActivity.getString(R.string.activate_srtm_plugin), Toast.LENGTH_LONG).show();
							}
						} else {
							mapActivity.startActivity(new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
									.getPluginsActivity()));
							Toast.makeText(mapActivity, mapActivity.getString(R.string.activate_srtm_plugin),
									Toast.LENGTH_SHORT).show();
						}
					} else if (!downloaded || indexItem.isOutdated()) {
						new DownloadValidationManager(app).startDownload(mapActivity, indexItem);
					} else if (isLiveUpdatesOn()) {
						LiveUpdatesHelper.runLiveUpdate(mapActivity, indexItem.getTargetFileName(), true);
					}
				}
			}
		};
		leftDownloadButtonController.caption = getMapActivity().getString(R.string.shared_string_download);
		leftDownloadButtonController.updateStateListDrawableIcon(R.drawable.ic_action_import, true);

		rightDownloadButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				getMapActivity().getContextMenu().close();

				Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
				IContextMenuProvider provider = mapActivity.getMapLayers().getDownloadedRegionsLayer();
				if (otherIndexItems != null && otherIndexItems.size() > 0) {
					for (IndexItem item : otherIndexItems) {
						selectedObjects.put(
								new DownloadMapObject(mapObject.getDataObject(), mapObject.getWorldRegion(), item, null),
								provider);
					}
				} else if (otherLocalIndexInfos != null && otherLocalIndexInfos.size() > 0) {
					for (LocalIndexInfo info : otherLocalIndexInfos) {
						selectedObjects.put(
								new DownloadMapObject(mapObject.getDataObject(), mapObject.getWorldRegion(), null, info),
								provider);
					}
				}
				mapActivity.getContextMenu().getMultiSelectionMenu().show(
						mapActivity.getContextMenu().getLatLon(), selectedObjects);
			}
		};
		rightDownloadButtonController.caption = getMapActivity().getString(R.string.download_select_map_types);
		rightDownloadButtonController.updateStateListDrawableIcon(R.drawable.ic_plugin_srtm, true);

		bottomTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					if (backuped) {
						deleteItem(indexItem.getBackupFile(app));
					} else {
						deleteItem(indexItem.getTargetFile(app));
					}
				} else if (localIndexInfo != null) {
					deleteItem(new File(localIndexInfo.getPathToData()));
				}
			}
		};
		bottomTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		bottomTitleButtonController.updateStateListDrawableIcon(R.drawable.ic_action_delete_dark, true);

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
		return getMapActivity().getMyApplication().getSettings().IS_LIVE_UPDATES_ON.get();
	}

	@Override
	public boolean displayDistanceDirection() {
		return true;
	}

	@Override
	public int getAdditionalInfoColorId() {
		return R.color.icon_color;
	}

	@Override
	public CharSequence getAdditionalInfoStr() {
		double mb = 0;
		if (backuped) {
			if (localIndexInfo != null) {
				mb = localIndexInfo.getSize();
			} else if (indexItem != null) {
				mb = indexItem.getArchiveSizeMB();
			}
		} else if (indexItem != null) {
			mb = indexItem.getArchiveSizeMB();
		}
		if (mb != 0) {
			return getMapActivity().getString(R.string.file_size_in_mb, mb);
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

	@Override
	public String getTypeStr() {
		String res;
		if (mapObject.getWorldRegion().getSuperregion() != null) {
			res = mapObject.getWorldRegion().getSuperregion().getLocaleName();
		} else {
			res = getMapActivity().getString(R.string.shared_string_map);
		}
		DownloadActivityType downloadActivityType = getDownloadActivityType();
		if (downloadActivityType != null) {
			res += ", " + downloadActivityType.getString(getMapActivity());
		}
		if (getMenuType() == MenuType.STANDARD) {
			res += "\n";
		}
		return res;
	}

	@Override
	public boolean needStreetName() {
		return false;
	}

	@Override
	public void addPlainMenuItems(String typeStr, PointDescription pointDescription, LatLon latLon) {
		if (indexItem != null) {
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, indexItem.getType().getString(getMapActivity()), false, false, null);
			StringBuilder sizeStr = new StringBuilder();
			sizeStr.append(indexItem.getSizeDescription(getMapActivity()));
			if (backuped) {
				sizeStr.append(" — ").append(LocalIndexType.DEACTIVATED.getHumanString(getMapActivity()));
			}
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, sizeStr.toString(), false, false, null);
		} else if (localIndexInfo != null) {
			if (getDownloadActivityType() != null) {
				addPlainMenuItem(R.drawable.ic_action_info_dark, null, getDownloadActivityType().getString(getMapActivity()), false, false, null);
			}
			StringBuilder sizeStr = new StringBuilder();
			if (localIndexInfo.getSize() >= 0) {
				if (localIndexInfo.getSize() > 100) {
					sizeStr.append(DownloadActivity.formatMb.format(new Object[]{(float) localIndexInfo.getSize() / (1 << 10)}));
				} else {
					sizeStr.append(localIndexInfo.getSize()).append(" KB");
				}
			}
			if (backuped) {
				if (sizeStr.length() > 0) {
					sizeStr.append(" — ").append(LocalIndexType.DEACTIVATED.getHumanString(getMapActivity()));
				} else {
					sizeStr.append(LocalIndexType.DEACTIVATED.getHumanString(getMapActivity()));
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
			addPlainMenuItem(R.drawable.ic_action_info_dark, null, getMapActivity().getResources().getString(R.string.poi_population)
					+ ": " + b, false, false, null);
		}
		if (indexItem != null) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getMapActivity());
			addPlainMenuItem(R.drawable.ic_action_data, null, indexItem.getRemoteDate(dateFormat), false, false, null);
		} else if (localIndexInfo != null) {
			addPlainMenuItem(R.drawable.ic_action_data, null, localIndexInfo.getDescription(), false, false, null);
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
		leftDownloadButtonController.updateStateListDrawableIcon(R.drawable.ic_action_import, true);
		if (backuped) {
			leftDownloadButtonController.caption = getMapActivity().getString(R.string.local_index_mi_restore);
		} else if (indexItem != null) {
			if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
					|| indexItem.getType() == DownloadActivityType.HILLSHADE_FILE)
					&& srtmDisabled) {
				leftDownloadButtonController.caption = getMapActivity().getString(R.string.get_plugin);
				leftDownloadButtonController.clearIcon(true);
			} else if (indexItem.isOutdated()) {
				leftDownloadButtonController.caption = getMapActivity().getString(R.string.shared_string_update);
			} else if (!downloaded) {
				leftDownloadButtonController.caption = getMapActivity().getString(R.string.shared_string_download);
			} else if (isLiveUpdatesOn()) {
				leftDownloadButtonController.caption = getMapActivity().getString(R.string.live_update);
			} else {
				leftDownloadButtonController.visible = false;
			}
		} else {
			leftDownloadButtonController.visible = false;
		}

		bottomTitleButtonController.visible = downloaded;
		rightDownloadButtonController.visible = (otherIndexItems != null && otherIndexItems.size() > 0)
				|| (otherLocalIndexInfos != null && otherLocalIndexInfos.size() > 0);

		boolean internetConnectionAvailable =
				getMapActivity().getMyApplication().getSettings().isInternetConnectionAvailable();
		boolean downloadIndexes = internetConnectionAvailable
				&& !downloadThread.getIndexes().isDownloadedFromInternet
				&& !downloadThread.getIndexes().downloadFromInternetFailed;

		boolean isDownloading = indexItem != null && downloadThread.isDownloading(indexItem);
		if (isDownloading) {
			titleProgressController.setMapDownloadMode();
			if (downloadThread.getCurrentDownloadingItem() == indexItem) {
				titleProgressController.indeterminate = false;
				titleProgressController.progress = downloadThread.getCurrentDownloadingItemProgress();
			} else {
				titleProgressController.indeterminate = true;
				titleProgressController.progress = 0;
			}
			double mb = indexItem.getArchiveSizeMB();
			String v;
			if (titleProgressController.progress != -1) {
				v = getMapActivity().getString(R.string.value_downloaded_of_max, mb * titleProgressController.progress / 100, mb);
			} else {
				v = getMapActivity().getString(R.string.file_size_in_mb, mb);
			}
			if (indexItem.getType() == DownloadActivityType.ROADS_FILE) {
				titleProgressController.caption = indexItem.getType().getString(getMapActivity()) + " • " + v;
			} else {
				titleProgressController.caption = v;
			}
			titleProgressController.visible = true;
		} else if (downloadIndexes) {
			titleProgressController.setIndexesDownloadMode();
			titleProgressController.visible = true;
		} else if (!internetConnectionAvailable) {
			titleProgressController.setNoInternetConnectionMode();
			titleProgressController.visible = true;
		} else {
			titleProgressController.visible = false;
		}
	}

	public void deleteItem(final File fl) {
		final OsmandApplication app = getMapActivity().getMyApplication();
		if (fl.exists()) {
			AlertDialog.Builder confirm = new AlertDialog.Builder(getMapActivity());
			confirm.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					new AsyncTask<Void, Void, Void>() {

						@Override
						protected void onPreExecute() {
							getMapActivity().getContextMenu().close();
						}

						@Override
						protected Void doInBackground(Void... params) {
							boolean successfull = Algorithms.removeAllFiles(fl.getAbsoluteFile());
							if (successfull) {
								app.getResourceManager().closeFile(fl.getName());
							}
							app.getDownloadThread().updateLoadedFiles();
							return null;
						}

						protected void onPostExecute(Void result) {
							getMapActivity().refreshMap();
						}

					}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
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
			confirm.setMessage(getMapActivity().getString(R.string.delete_confirmation_msg, fn));
			confirm.show();
		}
	}

	public void restoreFromBackup() {
		final OsmandApplication app = getMapActivity().getMyApplication();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				getMapActivity().getContextMenu().close();
			}

			@Override
			protected Void doInBackground(Void... params) {
				if (localIndexInfo != null) {
					LocalIndexInfo info = localIndexInfo;
					move(new File(info.getPathToData()), getFileToRestore(info));
					app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
					app.getDownloadThread().updateLoadedFiles();
				} else if (indexItem != null) {
					move(indexItem.getBackupFile(app), indexItem.getTargetFile(app));
					app.getResourceManager().reloadIndexes(IProgress.EMPTY_PROGRESS, new ArrayList<String>());
					app.getDownloadThread().updateLoadedFiles();
				}
				return null;
			}

			protected void onPostExecute(Void result) {
				getMapActivity().refreshMap();
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
	}

	private boolean move(File from, File to) {
		if (!to.getParentFile().exists()) {
			to.getParentFile().mkdirs();
		}
		return from.renameTo(to);
	}

	private File getFileToRestore(LocalIndexInfo i) {
		if (i.isBackupedData()) {
			final OsmandApplication app = getMapActivity().getMyApplication();
			File parent = new File(i.getPathToData()).getParentFile();
			if (i.getOriginalType() == LocalIndexType.MAP_DATA) {
				if (i.getFileName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
					parent = app.getAppPath(IndexConstants.ROADS_INDEX_DIR);
				} else {
					parent = app.getAppPath(IndexConstants.MAPS_PATH);
				}
			} else if (i.getOriginalType() == LocalIndexType.TILES_DATA) {
				parent = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexType.SRTM_DATA) {
				parent = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexType.WIKI_DATA) {
				parent = app.getAppPath(IndexConstants.WIKI_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexType.TTS_VOICE_DATA) {
				parent = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexType.VOICE_DATA) {
				parent = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			}
			return new File(parent, i.getFileName());
		}
		return new File(i.getPathToData());
	}

	public DownloadActivityType getDownloadActivityType() {
		if (indexItem != null) {
			return indexItem.getType();
		} else if (localIndexInfo != null) {
			if (localIndexInfo.getOriginalType() == LocalIndexType.MAP_DATA) {
				if (localIndexInfo.getFileName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
					return DownloadActivityType.ROADS_FILE;
				} else {
					return DownloadActivityType.NORMAL_FILE;
				}
			} else if (localIndexInfo.getOriginalType() == LocalIndexType.SRTM_DATA) {
				return DownloadActivityType.SRTM_COUNTRY_FILE;
			} else if (localIndexInfo.getOriginalType() == LocalIndexType.WIKI_DATA) {
				return DownloadActivityType.WIKIPEDIA_FILE;
			} else if (localIndexInfo.getOriginalType() == LocalIndexType.TTS_VOICE_DATA
					|| localIndexInfo.getOriginalType() == LocalIndexType.VOICE_DATA) {
				return DownloadActivityType.VOICE_FILE;
			} else if (localIndexInfo.getOriginalType() == LocalIndexType.FONT_DATA) {
				return DownloadActivityType.FONT_FILE;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}
