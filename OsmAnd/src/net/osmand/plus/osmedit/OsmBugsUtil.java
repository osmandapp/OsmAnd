package net.osmand.plus.osmedit;

public interface OsmBugsUtil {

	public static enum Action {CREATE, MODIFY, CLOSE};

	public String createNewBug(double latitude, double longitude, String text);

	public String addingComment(long id, String text);

	public String closingBug(long id, String text);
}
