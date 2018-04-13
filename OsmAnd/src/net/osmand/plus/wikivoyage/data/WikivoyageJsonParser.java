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
	public static ContentsItem parseJsonContents(String contentsJson) {

		JSONObject jArray;
		JSONObject reader;

		try {
			reader = new JSONObject(contentsJson);
			jArray = reader.getJSONObject(HEADERS);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		ContentsItem topContentsItem = new ContentsItem(HEADERS, null);
		for (int i = 0; i < jArray.length(); i++) {
			try {
				String link = "";
				JSONObject jsonHeader = jArray.getJSONObject(jArray.names().getString(i));
				link = jsonHeader.getString(LINK);
				ContentsItem contentsHeaderItem = new ContentsItem(jArray.names().getString(i), link);
				topContentsItem.childItems.add(contentsHeaderItem);
				contentsHeaderItem.setParent(topContentsItem);

				JSONArray jsonSubheaders = jsonHeader.getJSONArray(SUBHEADERS);
				List<String> subheaderNames = null;
				for (int j = 0; j < jsonSubheaders.length(); j++) {
					JSONObject jsonSubheader = jsonSubheaders.getJSONObject(j);
					JSONObject jsonSubheaderLink = jsonSubheader.getJSONObject(jsonSubheader.names().getString(0));
					if (subheaderNames == null) {
						subheaderNames = new ArrayList<>();
					}
					subheaderNames.add(jsonSubheader.names().getString(0));
					link = jsonSubheaderLink.getString(LINK);

					ContentsItem contentsSubHeaderContainer = new ContentsItem(jsonSubheader.names().getString(0), link);
					contentsHeaderItem.childItems.add(contentsSubHeaderContainer);
					contentsSubHeaderContainer.setParent(topContentsItem);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return topContentsItem;
	}

	public static class ContentsItem {

		private ArrayList<ContentsItem> childItems = new ArrayList<>();
		private ContentsItem parent;

		private String name;
		private String link;

		public ContentsItem(String name, String link) {
			this.name = name;
			this.link = link;
		}

		public ArrayList<ContentsItem> getChildItems() {
			return childItems;
		}

		public void setChildItems(ArrayList<ContentsItem> childItems) {
			this.childItems = childItems;
		}


		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLink() {
			return link;
		}

		public void setLink(String link) {
			this.link = link;
		}

		public ContentsItem getParent() {
			return parent;
		}

		public void setParent(ContentsItem parent) {
			this.parent = parent;
		}
	}
}
