package net.osmand.plus.settings;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.SettingsHelper.AvoidRoadsSettingsItem;
import net.osmand.plus.SettingsHelper.FileSettingsItem;
import net.osmand.plus.SettingsHelper.FileSettingsItem.FileSubtype;
import net.osmand.plus.SettingsHelper.ImportAsyncTask;
import net.osmand.plus.SettingsHelper.ImportType;
import net.osmand.plus.SettingsHelper.MapSourcesSettingsItem;
import net.osmand.plus.SettingsHelper.PoiUiFilterSettingsItem;
import net.osmand.plus.SettingsHelper.ProfileSettingsItem;
import net.osmand.plus.SettingsHelper.QuickActionsSettingsItem;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.SettingsHelper.SettingsItemType;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.ExportImportSettingsAdapter.Type;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportSettingsFragment extends BaseOsmAndFragment
		implements View.OnClickListener {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();
	public static final Log LOG = PlatformUtil.getLog(ImportSettingsFragment.class.getSimpleName());
	private static final String DUPLICATES_START_TIME_KEY = "duplicates_start_time";
	private static final long MIN_DELAY_TIME_MS = 500;
	static final String IMPORT_SETTINGS_TAG = "import_settings_tag";
	private OsmandApplication app;
	private ExportImportSettingsAdapter adapter;
	private ExpandableListView expandableList;
	private TextViewEx selectBtn;
	private TextView description;
	private List<SettingsItem> settingsItems;
	private File file;
	private boolean allSelected;
	private boolean nightMode;
	private LinearLayout buttonsContainer;
	private ProgressBar progressBar;
	private CollapsingToolbarLayout toolbarLayout;
	private SettingsHelper settingsHelper;
	private long duplicateStartTime;

	public static void showInstance(@NonNull FragmentManager fm, @NonNull List<SettingsItem> settingsItems, @NonNull File file) {
		ImportSettingsFragment fragment = new ImportSettingsFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction().
				replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(IMPORT_SETTINGS_TAG)
				.commitAllowingStateLoss();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			duplicateStartTime = savedInstanceState.getLong(DUPLICATES_START_TIME_KEY);
		}
		app = requireMyApplication();
		settingsHelper = app.getSettingsHelper();
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import, container, false);
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		TextViewEx continueBtn = root.findViewById(R.id.continue_button);
		toolbarLayout = root.findViewById(R.id.toolbar_layout);
		selectBtn = root.findViewById(R.id.select_button);
		expandableList = root.findViewById(R.id.list);
		buttonsContainer = root.findViewById(R.id.buttons_container);
		progressBar = root.findViewById(R.id.progress_bar);
		setupToolbar(toolbar);
		ViewCompat.setNestedScrollingEnabled(expandableList, true);
		View header = inflater.inflate(R.layout.list_item_description_header, null);
		description = header.findViewById(R.id.description);
		description.setText(R.string.select_data_to_import);
		expandableList.addHeaderView(header);
		continueBtn.setOnClickListener(this);
		selectBtn.setOnClickListener(this);
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		ViewTreeObserver treeObserver = buttonsContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (buttonsContainer != null) {
					ViewTreeObserver vts = buttonsContainer.getViewTreeObserver();
					int height = buttonsContainer.getMeasuredHeight();
					expandableList.setPadding(0, 0, 0, height);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						vts.removeOnGlobalLayoutListener(this);
					} else {
						vts.removeGlobalOnLayoutListener(this);
					}
				}
			}
		});
		return root;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
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

		adapter = new ExportImportSettingsAdapter(app, nightMode, true);
		Map<Type, List<?>> itemsMap = new HashMap<>();
		if (settingsItems != null) {
			itemsMap = getSettingsToOperate(settingsItems, true);
			adapter.updateSettingsList(itemsMap);
		}
		expandableList.setAdapter(adapter);
		toolbarLayout.setTitle(getString(R.string.shared_string_import));

		ImportType importTaskType = settingsHelper.getImportTaskType();
		if (importTaskType == ImportType.CHECK_DUPLICATES && !settingsHelper.isImportDone()) {
			updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
		} else if (importTaskType == ImportType.IMPORT) {
			updateUi(R.string.shared_string_importing, R.string.importing_from);
		} else {
			toolbarLayout.setTitle(getString(R.string.shared_string_import));
		}
		if (itemsMap.size() == 1 && itemsMap.containsKey(Type.PROFILE)) {
			expandableList.expandGroup(0);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(DUPLICATES_START_TIME_KEY, duplicateStartTime);
	}

	@Override
	public void onResume() {
		super.onResume();
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			((MapActivity) activity).closeDrawer();
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.select_button: {
				allSelected = !allSelected;
				selectBtn.setText(allSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
				adapter.selectAll(allSelected);
				break;
			}
			case R.id.continue_button: {
				if (adapter.getDataToOperate().isEmpty()) {
					app.showShortToastMessage(getString(R.string.shared_string_nothing_selected));
				} else {
					importItems();
				}
				break;
			}
		}
	}

	private void updateUi(int toolbarTitleRes, int descriptionRes) {
		if (file != null) {
			String fileName = file.getName();
			toolbarLayout.setTitle(getString(toolbarTitleRes));
			description.setText(UiUtilities.createSpannableString(
					String.format(getString(descriptionRes), fileName),
					fileName,
					new StyleSpan(Typeface.BOLD)
			));
			buttonsContainer.setVisibility(View.GONE);
			progressBar.setVisibility(View.VISIBLE);
			adapter.clearSettingsList();
		}
	}

	private void importItems() {
		updateUi(R.string.shared_string_preparing, R.string.checking_for_duplicate_description);
		List<SettingsItem> selectedItems = getSettingsItemsFromData(adapter.getDataToOperate());
		if (file != null && settingsItems != null) {
			duplicateStartTime = System.currentTimeMillis();
			settingsHelper.checkDuplicates(file, settingsItems, selectedItems, getDuplicatesListener());
		}
	}

	private SettingsHelper.SettingsImportListener getImportListener() {
		return new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items) {
				FragmentManager fm = getFragmentManager();
				if (succeed) {
					app.getRendererRegistry().updateExternalRenderers();
					AppInitializer.loadRoutingFiles(app, new AppInitializer.LoadRoutingFilesCallback() {
						@Override
						public void onRoutingFilesLoaded() {
						}
					});
					if (fm != null && file != null) {
						ImportCompleteFragment.showInstance(fm, items, file.getName());
					}
				}
			}
		};
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
				ImportDuplicatesFragment.showInstance(fm, duplicates, items, file);
			}
		}
	}

	private void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			getFragmentManager().popBackStack(IMPORT_SETTINGS_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void setSettingsItems(List<SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	private List<SettingsItem> getSettingsItemsFromData(List<Object> dataToOperate) {
		List<SettingsItem> settingsItems = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		for (Object object : dataToOperate) {
			if (object instanceof ApplicationMode.ApplicationModeBean) {
				settingsItems.add(new SettingsHelper.ProfileSettingsItem(app, (ApplicationMode.ApplicationModeBean) object));
			}
			if (object instanceof QuickAction) {
				quickActions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				poiUIFilters.add((PoiUIFilter) object);
			} else if (object instanceof TileSourceManager.TileSourceTemplate
					|| object instanceof SQLiteTileSource) {
				tileSourceTemplates.add((ITileSource) object);
			} else if (object instanceof File) {
				settingsItems.add(new FileSettingsItem(app, (File) object));
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			}
		}
		if (!quickActions.isEmpty()) {
			settingsItems.add(new QuickActionsSettingsItem(app, quickActions));
		}
		if (!poiUIFilters.isEmpty()) {
			settingsItems.add(new SettingsHelper.PoiUiFilterSettingsItem(app, poiUIFilters));
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsItems.add(new SettingsHelper.MapSourcesSettingsItem(app, tileSourceTemplates));
		}
		if (!avoidRoads.isEmpty()) {
			settingsItems.add(new SettingsHelper.AvoidRoadsSettingsItem(app, avoidRoads));
		}
		return settingsItems;
	}

	public static Map<Type, List<?>> getSettingsToOperate(List<SettingsItem> settingsItems, boolean collectedItems) {
		Map<Type, List<?>> settingsToOperate = new HashMap<>();
		List<ApplicationMode.ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();

		for (SettingsItem item : settingsItems) {
			if (item.getType().equals(SettingsItemType.PROFILE)) {
				profiles.add(((ProfileSettingsItem) item).getModeBean());
			} else if (item.getType().equals(SettingsItemType.QUICK_ACTIONS)) {
				QuickActionsSettingsItem quickActionsItem = (QuickActionsSettingsItem) item;
				if (collectedItems) {
					quickActions.addAll(quickActionsItem.getItems());
				} else {
					quickActions.addAll(quickActionsItem.getImportItems());
				}
			} else if (item.getType().equals(SettingsItemType.POI_UI_FILTERS)) {
				PoiUiFilterSettingsItem poiUiFilterItem = (PoiUiFilterSettingsItem) item;
				if (collectedItems) {
					poiUIFilters.addAll(poiUiFilterItem.getItems());
				} else {
					poiUIFilters.addAll(poiUiFilterItem.getImportItems());
				}
			} else if (item.getType().equals(SettingsItemType.MAP_SOURCES)) {
				MapSourcesSettingsItem mapSourcesItem = (MapSourcesSettingsItem) item;
				if (collectedItems) {
					tileSourceTemplates.addAll(mapSourcesItem.getItems());
				} else {
					tileSourceTemplates.addAll(mapSourcesItem.getImportItems());
				}
			} else if (item.getType().equals(SettingsItemType.FILE)) {
				FileSettingsItem fileItem = (FileSettingsItem) item;
				if (fileItem.getSubtype() == FileSubtype.RENDERING_STYLE) {
					renderFilesList.add(fileItem.getFile());
				} else if (fileItem.getSubtype() == FileSubtype.ROUTING_CONFIG) {
					routingFilesList.add(fileItem.getFile());
				}
			} else if (item.getType().equals(SettingsItemType.AVOID_ROADS)) {
				AvoidRoadsSettingsItem avoidRoadsItem = (AvoidRoadsSettingsItem) item;
				if (collectedItems) {
					avoidRoads.addAll(avoidRoadsItem.getItems());
				} else {
					avoidRoads.addAll(avoidRoadsItem.getImportItems());
				}
			}
		}

		if (!profiles.isEmpty()) {
			settingsToOperate.put(Type.PROFILE, profiles);
		}
		if (!quickActions.isEmpty()) {
			settingsToOperate.put(Type.QUICK_ACTIONS, quickActions);
		}
		if (!poiUIFilters.isEmpty()) {
			settingsToOperate.put(Type.POI_TYPES, poiUIFilters);
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsToOperate.put(Type.MAP_SOURCES, tileSourceTemplates);
		}
		if (!renderFilesList.isEmpty()) {
			settingsToOperate.put(Type.CUSTOM_RENDER_STYLE, renderFilesList);
		}
		if (!routingFilesList.isEmpty()) {
			settingsToOperate.put(Type.CUSTOM_ROUTING, routingFilesList);
		}
		if (!avoidRoads.isEmpty()) {
			settingsToOperate.put(Type.AVOID_ROADS, avoidRoads);
		}
		return settingsToOperate;
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	public void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismissFragment();
			}
		});
		dismissDialog.show();
	}

	private void setupToolbar(Toolbar toolbar) {
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_action_close, nightMode
				? getResources().getColor(R.color.active_buttons_and_links_text_dark)
				: getResources().getColor(R.color.active_buttons_and_links_text_light)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showExitDialog();
			}
		});
	}

	public void setFile(File file) {
		this.file = file;
	}
}
