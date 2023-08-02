package net.osmand.aidlapi;

import net.osmand.aidlapi.map.ALatLon;
import net.osmand.aidlapi.map.SetMapLocationParams;
import net.osmand.aidlapi.map.SetLocationParams;

import net.osmand.aidlapi.favorite.group.AFavoriteGroup;
import net.osmand.aidlapi.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidlapi.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidlapi.favorite.group.UpdateFavoriteGroupParams;

import net.osmand.aidlapi.favorite.AFavorite;
import net.osmand.aidlapi.favorite.AddFavoriteParams;
import net.osmand.aidlapi.favorite.RemoveFavoriteParams;
import net.osmand.aidlapi.favorite.UpdateFavoriteParams;

import net.osmand.aidlapi.mapmarker.AMapMarker;
import net.osmand.aidlapi.mapmarker.AddMapMarkerParams;
import net.osmand.aidlapi.mapmarker.RemoveMapMarkerParams;
import net.osmand.aidlapi.mapmarker.UpdateMapMarkerParams;

import net.osmand.aidlapi.calculateroute.CalculateRouteParams;

import net.osmand.aidlapi.profile.ExportProfileParams;

import net.osmand.aidlapi.gpx.ImportGpxParams;
import net.osmand.aidlapi.gpx.ShowGpxParams;
import net.osmand.aidlapi.gpx.StartGpxRecordingParams;
import net.osmand.aidlapi.gpx.StopGpxRecordingParams;
import net.osmand.aidlapi.gpx.HideGpxParams;
import net.osmand.aidlapi.gpx.ASelectedGpxFile;

import net.osmand.aidlapi.mapwidget.AMapWidget;
import net.osmand.aidlapi.mapwidget.AddMapWidgetParams;
import net.osmand.aidlapi.mapwidget.RemoveMapWidgetParams;
import net.osmand.aidlapi.mapwidget.UpdateMapWidgetParams;

import net.osmand.aidlapi.maplayer.point.AMapPoint;
import net.osmand.aidlapi.maplayer.point.AddMapPointParams;
import net.osmand.aidlapi.maplayer.point.RemoveMapPointParams;
import net.osmand.aidlapi.maplayer.point.UpdateMapPointParams;
import net.osmand.aidlapi.maplayer.AMapLayer;
import net.osmand.aidlapi.maplayer.AddMapLayerParams;
import net.osmand.aidlapi.maplayer.RemoveMapLayerParams;
import net.osmand.aidlapi.maplayer.UpdateMapLayerParams;

import net.osmand.aidlapi.navigation.NavigateParams;
import net.osmand.aidlapi.navigation.NavigateGpxParams;

import net.osmand.aidlapi.note.TakePhotoNoteParams;
import net.osmand.aidlapi.note.StartVideoRecordingParams;
import net.osmand.aidlapi.note.StartAudioRecordingParams;
import net.osmand.aidlapi.note.StopRecordingParams;

import net.osmand.aidlapi.gpx.RemoveGpxParams;

import net.osmand.aidlapi.maplayer.point.ShowMapPointParams;

import net.osmand.aidlapi.navdrawer.SetNavDrawerItemsParams;
import net.osmand.aidlapi.navdrawer.NavDrawerFooterParams;
import net.osmand.aidlapi.navdrawer.NavDrawerHeaderParams;

import net.osmand.aidlapi.navigation.PauseNavigationParams;
import net.osmand.aidlapi.navigation.ResumeNavigationParams;
import net.osmand.aidlapi.navigation.StopNavigationParams;
import net.osmand.aidlapi.navigation.MuteNavigationParams;
import net.osmand.aidlapi.navigation.UnmuteNavigationParams;

import net.osmand.aidlapi.IOsmAndAidlCallback;

import net.osmand.aidlapi.search.SearchResult;
import net.osmand.aidlapi.search.SearchParams;
import net.osmand.aidlapi.navigation.NavigateSearchParams;

import net.osmand.aidlapi.customization.SetWidgetsParams;
import net.osmand.aidlapi.customization.OsmandSettingsParams;
import net.osmand.aidlapi.customization.OsmandSettingsInfoParams;
import net.osmand.aidlapi.customization.CustomizationInfoParams;
import net.osmand.aidlapi.customization.ProfileSettingsParams;
import net.osmand.aidlapi.customization.MapMarginsParams;
import net.osmand.aidlapi.customization.CustomPluginParams;
import net.osmand.aidlapi.customization.SelectProfileParams;
import net.osmand.aidlapi.customization.AProfile;
import net.osmand.aidlapi.customization.PreferenceParams;
import net.osmand.aidlapi.customization.ZoomLimitsParams;

import net.osmand.aidlapi.gpx.AGpxFile;
import net.osmand.aidlapi.gpx.AGpxFileDetails;
import net.osmand.aidlapi.gpx.CreateGpxBitmapParams;

