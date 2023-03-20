package net.osmand.plus.download.ui;

import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.core.android.MapRendererContext;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.LocalIndexHelper;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.download.ui.LocalIndexesFragment.LocalIndexesAdapter;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import java.io.File;

public class LocalIndexOperationTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, String> {
	protected static int DELETE_OPERATION = 1;
	protected static int BACKUP_OPERATION = 2;
	protected static int RESTORE_OPERATION = 3;
	protected static int CLEAR_TILES_OPERATION = 4;

	private final int operation;
	private final DownloadActivity a;
	private final LocalIndexesAdapter listAdapter;

	public LocalIndexOperationTask(DownloadActivity a, LocalIndexesAdapter listAdapter, int operation) {
		this.a = a;
		this.listAdapter = listAdapter;
		this.operation = operation;
	}

	private boolean move(File from, File to) {
		File parent = to.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		return from.renameTo(to);
	}

	private File getFileToBackup(LocalIndexInfo i) {
		if (!i.isBackupedData()) {
			return new File(getMyApplication().getAppPath(IndexConstants.BACKUP_INDEX_DIR), i.getFileName());
		}
		return new File(i.getPathToData());
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) a.getApplication();
	}


	private File getFileToRestore(LocalIndexInfo i) {
		String fileName = i.getFileName();
		if (i.isBackupedData()) {
			File parent = new File(i.getPathToData()).getParentFile();
			if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.MAP_DATA) {
				if (fileName.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
					parent = getMyApplication().getAppPath(IndexConstants.ROADS_INDEX_DIR);
				} else {
					parent = getMyApplication().getAppPath(IndexConstants.MAPS_PATH);
				}
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.TILES_DATA) {
				if (fileName.endsWith(IndexConstants.HEIGHTMAP_SQLITE_EXT)) {
					parent = getMyApplication().getAppPath(IndexConstants.HEIGHTMAP_INDEX_DIR);
				} else if (fileName.endsWith(IndexConstants.TIF_EXT)) {
					parent = getMyApplication().getAppPath(IndexConstants.GEOTIFF_DIR);
				} else {
					parent = getMyApplication().getAppPath(IndexConstants.TILES_INDEX_DIR);
				}
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.SRTM_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.SRTM_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.WIKI_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.WIKI_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.TRAVEL_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.TTS_VOICE_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.VOICE_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.FONT_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.FONT_INDEX_DIR);
			} else if (i.getOriginalType() == LocalIndexHelper.LocalIndexType.DEPTH_DATA) {
				parent = getMyApplication().getAppPath(IndexConstants.NAUTICAL_INDEX_DIR);
			}
			return new File(parent, fileName);
		}
		return new File(i.getPathToData());
	}

	@Override
	protected String doInBackground(LocalIndexInfo... params) {
		int count = 0;
		int total = 0;
		for (LocalIndexInfo info : params) {
			if (!isCancelled()) {
				boolean successfull = false;
				if (operation == DELETE_OPERATION) {
					File f = new File(info.getPathToData());
					successfull = Algorithms.removeAllFiles(f);

					if (InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication())) {
						String fileNameWithoutExtension =
								Algorithms.getFileNameWithoutExtension(f);
						IncrementalChangesManager changesManager =
								getMyApplication().getResourceManager().getChangesManager();
						changesManager.deleteUpdates(fileNameWithoutExtension);
					}
					if (successfull) {
						getMyApplication().getResourceManager().closeFile(info.getFileName());
						File tShm = new File(f.getParentFile(), f.getName() + "-shm");
						File tWal = new File(f.getParentFile(), f.getName() + "-wal");
						if (tShm.exists()) {
							Algorithms.removeAllFiles(tShm);
						}
						if (tWal.exists()) {
							Algorithms.removeAllFiles(tWal);
						}
						clearMapillaryTiles(info);
						clearHeightmapTiles(info);
					}
				} else if (operation == RESTORE_OPERATION) {
					successfull = move(new File(info.getPathToData()), getFileToRestore(info));
					if (successfull) {
						info.setBackupedData(false);
					}
				} else if (operation == BACKUP_OPERATION) {
					successfull = move(new File(info.getPathToData()), getFileToBackup(info));
					if (successfull) {
						info.setBackupedData(true);
						getMyApplication().getResourceManager().closeFile(info.getFileName());
					}
				} else if (operation == CLEAR_TILES_OPERATION) {
					ITileSource src = (ITileSource) info.getAttachedObject();
					if (src != null) {
						src.deleteTiles(info.getPathToData());
						clearMapillaryTiles(info);
					}
				}
				total++;
				if (successfull) {
					count++;
					publishProgress(info);
				}
			}
		}
		if (operation == DELETE_OPERATION) {
			a.getDownloadThread().updateLoadedFiles();
		}
		if (operation == DELETE_OPERATION) {
			return a.getString(R.string.local_index_items_deleted, count, total);
		} else if (operation == BACKUP_OPERATION) {
			return a.getString(R.string.local_index_items_backuped, count, total);
		} else if (operation == RESTORE_OPERATION) {
			return a.getString(R.string.local_index_items_restored, count, total);
		}

		return "";
	}


	@Override
	protected void onProgressUpdate(LocalIndexInfo... values) {
		if (listAdapter != null) {
			if (operation == DELETE_OPERATION) {
				listAdapter.delete(values);
			} else if (operation == BACKUP_OPERATION) {
				listAdapter.move(values, false);
			} else if (operation == RESTORE_OPERATION) {
				listAdapter.move(values, true);
			}
		}

	}

	@Override
	protected void onPreExecute() {
		a.setProgressBarIndeterminateVisibility(true);
	}

	@Override
	protected void onPostExecute(String result) {
		a.setProgressBarIndeterminateVisibility(false);
		if (result != null && result.length() > 0) {
			Toast.makeText(a, result, Toast.LENGTH_LONG).show();
		}

		if (operation == RESTORE_OPERATION || operation == BACKUP_OPERATION || operation == CLEAR_TILES_OPERATION) {
			a.reloadLocalIndexes();
		} else {
			a.onUpdatedIndexesList();
		}
	}

	// Clear tiles for both Mapillary sources together
	private void clearMapillaryTiles(LocalIndexInfo info) {
		ITileSource src = (ITileSource) info.getAttachedObject();
		ITileSource mapilaryCache = TileSourceManager.getMapillaryCacheSource();
		ITileSource mapilaryVector = TileSourceManager.getMapillaryVectorSource();
		if (src != null && (mapilaryVector.getName().equals(src.getName()) || mapilaryCache.getName().equals(src.getName()))) {
			File current = new File(info.getPathToData());
			File parent = current.getParentFile();
			if (parent == null) {
				return;
			}
			File[] list = parent.listFiles();
			if (list == null) {
				return;
			}
			for (File f : list) {
				String withoutExt = Algorithms.getFileNameWithoutExtension(f);
				String sqliteExt = IndexConstants.SQLITE_EXT.replace(".", "");
				ITileSource cache = null;
				if (withoutExt.equals(mapilaryCache.getName())) {
					cache = mapilaryCache;
				} else if (withoutExt.equals(mapilaryVector.getName())) {
					cache = mapilaryVector;
				}
				if (cache != null) {
					if (f.isDirectory()) {
						cache.deleteTiles(f.getPath());
					} else if (Algorithms.getFileExtension(f).equals(sqliteExt)) {
						SQLiteTileSource sqlTileSource = new SQLiteTileSource(getMyApplication(), f, TileSourceManager.getKnownSourceTemplates());
						sqlTileSource.deleteTiles(f.getPath());
					}
				}
			}
		}
	}

	private void clearHeightmapTiles(@NonNull LocalIndexInfo info) {
		String filePath = info.getPathToData();
		boolean heightmap = filePath.endsWith(IndexConstants.TIF_EXT);
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (heightmap && mapRendererContext != null) {
			mapRendererContext.removeCachedHeightmapTiles(filePath);
		}
	}
}
