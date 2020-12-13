package net.osmand.plus.settings.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton.Builder;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.profiles.RoutingProfileDataObject.RoutingProfilesResources;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationMode.ApplicationModeBean;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.GlobalSettingsItem;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.settings.backend.backup.GpxSettingsItem;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter.OnItemSelectedListener;
import net.osmand.util.Algorithms;
import net.osmand.view.ThreeStateCheckbox;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

public class ExportItemsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ExportItemsBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ExportItemsBottomSheet.class);

	private static final String SETTINGS_TYPE_KEY = "settings_type_key";
	private static final String EXPORT_MODE_KEY = "export_mode_key";

	private OsmandApplication app;
	private UiUtilities uiUtilities;

	private ExportSettingsType type;
	private final List<Object> allItems = new ArrayList<>();
	private final List<Object> selectedItems = new ArrayList<>();

	private TextView selectedSize;
	private ThreeStateCheckbox checkBox;

	private int activeColorRes;
	private int secondaryColorRes;
	private boolean exportMode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			exportMode = savedInstanceState.getBoolean(EXPORT_MODE_KEY);
			type = ExportSettingsType.valueOf(savedInstanceState.getString(SETTINGS_TYPE_KEY));
		}
		Fragment target = getTargetFragment();
		if (target instanceof BaseSettingsListFragment) {
			BaseSettingsListFragment fragment = (BaseSettingsListFragment) target;
			List<Object> items = fragment.getItemsForType(type);
			if (items != null) {
				allItems.addAll(items);
			}
			List<Object> selectedItemsForType = fragment.getSelectedItemsForType(type);
			if (selectedItemsForType != null) {
				selectedItems.addAll(selectedItemsForType);
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		uiUtilities = app.getUIUtilities();
		activeColorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
		secondaryColorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;

		items.add(createTitleItem());
		items.add(new SimpleDividerItem(app));

		for (Object object : allItems) {
			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			Builder builder = (BottomSheetItemWithCompoundButton.Builder) new Builder()
					.setChecked(selectedItems.contains(object))
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean checked = !item[0].isChecked();
							item[0].setChecked(checked);
							if (checked) {
								selectedItems.add(item[0].getTag());
							} else {
								selectedItems.remove(item[0].getTag());
							}
							updateTitleView();
						}
					})
					.setTag(object);
			setupBottomSheetItem(builder, object);
			item[0] = builder.create();
			items.add(item[0]);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(EXPORT_MODE_KEY, exportMode);
		outState.putString(SETTINGS_TYPE_KEY, type.name());
	}

	private BaseBottomSheetItem createTitleItem() {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.settings_group_title, null);

		checkBox = view.findViewById(R.id.check_box);
		selectedSize = view.findViewById(R.id.selected_size);
		TextView textView = view.findViewById(R.id.title);
		textView.setText(type.getTitleId());
		view.findViewById(R.id.select_all_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				checkBox.performClick();
				boolean checked = checkBox.getState() == CHECKED;
				if (checked) {
					selectedItems.addAll(allItems);
				} else {
					selectedItems.clear();
				}
				updateTitleView();
				updateItems(checked);
			}
		});
		setupDescription(view);
		updateTitleView();

		return new SimpleBottomSheetItem.Builder().setCustomView(view).create();
	}

	private void updateTitleView() {
		if (Algorithms.isEmpty(selectedItems)) {
			checkBox.setState(UNCHECKED);
		} else {
			checkBox.setState(selectedItems.containsAll(allItems) ? CHECKED : MISC);
		}
		int checkBoxColor = checkBox.getState() == UNCHECKED ? secondaryColorRes : activeColorRes;
		CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, checkBoxColor)));

		String description;
		if (type == ExportSettingsType.OFFLINE_MAPS && !selectedItems.isEmpty()) {
			String size = AndroidUtils.formatSize(app, calculateSelectedItemsSize());
			String selected = getString(R.string.ltr_or_rtl_combine_via_slash, selectedItems.size(), allItems.size());
			description = getString(R.string.ltr_or_rtl_combine_via_comma, selected, size);
		} else {
			description = getString(R.string.ltr_or_rtl_combine_via_slash, selectedItems.size(), allItems.size());
		}
		selectedSize.setText(description);
	}

	private long calculateSelectedItemsSize() {
		long itemsSize = 0;
		for (int i = 0; i < allItems.size(); i++) {
			Object object = allItems.get(i);
			if (selectedItems.contains(object)) {
				if (object instanceof FileSettingsItem) {
					itemsSize += ((FileSettingsItem) object).getSize();
				} else if (object instanceof File) {
					itemsSize += ((File) object).length();
				}
			}
		}
		return itemsSize;
	}

	private void updateItems(boolean checked) {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment target = getTargetFragment();
		if (target instanceof OnItemSelectedListener) {
			OnItemSelectedListener listener = (OnItemSelectedListener) target;
			listener.onItemsSelected(type, selectedItems);
		}
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fm, @NonNull ExportSettingsType type,
									@NonNull BaseSettingsListFragment target, boolean exportMode) {
		try {
			if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
				ExportItemsBottomSheet fragment = new ExportItemsBottomSheet();
				fragment.type = type;
				fragment.exportMode = exportMode;
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	private String setupDescription(View view) {
		TextView description = view.findViewById(R.id.description);
		if (type == ExportSettingsType.FAVORITES) {
			description.setText(R.string.select_groups_for_import);
		} else {
			description.setText(R.string.select_items_for_import);
		}
		return null;
	}

	private void setupBottomSheetItem(Builder builder, Object object) {
		if (object instanceof ApplicationModeBean) {
			ApplicationModeBean modeBean = (ApplicationModeBean) object;
			String profileName = modeBean.userProfileName;
			if (Algorithms.isEmpty(profileName)) {
				ApplicationMode appMode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
				profileName = appMode.toHumanString();
			}
			builder.setTitle(profileName);

			String routingProfile = "";
			String routingProfileValue = modeBean.routingProfile;
			if (!routingProfileValue.isEmpty()) {
				try {
					routingProfile = getString(RoutingProfilesResources.valueOf(routingProfileValue.toUpperCase()).getStringRes());
					routingProfile = Algorithms.capitalizeFirstLetterAndLowercase(routingProfile);
				} catch (IllegalArgumentException e) {
					routingProfile = Algorithms.capitalizeFirstLetterAndLowercase(routingProfileValue);
					LOG.error("Error trying to get routing resource for " + routingProfileValue + "\n" + e);
				}
			}
			if (!Algorithms.isEmpty(routingProfile)) {
				builder.setDescription(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.nav_type_hint), routingProfile));
			} else {
				builder.setDescription(getString(R.string.profile_type_osmand_string));
			}
			int profileIconRes = AndroidUtils.getDrawableId(app, modeBean.iconName);
			ProfileIconColors iconColor = modeBean.iconColor;
			builder.setIcon(uiUtilities.getIcon(profileIconRes, iconColor.getColor(nightMode)));
		} else if (object instanceof QuickAction) {
			QuickAction quickAction = (QuickAction) object;
			builder.setTitle(quickAction.getName(app));
			builder.setIcon(uiUtilities.getIcon(quickAction.getIconRes(), activeColorRes));
		} else if (object instanceof PoiUIFilter) {
			PoiUIFilter poiUIFilter = (PoiUIFilter) object;
			builder.setTitle(poiUIFilter.getName());
			int iconRes = RenderingIcons.getBigIconResourceId(poiUIFilter.getIconId());
			builder.setIcon(uiUtilities.getIcon(iconRes != 0 ? iconRes : R.drawable.ic_action_user, activeColorRes));
		} else if (object instanceof TileSourceTemplate || object instanceof SQLiteTileSource) {
			ITileSource tileSource = (ITileSource) object;
			builder.setTitle(tileSource.getName());
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_map, activeColorRes));
		} else if (object instanceof File) {
			setupBottomSheetItemForFile(builder, (File) object);
		} else if (object instanceof GpxSettingsItem) {
			GpxSettingsItem item = (GpxSettingsItem) object;
			setupBottomSheetItemForGpx(builder, item.getFile(), item.getAppearanceInfo());
		} else if (object instanceof FileSettingsItem) {
			FileSettingsItem item = (FileSettingsItem) object;
			setupBottomSheetItemForFile(builder, item.getFile());
		} else if (object instanceof AvoidRoadInfo) {
			AvoidRoadInfo avoidRoadInfo = (AvoidRoadInfo) object;
			builder.setTitle(avoidRoadInfo.name);
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_alert, activeColorRes));
		} else if (object instanceof OsmNotesPoint) {
			OsmNotesPoint osmNotesPoint = (OsmNotesPoint) object;
			builder.setTitle(osmNotesPoint.getText());
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_osm_note_add, activeColorRes));
		} else if (object instanceof OpenstreetmapPoint) {
			OpenstreetmapPoint openstreetmapPoint = (OpenstreetmapPoint) object;
			builder.setTitle(OsmEditingPlugin.getTitle(openstreetmapPoint, app));
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_info_dark, activeColorRes));
		} else if (object instanceof FavoriteGroup) {
			FavoriteGroup group = (FavoriteGroup) object;
			builder.setTitle(group.getDisplayName(app));
			int color = group.getColor() == 0 ? ContextCompat.getColor(app, R.color.color_favorite) : group.getColor();
			builder.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_folder, color));
			int points = group.getPoints().size();
			String itemsDescr = getString(R.string.shared_string_gpx_points);
			builder.setDescription(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, points));
		} else if (object instanceof GlobalSettingsItem) {
			GlobalSettingsItem globalSettingsItem = (GlobalSettingsItem) object;
			builder.setTitle(globalSettingsItem.getPublicName(app));
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_settings, activeColorRes));
		} else if (object instanceof MapMarkersGroup) {
			MapMarkersGroup markersGroup = (MapMarkersGroup) object;
			if (ExportSettingsType.ACTIVE_MARKERS.name().equals(markersGroup.getId())) {
				builder.setTitle(getString(R.string.map_markers));
				builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_flag, activeColorRes));
			} else if (ExportSettingsType.HISTORY_MARKERS.name().equals(markersGroup.getId())) {
				builder.setTitle(getString(R.string.markers_history));
				builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_history, activeColorRes));
			}
			int selectedMarkers = markersGroup.getMarkers().size();
			String itemsDescr = getString(R.string.shared_string_items);
			builder.setDescription(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, selectedMarkers));
		} else if (object instanceof HistoryEntry) {
			HistoryEntry historyEntry = (HistoryEntry) object;
			builder.setTitle(historyEntry.getName().getName());
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_history, activeColorRes));
		}
	}

	private void setupBottomSheetItemForFile(Builder builder, File file) {
		FileSubtype fileSubtype = FileSubtype.getSubtypeByPath(app, file.getPath());
		builder.setTitle(file.getName());
		if (file.getAbsolutePath().contains(IndexConstants.RENDERERS_DIR)) {
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_map_style, activeColorRes));
		} else if (file.getAbsolutePath().contains(IndexConstants.ROUTING_PROFILES_DIR)) {
			builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_route_distance, activeColorRes));
		} else if (file.getAbsolutePath().contains(IndexConstants.GPX_INDEX_DIR)) {
			setupBottomSheetItemForGpx(builder, file, null);
		} else if (file.getAbsolutePath().contains(IndexConstants.AV_INDEX_DIR)) {
			int iconId = AudioVideoNotesPlugin.getIconIdForRecordingFile(file);
			if (iconId == -1) {
				iconId = R.drawable.ic_action_photo_dark;
			}
			builder.setIcon(uiUtilities.getIcon(iconId, activeColorRes));
			builder.setDescription(AndroidUtils.formatSize(app, file.length()));
		} else if (fileSubtype.isMap()
				|| fileSubtype == FileSettingsItem.FileSubtype.TTS_VOICE
				|| fileSubtype == FileSettingsItem.FileSubtype.VOICE) {
			builder.setTitle(FileNameTranslationHelper.getFileNameWithRegion(app, file.getName()));
			builder.setIcon(uiUtilities.getIcon(fileSubtype.getIconId(), activeColorRes));

			if (fileSubtype.isMap()) {
				String mapDescription = getMapDescription(file);
				String formattedSize = AndroidUtils.formatSize(app, file.length());
				if (mapDescription != null) {
					builder.setDescription(getString(R.string.ltr_or_rtl_combine_via_bold_point, mapDescription, formattedSize));
				} else {
					builder.setDescription(formattedSize);
				}
			}
		}
	}

	private void setupBottomSheetItemForGpx(Builder builder, File file, @Nullable GpxAppearanceInfo appearanceInfo) {
		builder.setTitle(GpxUiHelper.getGpxTitle(file.getName()));
		builder.setDescription(getTrackDescr(file, file.lastModified(), file.length(), appearanceInfo));
		builder.setIcon(uiUtilities.getIcon(R.drawable.ic_action_route_distance, activeColorRes));
	}

	private final GpxDataItemCallback gpxDataItemCallback = new GpxDataItemCallback() {
		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public void onGpxDataItemReady(GpxDataItem item) {
			for (BaseBottomSheetItem bottomSheetItem : items) {
				Object tag = bottomSheetItem.getTag();
				if (tag instanceof FileSettingsItem) {
					if (Algorithms.objectEquals(item.getFile(), ((FileSettingsItem) tag).getFile())) {
						((BottomSheetItemWithDescription) bottomSheetItem).setDescription(getTrackDescrForDataItem(item));
						break;
					}
				}
			}
		}
	};

	private String getTrackDescr(@NonNull File file, long lastModified, long size, GpxAppearanceInfo appearanceInfo) {
		String folder = "";
		File parent = file.getParentFile();
		if (parent != null) {
			folder = Algorithms.capitalizeFirstLetter(parent.getName());
		}
		if (exportMode) {
			GpxDataItem dataItem = getDataItem(file, gpxDataItemCallback);
			if (dataItem != null) {
				return getTrackDescrForDataItem(dataItem);
			}
		} else if (appearanceInfo != null) {
			String dist = OsmAndFormatter.getFormattedDistance(appearanceInfo.totalDistance, app);
			String points = appearanceInfo.wptPoints + " " + getString(R.string.shared_string_gpx_points).toLowerCase();
			String descr = getString(R.string.ltr_or_rtl_combine_via_bold_point, folder, dist);
			return getString(R.string.ltr_or_rtl_combine_via_comma, descr, points);
		} else {
			String date = OsmAndFormatter.getFormattedDate(app, lastModified);
			String formattedSize = AndroidUtils.formatSize(app, size);
			String descr = getString(R.string.ltr_or_rtl_combine_via_bold_point, folder, date);
			return getString(R.string.ltr_or_rtl_combine_via_comma, descr, formattedSize);
		}
		return null;
	}

	private String getTrackDescrForDataItem(@NonNull GpxDataItem dataItem) {
		GPXTrackAnalysis analysis = dataItem.getAnalysis();
		if (analysis != null) {
			File parent = dataItem.getFile().getParentFile();
			String folder = Algorithms.capitalizeFirstLetter(parent.getName());
			String dist = OsmAndFormatter.getFormattedDistance(analysis.totalDistance, app);
			String points = analysis.wptPoints + " " + getString(R.string.shared_string_gpx_points).toLowerCase();
			String descr = getString(R.string.ltr_or_rtl_combine_via_bold_point, folder, dist);
			return getString(R.string.ltr_or_rtl_combine_via_comma, descr, points);
		}
		return null;
	}

	private GpxDataItem getDataItem(File file, @Nullable GpxDataItemCallback callback) {
		return app.getGpxDbHelper().getItem(file, callback);
	}

	private String getMapDescription(File file) {
		if (file.isDirectory() || file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
			return getString(R.string.online_map);
		} else if (file.getName().endsWith(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT)) {
			return getString(R.string.download_roads_only_item);
		} else if (file.getName().endsWith(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT)) {
			return getString(R.string.download_wikipedia_maps);
		} else if (file.getName().endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)) {
			return getString(R.string.download_srtm_maps);
		} else if (file.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			return getString(R.string.download_regular_maps);
		}
		return null;
	}
}