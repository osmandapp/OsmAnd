package net.osmand.aidl;

import net.osmand.aidl.map.ALatLon;
import net.osmand.aidl.map.SetMapLocationParams;

import net.osmand.aidl.favorite.group.AFavoriteGroup;
import net.osmand.aidl.favorite.group.AddFavoriteGroupParams;
import net.osmand.aidl.favorite.group.RemoveFavoriteGroupParams;
import net.osmand.aidl.favorite.group.UpdateFavoriteGroupParams;

import net.osmand.aidl.favorite.AFavorite;
import net.osmand.aidl.favorite.AddFavoriteParams;
import net.osmand.aidl.favorite.RemoveFavoriteParams;
import net.osmand.aidl.favorite.UpdateFavoriteParams;

import net.osmand.aidl.mapmarker.AMapMarker;
import net.osmand.aidl.mapmarker.AddMapMarkerParams;
import net.osmand.aidl.mapmarker.RemoveMapMarkerParams;
import net.osmand.aidl.mapmarker.UpdateMapMarkerParams;

import net.osmand.aidl.calculateroute.CalculateRouteParams;

import net.osmand.aidl.gpx.ImportGpxParams;
import net.osmand.aidl.gpx.ShowGpxParams;
import net.osmand.aidl.gpx.StartGpxRecordingParams;
import net.osmand.aidl.gpx.StopGpxRecordingParams;
import net.osmand.aidl.gpx.HideGpxParams;
import net.osmand.aidl.gpx.ASelectedGpxFile;

import net.osmand.aidl.mapwidget.AMapWidget;
import net.osmand.aidl.mapwidget.AddMapWidgetParams;
import net.osmand.aidl.mapwidget.RemoveMapWidgetParams;
import net.osmand.aidl.mapwidget.UpdateMapWidgetParams;

import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.aidl.maplayer.point.AddMapPointParams;
import net.osmand.aidl.maplayer.point.RemoveMapPointParams;
import net.osmand.aidl.maplayer.point.UpdateMapPointParams;
import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.AddMapLayerParams;
import net.osmand.aidl.maplayer.RemoveMapLayerParams;
import net.osmand.aidl.maplayer.UpdateMapLayerParams;

import net.osmand.aidl.navigation.NavigateParams;
import net.osmand.aidl.navigation.NavigateGpxParams;

import net.osmand.aidl.note.TakePhotoNoteParams;
import net.osmand.aidl.note.StartVideoRecordingParams;
import net.osmand.aidl.note.StartAudioRecordingParams;
import net.osmand.aidl.note.StopRecordingParams;

import net.osmand.aidl.gpx.RemoveGpxParams;

import net.osmand.aidl.maplayer.point.ShowMapPointParams;

import net.osmand.aidl.navdrawer.SetNavDrawerItemsParams;
import net.osmand.aidl.navdrawer.NavDrawerFooterParams;
import net.osmand.aidl.navdrawer.NavDrawerHeaderParams;

import net.osmand.aidl.navigation.PauseNavigationParams;
import net.osmand.aidl.navigation.ResumeNavigationParams;
import net.osmand.aidl.navigation.StopNavigationParams;
import net.osmand.aidl.navigation.MuteNavigationParams;
import net.osmand.aidl.navigation.UnmuteNavigationParams;

import net.osmand.aidl.IOsmAndAidlCallback;

import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.search.SearchParams;
import net.osmand.aidl.navigation.NavigateSearchParams;

import net.osmand.aidl.customization.SetWidgetsParams;
import net.osmand.aidl.customization.OsmandSettingsParams;

import net.osmand.aidl.gpx.AGpxFile;
import net.osmand.aidl.gpx.AGpxFileDetails;
import net.osmand.aidl.gpx.CreateGpxBitmapParams;

import net.osmand.aidl.tiles.ASqliteDbFile;

import net.osmand.aidl.plugins.PluginParams;
import net.osmand.aidl.copyfile.CopyFileParams;

import net.osmand.aidl.navigation.ANavigationUpdateParams;


// NOTE: Add new methods at the end of file!!!

interface IOsmAndAidlInterface {

    /**
     * Add map marker at given location.
     *
     * @param lat  - latitude.
     * @param lon  - longitude.
     * @param name - name.
     */
    boolean addMapMarker(in AddMapMarkerParams params);

