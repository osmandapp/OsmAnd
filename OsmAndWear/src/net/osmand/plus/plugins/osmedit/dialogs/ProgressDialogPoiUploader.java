package net.osmand.plus.plugins.osmedit.dialogs;

import net.osmand.plus.plugins.osmedit.data.OsmPoint;

public interface ProgressDialogPoiUploader {
	void showProgressDialog(OsmPoint[] points, boolean closeChangeSet, boolean anonymously);
}
