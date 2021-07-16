package net.osmand.plus.wikivoyage;

import android.util.Log;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.RequestCreator;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.MapUtils;

import java.util.List;

import static net.osmand.util.MapUtils.ROUNDING_ERROR;

public class WikivoyageUtils {

	private static final String TAG = WikivoyageUtils.class.getSimpleName();

	public static void setupNetworkPolicy(OsmandSettings settings, RequestCreator rc) {
		switch (settings.WIKI_ARTICLE_SHOW_IMAGES.get()) {
			case ON:
				break;
			case OFF:
				rc.networkPolicy(NetworkPolicy.OFFLINE);
				break;
			case WIFI:
				if (!settings.isWifiConnected()) {
					rc.networkPolicy(NetworkPolicy.OFFLINE);
				}
				break;
		}
	}

	public static WptPt findNearestPoint(List<WptPt> points, String coordinates) {
		double lat;
		double lon;
		try {
			lat = Double.parseDouble(coordinates.substring(0, coordinates.indexOf(",")));
			lon = Double.parseDouble(coordinates.substring(coordinates.indexOf(",") + 1));
		} catch (NumberFormatException e) {
			Log.w(TAG, e.getMessage(), e);
			return null;
		}
		for (WptPt point : points) {
			if (MapUtils.getDistance(point.getLatitude(), point.getLongitude(), lat, lon) < ROUNDING_ERROR) {
				return point;
			}
		}
		return null;
	}

}