    /**
     * Remove map marker at given location with name.
     *
     * @param lat  - latitude.
     * @param lon  - longitude.
     * @param name - name.
     */
    boolean removeMapMarker(in RemoveMapMarkerParams params);

    /**
     * Update map marker at given location with name.
     *
     * @param latPrev  - latitude (current marker).
     * @param lonPrev  - longitude (current marker).
     * @param namePrev - name (current marker).
     * @param latNew  - latitude (new marker).
     * @param lonNew  - longitude (new marker).
     * @param nameNew - name (new marker).
     */
    boolean updateMapMarker(in UpdateMapMarkerParams params);

    /**
     * Add map widget to the right side of the main screen.
     * Note: any specified icon should exist in OsmAnd app resources.
     *
     * @param id - widget id.
     * @param menuIconName - icon name (configure map menu).
     * @param menuTitle - widget name (configure map menu).
     * @param lightIconName - icon name for the light theme (widget).
     * @param darkIconName - icon name for the dark theme (widget).
     * @param text - main widget text.
     * @param description - sub text, like "km/h".
     * @param order - order position in the widgets list.
     * @param intentOnClick - onClick intent. Called after click on widget as startActivity(Intent intent).
     */
    boolean addMapWidget(in AddMapWidgetParams params);

    /**
     * Remove map widget.
     *
     * @param id - widget id.
     */
    boolean removeMapWidget(in RemoveMapWidgetParams params);

    /**
     * Update map widget.
     * Note: any specified icon should exist in OsmAnd app resources.
     *
     * @param id - widget id.
     * @param menuIconName - icon name (configure map menu).
     * @param menuTitle - widget name (configure map menu).
     * @param lightIconName - icon name for the light theme (widget).
     * @param darkIconName - icon name for the dark theme (widget).
     * @param text - main widget text.
     * @param description - sub text, like "km/h".
     * @param order - order position in the widgets list.
     * @param intentOnClick - onClick intent. Called after click on widget as startActivity(Intent intent).
     */
    boolean updateMapWidget(in UpdateMapWidgetParams params);

   /**
    * Add point to user layer.
    *
    * @param layerId - layer id. Note: layer should be added first.
    * @param pointId - point id.
    * @param shortName - short name (single char). Displayed on the map.
    * @param fullName - full name. Displayed in the context menu on first row.
    * @param typeName - type name. Displayed in context menu on second row.
    * @param color - color of circle's background.
    * @param location - location of the point.
    * @param details - list of details. Displayed under context menu.
    * @param params - optional map of params for point.
    */
    boolean addMapPoint(in AddMapPointParams params);


    /**
     * Remove point.
     *
     * @param layerId - layer id.
     * @param pointId - point id.
     */
    boolean removeMapPoint(in RemoveMapPointParams params);

    /**
     * Update point.
     *
     * @param layerId - layer id.
     * @param pointId - point id.
     * @param shortName - short name (single char). Displayed on the map.
     * @param fullName - full name. Displayed in the context menu on first row.
     * @param typeName - type name. Displayed in context menu on second row.
     * @param color - color of circle's background.
     * @param location - location of the point.
     * @param details - list of details. Displayed under context menu.
     * @param params - optional map of params for point.
     */
    boolean updateMapPoint(in UpdateMapPointParams params);

    /**
     * Add user layer on the map.
     *
     * @param id - layer id.
     * @param name - layer name.
     * @param zOrder - z-order position of layer. Default value is 5.5f
     * @param points - initial list of points. Nullable.
     * @param imagePoints - use new style for points on map or not. Also default zoom bounds for new style can be edited.
     */
    boolean addMapLayer(in AddMapLayerParams params);

    /**
     * Remove user layer.
     *
     * @param id - layer id.
     */
    boolean removeMapLayer(in RemoveMapLayerParams params);

    /**
     * Update user layer.
     *
     * @param id - layer id.
     * @param name - layer name.
     * @param zOrder - z-order position of layer. Default value is 5.5f
     * @param points - list of points. Nullable.
     * @param imagePoints - use new style for points on map or not. Also default zoom bounds for new style can be edited.
     */
    boolean updateMapLayer(in UpdateMapLayerParams params);

