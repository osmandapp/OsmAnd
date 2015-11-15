package net.osmand.plus.osmedit;



import java.util.List;

import android.content.Context;

public class OsmBugsLocalUtil implements OsmBugsUtil {


	private final Context ctx;
	private final OsmBugsDbHelper db;

	public OsmBugsLocalUtil(Context uiContext, OsmBugsDbHelper db) {
		this.ctx = uiContext;
		this.db = db;
	}

	@Override
	public String createNewBug(double latitude, double longitude, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(Math.min(-2, db.getMinID() -1));
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.CREATE);
		return db.addOsmbugs(p) ? null : "";
	}
	
	@Override
	public String reopenBug(double latitude, double longitude, long id, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(Math.min(-2, db.getMinID() -1));
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.REOPEN);
		return db.addOsmbugs(p) ? null : "";
	}
	
	public List<OsmNotesPoint> getOsmbugsPoints() {
		return db.getOsmbugsPoints();
	}

	@Override
	public String addingComment(double latitude, double longitude, long id, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(id);
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.MODIFY);
		return db.addOsmbugs(p) ? null : "";
	}

	@Override
	public String closingBug(double latitude, double longitude, long id, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(id);
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.DELETE);
		return db.addOsmbugs(p) ? null : "";
	}

}
