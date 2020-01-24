package net.osmand.plus.routepreparationmenu.cards;

import android.support.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class PublicTransportNotFoundWarningCard extends WarningCard {

	public PublicTransportNotFoundWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		imageId = R.drawable.ic_action_pedestrian;
		title = mapActivity.getString(R.string.public_transport_no_route_title) + "\n\n" + mapActivity.getString(R.string.public_transport_try_ped);
		linkText = mapActivity.getString(R.string.public_transport_calc_pedestrian);
	}
}
