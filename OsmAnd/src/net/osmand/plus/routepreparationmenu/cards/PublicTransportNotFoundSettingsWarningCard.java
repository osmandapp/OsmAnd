package net.osmand.plus.routepreparationmenu.cards;

import android.support.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class PublicTransportNotFoundSettingsWarningCard extends WarningCard {

	public PublicTransportNotFoundSettingsWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		imageId = R.drawable.ic_action_no_route;
		title = mapActivity.getString(R.string.public_transport_no_route_title) + "\n\n" + mapActivity.getString(R.string.public_transport_try_change_settings);
		linkText = mapActivity.getString(R.string.public_transport_type);
	}
}
