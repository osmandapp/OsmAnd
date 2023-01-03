package net.osmand.plus.settings.fragments;

import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.data.PointDescription;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.bottomsheets.ClearAllHistoryBottomSheet;
import net.osmand.search.SearchUICore;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.SearchResult;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
			int size = getSearchHistoryResults(app).size();
			String itemsDescr = getString(R.string.shared_string_items);
			preference.setSummary(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, String.valueOf(size)));
			preference.setIcon(getActiveIcon(R.drawable.ic_action_search_dark));
		} else {
			preference.setSummary(R.string.shared_string_disabled);
			preference.setIcon(getContentIcon(R.drawable.ic_action_search_dark));
		}
	}

	private void setupNavigationHistoryPref() {
		Preference preference = findPreference(NAVIGATION_HISTORY);
		if (settings.NAVIGATION_HISTORY.get()) {
			int size = getNavigationHistoryResults(app).size();
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
	protected void onBindPreferenceViewHolder(Preference preference, PreferenceViewHolder holder) {
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

		if (prefId.equals(BACKUP_TO_FILE)) {
			MapActivity activity = getMapActivity();
			if (AndroidUtils.isActivityNotDestroyed(activity)) {
				ApplicationMode mode = getSelectedAppMode();
				List<ExportSettingsType> types = new ArrayList<>();
				types.add(ExportSettingsType.SEARCH_HISTORY);
				types.add(ExportSettingsType.HISTORY_MARKERS);
				ExportSettingsFragment.showInstance(activity.getSupportFragmentManager(), mode, types, true);
			}
		} else if (prefId.equals(CLEAR_ALL_HISTORY)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				ClearAllHistoryBottomSheet.showInstance(fragmentManager, this);
			}
		} else if (prefId.equals(SEARCH_HISTORY)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				SearchHistorySettingsFragment.showInstance(fragmentManager, this);
			}
		} else if (prefId.equals(NAVIGATION_HISTORY)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				NavigationHistorySettingsFragment.showInstance(fragmentManager, this);
			}
		} else if (prefId.equals(MAP_MARKERS_HISTORY)) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				MarkersHistorySettingsFragment.showInstance(fragmentManager, this);
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

		updateAllSettings();
	}

	protected static List<SearchResult> getNavigationHistoryResults(@NonNull OsmandApplication app) {
		List<SearchResult> searchResults = getSearchHistoryResults(app);
		for (Iterator<SearchResult> iterator = searchResults.iterator(); iterator.hasNext(); ) {
			HistoryEntry historyEntry = getHistoryEntry(iterator.next());
			if (historyEntry != null) {
				PointDescription pointDescription = historyEntry.getName();
				if (pointDescription.isPoiType() || pointDescription.isCustomPoiFilter()) {
					iterator.remove();
				}
			}
		}
		return searchResults;
	}

	protected static List<SearchResult> getSearchHistoryResults(@NonNull OsmandApplication app) {
		List<SearchResult> searchResults = new ArrayList<>();
		try {
			SearchUICore searchUICore = app.getSearchUICore().getCore();
			SearchResultCollection res = searchUICore.shallowSearch(SearchHistoryAPI.class, "", null, false, false);
			if (res != null) {
				searchResults.addAll(res.getCurrentSearchResults());
			}
		} catch (IOException e) {
			log.error(e);
		}
		return searchResults;
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