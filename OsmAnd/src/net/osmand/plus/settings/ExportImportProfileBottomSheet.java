package net.osmand.plus.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.SettingsHelper;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.AdditionalDataWrapper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportImportProfileBottomSheet extends BasePreferenceBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(ExportImportProfileBottomSheet.class);

	public static final String TAG = ExportImportProfileBottomSheet.class.getSimpleName();

	private static final String STATE_KEY = "EXPORT_IMPORT_DIALOG_STATE_KEY";

	private static final String INCLUDE_ADDITIONAL_DATA_KEY = "INCLUDE_ADDITIONAL_DATA_KEY";

	private boolean includeAdditionalData = false;

	private boolean containsAdditionalData = false;

	private OsmandApplication app;

	private ApplicationMode profile;

	private State state;

	private List<AdditionalDataWrapper> dataList = new ArrayList<>();

	private List<SettingsHelper.SettingsItem> settingsItems;

	private ExpandableListView listView;

	private ExportImportSettingsAdapter adapter;

	private SettingsHelper.ProfileSettingsItem profileSettingsItem;

	private File file;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			includeAdditionalData = savedInstanceState.getBoolean(INCLUDE_ADDITIONAL_DATA_KEY);
		}
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		Bundle bundle = getArguments();
		if (bundle != null) {
			this.state = (State) getArguments().getSerializable(STATE_KEY);
		}
		if (state == State.IMPORT) {
			if (settingsItems == null) {
				settingsItems = app.getSettingsHelper().getSettingsItems();
			}
			if (file == null) {
				file = app.getSettingsHelper().getSettingsFile();
			}
			containsAdditionalData = checkAdditionalDataContains();
		} else {
			dataList = getAdditionalData();
//			for (AdditionalDataWrapper dataWrapper : dataList) {
//				dataToOperate.addAll(dataWrapper.getItems());
//			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(INCLUDE_ADDITIONAL_DATA_KEY, includeAdditionalData);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final Context context = getContext();
		if (context == null) {
			return;
		}
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);

		profile = state == State.IMPORT ? getAppModeFromSettingsItems() : getAppMode();

		int profileColor = profile.getIconColorInfo().getColor(nightMode);
		int colorNoAlpha = ContextCompat.getColor(context, profileColor);

		Drawable backgroundIcon = UiUtilities.getColoredSelectableDrawable(context, colorNoAlpha, 0.3f);
		Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.10f)), backgroundIcon};

		items.add(new TitleItem(state == State.EXPORT ?
				getString(R.string.export_profile)
				: getString(R.string.import_profile)));

		BaseBottomSheetItem profileItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(true)
				.setCompoundButtonColorId(profileColor)
				.setButtonTintList(ColorStateList.valueOf(getResolvedColor(profileColor)))
				.setDescription(BaseSettingsFragment.getAppModeDescription(context, profile))
				.setIcon(getIcon(profile.getIconRes(), profileColor))
				.setTitle(profile.toHumanString())
				.setBackground(new LayerDrawable(layers))
				.setLayoutId(R.layout.preference_profile_item_with_radio_btn)
				.create();
		items.add(profileItem);

		if (state == State.IMPORT && containsAdditionalData || state == State.EXPORT && !dataList.isEmpty()) {
			BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(state == State.EXPORT ?
							getString(R.string.export_profile_dialog_description)
							: getString(R.string.import_profile_dialog_description))
					.setLayoutId(R.layout.bottom_sheet_item_pref_info)
					.create();
			items.add(descriptionItem);

			final View additionalDataView = inflater.inflate(R.layout.bottom_sheet_item_additional_data, null);
			listView = additionalDataView.findViewById(R.id.list);
			SwitchCompat switchItem = additionalDataView.findViewById(R.id.switchItem);
			switchItem.setTextColor(getResources().getColor(nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light));
			switchItem.setChecked(includeAdditionalData);
			switchItem.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					includeAdditionalData = !includeAdditionalData;
					listView.setVisibility(includeAdditionalData ?
							View.VISIBLE : View.GONE);
//					if (includeAdditionalData && state == State.IMPORT) {
//						updateDataToOperateFromSettingsItems();
//					}
					setupHeightAndBackground(getView());
				}
			});
			listView.setVisibility(includeAdditionalData ? View.VISIBLE : View.GONE);
			adapter = new ExportImportSettingsAdapter(app, dataList, nightMode, false);
			listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
				@Override
				public void onGroupExpand(int i) {
					setupHeightAndBackground(getView());
				}
			});
			listView.setAdapter(adapter);
			final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
					.setCustomView(additionalDataView)
					.create();
			items.add(titleItem);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return state == State.EXPORT ? R.string.shared_string_export : R.string.shared_string_import;
	}

	@Override
	protected void onRightBottomButtonClick() {
		super.onRightBottomButtonClick();
		if (state == State.EXPORT) {
			prepareFile();
		} else {
			importSettings();
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected boolean useScrollableItemsContainer() {
		return false;
	}

	private ApplicationMode getAppModeFromSettingsItems() {
		for (SettingsHelper.SettingsItem item : settingsItems) {
			if (item.getType().equals(SettingsHelper.SettingsItemType.PROFILE)) {
				profileSettingsItem = ((SettingsHelper.ProfileSettingsItem) item);
				return ((SettingsHelper.ProfileSettingsItem) item).getAppMode();
			}
		}
		return getAppMode();
	}

	private List<AdditionalDataWrapper> getAdditionalData() {
		List<AdditionalDataWrapper> dataList = new ArrayList<>();

		QuickActionFactory factory = new QuickActionFactory();
		List<QuickAction> actionsList = factory.parseActiveActionsList(app.getSettings().QUICK_ACTION_LIST.get());
		if (!actionsList.isEmpty()) {
			dataList.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.QUICK_ACTIONS, actionsList));
		}

		List<PoiUIFilter> poiList = app.getPoiFilters().getUserDefinedPoiFilters(false);
		if (!poiList.isEmpty()) {
			dataList.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.POI_TYPES,
					poiList
			));
		}

		List<ITileSource> iTileSources = new ArrayList<>();
		final LinkedHashMap<String, String> tileSourceEntries = new LinkedHashMap<>(app.getSettings().getTileSourceEntries(true));
		for (Map.Entry<String, String> entry : tileSourceEntries.entrySet()) {
			File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + entry.getKey());
			if (f != null) {
				ITileSource template;
				if (f.getName().endsWith(SQLiteTileSource.EXT)) {
					template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
				} else {
					template = TileSourceManager.createTileSourceTemplate(f);
				}
				if (template != null && template.getUrlTemplate() != null) {
					iTileSources.add(template);
				}
			}
		}
		if (!iTileSources.isEmpty()) {
			dataList.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.MAP_SOURCES,
					iTileSources
			));
		}

		Map<String, File> externalRenderers = app.getRendererRegistry().getExternalRenderers();
		if (!externalRenderers.isEmpty()) {
			dataList.add(new AdditionalDataWrapper(
					AdditionalDataWrapper.Type.CUSTOM_RENDER_STYLE,
					new ArrayList<>(externalRenderers.values())
			));
		}

		File routingProfilesFolder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		if (routingProfilesFolder.exists() && routingProfilesFolder.isDirectory()) {
			File[] fl = routingProfilesFolder.listFiles();
			if (fl != null && fl.length > 0) {
				dataList.add(new AdditionalDataWrapper(
						AdditionalDataWrapper.Type.CUSTOM_ROUTING,
						Arrays.asList(fl)
				));
			}
		}

		return dataList;
	}

	private List<SettingsHelper.SettingsItem> prepareSettingsItemsForExport() {
		List<SettingsHelper.SettingsItem> settingsItems = new ArrayList<>();
		settingsItems.add(new SettingsHelper.ProfileSettingsItem(app.getSettings(), profile));
		if (includeAdditionalData) {
			settingsItems.addAll(prepareAdditionalSettingsItems());
		}
		return settingsItems;
	}

	private List<SettingsHelper.SettingsItem> prepareAdditionalSettingsItems() {
		List<SettingsHelper.SettingsItem> settingsItems = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		for (Object object : adapter.getDataToOperate()) {
			if (object instanceof QuickAction) {
				quickActions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				poiUIFilters.add((PoiUIFilter) object);
			} else if (object instanceof TileSourceManager.TileSourceTemplate
					|| object instanceof SQLiteTileSource) {
				tileSourceTemplates.add((ITileSource) object);
			} else if (object instanceof File) {
				settingsItems.add(new SettingsHelper.FileSettingsItem(app, (File) object));
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
		return settingsItems;
	}

	private Boolean checkAdditionalDataContains() {
		boolean containsData = false;
		for (SettingsHelper.SettingsItem item : settingsItems) {
			containsData = item.getType().equals(SettingsHelper.SettingsItemType.QUICK_ACTION)
					|| item.getType().equals(SettingsHelper.SettingsItemType.POI_UI_FILTERS)
					|| item.getType().equals(SettingsHelper.SettingsItemType.MAP_SOURCES)
					|| item.getType().equals(SettingsHelper.SettingsItemType.FILE);
			if (containsData) {
				break;
			}
		}
		return containsData;
	}

	private void importSettings() {
		List<SettingsHelper.SettingsItem> list = new ArrayList<>();
		list.add(profileSettingsItem);
		if (includeAdditionalData) {
			list.addAll(prepareAdditionalSettingsItems());
		}
		app.getSettingsHelper().importSettings(file, list, "", 1, new SettingsHelper.SettingsImportListener() {
			@Override
			public void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsHelper.SettingsItem> items) {
				if (succeed) {
					app.showShortToastMessage(app.getString(R.string.file_imported_successfully, file.getName()));
				} else if (empty) {
					app.showShortToastMessage(app.getString(R.string.file_import_error, file.getName(), app.getString(R.string.shared_string_unexpected_error)));
				}
			}
		});
		dismiss();
	}

	private void prepareFile() {
		if (app != null) {
			File tempDir = app.getAppPath(IndexConstants.TEMP_DIR);
			if (!tempDir.exists()) {
				tempDir.mkdirs();
			}
			String fileName = profile.toHumanString();
			app.getSettingsHelper().exportSettings(tempDir, fileName, new SettingsHelper.SettingsExportListener() {
				@Override
				public void onSettingsExportFinished(@NonNull File file, boolean succeed) {
					if (succeed) {
						shareProfile(file, profile);
					} else {
						app.showToastMessage(R.string.export_profile_failed);
					}
				}
			}, prepareSettingsItemsForExport());
		}
	}

	private void shareProfile(@NonNull File file, @NonNull ApplicationMode profile) {
		try {
			final Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.exported_osmand_profile, profile.toHumanString()));
			sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(getMyApplication(), file));
			sendIntent.setType("*/*");
			sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(sendIntent);
			dismiss();
		} catch (Exception e) {
			Toast.makeText(requireContext(), R.string.export_profile_failed, Toast.LENGTH_SHORT).show();
			LOG.error("Share profile error", e);
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
									   State state,
									   Fragment target,
									   @NonNull ApplicationMode appMode) {
		try {
			Bundle bundle = new Bundle();
			bundle.putSerializable(STATE_KEY, state);
			ExportImportProfileBottomSheet fragment = new ExportImportProfileBottomSheet();
			fragment.setArguments(bundle);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
									   State state,
									   File file,
									   List<SettingsHelper.SettingsItem> items) {
		try {
			Bundle bundle = new Bundle();
			bundle.putSerializable(STATE_KEY, state);
			ExportImportProfileBottomSheet fragment = new ExportImportProfileBottomSheet();
			fragment.setArguments(bundle);
			fragment.setSettingsItems(items);
			fragment.setFile(file);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	public void setSettingsItems(List<SettingsHelper.SettingsItem> settingsItems) {
		this.settingsItems = settingsItems;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public enum State {
		EXPORT,
		IMPORT
	}
}
