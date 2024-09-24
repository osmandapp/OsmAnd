package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.fragment.app.Fragment;

// FK-TODO: rename
public interface Info {

	// FK-TODO: remove method
	Class<? extends Fragment> getClazz();

	Fragment createFragment(final Context context);

	boolean showFragment(Fragment fragment);
}
