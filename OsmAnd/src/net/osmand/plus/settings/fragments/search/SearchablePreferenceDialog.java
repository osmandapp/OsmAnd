package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;

public interface SearchablePreferenceDialog {

	void show(OsmandApplication app, FragmentManager fragmentManager);

	String getSearchableInfo();
}
