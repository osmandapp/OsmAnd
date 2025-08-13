package net.osmand.plus.settings.backend.menuitems;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainContextMenuItemsSettings extends ContextMenuItemsSettings {

	private static final String MAIN = "main";

	private List<String> mainIds = new ArrayList<>();

	public MainContextMenuItemsSettings() {

	}

	public MainContextMenuItemsSettings(@NonNull List<String> mainIds, @NonNull List<String> hiddenIds, @NonNull List<String> orderIds) {
		super(hiddenIds, orderIds);
		this.mainIds = mainIds;
	}

	@NonNull
	@Override
	public ContextMenuItemsSettings newInstance() {
		return new MainContextMenuItemsSettings();
	}

	@Override
	public void readFromJson(@NonNull JSONObject json, @NonNull String idScheme) {
		super.readFromJson(json, idScheme);
		mainIds = readIdsList(json.optJSONArray(MAIN), idScheme);
	}

	@Override
	public void writeToJson(JSONObject json, String idScheme) throws JSONException {
		super.writeToJson(json, idScheme);
		json.put(MAIN, getJsonArray(mainIds, idScheme));
	}

	public List<String> getMainIds() {
		return Collections.unmodifiableList(mainIds);
	}
}