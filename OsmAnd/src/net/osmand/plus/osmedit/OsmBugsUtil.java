package net.osmand.plus.osmedit;

public interface OsmBugsUtil {

	public static enum Action {CREATE, MODIFY, CLOSE};

	public boolean createNewBug(double latitude, double longitude, String text, String authorName);

	public boolean addingComment(long id, String text, String authorName);

	public boolean closingBug(long id, String text, String authorName);
}
