package net.osmand.plus.settings.backend;

import androidx.annotation.NonNull;

public class ContextMenuItemsPreference extends CommonPreference<ContextMenuItemsSettings> {
	@NonNull
	private String idScheme;

	ContextMenuItemsPreference(OsmandSettings osmandSettings, String id, @NonNull String idScheme, @NonNull ContextMenuItemsSettings defValue) {
		super(osmandSettings, id, defValue);
		this.idScheme = idScheme;
	}

	@Override
	public ContextMenuItemsSettings getValue(Object prefs, ContextMenuItemsSettings defaultValue) {
		String s = getSettingsAPI().getString(prefs, getId(), "");
		return readValue(s);
	}

	@Override
	protected boolean setValue(Object prefs, ContextMenuItemsSettings val) {
		return getSettingsAPI().edit(prefs).putString(getId(), val.writeToJsonString(idScheme)).commit();
	}


	@Override
	protected String toString(ContextMenuItemsSettings o) {
		return o.writeToJsonString(idScheme);
	}

	@Override
	public ContextMenuItemsSettings parseString(String s) {
		return readValue(s);
	}

	private ContextMenuItemsSettings readValue(String s) {
		ContextMenuItemsSettings value = getDefaultValue().newInstance();
		value.readFromJsonString(s, idScheme);
		return value;
	}

	@NonNull
	public String getIdScheme() {
		return idScheme;
	}
}
