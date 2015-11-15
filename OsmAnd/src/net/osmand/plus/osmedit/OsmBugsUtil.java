package net.osmand.plus.osmedit;

public interface OsmBugsUtil {

	public static enum Action {CREATE, MODIFY, CLOSE, REOPEN};
	
	public static class OsmBugResult {
		OsmNotesPoint local;
		String warning;
	}

	public OsmBugResult createNewBug(double latitude, double longitude, String text);

	public OsmBugResult addingComment(double latitude, double longitude, long id, String text);
	
	public OsmBugResult reopenBug(double latitude, double longitude, long id, String text);

	public OsmBugResult closingBug(double latitude, double longitude, long id, String text);
}
