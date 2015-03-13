package net.osmand.plus.osmedit;

/**
 * Created by Denis
 * on 11.03.2015.
 */
public interface OsmEditsUploadListener {

	public void uploadUpdated(OsmPoint point);

	public void uploadEnded(Integer result);
}
