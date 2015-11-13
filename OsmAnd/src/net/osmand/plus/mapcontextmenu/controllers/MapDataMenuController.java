package net.osmand.plus.mapcontextmenu.controllers;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResourceGroup;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MapDataMenuController extends MenuController {
	private WorldRegion region;
	private IndexItem indexItem;
	private List<IndexItem> otherIndexItems;

	private DownloadIndexesThread downloadThread;

	public MapDataMenuController(OsmandApplication app, MapActivity mapActivity, PointDescription pointDescription, BinaryMapDataObject dataObject) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		initData(app, dataObject);
		downloadThread = app.getDownloadThread();

		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					new DownloadValidationManager(getMapActivity().getMyApplication())
							.startDownload(getMapActivity(), indexItem);
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

				DownloadResourceGroup group = downloadThread.getIndexes().getRegionGroup(region);
				if (group != null) {
					final Intent intent = new Intent(getMapActivity(), getMapActivity().getMyApplication()
							.getAppCustomization().getDownloadIndexActivity());
					intent.putExtra(DownloadActivity.FILTER_GROUP, group.getUniqueId());
					intent.putExtra(DownloadActivity.TAB_TO_OPEN, DownloadActivity.DOWNLOAD_TAB);
					getMapActivity().startActivity(intent);
				}
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

	private void initData(OsmandApplication app, BinaryMapDataObject dataObject) {
		OsmandRegions osmandRegions = app.getRegions();
		String fullName = osmandRegions.getFullName(dataObject);
		this.region = osmandRegions.getRegionData(fullName);
	}

	@Override
	protected void setObject(Object object) {
		if (object instanceof BinaryMapDataObject) {
			initData(getMapActivity().getMyApplication(), (BinaryMapDataObject) object);
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
		return getIcon(R.drawable.ic_map, R.color.osmand_orange);
	}

	@Override
	public String getTypeStr() {
		String res;
		if (region != null && region.getSuperregion() != null) {
			res = region.getSuperregion().getLocaleName();
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
			addPlainMenuItem(R.drawable.ic_action_info_dark, indexItem.getSizeDescription(getMapActivity()), false);
		}
		if (region != null && !Algorithms.isEmpty(region.getParams().getWikiLink())) {
			String[] items = region.getParams().getWikiLink().split(":");
			String url;
			if (items.length > 1) {
				url = "https://" + items[0] + ".wikipedia.org/wiki/" + items[1].replace(' ', '_');
			} else {
				url = "https://wikipedia.org/wiki/" + items[0].replace(' ', '_');
			}
			addPlainMenuItem(R.drawable.ic_world_globe_dark, url, true);
		}
		if (indexItem != null) {
			DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(getMapActivity());
			addPlainMenuItem(R.drawable.ic_action_data, indexItem.getRemoteDate(dateFormat), false);
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
			otherIndexItems = new LinkedList<>(downloadThread.getIndexes().getIndexItems(region));
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

		topRightTitleButtonController.visible = otherIndexItems.size() > 0;
		if (indexItem != null) {
			if (indexItem.isOutdated()) {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_update);
			} else {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_download);
			}
		}
		rightTitleButtonController.visible = indexItem != null && indexItem.isDownloaded();

		boolean hasIndexes = downloadThread.getIndexes().isDownloadedFromInternet;
		boolean isDownloading = indexItem != null && downloadThread.isDownloading(indexItem);
		if (!hasIndexes) {
			titleProgressController.setIndexesDownloadMode();
			titleProgressController.visible = true;
		} else if (isDownloading) {
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
				v = getMapActivity().getString(R.string.value_downloaded_from_max, mb * titleProgressController.progress / 100, mb);
			} else {
				v = getMapActivity().getString(R.string.file_size_in_mb, mb);
			}
			if(indexItem.getType() == DownloadActivityType.ROADS_FILE) {
				titleProgressController.caption = indexItem.getType().getString(getMapActivity()) + " • " + v;
			} else {
				titleProgressController.caption = v;
			}
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
