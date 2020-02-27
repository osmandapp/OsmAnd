package net.osmand.plus.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.AdditionalDataWrapper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.widgets.TextViewEx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportSettingsFragment extends BaseOsmAndFragment
		implements View.OnClickListener {

	public static final String TAG = ImportSettingsFragment.class.getSimpleName();
	private OsmandApplication app;
	private ExportImportSettingsAdapter adapter;
	private ExpandableListView expandableList;
	private TextViewEx selectBtn;
	private List<SettingsItem> settingsItems;
	private File file;
	private boolean allSelected;
	private boolean nightMode;

	public static void showInstance(@NonNull FragmentManager fm, List<SettingsItem> settingsItems, @NonNull File file) {
		ImportSettingsFragment fragment = new ImportSettingsFragment();
		fragment.setSettingsItems(settingsItems);
		fragment.setFile(file);
		fm.beginTransaction().replace(R.id.fragmentContainer, fragment, TAG).addToBackStack(null).commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
		if (settingsItems == null) {
			settingsItems = app.getSettingsHelper().getSettingsItems();
		}
		if (file == null) {
			file = app.getSettingsHelper().getSettingsFile();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(app, nightMode);
		View root = inflater.inflate(R.layout.fragment_import, container, false);
		setupToolbar((Toolbar) root.findViewById(R.id.toolbar));
		TextViewEx continueBtn = root.findViewById(R.id.continue_button);
		selectBtn = root.findViewById(R.id.select_button);
		expandableList = root.findViewById(R.id.list);
		ViewCompat.setNestedScrollingEnabled(expandableList, true);
		View header = inflater.inflate(R.layout.list_item_description_header, null);
		TextView description = header.findViewById(R.id.description);
		description.setText(R.string.select_data_to_import);
		expandableList.addHeaderView(header);
		continueBtn.setOnClickListener(this);
		selectBtn.setOnClickListener(this);
		if (Build.VERSION.SDK_INT >= 21) {
			AndroidUtils.addStatusBarPadding21v(app, root);
		}
		return root;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		adapter = new ExportImportSettingsAdapter(app, getSettingsToOperate(), nightMode, true);
		expandableList.setAdapter(adapter);
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

	private void importItems() {
		List<SettingsItem> settingsItems = getSettingsItemsFromData(adapter.getDataToOperate());
		List<Object> duplicateItems = getDuplicatesData(settingsItems);
		if (duplicateItems.isEmpty()) {
			app.getSettingsHelper().importSettings(file, settingsItems, "", 1, new SettingsHelper.SettingsImportListener() {
				@Override
				public void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsHelper.SettingsItem> items) {
					if (succeed) {
						app.showShortToastMessage(app.getString(R.string.file_imported_successfully, file.getName()));
						app.getRendererRegistry().updateExternalRenderers();
						AppInitializer.loadRoutingFiles(app, new AppInitializer.LoadRoutingFilesCallback() {
							@Override
							public void onRoutingFilesLoaded() {
							}
						});
					} else if (empty) {
						app.showShortToastMessage(app.getString(R.string.file_import_error, file.getName(), app.getString(R.string.shared_string_unexpected_error)));
					}
				}
			});
			FragmentManager fm = getFragmentManager();
			if (fm != null) {
				fm.popBackStackImmediate();
			}
		} else {
			ImportDuplicatesFragment.showInstance(requireActivity().getSupportFragmentManager(), duplicateItems, settingsItems, file);
		}
	}

	public static List<Object> getDuplicatesData(List<SettingsItem> items) {
		List<Object> duplicateItems = new ArrayList<>();
		for (SettingsItem item : items) {
			if (item instanceof SettingsHelper.ProfileSettingsItem) {
				if (item.exists()) {
					duplicateItems.add(((SettingsHelper.ProfileSettingsItem) item).getModeBean());
				}
			} else if (item instanceof SettingsHelper.QuickActionSettingsItem) {
				List<QuickAction> duplicates = ((SettingsHelper.QuickActionSettingsItem) item).excludeDuplicateItems();
				if (!duplicates.isEmpty()) {
					duplicateItems.addAll(duplicates);
				}
			} else if (item instanceof SettingsHelper.PoiUiFilterSettingsItem) {
				List<PoiUIFilter> duplicates = ((SettingsHelper.PoiUiFilterSettingsItem) item).excludeDuplicateItems();
				if (!duplicates.isEmpty()) {
					duplicateItems.addAll(duplicates);
				}
			} else if (item instanceof SettingsHelper.MapSourcesSettingsItem) {
				List<ITileSource> duplicates = ((SettingsHelper.MapSourcesSettingsItem) item).excludeDuplicateItems();
				if (!duplicates.isEmpty()) {
					duplicateItems.addAll(duplicates);
				}
			} else if (item instanceof SettingsHelper.FileSettingsItem) {
				if (item.exists()) {
					duplicateItems.add(((SettingsHelper.FileSettingsItem) item).getFile());
				}
			} else if (item instanceof SettingsHelper.AvoidRoadsSettingsItem) {
				List<AvoidRoadInfo> avoidRoads = ((SettingsHelper.AvoidRoadsSettingsItem) item).excludeDuplicateItems();
				if (!avoidRoads.isEmpty()) {
					duplicateItems.addAll(avoidRoads);
				}
			}
		}
		return duplicateItems;
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
				settingsItems.add(new SettingsHelper.FileSettingsItem(app, (File) object));
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			}
		}
		if (!quickActions.isEmpty()) {
			settingsItems.add(new SettingsHelper.QuickActionSettingsItem(app, quickActions));
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

	private List<AdditionalDataWrapper> getSettingsToOperate() {
		List<AdditionalDataWrapper> settingsToOperate = new ArrayList<>();
		List<ApplicationMode.ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();

		for (SettingsHelper.SettingsItem item : settingsItems) {
			if (item.getType().equals(SettingsHelper.SettingsItemType.PROFILE)) {
				profiles.add(((SettingsHelper.ProfileSettingsItem) item).getModeBean());
			} else if (item.getType().equals(SettingsHelper.SettingsItemType.QUICK_ACTION)) {
				quickActions.addAll(((SettingsHelper.QuickActionSettingsItem) item).getItems());
			} else if (item.getType().equals(SettingsHelper.SettingsItemType.POI_UI_FILTERS)) {
				poiUIFilters.addAll(((SettingsHelper.PoiUiFilterSettingsItem) item).getItems());
			} else if (item.getType().equals(SettingsHelper.SettingsItemType.MAP_SOURCES)) {
				tileSourceTemplates.addAll(((SettingsHelper.MapSourcesSettingsItem) item).getItems());
			} else if (item.getType().equals(SettingsHelper.SettingsItemType.FILE)) {
				if (item.getName().startsWith("/rendering/")) {
					renderFilesList.add(((SettingsHelper.FileSettingsItem) item).getFile());
				} else if (item.getName().startsWith("/routing/")) {
					routingFilesList.add(((SettingsHelper.FileSettingsItem) item).getFile());
				}
			} else if (item.getType().equals(SettingsHelper.SettingsItemType.AVOID_ROADS)) {
				avoidRoads.addAll(((SettingsHelper.AvoidRoadsSettingsItem) item).getItems());
			}
		}

		if (!profiles.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.PROFILE,
					profiles));
		}
		if (!quickActions.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.QUICK_ACTIONS,
					quickActions));
		}
		if (!poiUIFilters.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.POI_TYPES,
					poiUIFilters));
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.MAP_SOURCES,
					tileSourceTemplates
			));
		}
		if (!renderFilesList.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.CUSTOM_RENDER_STYLE,
					renderFilesList
			));
		}
		if (!routingFilesList.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.CUSTOM_ROUTING,
					routingFilesList
			));
		}
		if (!avoidRoads.isEmpty()) {
			settingsToOperate.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.AVOID_ROADS,
					avoidRoads
			));
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
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					fm.popBackStackImmediate();
				}
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
