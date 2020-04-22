package net.osmand.plus.download.ui;

import android.content.Context;
import android.text.Html;

import net.osmand.JsonUtils;
import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DescriptionInfo {

	private static final Log LOG = PlatformUtil.getLog(DescriptionInfo.class);

	private JSONArray buttonsJson;
	private List<String> imageUrls;
	private Map<String, String> texts;

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public CharSequence getLocalizedDescription(Context ctx) {
		String description = JsonUtils.getLocalizedResFromMap(ctx, texts, null);
		return description != null ? Html.fromHtml(description) : null;
	}

	public List<DownloadActionButton> getDownloadActionButtons(Context ctx) {
		List<DownloadActionButton> downloadActionButtons = new ArrayList<>();
		if (buttonsJson != null) {
			try {
				for (int i = 0; i < buttonsJson.length(); i++) {
					DescriptionActionButtonType type = null;
					String url = null;

					JSONObject object = buttonsJson.getJSONObject(i);
					if (object.has("url")) {
						url = object.optString("url");
					} else if (object.has("action")) {
						String action = object.optString("action");
						for (DescriptionActionButtonType buttonType : DescriptionActionButtonType.values()) {
							if (buttonType.name().equalsIgnoreCase(action)) {
								type = buttonType;
							}
						}
					}
					Map<String, String> localizedMap = JsonUtils.getLocalizedMapFromJson(object);
					String name = JsonUtils.getLocalizedResFromMap(ctx, localizedMap, null);

					DownloadActionButton actionButton = new DownloadActionButton(type, name, url);
					downloadActionButtons.add(actionButton);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return downloadActionButtons;
	}

	public static DescriptionInfo fromJson(JSONObject json) {
		if (json != null) {
			DescriptionInfo descriptionInfo = new DescriptionInfo();
			try {
				descriptionInfo.texts = JsonUtils.getLocalizedMapFromJson("text", json);
				descriptionInfo.imageUrls = JsonUtils.jsonArrayToList("image", json);
				descriptionInfo.buttonsJson = json.optJSONArray("button");
			} catch (JSONException e) {
				LOG.error(e);
			}
			return descriptionInfo;
		}
		return null;
	}

	public JSONObject toJson() throws JSONException {
		JSONObject descrJson = new JSONObject();

		JsonUtils.writeLocalizedMapToJson("text", descrJson, texts);
		JsonUtils.writeStringListToJson("image", descrJson, imageUrls);

		descrJson.putOpt("button", buttonsJson);

		return descrJson;
	}

	public enum DescriptionActionButtonType {
		DOWNLOAD,
		URL
	}

	public static class DownloadActionButton {

		private DescriptionInfo.DescriptionActionButtonType type;
		private String name;
		private String url;

		public DownloadActionButton(DescriptionInfo.DescriptionActionButtonType type, String name, String url) {
			this.type = type;
			this.name = name;
			this.url = url;
		}

		public DescriptionInfo.DescriptionActionButtonType getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getUrl() {
			return url;
		}
	}
}