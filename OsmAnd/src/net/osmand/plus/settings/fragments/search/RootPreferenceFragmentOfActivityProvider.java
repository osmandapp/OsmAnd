package net.osmand.plus.settings.fragments.search;

import android.app.Activity;

import androidx.preference.PreferenceFragmentCompat;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;

import java.util.Optional;

class RootPreferenceFragmentOfActivityProvider implements de.KnollFrank.lib.settingssearch.provider.RootPreferenceFragmentOfActivityProvider {

	@Override
	public Optional<Class<? extends PreferenceFragmentCompat>> getRootPreferenceFragmentOfActivity(final Class<? extends Activity> activityClass) {
		return MapActivity.class.equals(activityClass) ?
				Optional.of(ConfigureMapFragment.PreferenceFragment.class) :
				Optional.empty();
	}
}
