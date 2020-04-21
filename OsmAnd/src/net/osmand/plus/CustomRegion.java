package net.osmand.plus;

import androidx.annotation.ColorInt;

import net.osmand.JsonUtils;
import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;
import net.osmand.plus.download.CustomIndexItem;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRegion extends WorldRegion {

	private static final Log LOG = PlatformUtil.getLog(CustomRegion.class);

	private String scopeId;
	private String path;
	private String parentPath;
	private String type;
	private String subfolder;
	private String headerButton;

	private JSONArray downloadItemsJson;

	private Map<String, String> names = new HashMap<>();
	private Map<String, String> icons = new HashMap<>();
	private Map<String, String> headers = new HashMap<>();

	private int headerColor = -1;


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
		return headerColor;
	}

	public static CustomRegion fromJson(JSONObject object) throws JSONException {
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
			region.regionNameEn = region.names.get("");
			region.regionFullName = region.names.get("");
			region.regionNameLocale = region.names.get("");
		}

		region.icons = JsonUtils.getLocalizedMapFromJson("icon", object);
		region.headers = JsonUtils.getLocalizedMapFromJson("header", object);

		region.headerButton = object.optString("header-button", null);
		region.downloadItemsJson = object.optJSONArray("items");

		String headerColor = object.optString("header-color", null);
		try {
			region.headerColor = Algorithms.isEmpty(headerColor) ? 0 : Algorithms.parseColor(headerColor);
		} catch (IllegalArgumentException e) {
			region.headerColor = 0;
		}

		return region;
	}

	public List<IndexItem> loadIndexItems() {
		List<IndexItem> items = new ArrayList<>();
		if (downloadItemsJson != null) {
			try {
				for (int i = 0; i < downloadItemsJson.length(); i++) {
					JSONObject itemJson = downloadItemsJson.getJSONObject(i);

					long timestamp = itemJson.optLong("timestamp") * 1000;
					long contentSize = itemJson.optLong("contentSize");
					long containerSize = itemJson.optLong("containerSize");

					String indexType = itemJson.optString("type", type);
					String webUrl = itemJson.optString("weburl");
					String fileName = itemJson.optString("filename");
					String downloadUrl = itemJson.optString("downloadurl");
					String size = new DecimalFormat("#.#").format(containerSize / (1024f * 1024f));

					List<String> descrImageUrl = JsonUtils.jsonArrayToList("image-description-url", itemJson);
					Map<String, String> indexNames = JsonUtils.getLocalizedMapFromJson("name", itemJson);
					Map<String, String> descriptions = JsonUtils.getLocalizedMapFromJson("description", itemJson);
					Map<String, String> webButtonText = JsonUtils.getLocalizedMapFromJson("web-button-text", itemJson);

					DownloadActivityType type = DownloadActivityType.getIndexType(indexType);
					if (type != null) {
						IndexItem indexItem = new CustomIndexItem.CustomIndexItemBuilder()
								.setFileName(fileName)
								.setSubfolder(subfolder)
								.setDownloadUrl(downloadUrl)
								.setNames(indexNames)
								.setDescriptions(descriptions)
								.setImageDescrUrl(descrImageUrl)
								.setWebUrl(webUrl)
								.setWebButtonText(webButtonText)
								.setTimestamp(timestamp)
								.setSize(size)
								.setContentSize(contentSize)
								.setContainerSize(containerSize)
								.setType(type)
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

	public JSONObject toJson() throws JSONException {
		JSONObject jsonObject = new JSONObject();

		jsonObject.putOpt("scope-id", scopeId);
		jsonObject.putOpt("path", path);
		jsonObject.putOpt("type", type);
		jsonObject.putOpt("subfolder", subfolder);
		jsonObject.putOpt("header-button", headerButton);

		JsonUtils.writeLocalizedMapToJson("name", jsonObject, names);
		JsonUtils.writeLocalizedMapToJson("icon", jsonObject, icons);
		JsonUtils.writeLocalizedMapToJson("header", jsonObject, headers);

		jsonObject.putOpt("items", downloadItemsJson);

		return jsonObject;
	}
}