    /**
     * Import GPX file to OsmAnd (from URI or file).
     *
     * @param gpxUri    - URI created by FileProvider (preferable method).
     * @param file      - File which represents GPX track (not recomended, OsmAnd should have rights to access file location).
     * @param fileName  - Destination file name. May contain dirs.
     * @param color     - color of gpx. Can be one of: "red", "orange", "lightblue", "blue", "purple",
     *                    "translucent_red", "translucent_orange", "translucent_lightblue",
     *                    "translucent_blue", "translucent_purple"
     * @param show      - show track on the map after import
     */
    boolean importGpx(in ImportGpxParams params);

    /**
     * Show GPX file on map.
     *
     * @param fileName - file name to show. Must be imported first.
     */
    boolean showGpx(in ShowGpxParams params);

    /**
     * Hide GPX file.
     *
     * @param fileName - file name to hide.
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
     * @param latitude - latitude of new map center.
     * @param longitude - longitude of new map center.
     * @param zoom - map zoom level. Set 0 to keep zoom unchanged.
     * @param animated - set true to animate changes.
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
       * @param name    - group name.
       * @param color   - group color. Can be one of: "red", "orange", "yellow",
       *                "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
       * @param visible - group visibility.
       */
    boolean addFavoriteGroup(in AddFavoriteGroupParams params);

    	/**
    	 * Update favorite group with given params.
    	 *
    	 * @param namePrev    - group name (current).
    	 * @param colorPrev   - group color (current).
    	 * @param visiblePrev - group visibility (current).
    	 * @param nameNew     - group name (new).
    	 * @param colorNew    - group color (new).
    	 * @param visibleNew  - group visibility (new).
    	 */
    boolean removeFavoriteGroup(in RemoveFavoriteGroupParams params);

    	/**
    	 * Remove favorite group with given name.
    	 *
    	 * @param name - name of favorite group.
    	 */
    boolean updateFavoriteGroup(in UpdateFavoriteGroupParams params);

    	/**
    	 * Add favorite at given location with given params.
    	 *
    	 * @param lat         - latitude.
    	 * @param lon         - longitude.
    	 * @param name        - name of favorite item.
    	 * @param description - description of favorite item.
    	 * @param category    - category of favorite item.
    	 * @param color       - color of favorite item. Can be one of: "red", "orange", "yellow",
    	 *                    "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
    	 * @param visible     - should favorite item be visible after creation.
    	 */
    boolean addFavorite(in AddFavoriteParams params);

    	/**
    	 * Update favorite at given location with given params.
    	 *
    	 * @param latPrev        - latitude (current favorite).
    	 * @param lonPrev        - longitude (current favorite).
    	 * @param namePrev       - name of favorite item (current favorite).
    	 * @param categoryPrev   - category of favorite item (current favorite).
    	 * @param latNew         - latitude (new favorite).
    	 * @param lonNew         - longitude (new favorite).
    	 * @param nameNew        - name of favorite item (new favorite).
    	 * @param descriptionNew - description of favorite item (new favorite).
    	 * @param categoryNew    - category of favorite item (new favorite). Use only to create a new category,
    	 *                       not to update an existing one. If you want to  update an existing category,
    	 *                       use the {@link #updateFavoriteGroup(String, String, boolean, String, String, boolean)} method.
    	 * @param colorNew       - color of new category. Can be one of: "red", "orange", "yellow",
    	 *                       "lightgreen", "green", "lightblue", "blue", "purple", "pink", "brown".
    	 * @param visibleNew     - should new category be visible after creation.
    	 */
    boolean removeFavorite(in RemoveFavoriteParams params);

      /**
       * Remove favorite at given location with given params.
       *
       * @param lat      - latitude.
       * @param lon      - longitude.
       * @param name     - name of favorite item.
       * @param category - category of favorite item.
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
     * @param lat - latutude of photo note.
     * @param lon - longitude of photo note.
     */
    boolean takePhotoNote(in TakePhotoNoteParams params);

    /**
     * Start video note recording.
     *
     * @param lat - latutude of video note point.
     * @param lon - longitude of video note point.
     */
    boolean startVideoRecording(in StartVideoRecordingParams params);

    /**
     * Start audio note recording.
     *
     * @param lat - latutude of audio note point.
     * @param lon - longitude of audio note point.
     */
    boolean startAudioRecording(in StartAudioRecordingParams params);

    /**
     * Stop Audio/Video recording.
     */
    boolean stopRecording(in StopRecordingParams params);

