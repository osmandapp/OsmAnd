package net.osmand.plus.settings.backend.preferences;

import net.osmand.plus.settings.backend.menuitems.ContextMenuItemsSettings;
import net.osmand.plus.settings.backend.OsmandSettings;

import androidx.annotation.NonNull;

public class ContextMenuItemsPreference extends CommonPreference<ContextMenuItemsSettings> {

	@NonNull
	private final String idScheme;

	public ContextMenuItemsPreference(OsmandSettings settings, String id, @NonNull String idScheme, @NonNull ContextMenuItemsSettings defValue) {
		super(settings, id, defValue);
		this.idScheme = idScheme;
	}

	@Override
	public ContextMenuItemsSettings getValue(Object prefs, ContextMenuItemsSettings defaultValue) {
		String s = getSettingsAPI().getString(prefs, getId(), defaultValue.writeToJsonString(idScheme));
		return readValue(s);
	}

	@Override
	protected boolean setValue(Object prefs, ContextMenuItemsSettings val) {
		return super.setValue(prefs, val)
				&& getSettingsAPI().edit(prefs).putString(getId(), val.writeToJsonString(idScheme)).commit();
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