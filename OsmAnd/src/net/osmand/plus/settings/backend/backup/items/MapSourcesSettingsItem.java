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
import net.osmand.util.Algorithms;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
		String storedStateHash = settings.MAP_SOURCES_BACKUP_STATE_HASH.get();
		long storedModifiedTime = settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.get();
		boolean hasStoredState = !Algorithms.isEmpty(storedStateHash);
		// Has to be resolved before the state hash is computed: computing the hash opens
		// the tile databases, which may add missing info columns to them and by that
		// change the very file timestamps the initial time is derived from.
		long initialModifiedTime = hasStoredState ? 0 : getInitialModifiedTime(storedModifiedTime);

		String currentStateHash = getStateHash(items);
		if (currentStateHash.equals(storedStateHash)) {
			return storedModifiedTime;
		}
		long modifiedTime = hasStoredState
				? Math.max(System.currentTimeMillis(), storedModifiedTime + 1)
				: initialModifiedTime;
		persistBackupState(currentStateHash, modifiedTime);
		return modifiedTime;
	}

	/**
	 * There is no semantic baseline on the first run after upgrading, so the legacy file
	 * time is kept to not silently discard a source edit that was never synced.
	 *
	 * @param storedModifiedTime a non zero time without a stored hash is left over from an
	 * initialization that was interrupted between the two writes, and is reused as it is.
	 */
	private long getInitialModifiedTime(long storedModifiedTime) {
		if (storedModifiedTime != 0) {
			return storedModifiedTime;
		}
		long legacyModifiedTime = getLegacyLocalModifiedTime();
		return legacyModifiedTime == 0 && !items.isEmpty() ? System.currentTimeMillis() : legacyModifiedTime;
	}

	private long getLegacyLocalModifiedTime() {
		long lastModifiedTime = 0;
		for (ITileSource source : items) {
			File file = null;
			if (source instanceof SQLiteTileSource) {
				file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName() + IndexConstants.SQLITE_EXT);
			} else if (source instanceof TileSourceManager.TileSourceTemplate) {
				file = new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName()), ".metainfo");
			}
			if (file != null) {
				lastModifiedTime = Math.max(lastModifiedTime, file.lastModified());
			}
		}
		return lastModifiedTime;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		persistBackupState(getStateHash(items), lastModifiedTime);
	}

	private void persistBackupState(@NonNull String stateHash, long lastModifiedTime) {
		OsmandSettings settings = app.getSettings();
		// Write the hash last. An interrupted update may then cause a harmless repeat
		// detection, but cannot acknowledge a state whose timestamp was not stored.
		settings.MAP_SOURCES_LOCAL_MODIFIED_TIME.set(lastModifiedTime);
		settings.MAP_SOURCES_BACKUP_STATE_HASH.set(stateHash);
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
					template = SQLiteTileSource.fromBackup(app, name, minZoom, maxZoom, url, randoms, ellipsoid, invertedY,
							referer, userAgent, timeSupported, expire, inversiveZoom, rule, ext, tileSize, bitDensity, avgSize);
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
				for (MapSourceBackupState state : snapshotSources(items)) {
					jsonArray.put(state.toJson());
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
	static String getStateHash(@NonNull List<? extends ITileSource> sources) {
		StringBuilder state = new StringBuilder();
		List<MapSourceBackupState> sourceStates = snapshotSources(sources);
		sourceStates.sort(Comparator.comparing(sourceState -> sourceState.canonicalState));
		for (MapSourceBackupState sourceState : sourceStates) {
			appendField(state, sourceState.canonicalState);
		}
		return DigestUtils.sha256Hex(state.toString());
	}

	@NonNull
	private static List<MapSourceBackupState> snapshotSources(@NonNull List<? extends ITileSource> sources) {
		List<MapSourceBackupState> states = new ArrayList<>(sources.size());
		for (ITileSource source : sources) {
			states.add(new MapSourceBackupState(source));
		}
		return states;
	}

	private static void appendField(@NonNull StringBuilder state, @Nullable Object value) {
		String stringValue = value != null ? String.valueOf(value) : "";
		state.append(stringValue.length()).append(':').append(stringValue).append('|');
	}

	private static final class MapSourceBackupState {

		private final SortedMap<String, BackupProperty> properties = new TreeMap<>();
		private final String canonicalState;

		private MapSourceBackupState(@NonNull ITileSource source) {
			boolean sqlite = source instanceof SQLiteTileSource;
			if (sqlite) {
				// Load the SQLite metadata before reading any getters. Otherwise lazily
				// initialized fields such as zoom limits could be captured as defaults.
				((SQLiteTileSource) source).initDatabaseIfNeeded();
			}
			put("sql", sqlite);
			put("name", normalizeString(source.getName()));
			put("minZoom", source.getMinimumZoomSupported());
			put("maxZoom", source.getMaximumZoomSupported());
			put("url", normalizeString(source.getUrlTemplate()));
			put("randoms", normalizeString(source.getRandoms()));
			put("ellipsoid", source.isEllipticYTile());
			put("inverted_y", source.isInvertedYTile());
			put("referer", normalizeString(source.getReferer()));
			put("userAgent", normalizeString(source.getUserAgent()));
			put("timesupported", source.isTimeSupported());
			put("expire", source.getExpirationTimeMinutes());
			put("inversiveZoom", source.getInversiveZoom());
			put("ext", normalizeString(source.getTileFormat()));
			// Legacy SQLite sources can discover and persist this value when a tile is
			// decoded. Keep it in backup, but do not treat derived runtime metadata as
			// a semantic source change. Directory-source tile size remains semantic.
			put("tileSize", source.getTileSize(), !sqlite);
			put("bitDensity", source.getBitDensity());
			put("avgSize", source.getAvgSize());
			put("rule", normalizeString(source.getRule()));
			canonicalState = createCanonicalState();
		}

		private void put(@NonNull String name, @NonNull Object value) {
			put(name, value, true);
		}

		private void put(@NonNull String name, @NonNull Object value, boolean hashRelevant) {
			properties.put(name, new BackupProperty(value, hashRelevant));
		}

		@NonNull
		private JSONObject toJson() throws JSONException {
			JSONObject json = new JSONObject();
			for (Map.Entry<String, BackupProperty> property : properties.entrySet()) {
				json.put(property.getKey(), property.getValue().value);
			}
			return json;
		}

		@NonNull
		private String createCanonicalState() {
			StringBuilder state = new StringBuilder();
			for (Map.Entry<String, BackupProperty> entry : properties.entrySet()) {
				BackupProperty property = entry.getValue();
				if (!property.hashRelevant) {
					continue;
				}
				appendField(state, entry.getKey());
				Object value = property.value;
				appendField(state, value instanceof Boolean ? "boolean" : value instanceof Number ? "number" : "string");
				appendField(state, value);
			}
			return state.toString();
		}

		@NonNull
		private static String normalizeString(@Nullable String value) {
			return value != null ? value : "";
		}

		private static final class BackupProperty {

			private final Object value;
			private final boolean hashRelevant;

			private BackupProperty(@NonNull Object value, boolean hashRelevant) {
				this.value = value;
				this.hashRelevant = hashRelevant;
			}
		}
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
