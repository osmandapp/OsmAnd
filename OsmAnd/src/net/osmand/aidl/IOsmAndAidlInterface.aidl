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

    boolean refreshMap();

    boolean addFavoriteGroup(in AddFavoriteGroupParams params);
    boolean removeFavoriteGroup(in RemoveFavoriteGroupParams params);
    boolean updateFavoriteGroup(in UpdateFavoriteGroupParams params);

    boolean addFavorite(in AddFavoriteParams params);
    boolean removeFavorite(in RemoveFavoriteParams params);
    boolean updateFavorite(in UpdateFavoriteParams params);

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
}