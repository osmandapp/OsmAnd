package net.osmand.plus.track;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.QuadRect;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;

import java.util.List;

public class DisplayGroupRectHelper {

	public static QuadRect getRect(List<GpxDisplayItem> items) {
		QuadRect qr = new QuadRect();
		for (GpxDisplayItem item : items) {
			updateQR(qr, item.locationStart, 0, 0);
		}
		return qr;
	}

	private static void updateQR(QuadRect q, WptPt p, double defLat, double defLon) {
		if (q.left == defLon && q.top == defLat &&
				q.right == defLon && q.bottom == defLat) {
			q.left = p.getLongitude();
			q.right = p.getLongitude();
			q.top = p.getLatitude();
			q.bottom = p.getLatitude();
		} else {
			q.left = Math.min(q.left, p.getLongitude());
			q.right = Math.max(q.right, p.getLongitude());
			q.top = Math.max(q.top, p.getLatitude());
			q.bottom = Math.min(q.bottom, p.getLatitude());
		}
	}

}
