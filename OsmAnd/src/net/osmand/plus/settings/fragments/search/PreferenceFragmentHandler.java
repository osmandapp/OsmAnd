package net.osmand.plus.settings.fragments.search;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

public interface PreferenceFragmentHandler {

	Class<? extends PreferenceFragmentCompat> getClassOfPreferenceFragment();

	PreferenceFragmentCompat createPreferenceFragment(Context context, Optional<Fragment> target);

	boolean showPreferenceFragment(PreferenceFragmentCompat preferenceFragment);
}
