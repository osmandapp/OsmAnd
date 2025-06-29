package net.osmand.plus.settings.backend.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListStringPreference extends StringPreference {

	private final String delimiter;

	public ListStringPreference(@NonNull OsmandSettings settings, @NonNull String id,
	                            @Nullable String defaultValue, @NonNull String delimiter) {
		super(settings, id, defaultValue);
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
		setModeValue(appMode, null);
	}

	public boolean containsValue(String res) {
		return containsValue(getApplicationMode(), res);
	}

	public boolean containsValue(ApplicationMode appMode, String res) {
		String vl = getModeValue(appMode);
		String r = res + delimiter;
		return vl != null && (vl.startsWith(r) || vl.contains(delimiter + r));
	}

	public boolean removeValue(String res) {
		return removeValueForProfile(getApplicationMode(), res);
	}

	public boolean removeValueForProfile(ApplicationMode appMode, String res) {
		String vl = getModeValue(appMode);
		if (vl != null) {
			String r = res + delimiter;
			if (vl.equals(res)) {
				vl = "";
			} else if (vl.startsWith(r)) {
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

	@Nullable
	public List<String> getStringsList() {
		return getStringsListForProfile(getApplicationMode());
	}

	@Nullable
	public List<String> getStringsListForProfile(ApplicationMode appMode) {
		return getStringsList(getModeValue(appMode), delimiter);
	}

	@Nullable
	public static List<String> getStringsList(@Nullable String listAsString, @NonNull String delimiter) {
		if (listAsString != null) {
			if (listAsString.contains(delimiter)) {
				return Arrays.asList(listAsString.split(delimiter));
			} else {
				return new ArrayList<>(Collections.singleton(listAsString));
			}
		}
		return null;
	}

	public void setStringsList(List<String> values) {
		setStringsListForProfile(getApplicationMode(), values);
	}

	public void setStringsListForProfile(ApplicationMode appMode, List<String> values) {
		setModeValues(appMode, values);
	}

	public boolean setModeValues(ApplicationMode mode, List<String> values) {
		if (Algorithms.isEmpty(values)) {
			return setModeValue(mode, null);
		}
		StringBuilder vl = new StringBuilder();
		for (String value : values) {
			vl.append(value).append(delimiter);
		}
		return setModeValue(mode, vl.toString());
	}

	@NonNull
	public String getDelimiter() {
		return delimiter;
	}

	@Nullable
	public String getRawModeValue(@NonNull ApplicationMode mode) {
		return super.getModeValue(mode);
	}
}