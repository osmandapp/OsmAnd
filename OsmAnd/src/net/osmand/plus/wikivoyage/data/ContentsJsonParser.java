package net.osmand.plus.wikivoyage.data;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

class ContentsJsonParser {

	@Nullable
	static ContentsContainer parseJsonContents(String contentsJson) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		ArrayList<String> listDataHeader = new ArrayList<>();
		LinkedHashMap<String, List<String>> listDataChild = new LinkedHashMap<>();

		JSONObject reader;
		try {
			reader = new JSONObject(contentsJson);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		List<String> secondLevel = null;
		JSONArray jArray = reader.names();
		for (int i = 0; i < jArray.length(); i++) {
			try {
				JSONArray contacts = reader.getJSONArray(reader.names().getString(i));
				String link = contacts.getString(1);

				map.put(reader.names().getString(i), link);

				int level = contacts.getInt(0);

				if (level == 2) {
					listDataHeader.add(reader.names().getString(i));
					secondLevel = new ArrayList<>();
				}
				if (level == 3) {
					if (secondLevel == null) {
						secondLevel = new ArrayList<>();
					}
					secondLevel.add(reader.names().getString(i));
					listDataChild.put(listDataHeader.get(listDataHeader.size() - 1), secondLevel);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return new ContentsContainer(map, listDataHeader, listDataChild);
	}

	static class ContentsContainer {

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
}
