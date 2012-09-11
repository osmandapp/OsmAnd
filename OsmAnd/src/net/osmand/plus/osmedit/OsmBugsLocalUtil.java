package net.osmand.plus.osmedit;



import android.content.Context;

public class OsmBugsLocalUtil implements OsmBugsUtil {


	private final Context ctx;
	private final OsmBugsDbHelper db;

	public OsmBugsLocalUtil(Context uiContext) {
		this.ctx = uiContext;
		this.db = new OsmBugsDbHelper(ctx);
	}

	@Override
	public boolean createNewBug(double latitude, double longitude, String text, String authorName){
		OsmbugsPoint p = new OsmbugsPoint();
		p.setId(-1);
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.CREATE);
		p.setAuthor(authorName);
		return db.addOsmbugs(p);
	}

	@Override
	public boolean addingComment(long id, String text, String authorName){
		OsmbugsPoint p = new OsmbugsPoint();
		p.setId(id);
		p.setText(text);
		p.setAction(OsmPoint.Action.MODIFY);
		p.setAuthor(authorName);
		return db.addOsmbugs(p);
	}

	@Override
	public boolean closingBug(long id, String text, String authorName){
		OsmbugsPoint p = new OsmbugsPoint();
		p.setId(id);
		p.setAuthor(authorName);
		p.setText(text);
		p.setAction(OsmPoint.Action.DELETE);
		return db.addOsmbugs(p);
	}

}
