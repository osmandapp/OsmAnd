package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.backend.preferences.ListStringPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SetStringPreference {

	private final ListStringPreference listStringPreference;

	public SetStringPreference(final ListStringPreference listStringPreference) {
		this.listStringPreference = listStringPreference;
	}

	public void set(final Set<String> strings) {
		listStringPreference.setStringsList(new ArrayList<>(strings));
	}

	public Set<String> get() {
		return new HashSet<>(getList());
	}

	private List<String> getList() {
		final List<String> strings = listStringPreference.getStringsList();
		return strings != null ? strings : Collections.emptyList();
	}
}
