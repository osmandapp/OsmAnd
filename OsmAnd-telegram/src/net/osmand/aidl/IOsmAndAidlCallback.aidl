package net.osmand.aidl;

import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.navigation.ADirectionInfo;
import net.osmand.aidl.navigation.OnVoiceNavigationParams;

interface IOsmAndAidlCallback {

    /**
     *  Callback for search requests.
     *
     *  @return resultSet - set of SearchResult
     */
    void onSearchComplete(in List<SearchResult> resultSet);

    /**
     *  Callback for {@link IOsmAndAidlInterface} registerForUpdates() method.
     */
    void onUpdate();

    /**
     *  Callback for {@link IOsmAndAidlInterface} registerForOsmandInitListener() method.
     */
    void onAppInitialized();

    /**
     *  Callback for {@link IOsmAndAidlInterface} getBitmapForGpx() method.
     *
     *  @return bitmap - snapshot image of gpx track on map
     */
    void onGpxBitmapCreated(in AGpxBitmap bitmap);

    /**
     *  Callback for {@link IOsmAndAidlInterface} registerForNavigationUpdates() method.
     *
     *  @return directionInfo - update on distance to next turn and turns type.
     */
    void updateNavigationInfo(in ADirectionInfo directionInfo);

    /**
     *  Callback for {@link IOsmAndAidlInterface} buttons set with addContextMenuButtons() method.
     *
     *  @param buttonId - id of custom button
     *  @param pointId - id of point button associated with
     *  @param layerId - id of layer point and button associated with
     */
    void onContextMenuButtonClicked(in int buttonId, String pointId, String layerId);

    /**
     *  Callback for {@link IOsmAndAidlInterface} registerForVoiceRouterMessages() method.
     */
    void onVoiceRouterNotify(in OnVoiceNavigationParams params);
}