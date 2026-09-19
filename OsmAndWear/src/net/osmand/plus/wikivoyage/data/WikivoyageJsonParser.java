package net.osmand.plus.wikivoyage.data;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageJsonParser {

	private static final String TAG = WikivoyageJsonParser.class.getSimpleName();

	private static final String HEADERS = "headers";
	private static final String SUBHEADERS = "subheaders";
	private static final String LINK = "link";

	@Nullable
	public static WikivoyageContentItem parseJsonContents(String contentsJson) {

		JSONObject jArray;
		JSONObject reader;

		try {
			reader = new JSONObject(contentsJson);
			jArray = reader.getJSONObject(HEADERS);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
			return null;
		}

		WikivoyageContentItem topContentItem = new WikivoyageContentItem(HEADERS, null);
		for (int i = 0; i < jArray.length(); i++) {
			try {
				JSONObject header = jArray.getJSONObject(jArray.names().getString(i));
				String link = header.getString(LINK);
				WikivoyageContentItem headerItem = new WikivoyageContentItem(jArray.names().getString(i), link, topContentItem);
				topContentItem.subItems.add(headerItem);

				JSONObject subheaders = header.getJSONObject(SUBHEADERS);
				JSONArray subNames = subheaders.names();
				List<String> subheaderNames = null;
				for (int j = 0; j < subheaders.length(); j++) {
					String title = subNames.get(j).toString();
					JSONObject subheaderLink = subheaders.getJSONObject(title);
					if (subheaderNames == null) {
						subheaderNames = new ArrayList<>();
					}
					subheaderNames.add(title);
					link = subheaderLink.getString(LINK);

					WikivoyageContentItem subheaderItem = new WikivoyageContentItem(title, link, headerItem);
					headerItem.subItems.add(subheaderItem);
				}
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
		return topContentItem;
	}

	public static class WikivoyageContentItem {

		private final String link;
		private final String name;
		private final ArrayList<WikivoyageContentItem> subItems = new ArrayList<>();
		private WikivoyageContentItem parent;

		private WikivoyageContentItem(String name, String link) {
			this.name = name;
			this.link = link;
		}

		private WikivoyageContentItem(String name, String link, WikivoyageContentItem parent) {
			this.parent = parent;
			this.name = name;
			this.link = link;
		}

		public String getName() {
			return name;
		}

		public String getLink() {
			return link;
		}

		public WikivoyageContentItem getParent() {
			return parent;
		}

		public ArrayList<WikivoyageContentItem> getSubItems() {
			return subItems;
		}

	}
}
