package net.osmand.plus.settings.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListStringPreference extends StringPreference {

	private String delimiter;

	ListStringPreference(OsmandSettings osmandSettings, String id, String defaultValue, String delimiter) {
		super(osmandSettings, id, defaultValue);
		this.delimiter = delimiter;
	}

	public boolean addValue(String res) {
		return addModeValue(getApplicationMode(), res);
	}

	public boolean addModeValue(ApplicationMode appMode, String res) {
		String vl = getModeValue(appMode);
		if (vl == null || vl.isEmpty()) {
			vl = res + delimiter;
		} else {
			vl = vl + res + delimiter;
		}
		setModeValue(appMode, vl);
		return true;
	}

	public void clearAll() {
		clearAllForProfile(getApplicationMode());
	}

	public void clearAllForProfile(ApplicationMode appMode) {
		setModeValue(appMode, "");
	}

	public boolean containsValue(String res) {
		return containsValue(getApplicationMode(), res);
	}

	public boolean containsValue(ApplicationMode appMode, String res) {
		String vl = getModeValue(appMode);
		String r = res + delimiter;
		return vl.startsWith(r) || vl.contains(delimiter + r);
	}

	public boolean removeValue(String res) {
		return removeValueForProfile(getApplicationMode(), res);
	}

	public boolean removeValueForProfile(ApplicationMode appMode, String res) {
		String vl = getModeValue(appMode);
		String r = res + delimiter;
		if (vl != null) {
			if (vl.startsWith(r)) {
				vl = vl.substring(r.length());
			} else {
				int it = vl.indexOf(delimiter + r);
				if (it >= 0) {
					vl = vl.substring(0, it + delimiter.length()) + vl.substring(it + delimiter.length() + r.length());
				}
			}
			setModeValue(appMode, vl);
			return true;
		}
		return false;
	}

	public List<String> getStringsList() {
		return getStringsListForProfile(getApplicationMode());
	}

	public List<String> getStringsListForProfile(ApplicationMode appMode) {
		final String listAsString = getModeValue(appMode);
		if (listAsString != null) {
			if (listAsString.contains(delimiter)) {
				return Arrays.asList(listAsString.split(delimiter));
			} else {
				return new ArrayList<String>() {
					{
						add(listAsString);
					}
				};
			}
		}
		return null;
	}

	public void setStringsList(List<String> values) {
		setStringsListForProfile(getApplicationMode(), values);
	}

	public void setStringsListForProfile(ApplicationMode appMode, List<String> values) {
		if (values == null || values.size() == 0) {
			setModeValue(appMode, null);
			return;
		}
		clearAllForProfile(appMode);
		for (String value : values) {
			addModeValue(appMode, value);
		}
	}

	public boolean setModeValues(ApplicationMode mode, List<String> values) {
		if (values == null || values.size() == 0) {
			setModeValue(mode, null);
			return false;
		}
		clearAll();
		String vl = get();
		for (String value : values) {
			addValue(value);
			if (vl == null || vl.isEmpty()) {
				vl = value + delimiter;
			} else {
				vl = vl + value + delimiter;
			}
		}
		return setModeValue(mode, vl);
	}
}