import net.osmand.aidlapi.tiles.ASqliteDbFile;

import net.osmand.aidlapi.plugins.PluginParams;
import net.osmand.aidlapi.copyfile.CopyFileParams;

import net.osmand.aidlapi.navigation.ANavigationUpdateParams;
import net.osmand.aidlapi.navigation.ANavigationVoiceRouterMessageParams;
import net.osmand.aidlapi.navigation.ABlockedRoad;
import net.osmand.aidlapi.navigation.AddBlockedRoadParams;
import net.osmand.aidlapi.navigation.RemoveBlockedRoadParams;

import net.osmand.aidlapi.contextmenu.ContextMenuButtonsParams;
import net.osmand.aidlapi.contextmenu.UpdateContextMenuButtonsParams;
import net.osmand.aidlapi.contextmenu.RemoveContextMenuButtonsParams;

import net.osmand.aidlapi.mapmarker.RemoveMapMarkersParams;

import net.osmand.aidlapi.quickaction.QuickActionParams;
import net.osmand.aidlapi.quickaction.QuickActionInfoParams;

import net.osmand.aidlapi.lock.SetLockStateParams;

import net.osmand.aidlapi.events.AKeyEventsParams;

import net.osmand.aidlapi.info.AppInfoParams;
import net.osmand.aidlapi.info.GetTextParams;

import net.osmand.aidlapi.profile.ExportProfileParams;

import net.osmand.aidlapi.exit.ExitAppParams;

import net.osmand.aidlapi.logcat.ALogcatListenerParams;

// NOTE: Add new methods at the end of file!!!

interface IOsmAndAidlInterface {

    /**
     * Add map marker at given location.
     *
     * @param lat (double) -  latitude.
     * @param lon (double) - longitude.
     * @param name (String)- name of marker.
     */
    boolean addMapMarker(in AddMapMarkerParams params);

    /**
     * Remove map marker.
     *
     * If ignoreCoordinates is false the marker is only removed if lat/lon match the currently set values of the marker.
     * If ignoreCoordinates is true the marker is removed if the name matches, the values of lat/lon are ignored.
     *
     * @param lat (double) -  latitude.
     * @param lon (double) - longitude.
     * @param name (String)- name of marker.
     * @param ignoreCoordinates (boolean) - flag to determine whether lat/lon shall be ignored
     */
    boolean removeMapMarker(in RemoveMapMarkerParams params);

    /**
     * Update map marker.
     *
     * If ignoreCoordinates is false the marker gets updated only if latPrev/lonPrev match the currently set values of the marker.
     * If ignoreCoordinates is true the marker gets updated if the name matches, the values of latPrev/lonPrev are ignored.
     *
     * @param latPrev (double) - latitude (current marker).
     * @param lonPrev (double) - longitude (current marker).
     * @param namePrev (String) - name (current marker).
     * @param latNew (double) - latitude (new marker).
     * @param lonNew (double) - longitude (new marker).
     * @param nameNew (String) - name (new marker).
     * @param ignoreCoordinates (boolean) - flag to determine whether latPrev/lonPrev shall be ignored
     */
    boolean updateMapMarker(in UpdateMapMarkerParams params);

    /**
     * Add map widget to the right side of the main screen.
     * Note: any specified icon should exist in OsmAnd app resources.
     *
     * @param id (String) - widget id.
     * @param menuIconName (String) - icon name (configure map menu).
     * @param menuTitle (String) - widget name (configure map menu).
     * @param lightIconName (String) - icon name for the light theme (widget).
     * @param darkIconName (String) - icon name for the dark theme (widget).
     * @param text (String) - main widget text.
     * @param description (String) - sub text, like "km/h".
     * @param order (int) - order position in the widgets list.
     * @param intentOnClick (String) - onClick intent. Called after click on widget as startActivity(Intent intent).
     */
    boolean addMapWidget(in AddMapWidgetParams params);

    /**
     * Remove map widget.
     *
     * @param (String) id - widget id.
     */
    boolean removeMapWidget(in RemoveMapWidgetParams params);

    /**
     * Update map widget.
     * Note: any specified icon should exist in OsmAnd app resources.
     *
     * @param id (String) - widget id.
     * @param menuIconName (String) - icon name (configure map menu).
     * @param menuTitle (String) - widget name (configure map menu).
     * @param lightIconName (String) - icon name for the light theme (widget).
     * @param darkIconName (String) - icon name for the dark theme (widget).
     * @param text (String) - main widget text.
     * @param description (String) - sub text, like "km/h".
     * @param order (int) - order position in the widgets list.
     * @param intentOnClick (String) - onClick intent. Called after click on widget as startActivity(Intent intent).
     */
    boolean updateMapWidget(in UpdateMapWidgetParams params);

