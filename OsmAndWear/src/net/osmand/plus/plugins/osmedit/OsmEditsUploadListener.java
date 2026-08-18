package net.osmand.plus.plugins.osmedit;

import net.osmand.plus.plugins.osmedit.data.OsmPoint;

import java.util.Map;

/**
 * Created by Denis
 * on 11.03.2015.
 */
public interface OsmEditsUploadListener {
	
	void uploadUpdated(OsmPoint point);
	
	void uploadEnded(Map<OsmPoint, String> loadErrorsMap);
}
