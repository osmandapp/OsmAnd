package net.osmand.plus.plugins.osmedit;

import net.osmand.plus.plugins.osmedit.OsmPoint.Action;

public interface OsmBugsUtil {

	class OsmBugResult {
		OsmNotesPoint local;
		String userName;
		String warning;
	}
	
	OsmBugResult commit(OsmNotesPoint bug, String text, Action action);

	OsmBugResult modify(OsmNotesPoint bug, String text);
	
}