package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ConfigureProfileFragment;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import org.jgrapht.Graph;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import de.KnollFrank.lib.settingssearch.PreferenceEdge;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.Preferences;
import de.KnollFrank.lib.settingssearch.common.graph.Graphs;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.SearchablePreferenceScreenGraphTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceEdge;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreens;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragmentFactory;
import de.KnollFrank.lib.settingssearch.graph.AddEdgeToGraphPredicate;
import de.KnollFrank.lib.settingssearch.graph.GraphMerger;
import de.KnollFrank.lib.settingssearch.graph.GraphPathFactory;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphProviderFactory;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

// FK-TODO: DRY with SearchDatabaseRootedAtPreferenceFragmentAdapter
public class PluginSettingsOfConfigureProfileFragmentAdapter implements SearchablePreferenceScreenGraphTransformer<Configuration> {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public PluginSettingsOfConfigureProfileFragmentAdapter(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public SearchablePreferenceScreenGraph transformGraph(final SearchablePreferenceScreenGraph graph,
														  final Configuration actualConfiguration,
														  final FragmentActivity activityContext) {
		return adaptPluginSettingsOfConfigureProfileFragment(graph, actualConfiguration, activityContext);
	}

	private SearchablePreferenceScreenGraph adaptPluginSettingsOfConfigureProfileFragment(
			final SearchablePreferenceScreenGraph graph,
			final Configuration newConfiguration,
			final FragmentActivity activityContext) {
		return PluginSettingsOfConfigureProfileFragmentAdapter
				.getApplicationModesWithoutDefault()
				.stream()
				.reduce(
						graph,
						(currentGraph, applicationMode) ->
								adaptPluginSettingsOfConfigureProfileFragment(
										currentGraph,
										applicationMode,
										newConfiguration,
										activityContext),
						(graph1, graph2) -> {
							throw new UnsupportedOperationException("Parallel stream not supported");
						});
	}

	private SearchablePreferenceScreenGraph adaptPluginSettingsOfConfigureProfileFragment(
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
		final SearchablePreferenceScreen configureProfilePreferenceScreen =
				getPreferenceScreenOfConfigureProfileFragment(
						graph.graph().vertexSet(),
						applicationMode);
		final SearchDatabaseConfig searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return new SearchablePreferenceScreenGraph(
				GraphMerger.mergeSrcGraphIntoDstGraphAtMergePoint(
						// FK-TODO: dieser Aufruf von getPojoGraphRootedAt() darf aus Performancegründen ausschließlich den Pluginpreferences folgen.
						getPojoGraphRootedAt(
								instantiateSearchablePreferenceScreen(
										configureProfilePreferenceScreen,
										graph.graph(),
										createGraphPathFactory(searchDatabaseConfig, activityContext)),
								graph.locale(),
								activityContext,
								searchDatabaseConfig),
						new GraphMerger.GraphAtMergePoint(graph.graph(), configureProfilePreferenceScreen)),
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
						locale,
						addPluginEdgesOfConfigureProfileFragmentToGraph())
				.getSearchablePreferenceScreenGraph(root);
	}

	private static AddEdgeToGraphPredicate addPluginEdgesOfConfigureProfileFragmentToGraph() {
		return new AddEdgeToGraphPredicate() {

			@Override
			public boolean shallAddEdgeToGraph(final PreferenceEdge edge,
											   final PreferenceScreenWithHost sourceNodeOfEdge,
											   final PreferenceScreenWithHost targetNodeOfEdge) {
				if (sourceNodeOfEdge.host() instanceof ConfigureProfileFragment) {
					return isPluginPreferenceOfPreferenceScreen(
							edge.preference,
							sourceNodeOfEdge.preferenceScreen());
				}
				return true;
			}

			private static boolean isPluginPreferenceOfPreferenceScreen(final Preference preference,
																		final PreferenceScreen preferenceScreen) {
				return getPluginPreferences(preferenceScreen).contains(preference);
			}

			private static List<Preference> getPluginPreferences(final PreferenceScreen preferenceScreen) {
				final PreferenceGroup preferenceGroup = Objects.requireNonNull(preferenceScreen.findPreference(ConfigureProfileFragment.PLUGIN_SETTINGS));
				return ImmutableList
						.<Preference>builder()
						.add(preferenceGroup)
						.addAll(Preferences.getChildrenRecursively(preferenceGroup))
						.build();
			}
		};
	}

	private SearchablePreferenceScreen getPreferenceScreenOfConfigureProfileFragment(
			final Set<SearchablePreferenceScreen> preferenceScreens,
			final ApplicationMode applicationMode) {
		return SearchablePreferenceScreens
				.findSearchablePreferenceScreenById(
						preferenceScreens,
						String.format(
								"en-%s Bundle[{app_mode_key=%s, configureSettingsSearch=true}]",
								ConfigureProfileFragment.class.getName(),
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

	private GraphPathFactory createGraphPathFactory(
			final SearchDatabaseConfig searchDatabaseConfig,
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