   /**
    * Add point to user layer.
    *
    * @param layerId (String) - layer id. Note: layer should be added first.
    * @param pointId (String) - point id.
    * @param shortName (String) - short name (single char). Displayed on the map.
    * @param fullName (String) - full name. Displayed in the context menu on first row.
    * @param typeName (String) - type name. Displayed in context menu on second row.
    * @param color (int) - color of circle's background.
    * @param location (ALatLon) - location of the point.
    * @param details (List<String>)- list of details. Displayed under context menu.
    * @param params (Map<String, String>) - optional map of params for point.
    */
    boolean addMapPoint(in AddMapPointParams params);


    /**
     * Remove point.
     *
     * @param layerId (String) - layer id.
     * @param pointId (String) - point id.
     */
    boolean removeMapPoint(in RemoveMapPointParams params);

    /**
     * Update point.
     *
     * @param layerId (String) - layer id.
     * @param pointId (String) - point id.
     * @param updateOpenedMenuAndMap (boolean) - flag to enable folowing mode and menu updates for point
     * @param shortName (String) - short name (single char). Displayed on the map.
     * @param fullName (String) - full name. Displayed in the context menu on first row.
     * @param typeName (String) - type name. Displayed in context menu on second row.
     * @param color (String) - color of circle's background.
     * @param location (ALatLon)- location of the point.
     * @param details (List<String>) - list of details. Displayed under context menu.
     * @param params (Map<String, String>) - optional map of params for point.
     */
    boolean updateMapPoint(in UpdateMapPointParams params);

    /**
     * Add user layer on the map.
     *
     * @param id (String) - layer id.
     * @param name (String) - layer name.
     * @param zOrder (float) - z-order position of layer. Default value is 5.5f
     * @param points Map<Sting, AMapPoint> - initial list of points. Nullable.
     * @param imagePoints (boolean) - use new style for points on map or not. Also default zoom bounds for new style can be edited.
     */
    boolean addMapLayer(in AddMapLayerParams params);

    /**
     * Remove user layer.
     *
     * @param id (String) - layer id.
     */
    boolean removeMapLayer(in RemoveMapLayerParams params);

    /**
     * Update user layer.
     *
     * @param id (String) - layer id.
     * @param name (String) - layer name.
     * @param zOrder (float) - z-order position of layer. Default value is 5.5f
     * @param points Map<Sting, AMapPoint> - list of points. Nullable.
     * @param imagePoints (boolean) - use new style for points on map or not. Also default zoom bounds for new style can be edited.
     */
    boolean updateMapLayer(in UpdateMapLayerParams params);

    /**
     * Import GPX file to OsmAnd (from URI or file).
     *
     * @param gpxUri (Uri) - URI created by FileProvider (preferable method).
     * @param file (File) - File which represents GPX track (not recomended, OsmAnd should have rights to access file location).
     * @param fileName (String) - Destination file name. May contain dirs.
     * @param color (String) - color of gpx. Can be one of: "red", "orange", "lightblue", "blue", "purple",
     *                    "translucent_red", "translucent_orange", "translucent_lightblue",
     *                    "translucent_blue", "translucent_purple"
     * @param show (boolean) - show track on the map after import
     */
    boolean importGpx(in ImportGpxParams params);

    /**
     * Show GPX file on map.
     *
     * @param fileName (String) - file name to show. Must be imported first.
     */
    boolean showGpx(in ShowGpxParams params);

    /**
     * Hide GPX file.
     *
     * @param fileName (String) - file name to hide.
     */
    boolean hideGpx(in HideGpxParams params);

    /**
     * Get list of active GPX files.
     *
     * @return list of active gpx files.
     */
    boolean getActiveGpx(out List<ASelectedGpxFile> files);

    /**
     * Set map view to current location.
     *
     * @param latitude (double) - latitude of new map center.
     * @param longitude (double) - longitude of new map center.
     * @param zoom (float) - map zoom level. Set 0 to keep zoom unchanged.
     * @param animated (boolean) - set true to animate changes.
     */
    boolean setMapLocation(in SetMapLocationParams params);


    boolean calculateRoute(in CalculateRouteParams params);

      /**
       * Refresh the map (UI)
       */
    boolean refreshMap();

      /**
       * Add favorite group with given params.
       *
       * @param name (String)    - group name.
       * @param color (String)  - group color. Can be one of: "red", "orange", "yellow",
       *                "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
       * @param visible (boolean) - group visibility.
       */
    boolean addFavoriteGroup(in AddFavoriteGroupParams params);

    	/**
    	 * Remove favorite group with given name.
    	 *
    	 * @param name (String) - name of favorite group.
    	 */
    boolean removeFavoriteGroup(in RemoveFavoriteGroupParams params);

