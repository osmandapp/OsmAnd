package net.osmand.aidl;

import java.util.Map;

public interface AidlCallbackListener{
	void addAidlCallback(IOsmAndAidlCallback callback, int key);
	boolean removeAidlCallback(long id);
	Map<Long, OsmandAidlService.AidlCallbackParams> getAidlCallbacks();
}
