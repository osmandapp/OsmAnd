package net.osmand.plus.download.ui;

import android.os.AsyncTask;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.core.android.MapRendererContext;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.LocalIndexHelper.LocalIndexType;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LocalIndexOperationTask extends AsyncTask<LocalIndexInfo, LocalIndexInfo, String> {

	public static final int DELETE_OPERATION = 1;
	public static final int BACKUP_OPERATION = 2;
	public static final int RESTORE_OPERATION = 3;
	public static final int CLEAR_TILES_OPERATION = 4;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({DELETE_OPERATION, BACKUP_OPERATION, RESTORE_OPERATION, CLEAR_TILES_OPERATION})
	public @interface IndexOperationType {
	}

	private final OsmandApplication app;
	private final OperationListener listener;

	@IndexOperationType
	private final int operation;

	public LocalIndexOperationTask(@NonNull OsmandApplication app, @Nullable OperationListener listener, @IndexOperationType int operation) {
		this.app = app;
		this.listener = listener;
		this.operation = operation;
	}

	private boolean move(File from, File to) {
		File parent = to.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		return from.renameTo(to);
	}

	private File getFileToBackup(@NonNull LocalIndexInfo indexInfo) {
		if (!indexInfo.isBackupedData()) {
			return new File(app.getAppPath(IndexConstants.BACKUP_INDEX_DIR), indexInfo.getFileName());
		}
		return new File(indexInfo.getPathToData());
	}

	private File getFileToRestore(LocalIndexInfo info) {
		String fileName = info.getFileName();
		if (info.isBackupedData()) {
			File parent = new File(info.getPathToData()).getParentFile();
			if (info.getOriginalType() == LocalIndexType.MAP_DATA) {
				if (fileName.endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
					parent = app.getAppPath(IndexConstants.ROADS_INDEX_DIR);
				} else {
					parent = app.getAppPath(IndexConstants.MAPS_PATH);
				}
			} else if (info.getOriginalType() == LocalIndexType.TILES_DATA) {
				if (fileName.endsWith(IndexConstants.HEIGHTMAP_SQLITE_EXT)) {
					parent = app.getAppPath(IndexConstants.HEIGHTMAP_INDEX_DIR);
				} else if (fileName.endsWith(IndexConstants.TIF_EXT)) {
					parent = app.getAppPath(IndexConstants.GEOTIFF_DIR);
				} else {
					parent = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
				}
			} else if (info.getOriginalType() == LocalIndexType.SRTM_DATA) {
				parent = app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
			} else if (info.getOriginalType() == LocalIndexType.WIKI_DATA) {
				parent = app.getAppPath(IndexConstants.WIKI_INDEX_DIR);
			} else if (info.getOriginalType() == LocalIndexType.TRAVEL_DATA) {
				parent = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
			} else if (info.getOriginalType() == LocalIndexType.TTS_VOICE_DATA) {
				parent = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			} else if (info.getOriginalType() == LocalIndexType.VOICE_DATA) {
				parent = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
			} else if (info.getOriginalType() == LocalIndexType.FONT_DATA) {
				parent = app.getAppPath(IndexConstants.FONT_INDEX_DIR);
			} else if (info.getOriginalType() == LocalIndexType.DEPTH_DATA) {
				parent = app.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR);
			}
			return new File(parent, fileName);
		}
		return new File(info.getPathToData());
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

					if (InAppPurchaseHelper.isSubscribedToLiveUpdates(app)) {
						String fileNameWithoutExtension = Algorithms.getFileNameWithoutExtension(f);
						IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
						changesManager.deleteUpdates(fileNameWithoutExtension);
					}
					if (successfull) {
						app.getResourceManager().closeFile(info.getFileName());
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
						app.getResourceManager().closeFile(info.getFileName());
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
			app.getDownloadThread().updateLoadedFiles();
		}
		if (operation == DELETE_OPERATION) {
			return app.getString(R.string.local_index_items_deleted, count, total);
		} else if (operation == BACKUP_OPERATION) {
			return app.getString(R.string.local_index_items_backuped, count, total);
		} else if (operation == RESTORE_OPERATION) {
			return app.getString(R.string.local_index_items_restored, count, total);
		}

		return "";
	}

	@Override
	protected void onProgressUpdate(LocalIndexInfo... values) {
		if (listener != null) {
			listener.onOperationProgress(operation, values);
		}
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onOperationStarted();
		}
	}

	@Override
	protected void onPostExecute(String result) {
		if (listener != null) {
			listener.onOperationFinished(operation, result);
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
						SQLiteTileSource sqlTileSource = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
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

	public interface OperationListener {
		default void onOperationStarted() {

		}

		default void onOperationProgress(@IndexOperationType int operation, LocalIndexInfo... values) {

		}

		default void onOperationFinished(@IndexOperationType int operation, String result) {

		}
	}
}
