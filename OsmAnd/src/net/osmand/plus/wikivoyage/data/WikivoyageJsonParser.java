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

		WikivoyageContentItem topWikivoyageContentItem = new WikivoyageContentItem(HEADERS, null);
		for (int i = 0; i < jArray.length(); i++) {
			try {
				String link = "";
				JSONObject jsonHeader = jArray.getJSONObject(jArray.names().getString(i));
				link = jsonHeader.getString(LINK);
				WikivoyageContentItem contentHeaderItem = new WikivoyageContentItem(jArray.names().getString(i), link, topWikivoyageContentItem);
				topWikivoyageContentItem.subItems.add(contentHeaderItem);

				JSONArray jsonSubheaders = jsonHeader.getJSONArray(SUBHEADERS);
				List<String> subheaderNames = null;
				for (int j = 0; j < jsonSubheaders.length(); j++) {
					JSONObject jsonSubheader = jsonSubheaders.getJSONObject(j);
					JSONObject jsonSubheaderLink = jsonSubheader.getJSONObject(jsonSubheader.keys().next());
					if (subheaderNames == null) {
						subheaderNames = new ArrayList<>();
					}
					subheaderNames.add(jsonSubheader.keys().next());
					link = jsonSubheaderLink.getString(LINK);

					WikivoyageContentItem contentsSubHeaderContainer = new WikivoyageContentItem(jsonSubheader.names().getString(0), link, contentHeaderItem);
					contentHeaderItem.subItems.add(contentsSubHeaderContainer);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return topWikivoyageContentItem;
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