    	/**
    	 * Update favorite group with given params.
    	 *
    	 * @param namePrev (String) - group name (current).
    	 * @param colorPrev (String) - group color (current).
    	 * @param visiblePrev (boolean) - group visibility (current).
    	 * @param nameNew (String)  - group name (new).
    	 * @param colorNew (String)  - group color (new).
    	 * @param visibleNew (boolean) - group visibility (new).
    	 */
    boolean updateFavoriteGroup(in UpdateFavoriteGroupParams params);

    	/**
    	 * Add favorite at given location with given params.
    	 *
    	 * @param lat (double) - latitude.
    	 * @param lon (double)  - longitude.
    	 * @param name (String)  - name of favorite item.
    	 * @param description (String)  - description of favorite item.
    	 * @param category (String)  - category of favorite item.
    	 * @param color (String)  - color of favorite item. Can be one of: "red", "orange", "yellow",
    	 *                    "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
    	 * @param visible (boolean) - should favorite item be visible after creation.
    	 */
    boolean addFavorite(in AddFavoriteParams params);

      /**
       * Remove favorite at given location with given params.
       *
       * @param lat (double)  - latitude.
       * @param lon (double) - longitude.
       * @param name (String) - name of favorite item.
       * @param category (String) - category of favorite item.
       */
    boolean removeFavorite(in RemoveFavoriteParams params);

    	/**
    	 * Update favorite at given location with given params.
    	 *
    	 * @param latPrev (double)  - latitude (current favorite).
    	 * @param lonPrev (double) - longitude (current favorite).
    	 * @param namePrev (String) - name of favorite item (current favorite).
    	 * @param categoryPrev (String) - category of favorite item (current favorite).
    	 * @param latNew (double)  - latitude (new favorite).
    	 * @param lonNew (double)  - longitude (new favorite).
    	 * @param nameNew (String)  - name of favorite item (new favorite).
    	 * @param descriptionNew (String) - description of favorite item (new favorite).
    	 * @param categoryNew (String) - category of favorite item (new favorite). Use only to create a new category,
    	 *                       not to update an existing one. If you want to  update an existing category,
    	 *                       use the {@link #updateFavoriteGroup(String, String, boolean, String, String, boolean)} method.
    	 * @param colorNew (String) - color of new category. Can be one of: "red", "orange", "yellow",
    	 *                       "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
    	 * @param visibleNew (boolean) - should new category be visible after creation.
    	 */
    boolean updateFavorite(in UpdateFavoriteParams params);

    /**
     * Start gpx recording.
     */
    boolean startGpxRecording(in StartGpxRecordingParams params);

    /**
     * Stop gpx recording.
     */
    boolean stopGpxRecording(in StopGpxRecordingParams params);

    /**
     * Take photo note.
     *
     * @param lat (double) - latutude of photo note.
     * @param lon (double) - longitude of photo note.
     */
    boolean takePhotoNote(in TakePhotoNoteParams params);

    /**
     * Start video note recording.
     *
     * @param lat (double) - latutude of video note point.
     * @param lon (double) - longitude of video note point.
     */
    boolean startVideoRecording(in StartVideoRecordingParams params);

    /**
     * Start audio note recording.
     *
     * @param lat (double) - latutude of audio note point.
     * @param lon (double) - longitude of audio note point.
     */
    boolean startAudioRecording(in StartAudioRecordingParams params);

    /**
     * Stop Audio/Video recording.
     */
    boolean stopRecording(in StopRecordingParams params);

    /**
     * Start navigation.
     *
     * @param startName (String) - name of the start point as it displays in OsmAnd's UI. Nullable.
     * @param startLat (double) - latitude of the start point. If 0 - current location is used.
     * @param startLon (double) - longitude of the start point. If 0 - current location is used.
     * @param destName (String) - name of the start point as it displays in OsmAnd's UI.
     * @param destLat (double) - latitude of a destination point.
     * @param destLon (double) - longitude of a destination point.
     * @param profile (String)  - One of: "default", "car", "bicycle", "pedestrian", "aircraft", "boat", "hiking", "motorcycle", "truck". Nullable (default).
     * @param force (boolean) - ask to stop current navigation if any. False - ask. True - don't ask.
     */
    boolean navigate(in NavigateParams params);

    /**
     * Start navigation using gpx file. User need to grant Uri permission to OsmAnd.
     *
     * @param gpxUri (Uri) - URI created by FileProvider.
     * @param force (boolean) - ask to stop current navigation if any. False - ask. True - don't ask.
     */
    boolean navigateGpx(inout NavigateGpxParams params);

    /**
     * Remove GPX file.
     *
     * @param fileName (String) - file name to remove;
     */
    boolean removeGpx(in RemoveGpxParams params);


    /**
     * Show AMapPoint on map in OsmAnd.
     *
     * @param layerId (String) - layer id. Note: layer should be added first.
     * @param pointId (String) - point id.
     * @param shortName (String) - short name (single char). Displayed on the map.
     * @param fullName (String) - full name. Displayed in the context menu on first row.
     * @param typeName (String) - type name. Displayed in context menu on second row.
     * @param color (int) - color of circle's background.
     * @param location (ALatLon) - location of the point.
     * @param details List<String> - list of details. Displayed under context menu.
     * @param params Map<String, String> - optional map of params for point.
     */
    boolean showMapPoint(in ShowMapPointParams params);

