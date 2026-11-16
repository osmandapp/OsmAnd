package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapSourcesSettingsItem extends CollectionSettingsItem<ITileSource> {

	private static final int APPROXIMATE_MAP_SOURCES_SIZE_BYTES = 450;

	private List<String> existingItemsNames;

	public MapSourcesSettingsItem(@NonNull OsmandApplication app, @NonNull List<ITileSource> items) {
		super(app, null, items);
	}

	public MapSourcesSettingsItem(@NonNull OsmandApplication app, @Nullable MapSourcesSettingsItem baseItem, @NonNull List<ITileSource> items) {
		super(app, baseItem, items);
	}

	public MapSourcesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		existingItemsNames = new ArrayList<>(app.getSettings().getTileSourceEntries().values());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.MAP_SOURCES;
	}

	@Override
	public long getLocalModifiedTime() {
		OsmandSettings settings = app.getSettings();
		String savedHash = settings.MAP_SOURCES_BACKUP_STATE_HASH.get();
		long savedTime = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get();
		boolean firstRun = Algorithms.isEmpty(savedHash);
		// Read the file times before the hash: opening a tile database may add info columns to it.
		long fileTime = firstRun ? getFilesModifiedTime() : 0;
		String hash = getStateHash(items);
		if (hash.equals(savedHash)) {
			return savedTime;
		}
		// The first run keeps the file time, so that an edit made before the update is not lost.
		long modifiedTime = firstRun ? fileTime : Math.max(System.currentTimeMillis(), savedTime + 1);
		saveState(hash, modifiedTime);
		return modifiedTime;
	}

	private long getFilesModifiedTime() {
		long lastModifiedTime = 0;
		for (ITileSource source : items) {
			if (source instanceof SQLiteTileSource) {
				long lastModified = app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName()
						+ IndexConstants.SQLITE_EXT).lastModified();
				if (lastModified > lastModifiedTime) {
					lastModifiedTime = lastModified;
				}
			} else if (source instanceof TileSourceManager.TileSourceTemplate) {
				long lastModified = new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName()), ".metainfo").lastModified();
				if (lastModified > lastModifiedTime) {
					lastModifiedTime = lastModified;
				}
			}
		}
		return lastModifiedTime;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		if (appliedItems.isEmpty()) {
			// Nothing was installed, so there is no imported state to acknowledge.
			return;
		}
		Set<String> appliedIds = new HashSet<>();
		for (ITileSource source : appliedItems) {
			appliedIds.add(getSourceId(source));
		}
		// Hash the sources as installed, the way the next local scan reads them. Local sources
		// that were not imported stay out of the hash and remain a local change.
		List<?> installedSources = ExportType.MAP_SOURCES.fetchExportData(app, false);
		try {
			List<ITileSource> importedSources = new ArrayList<>();
			for (Object object : installedSources) {
				ITileSource source = (ITileSource) object;
				if (appliedIds.contains(getSourceId(source))) {
					importedSources.add(source);
				}
			}
			saveState(getStateHash(importedSources), lastModifiedTime);
		} finally {
			for (Object source : installedSources) {
				if (source instanceof SQLiteTileSource) {
					((SQLiteTileSource) source).closeConnection();
				}
			}
		}
	}

	@NonNull
	private static String getSourceId(@NonNull ITileSource source) {
		return (source instanceof SQLiteTileSource ? "sqlite:" : "template:") + source.getName();
	}

	private void saveState(@NonNull String hash, long modifiedTime) {
		OsmandSettings settings = app.getSettings();
		// The hash goes last: an interrupted save then repeats the change instead of losing it.
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(modifiedTime);
		settings.MAP_SOURCES_BACKUP_STATE_HASH.set(hash);
	}

	/**
	 * Hash of the source settings that go to the backup. Tile writes do not change it, unlike the
	 * file time of a tile database.
	 */
	@NonNull
	private static String getStateHash(@NonNull List<ITileSource> sources) {
		List<String> states = new ArrayList<>();
		for (ITileSource source : sources) {
			if (source instanceof SQLiteTileSource && ((SQLiteTileSource) source).isFileExist()) {
				// The getters return defaults until the database info is read.
				((SQLiteTileSource) source).initDatabaseIfNeeded();
			}
			try {
				JSONObject json = writeSourceToJson(source);
				if (source instanceof SQLiteTileSource) {
					// Detected from the first decoded tile and saved to the database, not a user edit.
					json.remove("tileSize");
				}
				states.add(json.toString());
			} catch (JSONException e) {
				SettingsHelper.LOG.error("Failed to write map source state", e);
			}
		}
		Collections.sort(states);
		StringBuilder state = new StringBuilder();
		for (String sourceState : states) {
			state.append(sourceState).append('\n');
		}
		// Older Android versions provide a platform Hex class without encodeHexString().
		return new String(Hex.encodeHex(DigestUtils.sha256(state.toString())));
	}

	@Override
	public void apply() {
		List<ITileSource> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);
			if (shouldReplace) {
				for (ITileSource tileSource : duplicateItems) {
					if (tileSource instanceof SQLiteTileSource) {
						File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + tileSource.getName() + IndexConstants.SQLITE_EXT);
						if (f != null && f.exists() && Algorithms.removeAllFiles(f)) {
							appliedItems.add(tileSource);
						}
					} else if (tileSource instanceof TileSourceManager.TileSourceTemplate) {
						File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + tileSource.getName());
						if (f != null && f.exists() && f.isDirectory() && Algorithms.removeAllFiles(f)) {
							appliedItems.add(tileSource);
						}
					}
				}
			} else {
				for (ITileSource tileSource : duplicateItems) {
					appliedItems.add(renameItem(tileSource));
				}
			}
			for (ITileSource tileSource : appliedItems) {
				if (tileSource instanceof TileSourceManager.TileSourceTemplate) {
					app.getSettings().installTileSource((TileSourceManager.TileSourceTemplate) tileSource);
				} else if (tileSource instanceof SQLiteTileSource) {
					((SQLiteTileSource) tileSource).createDataBase();
				}
			}
		}
	}

	@Override
	protected void deleteItem(ITileSource item) {
		// TODO: delete settings item
	}

	@NonNull
	@Override
	public ITileSource renameItem(@NonNull ITileSource item) {
		int number = 0;
		while (true) {
			number++;
			if (item instanceof SQLiteTileSource) {
				SQLiteTileSource oldItem = (SQLiteTileSource) item;
				String newName = oldItem.getName() + "_" + number;
				SQLiteTileSource renamedItem = new SQLiteTileSource(oldItem, newName, app);
				if (!isDuplicate(renamedItem)) {
					return renamedItem;
				}
			} else if (item instanceof TileSourceManager.TileSourceTemplate) {
				TileSourceManager.TileSourceTemplate oldItem = (TileSourceManager.TileSourceTemplate) item;
				oldItem.setName(oldItem.getName() + "_" + number);
				if (!isDuplicate(oldItem)) {
					return oldItem;
				}
			}
		}
	}

	@Override
	public long getEstimatedItemSize(@NonNull ITileSource item) {
		return APPROXIMATE_MAP_SOURCES_SIZE_BYTES;
	}

	@Override
	public boolean isDuplicate(@NonNull ITileSource item) {
		for (String name : existingItemsNames) {
			if (name.equals(item.getName())) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public String getName() {
		return "map_sources";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.quick_action_map_source_title);
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			JSONArray jsonArray = json.getJSONArray("items");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				boolean sql = object.optBoolean("sql");
				String name = object.optString("name");
				int minZoom = object.optInt("minZoom");
				int maxZoom = object.optInt("maxZoom");
				String url = object.optString("url");
				String randoms = object.optString("randoms");
				boolean ellipsoid = object.optBoolean("ellipsoid", false);
				boolean invertedY = object.optBoolean("inverted_y", false);
				String referer = object.optString("referer");
				String userAgent = object.optString("userAgent");
				boolean timeSupported = object.optBoolean("timesupported", false);
				long expire = object.optLong("expire", -1);
				boolean inversiveZoom = object.optBoolean("inversiveZoom", false);
				String ext = object.optString("ext");
				int tileSize = object.optInt("tileSize");
				int bitDensity = object.optInt("bitDensity");
				int avgSize = object.optInt("avgSize");
				String rule = object.optString("rule");

				if (expire > 0 && expire < 3600000) {
					expire = expire * 60 * 1000L;
				}

				ITileSource template;
				if (!sql) {
					TileSourceManager.TileSourceTemplate tileSourceTemplate = new TileSourceManager.TileSourceTemplate(name, url, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
					tileSourceTemplate.setRule(rule);
					tileSourceTemplate.setRandoms(randoms);
					tileSourceTemplate.setReferer(referer);
					tileSourceTemplate.setUserAgent(userAgent);
					tileSourceTemplate.setEllipticYTile(ellipsoid);
					tileSourceTemplate.setInvertedYTile(invertedY);
					tileSourceTemplate.setExpirationTimeMillis(timeSupported ? expire : -1);

					template = tileSourceTemplate;
				} else {
					template = new SQLiteTileSource(app, name, minZoom, maxZoom, url, randoms, ellipsoid, invertedY, referer, userAgent, timeSupported, expire, inversiveZoom, rule);
				}
				items.add(template);
			}
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		if (!items.isEmpty()) {
			try {
				for (ITileSource template : items) {
					jsonArray.put(writeSourceToJson(template));
				}
				json.put("items", jsonArray);

			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
		return json;
	}

	@NonNull
	private static JSONObject writeSourceToJson(@NonNull ITileSource template) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		boolean sql = template instanceof SQLiteTileSource;
		jsonObject.put("sql", sql);
		jsonObject.put("name", template.getName());
		jsonObject.put("minZoom", template.getMinimumZoomSupported());
		jsonObject.put("maxZoom", template.getMaximumZoomSupported());
		jsonObject.put("url", template.getUrlTemplate());
		jsonObject.put("randoms", template.getRandoms());
		jsonObject.put("ellipsoid", template.isEllipticYTile());
		jsonObject.put("inverted_y", template.isInvertedYTile());
		jsonObject.put("referer", template.getReferer());
		jsonObject.put("userAgent", template.getUserAgent());
		jsonObject.put("timesupported", template.isTimeSupported());
		jsonObject.put("expire", template.getExpirationTimeMinutes());
		jsonObject.put("inversiveZoom", template.getInversiveZoom());
		jsonObject.put("ext", template.getTileFormat());
		jsonObject.put("tileSize", template.getTileSize());
		jsonObject.put("bitDensity", template.getBitDensity());
		jsonObject.put("avgSize", template.getAvgSize());
		jsonObject.put("rule", template.getRule());
		return jsonObject;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader(false);
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
