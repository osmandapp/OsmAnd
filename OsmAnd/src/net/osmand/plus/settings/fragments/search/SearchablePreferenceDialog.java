package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;

public interface SearchablePreferenceDialog {

	void show(FragmentManager fragmentManager, OsmandApplication app);

	String getSearchableInfo();
}