    /**
     * Method for adding up to 3 items to the OsmAnd navigation drawer.
     *
     * @param appPackage - current application package.
     * @param names - list of names for items.
     * @param uris - list of uris for intents.
     * @param iconNames - list of icon names for items.
     * @param flags - list of flags for intents. Use -1 if no flags needed.
     */
    boolean setNavDrawerItems(in SetNavDrawerItemsParams params);

    /**
     * Put navigation on pause.
     */
    boolean pauseNavigation(in PauseNavigationParams params);

    /**
     * Resume navigation if it was paused before.
     */
    boolean resumeNavigation(in ResumeNavigationParams params);

    /**
     * Stop navigation. Removes target / intermediate points and route path from the map.
     */
    boolean stopNavigation(in StopNavigationParams params);


    /**
     * Mute voice guidance. Stays muted until unmute manually or via the api.
     */
    boolean muteNavigation(in MuteNavigationParams params);

    /**
     * Unmute voice guidance.
     */
    boolean unmuteNavigation(in UnmuteNavigationParams params);

    /**
     * Run search for POI / Address.
     *
     * @param searchQuery (String) - search query string.
     * @param searchType (int) - type of search. Values:
     *                   SearchParams.SEARCH_TYPE_ALL - all kind of search
     *                   SearchParams.SEARCH_TYPE_POI - POIs only
     *                   SearchParams.SEARCH_TYPE_ADDRESS - addresses only
     *
     * @param latitude (double) - latitude of original search location.
     * @param longitude (double) - longitude of original search location.
     * @param radiusLevel (int) - value from 1 to 7. Default value = 1.
     * @param totalLimit (int) - limit of returned search result rows. Default value = -1 (unlimited).
     */
    boolean search(in SearchParams params, IOsmAndAidlCallback callback);

    /**
     * Do search and start navigation.
     *
     * @param startName (String) - name of the start point as it displays in OsmAnd's UI. Nullable.
     * @param startLat (double) - latitude of the start point. If 0 - current location is used.
     * @param startLon (double) - longitude of the start point. If 0 - current location is used.
     * @param searchQuery (String) - Text of a query for searching a destination point. Sent as URI parameter.
     * @param searchLat (double) - original location of search (latitude). Sent as URI parameter.
     * @param searchLon (double) - original location of search (longitude). Sent as URI parameter.
     * @param profile (String) - one of: "default", "car", "bicycle", "pedestrian", "aircraft", "boat", "hiking", "motorcycle", "truck". Nullable (default).
     * @param force (boolean) - ask to stop current navigation if any. False - ask. True - don't ask.
     */
    boolean navigateSearch(in NavigateSearchParams params);

    /**
     * Method to register for periodical callbacks from OsmAnd
     *
     * @param updateTimeMS (long)- period of time in millisecond after which callback is triggered
     * @param callback (IOsmAndCallback)- create and provide instance of {@link IOsmAndAidlCallback} interface
     * @return id (long) - id of callback in OsmAnd. Needed to unsubscribe from updates.
     */
    long registerForUpdates(in long updateTimeMS, IOsmAndAidlCallback callback);

    /**
     * Method to unregister from periodical callbacks from OsmAnd
     *
     * @param callbackId (long)- id of registered callback (provided by OsmAnd
     * in {@link OsmAndAidlHelper#registerForUpdates(long, IOsmAndAidlCallback)})
     */
    boolean unregisterFromUpdates(in long callbackId);

    /**
     * Method for adding image to the top of Osmand's NavDrawer.
     *
     * @param imageUri (String)- image's URI.toString
     *
     * @deprecated
     * Use the {@link #setNavDrawerLogoWithParams(NavDrawerHeaderParams params)} method.
     */
    boolean setNavDrawerLogo(in String imageUri);

    /**
     * Method for selected UI elements (like QuickSearch button) to show.
     *
     * @param ids (List<String>)- list of menu items keys from {@link OsmAndCustomizationConstants}
     */
    boolean setEnabledIds(in List<String> ids);

    /**
     * Method for selected UI elements (like QuickSearch button) to hide.
     *
     * @param ids (List<String>)- list of menu items keys from {@link OsmAndCustomizationConstants}
     */
    boolean setDisabledIds(in List<String> ids);

    /**
     * Method to show selected NavDrawer's menu items.
     *
     * @param patterns (List<String>) - list of menu items names from {@link OsmAndCustomizationConstants}
     */
    boolean setEnabledPatterns(in List<String> patterns);