    /**
     * Start navigation.
     *
     * @param startName - name of the start point as it displays in OsmAnd's UI. Nullable.
     * @param startLat - latitude of the start point. If 0 - current location is used.
     * @param startLon - longitude of the start point. If 0 - current location is used.
     * @param destName - name of the start point as it displays in OsmAnd's UI.
     * @param destLat - latitude of a destination point.
     * @param destLon - longitude of a destination point.
     * @param profile - One of: "default", "car", "bicycle", "pedestrian", "aircraft", "boat", "hiking", "motorcycle", "truck". Nullable (default).
     * @param force - ask to stop current navigation if any. False - ask. True - don't ask.
     */
    boolean navigate(in NavigateParams params);

    /**
     * Start navigation using gpx file. User need to grant Uri permission to OsmAnd.
     *
     * @param gpxUri - URI created by FileProvider.
     * @param force - ask to stop current navigation if any. False - ask. True - don't ask.
     */
    boolean navigateGpx(in NavigateGpxParams params);

    /**
     * Remove GPX file.
     *
     * @param fileName - file name to remove;
     */
    boolean removeGpx(in RemoveGpxParams params);


    /**
     * Show AMapPoint on map in OsmAnd.
     *
     * @param layerId   - layer id. Note: layer should be added first.
     * @param pointId   - point id.
     * @param shortName - short name (single char). Displayed on the map.
     * @param fullName  - full name. Displayed in the context menu on first row.
     * @param typeName  - type name. Displayed in context menu on second row.
     * @param color     - color of circle's background.
     * @param location  - location of the point.
     * @param details   - list of details. Displayed under context menu.
     * @param params    - optional map of params for point.
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
     * @param searchQuery - search query string.
     * @param searchType - type of search. Values:
     *                   SearchParams.SEARCH_TYPE_ALL - all kind of search
     *                   SearchParams.SEARCH_TYPE_POI - POIs only
     *                   SearchParams.SEARCH_TYPE_ADDRESS - addresses only
     *
     * @param latitude - latitude of original search location.
     * @param longitude - longitude of original search location.
     * @param radiusLevel - value from 1 to 7. Default value = 1.
     * @param totalLimit - limit of returned search result rows. Default value = -1 (unlimited).
     */
    boolean search(in SearchParams params, IOsmAndAidlCallback callback);

    /**
     * Do search and start navigation.
     *
     * @param startName - name of the start point as it displays in OsmAnd's UI. Nullable.
     * @param startLat - latitude of the start point. If 0 - current location is used.
     * @param startLon - longitude of the start point. If 0 - current location is used.
     * @param searchQuery  - Text of a query for searching a destination point. Sent as URI parameter.
     * @param searchLat - original location of search (latitude). Sent as URI parameter.
     * @param searchLon - original location of search (longitude). Sent as URI parameter.
     * @param profile - one of: "default", "car", "bicycle", "pedestrian", "aircraft", "boat", "hiking", "motorcycle", "truck". Nullable (default).
     * @param force - ask to stop current navigation if any. False - ask. True - don't ask.
     */
    boolean navigateSearch(in NavigateSearchParams params);

    /**
     * Method to register for periodical callbacks from OsmAnd
     *
     * @param updateTimeMS - period of time in millisecond after which callback is triggered
     * @param callback - create and provide instance of {@link IOsmAndAidlCallback} interface
     * @return id of callback in OsmAnd. Needed to unsubscribe from updates.
     */
    long registerForUpdates(in long updateTimeMS, IOsmAndAidlCallback callback);

    /**
     * Method to unregister from periodical callbacks from OsmAnd
     *
     * @param callbackId - id of registered callback (provided by OsmAnd
     * in {@link OsmAndAidlHelper#registerForUpdates(long, IOsmAndAidlCallback)})
     */
    boolean unregisterFromUpdates(in long callbackId);

    /**
     * Method for adding image to the top of Osmand's NavDrawer.
     *
     * @param imageUri - image's URI.toString
     *
     * @deprecated
     * Use the {@link #setNavDrawerLogoWithParams(NavDrawerHeaderParams params)} method.
     */
    boolean setNavDrawerLogo(in String imageUri);

    /**
     * Method for selected UI elements (like QuickSearch button) to show.
     *
     * @param ids - list of menu items keys from {@link OsmAndCustomizationConstants}
     */
    boolean setEnabledIds(in List<String> ids);

