package net.osmand.plus.settings.backend.backup.exporttype;

import static net.osmand.plus.settings.backend.backup.exporttype.AbstractMapExportType.OFFLINE_MAPS_EXPORT_TYPE_KEY;
import static net.osmand.util.CollectionUtils.addAllIfNotContains;
import static net.osmand.util.CollectionUtils.addIfNotContains;
import static net.osmand.util.CollectionUtils.filterElementsWithCondition;
import static net.osmand.util.CollectionUtils.searchElementWithCondition;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public enum ExportType {

	PROFILE(new ProfileExportType()),
	GLOBAL(new GlobalExportType()),
	QUICK_ACTIONS(new QuickActionsExportType()),
	POI_TYPES(new PoiTypesExportType()),
	AVOID_ROADS(new AvoidRoadsExportType()),
	FAVORITES(new FavoritesExportType()),
	TRACKS(new TracksExportType()),
	OSM_NOTES(new OsmNotesExportType()),
	OSM_EDITS(new OsmEditsExportType()),
	MULTIMEDIA_NOTES(new MultimediaNotesExportType()),
	ACTIVE_MARKERS(new ActiveMarkersExportType()),
	HISTORY_MARKERS(new HistoryMarkersExportType()),
	SEARCH_HISTORY(new SearchHistoryExportType()),
	NAVIGATION_HISTORY(new NavigationHistoryExportType()),
	ITINERARY_GROUPS(new ItineraryGroupsExportType()),
	CUSTOM_RENDER_STYLE(new CustomRenderStyleExportType()),
	CUSTOM_ROUTING(new CustomRoutingExportType()),
	ONLINE_ROUTING_ENGINES(new OnlineRoutingEnginesExportType()),
	MAP_SOURCES(new MapSourcesExportType()),
	STANDARD_MAPS(new StandardMapsExportType()),
	WIKI_AND_TRAVEL(new WikiAndTravelExportType()),
	DEPTH_DATA(new DepthDataExportType()),
	ROAD_MAPS(new RoadMapsExportType()),
	TERRAIN_DATA(new TerrainDataExportType()),
	TTS_VOICE(new TtsVoiceExportType()),
	VOICE(new VoiceExportType()),
	FAVORITES_BACKUP(new FavoritesBackupExportType());

	@NonNull
	private final IExportType instance;

	ExportType(@NonNull IExportType instance) {
		this.instance = instance;
	}

	@NonNull
	public String getTitle(@NonNull Context context) {
		return context.getString(getTitleId());
	}

	@StringRes
	public int getTitleId() {
		return instance.getTitleId();
	}

	@DrawableRes
	public int getIconId() {
		return instance.getIconId();
	}

	public String getItemName() {
		return instance.getRelatedSettingsItemType().name();
	}

	@NonNull
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		return instance.fetchExportData(app, offlineBackup);
	}

	@NonNull
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		return instance.fetchImportData(settingsItem, importCompleted);
	}

	public boolean isMap() {
		return instance.isMap();
	}

	public boolean isRelatedToCategory(@NonNull ExportCategory exportCategory) {
		return instance.isRelatedToCategory(exportCategory);
	}

	public boolean isAvailableInFreeVersion() {
		return instance.isAvailableInFreeVersion();
	}

	public boolean isEnabled() {
		Class<? extends OsmandPlugin> clazz = instance.getRelatedPluginClass();
		return clazz == null || PluginsHelper.isActive(clazz);
	}

	@Nullable
	public static ExportType findBy(@NonNull OsmandApplication app, @NonNull Object object) {
		return searchElementWithCondition(valuesList(),
				exportType -> exportType.instance.isRelatedObject(app, object));
	}

	@Nullable
	public static ExportType findBy(@NonNull RemoteFile remoteFile) {
		if (remoteFile.item != null) {
			return findBy(remoteFile.item);
		}
		if (Objects.equals(SettingsItemType.FILE.name(), remoteFile.getType())) {
			return findBy(FileSubtype.getSubtypeByFileName(remoteFile.getName()));
		}
		return searchElementWithCondition(valuesList(), exportType -> {
			SettingsItemType relatedSettingsType = exportType.instance.getRelatedSettingsItemType();
			return Objects.equals(relatedSettingsType.name(), remoteFile.getType());
		});
	}

	@Nullable
	public static ExportType findBy(@NonNull SettingsItem item) {
		if (item.getType() == SettingsItemType.FILE) {
			return findBy(((FileSettingsItem) item).getSubtype());
		}
		return searchElementWithCondition(valuesList(),
				exportType -> exportType.instance.getRelatedSettingsItemType() == item.getType());
	}

	@Nullable
	public static ExportType findBy(@NonNull FileSubtype fileSubtype) {
		return searchElementWithCondition(valuesList(),
				type -> type.instance.getRelatedFileSubtypes().contains(fileSubtype));
	}

	@Nullable
	public static ExportType findBy(@NonNull LocalItemType localItemType) {
		return searchElementWithCondition(valuesList(),
				exportType -> exportType.instance.getRelatedLocalItemType() == localItemType);
	}

	@NonNull
	public static List<ExportType> mapValues() {
		return filterElementsWithCondition(valuesList(), ExportType::isMap);
	}

	@NonNull
	public static List<ExportType> enabledValuesOf(@NonNull ExportCategory exportCategory) {
		return filterElementsWithCondition(enabledValues(),
				exportType -> exportType.isRelatedToCategory(exportCategory));
	}

	@NonNull
	public static List<ExportType> enabledValues() {
		return filterElementsWithCondition(valuesList(), ExportType::isEnabled);
	}

	@NonNull
	public static List<ExportType> valuesOf(@NonNull List<String> keys) {
		List<ExportType> result = new ArrayList<>();
		for (String key : keys) {
			if (Objects.equals(OFFLINE_MAPS_EXPORT_TYPE_KEY, key)) {
				addAllIfNotContains(result, mapValues());
			} else {
				addIfNotContains(result, valueOf(key));
			}
		}
		return result;
	}

	@NonNull
	public static List<ExportType> valuesList() {
		return Arrays.asList(values());
	}
}
