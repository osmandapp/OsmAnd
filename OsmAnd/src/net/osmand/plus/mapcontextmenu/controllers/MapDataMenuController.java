package net.osmand.plus.mapcontextmenu.controllers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

public class MapDataMenuController extends MenuController {
	private WorldRegion region;
	private IndexItem indexItem;
	private List<IndexItem> otherIndexItems;
	private String name;

	private DownloadValidationManager downloadValidationManager;
	private DownloadIndexesThread downloadThread;

	public MapDataMenuController(final OsmandApplication app, final MapActivity mapActivity, PointDescription pointDescription, final BinaryMapDataObject dataObject) {
		super(new MenuBuilder(app), pointDescription, mapActivity);
		OsmandRegions osmandRegions = app.getRegions();
		String fullName = osmandRegions.getFullName(dataObject);
		this.region = osmandRegions.getRegionData(fullName);
		name = getPointDescription().getName();
		downloadValidationManager = new DownloadValidationManager(app);
		downloadThread = app.getDownloadThread();

		mapActivity.getSupportFragmentManager();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				if (indexItem != null) {
					downloadValidationManager.startDownload(mapActivity, indexItem);
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
				// todo other maps
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
	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY;
	}

	@Override
	public boolean needTypeStr() {
		return true;
	}

	@Override
	public Drawable getLeftIcon() {
		return getIcon(R.drawable.ic_map, R.color.osmand_orange_dark, R.color.osmand_orange);
	}

	@Override
	public String getNameStr() {
		return name;
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
			otherIndexItems = downloadThread.getIndexes().getIndexItems(region.getRegionDownloadNameLC());
			for (IndexItem i : otherIndexItems) {
				if (i.getType() == DownloadActivityType.NORMAL_FILE) {
					indexItem = i;
					otherIndexItems.remove(i);
					break;
				}
			}
		}

		topRightTitleButtonController.visible = otherIndexItems.size() > 0;
		if (indexItem != null) {
			if (indexItem.isOutdated()) {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_update)
						+ " (" + indexItem.getSizeDescription(getMapActivity()) + ")";
			} else {
				leftTitleButtonController.caption = getMapActivity().getString(R.string.shared_string_download)
						+ " (" + indexItem.getSizeDescription(getMapActivity()) + ")";
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
