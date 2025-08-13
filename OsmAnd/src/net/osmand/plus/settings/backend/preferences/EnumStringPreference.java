package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandSettings;

public class EnumStringPreference<E extends Enum<E>> extends CommonPreference<E> {

	private final E[] values;

	public EnumStringPreference(@NonNull OsmandSettings settings, @NonNull String id,
			E defaultValue, E[] values) {
		super(settings, id, defaultValue);
		this.values = values;
	}

	@Override
	public E getValue(@NonNull Object prefs, E defaultValue) {
		try {
			String defaultValueName = defaultValue == null ? null : defaultValue.name();
			String name = getSettingsAPI().getString(prefs, getId(), defaultValueName);
			E value = parseString(name);
			return value != null ? value : defaultValue;
		} catch (ClassCastException ex) {
			setValue(prefs, defaultValue);
		}
		return defaultValue;
	}

	@Override
	public boolean setValue(Object prefs, E val) {
		String name = val == null ? null : val.name();
		return super.setValue(prefs, val) && getSettingsAPI().edit(prefs).putString(getId(), name).commit();
	}

	@Override
	protected String toString(E o) {
		return o == null ? null : o.name();
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