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

    boolean addMapMarker(in AddMapMarkerParams params);
    boolean removeMapMarker(in RemoveMapMarkerParams params);
    boolean updateMapMarker(in UpdateMapMarkerParams params);

    boolean addMapWidget(in AddMapWidgetParams params);
    boolean removeMapWidget(in RemoveMapWidgetParams params);
    boolean updateMapWidget(in UpdateMapWidgetParams params);

    boolean addMapPoint(in AddMapPointParams params);
    boolean removeMapPoint(in RemoveMapPointParams params);
    boolean updateMapPoint(in UpdateMapPointParams params);

    boolean addMapLayer(in AddMapLayerParams params);
    boolean removeMapLayer(in RemoveMapLayerParams params);
    boolean updateMapLayer(in UpdateMapLayerParams params);

    boolean importGpx(in ImportGpxParams params);
    boolean showGpx(in ShowGpxParams params);
    boolean hideGpx(in HideGpxParams params);
    boolean getActiveGpx(out List<ASelectedGpxFile> files);

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
       * Add map marker at given location.
       *
       * @param lat  - latitude.
       * @param lon  - longitude.
       * @param name - name.
       */
    boolean startGpxRecording(in StartGpxRecordingParams params);


    boolean stopGpxRecording(in StopGpxRecordingParams params);

    boolean takePhotoNote(in TakePhotoNoteParams params);
    boolean startVideoRecording(in StartVideoRecordingParams params);
    boolean startAudioRecording(in StartAudioRecordingParams params);
    boolean stopRecording(in StopRecordingParams params);

    boolean navigate(in NavigateParams params);
    boolean navigateGpx(in NavigateGpxParams params);

    boolean removeGpx(in RemoveGpxParams params);

    boolean showMapPoint(in ShowMapPointParams params);

    boolean setNavDrawerItems(in SetNavDrawerItemsParams params);

    boolean pauseNavigation(in PauseNavigationParams params);
    boolean resumeNavigation(in ResumeNavigationParams params);
    boolean stopNavigation(in StopNavigationParams params);
    boolean muteNavigation(in MuteNavigationParams params);
    boolean unmuteNavigation(in UnmuteNavigationParams params);

    boolean search(in SearchParams params, IOsmAndAidlCallback callback);
    boolean navigateSearch(in NavigateSearchParams params);

    long registerForUpdates(in long updateTimeMS, IOsmAndAidlCallback callback);
    boolean unregisterFromUpdates(in long callbackId);

    boolean setNavDrawerLogo(in String imageUri);

    boolean setEnabledIds(in List<String> ids);
    boolean setDisabledIds(in List<String> ids);
    boolean setEnabledPatterns(in List<String> patterns);
    boolean setDisabledPatterns(in List<String> patterns);

    boolean regWidgetVisibility(in SetWidgetsParams params);
    boolean regWidgetAvailability(in SetWidgetsParams params);

    boolean customizeOsmandSettings(in OsmandSettingsParams params);

    boolean getImportedGpx(out List<AGpxFile> files);

    boolean getSqliteDbFiles(out List<ASqliteDbFile> files);
    boolean getActiveSqliteDbFiles(out List<ASqliteDbFile> files);
    boolean showSqliteDbFile(String fileName);
    boolean hideSqliteDbFile(String fileName);

    boolean setNavDrawerLogoWithParams(in NavDrawerHeaderParams params);
    boolean setNavDrawerFooterWithParams(in NavDrawerFooterParams params);

    boolean restoreOsmand();

    boolean changePluginState(in PluginParams params);

    boolean registerForOsmandInitListener(in IOsmAndAidlCallback callback);

    boolean getBitmapForGpx(in CreateGpxBitmapParams file, IOsmAndAidlCallback callback);

    int copyFile(in CopyFileParams filePart);

    long registerForNavigationUpdates(in ANavigationUpdateParams params, IOsmAndAidlCallback callback);
}