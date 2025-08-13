package net.osmand.plus.configmap.routes;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

public class CycleRouteTypesCard extends MapBaseCard {

	private RouteLayersHelper routeLayersHelper;

	public CycleRouteTypesCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		routeLayersHelper = app.getRouteLayersHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cycle_route_types_card;
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		View topDivider = view.findViewById(R.id.top_divider);
		TextView description = view.findViewById(R.id.description);

		boolean nodeNetworkEnabled = routeLayersHelper.isCycleRoutesNodeNetworkEnabled();
		title.setText(R.string.routes_color_by_type);
		description.setText(nodeNetworkEnabled ? R.string.rendering_value_walkingRoutesOSMCNodes_description : R.string.walking_route_osmc_description);

		TextRadioItem relation = createRadioButton(false, R.string.layer_route);
		TextRadioItem nodeNetworks = createRadioButton(true, R.string.rendering_value_walkingRoutesOSMCNodes_name);

		TextToggleButton radioGroup = new TextToggleButton(app, view.findViewById(R.id.custom_radio_buttons), nightMode);
		radioGroup.setItems(relation, nodeNetworks);
		radioGroup.setSelectedItem(nodeNetworkEnabled ? nodeNetworks : relation);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.descr), false);
	}

	@NonNull
	private TextRadioItem createRadioButton(boolean enabled, int titleId) {
		TextRadioItem item = new TextRadioItem(getString(titleId));
		item.setOnClickListener((radioItem, view) -> {
			routeLayersHelper.toggleCycleRoutesNodeNetwork(enabled);

			updateContent();
			notifyCardPressed();
			return true;
		});
		return item;
	}
}
