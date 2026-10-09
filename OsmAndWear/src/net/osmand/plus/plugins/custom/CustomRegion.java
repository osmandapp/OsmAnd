package net.osmand.plus.plugins.custom;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.ui.DownloadDescriptionInfo;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.plus.utils.JsonUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CustomRegion extends WorldRegion {

	public static final int INVALID_ID = -1;

	private static final Log LOG = PlatformUtil.getLog(CustomRegion.class);

	private final String scopeId;
	private final String path;
	private String parentPath;
	private final String type;
	private String subfolder;

	private JSONArray downloadItemsJson;
	private JSONArray dynamicItemsJson;

	private DynamicDownloadItems dynamicDownloadItems;

	private DownloadDescriptionInfo descriptionInfo;

	private Map<String, String> names = new HashMap<>();
	private Map<String, String> icons = new HashMap<>();
	private Map<String, String> headers = new HashMap<>();

	private int headerColor = INVALID_ID;


	private CustomRegion(String scopeId, String path, String type) {
		super(path, null);
		this.scopeId = scopeId;
		this.path = path;
		this.type = type;
	}

	public String getScopeId() {
		return scopeId;
	}

	public String getPath() {
		return path;
	}

	public String getParentPath() {
		return parentPath;
	}

	@ColorInt
	public int getHeaderColor() {
		if (headerColor != INVALID_ID) {
			return headerColor;
		} else if (superregion instanceof CustomRegion) {
			return ((CustomRegion) superregion).getHeaderColor();
		}
		return headerColor;
	}

	@Nullable
	public DownloadDescriptionInfo getDescriptionInfo() {
		return descriptionInfo;
	}

	public String getIconName(Context ctx) {
		return JsonUtils.getLocalizedResFromMap(ctx, icons, null);
	}

	public static CustomRegion fromJson(@NonNull Context ctx, JSONObject object) throws JSONException {
		String scopeId = object.optString("scope-id", null);
		String path = object.optString("path", null);
		String type = object.optString("type", null);

		CustomRegion region = new CustomRegion(scopeId, path, type);
		region.subfolder = object.optString("subfolder", null);

		int index = path.lastIndexOf(File.separator);
		if (index != -1) {
			region.parentPath = path.substring(0, index);
		}

		region.names = JsonUtils.getLocalizedMapFromJson("name", object);
		if (!Algorithms.isEmpty(region.names)) {
			region.regionName = region.names.get("");
			region.regionNameEn = region.names.get("en");
			region.regionFullName = region.names.get("");
			region.regionNameLocale = JsonUtils.getLocalizedResFromMap(ctx, region.names, region.regionName);
		}

		region.icons = JsonUtils.getLocalizedMapFromJson("icon", object);
		region.headers = JsonUtils.getLocalizedMapFromJson("header", object);

		region.downloadItemsJson = object.optJSONArray("items");
		region.dynamicItemsJson = object.optJSONArray("dynamic-items");

		JSONObject urlItemsJson = object.optJSONObject("items-url");
		if (urlItemsJson != null) {
			region.dynamicDownloadItems = DynamicDownloadItems.fromJson(urlItemsJson);
		}

		String headerColor = object.optString("header-color", null);
		try {
			region.headerColor = Algorithms.isEmpty(headerColor) ? INVALID_ID : Algorithms.parseColor(headerColor);
		} catch (IllegalArgumentException e) {
			region.headerColor = INVALID_ID;
		}
		region.descriptionInfo = DownloadDescriptionInfo.fromJson(object.optJSONObject("description"));

		return region;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();

		jsonObject.putOpt("scope-id", scopeId);
		jsonObject.putOpt("path", path);
		jsonObject.putOpt("type", type);
		jsonObject.putOpt("subfolder", subfolder);

		JsonUtils.writeLocalizedMapToJson("name", jsonObject, names);
		JsonUtils.writeLocalizedMapToJson("icon", jsonObject, icons);
		JsonUtils.writeLocalizedMapToJson("header", jsonObject, headers);

		if (headerColor != INVALID_ID) {
			jsonObject.putOpt("header-color", Algorithms.colorToString(headerColor));
		}
		if (descriptionInfo != null) {
			jsonObject.putOpt("description", descriptionInfo.toJson());
		}
		jsonObject.putOpt("items", downloadItemsJson);
		jsonObject.putOpt("dynamic-items", dynamicItemsJson);
		if (dynamicDownloadItems != null) {
			jsonObject.putOpt("items-url", dynamicDownloadItems.toJson());
		}
		return jsonObject;
	}

	@NonNull
	public List<IndexItem> loadIndexItems() {
		List<IndexItem> items = new ArrayList<>();
		items.addAll(loadIndexItems(downloadItemsJson));
		items.addAll(loadIndexItems(dynamicItemsJson));
		return items;
	}

	@NonNull
	private List<IndexItem> loadIndexItems(@Nullable JSONArray itemsJson) {
		List<IndexItem> items = new ArrayList<>();
		if (itemsJson != null) {
			try {
				for (int i = 0; i < itemsJson.length(); i++) {
					JSONObject itemJson = itemsJson.getJSONObject(i);

					long timestamp = itemJson.optLong("timestamp") * 1000;
					long contentSize = itemJson.optLong("contentSize");
					long containerSize = itemJson.optLong("containerSize");

					boolean isHidden = itemJson.optBoolean("isHidden", false);
					String indexType = itemJson.optString("type", type);
					String fileName = itemJson.optString("filename");
					String downloadUrl = itemJson.optString("downloadurl");
					String size = new DecimalFormat("#.#").format(containerSize / (1024f * 1024f));

					Map<String, String> indexNames = JsonUtils.getLocalizedMapFromJson("name", itemJson);
					Map<String, String> firstSubNames = JsonUtils.getLocalizedMapFromJson("firstsubname", itemJson);
					Map<String, String> secondSubNames = JsonUtils.getLocalizedMapFromJson("secondsubname", itemJson);

					DownloadDescriptionInfo descriptionInfo = DownloadDescriptionInfo.fromJson(itemJson.optJSONObject("description"));

					DownloadActivityType type = DownloadActivityType.getIndexType(indexType);
					if (type != null) {
						IndexItem indexItem = new CustomIndexItem.CustomIndexItemBuilder()
								.setFileName(fileName)
								.setSubfolder(subfolder)
								.setDownloadUrl(downloadUrl)
								.setNames(indexNames)
								.setFirstSubNames(firstSubNames)
								.setSecondSubNames(secondSubNames)
								.setDescriptionInfo(descriptionInfo)
								.setTimestamp(timestamp)
								.setSize(size)
								.setContentSize(contentSize)
								.setContainerSize(containerSize)
								.setType(type)
								.setIsHidden(isHidden)
								.create();

						items.add(indexItem);
					}
				}
			} catch (JSONException e) {
				LOG.error(e);
			}
		}
		return items;
	}

	public void loadDynamicIndexItems(OsmandApplication app) {
		if (dynamicItemsJson == null && dynamicDownloadItems != null
				&& !Algorithms.isEmpty(dynamicDownloadItems.url)
				&& app.getSettings().isInternetConnectionAvailable()) {
			OnRequestResultListener resultListener = new OnRequestResultListener() {
				@Override
				public void onResult(@Nullable String result, @Nullable String error, @Nullable Integer resultCode) {
					if (!Algorithms.isEmpty(result)) {
						if ("json".equalsIgnoreCase(dynamicDownloadItems.format)) {
							dynamicItemsJson = mapJsonItems(result);
						}
						app.getDownloadThread().runReloadIndexFilesSilent();
					}
				}
			};

			AndroidNetworkUtils.sendRequestAsync(app, dynamicDownloadItems.url, null,
					null, false, false, resultListener);
		}
	}

	private JSONArray mapJsonItems(String jsonStr) {
		try {
			JSONObject json = new JSONObject(jsonStr);
			JSONArray jsonArray = json.optJSONArray(dynamicDownloadItems.itemsPath);
			if (jsonArray != null) {
				JSONArray itemsJson = new JSONArray();
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					JSONObject itemJson = mapDynamicJsonItem(jsonObject, dynamicDownloadItems.mapping);

					itemsJson.put(itemJson);
				}
				return itemsJson;
			}
		} catch (JSONException e) {
			LOG.error(e);
		}
		return null;
	}

	private JSONObject mapDynamicJsonItem(JSONObject jsonObject, JSONObject mapping) throws JSONException {
		JSONObject itemJson = new JSONObject();
		for (Iterator<String> it = mapping.keys(); it.hasNext(); ) {
			String key = it.next();
			Object value = checkMappingValue(mapping.opt(key), jsonObject);
			itemJson.put(key, value);
		}
		return itemJson;
	}

	private Object checkMappingValue(Object value, JSONObject json) throws JSONException {
		if (value instanceof String) {
			String key = (String) value;
			int index = key.indexOf("@");
			if (index != INVALID_ID) {
				key = key.substring(index + 1);
			}
			return json.opt(key);
		} else if (value instanceof JSONObject) {
			JSONObject checkedJsonObject = (JSONObject) value;
			JSONObject objectJson = new JSONObject();

			for (Iterator<String> iterator = checkedJsonObject.keys(); iterator.hasNext(); ) {
				String key = iterator.next();
				Object checkedValue = checkMappingValue(checkedJsonObject.opt(key), json);
				objectJson.put(key, checkedValue);
			}
			return objectJson;
		} else if (value instanceof JSONArray) {
			JSONArray checkedJsonArray = new JSONArray();
			JSONArray jsonArray = (JSONArray) value;

			for (int i = 0; i < jsonArray.length(); i++) {
				Object checkedValue = checkMappingValue(jsonArray.opt(i), json);
				checkedJsonArray.put(i, checkedValue);
			}
			return checkedJsonArray;
		}
		return value;
	}

	public static class DynamicDownloadItems {

		public final String url;
		public final String format;
		public final String itemsPath;
		public final JSONObject mapping;

		public DynamicDownloadItems(@NonNull String url, @NonNull String format,
		                            @NonNull String itemsPath, @Nullable JSONObject mapping) {
			this.url = url;
			this.format = format;
			this.itemsPath = itemsPath;
			this.mapping = mapping;
		}

		@NonNull
		public static DynamicDownloadItems fromJson(@NonNull JSONObject object) {
			String url = object.optString("url");
			String format = object.optString("format");
			String itemsPath = object.optString("items-path");
			JSONObject mapping = object.optJSONObject("mapping");

			return new DynamicDownloadItems(url, format, itemsPath, mapping);
		}

		@NonNull
		public JSONObject toJson() throws JSONException {
			JSONObject jsonObject = new JSONObject();

			jsonObject.putOpt("url", url);
			jsonObject.putOpt("format", format);
			jsonObject.putOpt("items-path", itemsPath);
			jsonObject.putOpt("mapping", mapping);

			return jsonObject;
		}
	}
}