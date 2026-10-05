package net.osmand.aidl;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Groups of the AIDL API methods a connected app can be allowed to use. Methods are named as in
 * OsmandAidlService(V2).getApi(reason); methods not listed in any group are always allowed (service calls).
 */
public enum AidlPermissionGroup {

	MAP("map", R.drawable.ic_world_globe_dark, R.string.shared_string_map, R.string.aidl_group_map_descr, true,
			"refreshMap", "setMapLocation", "setMapCamera", "showMapPoint", "addMapPoint", "updateMapPoint", "removeMapPoint",
			"addMapLayer", "updateMapLayer", "removeMapLayer", "addMapWidget", "updateMapWidget", "removeMapWidget",
			"addWidgetGroup", "removeWidgetGroup", "regWidgetVisibility", "setMapMargins", "setZoomLimits",
			"addContextMenuButtons", "updateContextMenuButtons", "removeContextMenuButtons",
			"isFragmentOpen", "isMenuOpen"),
	SEARCH("search", R.drawable.ic_action_search_dark, R.string.shared_string_search, R.string.aidl_group_search_descr, true,
			"search"),
	LOCATION("location", R.drawable.ic_action_my_location, R.string.shared_string_my_location, R.string.aidl_group_location_descr, false,
			"getAppInfo", "getMapWidgetValues"),
	NAVIGATION("navigation", R.drawable.ic_action_gdirections_dark, R.string.shared_string_navigation, R.string.aidl_group_navigation_descr, false,
			"navigate", "navigateGpx", "navigateSearch", "pauseNavigation", "resumeNavigation", "stopNavigation",
			"muteNavigation", "unmuteNavigation", "addRoadBlock", "removeRoadBlock", "getBlockedRoads", "registerForNavUpdates",
			"registerForVoiceRouterMessages"),
	FAVORITES("favorites", R.drawable.ic_action_favorite, R.string.shared_string_favorites, R.string.aidl_group_favorites_descr, false,
			"addFavorite", "updateFavorite", "removeFavorite", "addFavoriteGroup", "updateFavoriteGroup",
			"removeFavoriteGroup", "addMapMarker", "updateMapMarker", "removeMapMarker", "removeAllActiveMapMarkers"),
	TRACKS_VIEW("tracks_view", R.drawable.ic_action_polygom_dark, R.string.aidl_group_tracks_view, R.string.aidl_group_tracks_view_descr, false,
			"getActiveGpx", "getImportedGpx", "searchGpx", "getGpxPoints", "getBitmapForGpx", "getGpxColor"),
	TRACKS_EDIT("tracks_edit", R.drawable.ic_action_edit_dark, R.string.aidl_group_tracks_edit, R.string.aidl_group_tracks_edit_descr, false,
			"importGpx", "showGpx", "hideGpx", "removeGpx"),
	RECORDING("recording", R.drawable.ic_action_rec_start, R.string.aidl_group_recording, R.string.aidl_group_recording_descr, false,
			"startGpxRecording", "stopGpxRecording", "getGpxRecordingInfo"),
	NOTES("notes", R.drawable.ic_action_photo_dark, R.string.aidl_group_notes, R.string.aidl_group_notes_descr, false,
			"takePhotoNote", "startAudioRecording", "startVideoRecording", "stopRecording"),
	SCREEN("screen", R.drawable.ic_action_device_camera, R.string.aidl_group_screen, R.string.aidl_group_screen_descr, false,
			"getMapScreenshot"),
	SETTINGS("settings", R.drawable.ic_action_settings, R.string.shared_string_settings, R.string.aidl_group_settings_descr, false,
			"getPreference", "setPreference", "selectProfile", "getProfiles", "importProfile", "exportProfile",
			"changePluginState", "executeQuickAction", "getQuickActionsInfo", "setLockState",
			"getSqliteDbFiles", "getActiveSqliteDbFiles", "showSqliteDbFile", "hideSqliteDbFile"),
	SYSTEM("system", R.drawable.ic_action_alert, R.string.aidl_group_system, R.string.aidl_group_system_descr, false,
			"copyFile", "reloadIndexes", "restoreOsmand", "exitApp", "setLocation", "registerForLogcatMessages",
			"registerForKeyEvents", "setCustomization", "customizeOsmandSettings", "areOsmandSettingsCustomized",
			"setFeaturesEnabledIds", "setFeaturesDisabledIds", "setFeaturesEnabledPatterns",
			"setFeaturesDisabledPatterns", "setNavDrawerItems", "setNavDrawerFooterParams", "setNavDrawerLogo",
			"setNavDrawerLogoWithParams");

	private final String id;
	@DrawableRes
	private final int iconId;
	@StringRes
	private final int titleId;
	@StringRes
	private final int descriptionId;
	private final boolean grantedByDefault;
	private final Set<String> methods;

	AidlPermissionGroup(@NonNull String id, @DrawableRes int iconId, @StringRes int titleId, @StringRes int descriptionId,
	                    boolean grantedByDefault, @NonNull String... methods) {
		this.id = id;
		this.iconId = iconId;
		this.titleId = titleId;
		this.descriptionId = descriptionId;
		this.grantedByDefault = grantedByDefault;
		this.methods = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(methods)));
	}

	@NonNull
	public String getId() {
		return id;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getDescriptionId() {
		return descriptionId;
	}

	/**
	 * Safe groups are on by default: for a new app and preselected in a permission request
	 */
	public boolean isSafe() {
		return grantedByDefault;
	}

	public boolean isSensitive() {
		return this == SYSTEM;
	}

	@Nullable
	public static AidlPermissionGroup getById(@Nullable String id) {
		for (AidlPermissionGroup group : values()) {
			if (group.id.equals(id)) {
				return group;
			}
		}
		return null;
	}

	/**
	 * @return the group of an AIDL method, null for methods any connected app may call
	 */
	@Nullable
	public static AidlPermissionGroup getByMethod(@NonNull String method) {
		for (AidlPermissionGroup group : values()) {
			if (group.methods.contains(method)) {
				return group;
			}
		}
		return null;
	}

	@NonNull
	public static Set<AidlPermissionGroup> getDefaultGroups() {
		Set<AidlPermissionGroup> groups = new LinkedHashSet<>();
		for (AidlPermissionGroup group : values()) {
			if (group.grantedByDefault) {
				groups.add(group);
			}
		}
		return groups;
	}

	@NonNull
	public static Set<AidlPermissionGroup> getAllGroups() {
		return new LinkedHashSet<>(Arrays.asList(values()));
	}
}
