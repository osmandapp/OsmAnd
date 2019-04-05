package net.osmand.aidl;

import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.gpx.AGpxBitmap;
import net.osmand.aidl.navigation.ADirectionInfo;

interface IOsmAndAidlCallback {
    void onSearchComplete(in List<SearchResult> resultSet);
    
    void onUpdate();

    void onAppInitialized();

    void onGpxBitmapCreated(in AGpxBitmap bitmap);

    void updateNavigationInfo(in ADirectionInfo directionInfo);
}