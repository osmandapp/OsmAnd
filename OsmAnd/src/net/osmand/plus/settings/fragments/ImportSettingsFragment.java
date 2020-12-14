package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.download.ReloadIndexesTask;
import net.osmand.plus.download.ReloadIndexesTask.ReloadIndexesListener;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.ApplicationMode.ApplicationModeBean;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.AvoidRoadsSettingsItem;
import net.osmand.plus.settings.backend.backup.FavoritesSettingsItem;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.GlobalSettingsItem;
import net.osmand.plus.settings.backend.backup.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.HistoryMarkersSettingsItem;
import net.osmand.plus.settings.backend.backup.MapSourcesSettingsItem;
import net.osmand.plus.settings.backend.backup.MarkersSettingsItem;
import net.osmand.plus.settings.backend.backup.OsmEditsSettingsItem;
import net.osmand.plus.settings.backend.backup.OsmNotesSettingsItem;
import net.osmand.plus.settings.backend.backup.PoiUiFiltersSettingsItem;
import net.osmand.plus.settings.backend.backup.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.QuickActionsSettingsItem;
import net.osmand.plus.settings.backend.backup.SearchHistorySettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportAsyncTask;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ImportSettingsFragment extends BaseSettingsListFragment {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(ImportSettingsFragment.class.getSimpleName());

	private static final String DUPLICATES_START_TIME_KEY = "duplicates_start_time";
	private static final long MIN_DELAY_TIME_MS = 500;

	private File file;
	private SettingsHelper settingsHelper;
	private List<SettingsItem> settingsItems;

	private TextView description;
	private ProgressBar progressBar;
	private LinearLayout buttonsContainer;
	private CollapsingToolbarLayout toolbarLayout;

	private long duplicateStartTime;

	public static void showInstance(@NonNull FragmentManager fm, @NonNull List<SettingsItem> settingsItems, @NonNull File file) {
		ImportSettingsFragment fragment = new ImportSettingsFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction().
				replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			duplicateStartTime = savedInstanceState.getLong(DUPLICATES_START_TIME_KEY);
		}
		exportMode = false;
		settingsHelper = app.getSettingsHelper();

		ImportAsyncTask importTask = settingsHelper.getImportTask();
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getItems();
			}
			if (file == null) {
				file = importTask.getFile();
			}
			List<Object> duplicates = importTask.getDuplicates();
			List<SettingsItem> selectedItems = importTask.getSelectedItems();
			if (duplicates == null) {
				importTask.setDuplicatesListener(getDuplicatesListener());
			} else if (duplicates.isEmpty()) {
				if (selectedItems != null && file != null) {
					settingsHelper.importSettings(file, selectedItems, "", 1, getImportListener());
				}
			}
		}
		if (settingsItems != null) {
			dataList = SettingsHelper.getSettingsToOperateByCategory(settingsItems, false);
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		toolbarLayout = view.findViewById(R.id.toolbar_layout);
		buttonsContainer = view.findViewById(R.id.buttons_container);
		progressBar = view.findViewById(R.id.progress_bar);

		description = header.findViewById(R.id.description);
		description.setText(R.string.select_data_to_import);

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(DUPLICATES_START_TIME_KEY, duplicateStartTime);
	}

	@Override
	protected void onContinueButtonClickAction() {
		if (adapter.getData().isEmpty()) {
			app.showShortToastMessage(getString(R.string.shared_string_nothing_selected));
		} else {
			importItems();
		}
	}

	private void updateUi(int toolbarTitleRes, int descriptionRes) {
		if (file != null) {
			String fileName = file.getName();
			toolbarLayout.setTitle(getString(toolbarTitleRes));
			description.setText(UiUtilities.createSpannableString(
					String.format(getString(descriptionRes), fileName),
					new StyleSpan(Typeface.BOLD), fileName
			));
			buttonsContainer.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			adapter.clearSettingsList();
		}
	}

	private void importItems() {
		List<SettingsItem> selectedItems = getSettingsItemsFromData(adapter.getData());
		if (file != null && settingsItems != null) {
			duplicateStartTime = System.currentTimeMillis();
			settingsHelper.checkDuplicates(file, settingsItems, selectedItems, getDuplicatesListener());
		}
		updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
	}

	public SettingsHelper.SettingsImportListener getImportListener() {
		return new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items) {
				if (succeed) {
					app.getRendererRegistry().updateExternalRenderers();
					AppInitializer.loadRoutingFiles(app, null);
					reloadIndexes(items);
					AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
					if (plugin != null) {
						plugin.indexingFiles(null, true, true);
					}
					FragmentManager fm = getFragmentManager();
					if (fm != null && file != null) {
						ImportCompleteFragment.showInstance(fm, items, file.getName());
					}
				}
			}
		};
	}

	private void reloadIndexes(@NonNull List<SettingsItem> items) {
		for (SettingsItem item : items) {
			if (item instanceof FileSettingsItem && ((FileSettingsItem) item).getSubtype().isMap()) {
				Activity activity = getActivity();
				if (activity instanceof MapActivity) {
					final WeakReference<MapActivity> mapActivityRef = new WeakReference<>((MapActivity) activity);
					ReloadIndexesListener listener = new ReloadIndexesListener() {
						@Override
						public void reloadIndexesStarted() {

						}

						@Override
						public void reloadIndexesFinished(List<String> warnings) {
							MapActivity mapActivity = mapActivityRef.get();
							if (mapActivity != null) {
								mapActivity.refreshMap();
							}
						}
					};
					ReloadIndexesTask reloadIndexesTask = new ReloadIndexesTask(app, listener);
					reloadIndexesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				break;
			}
		}
	}

	private SettingsHelper.CheckDuplicatesListener getDuplicatesListener() {
		return new SettingsHelper.CheckDuplicatesListener() {
			@Override
			public void onDuplicatesChecked(@NonNull final List<Object> duplicates, final List<SettingsItem> items) {
				long spentTime = System.currentTimeMillis() - duplicateStartTime;
				if (spentTime < MIN_DELAY_TIME_MS) {
					long delay = MIN_DELAY_TIME_MS - spentTime;
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							processDuplicates(duplicates, items);
						}
					}, delay);
				} else {
					processDuplicates(duplicates, items);
				}
			}
		};
	}

	private void processDuplicates(List<Object> duplicates, List<SettingsItem> items) {
		FragmentManager fm = getFragmentManager();
		if (file != null) {
			if (duplicates.isEmpty()) {
				if (isAdded()) {
					updateUi(R.string.shared_string_importing, R.string.importing_from);
				}
				settingsHelper.importSettings(file, items, "", 1, getImportListener());
			} else if (fm != null && !isStateSaved()) {
				ImportDuplicatesFragment.showInstance(fm, duplicates, items, file, this);
			}
		}
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	@Nullable
	private ProfileSettingsItem getBaseProfileSettingsItem(ApplicationModeBean modeBean) {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == SettingsItemType.PROFILE) {
				ProfileSettingsItem profileItem = (ProfileSettingsItem) settingsItem;
				ApplicationModeBean bean = profileItem.getModeBean();
				if (Algorithms.objectEquals(bean.stringKey, modeBean.stringKey) && Algorithms.objectEquals(bean.userProfileName, modeBean.userProfileName)) {
					return profileItem;
				}
			}
		}
		return null;
	}

	@Nullable
	private QuickActionsSettingsItem getBaseQuickActionsSettingsItem() {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == SettingsItemType.QUICK_ACTIONS) {
				return (QuickActionsSettingsItem) settingsItem;
			}
		}
		return null;
	}

	@Nullable
	private PoiUiFiltersSettingsItem getBasePoiUiFiltersSettingsItem() {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == SettingsItemType.POI_UI_FILTERS) {
				return (PoiUiFiltersSettingsItem) settingsItem;
			}
		}
		return null;
	}

	@Nullable
	private MapSourcesSettingsItem getBaseMapSourcesSettingsItem() {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == SettingsItemType.MAP_SOURCES) {
				return (MapSourcesSettingsItem) settingsItem;
			}
		}
		return null;
	}

	@Nullable
	private AvoidRoadsSettingsItem getBaseAvoidRoadsSettingsItem() {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == SettingsItemType.AVOID_ROADS) {
				return (AvoidRoadsSettingsItem) settingsItem;
			}
		}
		return null;
	}

	@Nullable
	private <T> T getBaseItem(SettingsItemType settingsItemType, Class<T> clazz) {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == settingsItemType && clazz.isInstance(settingsItem)) {
				return clazz.cast(settingsItem);
			}
		}
		return null;
	}

	private List<SettingsItem> getSettingsItemsFromData(List<?> data) {
		List<SettingsItem> settingsItems = new ArrayList<>();
		List<ApplicationModeBean> appModeBeans = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		List<OsmNotesPoint> osmNotesPointList = new ArrayList<>();
		List<OpenstreetmapPoint> osmEditsPointList = new ArrayList<>();
		List<FavoriteGroup> favoriteGroups = new ArrayList<>();
		List<MapMarkersGroup> markersGroups = new ArrayList<>();
		List<MapMarkersGroup> markersHistoryGroups = new ArrayList<>();
		List<HistoryEntry> historyEntries = new ArrayList<>();
		for (Object object : data) {
			if (object instanceof ApplicationModeBean) {
				appModeBeans.add((ApplicationModeBean) object);
			} else if (object instanceof QuickAction) {
				quickActions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				poiUIFilters.add((PoiUIFilter) object);
			} else if (object instanceof TileSourceTemplate || object instanceof SQLiteTileSource) {
				tileSourceTemplates.add((ITileSource) object);
			} else if (object instanceof File) {
				File file = (File) object;
				if (file.getName().endsWith(IndexConstants.GPX_FILE_EXT)) {
					settingsItems.add(new GpxSettingsItem(app, file));
				} else {
					settingsItems.add(new FileSettingsItem(app, file));
				}
			} else if (object instanceof FileSettingsItem) {
				settingsItems.add((FileSettingsItem) object);
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			} else if (object instanceof OsmNotesPoint) {
				osmNotesPointList.add((OsmNotesPoint) object);
			} else if (object instanceof OpenstreetmapPoint) {
				osmEditsPointList.add((OpenstreetmapPoint) object);
			} else if (object instanceof FavoriteGroup) {
				favoriteGroups.add((FavoriteGroup) object);
			} else if (object instanceof GlobalSettingsItem) {
				settingsItems.add((GlobalSettingsItem) object);
			} else if (object instanceof MapMarkersGroup) {
				MapMarkersGroup markersGroup = (MapMarkersGroup) object;
				if (ExportSettingsType.ACTIVE_MARKERS.name().equals(markersGroup.getId())) {
					markersGroups.add((MapMarkersGroup) object);
				} else if (ExportSettingsType.HISTORY_MARKERS.name().equals(markersGroup.getId())) {
					markersHistoryGroups.add((MapMarkersGroup) object);
				}
			} else if (object instanceof HistoryEntry) {
				historyEntries.add((HistoryEntry) object);
			}
		}
		if (!appModeBeans.isEmpty()) {
			for (ApplicationModeBean modeBean : appModeBeans) {
				settingsItems.add(new ProfileSettingsItem(app, getBaseProfileSettingsItem(modeBean), modeBean));
			}
		}
		if (!quickActions.isEmpty()) {
			settingsItems.add(new QuickActionsSettingsItem(app, getBaseQuickActionsSettingsItem(), quickActions));
		}
		if (!poiUIFilters.isEmpty()) {
			settingsItems.add(new PoiUiFiltersSettingsItem(app, getBasePoiUiFiltersSettingsItem(), poiUIFilters));
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsItems.add(new MapSourcesSettingsItem(app, getBaseMapSourcesSettingsItem(), tileSourceTemplates));
		}
		if (!avoidRoads.isEmpty()) {
			settingsItems.add(new AvoidRoadsSettingsItem(app, getBaseAvoidRoadsSettingsItem(), avoidRoads));
		}
		if (!osmNotesPointList.isEmpty()) {
			OsmNotesSettingsItem baseItem = getBaseItem(SettingsItemType.OSM_NOTES, OsmNotesSettingsItem.class);
			settingsItems.add(new OsmNotesSettingsItem(app, baseItem, osmNotesPointList));
		}
		if (!osmEditsPointList.isEmpty()) {
			OsmEditsSettingsItem baseItem = getBaseItem(SettingsItemType.OSM_EDITS, OsmEditsSettingsItem.class);
			settingsItems.add(new OsmEditsSettingsItem(app, baseItem, osmEditsPointList));
		}
		if (!favoriteGroups.isEmpty()) {
			FavoritesSettingsItem baseItem = getBaseItem(SettingsItemType.FAVOURITES, FavoritesSettingsItem.class);
			settingsItems.add(new FavoritesSettingsItem(app, baseItem, favoriteGroups));
		}
		if (!markersGroups.isEmpty()) {
			List<MapMarker> mapMarkers = new ArrayList<>();
			for (MapMarkersGroup group : markersGroups) {
				mapMarkers.addAll(group.getMarkers());
			}
			MarkersSettingsItem baseItem = getBaseItem(SettingsItemType.ACTIVE_MARKERS, MarkersSettingsItem.class);
			settingsItems.add(new MarkersSettingsItem(app, baseItem, mapMarkers));
		}
		if (!markersHistoryGroups.isEmpty()) {
			List<MapMarker> mapMarkers = new ArrayList<>();
			for (MapMarkersGroup group : markersHistoryGroups) {
				mapMarkers.addAll(group.getMarkers());
			}
			HistoryMarkersSettingsItem baseItem = getBaseItem(SettingsItemType.HISTORY_MARKERS, HistoryMarkersSettingsItem.class);
			settingsItems.add(new HistoryMarkersSettingsItem(app, baseItem, mapMarkers));
		}
		if (!historyEntries.isEmpty()) {
			SearchHistorySettingsItem baseItem = getBaseItem(SettingsItemType.SEARCH_HISTORY, SearchHistorySettingsItem.class);
			settingsItems.add(new SearchHistorySettingsItem(app, baseItem, historyEntries));
		}
		return settingsItems;
	}

	public void setFile(File file) {
		this.file = file;
	}
}
