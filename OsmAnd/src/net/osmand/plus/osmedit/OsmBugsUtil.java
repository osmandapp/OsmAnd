package net.osmand.plus.osmedit;

import net.osmand.plus.osmedit.OsmPoint.Action;

public interface OsmBugsUtil {

	public static class OsmBugResult {
		OsmNotesPoint local;
		String warning;
	}
	
	public OsmBugResult commit(OsmNotesPoint bug, String text, Action action);
	
}
