package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import com.google.common.graph.ImmutableValueGraph;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import java.util.Locale;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.Views;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeCreator;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenTree;
import de.KnollFrank.lib.settingssearch.fragment.FragmentFactoryAndInitializer;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.Fragments;
import de.KnollFrank.lib.settingssearch.fragment.factory.FragmentFactoryAndInitializerRegistry;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenTreeProviderFactory;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

public class SearchDatabaseRebuilder implements SearchablePreferenceScreenTreeCreator<Configuration> {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public SearchDatabaseRebuilder(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	@SuppressWarnings({"UnstableApiUsage"})
	public SearchablePreferenceScreenTree createTree(final Locale locale,
													 final Configuration actualConfiguration,
													 final FragmentActivity activityContext) {
		OnUiThreadRunnerFactory
				.fromActivity(activityContext)
				.runBlockingOnUiThread(() -> {
					FragmentContainerViewAdder.addInvisibleFragmentContainerViewWithIdToParent(
							Views.getRootViewContainer(activityContext),
							FRAGMENT_CONTAINER_VIEW_ID);
					return null;
				});
		final SearchDatabaseConfig searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return new SearchablePreferenceScreenTree(
				getPojoTreeRootedAt(
						instantiateSearchablePreferenceScreen(
								searchDatabaseConfig,
								activityContext),
						locale,
						activityContext,
						searchDatabaseConfig),
				locale,
				new ConfigurationBundleConverter().convertForward(actualConfiguration));
	}

	@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
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
						(edge, sourceNodeOfEdge, targetNodeOfEdge) -> true)
				.getSearchablePreferenceScreenTree(root);
	}

	private PreferenceScreenWithHost instantiateSearchablePreferenceScreen(
			final SearchDatabaseConfig searchDatabaseConfig,
			final FragmentActivity activityContext) {
		final PreferenceScreenWithHostProvider preferenceScreenWithHostProvider =
				new PreferenceScreenWithHostProvider(
						new Fragments(
								new FragmentFactoryAndInitializerRegistry(
										new FragmentFactoryAndInitializer(
												searchDatabaseConfig.fragmentFactory,
												FragmentInitializerFactory.createFragmentInitializer(
														activityContext,
														FRAGMENT_CONTAINER_VIEW_ID,
														searchDatabaseConfig.preferenceSearchablePredicate))),
								activityContext),
						searchDatabaseConfig.principalAndProxyProvider);
		return preferenceScreenWithHostProvider.getPreferenceScreenWithHostOfFragment(searchDatabaseConfig.rootPreferenceFragment, Optional.empty()).orElseThrow();
	}
}
