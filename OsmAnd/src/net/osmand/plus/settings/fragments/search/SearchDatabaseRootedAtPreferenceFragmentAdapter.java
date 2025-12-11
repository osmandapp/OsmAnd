package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.google.common.collect.Sets;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import org.jgrapht.Graph;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.graph.Graphs;
import de.KnollFrank.lib.settingssearch.common.graph.SearchablePreferenceScreenSubtreeReplacer;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.SearchablePreferenceScreenGraphTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceEdge;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreens;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragmentFactory;
import de.KnollFrank.lib.settingssearch.graph.GraphPathFactory;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphProviderFactory;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

public class SearchDatabaseRootedAtPreferenceFragmentAdapter implements SearchablePreferenceScreenGraphTransformer<Configuration> {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	private final Class<? extends PreferenceFragmentCompat> preferenceFragment;
	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public SearchDatabaseRootedAtPreferenceFragmentAdapter(final Class<? extends PreferenceFragmentCompat> preferenceFragment,
														   final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.preferenceFragment = preferenceFragment;
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public SearchablePreferenceScreenGraph transformGraph(final SearchablePreferenceScreenGraph graph,
														  final Configuration actualConfiguration,
														  final FragmentActivity activityContext) {
		return adaptGraphAtPreferenceFragment(
				graph,
				actualConfiguration,
				activityContext);
	}

	private SearchablePreferenceScreenGraph adaptGraphAtPreferenceFragment(
			final SearchablePreferenceScreenGraph graph,
			final Configuration newConfiguration,
			final FragmentActivity activityContext) {
		return SearchDatabaseRootedAtPreferenceFragmentAdapter
				.getApplicationModesWithoutDefault()
				.stream()
				.reduce(
						graph,
						(currentGraph, applicationMode) ->
								adaptGraphAtPreferenceFragment(
										currentGraph,
										applicationMode,
										newConfiguration,
										activityContext),
						(graph1, graph2) -> {
							throw new UnsupportedOperationException("Parallel stream not supported");
						});
	}

	private SearchablePreferenceScreenGraph adaptGraphAtPreferenceFragment(
			final SearchablePreferenceScreenGraph graph,
			final ApplicationMode applicationMode,
			final Configuration newConfiguration,
			final FragmentActivity activityContext) {
		OnUiThreadRunnerFactory
				.fromActivity(activityContext)
				.runBlockingOnUiThread(() -> {
					FragmentContainerViewAdder.addInvisibleFragmentContainerViewWithIdToParent(
							activityContext.findViewById(android.R.id.content),
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
				SearchablePreferenceScreenSubtreeReplacer.replaceSubtreeWithTree(
						graph.graph(),
						preferenceScreen,
						getPojoGraphRootedAt(
								instantiateSearchablePreferenceScreen(
										preferenceScreen,
										graph.graph(),
										createGraphPathFactory(searchDatabaseConfig, activityContext)),
								graph.locale(),
								activityContext,
								searchDatabaseConfig)),
				graph.locale(),
				new ConfigurationBundleConverter().convertForward(newConfiguration));
	}

	private Graph<SearchablePreferenceScreen, SearchablePreferenceEdge> getPojoGraphRootedAt(
			final PreferenceScreenWithHost root,
			final Locale locale,
			final FragmentActivity activityContext,
			final SearchDatabaseConfig searchDatabaseConfig) {
		return SearchablePreferenceScreenGraphProviderFactory
				.createSearchablePreferenceScreenGraphProvider(
						FRAGMENT_CONTAINER_VIEW_ID,
						activityContext.findViewById(android.R.id.content),
						activityContext,
						activityContext.getSupportFragmentManager(),
						activityContext,
						searchDatabaseConfig,
						locale)
				.getSearchablePreferenceScreenGraph(root);
	}

	private SearchablePreferenceScreen getPreferenceScreenOfPreferenceFragment(
			final SearchablePreferenceScreenGraph graphToSearchIn,
			final ApplicationMode applicationMode) {
		return SearchablePreferenceScreens
				.findSearchablePreferenceScreenById(
						graphToSearchIn.graph().vertexSet(),
						String.format(
								"en-%s Bundle[{app_mode_key=%s, configureSettingsSearch=true}]",
								preferenceFragment.getName(),
								applicationMode.getStringKey()))
				.orElseThrow();
	}

	private PreferenceScreenWithHost instantiateSearchablePreferenceScreen(
			final SearchablePreferenceScreen searchablePreferenceScreen,
			final Graph<SearchablePreferenceScreen, SearchablePreferenceEdge> graph,
			final GraphPathFactory graphPathFactory) {
		return graphPathFactory
				.instantiate(Graphs.getPathFromRootNodeToTarget(graph, searchablePreferenceScreen))
				.getEndVertex();
	}

	private GraphPathFactory createGraphPathFactory(final SearchDatabaseConfig searchDatabaseConfig,
													final FragmentActivity activityContext) {
		return new GraphPathFactory(
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