    /**
     * Method to hide selected NavDrawer's menu items.
     *
     * @param patterns (List<String>)- list of menu items names from {@link OsmAndCustomizationConstants}
     */
    boolean setDisabledPatterns(in List<String> patterns);

    /**
     * Register OsmAnd widgets for visibility.
     *
     * @param widgetKey ()- widget id.
     * @param appModKeys - list of OsmAnd Application modes widget active with. Could be "null" for all modes.
     */
    boolean regWidgetVisibility(in SetWidgetsParams params);

    /**
     * Register OsmAnd widgets for availability.
     *
     * @param widgetKey (String) - widget id.
     * @param appModKeys (List<String>)- ist of OsmAnd Application modes widget active with. Could be "null" for all modes.
     */
    boolean regWidgetAvailability(in SetWidgetsParams params);

    /**
     * Add custom parameters for OsmAnd settings to associate with client app.
     *
     * @param sharedPreferencesName (String)- string with name of clint's app for shared preferences key
     * @param bundle (Bundle)- bundle with keys from Settings IDs {@link OsmAndCustomizationConstants} and Settings params
     */
    boolean customizeOsmandSettings(in OsmandSettingsParams params);

    /**
     * Method to get list of gpx files currently registered (imported or created) in OsmAnd;
     *
     * @return list of gpx files
     */
    boolean getImportedGpx(out List<AGpxFile> files);

    /**
     * Method to get list of sqlitedb files registered in OsmAnd;
     *
     * @return list of sqlitedb files
     */
    boolean getSqliteDbFiles(out List<ASqliteDbFile> files);

    /**
     * Method to get list of currently active sqlitedb files
     *
     * @return list of sqlitedb files
     */
    boolean getActiveSqliteDbFiles(out List<ASqliteDbFile> files);

    /**
     * Method to show selected sqlitedb file as map overlay.
     *
     * @param fileName (String) - name of sqlitedb file
     */
    boolean showSqliteDbFile(String fileName);

    /**
     * Method to hide sqlitedb file from map overlay.
     *
     * @param fileName (String) - name of sqlitedb file
     */
    boolean hideSqliteDbFile(String fileName);

    /**
     * Method for adding image to the top of OsmAnd's NavDrawer with additional params
     *
     * @param imageUri (String) - image's URI.toString
     * @param packageName (String) - client's app package name
     * @param intent (String) - intent for additional functionality on image click
     *
     */
    boolean setNavDrawerLogoWithParams(in NavDrawerHeaderParams params);

    /**
     * Method for adding functionality to "Powered by Osmand" logo in NavDrawer's footer
     * (reset OsmAnd settings to pre-clinet app's state)
     *
     * @param packageName (String) - package name
     * @param intent (String) - intent
     * @param appName (String) - client's app name
     */
    boolean setNavDrawerFooterWithParams(in NavDrawerFooterParams params);

    /**
     * Restore default (pre-client) OsmAnd settings and state:
     * clears features, widgets and settings customization, NavDraw logo.
     */
    boolean restoreOsmand();

    /**
     * Method to change state of plug-ins in OsmAnd.
     *
     * @param pluginId (String) - id (name) of plugin.
     * @param newState (int) - new state (0 - off, 1 - on).
     */
    boolean changePluginState(in PluginParams params);

    /**
     * Method to register for callback on OsmAnd initialization
     * @param callback (IOsmAndAidlCallback) - create and provide instance of {@link IOsmAndAidlCallback} interface
     */
    boolean registerForOsmandInitListener(in IOsmAndAidlCallback callback);

    /**
     * Requests bitmap snap-shot of map with GPX file from provided URI in its center.
     * You can set bitmap size, density and GPX lines color, but you need
     * to manually download appropriate map in OsmAnd or background will be empty.
     * Bitmap will be returned through callback {@link IOsmAndAidlCallback#onGpxBitmapCreated(AGpxBitmap)}
     *
     * @param gpxUri (Uri/File) - Uri for gpx file
     * @param density (float) - image density. Recommended to use default metrics for device's display.
     * @param widthPixels (int) - width of bitmap
     * @param heightPixels (int) - height of bitmap
     * @param color (int) - color in ARGB format
     * @param callback (IOsmAndAidlCallback) - instance of callback from OsmAnd.
     */
    boolean getBitmapForGpx(in CreateGpxBitmapParams file, IOsmAndAidlCallback callback);

    /**
     * Method to copy files to OsmAnd part by part. For now supports only sqlitedb format.
     * Part size (bytearray) should not exceed 256k.
     *
     * @param fileName (String) - name of file
     * @param filePartData (byte[]) - parts of file, byte[] with size 256k or less.
     * @param startTime (long) - timestamp of copying start.
     * @param isDone (boolean) - boolean to mark end of copying.
     * @return number of last successfully received file part or error(-1).
     */
    int copyFile(in CopyFileParams filePart);

