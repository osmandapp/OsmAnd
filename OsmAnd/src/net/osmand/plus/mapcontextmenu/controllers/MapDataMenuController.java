package net.osmand.plus.mapcontextmenu.controllers;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.DownloadedRegionsLayer.DownloadMapObject;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MapDataMenuController extends MenuController {
	private DownloadMapObject mapObject;
	private IndexItem indexItem;
	private List<IndexItem> otherIndexItems;
	private boolean srtmDisabled;
	private boolean srtmNeedsInstallation;

	private DownloadIndexesThread downloadThread;

	public MapDataMenuController(OsmandApplication app, final MapActivity mapActivity, PointDescription pointDescription, final DownloadMapObject mapObject) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		this.mapObject = mapObject;
		indexItem = mapObject.getIndexItem();
		downloadThread = app.getDownloadThread();
		if (indexItem != null) {
			otherIndexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(mapObject.getWorldRegion()));
			otherIndexItems.remove(indexItem);
		}

		srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
		OsmandPlugin srtmPlugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
		srtmNeedsInstallation = srtmPlugin == null || srtmPlugin.needsInstallation();

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
							|| indexItem.getType() == DownloadActivityType.HILLSHADE_FILE)
							&& srtmDisabled) {
						getMapActivity().getContextMenu().close();

						if (srtmNeedsInstallation) {
							OsmandPlugin plugin = OsmandPlugin.getPlugin(SRTMPlugin.class);
							if (plugin != null) {
								mapActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(plugin.getInstallURL())));
							}
						} else {
							mapActivity.startActivity(new Intent(mapActivity, mapActivity.getMyApplication().getAppCustomization()
									.getPluginsActivity()));
							AccessibleToast.makeText(mapActivity, mapActivity.getString(R.string.activate_srtm_plugin),
									Toast.LENGTH_SHORT).show();
						}
					} else {
						new DownloadValidationManager(getMapActivity().getMyApplication())
								.startDownload(getMapActivity(), indexItem);
					}
				}
			}
		};
		leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_download);
		leftTitleButtonController.leftIconId = R.drawable.ic_action_import;

		rightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					deleteItem();
				}
			}
		};
		rightTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_delete);
		rightTitleButtonController.leftIconId = R.drawable.ic_action_delete_dark;

		topRightTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				getMapActivity().getContextMenu().close();

				Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
				IContextMenuProvider provider = mapActivity.getMapLayers().getDownloadedRegionsLayer();
				for (IndexItem item : otherIndexItems) {
					selectedObjects.put(
							new DownloadMapObject(mapObject.getDataObject(), mapObject.getWorldRegion(), item),
							provider);
				}
				mapActivity.getContextMenu().getMultiSelectionMenu().show(
						mapActivity.getContextMenu().getLatLon(), selectedObjects);
			}
		};
		topRightTitleButtonController.caption = getMapActivity().getString(R.string.download_select_map_types);

		titleProgressController = new TitleProgressController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					downloadThread.cancelDownload(indexItem);
				}
			}
		};

		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			downloadThread.runReloadIndexFiles();
		}

		updateData();
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof DownloadMapObject) {
			this.mapObject = (DownloadMapObject) object;
			updateData();
		}
	}

	@Override
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		int iconResId;
		if (indexItem != null) {
			iconResId = indexItem.getType().getIconResource();
		} else {
			iconResId = R.drawable.ic_map;
		}
		return getIcon(iconResId, R.color.osmand_orange);
	}

	@Override
	public String getTypeStr() {
		String res;
		if (mapObject.getWorldRegion().getSuperregion() != null) {
			res = mapObject.getWorldRegion().getSuperregion().getLocaleName();
		} else {
			res = getMapActivity().getString(R.string.shared_string_map);
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
			addPlainMenuItem(R.drawable.ic_action_info_dark, indexItem.getType().getString(getMapActivity()), false, false);
			addPlainMenuItem(R.drawable.ic_action_info_dark, indexItem.getSizeDescription(getMapActivity()), false, false);
		}
		if (!Algorithms.isEmpty(mapObject.getWorldRegion().getParams().getWikiLink())) {
			String[] items = mapObject.getWorldRegion().getParams().getWikiLink().split(":");
			String url;
			if (items.length > 1) {
				url = "https://" + items[0] + ".wikipedia.org/wiki/" + items[1].replace(' ', '_');
			} else {
				url = "https://wikipedia.org/wiki/" + items[0].replace(' ', '_');
			}
			addPlainMenuItem(R.drawable.ic_world_globe_dark, url, false, true);
		}
		if (indexItem != null) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getMapActivity());
			addPlainMenuItem(R.drawable.ic_action_data, indexItem.getRemoteDate(dateFormat), false, false);
		}
	}

	@Override
	public boolean fabVisible() {
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

		leftTitleButtonController.leftIconId = R.drawable.ic_action_import;
		if (indexItem != null) {
			if ((indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
					|| indexItem.getType() == DownloadActivityType.HILLSHADE_FILE)
					&& srtmDisabled) {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.get_plugin);
				leftTitleButtonController.leftIconId = 0;
			} else if (indexItem.isOutdated()) {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_update);
			} else {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_download);
			}
		}

		rightTitleButtonController.visible = indexItem != null && indexItem.isDownloaded();
		topRightTitleButtonController.visible = otherIndexItems.size() > 0;

		boolean downloadIndexes = !downloadThread.getIndexes().isDownloadedFromInternet
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
			String v ;
			if (titleProgressController.progress != -1) {
				v = getMapActivity().getString(R.string.value_downloaded_of_max, mb * titleProgressController.progress / 100, mb);
			} else {
				v = getMapActivity().getString(R.string.file_size_in_mb, mb);
			}
			if(indexItem.getType() == DownloadActivityType.ROADS_FILE) {
				titleProgressController.caption = indexItem.getType().getString(getMapActivity()) + " • " + v;
			} else {
				titleProgressController.caption = v;
			}
			titleProgressController.visible = true;
		} else if (downloadIndexes) {
			titleProgressController.setIndexesDownloadMode();
			titleProgressController.visible = true;
		} else {
			titleProgressController.visible = false;
		}
	}

	public void deleteItem() {
		final OsmandApplication app = getMapActivity().getMyApplication();
		final File fl = indexItem.getTargetFile(app);
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
							getMapActivity().getMapLayers().getDownloadedRegionsLayer().updateObjects();
							getMapActivity().refreshMap();
						}

					}.execute((Void) null);
				}
			});
			confirm.setNegativeButton(R.string.shared_string_no, null);
			String fn = FileNameTranslationHelper.getFileName(getMapActivity(), app.getRegions(),
					indexItem.getVisibleName(getMapActivity(), app.getRegions()));
			confirm.setMessage(getMapActivity().getString(R.string.delete_confirmation_msg, fn));
			confirm.show();
		}
	}
}
