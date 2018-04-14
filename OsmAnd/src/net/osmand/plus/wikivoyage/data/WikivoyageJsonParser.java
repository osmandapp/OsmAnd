package net.osmand.plus.wikivoyage.data;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WikivoyageJsonParser {

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
			e.printStackTrace();
			return null;
		}

		WikivoyageContentItem topContentItem = new WikivoyageContentItem(HEADERS, null);
		for (int i = 0; i < jArray.length(); i++) {
			try {
				JSONObject header = jArray.getJSONObject(jArray.names().getString(i));
				String link = header.getString(LINK);
				WikivoyageContentItem headerItem = new WikivoyageContentItem(jArray.names().getString(i), link, topContentItem);
				topContentItem.subItems.add(headerItem);

				JSONArray subheaders = header.getJSONArray(SUBHEADERS);
				List<String> subheaderNames = null;
				for (int j = 0; j < subheaders.length(); j++) {
					JSONObject subheader = subheaders.getJSONObject(j);
					JSONObject subheaderLink = subheader.getJSONObject(subheader.keys().next());
					if (subheaderNames == null) {
						subheaderNames = new ArrayList<>();
					}
					subheaderNames.add(subheader.keys().next());
					link = subheaderLink.getString(LINK);

					WikivoyageContentItem subheaderItem = new WikivoyageContentItem(subheader.names().getString(0), link, headerItem);
					headerItem.subItems.add(subheaderItem);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return topContentItem;
	}

	public static class WikivoyageContentItem {

		private String link;
		private String name;
		private ArrayList<WikivoyageContentItem> subItems = new ArrayList<>();
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
