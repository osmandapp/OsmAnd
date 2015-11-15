package net.osmand.plus.osmedit;

public interface OsmBugsUtil {

	public static enum Action {CREATE, MODIFY, CLOSE, REOPEN};

	public String createNewBug(double latitude, double longitude, String text);

	public String addingComment(double latitude, double longitude, long id, String text);
	
	public String reopenBug(double latitude, double longitude, long id, String text);

	public String closingBug(double latitude, double longitude, long id, String text);
}
