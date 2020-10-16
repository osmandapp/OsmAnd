package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.osmedit.OsmPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OsmNotesSettingsItem extends CollectionSettingsItem<OsmNotesPoint> {

	public OsmNotesSettingsItem(@NonNull OsmandApplication app, @NonNull List<OsmNotesPoint> items) {
		super(app, null, items);
	}

	@Override
	protected void init() {
		super.init();
		OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			existingItems = osmEditingPlugin.getDBBug().getOsmbugsPoints();
		}
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.OSM_NOTES;
	}

	@Override
	public void apply() {
		List<OsmNotesPoint> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (OsmNotesPoint duplicate : duplicateItems) {
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull OsmNotesPoint item) {
		return false;
	}

	@NonNull
	@Override
	public OsmNotesPoint renameItem(@NonNull OsmNotesPoint item) {
		return item;
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
				for (OsmNotesPoint point : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("text", point.getText());
					jsonObject.put("lat", point.getLatitude());
					jsonObject.put("lon", point.getLongitude());
					jsonObject.put("action", OsmPoint.stringAction.get(point.getAction()));
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
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
