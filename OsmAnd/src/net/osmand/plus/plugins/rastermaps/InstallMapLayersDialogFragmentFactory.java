package net.osmand.plus.plugins.rastermaps;

import static net.osmand.plus.widgets.alert.AlertDialogData.INVALID_ID;

import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import net.osmand.ResultMatcher;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.alert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// FK-TODO: refractor
class InstallMapLayersDialogFragmentFactory {

	private final FragmentActivity activity;
	private final OsmandSettings settings;
	private final ResultMatcher<TileSourceManager.TileSourceTemplate> result;

	public InstallMapLayersDialogFragmentFactory(final FragmentActivity activity,
												 final ResultMatcher<TileSourceManager.TileSourceTemplate> result) {
		this.activity = activity;
		this.result = result;
		this.settings = ((OsmandApplication) activity.getApplication()).getSettings();
	}

	public Optional<InstallMapLayersDialogFragment> createInstallMapLayersDialogFragment(final List<TileSourceManager.TileSourceTemplate> downloaded) {
		if (activity.isFinishing()) {
			return Optional.empty();
		}
		if (downloaded == null || downloaded.isEmpty()) {
			Toast.makeText(activity, R.string.shared_string_io_error, Toast.LENGTH_SHORT).show();
			return Optional.empty();
		}
		final boolean[] selected = new boolean[downloaded.size()];
		return Optional.of(
				SelectionDialogFragmentFactory.createInstallMapLayersDialogFragment(
						getAlertDialogData(downloaded, selected, activity),
						createSelectionDialogFragmentData(downloaded, selected),
						v -> {
							if (!activity.isFinishing()) {
								final int which = (int) v.getTag();
								selected[which] = !selected[which];
								if (settings.getTileSourceEntries().containsKey(downloaded.get(which).getName()) && selected[which]) {
									Toast.makeText(activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
								}
							}
						}));
	}

	private AlertDialogData getAlertDialogData(final List<TileSourceManager.TileSourceTemplate> downloaded,
											   final boolean[] selected,
											   final FragmentActivity activity) {
		final boolean nightMode = OsmandRasterMapsPlugin.isNightMode(activity);
		return new AlertDialogData(activity, nightMode)
				.setTitle(R.string.select_tile_source_to_install)
				.setControlsColor(ColorUtilities.getAppModeColor((OsmandApplication) activity.getApplication(), nightMode))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
					if (!activity.isFinishing()) {
						List<TileSourceManager.TileSourceTemplate> toInstall = new ArrayList<>();
						for (int i = 0; i < selected.length; i++) {
							if (selected[i]) {
								toInstall.add(downloaded.get(i));
							}
						}
						for (TileSourceManager.TileSourceTemplate ts : toInstall) {
							if (settings.installTileSource(ts)) {
								if (result != null) {
									result.publish(ts);
								}
							}
						}
						// at the end publish null to show end of process
						if (!toInstall.isEmpty() && result != null) {
							result.publish(null);
						}
					}
				});
	}

	private static SelectionDialogFragmentData createSelectionDialogFragmentData(
			final List<TileSourceManager.TileSourceTemplate> downloaded,
			final boolean[] selected) {
		final List<String> names = getNames(downloaded);
		return new SelectionDialogFragmentData(
				names,
				asCharSequences(names),
				Optional.of(selected),
				INVALID_ID);
	}

	private static List<String> getNames(final List<TileSourceManager.TileSourceTemplate> downloaded) {
		return downloaded
				.stream()
				.map(TileSourceManager.TileSourceTemplate::getName)
				.collect(Collectors.toUnmodifiableList());
	}

	private static List<CharSequence> asCharSequences(final List<String> strs) {
		return strs.stream().collect(Collectors.toUnmodifiableList());
	}
}
