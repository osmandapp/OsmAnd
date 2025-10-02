package net.osmand.plus.download.ui;

import android.content.Context;
import android.text.Html;

import net.osmand.plus.utils.JsonUtils;
import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DownloadDescriptionInfo {

	private static final Log LOG = PlatformUtil.getLog(DownloadDescriptionInfo.class);

	private JSONArray buttonsJson;
	private List<String> imageUrls;
	private Map<String, String> localizedDescription;

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public CharSequence getLocalizedDescription(Context ctx) {
		String description = JsonUtils.getLocalizedResFromMap(ctx, localizedDescription, null);
		return description != null ? Html.fromHtml(description) : null;
	}

	public List<ActionButton> getActionButtons(Context ctx) {
		List<ActionButton> actionButtons = new ArrayList<>();
		if (buttonsJson != null) {
			try {
				for (int i = 0; i < buttonsJson.length(); i++) {
					String url = null;
					String actionType = null;

					JSONObject object = buttonsJson.getJSONObject(i);
					if (object.has("url")) {
						url = object.optString("url");
					} else if (object.has("action")) {
						actionType = object.optString("action");
					}
					Map<String, String> localizedMap = JsonUtils.getLocalizedMapFromJson(object);
					String name = JsonUtils.getLocalizedResFromMap(ctx, localizedMap, null);

					ActionButton actionButton = new ActionButton(actionType, name, url);
					actionButtons.add(actionButton);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return actionButtons;
	}

	public static DownloadDescriptionInfo fromJson(JSONObject json) {
		if (json != null) {
			DownloadDescriptionInfo downloadDescriptionInfo = new DownloadDescriptionInfo();
			try {
				downloadDescriptionInfo.localizedDescription = JsonUtils.getLocalizedMapFromJson("text", json);
				downloadDescriptionInfo.imageUrls = JsonUtils.jsonArrayToList("image", json);
				downloadDescriptionInfo.buttonsJson = json.optJSONArray("button");
			} catch (JSONException e) {
				LOG.error(e);
			}
			return downloadDescriptionInfo;
		}
		return null;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject descrJson = new JSONObject();

		JsonUtils.writeLocalizedMapToJson("text", descrJson, localizedDescription);
		JsonUtils.writeStringListToJson("image", descrJson, imageUrls);

		descrJson.putOpt("button", buttonsJson);

		return descrJson;
	}

	public static class ActionButton {

		public static final String DOWNLOAD_ACTION = "download";

		private final String actionType;
		private final String name;
		private final String url;

		public ActionButton(String actionType, String name, String url) {
			this.actionType = actionType;
			this.name = name;
			this.url = url;
		}

		public String getActionType() {
			return actionType;
		}

		public String getName() {
			return name;
		}

		public String getUrl() {
			return url;
		}
	}
}