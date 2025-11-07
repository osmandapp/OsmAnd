package net.osmand.plus.settings.fragments.search;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.google.common.collect.Sets;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.configmap.MapModeFragment;
import net.osmand.plus.dialogs.DetailsBottomSheet;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.widgets.alert.InstallMapLayersDialogFragment;
import net.osmand.plus.widgets.alert.MapLayerSelectionDialogFragment;
import net.osmand.plus.widgets.alert.MultiSelectionDialogFragment;
import net.osmand.plus.widgets.alert.RoadStyleSelectionDialogFragment;

import org.jgrapht.Graph;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.KnollFrank.lib.settingssearch.MergedPreferenceScreen;
import de.KnollFrank.lib.settingssearch.PreferenceEdge;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.client.SearchConfig;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.*;
import de.KnollFrank.lib.settingssearch.common.graph.GraphUtils;
import de.KnollFrank.lib.settingssearch.common.task.AsyncTaskWithProgressUpdateListeners;
import de.KnollFrank.lib.settingssearch.db.preference.db.DAOProvider;
import de.KnollFrank.lib.settingssearch.graph.ComputePreferencesListener;
import de.KnollFrank.lib.settingssearch.provider.ActivityInitializer;
import de.KnollFrank.lib.settingssearch.provider.PreferenceScreenGraphAvailableListener;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;
	private final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier;
	private final SearchDatabaseStatusHandler searchDatabaseStatusHandler;
	private final OsmandPreference<String> availableAppModes;
	private final TileSourceTemplatesProvider tileSourceTemplatesProvider;
	private final DAOProvider daoProvider;
	private final Configuration configuration;

	public static SettingsSearchButtonHelper of(final BaseSettingsFragment rootSearchPreferenceFragment,
												final @IdRes int fragmentContainerViewId,
												final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier,
												final OsmandApplication app,
												final Configuration configuration) {
		return new SettingsSearchButtonHelper(
				rootSearchPreferenceFragment,
				fragmentContainerViewId,
				createSearchDatabaseTaskSupplier,
				new SearchDatabaseStatusHandler(
						new SetStringPreference(
								app.getSettings().PLUGINS_COVERED_BY_SETTINGS_SEARCH)),
				app.getSettings().AVAILABLE_APP_MODES,
				app.getTileSourceTemplatesProvider(),
				app.daoProviderManager.getDAOProvider(),
				configuration);
	}

	private SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
									   final int fragmentContainerViewId,
									   final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier,
									   final SearchDatabaseStatusHandler searchDatabaseStatusHandler,
									   final OsmandPreference<String> availableAppModes,
									   final TileSourceTemplatesProvider tileSourceTemplatesProvider,
									   final DAOProvider daoProvider,
									   final Configuration configuration) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.fragmentContainerViewId = fragmentContainerViewId;
		this.createSearchDatabaseTaskSupplier = createSearchDatabaseTaskSupplier;
		this.searchDatabaseStatusHandler = searchDatabaseStatusHandler;
		this.availableAppModes = availableAppModes;
		this.tileSourceTemplatesProvider = tileSourceTemplatesProvider;
		this.daoProvider = daoProvider;
		this.configuration = configuration;
	}

	public void configureSettingsSearchButton(final ImageView settingsSearchButton) {
		onClickShowSearchPreferenceFragment(settingsSearchButton);
		settingsSearchButton.setImageDrawable(rootSearchPreferenceFragment.getIcon(R.drawable.searchpreference_ic_search));
		settingsSearchButton.setVisibility(View.VISIBLE);
	}

	public static SearchPreferenceFragments createSearchPreferenceFragments(
			final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<Void, DAOProvider>>> createSearchDatabaseTaskSupplier,
			final Consumer<MergedPreferenceScreen> onMergedPreferenceScreenAvailable,
			final FragmentActivity fragmentActivity,
			final @IdRes int fragmentContainerViewId,
			final Class<? extends BaseSettingsFragment> rootPreferenceFragment,
			final OsmandPreference<String> availableAppModes,
			final TileSourceTemplatesProvider tileSourceTemplatesProvider,
			final DAOProvider daoProvider,
			final Configuration configuration) {
		final SearchResultsFilter searchResultsFilter =
				SearchResultsFilterFactory.createSearchResultsFilter(
						PreferencePathDisplayerFactory.getApplicationModeKeys(),
						availableAppModes);
		final FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
		return SearchPreferenceFragments
				.builder(
						SearchDatabaseConfig
								.builder(rootPreferenceFragment)
								.withFragmentFactory(new FragmentFactory())
								.withActivitySearchDatabaseConfigs(createActivitySearchDatabaseConfigs())
								.withActivityInitializerByActivity(getActivityInitializerByActivity(fragmentManager))
								.withPreferenceFragmentConnected2PreferenceProvider(new PreferenceFragmentConnected2PreferenceProvider())
								.withSearchableInfoProvider(SettingsSearchButtonHelper::getSearchableInfo)
								.withPreferenceDialogAndSearchableInfoProvider(new PreferenceDialogAndSearchableInfoProvider())
								.withPreferenceSearchablePredicate(new PreferenceSearchablePredicate())
								.withComputePreferencesListener(enableCacheForDownloadedTileSourceTemplatesWhileBuildingSearchDatabase(tileSourceTemplatesProvider))
								// FK-TODO: remove PreferenceScreenGraphAvailableListener
								.withPreferenceScreenGraphAvailableListener(
										new PreferenceScreenGraphAvailableListener() {

											@Override
											public void onPreferenceScreenGraphAvailable(final Graph<PreferenceScreenWithHost, PreferenceEdge> preferenceScreenGraph) {
												final PreferenceScreenWithHost rootNode =
														GraphUtils
																.getRootNode(preferenceScreenGraph)
																.orElseThrow();

												final Set<PreferenceScreenWithHost> nodesToRemove =
														new HashSet<>(
																Sets.difference(
																		preferenceScreenGraph.vertexSet(),
																		Set.of(rootNode)));

												// preferenceScreenGraph.removeAllVertices(nodesToRemove);

												if (preferenceScreenGraph.vertexSet().size() == 1) {
													System.out.println("Graph erfolgreich auf den Wurzelknoten reduziert: " +
															preferenceScreenGraph.vertexSet().iterator().next().host().getClass().getSimpleName());
												} else {
													System.err.println("Fehler: Graph konnte nicht korrekt reduziert werden. Verbleibende Knoten: " +
															preferenceScreenGraph.vertexSet().size());
												}
											}
										}
								)
								.build(),
						SearchConfig
								.builder(fragmentContainerViewId, fragmentActivity)
								.withQueryHint("Search Settings")
								.withSearchResultsFilter(searchResultsFilter)
								.withPreferencePathDisplayer(PreferencePathDisplayerFactory.createPreferencePathDisplayer(fragmentActivity))
								.withSearchPreferenceFragmentUI(new SearchPreferenceFragmentUI(searchResultsFilter))
								.withSearchResultsFragmentUI(new SearchResultsFragmentUI())
								.withPrepareShow(new PrepareShow())
								.withShowSettingsFragmentAndHighlightSetting(new ShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId))
								.build(),
						fragmentActivity,
						daoProvider,
						new ConfigurationBundleConverter().doForward(configuration))
				.withCreateSearchDatabaseTaskSupplier(createSearchDatabaseTaskSupplier)
				.withOnMergedPreferenceScreenAvailable(onMergedPreferenceScreenAvailable)
				.build();
	}

	private static ActivitySearchDatabaseConfigs createActivitySearchDatabaseConfigs() {
		return new ActivitySearchDatabaseConfigs(
				Map.of(MapActivity.class, ConfigureMapFragment.ConfigureMapFragmentProxy.class),
				Set.of(
						new PrincipalAndProxy<>(ConfigureMapFragment.class, ConfigureMapFragment.ConfigureMapFragmentProxy.class),
						new PrincipalAndProxy<>(DetailsBottomSheet.class, DetailsBottomSheet.DetailsBottomSheetProxy.class),
						new PrincipalAndProxy<>(TransportLinesFragment.class, TransportLinesFragment.TransportLinesFragmentProxy.class),
						new PrincipalAndProxy<>(SelectMapStyleBottomSheetDialogFragment.class, SelectMapStyleBottomSheetDialogFragment.SelectMapStyleBottomSheetDialogFragmentProxy.class),
						new PrincipalAndProxy<>(RoadStyleSelectionDialogFragment.class, RoadStyleSelectionDialogFragment.RoadStyleSelectionDialogFragmentProxy.class),
						new PrincipalAndProxy<>(MultiSelectionDialogFragment.class, MultiSelectionDialogFragment.MultiSelectionDialogFragmentProxy.class),
						new PrincipalAndProxy<>(InstallMapLayersDialogFragment.class, InstallMapLayersDialogFragment.InstallMapLayersDialogFragmentProxy.class),
						new PrincipalAndProxy<>(ConfigureMapDialogs.MapLanguageDialog.class, ConfigureMapDialogs.MapLanguageDialog.MapLanguageDialogProxy.class),
						new PrincipalAndProxy<>(MapLayerSelectionDialogFragment.class, MapLayerSelectionDialogFragment.MapLayerSelectionDialogFragmentProxy.class),
						new PrincipalAndProxy<>(MapModeFragment.class, MapModeFragment.MapModeFragmentProxy.class)));
	}

	private static Map<Class<? extends Activity>, ActivityInitializer<?>> getActivityInitializerByActivity(final FragmentManager fragmentManager) {
		return Map.of(MapActivity.class, new MapActivityInitializer(fragmentManager));
	}

	private static Optional<String> getSearchableInfo(final Preference preference) {
		return preference instanceof final SearchableInfoProvider searchableInfoProvider ?
				Optional.of(searchableInfoProvider.getSearchableInfo()) :
				Optional.empty();
	}

	private void onClickShowSearchPreferenceFragment(final ImageView searchPreferenceButton) {
		final SearchPreferenceFragments searchPreferenceFragments =
				createSearchPreferenceFragments(
						createSearchDatabaseTaskSupplier,
						mergedPreferenceScreen -> {
						},
						rootSearchPreferenceFragment.requireActivity(),
						fragmentContainerViewId,
						rootSearchPreferenceFragment.getClass(),
						availableAppModes,
						tileSourceTemplatesProvider,
						daoProvider,
						configuration);
		searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment(searchPreferenceFragments));
	}

	private void showSearchPreferenceFragment(final SearchPreferenceFragments searchPreferenceFragments) {
		// FK-TODO: remove if-block? Test when condition is false
		if (!searchDatabaseStatusHandler.isSearchDatabaseUpToDate()) {
			searchPreferenceFragments.rebuildSearchDatabase();
			searchDatabaseStatusHandler.setSearchDatabaseUpToDate();
		}
		searchPreferenceFragments.showSearchPreferenceFragment();
	}

	private static ComputePreferencesListener enableCacheForDownloadedTileSourceTemplatesWhileBuildingSearchDatabase(
			final TileSourceTemplatesProvider tileSourceTemplatesProvider) {
		return new ComputePreferencesListener() {

			@Override
			public void onStartComputePreferences() {
				tileSourceTemplatesProvider.enableCache();
			}

			@Override
			public void onFinishComputePreferences() {
				tileSourceTemplatesProvider.disableCache();
			}
		};
	}
}
