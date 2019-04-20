package net.osmand.aidl;

import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.navigation.ADirectionInfo;

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

    void onContextMenuButtonClicked(in int buttonId, String pointId, String layerId);
}