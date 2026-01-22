package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;

import com.google.common.graph.ImmutableValueGraph;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.ConfigureProfileFragment;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;

class TreeBuilderListener implements de.KnollFrank.lib.settingssearch.graph.TreeBuilderListener<PreferenceScreenWithHost, Preference> {

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;
	private Optional<ApplicationMode> previousApplicationMode = Optional.empty();

	public TreeBuilderListener(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public void onStartBuildTree(final PreferenceScreenWithHost treeRoot) {
		tileSourceTemplatesProvider.enableCache();
	}

	@Override
	public void onStartBuildSubtree(final PreferenceScreenWithHost subtreeRoot) {
		if (subtreeRoot.host() instanceof final ConfigureProfileFragment configureProfileFragment) {
			setApplicationModePreference(getApplicationModePreference(configureProfileFragment), configureProfileFragment.getSelectedAppMode());
		}
	}

	@Override
	public void onFinishBuildSubtree(final PreferenceScreenWithHost subtreeRoot) {
		if (subtreeRoot.host() instanceof final ConfigureProfileFragment configureProfileFragment) {
			restoreApplicationModePreference(getApplicationModePreference(configureProfileFragment));
		}
	}

	@Override
	@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
	public void onFinishBuildTree(final Tree<PreferenceScreenWithHost, Preference, ImmutableValueGraph<PreferenceScreenWithHost, Preference>> tree) {
		tileSourceTemplatesProvider.disableCache();
	}

	private static OsmandPreference<ApplicationMode> getApplicationModePreference(final BaseSettingsFragment baseSettingsFragment) {
		return baseSettingsFragment.settings.APPLICATION_MODE;
	}

	// FK-TODO: do not set and reset application mode globally using applicationModePreference, instead use fragment argument APP_MODE_KEY for TransportLinesFragmentProxy, SelectMapStyleBottomSheetDialogFragmentProxy and DetailsBottomSheetProxy
	private void setApplicationModePreference(final OsmandPreference<ApplicationMode> applicationModePreference,
											  final ApplicationMode applicationMode) {
		previousApplicationMode = Optional.of(applicationModePreference.get());
		applicationModePreference.set(applicationMode);
	}

	private void restoreApplicationModePreference(final OsmandPreference<ApplicationMode> applicationModePreference) {
		applicationModePreference.set(previousApplicationMode.orElseThrow());
	}
}
