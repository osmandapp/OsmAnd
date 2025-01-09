package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

public interface PreferenceFragmentHandler {

	Class<? extends PreferenceFragmentCompat> getClassOfPreferenceFragment();

	// FK-TODO: refactor to "Optional<Fragment> target"
	PreferenceFragmentCompat createPreferenceFragment(Context context, Fragment target);

	boolean showPreferenceFragment(PreferenceFragmentCompat preferenceFragment);
}