    /**
     * Method to register for updates during navgation. Notifies user about distance to the next turn and its type.
     *
     * @param subscribeToUpdates (boolean) - subscribe or unsubscribe from updates
     * @param callbackId (long) - id of callback, needed to unsubscribe from updates
     * @param callback (IOsmAndAidlCallback) - callback to notify user on navigation data change
     */
    long registerForNavigationUpdates(in ANavigationUpdateParams params, IOsmAndAidlCallback callback);

    /**
     * Method to add Context Menu buttons to OsmAnd Context menu.
     *
     * {@link ContextMenuButtonsParams } is a wrapper class for params:
     *
     * @param leftButton (AContextMenuButton) - parameters for left context button:
     * @param buttonId (String at AContextMenuButton) - id of button in View
     * @param leftTextCaption (String at AContextMenuButton) - left-side button text
     * @param rightTextCaption (String at AContextMenuButton) - right-side button text
     * @param String leftIconName (String at AContextMenuButton) - name of left-side icon
     * @param String rightIconName (String at AContextMenuButton) - name of right-side icon
     * @param boolean needColorizeIcon (booleanat AContextMenuButton) - flag to apply color to icon
     * @param boolean enabled (boolean at AContextMenuButton) - enable button flag
     *
     * @param rightButton (AContextMenuButton) - parameters for right context button, see <i>leftButton</i> param for details.
     * @param id (String) - button id;
     * @param appPackage (String) - clinet's app package name
     * @param layerId (String) - id of Osmand's map layer
     * @param callbackId (long) - {@link IOsmAndAidlCallback} id
     * @param pointsIds (List<String>) - list of point Ids to which this rules applies to.
     *
     * @param callback (IOsmAndAidlCallback) - AIDL callback;
     *
     * @return long - callback's Id;
     */
    long addContextMenuButtons(in ContextMenuButtonsParams params, IOsmAndAidlCallback callback);

    /**
     * Method to remove Context Menu buttons from OsmAnd Context menu.
     *
     * {@link RemoveContextMenuButtonsParams} is a wrapper class for params:
     *
     * @param paramsId (String) - id of {@link ContextMenuButtonsParams} of button you want to remove;
     * @param callbackId (long) - id of {@ling IOsmAndAidlCallback} of button you want to remove;
     *
     */
    boolean removeContextMenuButtons(in RemoveContextMenuButtonsParams params);

    /**
     * Method to update params on already set custom Context Button.
     *
     * {@link UpdateContextMenuButtonsParams } is a wrapper class for params:
     *
     * @param leftButton (AContextMenuButton) - parameters for left context button:
     * @param buttonId (String at AContextMenuButton) - id of button in View
     * @param leftTextCaption (String at AContextMenuButton) - left-side button text
     * @param rightTextCaption (String at AContextMenuButton) - right-side button text
     * @param String leftIconName (String at AContextMenuButton) - name of left-side icon
     * @param String rightIconName (String at AContextMenuButton) - name of right-side icon
     * @param boolean needColorizeIcon (booleanat AContextMenuButton) - flag to apply color to icon
     * @param boolean enabled (boolean at AContextMenuButton) - enable button flag
     *
     * @param rightButton (AContextMenuButton) - parameters for right context button, see <i>leftButton</i> param for details.
     * @param id (String) - button id;
     * @param appPackage (String) - clinet's app package name
     * @param layerId (String) - id of Osmand's map layer
     * @param callbackId (long) - {@link IOsmAndAidlCallback} id
     * @param pointsIds (List<String>) - list of point Ids to which this rules applies to.
     *
     */
    boolean updateContextMenuButtons(in UpdateContextMenuButtonsParams params);

    /**
     * Method to check if there is a customized setting in OsmAnd Settings.
     *
     * {@link OsmandSettingsInfoParams} is a wrapper class for params:
     *
     * @param sharedPreferencesName (String at OsmandSettingInfoParams) - key of setting in OsmAnd's preferences.
     *
     * @return boolean - true if setting is already set in SharedPreferences
     *
     */
    boolean areOsmandSettingsCustomized(in OsmandSettingsInfoParams params);

