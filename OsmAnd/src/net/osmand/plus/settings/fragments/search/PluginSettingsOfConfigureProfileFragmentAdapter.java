package net.osmand.plus.settings.fragments.search;

import android.os.PersistableBundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.ImmutableValueGraph;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ConfigureProfileFragment;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.Preferences;
import de.KnollFrank.lib.settingssearch.common.Sets;
import de.KnollFrank.lib.settingssearch.common.graph.Edge;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;
import de.KnollFrank.lib.settingssearch.common.graph.TreeNode;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreens;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragmentFactory;
import de.KnollFrank.lib.settingssearch.graph.AddEdgeToTreePredicate;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenTreeProviderFactory;
import de.KnollFrank.lib.settingssearch.graph.TreeMerger;
import de.KnollFrank.lib.settingssearch.graph.TreePathInstantiator;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

// FK-TODO: DRY with SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter
@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
public class PluginSettingsOfConfigureProfileFragmentAdapter implements SearchablePreferenceScreenTreeTransformer<Configuration> {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public PluginSettingsOfConfigureProfileFragmentAdapter(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public PersistableBundle getParams() {
		return new PersistableBundle();
	}

	@Override
	public Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> transformSearchablePreferenceScreenTree(
			final SearchablePreferenceScreenTree<Configuration> searchablePreferenceScreenTree,
			final Configuration targetConfiguration,
			final FragmentActivity activityContext) {
		final Configuration srcConfiguration = searchablePreferenceScreenTree.configuration();
		return !PluginSettingsOfConfigureProfileFragmentAdapter
				.getPluginsToRemove(
						srcConfiguration.enabledPlugins(),
						targetConfiguration.enabledPlugins())
				.isEmpty() ?
				// FK-TODO: in SettingsSearch eine Klasse SubtreeRemover einführen, die einen Teilbaum entfernt und hier verwenden.
				this
						.createSearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter()
						.transformSearchablePreferenceScreenTree(
								searchablePreferenceScreenTree,
								targetConfiguration,
								activityContext) :
				adaptPluginSettingsOfConfigureProfileFragmentForAllApplicationModes(
						searchablePreferenceScreenTree.tree(),
						searchablePreferenceScreenTree.locale(),
						getPluginsToAdd(
								srcConfiguration.enabledPlugins(),
								targetConfiguration.enabledPlugins()),
						activityContext);
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> adaptPluginSettingsOfConfigureProfileFragmentForAllApplicationModes(
			final Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> tree,
			final Locale locale,
			final Set<String> pluginsToAdd,
			final FragmentActivity activityContext) {
		return PluginSettingsOfConfigureProfileFragmentAdapter
				.getApplicationModesWithoutDefault()
				.stream()
				.reduce(
						tree,
						(currentTree, applicationMode) ->
								adaptPluginSettingsOfConfigureProfileFragmentForApplicationMode(
										currentTree,
										applicationMode,
										locale,
										pluginsToAdd,
										activityContext),
						(tree1, tree2) -> {
							throw new UnsupportedOperationException("Parallel stream not supported");
						});
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> adaptPluginSettingsOfConfigureProfileFragmentForApplicationMode(
			final Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> tree,
			final ApplicationMode applicationMode,
			final Locale locale,
			final Set<String> pluginsToAdd,
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
						tree.graph().nodes(),
						applicationMode);
		final SearchDatabaseConfig<Configuration> searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return TreeMerger.mergeTreeIntoTreeNode(
				// FK-TODO: dieser Aufruf von getPojoGraphRootedAt() darf aus Performancegründen ausschließlich den Pluginpreferences folgen.
				getPojoTreeRootedAt(
						instantiateSearchablePreferenceScreen(
								configureProfilePreferenceScreen,
								tree,
								createTreePathInstantiator(searchDatabaseConfig, activityContext)),
						locale,
						activityContext,
						searchDatabaseConfig,
						pluginsToAdd),
				new TreeNode<>(
						configureProfilePreferenceScreen,
						tree));
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> getPojoTreeRootedAt(
			final PreferenceScreenWithHost root,
			final Locale locale,
			final FragmentActivity activityContext,
			final SearchDatabaseConfig<Configuration> searchDatabaseConfig,
			final Set<String> pluginsToAdd) {
		return SearchablePreferenceScreenTreeProviderFactory
				.createSearchablePreferenceScreenTreeProvider(
						FRAGMENT_CONTAINER_VIEW_ID,
						activityContext.findViewById(android.R.id.content),
						activityContext,
						activityContext.getSupportFragmentManager(),
						activityContext,
						searchDatabaseConfig,
						locale,
						addPluginEdgesOfConfigureProfileFragmentToTree(pluginsToAdd))
				.getSearchablePreferenceScreenTree(root);
	}

	private static AddEdgeToTreePredicate<PreferenceScreenWithHost, Preference> addPluginEdgesOfConfigureProfileFragmentToTree(final Set<String> pluginsToAdd) {
		return new AddEdgeToTreePredicate<>() {

			@Override
			public boolean shallAddEdgeToTree(final Edge<PreferenceScreenWithHost, Preference> edge) {
				if (edge.endpointPair().source().host() instanceof ConfigureProfileFragment) {
					return isPluginPreferenceOfPreferenceScreen(
							edge.value(),
							edge.endpointPair().source().preferenceScreen());
				}
				return true;
			}

			private boolean isPluginPreferenceOfPreferenceScreen(final Preference preference,
																 final PreferenceScreen preferenceScreen) {
				return pluginsToAdd.contains(preference.getKey()) && getPluginPreferences(preferenceScreen).contains(preference);
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
			final Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> tree,
			final TreePathInstantiator treePathInstantiator) {
		return treePathInstantiator
				.instantiate(tree.getPathFromRootNodeToTarget(searchablePreferenceScreen))
				.endNode();
	}

	private TreePathInstantiator createTreePathInstantiator(
			final SearchDatabaseConfig<Configuration> searchDatabaseConfig,
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

	private SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter createSearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter() {
		return new SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter(
				ConfigureProfileFragment.class,
				tileSourceTemplatesProvider);
	}

	private static Set<String> getPluginsToAdd(final Set<String> srcPlugins, final Set<String> targetPlugins) {
		return Sets.difference(targetPlugins, srcPlugins);
	}

	private static Set<String> getPluginsToRemove(final Set<String> srcPlugins, final Set<String> targetPlugins) {
		return Sets.difference(srcPlugins, targetPlugins);
	}
}
