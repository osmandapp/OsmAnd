package net.osmand.plus.settings.fragments.search;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.fragments.MainSettingsFragment;

import org.jgrapht.Graph;

import java.util.Locale;
import java.util.Optional;

import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHostProvider;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.task.OnUiThreadRunnerFactory;
import de.KnollFrank.lib.settingssearch.db.preference.db.SearchablePreferenceScreenGraphCreator;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceEdge;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreen;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferenceScreenGraph;
import de.KnollFrank.lib.settingssearch.fragment.FragmentFactoryAndInitializer;
import de.KnollFrank.lib.settingssearch.fragment.FragmentInitializerFactory;
import de.KnollFrank.lib.settingssearch.fragment.Fragments;
import de.KnollFrank.lib.settingssearch.fragment.factory.FragmentFactoryAndInitializerRegistry;
import de.KnollFrank.lib.settingssearch.graph.SearchablePreferenceScreenGraphProviderFactory;
import de.KnollFrank.lib.settingssearch.results.recyclerview.FragmentContainerViewAdder;

public class SearchDatabaseRebuilder implements SearchablePreferenceScreenGraphCreator<Configuration> {

	private final @IdRes int FRAGMENT_CONTAINER_VIEW_ID = View.generateViewId();

	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;

	public SearchDatabaseRebuilder(final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
	}

	@Override
	public SearchablePreferenceScreenGraph createGraph(final Locale locale,
													   final Configuration actualConfiguration,
													   final FragmentActivity activityContext) {
		OnUiThreadRunnerFactory
				.fromActivity(activityContext)
				.runBlockingOnUiThread(() -> {
					FragmentContainerViewAdder.addInvisibleFragmentContainerViewWithIdToParent(
							activityContext.findViewById(android.R.id.content),
							FRAGMENT_CONTAINER_VIEW_ID);
					return null;
				});
		final SearchDatabaseConfig searchDatabaseConfig =
				SearchDatabaseConfigFactory.createSearchDatabaseConfig(
						MainSettingsFragment.class,
						tileSourceTemplatesProvider,
						activityContext.getSupportFragmentManager());
		return new SearchablePreferenceScreenGraph(
				getPojoGraphRootedAt(
						instantiateSearchablePreferenceScreen(
								searchDatabaseConfig,
								activityContext),
						locale,
						activityContext,
						searchDatabaseConfig),
				locale,
				new ConfigurationBundleConverter().convertForward(actualConfiguration));
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
						(edge, sourceNodeOfEdge, targetNodeOfEdge) -> true)
				.getSearchablePreferenceScreenGraph(root);
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
