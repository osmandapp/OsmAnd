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
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
		for (ITileSource source : items) {
			if (source instanceof SQLiteTileSource) {
				File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName() + IndexConstants.SQLITE_EXT);
				if (file.lastModified() > lastModifiedTime) {
					file.setLastModified(lastModifiedTime);
				}
			} else if (source instanceof TileSourceManager.TileSourceTemplate) {
				File file = new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR + source.getName()), ".metainfo");
				if (file.lastModified() > lastModifiedTime) {
					file.setLastModified(lastModifiedTime);
				}
			}
		}
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
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);

			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
		return json;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader();
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
