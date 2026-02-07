package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;

import com.google.common.graph.ImmutableValueGraph;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;

import de.KnollFrank.lib.settingssearch.PreferenceScreenOfHostOfActivity;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;

class TreeBuilderListener implements de.KnollFrank.lib.settingssearch.graph.TreeBuilderListener<PreferenceScreenOfHostOfActivity, Preference> {

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public TreeBuilderListener(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public void onStartBuildTree(final PreferenceScreenOfHostOfActivity treeRoot) {
		tileSourceTemplatesProvider.enableCache();
	}

	@Override
	public void onStartBuildSubtree(final PreferenceScreenOfHostOfActivity subtreeRoot) {
	}

	@Override
	public void onFinishBuildSubtree(final PreferenceScreenOfHostOfActivity subtreeRoot) {
	}

	@Override
	@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
	public void onFinishBuildTree(final Tree<PreferenceScreenOfHostOfActivity, Preference, ImmutableValueGraph<PreferenceScreenOfHostOfActivity, Preference>> tree) {
		tileSourceTemplatesProvider.disableCache();
	}
}
