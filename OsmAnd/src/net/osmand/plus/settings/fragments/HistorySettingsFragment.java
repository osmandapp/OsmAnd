package net.osmand.plus.settings.fragments;

import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.bottomsheets.ClearAllHistoryBottomSheet;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.search.core.SearchResult;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.List;

public class HistorySettingsFragment extends BaseSettingsFragment implements OnConfirmDeletionListener {

	public static final String TAG = HistorySettingsFragment.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(HistorySettingsFragment.class);

	private static final String ACTIONS = "actions";
	private static final String BACKUP_TO_FILE = "backup_to_file";
	private static final String SEARCH_HISTORY = "search_history";
	private static final String CLEAR_ALL_HISTORY = "clear_all_history";
	private static final String NAVIGATION_HISTORY = "navigation_history";
	private static final String HISTORY_INFO = "history_preferences_info";
	private static final String MAP_MARKERS_HISTORY = "map_markers_history";

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@Override
	protected void setupPreferences() {
		setupHistoryInfoPref();
		setupSearchHistoryPref();
		setupNavigationHistoryPref();
		setupMapMarkersHistoryPref();

		setupActionsPref();
		setupBackupIntoFilePref();
		setupClearAllHistoryPref();
	}

	private void setupHistoryInfoPref() {
		Preference preference = findPreference(HISTORY_INFO);
		preference.setIconSpaceReserved(false);
	}

	private void setupActionsPref() {
		Preference preference = findPreference(ACTIONS);
		preference.setIconSpaceReserved(false);
	}

	private void setupSearchHistoryPref() {
		Preference preference = findPreference(SEARCH_HISTORY);
		if (settings.SEARCH_HISTORY.get()) {
			SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
			int size = historyHelper.getHistoryResults(HistorySource.SEARCH, false, true).size();
			String description = getString(R.string.shared_string_items);
			preference.setSummary(getString(R.string.ltr_or_rtl_combine_via_colon, description, String.valueOf(size)));
			preference.setIcon(getActiveIcon(R.drawable.ic_action_search_dark));
		} else {
			preference.setSummary(R.string.shared_string_disabled);
			preference.setIcon(getContentIcon(R.drawable.ic_action_search_dark));
		}
	}

	private void setupNavigationHistoryPref() {
		Preference preference = findPreference(NAVIGATION_HISTORY);
		if (settings.NAVIGATION_HISTORY.get()) {
			int size = calculateNavigationItemsCount(app);
			String itemsDescr = getString(R.string.shared_string_items);
			preference.setSummary(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, String.valueOf(size)));
			preference.setIcon(getActiveIcon(R.drawable.ic_action_gdirections_dark));
		} else {
			preference.setSummary(R.string.shared_string_disabled);
			preference.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
		}
	}

	private void setupMapMarkersHistoryPref() {
		Preference preference = findPreference(MAP_MARKERS_HISTORY);
		if (settings.MAP_MARKERS_HISTORY.get()) {
			MapMarkersHelper helper = app.getMapMarkersHelper();
			int itemsSize = helper.getMapMarkersHistory().size();
			String itemsDescr = getString(R.string.shared_string_items);
			preference.setSummary(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, String.valueOf(itemsSize)));
			preference.setIcon(getActiveIcon(R.drawable.ic_action_flag));
		} else {
			preference.setSummary(R.string.shared_string_disabled);
			preference.setIcon(getContentIcon(R.drawable.ic_action_flag));
		}
	}

	private void setupBackupIntoFilePref() {
		Preference preference = findPreference(BACKUP_TO_FILE);
		preference.setIcon(getActiveIcon(R.drawable.ic_action_read_from_file));
	}

	private void setupClearAllHistoryPref() {
		Preference preference = findPreference(CLEAR_ALL_HISTORY);
		preference.setIcon(getIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete));
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		String prefId = preference.getKey();
		if (HISTORY_INFO.equals(prefId)) {
			TextView title = holder.itemView.findViewById(android.R.id.title);
			AndroidUtils.setTextSecondaryColor(app, title, isNightMode());
		} else if (CLEAR_ALL_HISTORY.equals(prefId)) {
			TextView title = holder.itemView.findViewById(android.R.id.title);
			title.setTextColor(ContextCompat.getColor(app, R.color.color_osm_edit_delete));
		} else if (ACTIONS.equals(prefId)) {
			TextView title = holder.itemView.findViewById(android.R.id.title);
			title.setTypeface(FontCache.getFont(app, getString(R.string.font_roboto_medium)));
		}
		super.onBindPreferenceViewHolder(preference, holder);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String prefId = preference.getKey();

		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null) {
			switch (prefId) {
				case BACKUP_TO_FILE:
					HashMap<ExportType, List<?>> selectedTypes = new HashMap<>();
					selectedTypes.put(ExportType.SEARCH_HISTORY, null);
					selectedTypes.put(ExportType.NAVIGATION_HISTORY, null);
					selectedTypes.put(ExportType.HISTORY_MARKERS, null);

					ApplicationMode mode = getSelectedAppMode();
					ExportSettingsFragment.showInstance(fragmentManager, mode, selectedTypes, true);
					break;
				case CLEAR_ALL_HISTORY:
					ClearAllHistoryBottomSheet.showInstance(fragmentManager, this);
					break;
				case SEARCH_HISTORY:
					SearchHistorySettingsFragment.showInstance(fragmentManager, this);
					break;
				case NAVIGATION_HISTORY:
					NavigationHistorySettingsFragment.showInstance(fragmentManager, this);
					break;
				case MAP_MARKERS_HISTORY:
					MarkersHistorySettingsFragment.showInstance(fragmentManager, this);
					break;
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public void onDeletionConfirmed() {
		MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();
		mapMarkersHelper.removeMarkers(mapMarkersHelper.getMapMarkersHistory());

		SearchHistoryHelper searchHistoryHelper = SearchHistoryHelper.getInstance(app);
		searchHistoryHelper.removeAll();

		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		targetPointsHelper.clearBackupPoints();

		updateAllSettings();
	}

	private static int calculateNavigationItemsCount(@NonNull OsmandApplication app) {
		SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
		int count = historyHelper.getHistoryResults(HistorySource.NAVIGATION, true, true).size();
		if (app.getTargetPointsHelper().isBackupPointsAvailable()) {
			// Take "Previous Route" item into account during calculations
			count++;
		}
		return count;
	}

	@Nullable
	protected static HistoryEntry getHistoryEntry(SearchResult searchResult) {
		if (searchResult.object instanceof HistoryEntry) {
			return (HistoryEntry) searchResult.object;
		} else if (searchResult.relatedObject instanceof HistoryEntry) {
			return (HistoryEntry) searchResult.relatedObject;
		}
		return null;
	}

	@Override
	public void onPreferenceChanged(@NonNull String prefId) {
		updateSetting(prefId);
	}
}