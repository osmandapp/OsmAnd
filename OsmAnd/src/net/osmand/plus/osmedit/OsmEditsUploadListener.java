package net.osmand.plus.osmedit;

import java.util.Map;

/**
 * Created by Denis
 * on 11.03.2015.
 */
public interface OsmEditsUploadListener {
	
	void uploadUpdated(OsmPoint point);
	
	void uploadEnded(Map<OsmPoint, String> loadErrorsMap);
}