    /**
     * Method for selected UI elements (like QuickSearch button) to hide.
     *
     * @param ids - list of menu items keys from {@link OsmAndCustomizationConstants}
     */
    boolean setDisabledIds(in List<String> ids);

    /**
     * Method to show selected NavDrawer's menu items.
     *
     * @param patterns - list of menu items names from {@link OsmAndCustomizationConstants}
     */
    boolean setEnabledPatterns(in List<String> patterns);

    /**
     * Method to hide selected NavDrawer's menu items.
     *
     * @param patterns - list of menu items names from {@link OsmAndCustomizationConstants}
     */
    boolean setDisabledPatterns(in List<String> patterns);

    /**
     * Register OsmAnd widgets for visibility.
     *
     * @param widgetKey - widget id.
     * @param appModKeys - list of OsmAnd Application modes widget active with. Could be "null" for all modes.
     */
    boolean regWidgetVisibility(in SetWidgetsParams params);

    /**
     * Register OsmAnd widgets for availability.
     *
     * @param widgetKey - widget id.
     * @param appModKeys - ist of OsmAnd Application modes widget active with. Could be "null" for all modes.
     */
    boolean regWidgetAvailability(in SetWidgetsParams params);

    /**
     * Add custom parameters for OsmAnd settings to associate with client app.
     *
     * @param sharedPreferencesName - string with name of clint's app for shared preferences key
     * @param bundle - bundle with keys from Settings IDs {@link OsmAndCustomizationConstants} and Settings params
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
     * @param fileName - name of sqlitedb file
     */
    boolean showSqliteDbFile(String fileName);

    /**
     * Method to hide sqlitedb file from map overlay.
     *
     * @param fileName - name of sqlitedb file
     */
    boolean hideSqliteDbFile(String fileName);

    /**
     * Method for adding image to the top of OsmAnd's NavDrawer with additional params
     *
     * @param imageUri - image's URI.toString
     * @param packageName - client's app package name
     * @param intent - intent for additional functionality on image click
     *
     */
    boolean setNavDrawerLogoWithParams(in NavDrawerHeaderParams params);

    /**
     * Method for adding functionality to "Powered by Osmand" logo in NavDrawer's footer
     * (reset OsmAnd settings to pre-clinet app's state)
     *
     * @param packageName - package name
     * @param intent - intent
     * @param appName - client's app name
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
     * @param pluginId - id (name) of plugin.
     * @param newState - new state (0 - off, 1 - on).
     */
    boolean changePluginState(in PluginParams params);

    /**
     * Method to register for callback on OsmAnd initialization
     * @param callback - create and provide instance of {@link IOsmAndAidlCallback} interface
     */
    boolean registerForOsmandInitListener(in IOsmAndAidlCallback callback);

    /**
     * Requests bitmap snap-shot of map with GPX file from provided URI in its center.
     * You can set bitmap size, density and GPX lines color, but you need
     * to manually download appropriate map in OsmAnd or background will be empty.
     * Bitmap will be returned through callback {@link IOsmAndAidlCallback#onGpxBitmapCreated(AGpxBitmap)}
     *
     * @param gpxUri - Uri for gpx file
     * @param density - image density. Recommended to use default metrics for device's display.
     * @param widthPixels - width of bitmap
     * @param heightPixels - height of bitmap
     * @param color - color in ARGB format
     * @param callback - instance of callback from OsmAnd.
     */
    boolean getBitmapForGpx(in CreateGpxBitmapParams file, IOsmAndAidlCallback callback);

    /**
     * Method to copy files to OsmAnd part by part. For now supports only sqlitedb format.
     * Part size (bytearray) should not exceed 256k.
     *
     * @param copyPart.fileName - name of file
     * @param copyPart.filePartData - parts of file, byte[] with size 256k or less.
     * @param copyPart.startTime - timestamp of copying start.
     * @param copyPart.isDone - boolean to mark end of copying.
     * @return number of last successfully received file part or error(-1).
     */
    int copyFile(in CopyFileParams filePart);


    /**
     * Method to register for updates during navgation. Notifies user about distance to the next turn and its type.
     *
     * @params params.subscribeToUpdates - boolean flag to subscribe or unsubscribe from updates
     * @params params.callbackId - id of callback, needed to unsubscribe from updates
     * @params callback - callback to notify user on navigation data change
     */
    long registerForNavigationUpdates(in ANavigationUpdateParams params, IOsmAndAidlCallback callback);
}