package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapsDbHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmEditsSettingsItem extends CollectionSettingsItem<OpenstreetmapPoint> {

	private static final int APPROXIMATE_OSM_EDIT_SIZE_BYTES = 500;

	public static final String ID_KEY = "id";
	public static final String NAME_KEY = "name";
	public static final String LAT_KEY = "lat";
	public static final String LON_KEY = "lon";
	public static final String COMMENT_KEY = "comment";
	public static final String ACTION_KEY = "action";
	public static final String TYPE_KEY = "type";
	public static final String TAGS_KEY = "tags";
	public static final String ENTITY_KEY = "entity";

	public OsmEditsSettingsItem(@NonNull OsmandApplication app, @NonNull List<OpenstreetmapPoint> items) {
		super(app, null, items);
	}

	public OsmEditsSettingsItem(@NonNull OsmandApplication app, @Nullable OsmEditsSettingsItem baseItem,
	                            @NonNull List<OpenstreetmapPoint> items) {
		super(app, baseItem, items);
	}

	public OsmEditsSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			existingItems = osmEditingPlugin.getDBPOI().getOpenstreetmapPoints();
		}
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.OSM_EDITS;
	}

	@Override
	public long getLocalModifiedTime() {
		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			return osmEditingPlugin.getDBPOI().getLastModifiedTime();
		}
		return 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			osmEditingPlugin.getDBPOI().setLastModifiedTime(lastModifiedTime);
		}
	}

	@Override
	public void apply() {
		List<OpenstreetmapPoint> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
			if (osmEditingPlugin != null) {
				OpenstreetmapsDbHelper db = osmEditingPlugin.getDBPOI();
				for (OpenstreetmapPoint duplicate : duplicateItems) {
					db.deletePOI(duplicate);
					db.addOpenstreetmap(duplicate);
				}
				for (OpenstreetmapPoint point : appliedItems) {
					db.addOpenstreetmap(point);
				}
			}
		}
	}

	@Override
	protected void deleteItem(OpenstreetmapPoint item) {
		// TODO: delete settings item
	}

	@Override
	public boolean isDuplicate(@NonNull OpenstreetmapPoint item) {
		return existingItems.contains(item);
	}

	@NonNull
	@Override
	public OpenstreetmapPoint renameItem(@NonNull OpenstreetmapPoint item) {
		return item;
	}

	@Override
	public long getEstimatedItemSize(@NonNull OpenstreetmapPoint item) {
		return APPROXIMATE_OSM_EDIT_SIZE_BYTES;
	}

	@NonNull
	@Override
	public String getName() {
		return "osm_edits";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.osm_edits);
	}

	@Override
	public boolean shouldShowDuplicates() {
		return false;
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			JSONArray jsonArray = json.getJSONArray("items");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonPoint = jsonArray.getJSONObject(i);
				String comment = jsonPoint.optString(COMMENT_KEY);
				comment = comment.isEmpty() ? null : comment;
				JSONObject entityJson = jsonPoint.getJSONObject(ENTITY_KEY);
				long id = entityJson.getLong(ID_KEY);
				double lat = entityJson.getDouble(LAT_KEY);
				double lon = entityJson.getDouble(LON_KEY);
				String tags = entityJson.getString(TAGS_KEY);
				Map<String, String> tagMap = new Gson().fromJson(
						tags, new TypeToken<HashMap<String, String>>() {
						}.getType()
				);
				String action = entityJson.getString(ACTION_KEY);
				Entity entity;
				if (entityJson.get(TYPE_KEY).equals(Entity.EntityType.NODE.name())) {
					entity = new Node(lat, lon, id);
				} else {
					entity = new Way(id);
					entity.setLatitude(lat);
					entity.setLongitude(lon);
				}
				entity.replaceTags(tagMap);
				OpenstreetmapPoint point = new OpenstreetmapPoint();
				point.setComment(comment);
				point.setEntity(entity);
				point.setAction(action);
				items.add(point);
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
				for (OpenstreetmapPoint point : items) {
					JSONObject jsonPoint = new JSONObject();
					JSONObject jsonEntity = new JSONObject();
					jsonEntity.put(ID_KEY, point.getId());
					jsonEntity.put(NAME_KEY, point.getTagsString());
					jsonEntity.put(LAT_KEY, point.getLatitude());
					jsonEntity.put(LON_KEY, point.getLongitude());
					jsonEntity.put(TYPE_KEY, Entity.EntityType.valueOf(point.getEntity()));
					JSONObject jsonTags = new JSONObject(point.getEntity().getTags());
					jsonEntity.put(TAGS_KEY, jsonTags);
					jsonPoint.put(COMMENT_KEY, point.getComment());
					jsonEntity.put(ACTION_KEY, OsmPoint.stringAction.get(point.getAction()));
					jsonPoint.put(ENTITY_KEY, jsonEntity);
					jsonArray.put(jsonPoint);
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
