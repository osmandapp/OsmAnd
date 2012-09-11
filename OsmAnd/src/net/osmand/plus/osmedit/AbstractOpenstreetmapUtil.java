package net.osmand.plus.osmedit;


import java.util.ArrayList;
import java.util.List;

import net.osmand.data.Amenity;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Node;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.OsmandApplication;
import android.app.Activity;

public abstract class AbstractOpenstreetmapUtil implements OpenstreetmapUtil  {


	@Override
	public void updateNodeInIndexes(Activity ctx, OsmPoint.Action action, Node n, Node oldNode) {
		final OsmandApplication app = (OsmandApplication) ctx.getApplication();
		final AmenityIndexRepositoryOdb repo = app.getResourceManager().getUpdatablePoiDb();
		showMessageAfterCommit(ctx, app, repo);

		if (repo == null) {
			return;
		}
		
		if (oldNode != n) { //if the node has changed its ID, remove the old one
			repo.deleteAmenities(oldNode.getId() << 1);
			repo.clearCache();
		}
		
		// delete all amenities with same id
		if (OsmPoint.Action.DELETE == action || OsmPoint.Action.MODIFY == action) {
			repo.deleteAmenities(n.getId() << 1);
			repo.clearCache();
		}
		// add amenities
		if (OsmPoint.Action.DELETE != action) {
			List<Amenity> ams = Amenity.parseAmenities(MapRenderingTypes.getDefault(), n, new ArrayList<Amenity>());
			for (Amenity a : ams) {
				repo.addAmenity(a);
				repo.clearCache();
			}
		}
	}

	protected void showMessageAfterCommit(Activity ctx, final OsmandApplication app, final AmenityIndexRepositoryOdb repo) {
		
	}
}
