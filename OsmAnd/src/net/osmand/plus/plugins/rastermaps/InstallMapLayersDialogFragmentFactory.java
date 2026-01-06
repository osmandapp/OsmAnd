package net.osmand.plus.plugins.rastermaps;

import static net.osmand.map.TileSourceManager.TileSourceTemplate;
import static net.osmand.plus.widgets.alert.AlertDialogData.INVALID_ID;

import android.content.DialogInterface;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import net.osmand.ResultMatcher;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.search.Configuration;
import net.osmand.plus.settings.fragments.search.SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.InstallMapLayersDialogFragment;
import net.osmand.plus.widgets.alert.SelectionDialogFragmentData;
import net.osmand.plus.widgets.alert.SelectionDialogFragmentFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.db.preference.db.SearchablePreferenceScreenGraphRepository;

class InstallMapLayersDialogFragmentFactory {

	private final FragmentActivity activity;
	private final OsmandSettings settings;
	private final ResultMatcher<TileSourceTemplate> result;

	public InstallMapLayersDialogFragmentFactory(final FragmentActivity activity,
												 final ResultMatcher<TileSourceTemplate> result) {
		this.activity = activity;
		this.result = result;
		this.settings = ((OsmandApplication) activity.getApplication()).getSettings();
	}

	public Optional<InstallMapLayersDialogFragment> createInstallMapLayersDialogFragment(
			final List<TileSourceTemplate> tileSourceTemplates,
			final ApplicationMode appMode) {
		if (activity.isFinishing()) {
			return Optional.empty();
		}
		if (tileSourceTemplates == null || tileSourceTemplates.isEmpty()) {
			Toast.makeText(activity, R.string.shared_string_io_error, Toast.LENGTH_SHORT).show();
			return Optional.empty();
		}
		final boolean[] selected = new boolean[tileSourceTemplates.size()];
		return Optional.of(
				SelectionDialogFragmentFactory.createInstallMapLayersDialogFragment(
						getAlertDialogData(tileSourceTemplates, selected, activity),
						createSelectionDialogFragmentData(tileSourceTemplates, selected),
						v -> {
							if (!activity.isFinishing()) {
								final int which = (int) v.getTag();
								selected[which] = !selected[which];
								if (settings.getTileSourceEntries().containsKey(tileSourceTemplates.get(which).getName()) && selected[which]) {
									Toast.makeText(activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
								}
							}
						},
						appMode));
	}

	private AlertDialogData getAlertDialogData(final List<TileSourceTemplate> tileSourceTemplates,
											   final boolean[] selected,
											   final FragmentActivity activity) {
		final boolean nightMode = OsmandRasterMapsPlugin.isNightMode(activity);
		return new AlertDialogData(activity, nightMode)
				.setTitle(R.string.select_tile_source_to_install)
				.setControlsColor(ColorUtilities.getAppModeColor((OsmandApplication) activity.getApplication(), nightMode))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(
						R.string.shared_string_apply,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								if (!activity.isFinishing()) {
									final List<TileSourceTemplate> toInstall = getSelectedTileSourceTemplates();
									boolean someTileSourceWasInstalled = false;
									for (final TileSourceTemplate ts : toInstall) {
										if (settings.installTileSource(ts)) {
											someTileSourceWasInstalled = true;
											if (result != null) {
												result.publish(ts);
											}
										}
									}
									if (someTileSourceWasInstalled) {
										updateSearchDatabase();
									}
									// at the end publish null to show end of process
									if (!toInstall.isEmpty() && result != null) {
										result.publish(null);
									}
								}
							}

							private List<TileSourceTemplate> getSelectedTileSourceTemplates() {
								final List<TileSourceTemplate> selectedTileSourceTemplates = new ArrayList<>();
								for (int i = 0; i < selected.length; i++) {
									if (selected[i]) {
										selectedTileSourceTemplates.add(tileSourceTemplates.get(i));
									}
								}
								return selectedTileSourceTemplates;
							}

							private void updateSearchDatabase() {
								getGraphRepository().addGraphTransformer(
										new SearchDatabaseRootedAtApplicationModeDependentPreferenceFragmentAdapter(
												ConfigureMapFragment.ConfigureMapFragmentProxy.class,
												getTileSourceTemplatesProvider()));
							}

							private SearchablePreferenceScreenGraphRepository<Configuration> getGraphRepository() {
								return OsmandApplication
										.getInstanceFromContext(activity)
										.preferencesDatabaseManager
										.getPreferencesDatabase()
										.searchablePreferenceScreenGraphRepository();
							}

							private TileSourceTemplatesProvider getTileSourceTemplatesProvider() {
								return OsmandApplication
										.getInstanceFromContext(activity)
										.getTileSourceTemplatesProvider();
							}
						});
	}

	private static SelectionDialogFragmentData createSelectionDialogFragmentData(
			final List<TileSourceTemplate> tileSourceTemplates,
			final boolean[] selected) {
		final List<String> names = getNames(tileSourceTemplates);
		return new SelectionDialogFragmentData(
				names,
				asCharSequences(names),
				Optional.of(selected),
				INVALID_ID);
	}

	private static List<String> getNames(final List<TileSourceTemplate> tileSourceTemplates) {
		return tileSourceTemplates
				.stream()
				.map(TileSourceTemplate::getName)
				.collect(Collectors.toUnmodifiableList());
	}

	private static List<CharSequence> asCharSequences(final List<String> strs) {
		return strs.stream().collect(Collectors.toUnmodifiableList());
	}
}
