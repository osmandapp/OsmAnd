package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

// FK-TODO: rename
public interface Info {

	String getClassNameOfPreferenceFragment();

	PreferenceFragmentCompat createPreferenceFragment(Context context, final Fragment target);

	boolean showPreferenceFragment(PreferenceFragmentCompat preferenceFragment);
}
