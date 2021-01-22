package net.osmand.plus.settings.fragments;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.AndroidUtils;
import net.osmand.map.ITileSource;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportAsyncTask;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportType;
import net.osmand.plus.settings.backend.backup.SettingsHelper.SettingsImportListener;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.view.ComplexButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.settings.backend.backup.FileSettingsItem.FileSubtype;
import static net.osmand.plus.settings.fragments.BaseSettingsListFragment.SETTINGS_LIST_TAG;


public class ImportDuplicatesFragment extends BaseOsmAndFragment {

	public static final String TAG = ImportDuplicatesFragment.class.getSimpleName();
	private OsmandApplication app;
	private RecyclerView list;
	private LinearLayout buttonsContainer;
	private NestedScrollView nestedScroll;
	private List<? super Object> duplicatesList;
	private List<SettingsItem> settingsItems;
	private File file;
	private boolean nightMode;
	private ProgressBar progressBar;
	private CollapsingToolbarLayout toolbarLayout;
	private TextView description;
	private SettingsHelper settingsHelper;

	public static void showInstance(@NonNull FragmentManager fm, List<? super Object> duplicatesList,
									List<SettingsItem> settingsItems, File file, Fragment targetFragment) {
		ImportDuplicatesFragment fragment = new ImportDuplicatesFragment();
		fragment.setTargetFragment(targetFragment, 0);
		fragment.setDuplicatesList(duplicatesList);
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(SETTINGS_LIST_TAG)
				.commitAllowingStateLoss();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settingsHelper = app.getSettingsHelper();
		nightMode = !app.getSettings().isLightContent();
		ImportAsyncTask importTask = settingsHelper.getImportTask();
		if (importTask != null) {
			if (settingsItems == null) {
				settingsItems = importTask.getSelectedItems();
			}
			if (duplicatesList == null) {
				duplicatesList = importTask.getDuplicates();
			}
			if (file == null) {
				file = importTask.getFile();
			}
			Fragment target = getTargetFragment();
			if (target instanceof ImportSettingsFragment) {
				SettingsImportListener importListener = ((ImportSettingsFragment) target).getImportListener();
				importTask.setImportListener(importListener);
			}
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import_duplicates, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		setupToolbar(toolbar);
		ComplexButton replaceAllBtn = root.findViewById(R.id.replace_all_btn);
		ComplexButton keepBothBtn = root.findViewById(R.id.keep_both_btn);
		buttonsContainer = root.findViewById(R.id.buttons_container);
		nestedScroll = root.findViewById(R.id.nested_scroll);
		description = root.findViewById(R.id.description);
		progressBar = root.findViewById(R.id.progress_bar);
		toolbarLayout = root.findViewById(R.id.toolbar_layout);
		keepBothBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_keep_both,
				nightMode
						? getResources().getColor(R.color.icon_color_active_dark)
						: getResources().getColor(R.color.icon_color_active_light))
		);
		replaceAllBtn.setIcon(getPaintedContentIcon(R.drawable.ic_action_replace,
				nightMode
						? getResources().getColor(R.color.active_buttons_and_links_text_dark)
						: getResources().getColor(R.color.active_buttons_and_links_text_light))
		);
		keepBothBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				importItems(false);
			}
		});
		replaceAllBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				importItems(true);
			}
		});
		list = root.findViewById(R.id.list);
		ViewCompat.setNestedScrollingEnabled(list, false);
		ViewTreeObserver treeObserver = buttonsContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (buttonsContainer != null) {
					ViewTreeObserver vts = buttonsContainer.getViewTreeObserver();
					int height = buttonsContainer.getMeasuredHeight();
					nestedScroll.setPadding(0, 0, 0, height);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						vts.removeOnGlobalLayoutListener(this);
					} else {
						vts.removeGlobalOnLayoutListener(this);
					}
				}
			}
		});
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}

		return root;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (duplicatesList != null) {
			DuplicatesSettingsAdapter adapter = new DuplicatesSettingsAdapter(app, prepareDuplicates(duplicatesList), nightMode);
			list.setLayoutManager(new LinearLayoutManager(getMyApplication()));
			list.setAdapter(adapter);
		}
		if (settingsHelper.getImportTaskType() == ImportType.IMPORT) {
			setupImportingUi();
		} else {
			toolbarLayout.setTitle(getString(R.string.import_duplicates_title));
		}
		toolbarLayout.setTitle(getString(R.string.import_duplicates_title));
	}

	private List<Object> prepareDuplicates(List<? super Object> duplicatesList) {
		List<? super Object> duplicates = new ArrayList<>();
		List<ApplicationMode.ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> actions = new ArrayList<>();
		List<PoiUIFilter> filters = new ArrayList<>();
		List<ITileSource> tileSources = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<File> multimediaFilesList = new ArrayList<>();
		List<File> trackFilesList = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		List<FavoriteGroup> favoriteGroups = new ArrayList<>();
		List<OsmNotesPoint> osmNotesPointList = new ArrayList<>();
		List<OpenstreetmapPoint> osmEditsPointList = new ArrayList<>();
		List<File> ttsVoiceFilesList = new ArrayList<>();
		List<File> voiceFilesList = new ArrayList<>();
		List<File> mapFilesList = new ArrayList<>();
		List<MapMarker> mapMarkers = new ArrayList<>();
		List<MapMarker> mapMarkersGroups = new ArrayList<>();
		List<HistoryEntry> historyEntries = new ArrayList<>();
		List<OnlineRoutingEngine> onlineRoutingEngines = new ArrayList<>();

		for (Object object : duplicatesList) {
			if (object instanceof ApplicationMode.ApplicationModeBean) {
				profiles.add((ApplicationMode.ApplicationModeBean) object);
			} else if (object instanceof QuickAction) {
				actions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				filters.add((PoiUIFilter) object);
			} else if (object instanceof ITileSource) {
				tileSources.add((ITileSource) object);
			} else if (object instanceof File) {
				File file = (File) object;
				FileSubtype fileSubtype = FileSubtype.getSubtypeByPath(app, file.getPath());
				if (fileSubtype == FileSubtype.RENDERING_STYLE) {
					renderFilesList.add(file);
				} else if (fileSubtype == FileSubtype.ROUTING_CONFIG) {
					routingFilesList.add(file);
				} else if (fileSubtype == FileSubtype.MULTIMEDIA_NOTES) {
					multimediaFilesList.add(file);
				} else if (fileSubtype == FileSubtype.GPX) {
					trackFilesList.add(file);
				} else if (fileSubtype.isMap()) {
					mapFilesList.add(file);
				} else if (fileSubtype == FileSubtype.TTS_VOICE) {
					ttsVoiceFilesList.add(file);
				} else if (fileSubtype == FileSubtype.VOICE) {
					voiceFilesList.add(file);
				}
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			} else if (object instanceof FavoriteGroup) {
				favoriteGroups.add((FavoriteGroup) object);
			} else if (object instanceof OsmNotesPoint) {
				osmNotesPointList.add((OsmNotesPoint) object);
			} else if (object instanceof OpenstreetmapPoint) {
				osmEditsPointList.add((OpenstreetmapPoint) object);
			} else if (object instanceof MapMarker) {
				MapMarker mapMarker = (MapMarker) object;
				if (mapMarker.history) {
					mapMarkers.add(mapMarker);
				} else {
					mapMarkersGroups.add(mapMarker);
				}
			} else if (object instanceof HistoryEntry) {
				historyEntries.add((HistoryEntry) object);
			} else if (object instanceof OnlineRoutingEngine) {
				onlineRoutingEngines.add((OnlineRoutingEngine) object);
			}
		}
		if (!profiles.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_profiles));
			duplicates.addAll(profiles);
		}
		if (!actions.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_quick_actions));
			duplicates.addAll(actions);
		}
		if (!filters.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_poi_types));
			duplicates.addAll(filters);
		}
		if (!tileSources.isEmpty()) {
			duplicates.add(getString(R.string.quick_action_map_source_title));
			duplicates.addAll(tileSources);
		}
		if (!routingFilesList.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_routing));
			duplicates.addAll(routingFilesList);
		}
		if (!renderFilesList.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_rendering_style));
			duplicates.addAll(renderFilesList);
		}
		if (!multimediaFilesList.isEmpty()) {
			duplicates.add(getString(R.string.audionotes_plugin_name));
			duplicates.addAll(multimediaFilesList);
		}
		if (!trackFilesList.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_tracks));
			duplicates.addAll(trackFilesList);
		}
		if (!avoidRoads.isEmpty()) {
			duplicates.add(getString(R.string.avoid_road));
			duplicates.addAll(avoidRoads);
		}
		if (!favoriteGroups.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_favorites));
			duplicates.addAll(favoriteGroups);
		}
		if (!osmNotesPointList.isEmpty()) {
			duplicates.add(getString(R.string.osm_notes));
			duplicates.addAll(osmNotesPointList);
		}
		if (!osmEditsPointList.isEmpty()) {
			duplicates.add(getString(R.string.osm_edits));
			duplicates.addAll(osmEditsPointList);
		}
		if (!mapFilesList.isEmpty()) {
			duplicates.add(getString(R.string.shared_string_maps));
			duplicates.addAll(mapFilesList);
		}
		if (!ttsVoiceFilesList.isEmpty()) {
			duplicates.add(getString(R.string.local_indexes_cat_tts));
			duplicates.addAll(ttsVoiceFilesList);
		}
		if (!voiceFilesList.isEmpty()) {
			duplicates.add(getString(R.string.local_indexes_cat_voice));
			duplicates.addAll(voiceFilesList);
		}
		if (!mapMarkers.isEmpty()) {
			duplicates.add(getString(R.string.map_markers));
			duplicates.addAll(mapMarkers);
		}
		if (!mapMarkersGroups.isEmpty()) {
			duplicates.add(getString(R.string.markers_history));
			duplicates.addAll(mapMarkersGroups);
		}
		if (!onlineRoutingEngines.isEmpty()) {
			duplicates.add(getString(R.string.online_routing_engines));
			duplicates.addAll(onlineRoutingEngines);
		}
		return duplicates;
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	private void importItems(boolean shouldReplace) {
		if (settingsItems != null && file != null) {
			setupImportingUi();
			for (SettingsItem item : settingsItems) {
				item.setShouldReplace(shouldReplace);
			}
			Fragment target = getTargetFragment();
			if (target instanceof ImportSettingsFragment) {
				SettingsImportListener importListener = ((ImportSettingsFragment) target).getImportListener();
				settingsHelper.importSettings(file, settingsItems, "", 1, importListener);
			}
		}
	}

	private void setupImportingUi() {
		toolbarLayout.setTitle(getString(R.string.shared_string_importing));
		description.setText(UiUtilities.createSpannableString(
				String.format(getString(R.string.importing_from), file.getName()),
				new StyleSpan(Typeface.BOLD), file.getName()
		));
		progressBar.setVisibility(View.VISIBLE);
		list.setVisibility(View.GONE);
		buttonsContainer.setVisibility(View.GONE);
	}

	private void setupToolbar(Toolbar toolbar) {
		toolbar.setTitle(R.string.import_duplicates_title);
		toolbar.setNavigationIcon(getPaintedContentIcon(
				AndroidUtils.getNavigationIconResId(app),
				nightMode
						? getResources().getColor(R.color.active_buttons_and_links_text_dark)
						: getResources().getColor(R.color.active_buttons_and_links_text_light)));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = getFragmentManager();
				if (fm != null && !fm.isStateSaved()) {
					fm.popBackStackImmediate();
				}
			}
		});
	}

	public void setDuplicatesList(List<? super Object> duplicatesList) {
		this.duplicatesList = duplicatesList;
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	public void setFile(File file) {
		this.file = file;
	}
}
