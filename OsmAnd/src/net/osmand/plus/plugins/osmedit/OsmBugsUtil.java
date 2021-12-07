package net.osmand.plus.plugins.osmedit;

import net.osmand.plus.plugins.osmedit.OsmPoint.Action;

public interface OsmBugsUtil {

	class OsmBugResult {
		public OsmNotesPoint local;
		public String userName;
		public String warning;
	}
	
	OsmBugResult commit(OsmNotesPoint bug, String text, Action action);

	OsmBugResult modify(OsmNotesPoint bug, String text);
	
}