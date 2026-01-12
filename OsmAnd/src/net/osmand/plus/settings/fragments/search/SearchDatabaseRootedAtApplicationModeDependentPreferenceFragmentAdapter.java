package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.google.common.collect.Sets;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.Views;
import de.KnollFrank.lib.settingssearch.common.graph.Subtree;
import de.KnollFrank.lib.settingssearch.common.graph.SubtreeReplacer;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenGraphTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreens;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragmentFactory;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphProviderFactory;
import de.KnollFrank.lib.settingssearch.graph.TreePathInstantiator;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

public class SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter implements SearchablePreferenceScreenGraphTransformer<Configuration> {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	private final Class<? extends PreferenceFragmentCompat> preferenceFragment;
	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter(
			final Class<? extends PreferenceFragmentCompat> preferenceFragment,
			final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.preferenceFragment = preferenceFragment;
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public SearchablePreferenceScreenGraph transformGraph(final SearchablePreferenceScreenGraph graph,
														  final Configuration actualConfiguration,
														  final FragmentActivity activityContext) {
		return adaptGraphAtPreferenceFragmentForAllApplicationModes(graph, actualConfiguration, activityContext);
	}

	private SearchablePreferenceScreenGraph adaptGraphAtPreferenceFragmentForAllApplicationModes(
			final SearchablePreferenceScreenGraph graph,
			final Configuration newConfiguration,
			final FragmentActivity activityContext) {
		return SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter
				.getApplicationModesWithoutDefault()
				.stream()
				.reduce(
						graph,
						(currentGraph, applicationMode) ->
								adaptGraphAtPreferenceFragmentForApplicationMode(
										currentGraph,
										applicationMode,
										newConfiguration,
										activityContext),
						(graph1, graph2) -> {
							throw new UnsupportedOperationException("Parallel stream not supported");
						});
	}

	private SearchablePreferenceScreenGraph adaptGraphAtPreferenceFragmentForApplicationMode(
			final SearchablePreferenceScreenGraph graph,
			final ApplicationMode applicationMode,
			final Configuration newConfiguration,
			final FragmentActivity activityContext) {
		OnUiThreadRunnerFactory
				.fromActivity(activityContext)
				.runBlockingOnUiThread(() -> {
					FragmentContainerViewAdder.addInvisibleFragmentContainerViewWithIdToParent(
							Views.getRootViewContainer(activityContext),
							FRAGMENT_CONTAINER_VIEW_ID);
					return null;
				});
		final SearchablePreferenceScreen preferenceScreen =
				getPreferenceScreenOfPreferenceFragment(
						graph,
						applicationMode);
		final SearchDatabaseConfig searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return new SearchablePreferenceScreenGraph(
				SubtreeReplacer.replaceSubtreeWithTree(
						new Subtree<>(
								graph.tree(),
								preferenceScreen),
						getPojoGraphRootedAt(
								instantiateSearchablePreferenceScreen(
										preferenceScreen,
										graph.tree(),
										createTreePathInstantiator(searchDatabaseConfig, activityContext)),
								graph.locale(),
								activityContext,
								searchDatabaseConfig)),
				graph.locale(),
				new ConfigurationBundleConverter().convertForward(newConfiguration));
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference> getPojoGraphRootedAt(
			final PreferenceScreenWithHost root,
			final Locale locale,
			final FragmentActivity activityContext,
			final SearchDatabaseConfig searchDatabaseConfig) {
		return SearchablePreferenceScreenGraphProviderFactory
				.createSearchablePreferenceScreenGraphProvider(
						FRAGMENT_CONTAINER_VIEW_ID,
						Views.getRootViewContainer(activityContext),
						activityContext,
						activityContext.getSupportFragmentManager(),
						activityContext,
						searchDatabaseConfig,
						locale,
						(edge, sourceNodeOfEdge, targetNodeOfEdge) -> true)
				.getSearchablePreferenceScreenTree(root);
	}

	@SuppressWarnings({"UnstableApiUsage"})
	private SearchablePreferenceScreen getPreferenceScreenOfPreferenceFragment(
			final SearchablePreferenceScreenGraph graphToSearchIn,
			final ApplicationMode applicationMode) {
		return SearchablePreferenceScreens
				.findSearchablePreferenceScreenById(
						graphToSearchIn.tree().graph().nodes(),
						String.format(
								"en-%s Bundle[{app_mode_key=%s, configureSettingsSearch=true}]",
								preferenceFragment.getName(),
								applicationMode.getStringKey()))
				.orElseThrow();
	}

	private PreferenceScreenWithHost instantiateSearchablePreferenceScreen(
			final SearchablePreferenceScreen searchablePreferenceScreen,
			final Tree<SearchablePreferenceScreen, SearchablePreference> tree,
			final TreePathInstantiator treePathInstantiator) {
		return treePathInstantiator
				.instantiate(tree.getPathFromRootNodeToTarget(searchablePreferenceScreen))
				.endNode();
	}

	private TreePathInstantiator createTreePathInstantiator(final SearchDatabaseConfig searchDatabaseConfig,
															final FragmentActivity activityContext) {
		return new TreePathInstantiator(
				new PreferenceScreenWithHostProvider(
						InstantiateAndInitializeFragmentFactory.createInstantiateAndInitializeFragment(
								searchDatabaseConfig.fragmentFactory,
								FragmentInitializerFactory.createFragmentInitializer(
										activityContext,
										FRAGMENT_CONTAINER_VIEW_ID,
										searchDatabaseConfig.preferenceSearchablePredicate),
								activityContext),
						searchDatabaseConfig.principalAndProxyProvider));
	}

	private static Set<ApplicationMode> getApplicationModesWithoutDefault() {
		return Sets.difference(
				new HashSet<>(ApplicationMode.allPossibleValues()),
				Set.of(ApplicationMode.DEFAULT));
	}
}
