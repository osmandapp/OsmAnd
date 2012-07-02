package net.osmand.plus.osmedit;

public interface OsmBugsUtil {

	public boolean createNewBug(double latitude, double longitude, String text, String authorName);

	public boolean addingComment(long id, String text, String authorName);

	public boolean closingBug(long id);
}
