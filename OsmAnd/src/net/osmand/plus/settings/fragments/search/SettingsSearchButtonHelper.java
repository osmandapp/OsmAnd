package net.osmand.plus.settings.fragments.search;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dialogs.DetailsBottomSheet;
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.transport.TransportLinesFragment;
import net.osmand.plus.widgets.alert.CustomAlert;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.KnollFrank.lib.settingssearch.MergedPreferenceScreen;
import de.KnollFrank.lib.settingssearch.client.SearchConfig;
import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.*;
import de.KnollFrank.lib.settingssearch.common.task.AsyncTaskWithProgressUpdateListeners;
import de.KnollFrank.lib.settingssearch.provider.ActivityInitializer;

public class SettingsSearchButtonHelper {

	private final BaseSettingsFragment rootSearchPreferenceFragment;
	private final @IdRes int fragmentContainerViewId;
	private final SearchDatabaseStatusHandler searchDatabaseStatusHandler;
	private final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<?>>> createSearchDatabaseTaskSupplier;
	private final OsmandPreference<String> availableAppModes;

	public SettingsSearchButtonHelper(final BaseSettingsFragment rootSearchPreferenceFragment,
									  final @IdRes int fragmentContainerViewId,
									  final OsmandApplication app,
									  final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<?>>> createSearchDatabaseTaskSupplier) {
		this.rootSearchPreferenceFragment = rootSearchPreferenceFragment;
		this.fragmentContainerViewId = fragmentContainerViewId;
		this.searchDatabaseStatusHandler =
				new SearchDatabaseStatusHandler(
						new SetStringPreference(
								app.getSettings().PLUGINS_COVERED_BY_SETTINGS_SEARCH));
		this.createSearchDatabaseTaskSupplier = createSearchDatabaseTaskSupplier;
		this.availableAppModes = app.getSettings().AVAILABLE_APP_MODES;
	}

	public void configureSettingsSearchButton(final ImageView settingsSearchButton) {
		onClickShowSearchPreferenceFragment(settingsSearchButton);
		settingsSearchButton.setImageDrawable(rootSearchPreferenceFragment.getIcon(R.drawable.searchpreference_ic_search));
		settingsSearchButton.setVisibility(View.VISIBLE);
	}

	public static SearchPreferenceFragments createSearchPreferenceFragments(
			final Supplier<Optional<AsyncTaskWithProgressUpdateListeners<?>>> createSearchDatabaseTaskSupplier,
			final Consumer<MergedPreferenceScreen> onMergedPreferenceScreenAvailable,
			final FragmentActivity fragmentActivity,
			final @IdRes int fragmentContainerViewId,
			final Class<? extends BaseSettingsFragment> rootPreferenceFragment,
			final OsmandPreference<String> availableAppModes) {
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
								.build(),
						SearchConfig
								.builder(fragmentContainerViewId, fragmentActivity)
								.withQueryHint("Search Settings")
								.withSearchResultsFilter(searchResultsFilter)
								.withPreferencePathDisplayer(PreferencePathDisplayerFactory.createPreferencePathDisplayer(fragmentActivity))
								.withSearchPreferenceFragmentUI(new SearchPreferenceFragmentUI(searchResultsFilter))
								.withSearchResultsFragmentUI(new SearchResultsFragmentUI())
								.withPrepareShow(new PrepareShow())
								.withIncludePreferenceInSearchResultsPredicate(new IncludePreferenceInSearchResultsPredicate())
								.withShowSettingsFragmentAndHighlightSetting(new ShowSettingsFragmentAndHighlightSetting(fragmentContainerViewId))
								.build(),
						fragmentActivity)
				.withCreateSearchDatabaseTaskSupplier(createSearchDatabaseTaskSupplier)
				.withOnMergedPreferenceScreenAvailable(onMergedPreferenceScreenAvailable)
				.build();
	}

	private static ActivitySearchDatabaseConfigs createActivitySearchDatabaseConfigs() {
		return new ActivitySearchDatabaseConfigs(
				Map.of(MapActivity.class, ConfigureMapFragment.PreferenceFragment.class),
				Set.of(
						new PrincipalAndProxy<>(ConfigureMapFragment.class, ConfigureMapFragment.PreferenceFragment.class),
						new PrincipalAndProxy<>(DetailsBottomSheet.class, DetailsBottomSheet.PreferenceFragment.class),
						new PrincipalAndProxy<>(TransportLinesFragment.class, TransportLinesFragment.PreferenceFragment.class),
						new PrincipalAndProxy<>(SelectMapStyleBottomSheetDialogFragment.class, SelectMapStyleBottomSheetDialogFragment.PreferenceFragment.class),
						new PrincipalAndProxy<>(CustomAlert.SingleSelectionDialogFragment.class, CustomAlert.SingleSelectionDialogFragment.PreferenceFragment.class)));
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
						availableAppModes);
		searchPreferenceButton.setOnClickListener(v -> showSearchPreferenceFragment(searchPreferenceFragments));
	}

	private void showSearchPreferenceFragment(final SearchPreferenceFragments searchPreferenceFragments) {
		if (!searchDatabaseStatusHandler.isSearchDatabaseUpToDate()) {
			searchPreferenceFragments.rebuildSearchDatabase();
			searchDatabaseStatusHandler.setSearchDatabaseUpToDate();
		}
		searchPreferenceFragments.showSearchPreferenceFragment();
	}
}
