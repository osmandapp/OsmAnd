package net.osmand.plus.settings.backend;

public class EnumStringPreference<E extends Enum<E>> extends CommonPreference<E> {

	private final E[] values;

	EnumStringPreference(OsmandSettings settings, String id, E defaultValue, E[] values) {
		super(settings, id, defaultValue);
		this.values = values;
	}

	@Override
	protected E getValue(Object prefs, E defaultValue) {
		try {
			String name = getSettingsAPI().getString(prefs, getId(), defaultValue.name());
			E value = parseString(name);
			return value != null ? value : defaultValue;
		} catch (ClassCastException ex) {
			setValue(prefs, defaultValue);
		}
		return defaultValue;
	}

	@Override
	public boolean setValue(Object prefs, E val) {
		return getSettingsAPI().edit(prefs).putString(getId(), val.name()).commit();
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

	public E[] getValues() {
		return values;
	}
}