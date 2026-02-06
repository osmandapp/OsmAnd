package net.osmand.plus.settings.fragments.search;

import android.os.PersistableBundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import com.google.common.graph.ImmutableValueGraph;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;

import java.util.Locale;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceScreenOfHostOfActivity;
import de.KnollFrank.lib.settingssearch.PreferenceScreenProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.Views;
import de.KnollFrank.lib.settingssearch.common.graph.Tree;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeCreator;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreference;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
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
	public PersistableBundle getParams() {
		return new PersistableBundle();
	}

	@Override
	@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
	public Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> createTree(
			final Locale locale,
			final Configuration targetConfiguration,
			final FragmentActivity activityContext) {
		OnUiThreadRunnerFactory
				.fromActivity(activityContext)
				.runBlockingOnUiThread(() -> {
					FragmentContainerViewAdder.addInvisibleFragmentContainerViewWithIdToParent(
							Views.getRootViewContainer(activityContext),
							FRAGMENT_CONTAINER_VIEW_ID);
					return null;
				});
		final SearchDatabaseConfig<Configuration> searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return getPojoTreeRootedAt(
				instantiateSearchablePreferenceScreen(
						searchDatabaseConfig,
						activityContext),
				locale,
				activityContext,
				searchDatabaseConfig);
	}

	@SuppressWarnings({"UnstableApiUsage", "NullableProblems"})
	private Tree<SearchablePreferenceScreen, SearchablePreference, ImmutableValueGraph<SearchablePreferenceScreen, SearchablePreference>> getPojoTreeRootedAt(
			final PreferenceScreenOfHostOfActivity root,
			final Locale locale,
			final FragmentActivity activityContext,
			final SearchDatabaseConfig<Configuration> searchDatabaseConfig) {
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

	private PreferenceScreenOfHostOfActivity instantiateSearchablePreferenceScreen(
			final SearchDatabaseConfig<Configuration> searchDatabaseConfig,
			final FragmentActivity activityContext) {
		final PreferenceScreenProvider preferenceScreenProvider =
				new PreferenceScreenProvider(
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
		return preferenceScreenProvider
				.getPreferenceScreen(searchDatabaseConfig.rootPreferenceFragment, Optional.empty())
				.orElseThrow();
	}
}
