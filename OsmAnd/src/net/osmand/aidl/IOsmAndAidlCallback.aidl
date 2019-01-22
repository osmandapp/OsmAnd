package net.osmand.aidl;

import net.osmand.aidl.search.SearchResult;
import net.osmand.aidl.gpx.AGpxBitmap;

interface IOsmAndAidlCallback {
    void onSearchComplete(in List<SearchResult> resultSet);
    
    void onUpdate();

    void onAppInitialized();

    void onGpxBitmapCreated(in AGpxBitmap bitmap);
}