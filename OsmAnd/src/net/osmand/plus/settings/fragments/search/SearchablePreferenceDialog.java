package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;

// FK-TODO: make at least all BasePreferenceBottomSheets implement this interface?
public interface SearchablePreferenceDialog {

	void show(FragmentManager fragmentManager, OsmandApplication app);

	String getSearchableInfo();
}
