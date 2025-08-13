package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.HIKING;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class HikingRoutesCard extends MapBaseCard {

	private RouteLayersHelper routeLayersHelper;

	@Nullable
	private RenderingRuleProperty property;

	public HikingRoutesCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		routeLayersHelper = app.getRouteLayersHelper();
		property = app.getRendererRegistry().getCustomRenderingRuleProperty(HIKING.getRenderingPropertyAttr());
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.cycle_route_types_card;
	}

	@Override
	protected void updateContent() {
		boolean enabled = property != null && routeLayersHelper.isHikingRoutesEnabled();
		if (enabled) {
			String selectedValue = routeLayersHelper.getSelectedHikingRoutesValue();
			TextRadioItem selectedItem = null;
			List<TextRadioItem> items = new ArrayList<>();
			for (String value : property.getPossibleValues()) {
				TextRadioItem item = createRadioButton(value);
				if (Algorithms.stringsEqual(value, selectedValue)) {
					selectedItem = item;
				}
				items.add(item);
			}

			TextView title = view.findViewById(R.id.title);
			TextView description = view.findViewById(R.id.description);

			title.setText(R.string.routes_color_by_type);
			description.setText(AndroidUtils.getRenderingStringPropertyDescription(app, selectedValue));

			LinearLayout radioButtonsContainer = view.findViewById(R.id.custom_radio_buttons);
			TextToggleButton radioGroup = new TextToggleButton(app, radioButtonsContainer, nightMode, true);
			radioGroup.setItems(items);
			radioGroup.setSelectedItem(selectedItem);
		}
		updateVisibility(enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.descr), false);
	}

	@NonNull
	private TextRadioItem createRadioButton(@NonNull String value) {
		String name = AndroidUtils.getRenderingStringPropertyValue(app, value);
		TextRadioItem item = new TextRadioItem(name);
		item.setOnClickListener((radioItem, v) -> {
			routeLayersHelper.updateHikingRoutesValue(value);

			updateContent();
			notifyCardPressed();
			return true;
		});
		return item;
	}
}