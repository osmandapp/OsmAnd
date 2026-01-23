package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;

import com.google.common.collect.Sets;
import com.google.common.graph.ImmutableValueGraph;

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
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreens;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.InstantiateAndInitializeFragmentFactory;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenTreeProviderFactory;
import de.KnollFrank.lib.settingssearch.graph.TreePathInstantiator;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
public class SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter implements SearchablePreferenceScreenTreeTransformer<Configuration> {

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
	public Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> transformSearchablePreferenceScreenTree(
			final SearchablePreferenceScreenTree<Configuration> searchablePreferenceScreenTree,
			final Configuration targetConfiguration,
			final FragmentActivity activityContext) {
		return adaptTreeAtPreferenceFragmentForAllApplicationModes(
				searchablePreferenceScreenTree.tree(),
				searchablePreferenceScreenTree.locale(),
				activityContext);
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> adaptTreeAtPreferenceFragmentForAllApplicationModes(
			final Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> tree,
			final Locale locale,
			final FragmentActivity activityContext) {
		return SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter
				.getApplicationModesWithoutDefault()
				.stream()
				.reduce(
						tree,
						(currentTree, applicationMode) ->
								adaptTreeAtPreferenceFragmentForApplicationMode(
										currentTree,
										applicationMode,
										locale,
										activityContext),
						(tree1, tree2) -> {
							throw new UnsupportedOperationException("Parallel stream not supported");
						});
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> adaptTreeAtPreferenceFragmentForApplicationMode(
			final Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> tree,
			final ApplicationMode applicationMode,
			final Locale locale,
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
						tree,
						applicationMode);
		final SearchDatabaseConfig searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return SubtreeReplacer.replaceSubtreeWithTree(
				new Subtree<>(
						tree,
						preferenceScreen),
				getPojoTreeRootedAt(
						instantiateSearchablePreferenceScreen(
								preferenceScreen,
								tree,
								createTreePathInstantiator(searchDatabaseConfig, activityContext)),
						locale,
						activityContext,
						searchDatabaseConfig));
	}

	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> getPojoTreeRootedAt(
			final PreferenceScreenWithHost root,
			final Locale locale,
			final FragmentActivity activityContext,
			final SearchDatabaseConfig searchDatabaseConfig) {
		return SearchablePreferenceScreenTreeProviderFactory
				.createSearchablePreferenceScreenTreeProvider(
						FRAGMENT_CONTAINER_VIEW_ID,
						Views.getRootViewContainer(activityContext),
						activityContext,
						activityContext.getSupportFragmentManager(),
						activityContext,
						searchDatabaseConfig,
						locale,
						edge -> true)
				.getSearchablePreferenceScreenTree(root);
	}

	private SearchablePreferenceScreen getPreferenceScreenOfPreferenceFragment(
			final Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> treeToSearchIn,
			final ApplicationMode applicationMode) {
		return SearchablePreferenceScreens
				.findSearchablePreferenceScreenById(
						treeToSearchIn.graph().nodes(),
						String.format(
								"en-%s Bundle[{app_mode_key=%s, configureSettingsSearch=true}]",
								preferenceFragment.getName(),
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
