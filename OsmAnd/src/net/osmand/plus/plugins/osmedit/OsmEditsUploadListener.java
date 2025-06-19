package net.osmand.plus.plugins.osmedit;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.osmedit.data.OsmPoint;

import java.util.Map;

/**
 * Created by Denis
 * on 11.03.2015.
 */
public interface OsmEditsUploadListener {
	
	void uploadUpdated(@NonNull OsmPoint point);
	
	void uploadEnded(@NonNull Map<OsmPoint, String> loadErrorsMap);
}
