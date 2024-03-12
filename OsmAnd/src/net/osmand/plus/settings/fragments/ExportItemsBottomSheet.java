package net.osmand.plus.settings.fragments;

import static net.osmand.view.ThreeStateCheckbox.State.CHECKED;
import static net.osmand.view.ThreeStateCheckbox.State.MISC;
import static net.osmand.view.ThreeStateCheckbox.State.UNCHECKED;

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

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton.Builder;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.profiles.data.RoutingProfilesResources;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationModeBean;
import net.osmand.plus.settings.backend.backup.GpxAppearanceInfo;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GlobalSettingsItem;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter.OnItemSelectedListener;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.plus.track.helpers.GpxDbHelper.GpxDataItemCallback;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;
import net.osmand.view.ThreeStateCheckbox;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExportItemsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ExportItemsBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ExportItemsBottomSheet.class);

	private static final String EXPORT_TYPE_KEY = "export_type_key";
	private static final String EXPORT_MODE_KEY = "export_mode_key";

	private OsmandApplication app;
	private UiUtilities uiUtilities;

	private ExportType type;
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
			type = ExportType.valueOf(savedInstanceState.getString(EXPORT_TYPE_KEY));
		}
		Fragment target = getTargetFragment();
		if (target instanceof BaseSettingsListFragment) {
			BaseSettingsListFragment fragment = (BaseSettingsListFragment) target;
			List<?> items = fragment.getItemsForType(type);
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
			BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			Builder builder = (BottomSheetItemWithCompoundButton.Builder) new Builder()
					.setChecked(selectedItems.contains(object))
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(app, secondaryColorRes, activeColorRes))
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setOnClickListener(v -> {
						boolean checked = !item[0].isChecked();
						item[0].setChecked(checked);
						if (checked) {
							selectedItems.add(item[0].getTag());
						} else {
							selectedItems.remove(item[0].getTag());
						}
						updateTitleView();
						setupBottomSheetItem(item[0], item[0].getTag());
					})
					.setTag(object);
			item[0] = builder.create();
			items.add(item[0]);
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateItems();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(EXPORT_MODE_KEY, exportMode);
		outState.putString(EXPORT_TYPE_KEY, type.name());
	}

	private BaseBottomSheetItem createTitleItem() {
		LayoutInflater themedInflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = themedInflater.inflate(R.layout.settings_group_title, null);

		checkBox = view.findViewById(R.id.check_box);
		selectedSize = view.findViewById(R.id.selected_size);
		TextView textView = view.findViewById(R.id.title);
		textView.setText(type.getTitleId());
		view.findViewById(R.id.select_all_button).setOnClickListener(v -> {
			checkBox.performClick();
			boolean checked = checkBox.getState() == CHECKED;
			if (checked) {
				selectedItems.addAll(allItems);
			} else {
				selectedItems.clear();
			}
			updateTitleView();
			updateItems();
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
		String allItemsSize = String.valueOf(allItems.size());
		String selectedItemsSize = String.valueOf(selectedItems.size());
		if (type != null && type.isMap() && !selectedItems.isEmpty()) {
			String size = AndroidUtils.formatSize(app, calculateSelectedItemsSize());
			String selected = getString(R.string.ltr_or_rtl_combine_via_slash, selectedItemsSize, allItemsSize);
			description = getString(R.string.ltr_or_rtl_combine_via_comma, selected, size);
		} else {
			description = getString(R.string.ltr_or_rtl_combine_via_slash, selectedItemsSize, allItemsSize);
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

	private void updateItems() {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton && item.getTag() != null) {
				BottomSheetItemWithCompoundButton bottomSheetItem = (BottomSheetItemWithCompoundButton) item;
				setupBottomSheetItem(bottomSheetItem, item.getTag());
				bottomSheetItem.setChecked(selectedItems.contains(item.getTag()));
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

	public static void showInstance(@NonNull FragmentManager fm, @NonNull ExportType exportType,
	                                @NonNull BaseSettingsListFragment target, boolean exportMode) {
		try {
			if (!fm.isStateSaved() && fm.findFragmentByTag(TAG) == null) {
				ExportItemsBottomSheet fragment = new ExportItemsBottomSheet();
				fragment.type = exportType;
				fragment.exportMode = exportMode;
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	private void setupDescription(@NonNull View view) {
		TextView description = view.findViewById(R.id.title_description);
		if (type == ExportType.FAVORITES) {
			description.setText(R.string.select_groups_for_import);
		} else {
			description.setText(R.string.select_items_for_import);
		}
	}

	private void setupBottomSheetItem(BottomSheetItemWithCompoundButton item, Object object) {
		if (object instanceof ApplicationModeBean) {
			ApplicationModeBean modeBean = (ApplicationModeBean) object;
			String profileName = modeBean.userProfileName;
			if (Algorithms.isEmpty(profileName)) {
				ApplicationMode appMode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
				if (appMode != null) {
					profileName = appMode.toHumanString();
				} else {
					profileName = Algorithms.capitalizeFirstLetter(modeBean.stringKey);
				}
			}
			item.setTitle(profileName);

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
				item.setDescription(getString(R.string.ltr_or_rtl_combine_via_colon, getString(R.string.nav_type_hint), routingProfile));
			} else {
				item.setDescription(getString(R.string.profile_type_osmand_string));
			}
			int profileIconRes = AndroidUtils.getDrawableId(app, modeBean.iconName);
			ProfileIconColors iconColor = modeBean.iconColor;
			Integer customIconColor = modeBean.customIconColor;
			int actualIconColor;
			if (selectedItems.contains(object)) {
				actualIconColor = customIconColor != null ? customIconColor : ContextCompat.getColor(app, iconColor.getColor(nightMode));
			} else {
				actualIconColor = ContextCompat.getColor(app, secondaryColorRes);
			}
			int iconRes = profileIconRes != 0 ? profileIconRes : R.drawable.ic_world_globe_dark;
			item.setIcon(uiUtilities.getPaintedIcon(iconRes, actualIconColor));
		} else if (object instanceof QuickActionButtonState) {
			QuickActionButtonState buttonState = (QuickActionButtonState) object;
			item.setTitle(buttonState.getName());
			item.setIcon(buttonState.getIcon(nightMode, false, ColorUtilities.getColor(app, getItemIconColor(object))));
		} else if (object instanceof PoiUIFilter) {
			PoiUIFilter poiUIFilter = (PoiUIFilter) object;
			item.setTitle(poiUIFilter.getName());
			int iconRes = RenderingIcons.getBigIconResourceId(poiUIFilter.getIconId());
			item.setIcon(uiUtilities.getIcon(iconRes != 0 ? iconRes : R.drawable.ic_action_user, activeColorRes));
		} else if (object instanceof TileSourceTemplate || object instanceof SQLiteTileSource) {
			ITileSource tileSource = (ITileSource) object;
			item.setTitle(tileSource.getName());
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_map, getItemIconColor(object)));
		} else if (object instanceof File) {
			setupBottomSheetItemForFile(item, (File) object);
		} else if (object instanceof GpxSettingsItem) {
			GpxSettingsItem settingsItem = (GpxSettingsItem) object;
			setupBottomSheetItemForGpx(item, settingsItem.getFile(), settingsItem.getAppearanceInfo());
		} else if (object instanceof FileSettingsItem) {
			FileSettingsItem settingsItem = (FileSettingsItem) object;
			setupBottomSheetItemForFile(item, settingsItem.getFile());
		} else if (object instanceof AvoidRoadInfo) {
			AvoidRoadInfo avoidRoadInfo = (AvoidRoadInfo) object;
			item.setTitle(avoidRoadInfo.getName(app));
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_alert, getItemIconColor(object)));
		} else if (object instanceof OsmNotesPoint) {
			OsmNotesPoint osmNotesPoint = (OsmNotesPoint) object;
			item.setTitle(osmNotesPoint.getText());
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_osm_note_add, getItemIconColor(object)));
		} else if (object instanceof OpenstreetmapPoint) {
			OpenstreetmapPoint openstreetmapPoint = (OpenstreetmapPoint) object;
			item.setTitle(OsmEditingPlugin.getTitle(openstreetmapPoint, app));
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_info_dark, getItemIconColor(object)));
		} else if (object instanceof FavoriteGroup) {
			FavoriteGroup group = (FavoriteGroup) object;
			item.setTitle(group.getDisplayName(app));
			int color;
			if (selectedItems.contains(object)) {
				color = group.getColor() == 0 ? ContextCompat.getColor(app, R.color.color_favorite) : group.getColor();
			} else {
				color = ContextCompat.getColor(app, secondaryColorRes);
			}
			item.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_folder, color));
			int points = group.getPoints().size();
			String itemsDescr = getString(R.string.shared_string_gpx_points);
			item.setDescription(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, String.valueOf(points)));
		} else if (object instanceof GlobalSettingsItem) {
			GlobalSettingsItem globalSettingsItem = (GlobalSettingsItem) object;
			item.setTitle(globalSettingsItem.getPublicName(app));
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_settings, getItemIconColor(object)));
		} else if (object instanceof MapMarkersGroup) {
			MapMarkersGroup markersGroup = (MapMarkersGroup) object;
			if (ExportType.ACTIVE_MARKERS.name().equals(markersGroup.getId())) {
				item.setTitle(getString(R.string.map_markers));
				item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_flag, getItemIconColor(object)));
			} else if (ExportType.HISTORY_MARKERS.name().equals(markersGroup.getId())) {
				item.setTitle(getString(R.string.markers_history));
				item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_history, getItemIconColor(object)));
			} else {
				String groupName = markersGroup.getName();
				if (Algorithms.isEmpty(groupName)) {
					if (markersGroup.getType() == ItineraryType.FAVOURITES) {
						groupName = app.getString(R.string.shared_string_favorites);
					} else if (markersGroup.getType() == ItineraryType.MARKERS) {
						groupName = app.getString(R.string.map_markers);
					}
				}
				item.setTitle(groupName);
				item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_flag, getItemIconColor(object)));
			}
			int selectedMarkers = markersGroup.getMarkers().size();
			String itemsDescr = getString(R.string.shared_string_items);
			item.setDescription(getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, String.valueOf(selectedMarkers)));
		} else if (object instanceof HistoryEntry) {
			HistoryEntry historyEntry = (HistoryEntry) object;
			item.setTitle(historyEntry.getName().getName());
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_history, getItemIconColor(object)));
		} else if (object instanceof OnlineRoutingEngine) {
			OnlineRoutingEngine onlineRoutingEngine = (OnlineRoutingEngine) object;
			item.setTitle(onlineRoutingEngine.getName(app));
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_world_globe_dark, getItemIconColor(object)));
		}
	}

	private void setupBottomSheetItemForFile(BottomSheetItemWithCompoundButton item, File file) {
		FileSubtype fileSubtype = FileSubtype.getSubtypeByPath(app, file.getPath());
		item.setTitle(file.getName());
		if (file.getAbsolutePath().contains(IndexConstants.RENDERERS_DIR)) {
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_map_style, getItemIconColor(item.getTag())));
		} else if (file.getAbsolutePath().contains(IndexConstants.ROUTING_PROFILES_DIR)) {
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_route_distance, getItemIconColor(item.getTag())));
		} else if (file.getAbsolutePath().contains(IndexConstants.GPX_INDEX_DIR)) {
			setupBottomSheetItemForGpx(item, file, null);
		} else if (file.getAbsolutePath().contains(IndexConstants.AV_INDEX_DIR)) {
			int iconId = AudioVideoNotesPlugin.getIconIdForRecordingFile(file);
			if (iconId == -1) {
				iconId = R.drawable.ic_action_photo_dark;
			}
			if (item.getTag() instanceof FileSettingsItem) {
				FileSettingsItem settingsItem = (FileSettingsItem) item.getTag();
				item.setTitle(Recording.getNameForMultimediaFile(app, file.getName(), settingsItem.getLastModifiedTime()));
			} else {
				item.setTitle(new Recording(file).getName(app, true));
			}
			item.setIcon(uiUtilities.getIcon(iconId, getItemIconColor(item.getTag())));
			item.setDescription(AndroidUtils.formatSize(app, file.length()));
		} else if (fileSubtype == FileSubtype.FAVORITES_BACKUP) {
			item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_folder_favorites, getItemIconColor(item.getTag())));
		} else if (fileSubtype.isMap()
				|| fileSubtype == FileSettingsItem.FileSubtype.TTS_VOICE
				|| fileSubtype == FileSettingsItem.FileSubtype.VOICE) {
			item.setTitle(FileNameTranslationHelper.getFileNameWithRegion(app, file.getName()));
			item.setIcon(uiUtilities.getIcon(fileSubtype.getIconId(), getItemIconColor(item.getTag())));

			if (fileSubtype.isMap()) {
				String mapDescription = getMapDescription(file);
				String formattedSize = AndroidUtils.formatSize(app, file.length());
				if (mapDescription != null) {
					item.setDescription(getString(R.string.ltr_or_rtl_combine_via_bold_point, mapDescription, formattedSize));
				} else {
					item.setDescription(formattedSize);
				}
			}
		}
	}

	private void setupBottomSheetItemForGpx(BottomSheetItemWithCompoundButton item, File file, @Nullable GpxAppearanceInfo appearanceInfo) {
		item.setTitle(GpxUiHelper.getGpxTitle(file.getName()));
		item.setDescription(getTrackDescr(file, file.lastModified(), file.length(), appearanceInfo));
		item.setIcon(uiUtilities.getIcon(R.drawable.ic_action_route_distance, getItemIconColor(item.getTag())));
	}

	private int getItemIconColor(Object object) {
		return selectedItems.contains(object) ? activeColorRes : secondaryColorRes;
	}

	private final GpxDataItemCallback gpxDataItemCallback = new GpxDataItemCallback() {
		@Override
		public boolean isCancelled() {
			return !isAdded();
		}

		@Override
		public void onGpxDataItemReady(@NonNull GpxDataItem item) {
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
			String dist = OsmAndFormatter.getFormattedDistance(analysis.getTotalDistance(), app);
			String points = analysis.getWptPoints() + " " + getString(R.string.shared_string_gpx_points).toLowerCase();
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
		} else if (file.getName().endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
			return getString(R.string.download_maps_travel);
		} else if (file.getName().endsWith(IndexConstants.TIF_EXT)) {
			return getString(R.string.shared_string_terrain);
		} else if (SrtmDownloadItem.isSrtmFile(file.getName())) {
			return getString(R.string.download_srtm_maps);
		} else if (file.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
			return getString(R.string.download_regular_maps);
		}
		return null;
	}
}