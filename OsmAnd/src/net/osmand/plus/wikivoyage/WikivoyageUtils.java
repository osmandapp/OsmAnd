package net.osmand.plus.wikivoyage;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.RequestCreator;

import net.osmand.plus.settings.backend.OsmandSettings;

public class WikivoyageUtils {

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
}
