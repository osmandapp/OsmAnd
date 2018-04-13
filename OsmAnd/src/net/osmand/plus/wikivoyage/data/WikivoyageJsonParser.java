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
	public static ContentsContainer parseJsonContents(String contentsJson) {

		JSONObject jArray;
		JSONObject reader;

		try {
			reader = new JSONObject(contentsJson);
			jArray = reader.getJSONObject(HEADERS);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		ContentsContainer topContentsContainer = new ContentsContainer(HEADERS, null);
		for (int i = 0; i < jArray.length(); i++) {
			try {
				String link = "";
				JSONObject jsonHeader = jArray.getJSONObject(jArray.names().getString(i));
				link = jsonHeader.getString(LINK);
				ContentsContainer contentsHeaderContainer = new ContentsContainer(jArray.names().getString(i), link);
				topContentsContainer.childItems.add(contentsHeaderContainer);
				contentsHeaderContainer.setParent(topContentsContainer);

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

					ContentsContainer contentsSubHeaderContainer = new ContentsContainer(jsonSubheader.names().getString(0), link);
					contentsHeaderContainer.childItems.add(contentsSubHeaderContainer);
					contentsSubHeaderContainer.setParent(topContentsContainer);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return topContentsContainer;
	}

	public static class ContentsContainer {

		ArrayList<ContentsContainer> childItems = new ArrayList<>();
		ContentsContainer parent;

		String name;
		String link;

		public ContentsContainer(String name, String link) {
			this.name = name;
			this.link = link;
		}

		public ArrayList<ContentsContainer> getChildItems() {
			return childItems;
		}

		public void setChildItems(ArrayList<ContentsContainer> childItems) {
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

		public ContentsContainer getParent() {
			return parent;
		}

		public void setParent(ContentsContainer parent) {
			this.parent = parent;
		}
	}
}
