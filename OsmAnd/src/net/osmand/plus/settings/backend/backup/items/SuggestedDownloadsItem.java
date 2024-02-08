package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.plugins.custom.CustomOsmandPlugin.SuggestedDownloadItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class SuggestedDownloadsItem extends SettingsItem {

	private static final int APPROXIMATE_SUGGESTED_DOWNLOAD_SIZE_BYTES = 120;

	private List<SuggestedDownloadItem> items;

	public SuggestedDownloadsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		items = new ArrayList<>();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.SUGGESTED_DOWNLOADS;
	}

	@NonNull
	@Override
	public String getName() {
		return "suggested_downloads";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.suggested_maps);
	}

	public List<SuggestedDownloadItem> getItems() {
		return items;
	}

	@Override
	public long getLocalModifiedTime() {
		return 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
	}

	@Override
	public long getEstimatedSize() {
		return APPROXIMATE_SUGGESTED_DOWNLOAD_SIZE_BYTES;
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
				String scopeId = object.optString("scope-id");
				String searchType = object.optString("search-type");
				int limit = object.optInt("limit", -1);

				List<String> names = new ArrayList<>();
				if (object.has("names")) {
					JSONArray namesArray = object.getJSONArray("names");
					for (int j = 0; j < namesArray.length(); j++) {
						names.add(namesArray.getString(j));
					}
				}
				SuggestedDownloadItem suggestedDownload = new SuggestedDownloadItem(scopeId, searchType, names, limit);
				items.add(suggestedDownload);
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
				for (SuggestedDownloadItem downloadItem : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("scope-id", downloadItem.getScopeId());
					if (downloadItem.getLimit() != -1) {
						jsonObject.put("limit", downloadItem.getLimit());
					}
					if (!Algorithms.isEmpty(downloadItem.getSearchType())) {
						jsonObject.put("search-type", downloadItem.getSearchType());
					}
					if (!Algorithms.isEmpty(downloadItem.getNames())) {
						JSONArray namesArray = new JSONArray();
						for (String downloadName : downloadItem.getNames()) {
							namesArray.put(downloadName);
						}
						jsonObject.put("names", namesArray);
					}
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
		return null;
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
