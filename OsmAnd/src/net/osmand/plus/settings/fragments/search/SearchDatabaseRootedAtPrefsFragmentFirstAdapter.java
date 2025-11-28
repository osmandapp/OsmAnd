package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import org.jgrapht.Graph;

import java.util.Locale;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.graph.Graphs;
import de.KnollFrank.lib.settingssearch.common.graph.SearchablePreferenceScreenSubtreeReplacer;
import de.KnollFrank.lib.settingssearch.db.preference.db.DAOProvider;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceEdge;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreens;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragmentFactory;
import de.KnollFrank.lib.settingssearch.graph.GraphPathFactory;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphProviderFactory;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

// FK-TODO: rename
public class SearchDatabaseRootedAtPrefsFragmentFirstAdapter {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	public void adaptSearchDatabaseRootedAtPrefsFragmentFirst(
			final DAOProvider preferencesDatabase,
			final SearchablePreferenceScreenGraph graph,
			final Configuration newConfiguration,
			final FragmentActivity activityContext,
			final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		preferencesDatabase
				.searchablePreferenceScreenGraphDAO()
				.persist(getAdaptedGraph(graph, newConfiguration, activityContext, tileSourceTemplatesProvider));
	}

	private SearchablePreferenceScreenGraph getAdaptedGraph(final SearchablePreferenceScreenGraph graph,
															final Configuration newConfiguration,
															final FragmentActivity activityContext,
															final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		FragmentContainerViewAdder.addInvisibleFragmentContainerViewWithIdToParent(
				activityContext.findViewById(android.R.id.content),
				FRAGMENT_CONTAINER_VIEW_ID);
		final SearchablePreferenceScreen searchablePreferenceScreen = findSearchablePreferenceScreen(graph);
		final SearchDatabaseConfig searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return new SearchablePreferenceScreenGraph(
				SearchablePreferenceScreenSubtreeReplacer.replaceSubtreeWithTree(
						graph.graph(),
						searchablePreferenceScreen,
						getPojoGraphRootedAt(
								instantiateSearchablePreferenceScreen(
										searchablePreferenceScreen,
										graph.graph(),
										createGraphPathFactory(searchDatabaseConfig, activityContext)),
								graph.locale(),
								activityContext,
								searchDatabaseConfig)),
				graph.locale(),
				new ConfigurationBundleConverter().doForward(newConfiguration));
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

	private SearchablePreferenceScreen findSearchablePreferenceScreen(final SearchablePreferenceScreenGraph graphToSearchIn) {
		return SearchablePreferenceScreens
				.findSearchablePreferenceScreenById(
						graphToSearchIn.graph().vertexSet(),
						"en-net.osmand.plus.configmap.ConfigureMapFragment$ConfigureMapFragmentProxy Bundle[{app_mode_key=car, configureSettingsSearch=true}]")
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
}
