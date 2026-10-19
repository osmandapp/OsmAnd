package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.helpers.OsmBugsDbHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OsmNotesSettingsItem extends CollectionSettingsItem<OsmNotesPoint> {

	private static final int APPROXIMATE_OSM_NOTE_SIZE_BYTES = 250;

	public static final String TEXT_KEY = "text";
	public static final String LAT_KEY = "lat";
	public static final String LON_KEY = "lon";
	public static final String AUTHOR_KEY = "author";
	public static final String ACTION_KEY = "action";

	public OsmNotesSettingsItem(@NonNull OsmandApplication app, @NonNull List<OsmNotesPoint> items) {
		super(app, null, items);
	}

	public OsmNotesSettingsItem(@NonNull OsmandApplication app, @Nullable OsmNotesSettingsItem baseItem,
	                            @NonNull List<OsmNotesPoint> items) {
		super(app, baseItem, items);
	}

	public OsmNotesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			existingItems = osmEditingPlugin.getDBBug().getOsmBugsPoints();
		}
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.OSM_NOTES;
	}

	@Override
	public long getLocalModifiedTime() {
		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			return osmEditingPlugin.getDBBug().getLastModifiedTime();
		}
		return 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			osmEditingPlugin.getDBBug().setLastModifiedTime(lastModifiedTime);
		}
	}

	@Override
	public void apply() {
		List<OsmNotesPoint> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
			if (osmEditingPlugin != null) {
				OsmBugsDbHelper db = osmEditingPlugin.getDBBug();
				for (OsmNotesPoint duplicate : duplicateItems) {
					int ind = existingItems.indexOf(duplicate);
					if (ind != -1 && ind < existingItems.size()) {
						OsmNotesPoint original = existingItems.get(ind);
						if (original != null) {
							db.deleteAllBugModifications(original);
						}
						db.addOsmbugs(duplicate);
					}
				}

				for (OsmNotesPoint point : appliedItems) {
					db.addOsmbugs(point);
				}
			}
		}
	}

	@Override
	protected void deleteItem(OsmNotesPoint item) {
		// TODO: delete settings item
	}

	@Override
	public boolean isDuplicate(@NonNull OsmNotesPoint item) {
		return existingItems.contains(item);
	}

	@NonNull
	@Override
	public OsmNotesPoint renameItem(@NonNull OsmNotesPoint item) {
		return item;
	}

	@Override
	public long getEstimatedItemSize(@NonNull OsmNotesPoint item) {
		return APPROXIMATE_OSM_NOTE_SIZE_BYTES;
	}

	@NonNull
	@Override
	public String getName() {
		return "osm_notes";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.osm_notes);
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
			OsmEditingPlugin osmEditingPlugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
			long minId = osmEditingPlugin != null ? osmEditingPlugin.getDBBug().getMinID() - 1 : -2;
			int idOffset = 0;
			JSONArray jsonArray = json.getJSONArray("items");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				String text = object.optString(TEXT_KEY);
				double lat = object.getDouble(LAT_KEY);
				double lon = object.getDouble(LON_KEY);
				String author = object.optString(AUTHOR_KEY);
				author = author.isEmpty() ? null : author;
				String action = object.getString(ACTION_KEY);
				OsmNotesPoint point = new OsmNotesPoint();
				point.setId(Math.min(-2, minId - idOffset));
				point.setText(text);
				point.setLatitude(lat);
				point.setLongitude(lon);
				point.setAuthor(author);
				point.setAction(action);
				items.add(point);
				idOffset++;
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
				for (OsmNotesPoint point : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put(TEXT_KEY, point.getText());
					jsonObject.put(LAT_KEY, point.getLatitude());
					jsonObject.put(LON_KEY, point.getLongitude());
					jsonObject.put(AUTHOR_KEY, point.getAuthor());
					jsonObject.put(ACTION_KEY, OsmPoint.stringAction.get(point.getAction()));
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
