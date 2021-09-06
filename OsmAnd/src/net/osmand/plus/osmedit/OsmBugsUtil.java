package net.osmand.plus.osmedit;

import net.osmand.plus.osmedit.OsmPoint.Action;

public interface OsmBugsUtil {

	class OsmBugResult {
		OsmNotesPoint local;
		String userName;
		String warning;
	}
	
	OsmBugResult commit(OsmNotesPoint bug, String text, Action action);

	OsmBugResult modify(OsmNotesPoint bug, String text);
	
}