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
	public OsmBugResult createNewBug(double latitude, double longitude, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(Math.min(-2, db.getMinID() -1));
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.CREATE);
		return wrap(p, db.addOsmbugs(p));
	}
	
	private OsmBugResult wrap(OsmNotesPoint p, boolean success) {
		OsmBugResult s = new OsmBugResult();
		s.local = p;
		s.warning = success ? null : "";
		return s;
	}

	@Override
	public OsmBugResult reopenBug(double latitude, double longitude, long id, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(Math.min(-2, db.getMinID() -1));
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.REOPEN);
		return wrap(p, db.addOsmbugs(p));
	}
	
	public List<OsmNotesPoint> getOsmbugsPoints() {
		return db.getOsmbugsPoints();
	}

	@Override
	public OsmBugResult addingComment(double latitude, double longitude, long id, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(id);
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.MODIFY);
		return wrap(p, db.addOsmbugs(p));
	}

	@Override
	public OsmBugResult closingBug(double latitude, double longitude, long id, String text){
		OsmNotesPoint p = new OsmNotesPoint();
		p.setId(id);
		p.setText(text);
		p.setLatitude(latitude);
		p.setLongitude(longitude);
		p.setAction(OsmPoint.Action.DELETE);
		return wrap(p, db.addOsmbugs(p));
	}

}
