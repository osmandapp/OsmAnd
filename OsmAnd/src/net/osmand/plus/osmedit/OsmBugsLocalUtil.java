package net.osmand.plus.osmedit;



import java.util.List;

import android.content.Context;

public class OsmBugsLocalUtil implements OsmBugsUtil {


	private final Context ctx;
	private final OsmBugsDbHelper db;

	public OsmBugsLocalUtil(Context uiContext) {
		this.ctx = uiContext;
		this.db = new OsmBugsDbHelper(ctx);
	}

	@Override
	public String createNewBug(double latitude, double longitude, String text, String author){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(Math.min(-2, db.getMinID() -1));
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAuthor(author);
		p.setAction(OsmPoint.Action.CREATE);
		return db.addOsmbugs(p) ? null : "";
	}
	
	public List<OsmNotesPoint> getOsmbugsPoints() {
		return db.getOsmbugsPoints();
	}

	@Override
	public String addingComment(long id, String text, String author){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(id);
		p.setText(text);
		p.setAuthor(author);
		p.setAction(OsmPoint.Action.MODIFY);
		return db.addOsmbugs(p) ? null : "";
	}

	@Override
	public String closingBug(long id, String text, String author){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(id);
		p.setText(text);
		p.setAuthor(author);
		p.setAction(OsmPoint.Action.DELETE);
		return db.addOsmbugs(p) ? null : "";
	}

}
