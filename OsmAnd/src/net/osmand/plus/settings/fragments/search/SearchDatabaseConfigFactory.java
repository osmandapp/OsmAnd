package net.osmand.plus.settings.fragments.search;

import android.app.Activity;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.google.common.collect.Sets;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapDialogs;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.configmap.MapModeFragment;
import net.osmand.plus.dialogs.DetailsBottomSheet;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.plugins.rastermaps.TileSourceTemplatesProvider;
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

import de.KnollFrank.lib.settingssearch.PreferenceEdge;
import de.KnollFrank.lib.settingssearch.PreferenceScreenWithHost;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.ActivitySearchDatabaseConfigs;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.PrincipalAndProxy;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.graph.Graphs;
import de.KnollFrank.lib.settingssearch.graph.ComputePreferencesListener;
import de.KnollFrank.lib.settingssearch.provider.ActivityInitializer;
import de.KnollFrank.lib.settingssearch.provider.PreferenceScreenGraphAvailableListener;

public class SearchDatabaseConfigFactory {

	public static SearchDatabaseConfig createSearchDatabaseConfig(
			final Class<? extends BaseSettingsFragment> rootPreferenceFragment,
			final TileSourceTemplatesProvider tileSourceTemplatesProvider,
			final FragmentManager fragmentManager) {
		return SearchDatabaseConfig
				.builder(rootPreferenceFragment)
				.withFragmentFactory(new FragmentFactory())
				.withActivitySearchDatabaseConfigs(createActivitySearchDatabaseConfigs())
				.withActivityInitializerByActivity(getActivityInitializerByActivity(fragmentManager))
				.withPreferenceFragmentConnected2PreferenceProvider(new PreferenceFragmentConnected2PreferenceProvider())
				.withSearchableInfoProvider(SearchDatabaseConfigFactory::getSearchableInfo)
				.withPreferenceDialogAndSearchableInfoProvider(new PreferenceDialogAndSearchableInfoProvider())
				.withPreferenceSearchablePredicate(new PreferenceSearchablePredicate())
				.withComputePreferencesListener(enableCacheForDownloadedTileSourceTemplatesWhileBuildingSearchDatabase(tileSourceTemplatesProvider))
				// FK-TODO: remove PreferenceScreenGraphAvailableListener
				.withPreferenceScreenGraphAvailableListener(
						new PreferenceScreenGraphAvailableListener() {

							@Override
							public void onPreferenceScreenGraphAvailable(final Graph<PreferenceScreenWithHost, PreferenceEdge> preferenceScreenGraph) {
								final PreferenceScreenWithHost rootNode =
										Graphs
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
						})
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