    /**
     * Method to customize parameters of OsmAnd.
     *
     * @param params (CustomizationInfoParams) - wrapper class for custom settings and ui.
     *
     * @param settingsParams (OsmandSettingsParams) - wrapper class for OsmAnd shared preferences params.
     * 			   See {@link #customizeOsmandSettings(in OsmandSettingsParams params) customizeOsmandSettings}
     * 			   method description for details.
     * @param navDrawerHeaderParams (NavDrawerHeaderParams) - wrapper class for OsmAnd navdrawer header params.
     * 			   See {@link #setNavDrawerLogoWithParams(in NavDrawerHeaderParams params) setNavDrawerLogoWithParams}
     * 			   method description for details.
     * @param navDrawerFooterParams (NavDrawerFooterParams) - wrapper class for OsmAnd navdrawer footer params.
     * 			   See {@link #setNavDrawerFooterWithParams(in NavDrawerFooterParams params) setNavDrawerFooterWithParams}
     * 			   method description for details.
     * @param visibilityWidgetsParams (ArrayList<SetWidgetsParams>) - wrapper class for OsmAnd widgets visibility.
     * 			   See {@link #regWidgetVisibility(in SetWidgetsParams params) regWidgetVisibility}
     * 			   method description for details.
     * @param availabilityWidgetsParams (ArrayList<SetWidgetsParams>) - wrapper class for OsmAnd widgets availability.
     * 			   See {@link #regWidgetAvailability(in SetWidgetsParams params) regWidgetAvailability}
     * 			   method description for details.
     * @param pluginsParams (ArrayList<PluginParams>) - wrapper class for OsmAnd plugins states params.
     * 			   See {@link #changePluginState(in PluginParams params) changePluginState}
     * 			   method description for details.
     * @param featuresEnabledIds (List<String>) - list of UI elements (like QuickSearch button) to show.
     * 			   See {@link #setEnabledIds(in List<String> ids) setEnabledIds}
     * @param featuresDisabledIds (List<String>) - list of UI elements (like QuickSearch button) to hide.
     * 			   See {@link #setDisabledIds(in List<String> ids) setDisabledIds}
     * @param featuresEnabledPatterns (List<String>) - list of NavDrawer menu items to show.
     * 			   See {@link #setEnabledPatterns(in List<String> patterns) setEnabledPatterns}
     * @param featuresDisabledPatterns (List<String>) - list of NavDrawer menu items to hide.
     * 			   See {@link #setDisabledPatterns(in List<String> patterns) setDisabledPatterns}
     *
     */
    boolean setCustomization(in CustomizationInfoParams params);

    /**
     * Method to register for Voice Router voice messages during navigation. Notifies user about voice messages.
     *
     * @params subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
     * @params callbackId (long) - id of callback, needed to unsubscribe from messages
     * @params callback (IOsmAndAidlCallback) - callback to notify user on voice message
     */
    long registerForVoiceRouterMessages(in ANavigationVoiceRouterMessageParams params, IOsmAndAidlCallback callback);

    /**
     * Removes all active map markers (marks them as passed and moves to history)
     * Empty class of params
     */
    boolean removeAllActiveMapMarkers(in RemoveMapMarkersParams params);

    boolean importProfile(in ProfileSettingsParams params);

    boolean executeQuickAction(in QuickActionParams params);

    boolean getQuickActionsInfo(out List<QuickActionInfoParams> quickActions);

    /**
     * Toggle Lock/Unlock screen.
     */
     boolean setLockState(in SetLockStateParams params);

    /**
     * Method to register for  key events.
     *
     * @params subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from key events
     * @params callbackId (long) - id of callback, needed to unsubscribe key events
     * @params callback (IOsmAndAidlCallback) - callback to notify user on key event
     * @params keyEventList (List<Integer>) - list of requested key events
     */
    long registerForKeyEvents(in AKeyEventsParams params, IOsmAndAidlCallback callback);

    AppInfoParams getAppInfo();

    boolean setMapMargins(in MapMarginsParams params);

    boolean exportProfile(in ExportProfileParams params);

     /**
     * Is any fragment open.
     */
    boolean isFragmentOpen();

    /**
    * Is contect menu open.
    */
    boolean isMenuOpen();

    int getPluginVersion(in CustomPluginParams params);

    boolean selectProfile(in SelectProfileParams params);

    boolean getProfiles(out List<AProfile> profiles);

    boolean getBlockedRoads(out List<ABlockedRoad> blockedRoads);

    boolean addRoadBlock(in AddBlockedRoadParams params);

    boolean removeRoadBlock(in RemoveBlockedRoadParams params);

    boolean setLocation(in SetLocationParams params);

    boolean exitApp(in ExitAppParams params);

    boolean getText(inout GetTextParams params);

    boolean reloadIndexes();

    boolean setPreference(in PreferenceParams params);

    boolean getPreference(inout PreferenceParams params);

    /**
     * Method to register for Logcat messages. Notifies user about new logs in application.
     *
     * @param subscribeToUpdates (boolean) - boolean flag to subscribe or unsubscribe from messages
     * @param callbackId (long) - id of callback, needed to unsubscribe from messages
     * @param filterLevel (String) determines which type of logs will be returned by callback
     * Must be one of the values below:
     * - "D" (debug)
     * - "I" (info)
     * - "W" (warn)
     * - "E" (error)
     * @param callback (IOsmAndAidlCallback) - callback to notify user on new OsmAnd logs
     */
    long registerForLogcatMessages(in ALogcatListenerParams params, IOsmAndAidlCallback callback);

    boolean setZoomLimits(in ZoomLimitsParams params);
}