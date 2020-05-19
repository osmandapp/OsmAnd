package net.osmand.plus.settings.backend;

public class EnumStringPreference<E extends Enum<E>> extends CommonPreference<E> {

	private OsmandSettings osmandSettings;
	private final E[] values;

	EnumStringPreference(OsmandSettings osmandSettings, String id, E defaultValue, E[] values) {
		super(id, defaultValue);
		this.osmandSettings = osmandSettings;
		this.values = values;
	}

	@Override
	protected E getValue(Object prefs, E defaultValue) {
		try {
			String name = osmandSettings.settingsAPI.getString(prefs, getId(), defaultValue.name());
			E value = parseString(name);
			return value != null ? value : defaultValue;
		} catch (ClassCastException ex) {
			setValue(prefs, defaultValue);
		}
		return defaultValue;
	}

	@Override
	protected boolean setValue(Object prefs, E val) {
		return osmandSettings.settingsAPI.edit(prefs).putString(getId(), val.name()).commit();
	}

	@Override
	protected String toString(E o) {
		return o.name();
	}

	@Override
	public E parseString(String s) {
		for (E value : values) {
			if (value.name().equals(s)) {
				return value;
			}
		}
		return null;
	}
}
