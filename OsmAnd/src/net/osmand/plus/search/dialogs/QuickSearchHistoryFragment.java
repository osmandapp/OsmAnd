package net.osmand.plus.search.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.search.QuickSearchHelper.SearchHistoryAPI;
import net.osmand.plus.search.history.HistoryEntry;
import net.osmand.plus.search.listitems.QuickSearchDisabledHistoryItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.settings.fragments.HistorySettingsDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.ObjectType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuickSearchHistoryFragment extends BaseFullScreenDialogFragment implements OsmAndCompassListener, OsmAndLocationListener {

	public static final String TAG = QuickSearchHistoryFragment.class.getSimpleName();
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(QuickSearchHistoryFragment.class);
	private static final String SORT_CHIP_ID = "sort";
	private static final String SOURCE_CHIP_ID = "source";
	private static final String TYPE_CHIP_ID_PREFIX = "type:";

	private enum HistorySortMode {
		RECENT(R.string.shared_string_recent, R.drawable.ic_action_sort),
		NEAREST(R.string.shared_string_nearest, R.drawable.ic_action_sort),
		MAP_CENTER(R.string.sort_by_nearest_to_map_center, R.drawable.ic_action_sort);

		final int titleId;
		final int iconId;

		HistorySortMode(int titleId, int iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}
	}

	private enum HistorySourceFilter {
		ALL(R.string.shared_string_all, R.drawable.ic_action_history, null),
		SEARCH(R.string.shared_string_search, R.drawable.ic_action_search_dark, HistorySource.SEARCH),
		NAVIGATION(R.string.shared_string_navigation, R.drawable.ic_action_gdirections_dark, HistorySource.NAVIGATION);

		final int titleId;
		final int iconId;
		@Nullable
		final HistorySource source;

		HistorySourceFilter(int titleId, int iconId, @Nullable HistorySource source) {
			this.titleId = titleId;
			this.iconId = iconId;
			this.source = source;
		}
	}

	private QuickSearchHistoryAdapter adapter;
	private TextView titleView;
	private AppCompatEditText searchEditText;
	private ImageButton settingsButton;
	private ImageButton clearButton;
	private ChipsLayout chipsToolbar;
	private ChipsLayout typeChipsToolbar;
	private ChipsLayout.DropDownChipData sortModeChip;
	private ChipsLayout.DropDownChipData sourceFilterChip;
	private final List<ChipsLayout.ChipData> toolbarChipItems = new ArrayList<>();

	private Float heading;
	private Location location;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;
	private boolean touching;
	private boolean searchFieldActive;
	private HistorySortMode selectedSortMode = HistorySortMode.RECENT;
	private HistorySourceFilter selectedSourceFilter = HistorySourceFilter.ALL;
	private final List<String> selectedTypeFilters = new ArrayList<>();
	private final List<String> visibleTypeFilters = new ArrayList<>();
	private boolean hasHistoryRecords;

	@ColorRes
	@Override
	protected int getStatusBarColorId() {
		return ColorUtilities.getListBgColorId(nightMode);
	}

	@Override
	public void onStart() {
		super.onStart();
		updateStatusBarAppearance();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable android.os.Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.quick_search_history_fragment, container, false);

		setupToolbar(view);
		setupChips(view);
		setupList(view);
		updateHistoryItems("");
		updateStatusBarAppearance();

		return view;
	}

	private void updateStatusBarAppearance() {
		Dialog dialog = getDialog();
		Window window = dialog != null ? dialog.getWindow() : null;
		if (window != null) {
			AndroidUiHelper.setStatusBarColor(window, getColor(getStatusBarColorId()));
			AndroidUiHelper.setStatusBarContentColor(window.getDecorView(), nightMode);
		}
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		int iconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		toolbar.setNavigationIcon(getPaintedIcon(AndroidUtils.getNavigationIconResId(app), iconColor));
		toolbar.setNavigationContentDescription(R.string.shared_string_back);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		titleView = view.findViewById(R.id.title);
		titleView.setOnClickListener(v -> activateSearchField());

		settingsButton = view.findViewById(R.id.settings_button);
		settingsButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_settings_outlined, iconColor));
		settingsButton.setOnClickListener(v -> openHistorySettings());

		clearButton = view.findViewById(R.id.clear_button);
		clearButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_remove_dark, iconColor));
		clearButton.setOnClickListener(v -> searchEditText.setText(""));

		searchEditText = view.findViewById(R.id.search);
		searchEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String query = s == null ? "" : s.toString();
				updateToolbarActions(query);
				updateHistoryItems(query);
			}
		});
		updateToolbarActions("");
	}

	private void setupChips(@NonNull View view) {
		chipsToolbar = view.findViewById(R.id.chips_toolbar);
		typeChipsToolbar = view.findViewById(R.id.type_chips_toolbar);
		chipsToolbar.setThemeContext(appMode, getThemeUsageContext());
		typeChipsToolbar.setThemeContext(appMode, getThemeUsageContext());
		typeChipsToolbar.setOnChipClickListener(this::onTypeChipClick);
		initToolbarChipItems();
		updateSourceFilterFromSettings();
		updateChipsState();
	}

	private void onTypeChipClick(@NonNull String chipId) {
		if (chipId.startsWith(TYPE_CHIP_ID_PREFIX)) {
			String typeName = chipId.substring(TYPE_CHIP_ID_PREFIX.length());
			Editable searchTextEditable = searchEditText.getText();
			if (searchTextEditable != null) {
				if (selectedTypeFilters.remove(typeName)) {
					updateHistoryItems(searchTextEditable.toString());
				} else {
					selectedTypeFilters.add(typeName);
					updateHistoryItems(searchTextEditable.toString());
				}
			}
		}
	}

	private void onDropdownItemClick(@NonNull String chipId, int itemId) {
		Editable searchTextEditable = searchEditText.getText();
		if (SORT_CHIP_ID.equals(chipId)) {
			HistorySortMode[] modes = HistorySortMode.values();
			if (itemId >= 0 && itemId < modes.length) {
				selectedSortMode = modes[itemId];
				updateChipsState();
				if (searchTextEditable != null) {
					updateHistoryItems(searchTextEditable.toString());
				}
			}
		} else if (SOURCE_CHIP_ID.equals(chipId)) {
			if (isSourceMenuDisabled()) {
				return;
			}
			HistorySourceFilter[] filters = HistorySourceFilter.values();
			if (itemId >= 0 && itemId < filters.length) {
				selectedSourceFilter = filters[itemId];
				updateChipsState();
				if (searchTextEditable != null) {
					updateHistoryItems(searchTextEditable.toString());
				}
			}
		}
	}

	private void activateSearchField() {
		if (searchFieldActive) {
			return;
		}
		searchFieldActive = true;
		titleView.setVisibility(View.GONE);
		searchEditText.setVisibility(View.VISIBLE);
		searchEditText.requestFocus();
		searchEditText.post(() -> {
			if (isAdded()) {
				AndroidUtils.showSoftKeyboard(requireActivity(), searchEditText);
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility")
	private void setupList(@NonNull View view) {
		MapActivity mapActivity = (MapActivity) requireActivity();
		ListView listView = view.findViewById(R.id.list);
		listView.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
		listView.setOnTouchListener((v, event) -> {
			switch (event.getAction()) {
				case android.view.MotionEvent.ACTION_DOWN:
				case android.view.MotionEvent.ACTION_POINTER_DOWN:
					touching = true;
					break;
				case android.view.MotionEvent.ACTION_UP:
				case android.view.MotionEvent.ACTION_POINTER_UP:
				case android.view.MotionEvent.ACTION_CANCEL:
					touching = false;
					break;
			}
			return false;
		});
		listView.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
				compassUpdateAllowed = scrollState == SCROLL_STATE_IDLE;
				if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
					AndroidUtils.hideSoftKeyboard(requireActivity(), searchEditText);
				}
			}

			@Override
			public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount,
					int totalItemCount) {
			}
		});
		listView.setOnItemClickListener((parent, itemView, position, id) -> {
			QuickSearchHistoryAdapter.Item item = adapter.getItem(position);
			if (item != null && item.getListItem() != null) {
				onHistoryItemClick(item.getListItem());
			}
		});

		adapter = new QuickSearchHistoryAdapter(app, mapActivity, nightMode);
		listView.setAdapter(adapter);
	}

	private void onHistoryItemClick(@NonNull QuickSearchListItem item) {
		Fragment target = getTargetFragment();
		SearchResult searchResult = item.getSearchResult();
		if (target instanceof QuickSearchDialogFragment quickSearchDialogFragment && searchResult != null) {
			dismissAllowingStateLoss();
			quickSearchDialogFragment.showSearchHistoryResult(searchResult);
		}
	}

	private void updateToolbarActions(@NonNull String query) {
		boolean searchActive = !TextUtils.isEmpty(query);
		settingsButton.setVisibility(searchActive ? View.GONE : View.VISIBLE);
		clearButton.setVisibility(searchActive ? View.VISIBLE : View.GONE);
	}

	private void updateHistoryItems(@NonNull String query) {
		try {
			if (adapter != null) {
				if (isHistoryDisabled()) {
					hasHistoryRecords = false;
					visibleTypeFilters.clear();
					selectedTypeFilters.clear();
					updateChipsState();
					adapter.setItems(Collections.singletonList(QuickSearchHistoryAdapter.disabledHistory(
							new QuickSearchDisabledHistoryItem(app, v -> openHistorySettings()))));
				} else {
					List<HistoryRecord> records = loadHistoryRecords(query);
					hasHistoryRecords = !records.isEmpty();
					updateTypeFilterChips(records);
					records = applyTypeFilters(records);
					sortRecords(records);
					adapter.setUseMapCenter(selectedSortMode == HistorySortMode.MAP_CENTER);
					adapter.setShowDestinationDate(selectedSortMode != HistorySortMode.RECENT);
					adapter.setItems(createAdapterItems(records));
				}
			}
		} catch (Exception e) {
			LOG.error(e);
			app.showToastMessage(e.getMessage());
		}
	}

	@NonNull
	private List<HistoryRecord> loadHistoryRecords(@NonNull String query) {
		List<HistoryRecord> records = new ArrayList<>();
		String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
		SearchPhrase phrase = createHistoryPhrase();
		for (HistoryEntry entry : app.getSearchHistoryHelper().getVisibleHistoryEntries(selectedSourceFilter.source, false, false)) {
			SearchResult result = SearchHistoryAPI.createSearchResult(app, entry, phrase);
			QuickSearchListItem item = new QuickSearchListItem(app, result);
			if (Algorithms.isEmpty(normalizedQuery) || matchesQuery(item, normalizedQuery)) {
				records.add(new HistoryRecord(app, entry, item));
			}
		}
		return records;
	}

	@NonNull
	private SearchPhrase createHistoryPhrase() {
		SearchSettings settings = app.getSearchUICore().getCore().getSearchSettings();
		LatLon origin = selectedSortMode == HistorySortMode.MAP_CENTER ? getMapCenter() : null;
		if (origin != null) {
			settings = settings.setOriginalLocation(origin);
		}
		return SearchPhrase.emptyPhrase(settings);
	}

	private boolean matchesQuery(@NonNull QuickSearchListItem item, @NonNull String query) {
		return containsIgnoreCase(item.getName(), query)
				|| containsIgnoreCase(item.getTypeName(), query)
				|| containsIgnoreCase(item.getAddress(), query);
	}

	private boolean containsIgnoreCase(@Nullable String value, @NonNull String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}

	@NonNull
	private List<HistoryRecord> applyTypeFilters(@NonNull List<HistoryRecord> records) {
		if (selectedTypeFilters.isEmpty()) {
			return records;
		}
		List<HistoryRecord> filtered = new ArrayList<>();
		for (HistoryRecord record : records) {
			if (selectedTypeFilters.contains(record.filterCategoryName)) {
				filtered.add(record);
			}
		}
		return filtered;
	}

	private void sortRecords(@NonNull List<HistoryRecord> records) {
		if (selectedSortMode == HistorySortMode.RECENT) {
			Collections.sort(records, (o1, o2) -> Long.compare(o2.time, o1.time));
			return;
		}
		LatLon origin = selectedSortMode == HistorySortMode.MAP_CENTER ? getMapCenter() : getMyLocation();
		Collections.sort(records, Comparator.comparingDouble(record -> getDistance(record, origin)));
	}

	private double getDistance(@NonNull HistoryRecord record, @Nullable LatLon origin) {
		SearchResult result = record.item.getSearchResult();
		if (origin == null || result == null || result.location == null) {
			return Double.MAX_VALUE;
		}
		return MapUtils.getDistance(origin, result.location);
	}

	@Nullable
	private LatLon getMyLocation() {
		Location current = location != null ? location : app.getLocationProvider().getLastKnownLocation();
		return current != null ? new LatLon(current.getLatitude(), current.getLongitude()) : null;
	}

	@Nullable
	private LatLon getMapCenter() {
		MapActivity mapActivity = getMapActivity();
		return mapActivity != null ? mapActivity.getMapLocation() : null;
	}

	@NonNull
	private List<QuickSearchHistoryAdapter.Item> createAdapterItems(@NonNull List<HistoryRecord> records) {
		List<QuickSearchHistoryAdapter.Item> items = new ArrayList<>();
		if (selectedSortMode != HistorySortMode.RECENT) {
			for (HistoryRecord record : records) {
				items.add(QuickSearchHistoryAdapter.result(record.item));
			}
			return items;
		}
		String previousHeader = null;
		for (HistoryRecord record : records) {
			String header = getHeader(record.time);
			if (!Algorithms.objectEquals(previousHeader, header)) {
				items.add(QuickSearchHistoryAdapter.header(header));
				previousHeader = header;
			}
			items.add(QuickSearchHistoryAdapter.result(record.item));
		}
		return items;
	}

	@NonNull
	private String getHeader(long time) {
		Calendar now = Calendar.getInstance();
		Calendar item = Calendar.getInstance();
		item.setTimeInMillis(time);
		if (isSameDay(now, item)) {
			return getString(R.string.today);
		}
		Calendar todayStart = startOfDay(now);
		Calendar yesterdayStart = (Calendar) todayStart.clone();
		yesterdayStart.add(Calendar.DAY_OF_YEAR, -1);
		Calendar itemStart = startOfDay(item);
		if (!itemStart.before(yesterdayStart) && itemStart.before(todayStart)) {
			return getString(R.string.yesterday);
		}
		Calendar lastWeekStart = (Calendar) todayStart.clone();
		lastWeekStart.add(Calendar.DAY_OF_YEAR, -7);
		if (!itemStart.before(lastWeekStart) && itemStart.before(yesterdayStart)) {
			return getString(R.string.last_week);
		}
		return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(item.getTime());
	}

	@NonNull
	private Calendar startOfDay(@NonNull Calendar calendar) {
		Calendar result = (Calendar) calendar.clone();
		result.set(Calendar.HOUR_OF_DAY, 0);
		result.set(Calendar.MINUTE, 0);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);
		return result;
	}

	private boolean isSameDay(@NonNull Calendar first, @NonNull Calendar second) {
		return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
				&& first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
	}

	private void updateTypeFilterChips(@NonNull List<HistoryRecord> records) {
		if (chipsToolbar == null) {
			return;
		}
		Map<String, Integer> categoryCounts = new LinkedHashMap<>();
		for (HistoryRecord record : records) {
			if (!Algorithms.isEmpty(record.filterCategoryName)) {
				Integer count = categoryCounts.get(record.filterCategoryName);
				categoryCounts.put(record.filterCategoryName, count != null ? count + 1 : 1);
			}
		}
		selectedTypeFilters.retainAll(categoryCounts.keySet());
		List<Map.Entry<String, Integer>> sortedCategories = new ArrayList<>(categoryCounts.entrySet());
		sortedCategories.sort((first, second) -> Integer.compare(second.getValue(), first.getValue()));
		List<String> categoryNames = new ArrayList<>();
		for (Map.Entry<String, Integer> categoryCount : sortedCategories) {
			categoryNames.add(categoryCount.getKey());
		}
		visibleTypeFilters.clear();
		visibleTypeFilters.addAll(categoryNames);
		updateChipsState();
	}

	private void updateSortChip() {
		updateChipsState();
	}

	private void updateSourceChip() {
		updateSourceFilterFromSettings();
		updateChipsState();
	}

	private void initToolbarChipItems() {
		sortModeChip = new ChipsLayout.DropDownChipData(
				SORT_CHIP_ID,
				HistorySortMode.RECENT.iconId,
				null,
				false,
				true,
				true,
				ChipsLayout.TextColorStyle.PRIMARY,
				ChipsLayout.IconColorStyle.ACTIVE,
				R.string.sort_by,
				new ArrayList<>(),
				false,
				this::onDropdownItemClick);
		sourceFilterChip = new ChipsLayout.DropDownChipData(
				SOURCE_CHIP_ID,
				HistorySourceFilter.ALL.iconId,
				null,
				false,
				true,
				true,
				ChipsLayout.TextColorStyle.PRIMARY,
				ChipsLayout.IconColorStyle.ACTIVE,
				R.string.shared_string_type,
				new ArrayList<>(),
				true,
				this::onDropdownItemClick);
		toolbarChipItems.clear();
		toolbarChipItems.add(sortModeChip);
		toolbarChipItems.add(sourceFilterChip);
	}

	private void updateChipsState() {
		if (chipsToolbar == null || typeChipsToolbar == null) {
			return;
		}
		List<ChipsLayout.ChipData> typeChips = new ArrayList<>();
		sortModeChip.iconId = selectedSortMode.iconId;
		sortModeChip.title = getString(selectedSortMode.titleId);
		sortModeChip.enabled = !isHistoryDisabled() && hasHistoryRecords;
		sortModeChip.iconColor = sortModeChip.enabled
				? ChipsLayout.IconColorStyle.ACTIVE
				: ChipsLayout.IconColorStyle.SECONDARY;
		sortModeChip.dropdownItems = getSortOptions();

		sourceFilterChip.iconId = selectedSourceFilter.iconId;
		sourceFilterChip.title = getString(selectedSourceFilter.titleId);
		sourceFilterChip.enabled = !isSourceMenuDisabled();
		sourceFilterChip.iconColor = isSourceMenuDisabled()
				? ChipsLayout.IconColorStyle.SECONDARY
				: ChipsLayout.IconColorStyle.ACTIVE;
		sourceFilterChip.dropdownItems = getSourceOptions();

		for (String typeName : visibleTypeFilters) {
			typeChips.add(new ChipsLayout.ChipData(
					TYPE_CHIP_ID_PREFIX + typeName,
					0,
					typeName,
					selectedTypeFilters.contains(typeName),
					true,
					true,
					false,
					ChipsLayout.TextColorStyle.PRIMARY,
					ChipsLayout.IconColorStyle.DEFAULT));
		}
		chipsToolbar.updateContent(toolbarChipItems);
		typeChipsToolbar.updateContent(typeChips);
		typeChipsToolbar.setVisibility(typeChips.isEmpty() ? View.GONE : View.VISIBLE);
	}

	@NonNull
	private List<ChipsLayout.DropdownItem> getSortOptions() {
		List<ChipsLayout.DropdownItem> options = new ArrayList<>();
		for (HistorySortMode sortMode : HistorySortMode.values()) {
			options.add(new ChipsLayout.DropdownItem(
					sortMode.ordinal(), 0, getString(sortMode.titleId), null, selectedSortMode == sortMode));
		}
		return options;
	}

	@NonNull
	private List<ChipsLayout.DropdownItem> getSourceOptions() {
		List<ChipsLayout.DropdownItem> options = new ArrayList<>();
		for (HistorySourceFilter sourceFilter : HistorySourceFilter.values()) {
			options.add(new ChipsLayout.DropdownItem(
					sourceFilter.ordinal(),
					0,
					getString(sourceFilter.titleId),
					null,
					selectedSourceFilter == sourceFilter,
					true,
					false,
					sourceFilter == HistorySourceFilter.ALL));
		}
		return options;
	}

	private void updateSourceFilterFromSettings() {
		boolean searchHistoryEnabled = settings.SEARCH_HISTORY.get();
		boolean navigationHistoryEnabled = settings.NAVIGATION_HISTORY.get();
		if (!searchHistoryEnabled && navigationHistoryEnabled) {
			selectedSourceFilter = HistorySourceFilter.NAVIGATION;
		} else if (searchHistoryEnabled && !navigationHistoryEnabled) {
			selectedSourceFilter = HistorySourceFilter.SEARCH;
		} else if (!searchHistoryEnabled) {
			selectedSourceFilter = HistorySourceFilter.ALL;
		}
	}

	private boolean isSourceMenuDisabled() {
		return !settings.SEARCH_HISTORY.get() || !settings.NAVIGATION_HISTORY.get();
	}

	private boolean isHistoryDisabled() {
		return !settings.SEARCH_HISTORY.get() && !settings.NAVIGATION_HISTORY.get();
	}

	private static class HistoryRecord {
		final long time;
		final QuickSearchListItem item;
		final String filterCategoryName;

		HistoryRecord(@NonNull OsmandApplication app, @NonNull HistoryEntry entry, @NonNull QuickSearchListItem item) {
			this.time = entry.getLastAccessTime();
			this.item = item;
			this.filterCategoryName = getFilterCategoryName(app, entry, item);
		}

		@Nullable
		private static String getFilterCategoryName(@NonNull OsmandApplication app, @NonNull HistoryEntry entry,
		                                            @NonNull QuickSearchListItem item) {
			PoiCategory category = getPoiCategory(app, entry, item);
			if (category != null) {
				if (category.isAdministrative()) {
					return app.getString(R.string.shared_string_address);
				}
				return category.getTranslation();
			}
			String typeName = getHistoryTypeName(app, entry.getName());
			if (!Algorithms.isEmpty(typeName)) {
				return typeName;
			}
			SearchResult result = item.getSearchResult();
			return result != null ? QuickSearchListItem.getTypeName(app, result) : null;
		}

		@Nullable
		private static String getHistoryTypeName(@NonNull OsmandApplication app, @NonNull PointDescription name) {
			String type = name.getType();
			if (Algorithms.isEmpty(type)) {
				return null;
			}
			return switch (type) {
				case PointDescription.POINT_TYPE_ADDRESS -> app.getString(R.string.shared_string_address);
				case PointDescription.POINT_TYPE_CUSTOM_POI_FILTER,
				     PointDescription.POINT_TYPE_POI,
				     PointDescription.POINT_TYPE_POI_TYPE -> app.getString(R.string.poi);
				case PointDescription.POINT_TYPE_FAVORITE -> app.getString(R.string.shared_string_favorite);
				case PointDescription.POINT_TYPE_GPX,
				     PointDescription.POINT_TYPE_GPX_FILE -> app.getString(R.string.shared_string_gpx_track);
				case PointDescription.POINT_TYPE_LOCATION -> app.getString(R.string.shared_string_location);
				case PointDescription.POINT_TYPE_MAP_MARKER -> app.getString(R.string.map_marker);
				case PointDescription.POINT_TYPE_MARKER -> app.getString(R.string.shared_string_marker);
				case PointDescription.POINT_TYPE_OSM_BUG -> app.getString(R.string.osn_bug_name);
				case PointDescription.POINT_TYPE_PHOTO_NOTE -> app.getString(R.string.shared_string_photo);
				case PointDescription.POINT_TYPE_TARGET -> app.getString(R.string.route_descr_destination);
				case PointDescription.POINT_TYPE_VIDEO_NOTE -> app.getString(R.string.shared_string_video);
				case PointDescription.POINT_TYPE_WORLD_REGION_SHOW_ON_MAP -> app.getString(R.string.regions);
				case PointDescription.POINT_TYPE_WPT -> app.getString(R.string.shared_string_waypoint);
				default -> null;
			};
		}

		@Nullable
		private static PoiCategory getPoiCategory(@NonNull OsmandApplication app, @NonNull HistoryEntry entry,
		                                          @NonNull QuickSearchListItem item) {
			if (!Algorithms.isEmpty(entry.getPoiCategoryKey())) {
				PoiCategory category = app.getPoiTypes().getPoiCategoryByName(entry.getPoiCategoryKey());
				if (category != null) {
					return category;
				}
			}
			if (!Algorithms.isEmpty(entry.getPoiSubtypeKey())) {
				AbstractPoiType poiType = app.getPoiTypes().getAnyPoiTypeByKey(entry.getPoiSubtypeKey());
				if (poiType instanceof PoiCategory category) {
					return category;
				} else if (poiType instanceof PoiType type) {
					return type.getCategory();
				}
			}
			SearchResult result = item.getSearchResult();
			if (result != null) {
				if (result.objectType == ObjectType.POI && result.object instanceof Amenity amenity) {
					return amenity.getType();
				} else if (result.objectType == ObjectType.POI_TYPE && result.object instanceof PoiCategory category) {
					return category;
				} else if (ObjectType.isAddress(result.objectType)) {
					return app.getPoiTypes().getPoiCategoryByName(MapPoiTypes.ADMINISTRATIVE_CATEGORY);
				}
			}
			return null;
		}
	}

	private void openHistorySettings() {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null) {
			HistorySettingsDialogFragment.showInstance(fragmentManager, this);
		}
	}

	public void reloadHistory() {
		if (chipsToolbar != null && searchEditText != null) {
			updateSourceChip();
			updateHistoryItems(searchEditText.getText().toString());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		reloadHistory();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
			if (selectedSortMode == HistorySortMode.NEAREST) {
				updateHistoryItems(searchEditText.getText().toString());
			}
		}
	}

	@Override
	public void updateCompassValue(float value) {
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (!touching && compassUpdateAllowed && adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	private void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection targetsCollection = super.getInsetTargets();
		targetsCollection.replace(InsetTarget.createScrollable(R.id.list));
		targetsCollection.add(InsetTarget.createHorizontalLandscape(R.id.chips_toolbar_container).build());
		return targetsCollection;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			QuickSearchHistoryFragment fragment = new QuickSearchHistoryFragment();
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
