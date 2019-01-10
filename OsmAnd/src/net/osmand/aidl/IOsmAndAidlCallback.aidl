package net.osmand.aidl;

import net.osmand.aidl.search.SearchResult;

interface IOsmAndAidlCallback {
    void onSearchComplete(in List<SearchResult> resultSet);
    
    void onUpdate();

    void onAppInitialized();
}