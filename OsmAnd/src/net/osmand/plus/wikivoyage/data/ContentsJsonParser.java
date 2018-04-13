package net.osmand.plus.wikivoyage.data;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ContentsJsonParser {

	@Nullable
	public static ContentsContainer parseJsonContents(String contentsJson) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		ArrayList<String> listDataHeader = new ArrayList<>();
		LinkedHashMap<String, List<String>> listDataChild = new LinkedHashMap<>();
		Container headers = new Container();

		JSONObject reader;
		try {
			reader = new JSONObject(contentsJson);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		List<String> secondLevel = null;
		JSONObject jArray = null;
		try {
			jArray = reader.getJSONObject("headers");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < jArray.length(); i++) {
			try {

				String link = "";
				JSONObject header = jArray.getJSONObject(jArray.names().getString(i));
				link = header.getString("link");
				map.put(jArray.names().getString(i), link);


				Container firstLvl = new Container(jArray.names().getString(i), link);
				headers.childs.add(firstLvl);
				firstLvl.setParent(headers);


				listDataHeader.add(jArray.names().getString(i));
				JSONArray contacts = header.getJSONArray("subheaders");

				if (contacts.length() > 1) {
					secondLevel = new ArrayList<>();
				}
				for (int j = 0; j < contacts.length(); j++) {
					String names = header.names().toString();
					JSONObject subheader = contacts.getJSONObject(j);
					JSONObject subheader2 = subheader.getJSONObject(subheader.names().getString(0));
					if (secondLevel == null) {
						secondLevel = new ArrayList<>();
					}
					secondLevel.add(subheader.names().getString(0));
					listDataChild.put(listDataHeader.get(listDataHeader.size() - 1), secondLevel);
					link = subheader2.getString("link");
					map.put(subheader.names().getString(0), link);

					Container secondLvl = new Container(subheader.names().getString(0), link);
					firstLvl.childs.add(secondLvl);
					secondLvl.setParent(headers);

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return new ContentsContainer(map, listDataHeader, listDataChild);
	}

	@Nullable
	public static Container parseJsonContents2(String contentsJson) {
		Container headers = new Container();

		JSONObject reader;
		try {
			reader = new JSONObject(contentsJson);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		List<String> secondLevel = null;
		JSONObject jArray = null;
		try {
			jArray = reader.getJSONObject("headers");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < jArray.length(); i++) {
			try {

				String link = "";
				JSONObject header = jArray.getJSONObject(jArray.names().getString(i));
				link = header.getString("link");


				Container firstLvl = new Container(jArray.names().getString(i), link);
				headers.childs.add(firstLvl);
				firstLvl.setParent(headers);


				JSONArray contacts = header.getJSONArray("subheaders");

				if (contacts.length() > 1) {
					secondLevel = new ArrayList<>();
				}
				for (int j = 0; j < contacts.length(); j++) {
					JSONObject subheader = contacts.getJSONObject(j);
					JSONObject subheader2 = subheader.getJSONObject(subheader.names().getString(0));
					if (secondLevel == null) {
						secondLevel = new ArrayList<>();
					}
					secondLevel.add(subheader.names().getString(0));
					link = subheader2.getString("link");

					Container secondLvl = new Container(subheader.names().getString(0), link);
					firstLvl.childs.add(secondLvl);
					secondLvl.setParent(headers);

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return headers;
	}

	public static class ContentsContainer {

		public LinkedHashMap<String, String> map;
		public ArrayList<String> listDataHeader;
		public LinkedHashMap<String, List<String>> listDataChild;

		ContentsContainer(LinkedHashMap<String, String> map,
		                  ArrayList<String> listDataHeader,
		                  LinkedHashMap<String, List<String>> listChildData) {
			this.map = map;
			this.listDataHeader = listDataHeader;
			this.listDataChild = listChildData;
		}
	}

	public static class Container {
		String name;
		String link;
		ArrayList<Container> childs = new ArrayList<>();
		Container parent;

		public ArrayList<Container> getChilds() {
			return childs;
		}

		public void setChilds(ArrayList<Container> childs) {
			this.childs = childs;
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

		public Container getParent() {
			return parent;
		}

		public void setParent(Container parent) {
			this.parent = parent;
		}

		public Container(String name, String link) {
			this.name = name;
			this.link = link;
		}

		public Container() {
		}
	}
}
