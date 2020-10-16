package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.osm.edit.Entity;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OsmEditsSettingsItem extends CollectionSettingsItem<OpenstreetmapPoint> {

	public OsmEditsSettingsItem(@NonNull OsmandApplication app, @NonNull List<OpenstreetmapPoint> items) {
		super(app, null, items);
	}

	@Override
	protected void init() {
		super.init();
		OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
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
	public void apply() {
		List<OpenstreetmapPoint> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (OpenstreetmapPoint duplicate : duplicateItems) {
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull OpenstreetmapPoint item) {
		return false;
	}

	@NonNull
	@Override
	public OpenstreetmapPoint renameItem(@NonNull OpenstreetmapPoint item) {
		return item;
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
	public boolean shouldReadOnCollecting() {
		return true;
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
	}

	@Override
	void writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		if (!items.isEmpty()) {
			try {
				for (OpenstreetmapPoint point : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", point.getTagsString());
					jsonObject.put("lat", point.getLatitude());
					jsonObject.put("lon", point.getLongitude());
					jsonObject.put("action", OsmPoint.stringAction.get(point.getAction()));
					jsonArray.put(jsonObject);
					jsonArray.put(writeTags(point.getEntity()));
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
	}

	private JSONArray writeTags(Entity entity) {
		JSONArray tagList = new JSONArray();
		for (String tag : entity.getTagKeySet()) {
			String val = entity.getTag(tag);
			if (entity.isNotValid(tag)) {
				continue;
			}
			try {
				JSONObject tagItem = new JSONObject();
				tagItem.put("k", tag);
				tagItem.put("v", val);
				tagList.put(tagItem);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return tagList;
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader();
	}

	@Nullable
	@Override
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		return null;
	}
}
