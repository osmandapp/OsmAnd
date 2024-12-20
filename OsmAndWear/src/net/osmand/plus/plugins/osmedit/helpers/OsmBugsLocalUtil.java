package net.osmand.plus.plugins.osmedit.helpers;



import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.osmedit.data.OsmNotesPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;

import java.util.List;

public class OsmBugsLocalUtil implements OsmBugsUtil {

	private final OsmBugsDbHelper db;

	public OsmBugsLocalUtil(OsmandApplication app, OsmBugsDbHelper db) {
		this.db = db;
	}
	
	@Override
	public OsmBugResult commit(OsmNotesPoint point, String text, Action action) {
		if(action == OsmPoint.Action.CREATE) {
			point.setId(Math.min(-2, db.getMinID() -1));
			point.setText(text);
			point.setAction(action);
		} else {
			OsmNotesPoint pnt = new OsmNotesPoint();
			pnt.setId(point.getId());
			pnt.setLatitude(point.getLatitude());
			pnt.setLongitude(point.getLongitude());
			pnt.setAction(action);
			pnt.setText(text);
			point = pnt;
		}
		return wrap(point, db.addOsmbugs(point));
	}

	@Override
	public OsmBugResult modify(OsmNotesPoint point, String text) {
		OsmNotesPoint pnt = new OsmNotesPoint();
		pnt.setId(point.getId());
		pnt.setLatitude(point.getLatitude());
		pnt.setLongitude(point.getLongitude());
		pnt.setText(text);
		pnt.setAction(point.getAction());
		point = pnt;
		return wrap(point, db.updateOsmBug(point.getId(), text));
	}

	private OsmBugResult wrap(OsmNotesPoint p, boolean success) {
		OsmBugResult s = new OsmBugResult();
		s.local = p;
		s.warning = success ? null : "";
		return s;
	}

	public List<OsmNotesPoint> getOsmbugsPoints() {
		return db.getOsmBugsPoints();
	}
	
}
