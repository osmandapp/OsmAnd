package net.osmand.plus.osmedit.dialogs;

import net.osmand.plus.osmedit.OsmPoint;

public interface ProgressDialogPoiUploader {
	void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously);
}
