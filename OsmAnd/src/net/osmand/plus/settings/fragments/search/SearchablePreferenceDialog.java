package net.osmand.plus.settings.fragments.search;

import androidx.fragment.app.FragmentManager;

public interface SearchablePreferenceDialog {

	void show(FragmentManager fragmentManager);

	String getSearchableInfo();
}
