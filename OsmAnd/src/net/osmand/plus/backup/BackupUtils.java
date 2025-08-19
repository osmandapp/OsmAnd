package net.osmand.plus.backup;

import static net.osmand.IProgress.EMPTY_PROGRESS;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.MULTIMEDIA_NOTES;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.RENDERING_STYLE;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.ROUTING_CONFIG;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.items.PoiUiFiltersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupUtils {


	private static final String BACKUP_TYPE_PREFIX = "backup_type_";
	private static final String VERSION_HISTORY_PREFIX = "save_version_history_";

	public static void setLastModifiedTime(@NonNull Context ctx, @NonNull String name) {
		setLastModifiedTime(ctx, name, System.currentTimeMillis());
	}

	public static void setLastModifiedTime(@NonNull Context ctx, @NonNull String name, long lastModifiedTime) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		app.getBackupHelper().getDbHelper().setLastModifiedTime(name, lastModifiedTime);
	}

	public static long getLastModifiedTime(@NonNull Context ctx, @NonNull String name) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		return app.getBackupHelper().getDbHelper().getLastModifiedTime(name);
	}

	public static boolean isTokenValid(@NonNull String token) {
		return token.matches("[0-9]+");
	}

	@NonNull
	public static List<SettingsItem> getItemsForRestore(@Nullable BackupInfo info, @NonNull List<SettingsItem> settingsItems) {
		List<SettingsItem> items = new ArrayList<>();
		if (info != null) {
			Map<RemoteFile, SettingsItem> restoreItems = getRemoteFilesSettingsItems(settingsItems, info.filteredFilesToDownload, false);
			for (SettingsItem restoreItem : restoreItems.values()) {
				if (restoreItem instanceof CollectionSettingsItem) {
					CollectionSettingsItem<?> settingsItem = (CollectionSettingsItem<?>) restoreItem;
					settingsItem.processDuplicateItems();
					settingsItem.setShouldReplace(true);
				}
				items.add(restoreItem);
			}
		}
		Collections.sort(items, (o1, o2) -> -Long.compare(o1.getLastModifiedTime(), o2.getLastModifiedTime()));

		return items;
	}

	@NonNull
	public static Map<RemoteFile, SettingsItem> getItemsMapForRestore(@Nullable BackupInfo info, @NonNull List<SettingsItem> settingsItems) {
		Map<RemoteFile, SettingsItem> itemsForRestore = new HashMap<>();
		if (info != null) {
			itemsForRestore.putAll(getRemoteFilesSettingsItems(settingsItems, info.filteredFilesToDownload, false));
		}
		return itemsForRestore;
	}

	@NonNull
	public static Map<RemoteFile, SettingsItem> getRemoteFilesSettingsItems(@NonNull List<SettingsItem> items,
			@NonNull List<RemoteFile> remoteFiles, boolean infoFiles) {
		Map<RemoteFile, SettingsItem> res = new HashMap<>();
		Map<String, SettingsItem> settingsItemMap = new HashMap<>();
		List<FileSettingsItem> subtypeFolders = new ArrayList<>();
		String DELIMETER = "___";
		for (SettingsItem item : items) {
			String itemFileName = getItemFileName(item);
			settingsItemMap.put(item.getType().name() + DELIMETER + itemFileName, item);
			// Commits FileSettingsItem introduced likely are related to TTS / Voice configuration (folders)
			// https://github.com/osmandapp/OsmAnd/commit/ba750f9df87057da268b36e0d32b8c1996f4023a
			// https://github.com/osmandapp/OsmAnd/commit/bf93162bd13ef7ab16622bb662c953e931c34a21
			if (item instanceof FileSettingsItem fileItem) {
				String subtypeFolder = fileItem.getSubtype().getSubtypeFolder();
				if (subtypeFolder != null && FileUtils.isProbablyDir(fileItem.getFile())) {
					subtypeFolders.add(fileItem);
				}
			}
		}
		for (RemoteFile file : remoteFiles) {
			String type = file.getType();
			String name = file.getName();
			if (infoFiles && name.endsWith(BackupHelper.INFO_EXT)) {
				name = name.substring(0, name.length() - BackupHelper.INFO_EXT.length());
			}
			SettingsItem item = settingsItemMap.get(type + DELIMETER + name);
			if (item != null) {
				res.put(file, item);
			} else {
				for (FileSettingsItem fileItem : subtypeFolders) {
					String itemFileName = getItemFileName(fileItem);
					boolean found = false;
					if (!itemFileName.endsWith("/")) {
						found = name.startsWith(itemFileName + "/");
					} else {
						found = name.startsWith(itemFileName);
					}
					if (found) {
						res.put(file, fileItem);
						break;
					}
				}
			}
		}
		return res;
	}

	public static CommonPreference<Boolean> getBackupTypePref(@NonNull OsmandApplication app, @NonNull ExportType type) {
		return app.getSettings().registerBooleanPreference(BACKUP_TYPE_PREFIX + type.name(), true).makeGlobal();
	}

	public static CommonPreference<Boolean> getVersionHistoryTypePref(@NonNull OsmandApplication app, @NonNull ExportType exportType) {
		return app.getSettings().registerBooleanPreference(VERSION_HISTORY_PREFIX + exportType.name(), true).makeGlobal().makeShared();
	}

	@NonNull
	public static String getItemFileName(@NonNull SettingsItem item) {
		String fileName;
		if (item instanceof FileSettingsItem fileItem) {
			fileName = getFileItemName(fileItem);
		} else {
			fileName = item.getFileName();
			if (Algorithms.isEmpty(fileName)) {
				fileName = item.getDefaultFileName();
			}
		}
		return removeLeadingSlash(fileName);
	}

	@NonNull
	public static String getFileItemName(@NonNull FileSettingsItem fileSettingsItem) {
		return getFileItemName(null, fileSettingsItem);
	}

	@NonNull
	public static String getFileItemName(@Nullable File file, @NonNull FileSettingsItem fileSettingsItem) {
		String subtypeFolder = fileSettingsItem.getSubtype().getSubtypeFolder();
		String fileName;
		if (file == null) {
			file = fileSettingsItem.getFile();
		}
		if (Algorithms.isEmpty(subtypeFolder)) {
			fileName = file.getName();
		} else if (fileSettingsItem instanceof GpxSettingsItem) {
			fileName = file.getPath().substring(file.getPath().indexOf(subtypeFolder) + subtypeFolder.length());
		} else {
			fileName = file.getPath().substring(file.getPath().indexOf(subtypeFolder) - 1);
		}
		return removeLeadingSlash(fileName);
	}

	public static String removeLeadingSlash(@Nullable String fileName) {
		if (!Algorithms.isEmpty(fileName) && fileName.charAt(0) == '/') {
			fileName = fileName.substring(1);
		}
		return fileName;
	}

	public static boolean isLimitedFilesCollectionItem(@NonNull FileSettingsItem item) {
		return item.getSubtype() == FileSubtype.VOICE;
	}

	public static boolean isDefaultObfMap(@NonNull OsmandApplication app,
	                                      @NonNull FileSettingsItem settingsItem,
	                                      @NonNull String fileName) {
		FileSubtype subtype = settingsItem.getSubtype();
		if (subtype.isMap()) {
			return isObfMapExistsOnServer(app, fileName);
		}
		return false;
	}

	private static boolean isObfMapExistsOnServer(@NonNull OsmandApplication app, @NonNull String name) {
		boolean[] exists = new boolean[1];

		Map<String, String> params = new HashMap<>();
		params.put("name", name);
		params.put("type", "file");

		OperationLog operationLog = new OperationLog("isObfMapExistsOnServer", BackupHelper.DEBUG);
		operationLog.startOperation(name);

		AndroidNetworkUtils.sendRequest(app, "https://osmand.net/userdata/check-file-on-server",
				params, "Check obf map on server", false, false,
				(result, error, resultCode) -> {
					int status;
					String message;
					if (!Algorithms.isEmpty(error)) {
						status = BackupHelper.STATUS_SERVER_ERROR;
						message = "Check obf map on server error: " + new BackupError(error);
					} else if (!Algorithms.isEmpty(result)) {
						try {
							JSONObject obj = new JSONObject(result);
							String fileStatus = obj.optString("status");
							exists[0] = Algorithms.stringsEqual(fileStatus, "present");

							status = BackupHelper.STATUS_SUCCESS;
							message = name + " exists: " + exists[0];
						} catch (JSONException e) {
							status = BackupHelper.STATUS_PARSE_JSON_ERROR;
							message = "Check obf map on server error: json parsing";
						}
					} else {
						status = BackupHelper.STATUS_EMPTY_RESPONSE_ERROR;
						message = "Check obf map on server error: empty response";
					}
					operationLog.finishOperation("(" + status + "): " + message);
				});
		return exists[0];
	}

	public static void updateCacheForItems(@NonNull OsmandApplication app, @NonNull List<SettingsItem> items) {
		if (Algorithms.isEmpty(items)) {
			return;
		}
		boolean updateIndexes = false;
		boolean updateRouting = false;
		boolean updateRenderers = false;
		boolean updatePoiFilters = false;
		boolean updateMultimedia = false;

		for (SettingsItem item : items) {
			if (item instanceof FileSettingsItem) {
				FileSubtype subtype = ((FileSettingsItem) item).getSubtype();
				updateIndexes |= subtype.isMap();
				updateRouting |= ROUTING_CONFIG == subtype;
				updateRenderers |= RENDERING_STYLE == subtype;
				updateMultimedia |= MULTIMEDIA_NOTES == subtype;
			} else if (item instanceof PoiUiFiltersSettingsItem || item instanceof ProfileSettingsItem) {
				updatePoiFilters = true;
			}
		}
		if (updateIndexes) {
			app.getResourceManager().reloadIndexesAsync(EMPTY_PROGRESS, warnings -> app.getOsmandMap().refreshMap());
		}
		if (updateRouting) {
			AppInitializer.loadRoutingFiles(app, null);
		}
		if (updateRenderers) {
			app.getRendererRegistry().updateExternalRenderers();
		}
		if (updatePoiFilters) {
			app.getPoiFilters().loadSelectedPoiFilters();
		}
		if (updateMultimedia) {
			AudioVideoNotesPlugin plugin = PluginsHelper.getPlugin(AudioVideoNotesPlugin.class);
			if (plugin != null) {
				plugin.indexingFiles(true, true);
			}
		}
	}

	public static long calculateItemsSize(@NonNull List<?> items) {
		long size = 0;
		for (Object item : items) {
			size += getItemSize(item);
		}
		return size;
	}

	public static long getItemSize(@NonNull Object object) {
		if (object instanceof FileSettingsItem fileSettingsItem) {
			return fileSettingsItem.getSize();
		} else if (object instanceof File file) {
			return file.length();
		} else if (object instanceof RemoteFile remoteFile) {
			return  remoteFile.getZipSize();
		} else if (object instanceof MapMarkersGroup markersGroup) {
			if (CollectionUtils.equalsToAny(markersGroup.getId(),
					ExportType.ACTIVE_MARKERS.name(), ExportType.HISTORY_MARKERS.name())) {
				return  markersGroup.getMarkers().size();
			}
		}
		return 0;
	}
}
