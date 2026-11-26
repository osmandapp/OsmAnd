package net.osmand.plus.download.local;

import static net.osmand.IndexConstants.*;
import static net.osmand.plus.download.local.LocalItemType.COLOR_DATA;
import static net.osmand.plus.download.local.LocalItemType.DEPTH_DATA;
import static net.osmand.plus.download.local.LocalItemType.FONT_DATA;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.ROAD_DATA;
import static net.osmand.plus.download.local.LocalItemType.TERRAIN_DATA;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.download.local.LocalItemType.TTS_VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.VOICE_DATA;
import static net.osmand.plus.download.local.LocalItemType.WIKI_AND_TRAVEL_MAPS;
import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.CLEAR_TILES_OPERATION;
import static net.osmand.plus.download.local.OperationType.DELETE_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererContext;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.download.local.dialogs.LiveGroupItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import java.io.File;

public class LocalOperationTask extends AsyncTask<BaseLocalItem, BaseLocalItem, String> {

	private final OsmandApplication app;
	private final OperationType type;
	private final OperationListener listener;

	public LocalOperationTask(@NonNull OsmandApplication app, @NonNull OperationType type, @Nullable OperationListener listener) {
		this.app = app;
		this.type = type;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onOperationStarted();
		}
	}

	@Override
	protected void onProgressUpdate(BaseLocalItem... values) {
		if (listener != null) {
			listener.onOperationProgress(type, values);
		}
	}

	@Override
	protected String doInBackground(BaseLocalItem... params) {
		int count = 0;
		int total = 0;
		for (BaseLocalItem item : params) {
			if (!isCancelled()) {
				boolean success = processItem(item);
				total++;

				if (success) {
					count++;
					publishProgress(item);
				}
			}
		}
		if (type == DELETE_OPERATION) {
			app.getDownloadThread().updateLoadedFiles();
		}
		if (type == DELETE_OPERATION) {
			return app.getString(R.string.local_index_items_deleted, count, total);
		} else if (type == BACKUP_OPERATION) {
			return app.getString(R.string.local_index_items_backuped, count, total);
		} else if (type == RESTORE_OPERATION) {
			return app.getString(R.string.local_index_items_restored, count, total);
		}
		return "";
	}

	@Override
	protected void onPostExecute(String result) {
		if (listener != null) {
			listener.onOperationFinished(type, result);
		}
	}

	private boolean processItem(@NonNull BaseLocalItem item) {
		if (item instanceof LocalItem) {
			return processItem((LocalItem) item);
		} else if (item instanceof LiveGroupItem) {
			LiveGroupItem groupItem = (LiveGroupItem) item;

			boolean success = false;
			for (LocalItem localItem : groupItem.getItems()) {
				if (!isCancelled()) {
					success |= processItem(localItem);
				}
			}
			return success;
		}
		return false;
	}

	private boolean processItem(@NonNull LocalItem item) {
		if (type == DELETE_OPERATION) {
			return deleteItem(item);
		} else if (type == RESTORE_OPERATION) {
			return restoreItem(item);
		} else if (type == BACKUP_OPERATION) {
			return backupItem(item);
		} else if (type == CLEAR_TILES_OPERATION) {
			return clearTilesItem(item);
		}
		return false;
	}

	private boolean deleteItem(@NonNull LocalItem item) {
		File file = item.getFile();
		boolean success = Algorithms.removeAllFiles(file);

		if (InAppPurchaseUtils.isLiveUpdatesAvailable(app)) {
			String fileNameWithoutExtension = Algorithms.getFileNameWithoutExtension(file);
			IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
			changesManager.deleteUpdates(fileNameWithoutExtension);
		}
		if (success) {
			app.getResourceManager().closeFile(file.getName());
			File tShm = new File(file.getParentFile(), file.getName() + "-shm");
			File tWal = new File(file.getParentFile(), file.getName() + "-wal");
			if (tShm.exists()) {
				Algorithms.removeAllFiles(tShm);
			}
			if (tWal.exists()) {
				Algorithms.removeAllFiles(tWal);
			}
			if (item.getType() == TILES_DATA) {
				clearMapillaryTiles(item);
			}
			if (item.getType() == TERRAIN_DATA) {
				clearHeightmapTiles(item);
			}
		}
		return success;
	}

	private boolean restoreItem(@NonNull LocalItem item) {
		return FileUtils.move(item.getFile(), getFileToRestore(item));
	}

	private boolean backupItem(@NonNull LocalItem item) {
		File file = item.getFile();
		boolean success = FileUtils.move(file, getFileToBackup(item));
		if (success) {
			app.getResourceManager().closeFile(file.getName());
		}
		return success;
	}

	private boolean clearTilesItem(@NonNull LocalItem item) {
		ITileSource source = (ITileSource) item.getAttachedObject();
		if (source != null) {
			source.deleteTiles(item.getFile().getAbsolutePath());
			clearMapillaryTiles(item);
		}
		return source != null;
	}

	@NonNull
	private File getFileToBackup(@NonNull LocalItem item) {
		File file = item.getFile();
		if (!item.isBackuped(app)) {
			File path = item.isHidden(app) ? app.getAppInternalPath(HIDDEN_BACKUP_DIR) : app.getAppPath(BACKUP_INDEX_DIR);
			return new File(path, file.getName());
		}
		return file;
	}

	@NonNull
	private File getFileToRestore(@NonNull LocalItem item) {
		File file = item.getFile();
		String fileName = file.getName();
		if (item.isBackuped(app)) {
			File parent = file.getParentFile();
			if (item.isHidden(app)) {
				parent = app.getAppInternalPath(HIDDEN_DIR);
			} else if (item.getType() == MAP_DATA) {
				parent = app.getAppPath(MAPS_PATH);
			} else if (item.getType() == ROAD_DATA) {
				parent = app.getAppPath(ROADS_INDEX_DIR);
			} else if (item.getType() == TILES_DATA) {
				if (fileName.endsWith(TIF_EXT)) {
					parent = app.getAppPath(GEOTIFF_DIR);
				} else {
					parent = app.getAppPath(TILES_INDEX_DIR);
				}
			} else if (item.getType() == TTS_VOICE_DATA) {
				parent = app.getAppPath(VOICE_INDEX_DIR);
			} else if (item.getType() == VOICE_DATA) {
				parent = app.getAppPath(VOICE_INDEX_DIR);
			} else if (item.getType() == FONT_DATA) {
				parent = app.getAppPath(FONT_INDEX_DIR);
			} else if (item.getType() == DEPTH_DATA) {
				parent = app.getAppPath(NAUTICAL_INDEX_DIR);
			} else if (item.getType() == TERRAIN_DATA) {
				if (fileName.endsWith(TIF_EXT)) {
					parent = app.getAppPath(GEOTIFF_DIR);
				} else if (SrtmDownloadItem.isSrtmFile(fileName)) {
					parent = app.getAppPath(SRTM_INDEX_DIR);
				}
			} else if (item.getType() == WIKI_AND_TRAVEL_MAPS) {
				if (fileName.endsWith(BINARY_WIKI_MAP_INDEX_EXT)) {
					parent = app.getAppPath(WIKI_INDEX_DIR);
				} else if (fileName.endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
						|| fileName.endsWith(BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
					parent = app.getAppPath(WIKIVOYAGE_INDEX_DIR);
				}
			} else if (item.getType() == COLOR_DATA) {
				parent = app.getAppPath(COLOR_PALETTE_DIR);
			}
			return new File(parent, fileName);
		}
		return file;
	}

	// Clear tiles for both Mapillary sources together
	private void clearMapillaryTiles(@NonNull LocalItem item) {
		ITileSource src = (ITileSource) item.getAttachedObject();
		ITileSource mapilaryCache = TileSourceManager.getMapillaryCacheSource();
		ITileSource mapilaryVector = TileSourceManager.getMapillaryVectorSource();
		if (src != null && (mapilaryVector.getName().equals(src.getName()) || mapilaryCache.getName().equals(src.getName()))) {
			File current = item.getFile();
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
				String sqliteExt = SQLITE_EXT.replace(".", "");
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
						SQLiteTileSource sqlTileSource = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
						sqlTileSource.deleteTiles(f.getPath());
					}
				}
			}
		}
	}

	private void clearHeightmapTiles(@NonNull LocalItem item) {
		String filePath = item.getFile().getAbsolutePath();
		boolean heightmap = filePath.endsWith(TIF_EXT);
		MapRendererContext mapRendererContext = NativeCoreContext.getMapRendererContext();
		if (heightmap && mapRendererContext != null) {
			mapRendererContext.removeCachedHeightmapTiles(filePath);
		}
	}

	public interface OperationListener {
		default void onOperationStarted() {

		}

		default void onOperationProgress(@NonNull OperationType type, @NonNull BaseLocalItem... items) {

		}

		default void onOperationFinished(@NonNull OperationType type, @NonNull String result) {

		}
	}
}
